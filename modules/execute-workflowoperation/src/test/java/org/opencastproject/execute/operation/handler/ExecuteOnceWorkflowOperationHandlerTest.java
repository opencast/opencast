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

package org.opencastproject.execute.operation.handler;

import static org.junit.Assert.fail;

import org.opencastproject.execute.api.ExecuteException;
import org.opencastproject.execute.api.ExecuteService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier.Result;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.CatalogImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;

/**
 * Tests for ExecuteOnceWorkflowOperationHandler
 */
public class ExecuteOnceWorkflowOperationHandlerTest {

  private static ExecuteService executeService;
  private static Workspace workspaceService;
  private static ExecuteOnceWorkflowOperationHandler execOnceWOH;
  private static WorkflowInstance workflowInstance;
  private static Catalog catalog;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    // Mocking just about everything, just testing the mediapackage parse
    String expectedTypeString = "catalog";
    String catalogId = "catalog-id";

    URI catUri = new URI("http://api.com/catalog");
    catalog = CatalogImpl.newInstance();
    catalog.addTag("engage-download");
    catalog.setIdentifier(catalogId);
    catalog.setURI(catUri);

    WorkflowOperationInstance operation = EasyMock.createMock(WorkflowOperationInstance.class);

    EasyMock.expect(operation.getId()).andReturn(123L).anyTimes();
    EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.EXEC_PROPERTY)).andReturn(null)
            .anyTimes();
    EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.PARAMS_PROPERTY)).andReturn(null)
            .anyTimes();
    EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.LOAD_PROPERTY)).andReturn("123")
            .anyTimes();
    EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY))
            .andReturn(null).anyTimes();
    EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.TARGET_TAGS_PROPERTY))
            .andReturn(null).anyTimes();
    EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.OUTPUT_FILENAME_PROPERTY))
            .andReturn(null).anyTimes();
    EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.SET_WF_PROPS_PROPERTY))
            .andReturn("false").anyTimes();
    // these two need to supply a real string
    EasyMock.expect(operation.getConfiguration(ExecuteOnceWorkflowOperationHandler.EXPECTED_TYPE_PROPERTY))
            .andReturn(expectedTypeString).anyTimes();
    EasyMock.replay(operation);

    Id mpId = EasyMock.createMock(Id.class);

    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    mediaPackage.add((MediaPackageElement) EasyMock.anyObject());
    EasyMock.expect(mediaPackage.getIdentifier()).andReturn(mpId).anyTimes();
    EasyMock.replay(mediaPackage);

    workspaceService = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspaceService.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(catUri).anyTimes();
    EasyMock.replay(workspaceService);

    workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andStubReturn(operation);
    EasyMock.replay(workflowInstance);

    // Override the waitForStatus method to not block the jobs
    execOnceWOH = new ExecuteOnceWorkflowOperationHandler() {
      @Override
      protected Result waitForStatus(long timeout, Job... jobs) {
        HashMap<Job, Status> map = Stream.mk(jobs).foldl(new HashMap<Job, Status>(),
                new Fn2<HashMap<Job, Status>, Job, HashMap<Job, Status>>() {
          @Override
          public HashMap<Job, Status> apply(HashMap<Job, Status> a, Job b) {
            a.put(b, Status.FINISHED);
            return a;
          }
        });
        return new Result(map);
      }
    };
    execOnceWOH.setWorkspace(workspaceService);
  }

  private void setEmptyPayload() throws ExecuteException, MediaPackageException {
    Job catalogJob = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(catalogJob.getQueueTime()).andReturn(123L).anyTimes();
    EasyMock.expect(catalogJob.getPayload()).andReturn("").anyTimes();
    EasyMock.replay(catalogJob);

    executeService = EasyMock.createMock(ExecuteService.class);
    EasyMock.expect(
            executeService.execute(EasyMock.anyString(), EasyMock.anyString(), (MediaPackage) EasyMock.anyObject(),
                    EasyMock.anyString(), (MediaPackageElement.Type) EasyMock.anyObject(), EasyMock.anyLong()))
            .andReturn(catalogJob).anyTimes();
    EasyMock.replay(executeService);

    execOnceWOH.setExecuteService(executeService);

  }

  private void setSomePayload() throws ExecuteException, MediaPackageException {
    Job catalogJob = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(catalogJob.getQueueTime()).andReturn(123L).anyTimes();
    EasyMock.expect(catalogJob.getPayload()).andReturn(MediaPackageElementParser.getAsXml(catalog)).anyTimes();
    EasyMock.replay(catalogJob);

    executeService = EasyMock.createMock(ExecuteService.class);
    EasyMock.expect(
            executeService.execute(EasyMock.anyString(), EasyMock.anyString(), (MediaPackage) EasyMock.anyObject(),
                    EasyMock.anyString(), (MediaPackageElement.Type) EasyMock.anyObject(), EasyMock.anyLong()))
            .andReturn(catalogJob).anyTimes();
    EasyMock.replay(executeService);

    execOnceWOH.setExecuteService(executeService);

  }

  @Test
  public void startParseSomePayloadTest() throws ExecuteException, MediaPackageException {
    try {
      setSomePayload();
      execOnceWOH.start(workflowInstance, null);
    } catch (WorkflowOperationException ex) {
      fail("Should not throw exception" + ex.getMessage());
    }
  }

  @Test
  public void startParseEmptyPayloadTest() throws ExecuteException, MediaPackageException {
    try {
      setEmptyPayload();
      execOnceWOH.start(workflowInstance, null);
    } catch (WorkflowOperationException ex) {
      fail("Should not throw exception" + ex.getMessage());
    }
  }
}
