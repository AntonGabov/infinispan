package org.infinispan.client.rest.impl.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.Promise;
import org.infinispan.client.rest.configuration.ServerConfiguration;
import org.infinispan.client.rest.impl.protocol.HttpHeaderNames;
import org.infinispan.client.rest.impl.transport.http.HttpResponseHandler;
import org.infinispan.client.rest.impl.transport.http2.Http2ResponseHandler;
import org.infinispan.client.rest.marshall.MarshallUtil;
import org.infinispan.client.rest.operations.OperationsConstants;
import org.infinispan.client.rest.operations.OperationsFactory;
import org.infinispan.client.rest.operations.http.HttpOperationsFactory;
import org.infinispan.client.rest.operations.http2.Http2OperationsFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

import static org.infinispan.client.rest.operations.OperationsConstants.*;

public abstract class Transport {

   // Variables for Http connection
   protected Bootstrap bootstrap;
   protected EventLoopGroup workerGroup;
   protected Channel channel;
   protected ServerConfiguration server;

   protected OperationType type;
   protected TransportFactory transportFactory;
   protected OperationsFactory operationsFactory;
   protected SynchronousQueue<Promise> syncRequests = new SynchronousQueue<>();

   /**
    * Start a new transport with attempt to establish HTTP2 connection.
    * @param server
    * @param transportFactory
     */
   public void start(ServerConfiguration server, TransportFactory transportFactory) {
      this.start(server, transportFactory, OperationType.HTTP_2);
   }

   /**
    * Start a new transport.
    * @param server
    * @param transportFactory
    * @param type
     */
   public void start(ServerConfiguration server, TransportFactory transportFactory, OperationType type) {
      this.server = server;
      this.transportFactory = transportFactory;
      this.type = type;

      workerGroup = new NioEventLoopGroup();
      bootstrap = new Bootstrap()
              .group(workerGroup)
              .channel(NioSocketChannel.class);
      bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
      bootstrap.remoteAddress(server.getHost(), Integer.valueOf(server.getPort()));
      beforeConnect();
      channel = bootstrap.connect().syncUninterruptibly().channel();
      afterConnect();
      setupOperationsFactory();
   }

   /**
    * Implement additional logic, when connection is already established.
    */
   protected abstract void afterConnect();

   /**
    * Setup specific handlers to establish either HTTP1 or HTTP2 connection.
    */
   protected abstract void beforeConnect();

   private void setupOperationsFactory() {
      switch (type) {
         case HTTP_2:
            this.operationsFactory = new Http2OperationsFactory(channel, syncRequests);
            break;
         default:
            this.operationsFactory = new HttpOperationsFactory(channel, syncRequests);
            break;
      }
   }

   /**
    * Switch to new operations type.
    * @param type
     */
   protected void switchToNewType(OperationType type) {
      if (type == null || this.type == type) {
         return;
      }

      this.type = type;
      switch (this.type) {
         case HTTP_1:
            channel.pipeline().remove(Http2ResponseHandler.class.getName());
            channel.pipeline().addLast(HttpResponseHandler.class.getName(), new HttpResponseHandler(syncRequests));
            break;
      }
   }

   public void stop() {
      workerGroup.shutdownGracefully();
   }

    /**
     * Write info to the connected server.
     * @param cacheName
     * @param key
     * @param value
     */
   public void write(Object cacheName, Object key, Object value) {
      HttpResponse response = operationsFactory.putRequest(transportFactory.getTopologyId(), cacheName, key, value);
      if (response != null && HttpResponseStatus.OK.equals(response.status())) {
         checkTopologyId(response);
      }
   }

    /**
     * Read info from the connected server.
     * @param cacheName
     * @param key
     * @return
     */
   public Object read(Object cacheName, Object key) {
      byte[] data = null;

      FullHttpResponse response = operationsFactory.getRequest(transportFactory.getTopologyId(), cacheName, key);
      if (response != null && HttpResponseStatus.OK.equals(response.status())) {
         ByteBuf content = response.content();
         data = new byte[content.readableBytes()];
         content.readBytes(data);
         response.release();
         checkTopologyId(response);
      }

      return data != null ? MarshallUtil.byteArray2Object(data) : null;
   }

   protected void checkTopologyId(HttpResponse response) {
      String retrievedTopologyInfo = response.headers().getAsString(HttpHeaderNames.TOPOLOGY_ID);
      if (retrievedTopologyInfo == null) {
         return;
      }
      String[] topology = retrievedTopologyInfo.split(";");
      Integer retrievedTopologyId = Integer.valueOf(topology[0].split(":")[1]);
      if (retrievedTopologyId != null && !retrievedTopologyId.equals(transportFactory.getTopologyId())) {
         String[] retrievedServers = topology[1].split(",");
         List<ServerConfiguration> servers = new LinkedList<ServerConfiguration>();
         for (String retrievedServer : retrievedServers) {
            String[] server = retrievedServer.split(":");
            servers.add(new ServerConfiguration(server[0], server[1]));
         }
         transportFactory.updateTopologyId(retrievedTopologyId, servers);
      }
   }

}
