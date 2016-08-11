package org.infinispan.client.rest.operations;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.concurrent.SynchronousQueue;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.infinispan.client.rest.operations.OperationsConstants.OperationType;
import static org.infinispan.client.rest.operations.OperationsConstants.URI_BASIS;

public abstract class OperationsFactory {

   //private static final Log log = LogFactory.getLog(OperationsFactory.class);

   private OperationType type = OperationType.HTTP_1;
   // Variables for synchronous request-response
   private Channel channel;
   private SynchronousQueue<Promise> syncRequests;

   public OperationsFactory(Channel channel, SynchronousQueue<Promise> syncRequests) {
      this.channel = channel;
      this.syncRequests = syncRequests;
   }

   protected String getUri(Object... values) {
      StringBuilder bld = new StringBuilder(URI_BASIS);
      for (Object value : values) {
         if (bld.length() > 0) {
            bld.append("/");
         }
         bld.append(value);
      }
      return bld.toString();
   }

   protected String getHostAddress() {
      InetSocketAddress address = (InetSocketAddress) channel.localAddress();
      return address.getHostName() + ":" + address.getPort();
   }

   public abstract FullHttpResponse putRequest(int topologyId, Object cacheName, Object key, Object value);

   public abstract FullHttpResponse getRequest(int topologyId, Object cacheName, Object key);

   /**
    * Call this method when you need to perform a request and get response back.
    * @param request
    * @return
    */
   protected FullHttpResponse executeRequest(HttpRequest request) {
      // TODO: Maybe it can be implemented in a more efficient way
      try {
         Promise<FullHttpResponse> promiseRequest = new DefaultPromise(new DefaultEventExecutor());
         channel.writeAndFlush(request).syncUninterruptibly();
         syncRequests.put(promiseRequest);
         return promiseRequest.awaitUninterruptibly().getNow();
      } catch (InterruptedException e) {
         // log.warn("Cannot perform " + request.method() + " request: " + e.getStackTrace());
      }
      return new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
   }

}
