package org.infinispan.client.rest.operations.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.Promise;
import org.infinispan.client.rest.marshall.MarshallUtil;
import org.infinispan.client.rest.operations.OperationsFactory;

import java.util.concurrent.SynchronousQueue;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static org.infinispan.client.rest.impl.protocol.HttpHeaderNames.TOPOLOGY_ID;

public class HttpOperationsFactory extends OperationsFactory{

   public HttpOperationsFactory(Channel channel, SynchronousQueue<Promise> syncRequests) {
      super(channel, syncRequests);
   }

   @Override
   public FullHttpResponse putRequest(int topologyId, Object cacheName, Object key, Object value) {
      ByteBuf content = Unpooled.wrappedBuffer(MarshallUtil.obj2byteArray(value));

      DefaultFullHttpRequest put = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, getUri(cacheName, key),
            content);
      put.headers().add(CONTENT_TYPE, "application/octet-stream");
      put.headers().add(CONTENT_LENGTH, content.readableBytes());
      put.headers().add(TOPOLOGY_ID, topologyId);

      return super.executeRequest(put);
   }

   @Override
   public FullHttpResponse getRequest(int topologyId, Object cacheName, Object key) {
      DefaultFullHttpRequest get = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, getUri(cacheName, key));
      get.headers().add(CONTENT_TYPE, "application/octet-stream");
      get.headers().add(TOPOLOGY_ID, topologyId);

      return executeRequest(get);
   }
}
