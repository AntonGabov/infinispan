package org.infinispan.client.rest.configuration;

import java.util.List;

import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.impl.transport.TransportFactory;

public class Configuration {
   private Class<? extends TransportFactory> transportFactory;
   private List<ServerConfiguration> servers;
   
   public Configuration(Class transportFactory, List<ServerConfiguration> servers) {
      this.transportFactory = transportFactory;
      this.servers = servers;
   }
   
   public Class<? extends TransportFactory> transportFactory() {
      return transportFactory;
   }
   
   public List<ServerConfiguration> servers() {
      return servers;
   }
}
