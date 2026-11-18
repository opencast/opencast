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
package org.opencastproject.scheduler.impl;

import org.opencastproject.scheduler.api.Recording;
import org.opencastproject.scheduler.api.RecordingImpl;
import org.opencastproject.scheduler.api.RecordingState;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RecordingTest {
  private Recording recording = null;
  private Long time = 0L;

  @BeforeEach
  public void setUp() {
    recording = new RecordingImpl("test", RecordingState.CAPTURING);
    Assertions.assertNotNull(recording);
    time = recording.getLastCheckinTime();
  }

  @AfterEach
  public void tearDown() {
    recording = null;
    time = 0L;
  }

  @Test
  public void correctInformation() {
    Assertions.assertEquals("test", recording.getID());
    Assertions.assertEquals(RecordingState.CAPTURING, recording.getState());
  }

  @Test
  public void changedInformation() throws InterruptedException {
    Assertions.assertEquals("test", recording.getID());
    Assertions.assertEquals(RecordingState.CAPTURING, recording.getState());
    Assertions.assertEquals(time, recording.getLastCheckinTime());

    Thread.sleep(1);
    recording.setState(RecordingState.UPLOADING);

    Assertions.assertEquals("test", recording.getID());
    Assertions.assertEquals(RecordingState.UPLOADING, recording.getState());
    Thread.sleep(1);
    if (recording.getLastCheckinTime() <= time || recording.getLastCheckinTime() >= System.currentTimeMillis()) {
      Assertions.fail("Invalid checkin time");
    }
  }
}
