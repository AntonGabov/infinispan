package org.infinispan.client.rest.impl.transport;

import org.infinispan.client.rest.configuration.Configuration;
import org.infinispan.client.rest.configuration.ServerConfiguration;
import org.infinispan.client.rest.impl.TopologyInfo;
import org.infinispan.client.rest.impl.transport.http.HttpTransport;
import org.infinispan.client.rest.impl.transport.http2.Http2Transport;
import org.infinispan.client.rest.operations.OperationsConstants;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransportFactory {

   private Configuration configuration;
   private List<ServerConfiguration> initialServers = new LinkedList<ServerConfiguration>();
   private volatile TopologyInfo topologyInfo;
   private Map<ServerConfiguration, Transport> transports = new ConcurrentHashMap<ServerConfiguration, Transport>();

   public Transport getTransport() {
      return getTransport(initialServers.get(0));
   }

   public Transport getTransport(ServerConfiguration server) {
      return transports.get(server);
   }

   public void start(Configuration configuration) {
      this.configuration = configuration;
      initialServers.addAll(configuration.servers());
      topologyInfo = new TopologyInfo(TransportConstants.DEFAULT_TOPOLOGY_ID, initialServers);
      startTransports();
   }

   public int getTopologyId() {
      return topologyInfo.getTopologyId();
   }

   public void updateTopologyId(int retrievedTopologyId) {
      topologyInfo = topologyInfo.updateTopologyId(retrievedTopologyId);
   }

   public void updateTopologyId(int retrievedTopologyId, Collection<ServerConfiguration> servers) {
      topologyInfo = topologyInfo.updateTopologyId(retrievedTopologyId, servers);
   }

   private void startTransports() {
      initialServers.forEach(server -> {
         Transport transport = new Http2Transport();
         transports.put(server, transport);
         transport.start(server, this, OperationsConstants.OperationType.HTTP_2);
      });
   }

   public void stop() {
      transports.forEach((key, value) -> {
         value.stop();
      });
   }
}
