package org.infinispan.client.rest.operations;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import org.infinispan.client.rest.impl.transport.http.HttpResponseHandler;
import org.infinispan.client.rest.impl.transport.http2.Http2ResponseHandler;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.infinispan.client.rest.operations.OperationsConstants.OperationType;
import static org.infinispan.client.rest.operations.OperationsConstants.URI_BASIS;

public abstract class OperationsFactory {

   private static final Log log = LogFactory.getLog(OperationsFactory.class);

   /**
    * This lock is for waiting of responses
    */
   private final Object lock = new Object();
   private Channel channel;
   private OperationType type;

   public OperationsFactory(Channel channel, OperationType type) {
      this.channel = channel;
      this.type = type;
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
    * Call this method when you need to perform a request and get response back in a synchronous way.
    * Response will be waited for 10 seconds. If it doesn't come, the BAD_REQUEST response will be obtained.
    * @param request
    * @return
    */
   public FullHttpResponse executeSynchronousRequest(HttpRequest request) {
      synchronized (lock) {
         log.info(request.method() + " synchronous request :" + request.uri());

         prepareRequestForExecution();
         channel.writeAndFlush(request).addListeners();
         return getResponse();
      }
   }

   private void prepareRequestForExecution() {
      switch (type) {
         case HTTP_2:
            ((Http2ResponseHandler) channel.pipeline().get(Http2ResponseHandler.class.getName()))
                    .preparePromiseForResponse();
            break;
         default:
            ((HttpResponseHandler) channel.pipeline().get(HttpResponseHandler.class.getName()))
                    .preparePromiseForResponse();
            break;
      }
   }

   private FullHttpResponse getResponse() {
      FullHttpResponse response;
      switch (type) {
         case HTTP_2:
            response = ((Http2ResponseHandler) channel.pipeline().get(Http2ResponseHandler.class.getName()))
                    .awaitResponse(10, TimeUnit.SECONDS);
            break;
         default:
            response = ((HttpResponseHandler) channel.pipeline().get(HttpResponseHandler.class.getName()))
                    .awaitResponse(10, TimeUnit.SECONDS);
            break;
      }
      return response;
   }

}