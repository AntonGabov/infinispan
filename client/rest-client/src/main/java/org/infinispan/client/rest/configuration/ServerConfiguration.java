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

   @Override
   public String toString() {
      return host + ":" + port;
   }

   @Override
   public boolean equals(Object server) {
      boolean isEqual = false;
      if (server instanceof ServerConfiguration &&
            this.host.equals(((ServerConfiguration) server).getHost()) &&
            this.port.equals(((ServerConfiguration) server).getPort())) {
         isEqual = true;
      }

      return isEqual;
   }
}
