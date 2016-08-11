package org.infinispan.client.rest.impl.transport.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.impl.transport.TransportConstants;

public class HttpTransport extends Transport {

   //private static final Log log = LogFactory.getLog(HttpTransport.class);

   @Override
   protected void beforeConnect() {
      bootstrap.handler(new ChannelInitializer<SocketChannel>() {
         @Override
         protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(HttpClientCodec.class.getName(), new HttpClientCodec());
            ch.pipeline().addLast(HttpObjectAggregator.class.getName(), new HttpObjectAggregator(TransportConstants.MAX_CONTENT_LENGTH));
            ch.pipeline().addLast(HttpResponseHandler.class.getName(), new HttpResponseHandler(syncRequests));
         }
      });
   }

   @Override
   protected void afterConnect() {
   }

//   @Override
//   protected void setupOperationsFactory() {
//      if (OperationsConstants.OperationType.HTTP_1.equals(type)) {
//         super.operationsFactory = new org.infinispan.client.rest.operations.http.HttpOperationsFactory(channel);
//      }
//   }

}
