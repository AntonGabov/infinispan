package org.infinispan.client.rest;

import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.rest.RestServerTestBase;
import org.infinispan.rest.configuration.RestServerConfigurationBuilder;
import org.infinispan.test.AbstractCacheTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertTrue;

@Test(testName = "rest.client.RestServerTestBase")
public class RestTestCluster extends RestServerTestBase {

   private static final String DEFAULT_CACHE_NAME = "default";
   private static RestCacheManager mng;

   @BeforeClass
   public void setup() throws Exception {
      ConfigurationBuilder cfgBuilder = AbstractCacheTest.getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, true);

      EmbeddedCacheManager cm1 = TestCacheManagerFactory.createClusteredCacheManager(cfgBuilder);
      addServer("1", cm1, new RestServerConfigurationBuilder().port(8080).build());

      EmbeddedCacheManager cm2 = TestCacheManagerFactory.createClusteredCacheManager(cfgBuilder);
      addServer("2", cm2, new RestServerConfigurationBuilder().port(8181).build());

      startServers();
      TestingUtil.blockUntilViewsReceived(10000, getCacheManager("1").getCache(DEFAULT_CACHE_NAME),
              getCacheManager("2").getCache(DEFAULT_CACHE_NAME));

      setupClient();
   }

   @AfterClass
   public void finish() throws Exception {
      stopServers();
      stopClient();
   }

   private void setupClient() {
      mng = new RestCacheManager();
   }

   private void stopClient() {
      mng.stop();
   }

   public void test2nodeCluster() throws Exception {
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
