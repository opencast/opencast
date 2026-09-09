/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.elasticsearch.index.rebuild;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Collects the identifiers of objects whose ACL contains deny rules, so that an index rebuild can
 * report them once at the end instead of logging a line per entry.
 *
 * <p>The search index cannot express denial, so such entries are dropped when an object is indexed.
 * That is worth telling an administrator about, but the per-entry message it used to produce fired
 * once per deny entry per object and drowned the rebuild log.
 *
 * <p>Collection is only active between {@link #start()} and {@link #stop()}, which
 * {@link IndexRebuildService} brackets a rebuild with. Outside a rebuild — during ordinary
 * indexing — {@link #record} does nothing beyond a volatile read, so nothing accumulates in a
 * long-running instance.
 *
 * <p>State is held per thread. A rebuild runs on the thread that called it, and keeping the
 * collected ids thread-local means concurrent ordinary indexing on other threads cannot add to a
 * rebuild's report, nor can two rebuilds interfere.
 */
public final class DenyAclCollector {

  /** Ids collected for the rebuild running on this thread, or null when no rebuild is running. */
  private static final ThreadLocal<Set<String>> COLLECTED = new ThreadLocal<>();

  private DenyAclCollector() {
  }

  /**
   * Start collecting on the current thread, discarding anything previously collected.
   */
  public static void start() {
    COLLECTED.set(new LinkedHashSet<>());
  }

  /**
   * Note that the object with the given identifier carries at least one deny entry. Does nothing
   * when no rebuild is running on this thread.
   *
   * @param identifier
   *          the identifier of the event or series, may be null
   */
  public static void record(String identifier) {
    Set<String> collected = COLLECTED.get();
    if (collected != null && identifier != null) {
      collected.add(identifier);
    }
  }

  /**
   * Return the identifiers collected on this thread, in encounter order.
   *
   * @return the identifiers, empty if no rebuild is running
   */
  public static Set<String> collected() {
    Set<String> collected = COLLECTED.get();
    return collected == null ? Collections.emptySet() : Collections.unmodifiableSet(collected);
  }

  /**
   * Stop collecting on the current thread and release the collected identifiers.
   */
  public static void stop() {
    COLLECTED.remove();
  }

}
