package org.infinispan.commands.write;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.LocalCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.container.InternalEntryFactory;
import org.infinispan.context.InvocationContext;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Collections;

/**
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public class EvictCommand extends RemoveCommand implements LocalCommand {

   private static final Log log = LogFactory.getLog(EvictCommand.class);
   private static final boolean trace = log.isTraceEnabled();

   private final InternalEntryFactory factory;

   public EvictCommand(Object key, CacheNotifier notifier, long flagsBitSet, CommandInvocationId commandInvocationId,
                       InternalEntryFactory factory) {
      super(key, null, notifier, flagsBitSet, null, commandInvocationId);
      this.factory = factory;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitEvictCommand(ctx, this);
   }

   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      if (key == null) {
         throw new NullPointerException("Key is null!!");
      }
      super.perform(ctx);
      return null;
   }

   @Override
   public void notify(InvocationContext ctx, Object value, Metadata previousMetadata, 
         boolean isPre) {
      // Eviction has no notion of pre/post event since 4.2.0.ALPHA4.
      // EvictionManagerImpl.onEntryEviction() triggers both pre and post events
      // with non-null values, so we should do the same here as an ugly workaround.
      if (!isPre) {
         if (trace)
            log.tracef("Notify eviction listeners for key=%s", key);

         notifier.notifyCacheEntriesEvicted(Collections.singleton(factory.create(key, value, previousMetadata)), ctx,
                 this);
      }
   }

   @Override
   public byte getCommandId() {
      return -1; // these are not meant for replication!
   }
   
   @Override
   public String toString() {
      return new StringBuilder()
         .append("EvictCommand{key=")
         .append(key)
         .append(", value=").append(value)
         .append(", flags=").append(printFlags())
         .append("}")
         .toString();
   }

   @Override
   public boolean readsExistingValues() {
      return false;
   }

}
