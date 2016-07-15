package org.infinispan.client.rest.configuration;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.impl.transport.http.HttpTransport;
import org.infinispan.commons.util.Util;

public class ConfigurationBuilder {
   
   private WeakReference<ClassLoader> classLoader;
   private Class<? extends Transport> transport = HttpTransport.class;
   private List<ServerConfiguration> servers = new LinkedList<ServerConfiguration>(); 
   
   public ConfigurationBuilder() {
      this.classLoader = new WeakReference<ClassLoader>(Thread.currentThread().getContextClassLoader());
   }
   
   private ClassLoader classLoader() {
      return classLoader != null ? classLoader.get() : null;
   }
   
   public ConfigurationBuilder addServer(String host, String port) {
      servers.add(new ServerConfiguration(host, port));
      return this;
   }
   
   public ConfigurationBuilder transport(String transport) {
      this.transport = Util.loadClass(transport, this.classLoader());
      return this;
   }

   public ConfigurationBuilder transportFactory(Class<? extends Transport> transport) {
      this.transport = transport;
      return this;
   }
   
   public Configuration create() {
      if (servers.isEmpty()) {
         addServer(ConfigurationProperties.DEFAULT_HOST, ConfigurationProperties.DEFAULT_PORT);
      }
      return new Configuration(transport, servers);
   }
}
