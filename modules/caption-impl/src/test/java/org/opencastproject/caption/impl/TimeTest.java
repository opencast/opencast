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


package org.opencastproject.caption.impl;

import org.opencastproject.caption.api.IllegalTimeFormatException;
import org.opencastproject.caption.util.TimeUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link TimeImpl} and {@link TimeUtil}.
 *
 */
public class TimeTest {

  @Test
  public void timeCreationTest() {
    // valid times
    try {
      new TimeImpl(99, 59, 59, 999);
      new TimeImpl(0, 0, 0, 0);
    } catch (IllegalTimeFormatException e) {
      Assertions.fail(e.getMessage());
    }

    // invalid times
    try {
      new TimeImpl(100, 0, 0, 0);
      Assertions.fail("Should fail with invalid hour");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      new TimeImpl(0, 60, 0, 0);
      Assertions.fail("Should fail with invalid minute");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      new TimeImpl(0, 0, 60, 0);
      Assertions.fail("Should fail with invalid second");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      new TimeImpl(0, 0, 0, 1000);
      Assertions.fail("Should fail with invalid millisecond");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      new TimeImpl(-1, 0, 0, 0);
      Assertions.fail("Should fail with invalid hour");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      new TimeImpl(0, -1, 0, 0);
      Assertions.fail("Should fail with invalid minute");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      new TimeImpl(0, 0, -1, 0);
      Assertions.fail("Should fail with invalid second");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      new TimeImpl(0, 0, 0, -1);
      Assertions.fail("Should fail with invalid millisecond");
    } catch (IllegalTimeFormatException e) {
    }
  }

  @Test
  public void timeConversionTest() {
    // valid entry formats
    try {
      TimeUtil.importSrt("00:00:00,001");
      TimeUtil.importDFXP("00:00:00");
      TimeUtil.importDFXP("00:00:00.1");
      TimeUtil.importDFXP("00:00:00.01");
      TimeUtil.importDFXP("00:00:00.001");
      TimeUtil.importDFXP("00:00:00.0011");
    } catch (IllegalTimeFormatException e) {
      Assertions.fail(e.getMessage());
    }

    // invalid time formats
    try {
      TimeUtil.importSrt("00:00:00.001");
      Assertions.fail("Should fail for this time");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      TimeUtil.importSrt("00:00:00:001");
      Assertions.fail("Should fail for this time");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      TimeUtil.importSrt("00:00:0,001");
      Assertions.fail("Should fail for this time");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      TimeUtil.importSrt("00:0:00,001");
      Assertions.fail("Should fail for this time");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      TimeUtil.importSrt("0:00:00,001");
      Assertions.fail("Should fail for this time");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      TimeUtil.importDFXP("00:00:00.");
      Assertions.fail("Should fail for this time");
    } catch (IllegalTimeFormatException e) {
    }
    try {
      TimeUtil.importDFXP("00:00:0.1");
      Assertions.fail("Should fail for this time");
    } catch (IllegalTimeFormatException e) {
    }
  }

  @Test
  public void testTimeEqualities() {
    try {
      Assertions.assertEquals("00:00:00,001", TimeUtil.exportToSrt(TimeUtil.importSrt("00:00:00,001")));
      Assertions.assertEquals("0:00:00.001", TimeUtil.exportToDFXP(TimeUtil.importDFXP("0:00:00.001")));
    } catch (IllegalTimeFormatException e) {
      Assertions.fail(e.getMessage());
    }
  }
}
