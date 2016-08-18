package org.infinispan.client.rest.impl.transport.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Http2ResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

   private static final Log log = LogFactory.getLog(Http2ResponseHandler.class);

   private Promise<FullHttpResponse> promise;

   /**
    * This method should be called before request is executed.
    */
   public void preparePromiseForResponse() {
      this.promise = new DefaultPromise<>(new DefaultEventExecutor());
   }

   /**
    * Wait for a time duration for each anticipated response.
    * After time is finished, return response.
    * @param timeout
    * @param unit
     */
   public FullHttpResponse awaitResponse(long timeout, TimeUnit unit) {
      FullHttpResponse response;
      if (promise.awaitUninterruptibly(timeout, unit) && promise.isSuccess()) {
         response = promise.getNow();
      } else {
         response = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
      }
      promise = null;
      return response;
   }

   @Override
   protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
      log.info("HTTP/2 response with streamId = " +
            response.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text()));
      if (promise != null) {
         promise.setSuccess(retainResponse(response));
      }
   }

   /**
    * Retain ByteBuf in the response object in order to use it outside of the handler.
    * @param response
    * @return
    */
   private FullHttpResponse retainResponse(FullHttpResponse response) {
      return response.content().isReadable() ? response.retain() : response;
   }

   @Override
   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      throw new Exception(cause);
   }
}
