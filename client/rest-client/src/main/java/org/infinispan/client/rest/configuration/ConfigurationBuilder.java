package org.infinispan.client.rest.configuration;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.impl.transport.http.HttpTransport;
import org.infinispan.commons.util.Util;

public class ConfigurationBuilder {

   private Class<? extends Transport> transport = HttpTransport.class;
   private List<ServerConfiguration> servers = new LinkedList<ServerConfiguration>(); 
   
   public ConfigurationBuilder() {
   }

   public ConfigurationBuilder addServer(String host, String port) {
      servers.add(new ServerConfiguration(host, port));
      return this;
   }
   
   public ConfigurationBuilder transport(String transport) {
      this.transport = Util.loadClass(transport, Thread.currentThread().getContextClassLoader());
      return this;
   }
   
   public Configuration create() {
      if (servers.isEmpty()) {
         addServer(ConfigurationProperties.DEFAULT_HOST, ConfigurationProperties.DEFAULT_PORT);
      }
      return new Configuration(transport, servers);
   }
}
