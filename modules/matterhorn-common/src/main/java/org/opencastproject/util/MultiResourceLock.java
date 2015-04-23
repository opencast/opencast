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

import org.opencastproject.util.data.Function;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Synchronization utility to concurrently access a variable set of resources.
 */
public class MultiResourceLock {
  private final ConcurrentHashMap<Object, AtomicInteger> lockMap = new ConcurrentHashMap<Object, AtomicInteger>();

  public MultiResourceLock() {
  }

  /**
   * Synchronize access to a given resource. Execute function <code>function</code> only, if currently now other
   * function accesses resource <code>resource</code>.
   * <p/>
   * Implementation note: The given resource is not used in any synchronization primitives, i.e. no monitor of that
   * object are being held.
   */
  public <A, K> A synchronize(final K resource, final Function<K, A> function) {
    final AtomicInteger counter;
    synchronized (lockMap) {
      AtomicInteger newCounter = new AtomicInteger();
      AtomicInteger currentCounter = lockMap.putIfAbsent(resource, newCounter);
      counter = currentCounter != null ? currentCounter : newCounter;
      counter.incrementAndGet();
    }

    final A ap;
    synchronized (counter) {
      ap = function.apply(resource);
    }

    synchronized (lockMap) {
      if (counter.decrementAndGet() == 0)
        lockMap.remove(resource);
    }
    return ap;
  }

  /** For testing purposes only. */
  int getLockMapSize() {
    return lockMap.size();
  }

}
