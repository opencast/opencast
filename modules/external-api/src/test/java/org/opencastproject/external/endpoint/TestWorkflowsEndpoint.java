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
package org.opencastproject.external.endpoint;

import static com.entwinemedia.fn.data.Opt.none;
import static com.entwinemedia.fn.data.Opt.some;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;

import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowStateException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.Ignore;

import java.time.Instant;
import java.util.Date;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestWorkflowsEndpoint extends WorkflowsEndpoint {

  public TestWorkflowsEndpoint() throws Exception {

    this.endpointBaseUrl = "https://api.opencast.org";

    long runningWorkflowId = 84L;
    long stoppedWorkflowId = 42L;
    long missingWorkflowId = 24L;
    long unauthorizedWorkflowId = 12L;

    WorkflowOperationInstance woi1 = createNiceMock(WorkflowOperationInstance.class);
    expect(woi1.getId()).andReturn(1234L);
    expect(woi1.getTemplate()).andReturn("my-op");
    expect(woi1.getDescription()).andReturn("Example Operation");
    expect(woi1.getState()).andReturn(WorkflowOperationInstance.OperationState.RUNNING);
    expect(woi1.getTimeInQueue()).andReturn(20L);
    expect(woi1.getExecutionHost()).andReturn("http://localhost:8080");
    expect(woi1.getExecutionCondition()).andReturn("${letfail}");
    expect(woi1.isFailWorkflowOnException()).andReturn(true);
    expect(woi1.getExceptionHandlingWorkflow()).andReturn("fail");
    expect(woi1.getRetryStrategy()).andReturn(RetryStrategy.RETRY);
    expect(woi1.getMaxAttempts()).andReturn(42);
    expect(woi1.getFailedAttempts()).andReturn(1);
    expect(woi1.getDateStarted()).andReturn(Date.from(Instant.parse("2018-01-01T12:00:00Z"))).anyTimes();
    expect(woi1.getConfigurationKeys()).andReturn(Sets.newHashSet("key", "foo"));
    expect(woi1.getConfiguration("key")).andReturn("value");
    expect(woi1.getConfiguration("foo")).andReturn("bar");
    replay(woi1);

    WorkflowOperationInstance woi2 = createNiceMock(WorkflowOperationInstance.class);
    expect(woi2.getId()).andReturn(5678L);
    expect(woi2.getTemplate()).andReturn("my-op2");
    expect(woi2.getDescription()).andReturn("Example Operation2");
    expect(woi2.getState()).andReturn(WorkflowOperationInstance.OperationState.SUCCEEDED);
    expect(woi2.getTimeInQueue()).andReturn(30L);
    expect(woi2.getExecutionHost()).andReturn("http://localhost:8080");
    expect(woi2.getSkipCondition()).andReturn("${letfail}");
    expect(woi2.isFailWorkflowOnException()).andReturn(false);
    expect(woi2.getRetryStrategy()).andReturn(RetryStrategy.HOLD);
    expect(woi2.getMaxAttempts()).andReturn(0);
    expect(woi2.getFailedAttempts()).andReturn(0);
    expect(woi2.getDateStarted()).andReturn(Date.from(Instant.parse("2018-02-03T12:00:00Z"))).anyTimes();
    expect(woi2.getDateCompleted()).andReturn(Date.from(Instant.parse("2018-02-03T13:14:15Z"))).anyTimes();
    expect(woi2.getConfigurationKeys()).andReturn(Sets.newHashSet("abcd"));
    expect(woi2.getConfiguration("abcd")).andReturn("1234");
    replay(woi2);

    MediaPackage mp = createNiceMock(MediaPackage.class);
    expect(mp.getIdentifier()).andReturn(new IdImpl("905672ed-181c-4d60-b7cd-02758f61e713")).anyTimes();
    replay(mp);

    MediaPackage mpWithRunningWorkflow = createNiceMock(MediaPackage.class);
    replay(mpWithRunningWorkflow);

    WorkflowInstance runningWorkflow = createNiceMock(WorkflowInstance.class);
    expect(runningWorkflow.getId()).andReturn(runningWorkflowId).anyTimes();
    expect(runningWorkflow.getTitle()).andReturn("Running Workflow");
    expect(runningWorkflow.getDescription()).andReturn("A running workflow");
    expect(runningWorkflow.getTemplate()).andReturn("fast");
    expect(runningWorkflow.getMediaPackage()).andReturn(mp);
    expect(runningWorkflow.getCreator()).andReturn(new JpaUser("User1", null, null, "User Name", null, null, true));
    expect(runningWorkflow.getState()).andReturn(WorkflowInstance.WorkflowState.RUNNING);
    expect(runningWorkflow.getOperations()).andReturn(Lists.newArrayList(woi1));
    expect(runningWorkflow.getConfigurationKeys()).andReturn(Sets.newHashSet("efgh"));
    expect(runningWorkflow.getConfiguration("efgh")).andReturn("5678");
    replay(runningWorkflow);

    WorkflowInstance stoppedWorkflow = createNiceMock(WorkflowInstance.class);
    expect(stoppedWorkflow.getId()).andReturn(stoppedWorkflowId);
    expect(stoppedWorkflow.getTitle()).andReturn("Stopped Workflow");
    expect(stoppedWorkflow.getDescription()).andReturn("A stopped workflow");
    expect(stoppedWorkflow.getTemplate()).andReturn("fast");
    expect(stoppedWorkflow.getMediaPackage()).andReturn(mp);
    expect(stoppedWorkflow.getCreator()).andReturn(new JpaUser("User2", null, null, "User Name2", null, null, true));
    expect(stoppedWorkflow.getState()).andReturn(WorkflowInstance.WorkflowState.STOPPED);
    expect(stoppedWorkflow.getOperations()).andReturn(Lists.newArrayList(woi2));
    expect(stoppedWorkflow.getConfigurationKeys()).andReturn(Sets.newHashSet("ijklm"));
    expect(stoppedWorkflow.getConfiguration("ijklm")).andReturn("9000");
    replay(stoppedWorkflow);

    WorkflowInstance startedStoppedWorkflow = createNiceMock(WorkflowInstance.class);
    expect(startedStoppedWorkflow.getId()).andReturn(stoppedWorkflowId);
    expect(startedStoppedWorkflow.getTitle()).andReturn("Stopped Workflow");
    expect(startedStoppedWorkflow.getDescription()).andReturn("A stopped workflow");
    expect(startedStoppedWorkflow.getTemplate()).andReturn("fast");
    expect(startedStoppedWorkflow.getMediaPackage()).andReturn(mp);
    expect(startedStoppedWorkflow.getCreator()).andReturn(
            new JpaUser("User2", null, null, "User Name2", null, null, true));
    expect(startedStoppedWorkflow.getState()).andReturn(WorkflowInstance.WorkflowState.PAUSED);
    expect(startedStoppedWorkflow.getOperations()).andReturn(Lists.newArrayList(woi2));
    expect(startedStoppedWorkflow.getConfigurationKeys()).andReturn(Sets.newHashSet("abc"));
    expect(startedStoppedWorkflow.getConfiguration("abc")).andReturn("123");
    replay(startedStoppedWorkflow);

    WorkflowSet workflows = createNiceMock(WorkflowSet.class);
    expect(workflows.getItems()).andReturn(new WorkflowInstance[] { runningWorkflow, stoppedWorkflow });
    replay(workflows);

    // WorkflowService
    WorkflowService ws = createNiceMock(WorkflowService.class);
    //all workflows
    expect(ws.getWorkflowInstances(anyObject())).andReturn(workflows);
    //create
    WorkflowDefinition fastWorkflowDefinition = new WorkflowDefinitionImpl();
    expect(ws.getWorkflowDefinitionById("missing")).andThrow(new NotFoundException());
    expect(ws.getWorkflowDefinitionById("fast")).andReturn(fastWorkflowDefinition);
    expect(ws.start(eq(fastWorkflowDefinition), eq(mp), isNull(), anyObject())).andReturn(runningWorkflow);
    expect(ws.start(eq(fastWorkflowDefinition), eq(mpWithRunningWorkflow), isNull(), anyObject())).andThrow(
            new IllegalStateException("Illegal state msg"));
    //runningWorkflowId
    ws.remove(runningWorkflowId);
    expectLastCall().andThrow(new WorkflowStateException());
    expect(ws.getWorkflowById(runningWorkflowId)).andReturn(runningWorkflow).anyTimes();
    //stoppedWorkflow
    ws.remove(stoppedWorkflowId);
    expectLastCall().andVoid();
    expect(ws.getWorkflowById(stoppedWorkflowId)).andReturn(stoppedWorkflow);
    expect(ws.getWorkflowById(stoppedWorkflowId)).andReturn(startedStoppedWorkflow);
    //unauthorizedWorkflow
    ws.remove(unauthorizedWorkflowId);
    expectLastCall().andThrow(new UnauthorizedException(""));
    expect(ws.getWorkflowById(unauthorizedWorkflowId)).andThrow(new UnauthorizedException(""));
    //missingWorkflow
    ws.remove(missingWorkflowId);
    expectLastCall().andThrow(new NotFoundException());
    expect(ws.getWorkflowById(missingWorkflowId)).andThrow(new NotFoundException());
    replay(ws);
    setWorkflowService(ws);

    // IndexService
    IndexService is = createNiceMock(IndexService.class);
    Event e = new Event();
    Event eWithRunningWorkflow = new Event();
    expect(is.getEvent(eq("missing"), anyObject())).andReturn(none());
    expect(is.getEvent(eq("905672ed-181c-4d60-b7cd-02758f61e713"), anyObject())).andReturn(some(e));
    expect(is.getEvent(eq("mediapackage-with-running-workflow"), anyObject())).andReturn(some(eWithRunningWorkflow));
    expect(is.getEventMediapackage(e)).andReturn(mp);
    expect(is.getEventMediapackage(eWithRunningWorkflow)).andReturn(mpWithRunningWorkflow);
    replay(is);
    setIndexService(is);
  }
}
