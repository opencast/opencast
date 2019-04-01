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
package org.opencastproject.scheduler.conflict.handler

import org.opencastproject.scheduler.api.ConflictResolution
import org.opencastproject.scheduler.api.ConflictResolution.Strategy
import org.opencastproject.scheduler.api.SchedulerEvent

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.util.Hashtable

class DefaultSchedulerConflictHandlerTest {

    private var conflictHandler: DefaultSchedulerConflictHandler? = null
    private var newEvent: SchedulerEvent? = null
    private var oldEvent: SchedulerEvent? = null

    @Before
    fun setUp() {
        conflictHandler = DefaultSchedulerConflictHandler()
        newEvent = EasyMock.createNiceMock<SchedulerEvent>(SchedulerEvent::class.java)
        oldEvent = EasyMock.createNiceMock<SchedulerEvent>(SchedulerEvent::class.java)
        EasyMock.replay(newEvent, oldEvent)
    }

    @Test
    @Throws(Exception::class)
    fun testDefaultOption() {
        conflictHandler!!.updated(Hashtable<String, String>())
        val resolution = conflictHandler!!.handleConflict(newEvent!!, oldEvent!!)
        Assert.assertEquals(Strategy.OLD, resolution.conflictStrategy)
    }

    @Test
    @Throws(Exception::class)
    fun testOldOption() {
        val properties = Hashtable<String, String>()
        properties["handler"] = "old"
        conflictHandler!!.updated(properties)
        val resolution = conflictHandler!!.handleConflict(newEvent!!, oldEvent!!)
        Assert.assertEquals(Strategy.OLD, resolution.conflictStrategy)
    }

    @Test
    @Throws(Exception::class)
    fun testNewOption() {
        val properties = Hashtable<String, String>()
        properties["handler"] = "new"
        conflictHandler!!.updated(properties)
        val resolution = conflictHandler!!.handleConflict(newEvent!!, oldEvent!!)
        Assert.assertEquals(Strategy.NEW, resolution.conflictStrategy)
    }

}
