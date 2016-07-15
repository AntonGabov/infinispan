package org.infinispan.client.rest.impl.transport.http;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.client.rest.configuration.Configuration;
import org.infinispan.client.rest.configuration.ServerConfiguration;
import org.infinispan.client.rest.impl.TopologyInfo;
import org.infinispan.client.rest.impl.protocol.HttpHeaderNames;
import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.impl.transport.operations.HttpOperationsFactory;
import org.infinispan.client.rest.marshall.MarshallUtil;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpTransport implements Transport {

   //private static final Log log = LogFactory.getLog(HttpTransport.class);

   private static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

   private Configuration configuration;
   private List<ServerConfiguration> initialServers = new LinkedList<ServerConfiguration>();
   private volatile TopologyInfo topologyInfo;
   private HttpOperationsFactory operations;

   @Override
   public void start(Configuration configuration, int initialTopologyId) {
      this.configuration = configuration;
      initialServers.addAll(configuration.servers());
      topologyInfo = new TopologyInfo(initialTopologyId, initialServers);
      setupOperations();
   }

   private void setupOperations() {
      operations = new HttpOperationsFactory();
      operations.start();
   }

   @Override
   public void stop() {
      operations.stop();
   }

   @Override
   public void write(Object cacheName, Object key, Object value) {
      write(initialServers.get(0), cacheName, key, value);
   }

   private void write(ServerConfiguration server, Object cacheName, Object key, Object value) {
      HttpResponse response = operations.putRequest(topologyInfo, server, cacheName, key, value);
      if (response != null && HttpResponseStatus.OK.equals(response.status())) {
         checkTopologyId(response);
      }
   }

   @Override
   public Object read(Object cacheName, Object key) {
      byte[] readInfo = read(initialServers.get(0), cacheName, key);
      if (readInfo != null) {
         return MarshallUtil.byteArray2Object(readInfo);
      }

      return null;
   }

   private byte[] read(ServerConfiguration server, Object cacheName, Object key) {
      byte[] data = null;

      FullHttpResponse response = (FullHttpResponse) operations.getRequest(topologyInfo, server, cacheName, key, MAX_CONTENT_LENGTH);

      if (response != null && HttpResponseStatus.OK.equals(response.status())) {
         ByteBuf content = response.content();
         data = new byte[content.readableBytes()];
         content.readBytes(data);
         response.release();
         checkTopologyId(response);
      }

      return data;
   }

    /**
     * Check that topology was changed.
     * @param response
     */
   private void checkTopologyId(HttpResponse response) {
      String retrievedTopologyInfo = response.headers().getAsString(HttpHeaderNames.TOPOLOGY_ID);
      if (retrievedTopologyInfo == null) {
         return;
      }
      String[] topology = retrievedTopologyInfo.split(";");
      Integer retrievedTopologyId = Integer.valueOf(topology[0].split(":")[1]);
      if (retrievedTopologyId != null && !retrievedTopologyId.equals(topologyInfo.getTopologyId())) {
         String[] retrievedServers = topology[1].split(",");
         List<ServerConfiguration> servers = new LinkedList<ServerConfiguration>();
         for (String retrievedServer : retrievedServers) {
            String[] server = retrievedServer.split(":");
            servers.add(new ServerConfiguration(server[0], server[1]));
         }
         topologyInfo = topologyInfo.updateTopologyId(retrievedTopologyId, servers);
      }
   }

}
