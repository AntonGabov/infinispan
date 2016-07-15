package org.infinispan.client.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.client.rest.api.RestCache;
import org.infinispan.client.rest.api.RestCacheContainer;
import org.infinispan.client.rest.configuration.Configuration;
import org.infinispan.client.rest.configuration.ConfigurationBuilder;
import org.infinispan.client.rest.impl.RestCacheImpl;
import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.impl.transport.TransportConstants;
import org.infinispan.commons.util.Util;

public class RestCacheManager implements RestCacheContainer {

   //private static final Log log = LogFactory.getLog(RestCacheManager.class);
   public static final String DEFAULT_CACHE_NAME = "default";

   protected Transport transport;
   protected Configuration configuration;

   private volatile boolean isStarted = false;
   private final Map<String, RestCache<?, ?>> cacheContainer = new HashMap<>();

   public RestCacheManager() {
      createConfiguration();
      start();
   }

   private void createConfiguration() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      this.configuration = builder.create();
   }

   @Override
   public <K, V> RestCache<K, V> getCache() {
      return getCache(DEFAULT_CACHE_NAME);
   }

   @Override
   public <K, V> RestCache<K, V> getCache(String cacheName) {
      synchronized (cacheContainer) {
         RestCache<K, V> cache = null;
         if (!cacheContainer.containsKey(cacheName)) {
            cache = createCache(cacheName);
            cacheContainer.put(cacheName, cache);
         } else {
            cache = (RestCache<K, V>) cacheContainer.get(cacheName);
         }

         return cache;
      }
   }

   @Override
   public void start() {
      transport = Util.getInstance(configuration.transport());
      transport.start(configuration, TransportConstants.DEFAULT_TOPOLOGY_ID);
      //log.info("RestManager is started");
      isStarted = true;
   }

   @Override
   public void stop() {
      synchronized (cacheContainer) {
         cacheContainer.clear();
      }
      transport.stop();
      
      //log.info("RestManager is stopped");
      isStarted = false;
   }

   public boolean isStarted() {
      return isStarted;
   }

   /**
    * 
    * Create a new instance of {@link org.infinispan.client.rest.impl.RestCacheImpl}
    * 
    * @param cacheName
    * @return a cache instance
    */
   private <K, V> RestCache<K, V> createCache(String cacheName) {
      RestCache<K, V> newCache = new RestCacheImpl<>(this, transport, cacheName);
      newCache.start();
      return newCache;
   }

}
