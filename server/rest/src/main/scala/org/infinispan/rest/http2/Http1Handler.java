package org.infinispan.rest.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpUtil;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Http1Handler extends SimpleChannelInboundHandler<FullHttpRequest> {

   @Override
   protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
      if (HttpUtil.is100ContinueExpected(request)) {
         ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
      }
      boolean keepAlive = HttpUtil.isKeepAlive(request);

      ByteBuf content = ctx.alloc().buffer();
      ByteBufUtil.writeAscii(content, " - via " + request.protocolVersion());

      FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
      response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
      response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

      if (!keepAlive) {
         ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
      } else {
         response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
         ctx.writeAndFlush(response);
      }
   }
}
