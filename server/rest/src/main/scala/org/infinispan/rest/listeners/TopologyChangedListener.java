package org.infinispan.rest.listeners;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;

// TODO: Why addressCache is null???
// TODO: Use it for "Split brain" situation
@Listener(sync = false)
public class TopologyChangedListener {

   private final Cache addressCache;
   private final Address nodeAddress;
   private final String serverAddress;

   public TopologyChangedListener(Cache addressCache, Address nodeAddress, String serverAddress) {
      this.addressCache = addressCache;
      this.nodeAddress = nodeAddress;
      this.serverAddress = serverAddress;
   }

   @TopologyChanged
   public void handlerTopologyChanged(TopologyChangedEvent event) {
      if (event.isPre() || !addressCache.getStatus().allowInvocations()) {
         return;
      }
   }

}
