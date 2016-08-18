package org.infinispan.rest.http2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.util.AsciiString;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.rest.RestCacheManager;
import org.infinispan.rest.Server;
import org.infinispan.rest.configuration.RestServerConfiguration;
import org.infinispan.rest.logging.RestAccessLoggingHandler;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedJaxrsServer;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.plugins.server.netty.RequestDispatcher;
import org.jboss.resteasy.plugins.server.netty.RequestHandler;
import org.jboss.resteasy.plugins.server.netty.RestEasyHttpRequestDecoder;
import org.jboss.resteasy.plugins.server.netty.RestEasyHttpResponseEncoder;
import org.jboss.resteasy.spi.ResteasyDeployment;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * This class stands for simple HTTP2 server.
 * If host and port are not set, tht server will start on "127.0.0.1:8080".
 */
public class Http2NettyServer implements EmbeddedJaxrsServer {

   private final int MAX_INCOMING_CONNECTIONS = 1024;

   private String host = "127.0.0.1";
   private int port = 8080;
   private int executorThreadCount = 16;
   private int maxHttpContentLength = 16 * 1024;
   private ServerBootstrap bootstrap;
   private EventLoopGroup eventLoopGroup;
   /** This executor is only fot HTTP1 connection */
   private EventLoopGroup eventExecutor;
   protected String root = "";
   protected ResteasyDeployment deployment = new ResteasyDeployment();
   protected SecurityDomain domain;
   protected RestServerConfiguration serverConfiguration;
   protected EmbeddedCacheManager cacheManager;
   protected RestCacheManager restCacheManager;
   protected Cache<Address, String> addressCache;

   public String getHostname() {
      return host;
   }

   public void setHostname(String host) {
      this.host = host;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   @Override
   public void setRootResourcePath(String rootResourcePath) {
      this.root = rootResourcePath;
   }

   @Override
   public ResteasyDeployment getDeployment() {
      return deployment;
   }

   @Override
   public void setDeployment(ResteasyDeployment deployment) {
      this.deployment = deployment;
   }

   @Override
   public void setSecurityDomain(SecurityDomain securityDomain) {
      this.domain = securityDomain;
   }

   public void setCacheManager(EmbeddedCacheManager cacheManager) {
      this.cacheManager = cacheManager;
   }

   public void setRestCacheManager(RestCacheManager restCacheManager) {
      this.restCacheManager = restCacheManager;
   }

   public void setServerConfiguration(RestServerConfiguration serverConfiguration) {
      this.serverConfiguration = serverConfiguration;
   }

   public void setAddressCache(Cache<Address, String> addressCache) {
      this.addressCache = addressCache;
   }

   @Override
   public void start() {
      // Start usual Netty server
      bootstrap = new ServerBootstrap();
      eventLoopGroup = new NioEventLoopGroup();
      bootstrap.option(ChannelOption.SO_BACKLOG, MAX_INCOMING_CONNECTIONS);
      bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
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
      if (eventExecutor != null) {
         eventExecutor.shutdownGracefully();
      }
   }

   private void configureSecuredeChannel(SocketChannel ch) {
      throw new UnsupportedOperationException("Doesn't support SSL");
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
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
               Http2Handler http2Handler = new Http2HandlerBuilder()
                     .addCacheManager(cacheManager)
                     .addAddressCache(addressCache)
                     .build();
               return new Http2ServerUpgradeCodec(http2Handler);
            } else {
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
//   private SimpleChannelInboundHandler<HttpMessage> getHttpFailedUpgradeHandler() {
   private ChannelInboundHandlerAdapter getHttpFailedUpgradeHandler() {
      return new ChannelInboundHandlerAdapter() {

         @Override
         public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            removeCurrentHandlers(ctx);
            startRestEasyDeployment();
            addSpecifiedHandlers(ctx);
            ctx.fireChannelRead(msg);
         }

         protected void channelRead0(ChannelHandlerContext ctx, HttpMessage message) throws Exception {
            removeCurrentHandlers(ctx);
            addSpecifiedHandlers(ctx);
            ctx.fireChannelRead(message);
         }

         private void removeCurrentHandlers(ChannelHandlerContext ctx) {
            Iterator<Entry<String, ChannelHandler>> iterator = ctx.pipeline().iterator();
            while (iterator.hasNext()) {
               ctx.pipeline().remove(iterator.next().getValue());
            }
         }

         private void startRestEasyDeployment() {
            deployment.start();
            Server server = new Server(serverConfiguration, restCacheManager, addressCache);
            deployment.getRegistry().addSingletonResource(server);
            deployment.getProviderFactory().register(new RestAccessLoggingHandler(), ContainerResponseFilter.class,
                  ContainerRequestFilter.class);
         }

         private void addSpecifiedHandlers(ChannelHandlerContext ctx) {
            eventExecutor = new NioEventLoopGroup(executorThreadCount);
            final RequestDispatcher dispatcher = this.createRequestDispatcher();

            ctx.pipeline().addLast(new HttpRequestDecoder());
            ctx.pipeline().addLast(new HttpObjectAggregator(maxHttpContentLength));
            ctx.pipeline().addLast(new HttpResponseEncoder());
            ctx.pipeline().addLast(new RestEasyHttpRequestDecoder(dispatcher.getDispatcher(), root,
                  RestEasyHttpRequestDecoder.Protocol.HTTP));
            ctx.pipeline().addLast(new RestEasyHttpResponseEncoder());
            ctx.pipeline().addLast(eventExecutor, new RequestHandler(dispatcher));
         }

         private RequestDispatcher createRequestDispatcher() {
            return new RequestDispatcher((SynchronousDispatcher) deployment.getDispatcher(),
                  deployment.getProviderFactory(), domain);
         }

      };
   }
}
