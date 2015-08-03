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

package org.opencastproject.capture.admin.impl;

import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.admin.api.RecordingStateUpdate;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RecordingStateUpdateTest {
  private Recording recording = null;
  private RecordingStateUpdate rsu = null;

  @Before
  public void setUp() throws InterruptedException {
    recording = new RecordingImpl("test", RecordingState.CAPTURING);
    Assert.assertNotNull(recording);
    Thread.sleep(5);
    rsu = new RecordingStateUpdate(recording);
    Assert.assertNotNull(rsu);
  }

  @After
  public void tearDown() {
    recording = null;
    rsu = null;
  }

  @Test
  public void correctInformation() {
    Assert.assertEquals("test", rsu.getId());
    Assert.assertEquals(RecordingState.CAPTURING, rsu.getState());
    if (rsu.getTimeSinceLastUpdate() <= 1) {
      Assert.fail("Invalid update time in recording state update");
    }
  }

  @Test
  //This is a stupid test, but it gets us up to 100%...
  public void blank() {
    rsu = new RecordingStateUpdate();
    Assert.assertNotNull(rsu);
    Assert.assertNull(rsu.getId());
    Assert.assertNull(rsu.getState());
    Assert.assertNull(rsu.getTimeSinceLastUpdate());
  }
}
