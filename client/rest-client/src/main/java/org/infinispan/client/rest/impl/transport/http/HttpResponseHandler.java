package org.infinispan.client.rest.impl.transport.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.concurrent.Promise;

import java.util.Queue;
import java.util.concurrent.SynchronousQueue;

public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

   private Queue<Promise> syncRequests;

   public HttpResponseHandler(Queue<Promise> syncRequests) {
      this.syncRequests = syncRequests;
   }

   protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
      syncRequests.remove().setSuccess(retainResponse(msg));
   }

   /**
    * Retain ByteBuf in the response object in order to use it outside of the handler.
    * @param response
    * @return
     */
   private FullHttpResponse retainResponse(FullHttpResponse response) {
      return response.content().readableBytes() > 0 ? response.retain() : response;
   }

   @Override
   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      throw new Exception(cause);
   }

}
