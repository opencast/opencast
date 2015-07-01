/**
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


package org.opencastproject.oaipmh.harvester;

import org.joda.time.DateTime;
import org.junit.Test;
import org.opencastproject.oaipmh.TestUtil;
import org.opencastproject.oaipmh.util.PersistenceEnv;
import org.opencastproject.oaipmh.util.PersistenceUtil;
import org.opencastproject.util.data.Option;

import javax.persistence.EntityManagerFactory;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Option.some;

/**
 * Test persistence of {@link LastHarvested}.
 */
public class LastHarvestedTest {
  @Test
  public void testLastHarvested() {
    PersistenceEnv penv = newPenv();
    assertEquals(Option.none(), LastHarvested.getLastHarvestDate(penv, "bla"));
    DateTime now = new DateTime();
    Date a = now.toDate();
    // save
    LastHarvested.update(penv, new LastHarvested("url-1", a));
    assertEquals(some(a), LastHarvested.getLastHarvestDate(penv, "url-1"));
    // now update
    Date b = now.plusMinutes(1).toDate();
    LastHarvested.update(penv, new LastHarvested("url-1", b));
    assertEquals(some(b), LastHarvested.getLastHarvestDate(penv, "url-1"));
    // save another
    LastHarvested.update(penv, new LastHarvested("url-2", a));
    assertEquals(some(a), LastHarvested.getLastHarvestDate(penv, "url-2"));
    // cleanup 1
    LastHarvested.cleanup(penv, "url-1 url-2".split(" "));
    assertTrue(LastHarvested.getLastHarvestDate(penv, "url-1").isSome());
    assertTrue(LastHarvested.getLastHarvestDate(penv, "url-2").isSome());
    // cleanup 2
    LastHarvested.cleanup(penv, "url-2".split(" "));
    assertTrue(LastHarvested.getLastHarvestDate(penv, "url-1").isNone());
    assertTrue(LastHarvested.getLastHarvestDate(penv, "url-2").isSome());
  }

  private PersistenceEnv newPenv() {
    EntityManagerFactory emf = TestUtil.newTestEntityManagerFactory("org.opencastproject.oaipmh.harvester");
    return PersistenceUtil.newPersistenceEnvironment(emf);
  }
}
