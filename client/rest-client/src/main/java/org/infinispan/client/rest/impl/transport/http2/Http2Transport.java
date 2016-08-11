package org.infinispan.client.rest.impl.transport.http2;

import io.netty.handler.codec.http2.Http2Settings;
import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.operations.OperationsConstants;

import java.util.concurrent.TimeUnit;

public class Http2Transport extends Transport {

   private Http2ChannelInitializer initializer;

   @Override
   protected void beforeConnect() {
      initializer = new Http2ChannelInitializer();
      bootstrap.handler(initializer);
   }

   @Override
   protected void afterConnect() {
      try {
         initializer.settingsHandler().awaitSettings(5, TimeUnit.SECONDS);
      } catch (IllegalStateException e) {
         super.switchToNewType(OperationsConstants.OperationType.HTTP_1);
      }
   }
}
