package org.infinispan.rest.listeners;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;

import java.util.List;
import java.util.stream.Collectors;

@Listener(sync = false)
public class ViewChangedListener {

   private final Cache localAddressCache;

   public ViewChangedListener(Cache cache) {
      localAddressCache = cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL);
   }

   @ViewChanged
   public void handleViewChanged(ViewChangedEvent event) {
      List<Address> goneAddressList = event.getOldMembers().stream().collect(Collectors.toList());
      goneAddressList.removeIf(address -> event.getNewMembers().contains(address));

      goneAddressList.forEach(address -> {
         if (localAddressCache.getStatus().allowInvocations()) {
            localAddressCache.remove(address);
         }
      });
   }
}
