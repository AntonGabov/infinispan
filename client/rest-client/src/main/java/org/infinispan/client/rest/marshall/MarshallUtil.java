package org.infinispan.client.rest.marshall;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

public class MarshallUtil {
   
   //private static final Log log = LogFactory.getLog(MarshallUtil.class);
   
   public static byte[] obj2byteArray(Object value) {
      byte[] data = null;
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(bos);) {
         oos.writeObject(value);
         oos.flush();
         data = bos.toByteArray();
      } catch (IOException e) {
         //log.warn("Cannot convert to byte array");
      }
      return data;
   }
   
   public static Object byteArray2Object(byte[] value) {
      Object data = null;
      try (ByteArrayInputStream bais = new ByteArrayInputStream(value);
         ObjectInputStream ois = new ObjectInputStream(bais);) {
         data = ois.readObject();
      } catch (IOException | ClassNotFoundException e) {
         //log.warn("Cannot convert to Object");
      }
      return data;
   }
}
