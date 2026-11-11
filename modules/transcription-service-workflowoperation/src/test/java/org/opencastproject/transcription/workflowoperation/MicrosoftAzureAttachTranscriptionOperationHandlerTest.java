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

package org.opencastproject.transcription.workflowoperation;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.transcription.api.TranscriptionService;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MicrosoftAzureAttachTranscriptionOperationHandlerTest {

  /** The operation handler to test */
  private MicrosoftAzureAttachTranscriptionOperationHandler operationHandler;

  /** The workflow instance */
  private WorkflowInstance workflowInstance;

  /** The operation instance */
  private WorkflowOperationInstance operation;

  @Before
  public void setUp() {
    Map<String, String> orgProperties = new HashMap<>();
    orgProperties.put("transcriber", "azure-1");
    // Collides with the workflow property 'series_id' below: the workflow property must win
    orgProperties.put("series_id", "org-should-not-win");
    // A dotted property name, as found in the default organization configuration
    orgProperties.put("my.var", "dotted-value");
    Organization organization = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(organization.getProperties()).andStubReturn(orgProperties);
    EasyMock.replay(organization);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andStubReturn(organization);
    EasyMock.replay(securityService);

    operationHandler = new MicrosoftAzureAttachTranscriptionOperationHandler();
    operationHandler.setSecurityService(securityService);
  }

  /**
   * Sets up a workflow instance with the given workflow properties and operation configurations, and a transcription
   * service that returns the given track.
   */
  private void setUpWorkflow(Map<String, String> workflowProperties, Map<String, String> operationConfigurations,
          Track track) throws Exception {
    MediaPackage mediaPackage =
        MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(new IdImpl("mpId1"));

    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("DCE-azure-attach-transcription");
    workflowInstance = new WorkflowInstance(def, mediaPackage, null, null, workflowProperties);
    workflowInstance.setId(1);
    operation = new WorkflowOperationInstance("azure-attach", OperationState.RUNNING);
    operationConfigurations.forEach(operation::setConfiguration);
    workflowInstance.setOperations(List.of(operation));

    TranscriptionService service = EasyMock.createNiceMock(TranscriptionService.class);
    EasyMock.expect(service.getGeneratedTranscription("mpId1", "transcriptionJob", Track.TYPE)).andReturn(track);
    EasyMock.replay(service);
    operationHandler.setTranscriptionService(service);
  }

  @Test
  public void testTemplatedConfigurationKeys() {
    Set<String> expected = Set.of(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_FLAVOR,
        MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_TAGS,
        MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_TAG);
    Assert.assertEquals(expected, operationHandler.getTemplatedConfigurationKeys());
  }

  @Test
  public void fullFlavorAndTagListFromWorkflowProperties() throws Exception {
    Map<String, String> workflowProperties = new HashMap<>();
    workflowProperties.put("captionFlavor", "captions/dfxp");
    workflowProperties.put("myTags", "tag1,tag2");

    Map<String, String> operationConfigurations = new HashMap<>();
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TRANSCRIPTION_JOB_ID,
        "transcriptionJob");
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_FLAVOR, "${captionFlavor}");
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_TAGS, "${myTags}");

    Track track = new TrackImpl();
    track.setURI(new URI("http://opencast.server.com/transcription"));
    setUpWorkflow(workflowProperties, operationConfigurations, track);

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Track[] tracks = result.getMediaPackage().getTracks();
    Assert.assertEquals(1, tracks.length);
    // The property holds a complete flavor and a complete comma-separated tag list
    Assert.assertEquals(MediaPackageElementFlavor.parseFlavor("captions/dfxp"), tracks[0].getFlavor());
    Assert.assertArrayEquals(new String[] {"tag1", "tag2"}, tracks[0].getTags());
  }

  @Test
  public void templatedTargetFlavorAndTagsUseWorkflowAndOrganizationProperties() throws Exception {
    Map<String, String> workflowProperties = new HashMap<>();
    workflowProperties.put("series_id", "series1");

    Map<String, String> operationConfigurations = new HashMap<>();
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TRANSCRIPTION_JOB_ID,
        "transcriptionJob");
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_FLAVOR,
        "captions/${series_id}");
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_TAGS,
        "+transcript-${series_id},+org-${org_transcriber}");

    Track track = new TrackImpl();
    track.setURI(new URI("http://opencast.server.com/transcription"));
    setUpWorkflow(workflowProperties, operationConfigurations, track);

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Track[] tracks = result.getMediaPackage().getTracks();
    Assert.assertEquals(1, tracks.length);
    // The target flavor is rendered with the workflow properties
    Assert.assertEquals(MediaPackageElementFlavor.parseFlavor("captions/series1"), tracks[0].getFlavor());
    // The tags are rendered with the workflow properties and the organization properties; a workflow property
    // takes precedence over an organization property with the same name
    Assert.assertArrayEquals(new String[] {"org-azure-1", "transcript-series1"}, tracks[0].getTags());
  }

  @Test
  public void renderedValuesAreNotWrittenBackToTheOperation() throws Exception {
    Map<String, String> workflowProperties = new HashMap<>();
    workflowProperties.put("captionFlavor", "captions/dfxp");
    workflowProperties.put("myTags", "tag1,tag2");

    Map<String, String> operationConfigurations = new HashMap<>();
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TRANSCRIPTION_JOB_ID,
        "transcriptionJob");
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_FLAVOR, "${captionFlavor}");
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_TAGS, "${myTags}");

    Track track = new TrackImpl();
    track.setURI(new URI("http://opencast.server.com/transcription"));
    setUpWorkflow(workflowProperties, operationConfigurations, track);

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Track[] tracks = result.getMediaPackage().getTracks();
    Assert.assertEquals(MediaPackageElementFlavor.parseFlavor("captions/dfxp"), tracks[0].getFlavor());
    Assert.assertArrayEquals(new String[] {"tag1", "tag2"}, tracks[0].getTags());
    // The rendered values must not be written back to the operation: the workflow is persisted after the
    // operation, and a retry would then render the rendered values again, evaluating the property values
    // as template source
    Assert.assertEquals("${captionFlavor}",
        operation.getConfiguration(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_FLAVOR));
    Assert.assertEquals("${myTags}",
        operation.getConfiguration(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_TAGS));
  }

  @Test
  public void dottedOrganizationPropertyIsAvailableInTheDocumentedForm() throws Exception {
    Map<String, String> workflowProperties = new HashMap<>();

    Map<String, String> operationConfigurations = new HashMap<>();
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TRANSCRIPTION_JOB_ID,
        "transcriptionJob");
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_FLAVOR, "captions/dfxp");
    // The dotted form documented in the workflow configuration guide
    operationConfigurations.put(MicrosoftAzureAttachTranscriptionOperationHandler.TARGET_TAGS, "${org_my.var}");

    Track track = new TrackImpl();
    track.setURI(new URI("http://opencast.server.com/transcription"));
    setUpWorkflow(workflowProperties, operationConfigurations, track);

    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Track[] tracks = result.getMediaPackage().getTracks();
    Assert.assertArrayEquals(new String[] {"dotted-value"}, tracks[0].getTags());
  }
}
