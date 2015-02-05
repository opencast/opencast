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
package org.opencastproject.workflow.handler.inspection;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.api.MediaPackageMetadata;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InspectWorkflowOperationHandlerTest {
  private InspectWorkflowOperationHandler operationHandler;

  // local resources
  private URI uriMP;
  private URI uriMPUpdated;
  private MediaPackage mp;
  private MediaPackage mpUpdatedDC;
  private Track newTrack;
  private Job job;

  // mock services and objects
  private Workspace workspace = null;
  private MediaInspectionService inspectionService = null;
  private DublinCoreCatalogService dcService = null;
  private MediaPackageMetadata metadata = null;

  // constant metadata values
  private static final Date DATE = new Date();
  private static final String LANGUAGE = "language";
  private static final String LICENSE = "license";
  private static final String SERIES = "series";
  private static final String SERIES_TITLE = "series title";
  private static final String TITLE = "title";
  private static final String NEW_DC_URL = "http://www.url.org";

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    uriMP = InspectWorkflowOperationHandler.class.getResource("/inspect_mediapackage.xml").toURI();
    uriMPUpdated = InspectWorkflowOperationHandler.class.getResource("/inspect_mediapackage_updated.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());
    mpUpdatedDC = builder.loadFromXml(uriMPUpdated.toURL().openStream());
    newTrack = (Track) mpUpdatedDC.getTracks()[0];

    // set up service
    operationHandler = new InspectWorkflowOperationHandler();

    // set up mock metadata and metadata service providing it
    metadata = EasyMock.createNiceMock(MediaPackageMetadata.class);
    EasyMock.expect(metadata.getDate()).andReturn(DATE);
    EasyMock.expect(metadata.getLanguage()).andReturn(LANGUAGE);
    EasyMock.expect(metadata.getLicense()).andReturn(LICENSE);
    EasyMock.expect(metadata.getSeriesIdentifier()).andReturn(SERIES);
    EasyMock.expect(metadata.getSeriesTitle()).andReturn(SERIES_TITLE);
    EasyMock.expect(metadata.getTitle()).andReturn(TITLE);
    EasyMock.replay(metadata);

    // set up mock dublin core and dcService providing it
    DublinCoreCatalog dc = EasyMock.createStrictMock(DublinCoreCatalog.class);
    EasyMock.expect(dc.hasValue(DublinCore.PROPERTY_EXTENT)).andReturn(false);
    dc.set((EName) EasyMock.anyObject(), (DublinCoreValue) EasyMock.anyObject());
    EasyMock.expect(dc.hasValue(DublinCore.PROPERTY_CREATED)).andReturn(false);
    dc.set((EName) EasyMock.anyObject(), (DublinCoreValue) EasyMock.anyObject());
    dc.toXml((ByteArrayOutputStream) EasyMock.anyObject(), EasyMock.anyBoolean());
    // EasyMock.expect(dc.getIdentifier()).andReturn("123");
    EasyMock.replay(dc);

    dcService = EasyMock.createNiceMock(DublinCoreCatalogService.class);
    EasyMock.expect(dcService.getMetadata((MediaPackage) EasyMock.anyObject())).andReturn(
            metadata);
    EasyMock.expect(
            dcService.load((InputStream) EasyMock.anyObject())).andReturn(dc);
    EasyMock.replay(dcService);
    operationHandler.setDublincoreService(dcService);

    // set up mock receipt and inspect service providing it
    job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getPayload()).andReturn(MediaPackageElementParser.getAsXml(newTrack)).anyTimes();
    EasyMock.expect(job.getId()).andReturn(new Long(123));
    EasyMock.expect(job.getStatus()).andReturn(Status.FINISHED);
    EasyMock.expect(job.getDateCreated()).andReturn(new Date());
    EasyMock.expect(job.getDateStarted()).andReturn(new Date());
    EasyMock.replay(job);

    // set up mock service registry
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job).anyTimes();
    EasyMock.replay(serviceRegistry);
    operationHandler.setServiceRegistry(serviceRegistry);

    inspectionService = EasyMock.createNiceMock(MediaInspectionService.class);
    EasyMock.expect(inspectionService.enrich((Track) EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(job);
    EasyMock.replay(inspectionService);
    operationHandler.setInspectionService(inspectionService);

    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    // workspace.delete((String) EasyMock.anyObject(), (String) EasyMock.anyObject());
    URI newURI = new URI(NEW_DC_URL);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(newURI);
    EasyMock.expect(workspace.getURI((String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(newURI);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(
            new File(getClass().getResource("/dublincore.xml").toURI()));
    EasyMock.replay(workspace);
    operationHandler.setWorkspace(workspace);
  }

  @Test
  public void testInspectOperationTrackMetadata() throws Exception {
    for (Catalog c : mp.getCatalogs()) {
      mp.remove(c);
    }
    WorkflowOperationResult result = getWorkflowOperationResult(mp);
    Track trackNew = result.getMediaPackage().getTracks()[0];

    // check track metadata
    Assert.assertNotNull(trackNew.getChecksum());
    Assert.assertNotNull(trackNew.getMimeType());
    Assert.assertNotNull(trackNew.getDuration());
    Assert.assertNotNull(trackNew.getStreams());
  }

  @Test
  public void testInspectOperationDCMetadata() throws Exception {
    WorkflowOperationResult result = getWorkflowOperationResult(mp);
    Catalog cat = result.getMediaPackage().getCatalogs()[0];
    // dublincore check: also checked with strict mock calls
    Assert.assertEquals(NEW_DC_URL, cat.getURI().toString());
  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp) throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    // Run the media package through the operation handler, ensuring that metadata gets added
    return operationHandler.start(workflowInstance, null);
  }
}
