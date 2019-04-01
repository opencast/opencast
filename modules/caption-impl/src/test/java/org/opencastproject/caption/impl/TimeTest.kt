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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package org.opencastproject.caption.impl

import org.opencastproject.caption.api.IllegalTimeFormatException
import org.opencastproject.caption.util.TimeUtil

import org.junit.Assert
import org.junit.Test

/**
 * Test for [TimeImpl] and [TimeUtil].
 *
 */
class TimeTest {

    @Test
    fun timeCreationTest() {
        // valid times
        try {
            TimeImpl(99, 59, 59, 999)
            TimeImpl(0, 0, 0, 0)
        } catch (e: IllegalTimeFormatException) {
            Assert.fail(e.message)
        }

        // invalid times
        try {
            TimeImpl(100, 0, 0, 0)
            Assert.fail("Should fail with invalid hour")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeImpl(0, 60, 0, 0)
            Assert.fail("Should fail with invalid minute")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeImpl(0, 0, 60, 0)
            Assert.fail("Should fail with invalid second")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeImpl(0, 0, 0, 1000)
            Assert.fail("Should fail with invalid millisecond")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeImpl(-1, 0, 0, 0)
            Assert.fail("Should fail with invalid hour")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeImpl(0, -1, 0, 0)
            Assert.fail("Should fail with invalid minute")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeImpl(0, 0, -1, 0)
            Assert.fail("Should fail with invalid second")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeImpl(0, 0, 0, -1)
            Assert.fail("Should fail with invalid millisecond")
        } catch (e: IllegalTimeFormatException) {
        }

    }

    @Test
    fun timeConversionTest() {
        // valid entry formats
        try {
            TimeUtil.importSrt("00:00:00,001")
            TimeUtil.importDFXP("00:00:00")
            TimeUtil.importDFXP("00:00:00.1")
            TimeUtil.importDFXP("00:00:00.01")
            TimeUtil.importDFXP("00:00:00.001")
            TimeUtil.importDFXP("00:00:00.0011")
        } catch (e: IllegalTimeFormatException) {
            Assert.fail(e.message)
        }

        // invalid time formats
        try {
            TimeUtil.importSrt("00:00:00.001")
            Assert.fail("Should fail for this time")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeUtil.importSrt("00:00:00:001")
            Assert.fail("Should fail for this time")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeUtil.importSrt("00:00:0,001")
            Assert.fail("Should fail for this time")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeUtil.importSrt("00:0:00,001")
            Assert.fail("Should fail for this time")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeUtil.importSrt("0:00:00,001")
            Assert.fail("Should fail for this time")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeUtil.importDFXP("00:00:00.")
            Assert.fail("Should fail for this time")
        } catch (e: IllegalTimeFormatException) {
        }

        try {
            TimeUtil.importDFXP("00:00:0.1")
            Assert.fail("Should fail for this time")
        } catch (e: IllegalTimeFormatException) {
        }

    }

    @Test
    fun testTimeEqualities() {
        try {
            Assert.assertEquals("00:00:00,001", TimeUtil.exportToSrt(TimeUtil.importSrt("00:00:00,001")))
            Assert.assertEquals("0:00:00.001", TimeUtil.exportToDFXP(TimeUtil.importDFXP("0:00:00.001")))
        } catch (e: IllegalTimeFormatException) {
            Assert.fail(e.message)
        }

    }
}
