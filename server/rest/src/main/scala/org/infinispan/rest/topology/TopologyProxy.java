package org.infinispan.rest.topology;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.rest.configuration.RestServerConfiguration;

public class TopologyProxy {

   private final static String TOPOLOGY_CACHE_NAME = "TOPOLOGY";
   private EmbeddedCacheManager cacheManager;
   private String key;

   private enum Operations {
      ADD, REMOVE
   }

   public TopologyProxy(EmbeddedCacheManager cacheManager, RestServerConfiguration serverConfiguration) {
      this.cacheManager = cacheManager;
      this.key = cacheManager.getClusterName();
      createTopologyInfo(serverConfiguration);
   }

   private void createTopologyInfo(RestServerConfiguration serverConfiguration) {
      Cache<String, TopologyInfo> cacheTopology = getCacheTopology();
      if (cacheTopology.get(key) == null) {
         cacheTopology.put(key, new TopologyInfo(getServerInfo(serverConfiguration)));
      } else {
          addServerConfiguration(serverConfiguration);
      }
   }

   public void addServerConfiguration(RestServerConfiguration serverConfiguration) {
      makeOperation(serverConfiguration, Operations.ADD);
   }

   public void removeServerConfiguration(RestServerConfiguration serverConfiguration) {
      makeOperation(serverConfiguration, Operations.REMOVE);
   }

   private void makeOperation(RestServerConfiguration serverConfiguration, Operations oper) {
      Cache<String, TopologyInfo> cacheTopology = getCacheTopology();
      TopologyInfo topology = cacheTopology.get(key);
      String serverInfo = getServerInfo(serverConfiguration);

      boolean isOperExecuted = false;
      switch (oper) {
         case ADD:
            isOperExecuted = topology.addServer(serverInfo);
            break;
         case REMOVE:
            isOperExecuted = topology.removeServer(serverInfo);
            break;
      }

      if (isOperExecuted) {
         topology.updateTopolyId();
         cacheTopology.replace(key, topology);
      }
   }

   public int getTopologyId() {
      return getCacheTopology().get(key).getTopologyId();
   }

   private Cache<String, TopologyInfo> getCacheTopology() {
      return cacheManager.getCache(TOPOLOGY_CACHE_NAME);
   }

   /**
    * Method returns server info as ip_addr:port
    * @return
    */
   private String getServerInfo(RestServerConfiguration configuration) {
      return configuration.host() + ":" + configuration.port();
   }

   public String getTopologyInfo() {
      return getCacheTopology().get(key).toString();
   }

}
