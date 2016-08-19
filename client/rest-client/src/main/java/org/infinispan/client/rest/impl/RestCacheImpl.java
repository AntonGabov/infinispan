package org.infinispan.client.rest.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.infinispan.client.rest.RestCacheManager;
import org.infinispan.client.rest.api.RestCache;
import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.impl.transport.TransportFactory;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

public class RestCacheImpl<K, V> implements RestCache<K, V> {

   private static final Log log = LogFactory.getLog(RestCacheImpl.class, Log.class);

   private final String name;
   private final RestCacheManager restCacheManager;
   private final TransportFactory transportFactory;
   private long defaultLifespan;
   private long defaultMaxIdleTime;

   public RestCacheImpl(RestCacheManager rcm, TransportFactory transportFactory, String name) {
      this(rcm, name, transportFactory, 0, 0);
   }

   public RestCacheImpl(RestCacheManager rcm, String name, TransportFactory transportFactory, long defaultLifespan, long defaultMaxIdleTime) {
      log.info("Creating rest cache: " + name);
      this.name = name;
      this.restCacheManager = rcm;
      this.transportFactory = transportFactory;
      this.defaultLifespan = defaultLifespan;
      this.defaultMaxIdleTime = defaultMaxIdleTime;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public String getVersion() {
      return RestCacheImpl.class.getPackage().getImplementationVersion();
   }

   @Override
   public V put(K key, V value) {
      return put(key, value, defaultLifespan, MILLISECONDS, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public V put(K key, V value, long lifespan, TimeUnit unit) {
      return put(key, value, lifespan, unit, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
      return putIfAbsent(key, value, lifespan, unit, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
      putAll(map, lifespan, unit, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public V replace(K key, V value, long lifespan, TimeUnit unit) {
      return replace(key, value, lifespan, unit, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue, long lifespan, TimeUnit unit) {
      return replace(key, oldValue, newValue, lifespan, unit, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      transportFactory.getTransport().write(name, key, value);
      return value;
   }

   @Override
   public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public V remove(Object key) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public CompletableFuture<V> putAsync(K key, V value) {
      return putAsync(key, value, defaultLifespan, MILLISECONDS, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public CompletableFuture<V> putAsync(K key, V value, long lifespan, TimeUnit unit) {
      return putAsync(key, value, lifespan, unit, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public CompletableFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> data) {
      return putAllAsync(data, defaultLifespan, MILLISECONDS, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit unit) {
      return putAllAsync(data, lifespan, unit, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public CompletableFuture<Void> clearAsync() {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public CompletableFuture<V> putIfAbsentAsync(K key, V value) {
      return putIfAbsentAsync(key, value, defaultLifespan, MILLISECONDS, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public CompletableFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit unit) {
      return putIfAbsentAsync(key, value, lifespan, unit, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public CompletableFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public CompletableFuture<V> removeAsync(Object key) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public CompletableFuture<Boolean> removeAsync(Object key, Object value) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public CompletableFuture<V> replaceAsync(K key, V value) {
      return replaceAsync(key, value, defaultLifespan, MILLISECONDS, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public CompletableFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit unit) {
      return replaceAsync(key, value, lifespan, unit, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public CompletableFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
      return replaceAsync(key, oldValue, newValue, defaultLifespan, MILLISECONDS, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit unit) {
      return replaceAsync(key, oldValue, newValue, lifespan, unit, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public CompletableFuture<V> getAsync(K key) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public V putIfAbsent(K key, V value) {
      return putIfAbsent(key, value, defaultLifespan, MILLISECONDS, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public boolean remove(Object key, Object value) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public V replace(K key, V value) {
      return replace(key, value, defaultLifespan, MILLISECONDS, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      return replace(key, oldValue, newValue, defaultLifespan, MILLISECONDS, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public void clear() {
      // TODO: IMPLEMENT IT
   }

   @Override
   public boolean containsKey(Object key) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public boolean containsValue(Object value) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public Set<java.util.Map.Entry<K, V>> entrySet() {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public V get(Object key) {
      return (V) transportFactory.getTransport().read(name, key);
   }

   @Override
   public boolean isEmpty() {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public Set<K> keySet() {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map) {
      putAll(map, defaultLifespan, MILLISECONDS, defaultMaxIdleTime, MILLISECONDS);
   }

   @Override
   public int size() {
      // TODO: IMPLEMENT IT
      return 0;
   }

   @Override
   public Collection<V> values() {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   @Override
   public void start() {
      log.info("Start the cache with name - " + name);
   }

   @Override
   public void stop() {
      log.info("Stop the cache with name - " + name);
   }

}
