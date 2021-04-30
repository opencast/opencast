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

package org.opencastproject.workflow.handler.composer;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.AdaptivePlaylist;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeType;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.handler.inspection.InspectWorkflowOperationHandler;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SanitizeAdaptiveWorkflowOperationHandlerTest {
  private SanitizeAdaptiveWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;
  private Job job;
  private String ingestedDir = "ingested";

  // mock services and objects
  private Workspace workspace = null;

  // constant metadata values
  private static final String SEGMENT_TRACK_ID = "track-5";
  private static final String VARIANT_TRACK_ID = "track-3";
  private static final String MASTER_TRACK_ID = "track-1";

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    File baseDir = testFolder.newFolder();
    new File(baseDir, ingestedDir);
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI uriMP = InspectWorkflowOperationHandler.class.getResource("/hls_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    // set up mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        URI uri = (URI) EasyMock.getCurrentArguments()[0];
        File file = new File(baseDir, uri.getPath());
        URL url = this.getClass().getResource(uri.getPath());
        try {
          FileUtils.copyURLToFile(url, file);
        } catch (IOException e) {
          return null;
        }
        return file;
      }
    }).anyTimes();

    // mediapackage storage
    EasyMock.expect(workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (String) EasyMock.anyObject(), (InputStream) EasyMock.anyObject())).andAnswer(new IAnswer<URI>() {
              @Override
              public URI answer() throws Throwable {
                return new URI("http://server/" + mp.getIdentifier().toString() + "/"
                        + EasyMock.getCurrentArguments()[1] + "/" + EasyMock.getCurrentArguments()[2]);
              }
            }).anyTimes();
    EasyMock.replay(workspace);

    // set up mock receipt
    job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getPayload()).andReturn(MediaPackageElementParser.getArrayAsXml(Arrays.asList(mp.getTracks())))
    .anyTimes();
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    EasyMock.expect(job.getDateCreated()).andReturn(new Date()).anyTimes();
    EasyMock.expect(job.getDateStarted()).andReturn(new Date()).anyTimes();
    EasyMock.expect(job.getQueueTime()).andReturn(new Long(0)).anyTimes();
    EasyMock.replay(job);

    // set up mock service registry
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job).anyTimes();
    EasyMock.replay(serviceRegistry);

    // set up service
    operationHandler = new SanitizeAdaptiveWorkflowOperationHandler();
    operationHandler.setJobBarrierPollingInterval(0);
    operationHandler.setWorkspace(workspace);
    operationHandler.setServiceRegistry(serviceRegistry);
  }

  @Test
  public void testFixedTrack() throws Exception {
    // operation configuration
    String targetTags = "engage,rss";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor", "presentation/source");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "presenter/delivery");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    MediaPackage mpNew = result.getMediaPackage();

    // replaced
    Assert.assertNull(mpNew.getTrack(MASTER_TRACK_ID));
    Assert.assertNull(mpNew.getTrack(VARIANT_TRACK_ID));
    // preserved
    Assert.assertNotNull(mpNew.getTrack(SEGMENT_TRACK_ID));

    // check track metadata
    // Old Tags should be overriden
    for (Track track : mpNew.getTracks()) {
      Assert.assertEquals("presenter/delivery", track.getFlavor().toString());
      Assert.assertArrayEquals(targetTags.split("\\W"), track.getTags());
      Assert.assertTrue(1300L == track.getDuration());
      Assert.assertNotNull(track.getLogicalName());
      Assert.assertNotNull(track.getURI());
    }
    List<Track> pls = Arrays.asList(mpNew.getTracks()).stream().filter(AdaptivePlaylist.isHLSTrackPred)
            .collect(Collectors.toList());
    for (Track track : pls) {
      Assert.assertEquals(track.getMimeType(), MimeType.mimeType("application", "x-mpegURL"));
    }
  }

  @Test
  public void testAddTags() throws Exception {
    // operation configuration
    String targetTags = "+cupcake";
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor", "presentation/source");
    configurations.put("target-tags", targetTags);
    configurations.put("target-flavor", "dessert/delivery");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    MediaPackage mpNew = result.getMediaPackage();

    // check track metadata
    for (Track track : mpNew.getTracks()) {
      Assert.assertEquals("dessert/delivery", track.getFlavor().toString());
      Assert.assertTrue(Arrays.asList(track.getTags()).contains("cupcake"));
      if (!SEGMENT_TRACK_ID.equals(track.getIdentifier()))
        Assert.assertTrue(track.getTags().length == 1);
    }
    // Tag should not be overriden
    Assert.assertTrue(Arrays.asList(mpNew.getTrack(SEGMENT_TRACK_ID).getTags()).contains("archive"));
  }

  @Test
  public void testNoTargetTags() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put("source-flavor", "presentation/source");
    configurations.put("target-tags", "");
    configurations.put("target-flavor", "dessert/delivery");
    String[] expectedTags = { "engage", "archive", "multiaudio" };

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    MediaPackage mpNew = result.getMediaPackage();

    // check track metadata
    for (Track track : mpNew.getTracks()) {
      Assert.assertEquals("dessert/delivery", track.getFlavor().toString());
      if (!SEGMENT_TRACK_ID.equals(track.getIdentifier()))
        Assert.assertTrue(track.getTags().length == 0);
    }
    Assert.assertEquals(mpNew.getTrack(SEGMENT_TRACK_ID).getTags().length, 3);
    Assert.assertTrue(
            Arrays.asList(mpNew.getTrack(SEGMENT_TRACK_ID).getTags()).containsAll(Arrays.asList(expectedTags)));
  }

  @Test
  public void testFlavorMinusTag() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    String[] expectedTags = { "engage", "multiaudio" };
    configurations.put("target-tags", "-archive");
    configurations.put("source-flavor", "presentation/source");
    configurations.put("target-flavor", "*/delivery");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    MediaPackage mpNew = result.getMediaPackage();
    // check track metadata
    for (Track track : mpNew.getTracks()) {
      Assert.assertEquals("presentation/delivery", track.getFlavor().toString());
      if (!SEGMENT_TRACK_ID.equals(track.getIdentifier()))
        Assert.assertTrue(track.getTags().length == 0);
    }
    Assert.assertArrayEquals(mpNew.getTrack(SEGMENT_TRACK_ID).getTags(), expectedTags);
  }

  @Test
  public void testSubFlavorNoTags() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    String[] expectedTags = { "engage", "archive", "multiaudio" };
    configurations.put("source-flavor", "presentation/source");
    configurations.put("target-flavor", "*/*");

    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    MediaPackage mpNew = result.getMediaPackage();
    // check track metadata
    for (Track track : mpNew.getTracks()) {
      Assert.assertEquals("presentation/source", track.getFlavor().toString());
      if (!SEGMENT_TRACK_ID.equals(track.getIdentifier()))
        Assert.assertTrue(track.getTags().length == 0);
    }
    Assert.assertTrue(
            Arrays.asList(mpNew.getTrack(SEGMENT_TRACK_ID).getTags()).containsAll(Arrays.asList(expectedTags)));
  }

  @Test
  public void testMissingTargetFlavor() throws Exception {
    // set up mock profile
    Map<String, String> configurations = new HashMap<String, String>();
    try {
      // no encoding profile
      configurations.put("source-flavor", "presentation/source");
      getWorkflowOperationResult(mp, configurations);
      Assert.fail("Since target flavor is not specified exception should be thrown");
    } catch (WorkflowOperationException e) {
      // expecting exception
    }

  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
          throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    operation.setTemplate("sanitize hls");
    operation.setState(OperationState.RUNNING);
    for (String key : configurations.keySet()) {
      operation.setConfiguration(key, configurations.get(key));
    }

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    // Run the media package through the operation handler, ensuring that metadata gets added
    return operationHandler.start(workflowInstance, null);
  }

}
