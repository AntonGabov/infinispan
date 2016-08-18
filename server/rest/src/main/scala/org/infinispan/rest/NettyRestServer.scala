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
import org.infinispan.rest.http2.Http2NettyServer
import org.infinispan.rest.listeners.ViewChangedListener
import org.infinispan.rest.logging.{Log, RestAccessLoggingHandler}
import org.infinispan.server.core.CacheIgnoreAware
import org.jboss.resteasy.plugins.server.embedded.EmbeddedJaxrsServer
import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer
import org.jboss.resteasy.spi.ResteasyDeployment

import scala.collection.JavaConversions._

final class NettyRestServer (
      val cacheManager: EmbeddedCacheManager, val configuration: RestServerConfiguration,
      netty: EmbeddedJaxrsServer, onStop: EmbeddedCacheManager => Unit, isNeedToStartHttp2: Boolean)
      extends Lifecycle with Log with CacheIgnoreAware {

   private val TOPOLOGY_CACHE_NAME: String = "___restTopologyCache"
   private var addressCache: Cache[Address, String] = null
   private var viewChangedListener: ViewChangedListener = null

   override def start(): Unit = {
      startListeners()
      configuration.getIgnoredCaches.foreach(ignoreCache)
      val restCacheManager = new RestCacheManager(cacheManager, isCacheIgnored)

      if (isNeedToStartHttp2) {
         netty.asInstanceOf[Http2NettyServer].setRestCacheManager(restCacheManager)
         netty.asInstanceOf[Http2NettyServer].setServerConfiguration(configuration)
         netty.asInstanceOf[Http2NettyServer].setAddressCache(addressCache)
         netty.start()
      } else {
         netty.start()
         val deployment = netty.getDeployment
         val server = new Server(configuration, restCacheManager, addressCache)
         deployment.getRegistry.addSingletonResource(server)
         deployment.getProviderFactory.register(new RestAccessLoggingHandler, classOf[ContainerResponseFilter],
            classOf[ContainerRequestFilter])
         logStartRestServer(configuration.host(), configuration.port())
      }
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

   val IS_NEED_TO_START_HTTP2: Boolean = true

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

      var netty: EmbeddedJaxrsServer = null
      val deployment = new ResteasyDeployment()
      if (IS_NEED_TO_START_HTTP2) {
         netty = new Http2NettyServer()
         netty.asInstanceOf[Http2NettyServer].setHostname(config.host())
         netty.asInstanceOf[Http2NettyServer].setPort(config.port())
         netty.asInstanceOf[Http2NettyServer].setCacheManager(cm)
     } else {
         netty = new NettyJaxrsServer()
         netty.asInstanceOf[NettyJaxrsServer].setHostname(config.host())
         netty.asInstanceOf[NettyJaxrsServer].setPort(config.port())
      }
      netty.setDeployment(deployment)
      netty.setRootResourcePath("")
      netty.setSecurityDomain(null)
      // THIS IS FOR HTTP2 Server
      //val netty = new NettyJaxrsServer()

      new NettyRestServer(cm, config, netty, onStop, IS_NEED_TO_START_HTTP2)
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