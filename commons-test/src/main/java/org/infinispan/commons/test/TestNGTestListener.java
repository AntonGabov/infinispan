package org.infinispan.commons.test;

import org.jboss.logging.Logger;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Logs TestNG test progress.
 */
public class TestNGTestListener implements ITestListener, IInvokedMethodListener, ISuiteListener {
   private static final Logger log = Logger.getLogger(TestNGTestListener.class);
   private Set<String> startupThreads;

   @Override
   public void onTestStart(ITestResult result) {
      TestSuiteProgress.testStarted(testName(result));
   }

   @Override
   public void onTestSuccess(ITestResult result) {
      TestSuiteProgress.testFinished(testName(result));
   }

   @Override
   public void onTestFailure(ITestResult result) {
      TestSuiteProgress.testFailed(testName(result), result.getThrowable());
   }

   @Override
   public void onTestSkipped(ITestResult result) {
      TestSuiteProgress.testIgnored(testName(result));
   }

   @Override
   public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
      TestSuiteProgress.testFailed(testName(result), result.getThrowable());
   }

   @Override
   public void onStart(ITestContext context) {
   }

   @Override
   public void onFinish(ITestContext context) {
   }

   private String testName(ITestResult res) {
      return res.getTestClass().getName() + "." + res.getMethod().getMethodName();
   }

   @Override
   public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
   }

   @Override
   public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
      if (testResult.getThrowable() != null && method.isConfigurationMethod()) {
         TestSuiteProgress.setupFailed(testName(testResult), testResult.getThrowable());
      }
   }

   @Override
   public void onStart(ISuite isuite) {
      Set<String> threads = new HashSet<String>();
      for (Map.Entry<Thread, StackTraceElement[]> s : Thread.getAllStackTraces().entrySet()) {
         Thread thread = s.getKey();
         if (!thread.getName().startsWith("TestNG")) {
            threads.add(thread.getName() + "@" + thread.getId());
         }
      }
      startupThreads = threads;
   }

   @Override
   public void onFinish(ISuite isuite) {
      int count = 0;
      for (Map.Entry<Thread, StackTraceElement[]> s : Thread.getAllStackTraces().entrySet()) {
         Thread thread = s.getKey();
         if (ignoreThread(thread))
            continue;

         if (count == 0) {
            log.warn("Possible leaked threads at the end of the test suite:");
         }
         count++;
         // "management I/O-2" #55 prio=5 os_prio=0 tid=0x00007fe6a8134000 nid=0x7f9d runnable
         // [0x00007fe64e4db000]
         //    java.lang.Thread.State:RUNNABLE
         log.warnf("\"%s\" #%d %sprio=%d tid=0x%x nid=NA %s", thread.getName(), count,
               thread.isDaemon() ? "daemon " : "", thread.getPriority(), thread.getId(),
               thread.getState().toString().toLowerCase());
         log.warnf("   java.lang.Thread.State: %s", thread.getState());
         for (StackTraceElement ste : s.getValue()) {
            log.warnf("\t%s", ste);
         }
      }
   }

   private boolean ignoreThread(Thread thread) {
      String threadName = thread.getName();
      return threadName.startsWith("testng-") || startupThreads.contains(threadName + "@" + thread.getId());
   }
}
