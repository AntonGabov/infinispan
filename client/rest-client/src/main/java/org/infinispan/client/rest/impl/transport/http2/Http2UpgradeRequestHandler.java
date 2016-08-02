package org.infinispan.client.rest.impl.transport.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.infinispan.client.rest.impl.transport.http.HttpResponseHandler;

public class Http2UpgradeRequestHandler extends SimpleChannelInboundHandler<HttpResponse> {

   @Override
   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      DefaultFullHttpRequest upgradeRequest =
            new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
      ctx.writeAndFlush(upgradeRequest).syncUninterruptibly();
      ctx.fireChannelActive();
   }

   @Override
   protected void channelRead0(ChannelHandlerContext ctx, HttpResponse response) throws Exception {
      ctx.pipeline().remove(this);
      if (HttpResponseStatus.SWITCHING_PROTOCOLS.equals(response.status())) {
         ctx.pipeline().addLast(Http2ResponseHandler.class.getName(), new Http2ResponseHandler());
      } else if (HttpResponseStatus.OK.equals(response.status())) {
         ctx.pipeline().addLast(new HttpResponseHandler());
      }
   }
}
