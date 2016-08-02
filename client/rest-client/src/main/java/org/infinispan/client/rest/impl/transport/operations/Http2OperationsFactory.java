package org.infinispan.client.rest.impl.transport.operations;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.HttpConversionUtil;
import org.infinispan.client.rest.impl.TopologyInfo;
import org.infinispan.client.rest.impl.protocol.HttpHeaderNames;
import org.infinispan.client.rest.impl.transport.http.HttpResponseHandler;
import org.infinispan.client.rest.impl.transport.http2.Http2ChannelInitializer;
import org.infinispan.client.rest.impl.transport.http2.Http2ResponseHandler;
import org.infinispan.client.rest.marshall.MarshallUtil;

import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static org.infinispan.client.rest.impl.protocol.HttpHeaderNames.TOPOLOGY_ID;

public class Http2OperationsFactory {
   //private static final Log log = LogFactory.getLog(HttpOperationsFactory.class);

   private static final String URI_BASIS = "/rest";

   // Variables for Http connection
   private Bootstrap bootstrap;
   private EventLoopGroup workerGroup;
   private Channel channel;
   private Integer streamId = 1;
   private String host;
   private String port;

   public Http2OperationsFactory(String host, String port) {
      this.host = host;
      this.port = port;
   }

   /**
     * Get new Stream Id for HTTP/2 connection. The client has only odd Ids.
     * The '1' stream id stands for Settings negotiation.
     * @return
      */
   private Integer getNewStreamId() {
      streamId += 2;
      return streamId;
   }

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
      bootstrap = new Bootstrap()
        .group(workerGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true);
      bootstrap.handler(new Http2ChannelInitializer());
      bootstrap.remoteAddress(host, Integer.valueOf(port));
      channel = bootstrap.connect().syncUninterruptibly().channel();
   }

   public void stop() {
      workerGroup.shutdownGracefully();
   }

   public FullHttpResponse putRequest(TopologyInfo topologyInfo, Object cacheName, Object key, Object value) {
      Integer currentStreamId = getNewStreamId();
      ByteBuf content = Unpooled.wrappedBuffer(MarshallUtil.obj2byteArray(value));

      FullHttpRequest put = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, getUri(cacheName, key),
          content);
      put.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), HttpScheme.HTTP);
      put.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), currentStreamId);
      put.headers().add(ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      put.headers().add(ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
      put.headers().add(HOST, host + ":" + port);
      put.headers().add(TOPOLOGY_ID, topologyInfo.getTopologyId());

      channel.writeAndFlush(put).syncUninterruptibly();
      FullHttpResponse putResponse = ((Http2ResponseHandler) channel.pipeline()
         .get(Http2ResponseHandler.class.getName())).getResponseByStreamId(currentStreamId);
      return putResponse;
   }

   public FullHttpResponse getRequest(TopologyInfo topologyInfo, Object cacheName, Object key) {
      Integer currentStreamId = getNewStreamId();
      DefaultHttpRequest get = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, getUri(cacheName, key));

      get.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), HttpScheme.HTTP);
      get.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), currentStreamId);
      get.headers().add(ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      get.headers().add(ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
      get.headers().add(HOST, host + ":" + port);
      get.headers().add(TOPOLOGY_ID, topologyInfo.getTopologyId());

      channel.writeAndFlush(get).syncUninterruptibly();
      FullHttpResponse getResponse = ((Http2ResponseHandler) channel.pipeline()
              .get(Http2ResponseHandler.class.getName())).getResponseByStreamId(currentStreamId);

      return getResponse;
   }
}
