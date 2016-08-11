package org.infinispan.client.rest.impl.transport.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2Settings;

import java.util.concurrent.TimeUnit;

public class Http2SettingsHandler extends SimpleChannelInboundHandler<Http2Settings> {

   private ChannelPromise promise;

   public Http2SettingsHandler(ChannelPromise promise) {
      this.promise = promise;
   }

   /**
    * Wait for this handler to be added after the upgrade to HTTP/2, and for initial preface
    * handshake to complete.
    *
    * @param timeout
    * @param unit
    * @throws Exception
    */
   public void awaitSettings(long timeout, TimeUnit unit) throws IllegalStateException {
      if (!promise.awaitUninterruptibly(timeout, unit)) {
         throw new IllegalStateException("Timed out waiting for settings");
      } else if (!promise.isSuccess()) {
         throw new RuntimeException(promise.cause());
      }
   }

   @Override
   protected void channelRead0(ChannelHandlerContext ctx, Http2Settings settings) throws Exception {
      promise.setSuccess();
      ctx.pipeline().remove(this);
   }
}
