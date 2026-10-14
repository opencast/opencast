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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.util.List;

public class DenyAclCollectorTest {

  @After
  public void tearDown() {
    DenyAclCollector.stop();
  }

  /**
   * Outside a rebuild nothing is collected, so ordinary indexing in a long-running instance does
   * not accumulate identifiers.
   */
  @Test
  public void testRecordsNothingWhileNoRebuildIsRunning() {
    DenyAclCollector.record("event-1");
    assertTrue(DenyAclCollector.collected().isEmpty());
  }

  /**
   * Each affected object is reported once regardless of how many deny entries its ACL carries,
   * which is the flooding this replaces.
   */
  @Test
  public void testCollectsEachIdentifierOnceInEncounterOrder() {
    DenyAclCollector.start();
    DenyAclCollector.record("event-2");
    DenyAclCollector.record("event-1");
    DenyAclCollector.record("event-2");

    assertEquals(List.of("event-2", "event-1"), List.copyOf(DenyAclCollector.collected()));
  }

  /**
   * A null identifier must not be collected or cause a failure while indexing.
   */
  @Test
  public void testIgnoresNullIdentifier() {
    DenyAclCollector.start();
    DenyAclCollector.record(null);
    assertTrue(DenyAclCollector.collected().isEmpty());
  }

  /**
   * Stopping releases the collected identifiers, so a later rebuild does not inherit them.
   */
  @Test
  public void testStopClearsCollectedIdentifiers() {
    DenyAclCollector.start();
    DenyAclCollector.record("event-1");
    DenyAclCollector.stop();

    assertTrue(DenyAclCollector.collected().isEmpty());
  }

  /**
   * Collection is per thread, so ordinary indexing running concurrently on another thread cannot
   * add to a rebuild's report.
   */
  @Test
  public void testCollectionIsIsolatedPerThread() throws Exception {
    DenyAclCollector.start();
    DenyAclCollector.record("event-1");

    Thread other = new Thread(() -> DenyAclCollector.record("event-from-other-thread"));
    other.start();
    other.join();

    assertEquals(List.of("event-1"), List.copyOf(DenyAclCollector.collected()));
  }

}
