package org.infinispan.rest.http2;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.AsciiString;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class Http2Handler extends Http2ConnectionHandler implements Http2FrameListener {

   protected Http2Handler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) {
      super(decoder, encoder, initialSettings);
   }

   @Override
   public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent) {
         Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText())
               .set(new AsciiString("http-to-http2-upgrade"), new AsciiString("true"));
         encoder().writeHeaders(ctx, 1, headers, 0, true, ctx.newPromise());
      }
      super.userEventTriggered(ctx, evt);
   }

   /**
    * Send a frame for the response status
    * @param ctx
    * @param streamId
    * @param payload
    */
   private void sendResponse(ChannelHandlerContext ctx, int streamId, ByteBuf payload) {
      Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
      encoder().writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise());
      encoder().writeData(ctx, streamId, payload, 0, true, ctx.newPromise());
      ctx.flush();
   }

   @Override
   public int onDataRead(ChannelHandlerContext ctx, int streamId,
                    ByteBuf data, int padding, boolean endOfStream)  {
      int processed = data.readableBytes() + padding;
      if (endOfStream) {
         sendResponse(ctx, streamId, data.retain());
      }
      return processed;
   }

   @Override
   public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) {
      if (endOfStream) {
         ByteBuf content = ctx.alloc().buffer();
         // content.writeBytes(RESPONSE_BYTES.duplicate());
         ByteBufUtil.writeAscii(content, " - via HTTP/2");
         sendResponse(ctx, streamId, content);
      }
   }

   @Override
   public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
                       short weight, boolean exclusive, int padding, boolean endOfStream) {
      onHeadersRead(ctx, streamId, headers, padding, endOfStream);
   }

   @Override
   public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency,
                        short weight, boolean exclusive) {
   }

   @Override
   public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
   }

   @Override
   public void onSettingsAckRead(ChannelHandlerContext ctx) {
   }

   @Override
   public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
   }

   @Override
   public void onPingRead(ChannelHandlerContext ctx, ByteBuf data) {
   }

   @Override
   public void onPingAckRead(ChannelHandlerContext ctx, ByteBuf data) {
   }

   @Override
   public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId,
                          Http2Headers headers, int padding) {
   }

   @Override
   public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) {
   }

   @Override
   public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) {
   }

   @Override
   public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId,
                        Http2Flags flags, ByteBuf payload) {
   }
}
