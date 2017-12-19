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
import java.util.Map;

/**
 * Test class for {@link TagWorkflowOperationHandler}
 */
public class ConfigureByDublinCoreTermWOHTest {

  private ConfigureByDublinCoreTermWOH operationHandler;
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
    operationHandler = new ConfigureByDublinCoreTermWOH();

    // Initialize the workflow
    instance = new WorkflowInstanceImpl();
    operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    ops.add(operation);
    instance.setOperations(ops);
    instance.setConfiguration("oldConfigProperty", "foo");
    instance.setMediaPackage(mp);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.read((URI) EasyMock.anyObject())).andReturn(
            new File(this.getClass().getResource("/dublincore.xml").toURI()));
    EasyMock.replay(workspace);
    operationHandler.setWorkspace(workspace);
  }

  @Test
  public void testMatchPresentDCTerm() throws Exception {
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "publisher");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "University of Opencast");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false");
    operation.setConfiguration("newConfigProperty", "true");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Map<String, String> properties = result.getProperties();

    Assert.assertTrue(properties.containsKey("newConfigProperty"));
    Assert.assertEquals("true", properties.get("newConfigProperty"));
  }

  @Test
  public void testMatchPresentDCTermOverwriteProperty() throws Exception {
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "publisher");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "University of Opencast");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false");
    operation.setConfiguration("oldConfigProperty", "bar");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Map<String, String> properties = result.getProperties();

    Assert.assertTrue(properties.containsKey("oldConfigProperty"));
    Assert.assertEquals("bar", properties.get("oldConfigProperty"));
  }

  @Test
  public void testMatchDefaultDCTerm() throws Exception {
    // Match == Default Value
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "source");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DEFAULT_VALUE_PROPERTY, "Timbuktu");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false");
    operation.setConfiguration("newConfigProperty", "true");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Map<String, String> properties = result.getProperties();

    Assert.assertTrue(properties.containsKey("newConfigProperty"));
    Assert.assertEquals("true", properties.get("newConfigProperty"));
  }

  @Test
  public void testMisMatchDefaultDCTerm() throws Exception {
    // Match != Default Value
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "source");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DEFAULT_VALUE_PROPERTY, "Cairo");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false");
    operation.setConfiguration("newConfigProperty", "true");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Map<String, String> properties = result.getProperties();

    Assert.assertTrue(properties == null);
  }

  @Test
  public void testMissingNoDefaultDCTerm() throws Exception {
    // No Default Value
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "source");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false");
    operation.setConfiguration("newConfigProperty", "true");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Map<String, String> properties = result.getProperties();

    Assert.assertTrue(properties == null);
  }

  @Test
  public void testNoMatchConfiguredDCTerm() throws Exception {
    // No Match or Default Value
    // without dcterm or default the match should always fail
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.DCTERM_PROPERTY, "source");
    operation.setConfiguration(ConfigureByDublinCoreTermWOH.COPY_PROPERTY, "false");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Map<String, String> properties = result.getProperties();

    Assert.assertTrue(properties == null);
  }
}
