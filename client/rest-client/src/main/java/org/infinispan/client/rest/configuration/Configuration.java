package org.infinispan.client.rest.configuration;

import java.util.List;

import org.infinispan.client.rest.impl.transport.Transport;

public class Configuration {
   private Class<? extends Transport> transport;
   private List<ServerConfiguration> servers;
   
   public Configuration(Class<? extends Transport> transport, List<ServerConfiguration> servers) {
      this.transport = transport;
      this.servers = servers;
   }
   
   public Class<? extends Transport> transport() {
      return transport;
   }
   
   public List<ServerConfiguration> servers() {
      return servers;
   }
}
