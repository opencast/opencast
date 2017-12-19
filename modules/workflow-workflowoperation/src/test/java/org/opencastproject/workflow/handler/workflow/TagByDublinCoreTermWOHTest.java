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
package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for {@link TagWorkflowOperationHandler}
 */
public class TagByDublinCoreTermWOHTest {

  private TagByDublinCoreTermWOH operationHandler;
  private WorkflowInstanceImpl instance;
  private WorkflowOperationInstanceImpl operation;
  private MediaPackage mp;
  private Workspace workspace;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mp = builder.loadFromXml(this.getClass().getResourceAsStream("/archive_mediapackage.xml"));
    mp.getCatalog("catalog-1").setURI(this.getClass().getResource("/dublincore.xml").toURI());

    // set up the handler
    operationHandler = new TagByDublinCoreTermWOH();

    // Initialize the workflow
    instance = new WorkflowInstanceImpl();
    operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(mp);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.read((URI) EasyMock.anyObject())).andReturn(
            new File(this.getClass().getResource("/dublincore.xml").toURI()));
    EasyMock.replay(workspace);
    operationHandler.setWorkspace(workspace);
  }

  @Test
  public void testMatchPresentDCTerm() throws Exception {
    operation.setConfiguration(TagByDublinCoreTermWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/*");
    operation.setConfiguration(TagByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(TagByDublinCoreTermWOH.DCTERM_PROPERTY, "publisher");
    operation.setConfiguration(TagByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "University of Opencast");
    operation.setConfiguration(TagByDublinCoreTermWOH.TARGET_TAGS_PROPERTY, "tag1,tag2");
    operation.setConfiguration(TagByDublinCoreTermWOH.COPY_PROPERTY, "false");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    MediaPackage resultingMediapackage = result.getMediaPackage();

    Catalog catalog = resultingMediapackage.getCatalog("catalog-1");
    Assert.assertEquals("tag1", catalog.getTags()[0]);
    Assert.assertEquals("tag2", catalog.getTags()[1]);
  }

  @Test
  public void testMatchDefaultDCTerm() throws Exception {
    // Match == Default Value
    operation.setConfiguration(TagByDublinCoreTermWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/*");
    operation.setConfiguration(TagByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(TagByDublinCoreTermWOH.DCTERM_PROPERTY, "source");
    operation.setConfiguration(TagByDublinCoreTermWOH.DEFAULT_VALUE_PROPERTY, "Timbuktu");
    operation.setConfiguration(TagByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu");
    operation.setConfiguration(TagByDublinCoreTermWOH.TARGET_TAGS_PROPERTY, "tag1,tag2");
    operation.setConfiguration(TagByDublinCoreTermWOH.COPY_PROPERTY, "false");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    MediaPackage resultingMediapackage = result.getMediaPackage();

    Catalog catalog = resultingMediapackage.getCatalog("catalog-1");
    Assert.assertEquals(2, catalog.getTags().length);
    Assert.assertEquals("tag1", catalog.getTags()[0]);
    Assert.assertEquals("tag2", catalog.getTags()[1]);
  }

  @Test
  public void testMisMatchDefaultDCTerm() throws Exception {
    // Match != Default Value
    operation.setConfiguration(TagByDublinCoreTermWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/*");
    operation.setConfiguration(TagByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(TagByDublinCoreTermWOH.DCTERM_PROPERTY, "source");
    operation.setConfiguration(TagByDublinCoreTermWOH.DEFAULT_VALUE_PROPERTY, "Cairo");
    operation.setConfiguration(TagByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu");
    operation.setConfiguration(TagByDublinCoreTermWOH.TARGET_TAGS_PROPERTY, "tag1,tag2");
    operation.setConfiguration(TagByDublinCoreTermWOH.COPY_PROPERTY, "false");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    MediaPackage resultingMediapackage = result.getMediaPackage();

    Catalog catalog = resultingMediapackage.getCatalog("catalog-1");
    Assert.assertEquals(1, catalog.getTags().length);
    Assert.assertEquals("archive", catalog.getTags()[0]);
  }

  @Test
  public void testMissingNoDefaultDCTerm() throws Exception {
    // No Default Value
    operation.setConfiguration(TagByDublinCoreTermWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/*");
    operation.setConfiguration(TagByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(TagByDublinCoreTermWOH.DCTERM_PROPERTY, "source");
    operation.setConfiguration(TagByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu");
    operation.setConfiguration(TagByDublinCoreTermWOH.TARGET_TAGS_PROPERTY, "tag1,tag2");
    operation.setConfiguration(TagByDublinCoreTermWOH.COPY_PROPERTY, "false");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    MediaPackage resultingMediapackage = result.getMediaPackage();

    Catalog catalog = resultingMediapackage.getCatalog("catalog-1");
    Assert.assertEquals(1, catalog.getTags().length);
    Assert.assertEquals("archive", catalog.getTags()[0]);
  }

  @Test
  public void testNoMatchConfiguredDCTerm() throws Exception {
    // No Match or Default Value
    // without dcterm or default the match should always fail
    operation.setConfiguration(TagByDublinCoreTermWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/*");
    operation.setConfiguration(TagByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(TagByDublinCoreTermWOH.DCTERM_PROPERTY, "source");
    operation.setConfiguration(TagByDublinCoreTermWOH.TARGET_TAGS_PROPERTY, "tag1,tag2");
    operation.setConfiguration(TagByDublinCoreTermWOH.COPY_PROPERTY, "false");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    MediaPackage resultingMediapackage = result.getMediaPackage();

    Catalog catalog = resultingMediapackage.getCatalog("catalog-1");
    Assert.assertEquals(1, catalog.getTags().length);
    Assert.assertEquals("archive", catalog.getTags()[0]);
  }
}
