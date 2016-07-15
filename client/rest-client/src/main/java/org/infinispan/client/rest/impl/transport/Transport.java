package org.infinispan.client.rest.impl.transport;

import org.infinispan.client.rest.configuration.Configuration;

public interface Transport {
   
   void start(Configuration configuration, int initialTopologyId);
   
   void stop();
   
   void write(Object cacheName, Object key, Object value);
   
   Object read(Object cacheName, Object key);

}
