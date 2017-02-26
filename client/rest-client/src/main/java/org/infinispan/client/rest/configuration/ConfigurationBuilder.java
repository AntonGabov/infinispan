package org.infinispan.client.rest.configuration;

import org.infinispan.client.rest.impl.transport.TransportFactory;
import org.infinispan.commons.util.Util;

import java.util.LinkedList;
import java.util.List;

public class ConfigurationBuilder {

   private Class transportFactory = TransportFactory.class;
   private List<ServerConfiguration> servers = new LinkedList<ServerConfiguration>();
   private boolean isSsl = true;
   
   public ConfigurationBuilder() {
   }

   public ConfigurationBuilder addServer(String host, String port) {
      servers.add(new ServerConfiguration(host, port));
      return this;
   }
   
   public ConfigurationBuilder transport(String transport) {
      this.transportFactory = Util.loadClass(transport, Thread.currentThread().getContextClassLoader());
      return this;
   }

   public ConfigurationBuilder setSsl(boolean isSsl) {
      this.isSsl = isSsl;
      return this;
   }

   public Configuration create() {
      if (servers.isEmpty()) {
         addServer(ConfigurationProperties.DEFAULT_HOST, ConfigurationProperties.DEFAULT_PORT);
      }
      return new Configuration(transportFactory, servers, isSsl);
   }
}
