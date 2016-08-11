package org.infinispan.client.rest.impl.transport.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.*;

public class Http2ChannelInitializer extends ChannelInitializer<SocketChannel> {

   private HttpToHttp2ConnectionHandler connectionHandler;
   private Http2ResponseHandler responseHandler;
   private Http2SettingsHandler settingsHandler;

   public Http2ResponseHandler responseHandler() {
      return responseHandler;
   }

   public Http2SettingsHandler settingsHandler() {
      return settingsHandler;
   }

   @Override
   protected void initChannel(SocketChannel channel) throws Exception {
      final Http2Connection connection = new DefaultHttp2Connection(false);
      connectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
            .frameListener(new DelegatingDecompressorFrameListener(connection,
                  new InboundHttp2ToHttpAdapterBuilder(connection)
                        .maxContentLength(Integer.MAX_VALUE)
                        .propagateSettings(true)
                        .build()))
            .connection(connection)
            .build();

      responseHandler = new Http2ResponseHandler();
      settingsHandler = new Http2SettingsHandler(channel.newPromise());
      configureSimpleChannel(channel);
   }

   /**
    * NOT YET IMPLEMENTED!
    * @param channel
    */
   private void configureSecuredeChannel(SocketChannel channel) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   private void configureSimpleChannel(SocketChannel channel) {
      HttpClientCodec sourceCodec = new HttpClientCodec();
      Http2ClientUpgradeCodec upgradeCodec = new Http2ClientUpgradeCodec(connectionHandler);
      HttpClientUpgradeHandler upgradeHandler = new HttpClientUpgradeHandler(sourceCodec, upgradeCodec, 65536);

      channel.pipeline().addLast(HttpClientCodec.class.getName(), sourceCodec);
      channel.pipeline().addLast(HttpClientUpgradeHandler.class.getName(), upgradeHandler);
      channel.pipeline().addLast(Http2UpgradeRequestHandler.class.getName(), new Http2UpgradeRequestHandler());
   }

   protected void configureEndOfPipeline(ChannelPipeline pipeline) {
      pipeline.addLast(Http2SettingsHandler.class.getName(), settingsHandler);
      pipeline.addLast(Http2ResponseHandler.class.getName(), responseHandler);
   }

   /**
    * A handler that triggers the cleartext upgrade to HTTP/2 by sending an initial HTTP request.
    */
   private class Http2UpgradeRequestHandler extends ChannelInboundHandlerAdapter {
      @Override
      public void channelActive(ChannelHandlerContext ctx) throws Exception {
         DefaultFullHttpRequest upgradeRequest =
                 new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
         ctx.writeAndFlush(upgradeRequest).syncUninterruptibly();
         ctx.fireChannelActive();
         ctx.pipeline().remove(this);
         configureEndOfPipeline(ctx.pipeline());
      }
   }

}
