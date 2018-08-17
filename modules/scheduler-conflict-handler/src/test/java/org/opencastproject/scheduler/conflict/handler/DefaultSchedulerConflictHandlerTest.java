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
package org.opencastproject.scheduler.conflict.handler;

import org.opencastproject.scheduler.api.ConflictResolution;
import org.opencastproject.scheduler.api.ConflictResolution.Strategy;
import org.opencastproject.scheduler.api.SchedulerEvent;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Hashtable;

public class DefaultSchedulerConflictHandlerTest {

  private DefaultSchedulerConflictHandler conflictHandler;
  private SchedulerEvent newEvent;
  private SchedulerEvent oldEvent;

  @Before
  public void setUp() {
    conflictHandler = new DefaultSchedulerConflictHandler();
    newEvent = EasyMock.createNiceMock(SchedulerEvent.class);
    oldEvent = EasyMock.createNiceMock(SchedulerEvent.class);
    EasyMock.replay(newEvent, oldEvent);
  }

  @Test
  public void testDefaultOption() throws Exception {
    conflictHandler.updated(new Hashtable<String, String>());
    ConflictResolution resolution = conflictHandler.handleConflict(newEvent, oldEvent);
    Assert.assertEquals(Strategy.OLD, resolution.getConflictStrategy());
  }

  @Test
  public void testOldOption() throws Exception {
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put("handler", "old");
    conflictHandler.updated(properties);
    ConflictResolution resolution = conflictHandler.handleConflict(newEvent, oldEvent);
    Assert.assertEquals(Strategy.OLD, resolution.getConflictStrategy());
  }

  @Test
  public void testNewOption() throws Exception {
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put("handler", "new");
    conflictHandler.updated(properties);
    ConflictResolution resolution = conflictHandler.handleConflict(newEvent, oldEvent);
    Assert.assertEquals(Strategy.NEW, resolution.getConflictStrategy());
  }

}
