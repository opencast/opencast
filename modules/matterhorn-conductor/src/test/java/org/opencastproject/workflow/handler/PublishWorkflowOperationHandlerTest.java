/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
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
package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PublishWorkflowOperationHandlerTest {

  private PublishWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;
  private MediaPackage mpSearch;
  private WorkflowInstanceImpl workflowInstance;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI uriMP = InspectWorkflowOperationHandler.class.getResource("/publish_mediapackage.xml").toURI();
    URI uriMPSearch = InspectWorkflowOperationHandler.class.getResource("/publish_search_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());
    mpSearch = builder.loadFromXml(uriMPSearch.toURL().openStream());

    // set up service
    operationHandler = new PublishWorkflowOperationHandler();

  }

  @Test
  // publish operation handler returns unchanged media package
  // the actual test is done on the media package that is passed to the SearchService
  // test is done in MediaPackageEquals class, all elements are tested also adding correct referenced flavour
  public void testPublishOperation() throws Exception {
    // Add the mediapackage to a workflow instance
    workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    operation.setConfiguration("source-tags", "publish");
    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    // mock Search service, ensuring the correct media package is distributed to search service
    JaxbJob job = new JaxbJob(Long.valueOf(1));
    job.setStatus(Job.Status.FINISHED);
    
    ServiceRegistry serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(1)).andReturn(job);
    EasyMock.replay(serviceRegistry);

    SearchService searchService = EasyMock.createMock(SearchService.class);
    EasyMock.expect(searchService.add((MediaPackage)EasyMock.anyObject())).andReturn(job).anyTimes();
    EasyMock.replay(searchService);

    searchService.add(eqMediaPackage(mpSearch));
    operationHandler.setSearchService(searchService);
    operationHandler.setServiceRegistry(serviceRegistry);

    // Run the media package through the operation handler, ensuring that the flavors are retained
    operationHandler.start(workflowInstance, null);
  }

  // register custom matcher for media packages to EasyMock
  private static <T extends MediaPackage> T eqMediaPackage(T in) {
    EasyMock.reportMatcher(new MediaPackageEquals(in));
    return null;
  }

}
