package org.infinispan.rest;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listenable;
import org.infinispan.security.Security;
import org.infinispan.security.actions.GetCacheAction;
import org.infinispan.security.actions.RemoveListenerAction;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * SecurityActions for the org.infinispan.server.hotrod package.
 *
 * Do not move. Do not change class and method visibility to avoid being called from other
 * {@link java.security.CodeSource}s, thus granting privilege escalation to external code.
 *
 * @author Tristan Tarrant
 * @since 8.2
 */
final class SecurityActions {
    private static <T> T doPrivileged(PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return Security.doPrivileged(action);
        }
    }

    @SuppressWarnings("unchecked")
    static <K, V> org.infinispan.Cache<K, V> getCache(final EmbeddedCacheManager cacheManager, String cacheName) {
        GetCacheAction action = new GetCacheAction(cacheManager, cacheName);
        return (org.infinispan.Cache<K, V>) doPrivileged(action);
    }

    static Void removeListener(Listenable listenable, Object listener) {
        RemoveListenerAction action = new RemoveListenerAction(listenable, listener);
        return doPrivileged(action);
    }

}
