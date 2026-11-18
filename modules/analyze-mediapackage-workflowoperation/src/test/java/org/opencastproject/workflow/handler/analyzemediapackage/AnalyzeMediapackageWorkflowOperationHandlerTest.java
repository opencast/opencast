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

package org.opencastproject.workflow.handler.analyzemediapackage;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

/**
 * Unit test class for {@link AnalyzeMediapackageWorkflowOperationHandler}.
 */
public class AnalyzeMediapackageWorkflowOperationHandlerTest {

  private static MediaPackageBuilder mediaPackageBuilder = null;
  private static MediaPackageElementBuilder mediaPackageElementBuilder = null;
  private MediaPackage mediaPackage = null;

  @BeforeAll
  public static void setupClass() {
    mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mediaPackageElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
  }

  @BeforeEach
  public void setup() throws MediaPackageException {
    mediaPackage = mediaPackageBuilder.createNew();
  }

  @Test
  public void testWithoutConfiguration() throws WorkflowOperationException {
    mediaPackage.add(mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Catalog,
        MediaPackageElementFlavor.flavor("dublincore", "episode")));
    mediaPackage.add(mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Attachment,
        MediaPackageElementFlavor.flavor("security", "episode+xacml")));
    mediaPackage.add(mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Track,
        MediaPackageElementFlavor.flavor("presenter", "source")));

    var operation = new WorkflowOperationInstance("test-id", WorkflowOperationInstance.OperationState.RUNNING);
    // operation.setConfiguration("source-flavor", "presenter/source");
    var workflowInstance = new WorkflowInstance();
    workflowInstance.setMediaPackage(mediaPackage);
    workflowInstance.setOperations(Collections.singletonList(operation));
    var service = new AnalyzeMediapackageWorkflowOperationHandler();
    WorkflowOperationResult result = service.start(workflowInstance, null);
    var wfProperties = result.getProperties();

    Assertions.assertTrue(Strings.CS.equals("true", wfProperties.get("dublincore_episode_exists")),
        "Workflowproperty 'dublincore_episode_exists' must be set to 'true'.");
    Assertions.assertTrue(Strings.CS.equals("Catalog", wfProperties.get("dublincore_episode_type")),
        "Workflowproperty 'dublincore_episode_type' must be set to 'Catalog'.");

    Assertions.assertTrue(Strings.CS.equals("true", wfProperties.get("security_episode+xacml_exists")),
        "Workflowproperty 'security_episode+xacml_exists' must be set to 'true'.");
    Assertions.assertTrue(Strings.CS.equals("Attachment", wfProperties.get("security_episode+xacml_type")),
        "Workflowproperty 'security_episode+xacml_type' must be set to 'Attachment'.");

    Assertions.assertTrue(Strings.CS.equals("true", wfProperties.get("presenter_source_exists")),
        "Workflowproperty 'presenter_source_exists' must be set to 'true'.");
    Assertions.assertTrue(Strings.CS.equals("Track", wfProperties.get("presenter_source_type")),
        "Workflowproperty 'presenter_source_type' must be set to 'Track'.");

    Assertions.assertFalse(wfProperties.containsKey("foo_bar_exists"),
        "Workflowproperty 'foo_bar_exists' must not exists.");
  }

  @Test
  public void testWithConfiguration() throws WorkflowOperationException {
    mediaPackage.add(mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Catalog,
        MediaPackageElementFlavor.flavor("dublincore", "episode")));
    mediaPackage.add(mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Attachment,
        MediaPackageElementFlavor.flavor("security", "episode+xacml")));
    mediaPackage.add(mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Track,
        MediaPackageElementFlavor.flavor("presenter", "source")));

    var operation = new WorkflowOperationInstance("test-id", WorkflowOperationInstance.OperationState.RUNNING);
    operation.setConfiguration("source-flavor", "presenter/source");
    var workflowInstance = new WorkflowInstance();
    workflowInstance.setMediaPackage(mediaPackage);
    workflowInstance.setOperations(Collections.singletonList(operation));
    var service = new AnalyzeMediapackageWorkflowOperationHandler();
    WorkflowOperationResult result = service.start(workflowInstance, null);
    var wfProperties = result.getProperties();

    Assertions.assertTrue(Strings.CS.equals("true", wfProperties.get("presenter_source_exists")),
        "Workflowproperty 'presenter_source_exists' must be set to 'true'.");
    Assertions.assertTrue(Strings.CS.equals("Track", wfProperties.get("presenter_source_type")),
        "Workflowproperty 'presenter_source_type' must be set to 'Track'.");

    Assertions.assertFalse(wfProperties.containsKey("dublincore_episode_exists"),
        "Workflowproperty 'dublincore_episode_exists' must not exists.");
    Assertions.assertFalse(wfProperties.containsKey("security_episode+xacml_exists"),
        "Workflowproperty 'security_episode+xacml_exists' must not exists.");
    Assertions.assertFalse(wfProperties.containsKey("foo_bar_exists"),
        "Workflowproperty 'foo_bar_exists' must not exists.");
  }

  @Test
  public void testWithFaultyConfiguration() throws WorkflowOperationException {
    mediaPackage.add(mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Catalog,
        MediaPackageElementFlavor.flavor("dublincore", "episode")));
    mediaPackage.add(mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Attachment,
        MediaPackageElementFlavor.flavor("security", "episode+xacml")));
    mediaPackage.add(mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Track,
        MediaPackageElementFlavor.flavor("presenter", "source")));

    var operation = new WorkflowOperationInstance("test-id", WorkflowOperationInstance.OperationState.RUNNING);
    operation.setConfiguration("source-flavor", "foo/bar");
    var workflowInstance = new WorkflowInstance();
    workflowInstance.setMediaPackage(mediaPackage);
    workflowInstance.setOperations(Collections.singletonList(operation));
    var service = new AnalyzeMediapackageWorkflowOperationHandler();
    WorkflowOperationResult result = service.start(workflowInstance, null);
    var wfProperties = result.getProperties();

    Assertions.assertTrue(wfProperties.isEmpty(), "Workflowproperties must be empty.");
  }

  @Test
  public void testWithEmptyMediapackage() throws WorkflowOperationException {
    var operation = new WorkflowOperationInstance("test-id", WorkflowOperationInstance.OperationState.RUNNING);
    operation.setConfiguration("source-flavor", "foo/bar");
    var workflowInstance = new WorkflowInstance();
    workflowInstance.setMediaPackage(mediaPackage);
    workflowInstance.setOperations(Collections.singletonList(operation));
    var service = new AnalyzeMediapackageWorkflowOperationHandler();
    WorkflowOperationResult result = service.start(workflowInstance, null);
    var wfProperties = result.getProperties();

    Assertions.assertTrue(wfProperties.isEmpty(), "Workflowproperties must be empty.");
  }
}
