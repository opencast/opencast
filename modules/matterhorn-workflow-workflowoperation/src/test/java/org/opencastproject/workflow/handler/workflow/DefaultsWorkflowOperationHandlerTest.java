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
package org.opencastproject.workflow.handler.workflow;

import static org.junit.Assert.assertEquals;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.presets.api.PresetProvider;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultsWorkflowOperationHandlerTest {

  /** The operation handler */
  private DefaultsWorkflowOperationHandler operationHandler;

  /** The configuration key to test */
  private static final String OPT_KEY = "key";

  /** The default value for the configuration key */
  private static final String DEFAULT_VALUE = "default";

  /** The default value for an event level preset. */
  private static final String EVENT_PRESET_VALUE = "EventValue";

  /** The default value for a series level preset. */
  private static final String SERIES_PRESET_VALUE = "SeriesValue";

  /** The default value for an organization level preset. */
  private static final String ORGANIZATION_PRESET_VALUE = "OrganizationValue";

  /** The default value for a workflow level preset. */
  private static final String WORKFLOW_PRESET_VALUE = "WorkflowValue";

  // local resources
  private MediaPackage mp;

  /** The preset provider to use */
  private PresetProvider presetProvider;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mp = builder.createNew();

    // set up service
    operationHandler = new DefaultsWorkflowOperationHandler();
  }

  @Test
  public void testDefault() throws Exception {
    // Workflow configuration
    Map<String, String> workflowConfiguration = new HashMap<String, String>();

    // Operation configuration
    Map<String, String> operationConfiguration = new HashMap<String, String>();
    operationConfiguration.put(OPT_KEY, DEFAULT_VALUE);

    // Run the operation handler
    WorkflowOperationResult workflowOperationResult = getWorkflowOperationResult(mp, workflowConfiguration,
            operationConfiguration);
    String configurationValue = workflowOperationResult.getProperties().get(OPT_KEY);

    // Make sure the default value has been applied
    Assert.assertEquals(DEFAULT_VALUE, configurationValue);
  }

  @Test
  public void testDontOverwriteExisting() throws Exception {

    // Workflow configuration
    Map<String, String> workflowConfiguration = new HashMap<String, String>();
    workflowConfiguration.put(OPT_KEY, "initial");

    // Operation configuration
    Map<String, String> operationConfiguration = new HashMap<String, String>();
    operationConfiguration.put(OPT_KEY, DEFAULT_VALUE);

    // Run the operation handler
    WorkflowOperationResult workflowOperationResult = getWorkflowOperationResult(mp, workflowConfiguration,
            operationConfiguration);
    String configurationValue = workflowOperationResult.getProperties().get(OPT_KEY);

    // Make sure the default value has been applied
    Assert.assertNotEquals(DEFAULT_VALUE, configurationValue);
  }

  @Test
  public void usesEventLevelPreset() throws WorkflowOperationException {
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.replay(organization);

    String seriesID = "series-ID";

    Map<String, String> workflowConfiguration = new HashMap<String, String>();
    workflowConfiguration.put(OPT_KEY, WORKFLOW_PRESET_VALUE);

    WorkflowInstance workflowInstance = setupInstance(organization, seriesID, workflowConfiguration, true);

    DefaultsWorkflowOperationHandler handler = new DefaultsWorkflowOperationHandler();
    handler.setPresetProvider(presetProvider);
    WorkflowOperationResult result = handler.start(workflowInstance, null);
    assertEquals(EVENT_PRESET_VALUE, result.getProperties().get(OPT_KEY));
  }

  @Test
  public void usesSeriesLevelPreset() throws WorkflowOperationException, NotFoundException {
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.replay(organization);

    String seriesID = "series-ID";

    presetProvider = EasyMock.createMock(PresetProvider.class);
    EasyMock.expect(presetProvider.getProperty(seriesID, OPT_KEY)).andReturn(SERIES_PRESET_VALUE);
    EasyMock.replay(presetProvider);

    // Workflow configuration
    Map<String, String> workflowConfiguration = new HashMap<String, String>();
    workflowConfiguration.put(OPT_KEY, WORKFLOW_PRESET_VALUE);

    WorkflowInstance workflowInstance = setupInstance(organization, seriesID, workflowConfiguration, false);

    DefaultsWorkflowOperationHandler handler = new DefaultsWorkflowOperationHandler();
    handler.setPresetProvider(presetProvider);
    WorkflowOperationResult result = handler.start(workflowInstance, null);
    assertEquals(SERIES_PRESET_VALUE, result.getProperties().get(OPT_KEY));
  }

  @Test
  public void usesOrganizationLevelPreset() throws WorkflowOperationException, NotFoundException {
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.replay(organization);

    String seriesID = "series-ID";

    presetProvider = EasyMock.createMock(PresetProvider.class);
    EasyMock.expect(presetProvider.getProperty(seriesID, OPT_KEY)).andReturn(ORGANIZATION_PRESET_VALUE);
    EasyMock.replay(presetProvider);

    // Workflow configuration
    Map<String, String> workflowConfiguration = new HashMap<String, String>();
    workflowConfiguration.put(OPT_KEY, WORKFLOW_PRESET_VALUE);

    WorkflowInstance workflowInstance = setupInstance(organization, seriesID, workflowConfiguration, false);

    DefaultsWorkflowOperationHandler handler = new DefaultsWorkflowOperationHandler();
    handler.setPresetProvider(presetProvider);
    WorkflowOperationResult result = handler.start(workflowInstance, null);
    assertEquals(ORGANIZATION_PRESET_VALUE, result.getProperties().get(OPT_KEY));
  }

  @Test
  public void usesOrganizationLevelPresetNullSeries() throws WorkflowOperationException, NotFoundException {
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.replay(organization);

    String seriesID = null;

    presetProvider = EasyMock.createMock(PresetProvider.class);
    EasyMock.expect(presetProvider.getProperty(seriesID, OPT_KEY)).andReturn(ORGANIZATION_PRESET_VALUE);
    EasyMock.replay(presetProvider);

    // Workflow configuration
    Map<String, String> workflowConfiguration = new HashMap<String, String>();
    workflowConfiguration.put(OPT_KEY, WORKFLOW_PRESET_VALUE);

    WorkflowInstance workflowInstance = setupInstance(organization, seriesID, workflowConfiguration, false);

    DefaultsWorkflowOperationHandler handler = new DefaultsWorkflowOperationHandler();
    handler.setPresetProvider(presetProvider);
    WorkflowOperationResult result = handler.start(workflowInstance, null);
    assertEquals(ORGANIZATION_PRESET_VALUE, result.getProperties().get(OPT_KEY));
  }

  @Test
  public void usesWorkflowLevelPresetDueToNotFound() throws WorkflowOperationException, NotFoundException {
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.replay(organization);

    String seriesID = "series-ID";

    presetProvider = EasyMock.createMock(PresetProvider.class);
    EasyMock.expect(presetProvider.getProperty(seriesID, OPT_KEY)).andThrow(new NotFoundException());
    EasyMock.replay(presetProvider);

    // Workflow configuration
    Map<String, String> workflowConfiguration = new HashMap<String, String>();
    workflowConfiguration.put(OPT_KEY, WORKFLOW_PRESET_VALUE);

    WorkflowInstance workflowInstance = setupInstance(organization, seriesID, workflowConfiguration, false);

    DefaultsWorkflowOperationHandler handler = new DefaultsWorkflowOperationHandler();
    handler.setPresetProvider(presetProvider);
    WorkflowOperationResult result = handler.start(workflowInstance, null);
    assertEquals(WORKFLOW_PRESET_VALUE, result.getProperties().get(OPT_KEY));
  }

  @Test
  public void usesWorkflowLevelPreset() throws WorkflowOperationException, NotFoundException {
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.replay(organization);

    String seriesID = "series-ID";

    presetProvider = EasyMock.createMock(PresetProvider.class);
    EasyMock.expect(presetProvider.getProperty(seriesID, OPT_KEY)).andReturn(null);
    EasyMock.replay(presetProvider);

    // Workflow configuration
    Map<String, String> workflowConfiguration = new HashMap<String, String>();
    workflowConfiguration.put(OPT_KEY, WORKFLOW_PRESET_VALUE);

    WorkflowInstance workflowInstance = setupInstance(organization, seriesID, workflowConfiguration, false);

    DefaultsWorkflowOperationHandler handler = new DefaultsWorkflowOperationHandler();
    handler.setPresetProvider(presetProvider);
    WorkflowOperationResult result = handler.start(workflowInstance, null);
    assertEquals(WORKFLOW_PRESET_VALUE, result.getProperties().get(OPT_KEY));
  }

  /**
   * Setup a workflow instance to test the preset values.
   * 
   * @param organization
   *          The {@link Organization} to use to get presets.
   * @param seriesID
   *          The series id to get the presets.
   * @param workflowConfiguration
   *          A workflow configuration to get the workflow keys and values.
   * @param provideEventValue
   *          Whether to provide an event level value for the key
   * @return A {@link WorkflowInstance} ready to run a test of {@link DefaultsWorkflowOperationHandler}
   */
  private WorkflowInstance setupInstance(Organization organization, String seriesID,
          Map<String, String> workflowConfiguration, boolean provideEventValue) {
    WorkflowOperationInstance operation = EasyMock.createMock(WorkflowOperationInstance.class);
    EasyMock.expect(operation.getConfigurationKeys()).andReturn(workflowConfiguration.keySet());
    EasyMock.expect(operation.getConfiguration(OPT_KEY)).andReturn(WORKFLOW_PRESET_VALUE);
    EasyMock.replay(operation);

    MediaPackage mediaPackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getSeries()).andReturn(seriesID);
    EasyMock.replay(mediaPackage);

    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(1L).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andReturn(operation).anyTimes();
    EasyMock.expect(workflowInstance.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    if (provideEventValue) {
      EasyMock.expect(workflowInstance.getConfiguration(OPT_KEY)).andReturn(EVENT_PRESET_VALUE);
    } else {
      EasyMock.expect(workflowInstance.getConfiguration(OPT_KEY)).andReturn(null);
    }
    EasyMock.replay(workflowInstance);
    return workflowInstance;
  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp,
          Map<String, String> workflowConfiguration, Map<String, String> operationConfiguration)
          throws WorkflowOperationException {

    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);

    // Apply the workflow configuration
    for (Map.Entry<String, String> entry : workflowConfiguration.entrySet()) {
      workflowInstance.setConfiguration(entry.getKey(), entry.getValue());
    }

    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl();
    operation.setTemplate("defaults");
    operation.setState(OperationState.RUNNING);

    // Apply the workflow operation configuration
    for (Map.Entry<String, String> entry : operationConfiguration.entrySet()) {
      operation.setConfiguration(entry.getKey(), entry.getValue());
    }

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    // Run the media package through the operation handler, ensuring that metadata gets added
    WorkflowOperationResult result = operationHandler.start(workflowInstance, null);
    Assert.assertEquals(result.getAction(), Action.CONTINUE);
    return result;

  }

}
