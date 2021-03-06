package org.infinispan.objectfilter.impl.util;

import java.util.Arrays;

/**
 * @author anistor@redhat.com
 * @since 7.0
 */
public final class StringHelper {

   private StringHelper() {
   }

   public static String join(String[] array) {
      return join(Arrays.asList(array));
   }

   public static String join(Iterable<String> iterable) {
      return String.join(".", iterable);
   }

   public static String[] split(String propertyPath) {
      return propertyPath.split("[.]");
   }
}
