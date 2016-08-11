package org.infinispan.client.rest.configuration;

import java.util.List;

import org.infinispan.client.rest.impl.transport.Transport;

public class Configuration {
   private Class transportFactory;
   private List<ServerConfiguration> servers;
   
   public Configuration(Class transportFactory, List<ServerConfiguration> servers) {
      this.transportFactory = transportFactory;
      this.servers = servers;
   }
   
   public Class transportFactory() {
      return transportFactory;
   }
   
   public List<ServerConfiguration> servers() {
      return servers;
   }
}
