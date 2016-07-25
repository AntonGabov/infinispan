package org.infinispan.rest

import java.io.IOException
import java.util.EnumSet
import javax.ws.rs.container.{ContainerRequestFilter, ContainerResponseFilter}

import org.infinispan.Cache
import org.infinispan.commons.api.Lifecycle
import org.infinispan.commons.equivalence.AnyEquivalence
import org.infinispan.configuration.cache.{CacheMode, ConfigurationBuilder}
import org.infinispan.context.Flag
import org.infinispan.eviction.EvictionStrategy
import org.infinispan.manager.{DefaultCacheManager, EmbeddedCacheManager}
import org.infinispan.registry.InternalCacheRegistry
import org.infinispan.remoting.transport.Address
import org.infinispan.rest.configuration.RestServerConfiguration
import org.infinispan.rest.listeners.{TopologyChangedListener, ViewChangedListener}
import org.infinispan.rest.logging.{Log, RestAccessLoggingHandler}
import org.infinispan.server.core.CacheIgnoreAware
import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer
import org.jboss.resteasy.spi.ResteasyDeployment

import scala.collection.JavaConversions._

final class NettyRestServer (
      val cacheManager: EmbeddedCacheManager, val configuration: RestServerConfiguration,
      netty: NettyJaxrsServer, onStop: EmbeddedCacheManager => Unit) extends Lifecycle with Log with CacheIgnoreAware {

   private val TOPOLOGY_CACHE_NAME: String = "___restTopologyCache"
   private var addressCache: Cache[Address, String] = null
   private var viewChangedListener: ViewChangedListener = null

   override def start(): Unit = {
      netty.start()
      startListeners()
      val deployment = netty.getDeployment
      configuration.getIgnoredCaches.foreach(ignoreCache)
      val restCacheManager = new RestCacheManager(cacheManager, isCacheIgnored)
      val server = new Server(configuration, restCacheManager, addressCache)
      deployment.getRegistry.addSingletonResource(server)
      deployment.getProviderFactory.register(new RestAccessLoggingHandler, classOf[ContainerResponseFilter],
         classOf[ContainerRequestFilter])
      logStartRestServer(configuration.host(), configuration.port())
   }

   override def stop(): Unit = {
      stopListeners()
      netty.stop()
      onStop(cacheManager)
   }

   private def startListeners(): Unit = {
      val isClustered: Boolean = cacheManager.getCacheManagerConfiguration.transport().transport() != null
      if (isClustered) {
         defineTopologyCacheConfig()
         addSelfToTopologyView()
      }
   }

   private def defineTopologyCacheConfig(): Unit = {
      val internalCacheRegistry = cacheManager.getGlobalComponentRegistry.getComponent(classOf[InternalCacheRegistry])
      internalCacheRegistry.registerInternalCache(TOPOLOGY_CACHE_NAME,
         createTopologyCacheConfig(cacheManager.getCacheManagerConfiguration.transport().distributedSyncTimeout()).build(),
         EnumSet.of(InternalCacheRegistry.Flag.EXCLUSIVE))
   }

   private def createTopologyCacheConfig(distSyncTimeout: Long): ConfigurationBuilder = {
      val builder = new ConfigurationBuilder
      builder.clustering().cacheMode(CacheMode.REPL_SYNC)
        .eviction().strategy(EvictionStrategy.NONE)
        .expiration().lifespan(-1).maxIdle(-1)
        // Topology cache uses Object based equals/hashCodes
        .dataContainer()
        .keyEquivalence(AnyEquivalence.getInstance())
        .valueEquivalence(AnyEquivalence.getInstance())
      builder
   }

   private def addSelfToTopologyView() = {
      addressCache = cacheManager.getCache(TOPOLOGY_CACHE_NAME)
      val nodeInClusterAddress: Address = cacheManager.getAddress
      val serverAddress: String = configuration.host() + ":" + configuration.port()

      viewChangedListener = new ViewChangedListener(addressCache)
      cacheManager.addListener(viewChangedListener)

      // Map cluster address to server endpoint address
      // Guaranteed delivery required since if data is lost, there won't be
      // any further cache calls, so negative acknowledgment can cause issues.
      addressCache.getAdvancedCache.withFlags(Flag.SKIP_CACHE_LOAD, Flag.GUARANTEED_DELIVERY)
        .put(nodeInClusterAddress, serverAddress)
   }


   private def stopListeners(): Unit = {
      if (viewChangedListener != null) {
         SecurityActions.removeListener(cacheManager, viewChangedListener)
      }
   }

}

object NettyRestServer extends Log {

   def apply(config: RestServerConfiguration): NettyRestServer = {
      NettyRestServer(config, new DefaultCacheManager(), cm => cm.stop())
   }

   def apply(config: RestServerConfiguration, cfgFile: String): NettyRestServer = {
      NettyRestServer(config, createCacheManager(cfgFile), cm => cm.stop())
   }

   def apply(config: RestServerConfiguration, cm: EmbeddedCacheManager): NettyRestServer = {
      NettyRestServer(config, cm, cm => ())
   }

   private def apply(config: RestServerConfiguration, cm: EmbeddedCacheManager,
         onStop: EmbeddedCacheManager => Unit): NettyRestServer = {
      // Start caches first, if not started
      startCaches(cm)

      val netty = new NettyJaxrsServer()
      val deployment = new ResteasyDeployment()
      netty.setDeployment(deployment)
      netty.setHostname(config.host())
      netty.setPort(config.port())
      netty.setRootResourcePath("")
      netty.setSecurityDomain(null)
      new NettyRestServer(cm, config, netty, onStop)
   }

   private def createCacheManager(cfgFile: String): EmbeddedCacheManager = {
      try {
         new DefaultCacheManager(cfgFile)
      } catch {
         case e: IOException =>
            logErrorReadingConfigurationFile(e, cfgFile)
            new DefaultCacheManager()
      }
   }

   private def startCaches(cm: EmbeddedCacheManager) = {
      // Start defined caches to avoid issues with lazily started caches
      import scala.collection.JavaConversions._
      cm.getCacheNames.foreach(x => SecurityActions.getCache(cm, x))

      // Finally, start default cache as well
      cm.getCache()
   }

}