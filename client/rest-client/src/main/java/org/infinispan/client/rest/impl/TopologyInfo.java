package org.infinispan.client.rest.impl;

import java.util.Collection;
import java.util.Collections;

import org.infinispan.client.rest.configuration.ServerConfiguration;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

public class TopologyInfo {
   
   private static final Log log = LogFactory.getLog(TopologyInfo.class);
   
   private final int topologyId;
   private final Collection<ServerConfiguration> servers;
   
   public TopologyInfo(int topologyId, Collection<ServerConfiguration> servers) {
      this.topologyId = topologyId;
      this.servers = servers;
   }
   
   public int getTopologyId() {
      return topologyId;
   }
   
   public Collection<ServerConfiguration> servers() {
      return Collections.unmodifiableCollection(servers);
   }

   public TopologyInfo updateTopologyId(int retrievedTopologyId) {
      return new TopologyInfo(retrievedTopologyId, servers);
   }

   public TopologyInfo updateTopologyId(int retrievedTopologyId, Collection<ServerConfiguration> servers) {
      return new TopologyInfo(retrievedTopologyId, servers);
   }
}
