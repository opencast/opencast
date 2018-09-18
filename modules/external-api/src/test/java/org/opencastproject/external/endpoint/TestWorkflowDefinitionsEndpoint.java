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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.Ignore;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestWorkflowDefinitionsEndpoint extends WorkflowDefinitionsEndpoint {

  public TestWorkflowDefinitionsEndpoint() throws Exception {
    WorkflowOperationDefinition wod1 = createNiceMock(WorkflowOperationDefinition.class);
    expect(wod1.getId()).andReturn("my-op");
    expect(wod1.getDescription()).andReturn("Example Operation");
    expect(wod1.getExceptionHandlingWorkflow()).andReturn("fail");
    expect(wod1.getExecutionCondition()).andReturn("${letfail}");
    expect(wod1.isFailWorkflowOnException()).andReturn(true);
    expect(wod1.getMaxAttempts()).andReturn(42);
    expect(wod1.getRetryStrategy()).andReturn(RetryStrategy.HOLD);
    expect(wod1.getConfigurationKeys()).andReturn(Sets.newHashSet("key", "foo"));
    expect(wod1.getConfiguration("key")).andReturn("value");
    expect(wod1.getConfiguration("foo")).andReturn("bar");
    replay(wod1);

    WorkflowOperationDefinition wod2 = createNiceMock(WorkflowOperationDefinition.class);
    expect(wod2.getId()).andReturn("my-op2");
    expect(wod2.getDescription()).andReturn("Example Operation2");
    expect(wod2.getSkipCondition()).andReturn("${letfail}");
    expect(wod2.isFailWorkflowOnException()).andReturn(false);
    expect(wod2.getMaxAttempts()).andReturn(0);
    expect(wod2.getRetryStrategy()).andReturn(RetryStrategy.RETRY);
    expect(wod2.getConfigurationKeys()).andReturn(Sets.newHashSet("abcd"));
    expect(wod2.getConfiguration("abcd")).andReturn("1234");
    replay(wod2);

    WorkflowDefinitionImpl wd1 = new WorkflowDefinitionImpl();
    wd1.setId("example1");
    wd1.setTitle("Example workflow");
    wd1.setDescription("Example workflow definition");
    wd1.setDisplayOrder(2);
    wd1.setConfigurationPanel("<h3>Config</h3>");
    wd1.addTag("archive");
    wd1.addTag("my-tag");
    wd1.getOperations().add(wod1);

    WorkflowDefinitionImpl wd2 = new WorkflowDefinitionImpl();
    wd2.setId("example2");
    wd2.setTitle("Another workflow");
    wd2.setDescription("Example workflow definition");
    wd2.setDisplayOrder(0);
    wd2.setConfigurationPanel("<h3>Config2</h3>");
    wd2.getOperations().add(wod2);

    WorkflowService ws = createNiceMock(WorkflowService.class);
    expect(ws.listAvailableWorkflowDefinitions()).andReturn(Lists.newArrayList(wd1, wd2));
    expect(ws.getWorkflowDefinitionById("example1")).andReturn(wd1);
    replay(ws);

    setWorkflowService(ws);
  }
}
