package org.infinispan.rest.http2;


import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Settings;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;

public class Http2HandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2Handler, Http2HandlerBuilder> {

   private EmbeddedCacheManager cacheManager;
   private Cache addressCache;

   public Http2HandlerBuilder addCacheManager(EmbeddedCacheManager cacheManager) {
      this.cacheManager = cacheManager;
      return this;
   }

   public Http2HandlerBuilder addAddressCache(Cache addressCache) {
      this.addressCache = addressCache;
      return this;
   }


   @Override
   public Http2Handler build() {
      return super.build();
   }

   @Override
   protected Http2Handler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings settings) {
      Http2Handler handler = new Http2Handler(decoder, encoder, settings, cacheManager, addressCache);
      frameListener(handler);
      return handler;
   }
}
