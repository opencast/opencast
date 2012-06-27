/**
 *  Copyright 2009, 2010 The Regents of the University of Californiaicensed under the Educational Community License, Version 2.0
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
package org.opencastproject.distribution.itunesu;

import org.opencastproject.deliver.schedule.Action;
import org.opencastproject.deliver.schedule.Schedule;
import org.opencastproject.deliver.schedule.Task;
import org.opencastproject.deliver.schedule.Task.State;
import org.opencastproject.deliver.schedule.TaskSerializer;
import org.opencastproject.deliver.store.MemoryStore;
import org.opencastproject.deliver.store.Serializer;
import org.opencastproject.deliver.store.Store;

import junit.framework.Assert;

import org.junit.Test;

public class DistributionServiceImplTest {
  @Test
  public void testSchedule() throws Exception {
    Schedule s = new Schedule();
    TestAction action = new TestAction("A1");
    s.start(action);
    Thread.sleep(5 * 1000);
    Task t = s.getTask("A1");
    Assert.assertEquals(State.COMPLETE, t.getState());
    Assert.assertEquals(3, t.getResumeCount());
    s.shutdown();
  }

  @Test
  public void testFails() throws Exception {
    Schedule s = new Schedule();
    TestAction action = new TestAction("A2");
    action.setExecuteLimit(2);
    action.setFails(true);
    s.start(action);
    Thread.sleep(3 * 1000);
    Task t = s.getTask("A2");
    // Assert.assertEquals(State.FAILED, t.getState());
    Assert.assertEquals(2, t.getResumeCount());
    s.shutdown();
  }

  @Test
  public void testRestart() throws Exception {
    Store<Task> active_store = new MemoryStore<Task>(new TaskSerializer());
    Store<Task> complete_store = new MemoryStore<Task>(new TaskSerializer());
    Schedule s1 = new Schedule(active_store, complete_store);

    TestAction a1 = new TestAction("A1");
    s1.start(a1);
    TestAction a2 = new TestAction("A2");
    s1.start(a2);
    TestAction a3 = new TestAction("A3");
    s1.start(a3);

    // Thread.sleep(100);
    s1.shutdownNow();

    Schedule s2 = new Schedule(active_store, complete_store);
    Thread.sleep(3 * 1000);

    Task t1 = s2.getTask("A1");
    Assert.assertEquals(State.COMPLETE, t1.getState());

    Task t2 = s2.getTask("A2");
    Assert.assertEquals(State.COMPLETE, t2.getState());

    Task t3 = s2.getTask("A3");
    Assert.assertEquals(State.COMPLETE, t3.getState());
  }

  @Test
  public void testDeadline() throws Exception {
    Schedule s1 = new Schedule();
    TestAction a1 = new TestAction("A1");
    a1.setDeadlineSeconds(1L);
    Task t1 = s1.start(a1);

    Thread.sleep(2 * 1000);
    // Assert.assertEquals(State.FAILED, t1.getState());
  }

  @Test
  public void testRetries() throws Exception {
    Schedule s = new Schedule();
    TestAction a1 = new TestAction("A1");
    a1.setExecuteCount(2);
    a1.setRetries(true);
    Task t1 = s.start(a1);

    Thread.sleep(4 * 1000);

    // Assert.assertEquals(3, t1.getRetryCount());
    // Assert.assertEquals(State.FAILED, t1.getState());
    
    s.start(a1);
    s.shutdown();
  }
  
  @Test
  public void testMakeTask() {
    TestAction a1 = new TestAction("T1");
    Task t1 = a1.makeTask();
    Assert.assertEquals(a1, t1.getAction());
  }
  
  @Test
  public void testToFromJSON() throws Exception {
    TestAction a1 = new TestAction("T2");
    a1.setExecuteLimit(8);
  
    Task t1 = a1.makeTask();
    t1.setStatus("testing");
  
    Serializer<Task> s = new TaskSerializer();
    String json = s.toString(t1);
  
    Task t2 = s.fromString(json);
    Assert.assertEquals(State.INITIAL, t2.getState());
    Assert.assertEquals("testing", t2.getStatus());
  
    Action a2 = t2.getAction();
    Assert.assertTrue(a2 instanceof TestAction);
    Assert.assertEquals(8, ((TestAction)a2).getExecuteLimit());
    Assert.assertEquals(0, ((TestAction)a2).getExecuteCount());
    Assert.assertFalse(((TestAction)a2).getFails());
  }

}
