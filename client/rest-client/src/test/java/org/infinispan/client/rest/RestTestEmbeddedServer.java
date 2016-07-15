package org.infinispan.client.rest;

import static org.testng.AssertJUnit.assertTrue;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.rest.RestServerTestBase;
import org.infinispan.rest.configuration.RestServerConfigurationBuilder;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(testName = "rest.client.RestTestEmbeddedServer")
public class RestTestEmbeddedServer extends RestServerTestBase {
   
   private static final String DEFAULT_CACHE_NAME = "default";
   private static RestCacheManager mng;

   @BeforeClass
   public void setup() throws Exception {      
      setupServers();
      setupClient();
   }
   
   @AfterClass 
   public void finish() throws Exception {
      stopServers();
      stopClient();
   }
   
   private void setupServers() throws Exception {
      EmbeddedCacheManager cm = TestCacheManagerFactory.createCacheManager();
      addServer("0", cm, new RestServerConfigurationBuilder().port(8080).build());
      cm.getCache(DEFAULT_CACHE_NAME);
      startServers();
   }

   private void setupClient() {
      mng = new RestCacheManager();
   }

   private void stopClient() {
      mng.stop();
   }

   public void testGetAndPutRqsts() {
      String key1 = "Test1";
      String key2 = "Test2";
      String value1 = "Cool";
      String value2 = "Story";
      
      mng.getCache().put(key1, value1);
      mng.getCache().put(key2, value2);

      assertTrue(value1.equals((String) mng.getCache().get(key1)));
      assertTrue(value2.equals((String) mng.getCache().get(key2)));
   }
}
