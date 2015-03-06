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
package org.opencastproject.util;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.util.data.Function;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Ignore
public class MultiResourceLockTest {
  private AtomicInteger taskCounter;
  private HashMap<Long, Double> data;
  private volatile boolean multipleIdsInParallel;
  private MultiResourceLock lock;

  @Before
  public void setUp() {
    taskCounter = new AtomicInteger();
    data = new HashMap<>();
    multipleIdsInParallel = false;
    lock = new MultiResourceLock();
  }

  @Test(expected = java.lang.AssertionError.class)
  public void testDetectNoParallelExecution() throws Exception {
    // Create tasks with just one id so no parallel execution can happen.
    // This will cause the parallel execution assertion to fail.
    testSynchronize(Executors.newFixedThreadPool(10), 15, 1, 100, 100);
  }

  @Test
  public void testSynchronize() throws Exception {
    testSynchronize(Executors.newFixedThreadPool(10), 20, 2, 0, 0);
    testSynchronize(Executors.newFixedThreadPool(10), 5, 2, 100, 500);
    testSynchronize(Executors.newFixedThreadPool(2), 20, 3, 200, 1000);
    testSynchronize(Executors.newScheduledThreadPool(10), 5, 2, 100, 100);
    testSynchronize(Executors.newScheduledThreadPool(3), 200, 5, 700, 1000);
  }

  private void testSynchronize(ExecutorService pool, int taskCount, final int idCount, final int minSleep,
          final int maxSleep) throws Exception {
    final Random random = new Random(System.nanoTime());
    for (int i = 0; i < taskCount; i++) {
      taskCounter.incrementAndGet();
      final String taskName = "Task " + i;
      sleep((long) (Math.random() * (maxSleep - minSleep) + minSleep));
      pool.execute(new Runnable() {
        @Override
        public void run() {
          final long id = random.nextInt(idCount);
          lock.synchronize(id, task(taskName, minSleep, maxSleep));
        }
      });
    }
    pool.shutdown();
    pool.awaitTermination(1, TimeUnit.DAYS);
    assertEquals("Some task haven't been executed", 0, taskCounter.get());
    assertEquals("Not all lock objects have been removed", 0, lock.getLockMapSize());
    assertTrue("No parallel execution of threads with different IDs", multipleIdsInParallel);
  }

  private Function<Long, Void> task(final String taskName, final long minSleep, final long maxSleep) {
    return new Function<Long, Void>() {
      @Override
      public Void apply(Long id) {
        final Double value = Math.random();
        data.put(id, value);
        final Map<Long, Double> dataCopy = new HashMap<>(data);
        sleep((long) (Math.random() * (maxSleep - minSleep) + minSleep));
        long seconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
        System.out.println(format("%s, id: %s, time %s", taskName, id, seconds));
        assertEquals("Concurrent modification of data for id " + id, value, data.get(id));
        taskCounter.decrementAndGet();
        multipleIdsInParallel = multipleIdsInParallel || !dataCopy.equals(data);
        return null;
      }
    };
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
