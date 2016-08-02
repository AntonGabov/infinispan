package org.infinispan.client.rest.impl.transport.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;

public class HttpResponseHandler extends SimpleChannelInboundHandler<HttpResponse> {

   private HttpResponse response;

   private boolean retainResponse;

   public HttpResponseHandler() {
      this(false);
   }

   public HttpResponseHandler(boolean retainResponse) {
      this.retainResponse = retainResponse;
   }

   protected void channelRead0(ChannelHandlerContext ctx, HttpResponse msg) throws Exception {    
      if (retainResponse) {
         this.response = ((FullHttpResponse) msg).retain();
      } else {
         this.response = msg;
      }
      ctx.close();
   }

   @Override
   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      throw new Exception(cause);
   }

   public HttpResponse getResponse() {
      return response;
   }

}
