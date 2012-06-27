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
package org.opencastproject.remotetest;

import org.junit.runners.Parameterized;
import org.junit.runners.model.RunnerScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A junit runner that launches each parameterized test in its own thread.
 */
public class Parallelized extends Parameterized {

  /** A Junit runner scheduler that schedules new test executions immediately in a new (or reused) thread */
  private static class ThreadPoolScheduler implements RunnerScheduler {

    /** The executor service */
    private ExecutorService executor;

    /** Constructs a new ThreadPoolScheduler with an unbounded, cached thread pool */
    public ThreadPoolScheduler() {
      executor = Executors.newCachedThreadPool();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.junit.runners.model.RunnerScheduler#finished()
     */
    @Override
    public void finished() {
      executor.shutdown();
      try {
        executor.awaitTermination(10, TimeUnit.MINUTES);
      } catch (InterruptedException exc) {
        throw new RuntimeException(exc);
      }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.junit.runners.model.RunnerScheduler#schedule(java.lang.Runnable)
     */
    @Override
    public void schedule(Runnable childStatement) {
      executor.submit(childStatement);
    }
  }

  /**
   * Constructs a new parallelized runner for the given test class
   * 
   * @param clazz
   *          the test class
   * @throws Throwable
   *           if something goes wrong during test execution
   */
  public Parallelized(@SuppressWarnings("rawtypes") Class clazz) throws Throwable {
    super(clazz);
    setScheduler(new ThreadPoolScheduler());
  }

}
