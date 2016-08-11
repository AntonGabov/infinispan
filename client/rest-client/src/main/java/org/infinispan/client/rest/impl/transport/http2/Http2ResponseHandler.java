package org.infinispan.client.rest.impl.transport.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http2.HttpConversionUtil;

import java.util.Map;
import java.util.TreeMap;

public class Http2ResponseHandler extends SimpleChannelInboundHandler<HttpResponse> {

   private Map<Integer, FullHttpResponse> streamIdMap;

   public Http2ResponseHandler() {
      streamIdMap = new TreeMap<Integer, FullHttpResponse>();
   }

   /**
    * Get FullHttpResponse by streamId. Also remove this response.
    * @param streamId
    * @return
    */
   public FullHttpResponse getResponseByStreamId(Integer streamId) {
      return streamIdMap.remove(streamId);
   }

   @Override
   protected void channelRead0(ChannelHandlerContext ctx, HttpResponse response) throws Exception {
      Integer streamId = response.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
      if (streamId == null) {
         return;
      }
      streamIdMap.put(streamId, (FullHttpResponse)response);
   }
}
