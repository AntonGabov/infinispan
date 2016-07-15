package org.infinispan.rest.topology;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class TopologyInfo implements Serializable {

   private int topologyId = 1;
   private Set<String> servers;

   public TopologyInfo(String server) {
      servers = new HashSet<String>();
      servers.add(server);
   }

   public int getTopologyId() {
      return topologyId;
   }

   public void updateTopolyId() {
      this.topologyId++;
   }

   public Set<String> getServers() {
      return servers;
   }

   public boolean addServer(String server) {
      return this.servers.add(server);
   }

   public boolean removeServer(String server) {
      return this.servers.remove(server);
   }

   @Override
   public String toString() {
      StringBuilder strServers = new StringBuilder();
      for (String server : servers) {
         if (strServers.length() > 0) {
            strServers.append(",");
         }
         strServers.append(server);
      }
      return "Topology:" + topologyId + ";" + strServers.toString();
   }
}
