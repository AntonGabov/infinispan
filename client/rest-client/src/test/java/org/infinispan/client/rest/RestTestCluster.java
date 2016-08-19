package org.infinispan.client.rest;

import org.infinispan.client.rest.impl.TopologyInfo;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.rest.RestServerTestBase;
import org.infinispan.rest.configuration.RestServerConfigurationBuilder;
import org.infinispan.test.AbstractCacheTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

@Test(testName = "rest.client.RestServerTestBase")
public class RestTestCluster extends RestServerTestBase {

   private static final String DEFAULT_CACHE_NAME = "default";
   private static final String HOST = "localhost";
   private static final String PORT_1 = "8080";
   private static final String PORT_2 = "8181";
   private static RestCacheManager mng1;
   private static RestCacheManager mng2;

   @BeforeClass
   public void setup() throws Exception {
      ConfigurationBuilder cfgBuilder = AbstractCacheTest.getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, true);

      EmbeddedCacheManager cm1 = TestCacheManagerFactory.createClusteredCacheManager(cfgBuilder);
      addServer("1", cm1, new RestServerConfigurationBuilder().port(Integer.valueOf(PORT_1)).build());

      EmbeddedCacheManager cm2 = TestCacheManagerFactory.createClusteredCacheManager(cfgBuilder);
      addServer("2", cm2, new RestServerConfigurationBuilder().port(Integer.valueOf(PORT_2)).build());

      startServers();
      TestingUtil.blockUntilViewsReceived(10000, getCacheManager("1").getCache(DEFAULT_CACHE_NAME),
              getCacheManager("2").getCache(DEFAULT_CACHE_NAME));

      setupClients();
   }

   @AfterClass
   public void finish() throws Exception {
      stopServers();
      stopClients();
   }

   private void setupClients() {
      mng1 = new RestCacheManager();
      org.infinispan.client.rest.configuration.ConfigurationBuilder builder = new org.infinispan.client.rest.configuration.ConfigurationBuilder();
      builder.addServer(HOST, PORT_2);
      mng2 = new RestCacheManager();
      mng2.defineConfiguration(builder.create());

      mng1.start();
      mng2.start();
   }

   private void stopClients() {
      mng1.stop();
      mng2.stop();
   }

   public void testGetAndPutRequests() throws Exception {
      String key1 = "Test1";
      String key2 = "Test2";
      String value1 = "Cool";
      String value2 = "Story";

      mng1.getCache().put(key1, value1);
      mng1.getCache().put(key2, value2);

      assertEquals((String) mng2.getCache().get(key1), value1);
      assertEquals((String) mng2.getCache().get(key2), value2);
   }

   public void testTopologyInfo() throws Exception {
      String key1 = "TestTopology1";
      String key2 = "TestTopology2";
      String value1 = "Summer";
      String value2 = "Google";
      String server1 = HOST + ":" + PORT_1;
      String server2 = HOST + ":" + PORT_2;

      mng1.getCache().put(key1, value1);
      mng2.getCache().put(key2, value2);
      TopologyInfo topologyInfo = TestingUtil.extractField(
            TestingUtil.extractField(mng1, "transportFactory"), "topologyInfo");
      String expectedTopologyInfo = retrieveTopologyInfo(topologyInfo);

      assertTrue(expectedTopologyInfo.contains(server1));
      assertTrue(expectedTopologyInfo.contains(server2));
   }

   private String retrieveTopologyInfo(TopologyInfo topologyInfo) {
      StringBuilder servers = new StringBuilder();
      topologyInfo.servers().forEach(server -> {
         if (servers.length() > 0) {
            servers.append(",");
         }
         servers.append(server.getHost()).append(":").append(server.getPort());
      });
      return servers.toString();
   }

}
