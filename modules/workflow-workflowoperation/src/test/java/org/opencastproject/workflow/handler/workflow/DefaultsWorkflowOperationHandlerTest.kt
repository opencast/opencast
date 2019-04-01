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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workflow.handler.workflow

import org.junit.Assert.assertEquals

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.presets.api.PresetProvider
import org.opencastproject.security.api.Organization
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.util.ArrayList
import java.util.HashMap

class DefaultsWorkflowOperationHandlerTest {

    /** The operation handler  */
    private var operationHandler: DefaultsWorkflowOperationHandler? = null

    // local resources
    private var mp: MediaPackage? = null

    /** The preset provider to use  */
    private var presetProvider: PresetProvider? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        mp = builder.createNew()

        // set up service
        operationHandler = DefaultsWorkflowOperationHandler()
    }

    @Test
    @Throws(Exception::class)
    fun testDefault() {
        // Workflow configuration
        val workflowConfiguration = HashMap<String, String>()

        // Operation configuration
        val operationConfiguration = HashMap<String, String>()
        operationConfiguration[OPT_KEY] = DEFAULT_VALUE

        // Run the operation handler
        val workflowOperationResult = getWorkflowOperationResult(mp, workflowConfiguration,
                operationConfiguration)
        val configurationValue = workflowOperationResult.properties[OPT_KEY]

        // Make sure the default value has been applied
        Assert.assertEquals(DEFAULT_VALUE, configurationValue)
    }

    @Test
    @Throws(Exception::class)
    fun testDontOverwriteExisting() {

        // Workflow configuration
        val workflowConfiguration = HashMap<String, String>()
        workflowConfiguration[OPT_KEY] = "initial"

        // Operation configuration
        val operationConfiguration = HashMap<String, String>()
        operationConfiguration[OPT_KEY] = DEFAULT_VALUE

        // Run the operation handler
        val workflowOperationResult = getWorkflowOperationResult(mp, workflowConfiguration,
                operationConfiguration)
        val configurationValue = workflowOperationResult.properties[OPT_KEY]

        // Make sure the default value has been applied
        Assert.assertNotEquals(DEFAULT_VALUE, configurationValue)
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun usesEventLevelPreset() {
        val seriesID = "series-ID"

        val workflowConfiguration = HashMap<String, String>()
        workflowConfiguration[OPT_KEY] = WORKFLOW_PRESET_VALUE

        val workflowInstance = setupInstance(seriesID, workflowConfiguration, true)

        val handler = DefaultsWorkflowOperationHandler()
        handler.setPresetProvider(presetProvider)
        val result = handler.start(workflowInstance, null)
        assertEquals(EVENT_PRESET_VALUE, result.properties[OPT_KEY])
    }

    @Test
    @Throws(WorkflowOperationException::class, NotFoundException::class)
    fun usesSeriesLevelPreset() {
        val organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.replay(organization)

        val seriesID = "series-ID"

        presetProvider = EasyMock.createMock<PresetProvider>(PresetProvider::class.java)
        EasyMock.expect(presetProvider!!.getProperty(seriesID, OPT_KEY)).andReturn(SERIES_PRESET_VALUE)
        EasyMock.replay(presetProvider!!)

        // Workflow configuration
        val workflowConfiguration = HashMap<String, String>()
        workflowConfiguration[OPT_KEY] = WORKFLOW_PRESET_VALUE

        val workflowInstance = setupInstance(seriesID, workflowConfiguration, false)

        val handler = DefaultsWorkflowOperationHandler()
        handler.setPresetProvider(presetProvider)
        val result = handler.start(workflowInstance, null)
        assertEquals(SERIES_PRESET_VALUE, result.properties[OPT_KEY])
    }

    @Test
    @Throws(WorkflowOperationException::class, NotFoundException::class)
    fun usesOrganizationLevelPreset() {
        val organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.replay(organization)

        val seriesID = "series-ID"

        presetProvider = EasyMock.createMock<PresetProvider>(PresetProvider::class.java)
        EasyMock.expect(presetProvider!!.getProperty(seriesID, OPT_KEY)).andReturn(ORGANIZATION_PRESET_VALUE)
        EasyMock.replay(presetProvider!!)

        // Workflow configuration
        val workflowConfiguration = HashMap<String, String>()
        workflowConfiguration[OPT_KEY] = WORKFLOW_PRESET_VALUE

        val workflowInstance = setupInstance(seriesID, workflowConfiguration, false)

        val handler = DefaultsWorkflowOperationHandler()
        handler.setPresetProvider(presetProvider)
        val result = handler.start(workflowInstance, null)
        assertEquals(ORGANIZATION_PRESET_VALUE, result.properties[OPT_KEY])
    }

    @Test
    @Throws(WorkflowOperationException::class, NotFoundException::class)
    fun usesOrganizationLevelPresetNullSeries() {
        val organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.replay(organization)

        val seriesID: String? = null

        presetProvider = EasyMock.createMock<PresetProvider>(PresetProvider::class.java)
        EasyMock.expect(presetProvider!!.getProperty(seriesID!!, OPT_KEY)).andReturn(ORGANIZATION_PRESET_VALUE)
        EasyMock.replay(presetProvider!!)

        // Workflow configuration
        val workflowConfiguration = HashMap<String, String>()
        workflowConfiguration[OPT_KEY] = WORKFLOW_PRESET_VALUE

        val workflowInstance = setupInstance(seriesID, workflowConfiguration, false)

        val handler = DefaultsWorkflowOperationHandler()
        handler.setPresetProvider(presetProvider)
        val result = handler.start(workflowInstance, null)
        assertEquals(ORGANIZATION_PRESET_VALUE, result.properties[OPT_KEY])
    }

    @Test
    @Throws(WorkflowOperationException::class, NotFoundException::class)
    fun usesWorkflowLevelPresetDueToNotFound() {
        val organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.replay(organization)

        val seriesID = "series-ID"

        presetProvider = EasyMock.createMock<PresetProvider>(PresetProvider::class.java)
        EasyMock.expect(presetProvider!!.getProperty(seriesID, OPT_KEY)).andThrow(NotFoundException())
        EasyMock.replay(presetProvider!!)

        // Workflow configuration
        val workflowConfiguration = HashMap<String, String>()
        workflowConfiguration[OPT_KEY] = WORKFLOW_PRESET_VALUE

        val workflowInstance = setupInstance(seriesID, workflowConfiguration, false)

        val handler = DefaultsWorkflowOperationHandler()
        handler.setPresetProvider(presetProvider)
        val result = handler.start(workflowInstance, null)
        assertEquals(WORKFLOW_PRESET_VALUE, result.properties[OPT_KEY])
    }

    @Test
    @Throws(WorkflowOperationException::class, NotFoundException::class)
    fun usesWorkflowLevelPreset() {
        val organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.replay(organization)

        val seriesID = "series-ID"

        presetProvider = EasyMock.createMock<PresetProvider>(PresetProvider::class.java)
        EasyMock.expect(presetProvider!!.getProperty(seriesID, OPT_KEY)).andReturn(null)
        EasyMock.replay(presetProvider!!)

        // Workflow configuration
        val workflowConfiguration = HashMap<String, String>()
        workflowConfiguration[OPT_KEY] = WORKFLOW_PRESET_VALUE

        val workflowInstance = setupInstance(seriesID, workflowConfiguration, false)

        val handler = DefaultsWorkflowOperationHandler()
        handler.setPresetProvider(presetProvider)
        val result = handler.start(workflowInstance, null)
        assertEquals(WORKFLOW_PRESET_VALUE, result.properties[OPT_KEY])
    }

    /**
     * Setup a workflow instance to test the preset values.
     *
     * @param seriesID
     * The series id to get the presets.
     * @param workflowConfiguration
     * A workflow configuration to get the workflow keys and values.
     * @param provideEventValue
     * Whether to provide an event level value for the key
     * @return A [WorkflowInstance] ready to run a test of [DefaultsWorkflowOperationHandler]
     */
    private fun setupInstance(seriesID: String, workflowConfiguration: Map<String, String>, provideEventValue: Boolean): WorkflowInstance {
        val operation = EasyMock.createMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        EasyMock.expect(operation.configurationKeys).andReturn(workflowConfiguration.keys)
        EasyMock.expect(operation.getConfiguration(OPT_KEY)).andReturn(WORKFLOW_PRESET_VALUE)
        EasyMock.replay(operation)

        val mediaPackage = EasyMock.createMock<MediaPackage>(MediaPackage::class.java)
        EasyMock.expect(mediaPackage.series).andReturn(seriesID)
        EasyMock.replay(mediaPackage)

        val workflowInstance = EasyMock.createMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance.id).andReturn(1L).anyTimes()
        EasyMock.expect(workflowInstance.currentOperation).andReturn(operation).anyTimes()
        EasyMock.expect(workflowInstance.organizationId).andReturn("org1").anyTimes()
        EasyMock.expect(workflowInstance.mediaPackage).andReturn(mediaPackage).anyTimes()
        if (provideEventValue) {
            EasyMock.expect(workflowInstance.getConfiguration(OPT_KEY)).andReturn(EVENT_PRESET_VALUE)
        } else {
            EasyMock.expect(workflowInstance.getConfiguration(OPT_KEY)).andReturn(null)
        }
        EasyMock.replay(workflowInstance)
        return workflowInstance
    }

    @Throws(WorkflowOperationException::class)
    private fun getWorkflowOperationResult(mp: MediaPackage?,
                                           workflowConfiguration: Map<String, String>, operationConfiguration: Map<String, String>): WorkflowOperationResult {

        // Add the mediapackage to a workflow instance
        val workflowInstance = WorkflowInstanceImpl()
        workflowInstance.id = 1
        workflowInstance.state = WorkflowState.RUNNING
        workflowInstance.mediaPackage = mp

        // Apply the workflow configuration
        for ((key, value) in workflowConfiguration) {
            workflowInstance.setConfiguration(key, value)
        }

        val operation = WorkflowOperationInstanceImpl()
        operation.template = "defaults"
        operation.state = OperationState.RUNNING

        // Apply the workflow operation configuration
        for ((key, value) in operationConfiguration) {
            operation.setConfiguration(key, value)
        }

        val operationsList = ArrayList<WorkflowOperationInstance>()
        operationsList.add(operation)
        workflowInstance.operations = operationsList

        // Run the media package through the operation handler, ensuring that metadata gets added
        val result = operationHandler!!.start(workflowInstance, null)
        Assert.assertEquals(result.action, Action.CONTINUE)
        return result

    }

    companion object {

        /** The configuration key to test  */
        private val OPT_KEY = "key"

        /** The default value for the configuration key  */
        private val DEFAULT_VALUE = "default"

        /** The default value for an event level preset.  */
        private val EVENT_PRESET_VALUE = "EventValue"

        /** The default value for a series level preset.  */
        private val SERIES_PRESET_VALUE = "SeriesValue"

        /** The default value for an organization level preset.  */
        private val ORGANIZATION_PRESET_VALUE = "OrganizationValue"

        /** The default value for a workflow level preset.  */
        private val WORKFLOW_PRESET_VALUE = "WorkflowValue"
    }

}
