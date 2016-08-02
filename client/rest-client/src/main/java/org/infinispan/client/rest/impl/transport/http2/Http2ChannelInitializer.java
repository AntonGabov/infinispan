package org.infinispan.client.rest.impl.transport.http2;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http2.*;

public class Http2ChannelInitializer extends ChannelInitializer<SocketChannel> {

   private HttpToHttp2ConnectionHandler connectionHandler;

   @Override
   protected void initChannel(SocketChannel socketChannel) throws Exception {
      final Http2Connection connection = new DefaultHttp2Connection(false);
      connectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
         .frameListener(new DelegatingDecompressorFrameListener(connection,
            new InboundHttp2ToHttpAdapterBuilder(connection)
            .maxContentLength(Integer.MAX_VALUE)
            .propagateSettings(true)
            .build()))
         .connection(connection)
         .build();


      HttpClientCodec sourceCodec = new HttpClientCodec();
      Http2ClientUpgradeCodec upgradeCodec = new Http2ClientUpgradeCodec(connectionHandler);
      HttpClientUpgradeHandler upgradeHandler = new HttpClientUpgradeHandler(sourceCodec, upgradeCodec, 65536);
      socketChannel.pipeline().addLast(sourceCodec, upgradeHandler, new Http2UpgradeRequestHandler());
   }
}
