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

package org.opencastproject.workflow.handler.distribution;

import static org.junit.Assert.assertEquals;

import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RetractEngageWorkflowOperationHandlerTest {

  private static final String MP_ID = "mp-id";
  private static final long DELETE_FROM_SEARCH_JOB_ID = 1L;

  private RetractEngageWorkflowOperationHandler handler;
  private WorkflowInstance workflowInstance;
  private MediaPackage mediaPackage;
  private SearchService searchService;
  private ServiceRegistry serviceRegistry;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mediaPackage = builder.createNew(new IdImpl(MP_ID));
    Publication publication = PublicationImpl.publication("pub-id", EngagePublicationChannel.CHANNEL_ID,
            new URI("http://engage.org/play/" + MP_ID), MimeTypes.parseMimeType("text/html"));
    mediaPackage.add(publication);

    Job deleteFromSearchJob = mockJob(DELETE_FROM_SEARCH_JOB_ID);
    serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(DELETE_FROM_SEARCH_JOB_ID)).andReturn(deleteFromSearchJob).anyTimes();
    EasyMock.replay(serviceRegistry);

    searchService = EasyMock.createNiceMock(SearchService.class);
    EasyMock.expect(searchService.delete(MP_ID)).andReturn(deleteFromSearchJob);

    DownloadDistributionService downloadDistributionService =
            EasyMock.createNiceMock(DownloadDistributionService.class);
    EasyMock.replay(downloadDistributionService);

    handler = new RetractEngageWorkflowOperationHandler();
    handler.setJobBarrierPollingInterval(1L);
    handler.setServiceRegistry(serviceRegistry);
    handler.setSearchService(searchService);
    handler.setDownloadDistributionService(downloadDistributionService);

    workflowInstance = new WorkflowInstance();
    workflowInstance.setId(1);
    workflowInstance.setMediaPackage(mediaPackage);
    workflowInstance.setState(WorkflowState.RUNNING);

    WorkflowOperationInstance operation =
            new WorkflowOperationInstance("retract-engage", WorkflowOperationInstance.OperationState.RUNNING);
    List<WorkflowOperationInstance> operationsList = new ArrayList<>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);
  }

  private Job mockJob(Long jobId) {
    Job job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getId()).andReturn(jobId).anyTimes();
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    EasyMock.expect(job.getDateCreated()).andReturn(new Date()).anyTimes();
    EasyMock.expect(job.getDateStarted()).andReturn(new Date()).anyTimes();
    EasyMock.expect(job.getQueueTime()).andReturn(0L).anyTimes();
    EasyMock.replay(job);
    return job;
  }

  /**
   * Regression test for OPENCAST-7911: engage retraction must remove the publication element from the
   * media package even though it also succeeded in retracting/removing the media package from search.
   */
  @Test
  public void testStartRemovesPublicationEvenWhenEngageRetractionSucceeds() throws Exception {
    MediaPackage searchMediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .createNew(new IdImpl(MP_ID));
    EasyMock.expect(searchService.get(MP_ID)).andReturn(searchMediaPackage);
    EasyMock.replay(searchService);

    WorkflowOperationResult result = handler.start(workflowInstance, null);

    assertEquals(WorkflowOperationResult.Action.CONTINUE, result.getAction());
    assertEquals(0, result.getMediaPackage().getPublications().length);
  }

  @Test
  public void testStartRemovesPublicationWhenAlreadyGoneFromSearch() throws Exception {
    EasyMock.expect(searchService.get(MP_ID)).andThrow(new NotFoundException("Not found"));
    EasyMock.replay(searchService);

    WorkflowOperationResult result = handler.start(workflowInstance, null);

    assertEquals(WorkflowOperationResult.Action.CONTINUE, result.getAction());
    assertEquals(0, result.getMediaPackage().getPublications().length);
  }

  @Test
  public void testStartSkipsWhenNothingToRetract() throws Exception {
    mediaPackage.remove(mediaPackage.getPublications()[0]);
    EasyMock.expect(searchService.get(MP_ID)).andThrow(new NotFoundException("Not found"));
    EasyMock.replay(searchService);

    WorkflowOperationResult result = handler.start(workflowInstance, null);

    assertEquals(WorkflowOperationResult.Action.SKIP, result.getAction());
  }
}
