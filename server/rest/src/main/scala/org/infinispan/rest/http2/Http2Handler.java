package org.infinispan.rest.http2;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.AsciiString;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class Http2Handler extends Http2ConnectionHandler implements Http2FrameListener {

   private static final String TOPOLOGY_HEADER = "Topology-Id";
   /** The path is "/rest/{cacheName}/{cacheKey}" */
   private static final int MAX_ELEMENTS_IN_PATH = 3;
   private EmbeddedCacheManager cacheManager;
   private Cache<Address, String> addressCache;
   private Map<Integer, Http2Headers> headersFromRequest;

   protected Http2Handler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings,
                          EmbeddedCacheManager cacheManager, Cache addressCache) {
      super(decoder, encoder, initialSettings);
      this.cacheManager = cacheManager;
      this.addressCache = addressCache;
      headersFromRequest = new ConcurrentHashMap<Integer, Http2Headers>();
   }

   @Override
   public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent) {
         Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText())
               .set(new AsciiString("http-to-http2-upgrade"), new AsciiString("true"));
         encoder().writeHeaders(ctx, 1, headers, 0, true, ctx.newPromise());
      }
      super.userEventTriggered(ctx, evt);
   }

   @Override
   public int onDataRead(ChannelHandlerContext ctx, int streamId,
                         ByteBuf data, int padding, boolean endOfStream)  {
      int processed = data.readableBytes() + padding;
      if (endOfStream && isNeedToPutInCache(headersFromRequest.get(streamId))) {
         putValueInCache(data.retain(), streamId);
         sendResponse(ctx, streamId, null);
      }
      return processed;
   }

   @Override
   public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                             Http2Headers headers, int padding, boolean endOfStream) {
      if (isNeedToPutInCache(headers) || isNeedToGetFromCache(headers)) {
         addHeadersFromRequest(streamId, headers);
      }

      if (endOfStream && isNeedToGetFromCache(headers)) {
         sendResponse(ctx, streamId, retrieveValueFromCache(ctx, streamId));
      }
   }

   @Override
   public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
                       short weight, boolean exclusive, int padding, boolean endOfStream) {
      onHeadersRead(ctx, streamId, headers, padding, endOfStream);
   }

   @Override
   public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency,
                        short weight, boolean exclusive) {
   }

   @Override
   public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
   }

   @Override
   public void onSettingsAckRead(ChannelHandlerContext ctx) {
   }

   @Override
   public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
   }

   @Override
   public void onPingRead(ChannelHandlerContext ctx, ByteBuf data) {
   }

   @Override
   public void onPingAckRead(ChannelHandlerContext ctx, ByteBuf data) {
   }

   @Override
   public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId,
                          Http2Headers headers, int padding) {
   }

   @Override
   public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) {
   }

   @Override
   public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) {
   }

   @Override
   public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId,
                        Http2Flags flags, ByteBuf payload) {
   }

   /**
    * Send a frame for the response status
    * @param ctx
    * @param streamId
    * @param payload
    */
   private void sendResponse(ChannelHandlerContext ctx, int streamId, ByteBuf payload) {
      Http2Headers headersRequest = headersFromRequest.remove(streamId);
      Http2Headers headersResponse = new DefaultHttp2Headers().status(OK.codeAsText());
      addHeadersToResponse(headersRequest, headersResponse);
      encoder().writeHeaders(ctx, streamId, headersResponse, 0, false, ctx.newPromise());
      encoder().writeData(ctx, streamId, payload == null ?
              ctx.alloc().buffer() :
              payload, 0, true, ctx.newPromise());
      ctx.flush();
   }

   /**
    * Retrieve cacheValue from cache.
    *
    * @param ctx
    * @param streamId
    * @return
    */
   private ByteBuf retrieveValueFromCache(ChannelHandlerContext ctx, int streamId) {
      String[] pathValues = getPathValues(streamId);

      if (pathValues == null) {
         return null;
      }

      byte[] cacheValue = (byte[]) cacheManager.getCache(pathValues[1]).get(pathValues[2]);
      ByteBuf byteBuf;
      if (cacheValue == null) {
         byteBuf = null;
      } else {
         byteBuf = ctx.alloc().buffer().writeBytes(cacheValue);
      }
      return byteBuf;
   }

   private void putValueInCache(ByteBuf data, int streamId) {
      String[] pathValues = getPathValues(streamId);

      if (pathValues == null) {
         return;
      }

      byte[] cacheValue = new byte[data.readableBytes()];
      data.readBytes(cacheValue);
      cacheManager.getCache(pathValues[1]).put(pathValues[2], cacheValue);
   }

   /**
    * Get path values from headers as array:
    * <ul>
    *     <li>[0] - "rest" </li>
    *     <li>[1] - cacheName </li>
    *     <li>[2] - cacheKey </li>
    * </ul>
    * @param streamId
    * @return
     */
   private String[] getPathValues(int streamId) {
      Http2Headers headers = headersFromRequest.get(streamId);
      String path = headers.path().toString();
      // Get rid of the first '/' in the path
      String[] pathValues = path.replaceFirst("/", "").split("/");

      if (pathValues.length != MAX_ELEMENTS_IN_PATH) {
         return null;
      } else {
         return pathValues;
      }
   }

   private boolean isNeedToGetFromCache(Http2Headers headers) {
      return HttpMethod.GET.name().equals(headers.method().toString());
   }

   private boolean isNeedToPutInCache(Http2Headers headers) {
      return HttpMethod.POST.name().equals(headers.method().toString())
              || HttpMethod.PUT.name().equals(headers.method().toString());
   }

   private void addHeadersFromRequest(int streamId, Http2Headers headersToAdd) {
      Http2Headers headers = headersFromRequest.get(streamId);
      if (headers == null) {
         headersFromRequest.put(streamId, headersToAdd);
      } else {
         Iterator<Entry<CharSequence, CharSequence>> headersIterator = headersToAdd.iterator();
         while(headersIterator.hasNext()) {
            Entry<CharSequence, CharSequence> header = headersIterator.next();
            headers.add(header.getKey(), header.getValue());
         }
      }
   }

   private void addHeadersToResponse(Http2Headers headersRequest, Http2Headers headersResponse) {
      addTopologyIdHeader(headersRequest, headersResponse);
   }

   private void addTopologyIdHeader(Http2Headers headersRequest, Http2Headers headersResponse) {
      String topologyIdAsString = (String) headersRequest.get(TOPOLOGY_HEADER);
      if (topologyIdAsString == null) {
         return;
      }

      Integer topologyId = Integer.valueOf(topologyIdAsString);
      Integer currentTopologyId = addressCache == null ? -1 :
            addressCache.getAdvancedCache().getComponentRegistry().getStateTransferManager().
            getCacheTopology().getTopologyId();

      if (!topologyId.equals(currentTopologyId)) {
         headersResponse.add(TOPOLOGY_HEADER, formTopologyInfo(currentTopologyId));
      }

   }

   private String formTopologyInfo(Integer topologyId) {
      String strTopologyId = "Topology:" + topologyId;
      StringBuilder strAddressesBuilder = new StringBuilder();
      addressCache.values().forEach(address -> {
         if (strAddressesBuilder.length() > 0) {
            strAddressesBuilder.append(",");
         }
         strAddressesBuilder.append(address);
      });

      return strTopologyId + ";" + strAddressesBuilder.toString();
   }

}
