/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.kernel.http;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;

public class DigestPerformanceTest {
  private static final Logger logger = LoggerFactory.getLogger(DigestPerformanceTest.class);

  /**
   * Verifying that running an md5 on a classpath resource is quick enough to do on each request. With a mean of around
   * one millisecond to md5 a large javascript library, this seems like an acceptable cost to support ETags. Testing
   * concurrent md5 hashing is difficult since it seems to run so quickly, but 100 more-or-less concurrent threads still
   * seem to perform OK.
   *
   * @throws Exception
   */
  @Test
  public void testSingleThreadedMd5Digest() throws Exception {
    URL url = getClass().getClassLoader().getResource("InfusionAll.js");
    InputStream in = url.openStream();
    Statistics stats = new Statistics();
    long count = 100;
    for (int i = 0; i < count; i++) {
      long start = System.currentTimeMillis();
      DigestUtils.md5Hex(in);
      long elapsed = (System.currentTimeMillis() - start);
      if (elapsed > stats.getMax())
        stats.setMax(elapsed);
      if (elapsed < stats.getMin())
        stats.setMin(elapsed);
      stats.setTotalElapsed(stats.getTotalElapsed() + elapsed);
    }
    logger.info("One thread running {} md5 hashes of InfusionAll.js took min {}ms, max {}ms, mean {}ms", new Long[] {
            count, stats.getMin(), stats.getMax(), (stats.getTotalElapsed() / count) });
  }

  @Test
  public void testMultiThreadedMd5Digest() throws Exception {
    final Statistics stats = new Statistics();
    long count = 100;
    for (int i = 0; i < count; i++) {
      new Thread(new Runnable() {
        public void run() {
          stats.addThread();
          try {
            URL url = getClass().getClassLoader().getResource("InfusionAll.js");
            InputStream in = url.openStream();
            long start = System.currentTimeMillis();
            DigestUtils.md5Hex(in);
            long elapsed = (System.currentTimeMillis() - start);
            if (elapsed > stats.getMax())
              stats.setMax(elapsed);
            if (elapsed < stats.getMin())
              stats.setMin(elapsed);
            stats.setTotalElapsed(stats.getTotalElapsed() + elapsed);
          } catch (Exception e) {
            throw new RuntimeException(e);
          } finally {
            stats.removeThread();
          }
        }
      }).start();
    }
    logger.info(
            "{} threads running ({} max concurrent) md5 hashes of InfusionAll.js took min {}ms, max {}ms, mean {}ms",
            new Long[] { count, stats.getMaxConcurrentThreads(), stats.getMin(), stats.getMax(),
                    (stats.getTotalElapsed() / count) });
  }
}

class Statistics {
  private long min = Long.MAX_VALUE;
  private long max = Long.MIN_VALUE;
  private long totalElapsed = 0;
  private long maxConcurrentThreads = 0;
  private long concurrentThreads = 0;

  long getMin() {
    return min;
  }

  long getMax() {
    return max;
  }

  long getTotalElapsed() {
    return totalElapsed;
  }

  void setMin(long min) {
    this.min = min;
  }

  void setMax(long max) {
    this.max = max;
  }

  void setTotalElapsed(long total) {
    this.totalElapsed = total;
  }

  long getMaxConcurrentThreads() {
    return maxConcurrentThreads;
  }

  void addThread() {
    this.concurrentThreads++;
    if (this.concurrentThreads > maxConcurrentThreads)
      maxConcurrentThreads = concurrentThreads;
  }

  void removeThread() {
    this.concurrentThreads--;
  }
}
