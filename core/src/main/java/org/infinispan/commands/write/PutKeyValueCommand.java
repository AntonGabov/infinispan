package org.infinispan.commands.write;

import org.infinispan.atomic.CopyableDeltaAware;
import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commons.equivalence.Equivalence;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.metadata.Metadata;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commands.MetadataAwareCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.metadata.Metadatas;
import org.infinispan.notifications.cachelistener.CacheNotifier;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static org.infinispan.commons.util.Util.toStr;

/**
 * Implements functionality defined by {@link org.infinispan.Cache#put(Object, Object)}
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public class PutKeyValueCommand extends AbstractDataWriteCommand implements MetadataAwareCommand {

   public static final byte COMMAND_ID = 8;

   Object value;
   boolean putIfAbsent;
   CacheNotifier notifier;
   boolean successful = true;
   Metadata metadata;
   private ValueMatcher valueMatcher;
   private Equivalence valueEquivalence;

   public PutKeyValueCommand() {
   }

   public PutKeyValueCommand(Object key, Object value, boolean putIfAbsent,
                             CacheNotifier notifier, Metadata metadata, long flagsBitSet,
                             Equivalence valueEquivalence, CommandInvocationId commandInvocationId) {
      super(key, flagsBitSet, commandInvocationId);
      this.value = value;
      this.putIfAbsent = putIfAbsent;
      this.valueMatcher = putIfAbsent ? ValueMatcher.MATCH_EXPECTED : ValueMatcher.MATCH_ALWAYS;
      this.notifier = notifier;
      this.metadata = metadata;
      this.valueEquivalence = valueEquivalence;

      if (value instanceof DeltaAware) {
         addFlag(Flag.DELTA_WRITE);
      }
   }

   public void init(CacheNotifier notifier, Configuration cfg) {
      this.notifier = notifier;
      this.valueEquivalence = cfg.dataContainer().valueEquivalence();
   }

   public Object getValue() {
      return value;
   }

   public void setValue(Object value) {
      this.value = value;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitPutKeyValueCommand(ctx, this);
   }

   @Override
   public boolean readsExistingValues() {
      return putIfAbsent || !hasFlag(Flag.IGNORE_RETURN_VALUES);
   }

   @Override
   public boolean alwaysReadsExistingValues() {
      return hasFlag(Flag.DELTA_WRITE);
   }

   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      // It's not worth looking up the entry if we're never going to apply the change.
      if (valueMatcher == ValueMatcher.MATCH_NEVER) {
         successful = false;
         return null;
      }
      MVCCEntry e = (MVCCEntry) ctx.lookupEntry(key);

      //possible as in certain situations (e.g. when locking delegation is used) we don't wrap
      if (e == null) return null;

      Object entryValue = e.getValue();
      if (!valueMatcher.matches(e, null, value, valueEquivalence)) {
         successful = false;
         return entryValue;
      }

      return performPut(e, ctx);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public void writeTo(ObjectOutput output) throws IOException {
      output.writeObject(key);
      output.writeObject(value);
      output.writeObject(metadata);
      MarshallUtil.marshallEnum(valueMatcher, output);
      CommandInvocationId.writeTo(output, commandInvocationId);
      output.writeLong(Flag.copyWithoutRemotableFlags(getFlagsBitSet()));
      output.writeBoolean(putIfAbsent);
   }

   @Override
   public void readFrom(ObjectInput input) throws IOException, ClassNotFoundException {
      key = input.readObject();
      value = input.readObject();
      metadata = (Metadata) input.readObject();
      valueMatcher = MarshallUtil.unmarshallEnum(input, ValueMatcher::valueOf);
      commandInvocationId = CommandInvocationId.readFrom(input);
      setFlagsBitSet(input.readLong());
      putIfAbsent = input.readBoolean();
   }

   @Override
   public Metadata getMetadata() {
      return metadata;
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = metadata;
   }

   public boolean isPutIfAbsent() {
      return putIfAbsent;
   }

   public void setPutIfAbsent(boolean putIfAbsent) {
      this.putIfAbsent = putIfAbsent;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      PutKeyValueCommand that = (PutKeyValueCommand) o;

      if (putIfAbsent != that.putIfAbsent) return false;
      if (value != null ? !value.equals(that.value) : that.value != null) return false;
      if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (value != null ? value.hashCode() : 0);
      result = 31 * result + (putIfAbsent ? 1 : 0);
      result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return new StringBuilder()
            .append("PutKeyValueCommand{key=")
            .append(toStr(key))
            .append(", value=").append(toStr(value))
            .append(", flags=").append(printFlags())
            .append(", putIfAbsent=").append(putIfAbsent)
            .append(", valueMatcher=").append(valueMatcher)
            .append(", metadata=").append(metadata)
            .append(", successful=").append(successful)
            .append("}")
            .toString();
   }

   @Override
   public boolean isSuccessful() {
      return successful;
   }

   @Override
   public boolean isConditional() {
      return putIfAbsent;
   }

   @Override
   public ValueMatcher getValueMatcher() {
      return valueMatcher;
   }

   @Override
   public void setValueMatcher(ValueMatcher valueMatcher) {
      this.valueMatcher = valueMatcher;
   }

   @Override
   public void updateStatusFromRemoteResponse(Object remoteResponse) {
      // Only putIfAbsent commands can fail
      if (putIfAbsent) {
         successful = remoteResponse == null;
      }
   }

   private Object performPut(MVCCEntry e, InvocationContext ctx) {
      Object entryValue = e.getValue();
      Object o;

      if (e.isCreated()) {
         notifier.notifyCacheEntryCreated(key, value, metadata, true, ctx, 
                                           this);
      } else {
         notifier.notifyCacheEntryModified(key, value, metadata, entryValue, e.getMetadata(), true, ctx, 
                                           this);
      }

      if (value instanceof Delta) {
         // magic
         Delta dv = (Delta) value;
         if (e.isRemoved()) {
            e.setExpired(false);
            e.setRemoved(false);
            e.setCreated(true);
            e.setValid(true);
            e.setValue(dv.merge(null));
            Metadatas.updateMetadata(e, metadata);
         } else {
            DeltaAware toMergeWith = null;
            if (entryValue instanceof CopyableDeltaAware) {
               toMergeWith = ((CopyableDeltaAware) entryValue).copy();
            } else if (entryValue instanceof DeltaAware) {
               toMergeWith = (DeltaAware) entryValue;
            }
            e.setValue(dv.merge(toMergeWith));
            Metadatas.updateMetadata(e, metadata);
         }
         o = entryValue;
      } else {
         o = e.setValue(value);
         Metadatas.updateMetadata(e, metadata);
         if (e.isRemoved()) {
            e.setCreated(true);
            e.setExpired(false);
            e.setRemoved(false);
            e.setValid(true);
            o = null;
         }
      }
      e.setChanged(true);
      // Return the expected value when retrying a putIfAbsent command (i.e. null)
      return valueMatcher != ValueMatcher.MATCH_EXPECTED_OR_NEW ? o : null;
   }
}
