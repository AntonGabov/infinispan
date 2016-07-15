package org.infinispan.client.rest.configuration;

public class ServerConfiguration {

   private String host;
   private String port;
   
   public ServerConfiguration(String host, String port) {
      this.host = host;
      this.port = port;
   }
   
   public String getHost() {
      return host;
   }

   public String getPort() {
      return port;
   }
}
