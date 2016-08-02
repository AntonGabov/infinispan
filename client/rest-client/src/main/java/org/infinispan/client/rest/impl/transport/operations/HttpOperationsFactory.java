package org.infinispan.client.rest.impl.transport.operations;

import org.infinispan.client.rest.configuration.ServerConfiguration;
import org.infinispan.client.rest.impl.TopologyInfo;
import org.infinispan.client.rest.impl.transport.http.HttpResponseHandler;
import org.infinispan.client.rest.marshall.MarshallUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static org.infinispan.client.rest.impl.protocol.HttpHeaderNames.TOPOLOGY_ID;

public class HttpOperationsFactory {
   
   //private static final Log log = LogFactory.getLog(HttpOperationsFactory.class);
   
   private static final String URI_BASIS = "/rest";
   
   // Variables for Http connection
   private Bootstrap bootstrap;
   private EventLoopGroup workerGroup;
   
   private String getUri(Object... values) {
      StringBuilder bld = new StringBuilder(URI_BASIS);
      for (Object value : values) {
         if (bld.length() > 0) {
            bld.append("/");
         }
         bld.append(value);
      }
      return bld.toString();
   }
   
   public void start() {
      workerGroup = new NioEventLoopGroup();
      bootstrap = new Bootstrap().group(workerGroup).channel(NioSocketChannel.class);
      bootstrap.handler(new ChannelInitializer<SocketChannel>() {
         @Override
         protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new HttpClientCodec());
         }
      });
      bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
      bootstrap.remoteAddress("127.0.0.1", 8080);
   }
   
   public void stop() {
      workerGroup.shutdownGracefully();
   }
   
   public HttpResponse putRequest(TopologyInfo topologyInfo, ServerConfiguration server, Object cacheName, Object key, Object value) {
      ByteBuf content = Unpooled.wrappedBuffer(MarshallUtil.obj2byteArray(value));

      DefaultFullHttpRequest put = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, getUri(cacheName, key),
            content);
      put.headers().add(CONTENT_TYPE, "application/octet-stream");
      put.headers().add(CONTENT_LENGTH, content.readableBytes());
      put.headers().add(TOPOLOGY_ID, topologyInfo.getTopologyId());

      HttpResponseHandler handler = new HttpResponseHandler();
      Channel ch = bootstrap.connect().awaitUninterruptibly().channel().pipeline()
            .addLast(handler).channel();
      try {
         ch.writeAndFlush(put).sync().channel().closeFuture().sync();
      } catch (InterruptedException e) {
         //log.warn("Cannot perform a PUT request");
      }
      
      return handler.getResponse();
   }
   
   public HttpResponse getRequest(TopologyInfo topologyInfo, ServerConfiguration server, Object cacheName, Object key, int maxContentLength) {
      DefaultHttpRequest get = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, getUri(cacheName, key));
      get.headers().add(CONTENT_TYPE, "application/octet-stream");
      get.headers().add(TOPOLOGY_ID, topologyInfo.getTopologyId());
      
      HttpResponseHandler handler = new HttpResponseHandler(true);
      Channel ch = bootstrap.connect(server.getHost(), Integer.valueOf(server.getPort())).awaitUninterruptibly().channel().pipeline()
            .addLast(new HttpObjectAggregator(maxContentLength), handler).channel();
      try {
         ch.writeAndFlush(get).sync().channel().closeFuture().sync();
      } catch (InterruptedException e) {
         //log.warn("Cannot perform a GET request");
      }

      return handler.getResponse();  
   }
}
