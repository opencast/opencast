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

import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreValue
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.series.api.SeriesService
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.data.Opt

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.easymock.Capture
import org.easymock.CaptureType
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Arrays

/**
 * Test class for [SeriesWorkflowOperationHandler]
 */
class SeriesWorkflowOperationHandlerTest {

    private var operationHandler: SeriesWorkflowOperationHandler? = null
    private var seriesCatalog: DublinCoreCatalog? = null
    private var mp: MediaPackage? = null
    private var capturedStream: Capture<InputStream>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        mp = builder.loadFromXml(javaClass.getResourceAsStream("/series_mediapackage.xml"))
        val uri = javaClass.getResource("/dublincore.xml").toURI()
        val file = File(uri)

        seriesCatalog = DublinCores.mkOpencast().catalog
        seriesCatalog!![DublinCore.PROPERTY_TITLE] = "Series 1"

        val seriesService = EasyMock.createNiceMock<SeriesService>(SeriesService::class.java)
        EasyMock.expect(seriesService.getSeries(EasyMock.anyString())).andReturn(seriesCatalog).anyTimes()
        EasyMock.expect(seriesService.getSeriesAccessControl(EasyMock.anyString())).andReturn(AccessControlList())
                .anyTimes()
        EasyMock.expect(seriesService.getSeriesElementData(EasyMock.anyString(), EasyMock.anyString()))
                .andReturn(Opt.some(FileUtils.readFileToByteArray(file))).anyTimes()
        EasyMock.replay(seriesService)

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect<Organization>(securityService.organization).andReturn(DefaultOrganization()).anyTimes()
        EasyMock.replay(securityService)

        capturedStream = Capture.newInstance(CaptureType.FIRST)
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject(URI::class.java))).andReturn(file).anyTimes()
        EasyMock.expect(workspace.read(EasyMock.anyObject(URI::class.java)))
                .andAnswer { javaClass.getResourceAsStream("/dublincore.xml") }.anyTimes()
        EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
                EasyMock.capture(capturedStream))).andReturn(uri).anyTimes()
        EasyMock.replay(workspace)

        val authorizationService = EasyMock.createNiceMock<AuthorizationService>(AuthorizationService::class.java)
        EasyMock.replay(authorizationService)

        val adapter = EasyMock.createNiceMock<SeriesCatalogUIAdapter>(SeriesCatalogUIAdapter::class.java)
        EasyMock.expect(adapter.organization).andReturn(DefaultOrganization().id).anyTimes()
        EasyMock.expect(adapter.flavor).andReturn("creativecommons/series").anyTimes()
        EasyMock.replay(adapter)

        val seriesAdapter = EasyMock.createNiceMock<SeriesCatalogUIAdapter>(SeriesCatalogUIAdapter::class.java)
        EasyMock.expect(seriesAdapter.organization).andReturn(DefaultOrganization().id).anyTimes()
        EasyMock.expect(seriesAdapter.flavor).andReturn("dublincore/series").anyTimes()
        EasyMock.replay(seriesAdapter)

        // set up the handler
        operationHandler = SeriesWorkflowOperationHandler()
        operationHandler!!.setSeriesService(seriesService)
        operationHandler!!.setSecurityService(securityService)
        operationHandler!!.setWorkspace(workspace)
        operationHandler!!.setAuthorizationService(authorizationService)
        operationHandler!!.addCatalogUIAdapter(adapter)
        operationHandler!!.addCatalogUIAdapter(seriesAdapter)
    }

    @Test
    @Throws(Exception::class)
    fun testNoSeries() {
        val instance = WorkflowInstanceImpl()
        val ops = ArrayList<WorkflowOperationInstance>()
        val operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops
        instance.mediaPackage = mp

        operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "*")
        operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "true")

        val result = operationHandler!!.start(instance, null)
        Assert.assertEquals(Action.SKIP, result.action)
    }

    @Test
    @Throws(Exception::class)
    fun testAclOnly() {
        val instance = WorkflowInstanceImpl()
        val ops = ArrayList<WorkflowOperationInstance>()
        val operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops
        instance.mediaPackage = mp

        operation.setConfiguration(SeriesWorkflowOperationHandler.SERIES_PROPERTY, "series1")
        operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "")
        operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "true")

        val result = operationHandler!!.start(instance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)
    }

    @Test
    @Throws(Exception::class)
    fun testChangeSeries() {
        val instance = WorkflowInstanceImpl()
        val ops = ArrayList<WorkflowOperationInstance>()
        val operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops
        instance.mediaPackage = mp
        val clone = mp!!.clone() as MediaPackage

        operation.setConfiguration(SeriesWorkflowOperationHandler.SERIES_PROPERTY, "series1")
        operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "*")
        operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "false")

        val result = operationHandler!!.start(instance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)
        val resultingMediapackage = result.mediaPackage
        Assert.assertEquals("series1", resultingMediapackage.series)
        Assert.assertEquals("Series 1", resultingMediapackage.seriesTitle)
        Assert.assertEquals((clone.elements.size + 1).toLong(), resultingMediapackage.elements.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAttachExtendedOnly() {
        val instance = WorkflowInstanceImpl()
        val ops = ArrayList<WorkflowOperationInstance>()
        val operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops
        instance.mediaPackage = mp
        val clone = mp!!.clone() as MediaPackage

        operation.setConfiguration(SeriesWorkflowOperationHandler.SERIES_PROPERTY, "series1")
        operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "creativecommons/*")
        operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "false")

        val result = operationHandler!!.start(instance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)
        val resultingMediapackage = result.mediaPackage
        Assert.assertEquals("series1", resultingMediapackage.series)
        Assert.assertEquals("Series 1", resultingMediapackage.seriesTitle)
        Assert.assertEquals((clone.elements.size + 1).toLong(), resultingMediapackage.elements.size.toLong())
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun testExtraMetadata() {
        val otherProperty = EName(DublinCore.TERMS_NS_URI, "my-custom-property")
        val otherValue = "foobar"

        // Add extra metadata to the series catalog.
        seriesCatalog!![DublinCore.PROPERTY_LANGUAGE] = "Opencastian"
        seriesCatalog!![otherProperty] = otherValue
        seriesCatalog!![DublinCore.PROPERTY_CONTRIBUTOR] = Arrays.asList<T>(
                *arrayOf(DublinCoreValue.mk("Mr. Contry Bute"), DublinCoreValue.mk("Mrs. Jane Doe")))

        // Prepare "copy metadata" property
        val extraMetadata = arrayOf(
                // Append a full metadata field, with NS
                DublinCore.PROPERTY_LANGUAGE.toString(),
                // Field without namespace
                DublinCore.PROPERTY_CONTRIBUTOR.getLocalName(),
                // Field with a namespace different than the default
                otherProperty.toString(),
                // Field that does not exist in the series catalog
                "does-not-exist")

        val instance = WorkflowInstanceImpl()
        val ops = ArrayList<WorkflowOperationInstance>()
        val operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops
        instance.mediaPackage = mp
        val clone = mp!!.clone() as MediaPackage

        operation.setConfiguration(SeriesWorkflowOperationHandler.SERIES_PROPERTY, "series1")
        operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "*")
        operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "false")
        operation.setConfiguration(SeriesWorkflowOperationHandler.COPY_METADATA_PROPERTY,
                StringUtils.join(extraMetadata, ", "))

        val result = operationHandler!!.start(instance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)
        val resultingMediapackage = result.mediaPackage

        // Get episode DublinCore
        val episodeCatalog = DublinCores.read(capturedStream!!.value)

        Assert.assertEquals("series1", resultingMediapackage.series)
        Assert.assertEquals("Series 1", resultingMediapackage.seriesTitle)
        Assert.assertEquals((clone.elements.size + 1).toLong(), resultingMediapackage.elements.size.toLong())

        // Check the extra metadata were copied into the dublincore (only those present in the series catalog)
        Assert.assertTrue(episodeCatalog.hasValue(DublinCore.PROPERTY_CONTRIBUTOR))
        Assert.assertEquals(seriesCatalog!![DublinCore.PROPERTY_CONTRIBUTOR],
                episodeCatalog[DublinCore.PROPERTY_CONTRIBUTOR])
        Assert.assertTrue(episodeCatalog.hasValue(DublinCore.PROPERTY_LANGUAGE))
        Assert.assertEquals(seriesCatalog!![DublinCore.PROPERTY_LANGUAGE],
                episodeCatalog[DublinCore.PROPERTY_LANGUAGE])
        Assert.assertTrue(episodeCatalog.hasValue(otherProperty))
        Assert.assertEquals(seriesCatalog!![otherProperty], episodeCatalog[otherProperty])
        Assert.assertFalse(episodeCatalog.hasValue(EName(DublinCore.TERMS_NS_URI, "does-not-exist")))
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun testExtraMetadataDefaultNS() {

        val customProperty = EName(DublinCores.OC_PROPERTY_NS_URI, "my-custom-property")
        val customValue = "my-custom-value"

        // Add extra metadata to the series catalog.
        seriesCatalog!![DublinCore.PROPERTY_LANGUAGE] = "Opencastian"
        seriesCatalog!![DublinCore.PROPERTY_CONTRIBUTOR] = Arrays.asList<T>(
                *arrayOf(DublinCoreValue.mk("Mr. Contry Bute"), DublinCoreValue.mk("Mrs. Jane Doe")))
        seriesCatalog!![customProperty] = customValue

        // Prepare "copy metadata" property
        // All field names without namespace
        // However, in the series metadata, the third one has a different NS than the other two
        val extraMetadata = arrayOf(DublinCore.PROPERTY_LANGUAGE.getLocalName(), DublinCore.PROPERTY_CONTRIBUTOR.getLocalName(), customProperty.localName)

        val instance = WorkflowInstanceImpl()
        val ops = ArrayList<WorkflowOperationInstance>()
        val operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        ops.add(operation)
        instance.operations = ops
        instance.mediaPackage = mp
        val clone = mp!!.clone() as MediaPackage

        operation.setConfiguration(SeriesWorkflowOperationHandler.SERIES_PROPERTY, "series1")
        operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "*")
        operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "false")
        operation.setConfiguration(SeriesWorkflowOperationHandler.COPY_METADATA_PROPERTY,
                StringUtils.join(extraMetadata, ", "))
        // Set the namespace of the third, custom property as the default
        operation.setConfiguration(SeriesWorkflowOperationHandler.DEFAULT_NS_PROPERTY, DublinCores.OC_PROPERTY_NS_URI)

        val result = operationHandler!!.start(instance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)
        val resultingMediapackage = result.mediaPackage
        Assert.assertEquals("series1", resultingMediapackage.series)
        Assert.assertEquals("Series 1", resultingMediapackage.seriesTitle)
        Assert.assertEquals((clone.elements.size + 1).toLong(), resultingMediapackage.elements.size.toLong())

        // Get episode DublinCore
        val episodeCatalog = DublinCores.read(capturedStream!!.value)

        // Only the later metadatum should have been resolved. The other had a different namespace.
        Assert.assertFalse(episodeCatalog.hasValue(DublinCore.PROPERTY_CONTRIBUTOR))
        Assert.assertFalse(episodeCatalog.hasValue(DublinCore.PROPERTY_LANGUAGE))
        Assert.assertTrue(episodeCatalog.hasValue(customProperty))
        Assert.assertEquals(seriesCatalog!![customProperty],
                episodeCatalog[customProperty])
    }

}
