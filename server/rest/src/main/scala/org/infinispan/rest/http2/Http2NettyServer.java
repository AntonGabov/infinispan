package org.infinispan.rest.http2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.util.AsciiString;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedJaxrsServer;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * This class stands for simple HTTP2 server.
 * If host and port are not set, tht server will start on "127.0.0.1:8080".
 */
public class Http2NettyServer implements EmbeddedJaxrsServer {

   private final int MAX_INCOMING_CONNECTIONS = 1024;

   private String host = "127.0.0.1";
   private String port = "8080";
   private int maxHttpContentLength = 16 * 1024;
   private ServerBootstrap bootstrap;
   private EventLoopGroup eventLoopGroup;

   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public String getPort() {
      return port;
   }

   public void setPort(String port) {
      this.port = port;
   }

   @Override
   public void setRootResourcePath(String s) {

   }

   @Override
   public void start() {
      bootstrap = new ServerBootstrap();
      eventLoopGroup = new NioEventLoopGroup();
      bootstrap.option(ChannelOption.SO_BACKLOG, MAX_INCOMING_CONNECTIONS);
      bootstrap.group(eventLoopGroup);
      bootstrap.channel(NioServerSocketChannel.class);
      bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
         @Override
         protected void initChannel(SocketChannel socketChannel) throws Exception {
            configureSimpleChannel(socketChannel);
         }
      });
      bootstrap.bind(host, Integer.valueOf(port)).syncUninterruptibly();
   }

   @Override
   public void stop() {
      eventLoopGroup.shutdownGracefully();
   }

   @Override
   public ResteasyDeployment getDeployment() {
      return null;
   }

   @Override
   public void setDeployment(ResteasyDeployment resteasyDeployment) {

   }

   @Override
   public void setSecurityDomain(SecurityDomain securityDomain) {

   }

   /**
    * NOT YET IMPLEMENTED!
    * @param ch
    */
   private void configureSecuredeChannel(SocketChannel ch) {
      throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
   }

   private void configureSimpleChannel(SocketChannel ch) {
      HttpServerCodec sourceCodec = new HttpServerCodec();
      ch.pipeline().addLast(sourceCodec);
      ch.pipeline().addLast(getHttpUpgradeHandler(sourceCodec));
      ch.pipeline().addLast(getHttpFailedUpgradeHandler());
   }

   private HttpServerUpgradeHandler getHttpUpgradeHandler(HttpServerCodec sourceCodec) {
      UpgradeCodecFactory upgradeCodecFactory = new UpgradeCodecFactory() {
         @Override
         public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol))
               return new Http2ServerUpgradeCodec(new Http2HandlerBuilder().build());
            else {
               return null;
            }
         }
      };

      return new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory);
   }

   /**
    * If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
    * @return
    */
   private SimpleChannelInboundHandler<HttpMessage> getHttpFailedUpgradeHandler() {
      return new SimpleChannelInboundHandler<HttpMessage>() {
         @Override
         protected void channelRead0(ChannelHandlerContext ctx, HttpMessage message) throws Exception {
            ChannelHandlerContext handlerCtx = ctx.pipeline().context(this);
            ctx.pipeline().addAfter(handlerCtx.name(), null, new Http1Handler());
            ctx.pipeline().replace(this, null, new HttpObjectAggregator(maxHttpContentLength));
            ctx.fireChannelRead(message);
         }
      };
   }
}
