package org.infinispan.client.rest.operations.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.concurrent.Promise;
import org.infinispan.client.rest.marshall.MarshallUtil;
import org.infinispan.client.rest.operations.OperationsFactory;

import java.util.concurrent.SynchronousQueue;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static org.infinispan.client.rest.impl.protocol.HttpHeaderNames.TOPOLOGY_ID;

public class Http2OperationsFactory extends OperationsFactory {

   private Integer streamId = 1;

   public Http2OperationsFactory(Channel channel, SynchronousQueue<Promise> syncRequests) {
      super(channel, syncRequests);
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

   @Override
   public FullHttpResponse putRequest(int topologyId, Object cacheName, Object key, Object value) {
      Integer currentStreamId = getNewStreamId();
      ByteBuf content = Unpooled.wrappedBuffer(MarshallUtil.obj2byteArray(value));

      DefaultFullHttpRequest put = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, getUri(cacheName, key),
              content);
      put.headers().add(CONTENT_TYPE, "application/octet-stream");
      put.headers().add(CONTENT_LENGTH, content.readableBytes());
      put.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), HttpScheme.HTTP);
      put.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), currentStreamId);
      put.headers().add(ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      put.headers().add(ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
      put.headers().add(HOST, super.getHostAddress());
      put.headers().add(TOPOLOGY_ID, topologyId);

      return super.executeRequest(put);
   }

   @Override
   public FullHttpResponse getRequest(int topologyId, Object cacheName, Object key) {
      Integer currentStreamId = getNewStreamId();
      DefaultFullHttpRequest get = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, getUri(cacheName, key));

      get.headers().add(CONTENT_TYPE, "application/octet-stream");
      get.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), HttpScheme.HTTP);
      get.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), currentStreamId);
      get.headers().add(ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      get.headers().add(ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
      get.headers().add(HOST, super.getHostAddress());
      get.headers().add(TOPOLOGY_ID, topologyId);

      return super.executeRequest(get);
   }
}
