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

import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test class for {@link SeriesWorkflowOperationHandler}
 */
public class SeriesWorkflowOperationHandlerTest {

  private SeriesWorkflowOperationHandler operationHandler;
  private DublinCoreCatalog seriesCatalog;
  private MediaPackage mp;
  private Capture<InputStream> capturedStream;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mp = builder.loadFromXml(getClass().getResourceAsStream("/series_mediapackage.xml"));
    URI uri = getClass().getResource("/dublincore.xml").toURI();
    File file = new File(uri);

    seriesCatalog = DublinCores.mkOpencast().getCatalog();
    seriesCatalog.set(DublinCore.PROPERTY_TITLE, "Series 1");

    SeriesService seriesService = EasyMock.createNiceMock(SeriesService.class);
    EasyMock.expect(seriesService.getSeries(EasyMock.anyString())).andReturn(seriesCatalog).anyTimes();
    EasyMock.expect(seriesService.getSeriesAccessControl(EasyMock.anyString())).andReturn(new AccessControlList())
            .anyTimes();
    EasyMock.expect(seriesService.getSeriesElementData(EasyMock.anyString(), EasyMock.anyString()))
            .andReturn(Opt.some(FileUtils.readFileToByteArray(file))).anyTimes();
    EasyMock.replay(seriesService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    capturedStream = Capture.newInstance(CaptureType.FIRST);
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class))).andReturn(file).anyTimes();
    EasyMock.expect(workspace.read(EasyMock.anyObject(URI.class)))
            .andAnswer(() -> getClass().getResourceAsStream("/dublincore.xml")).anyTimes();
    EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.capture(capturedStream))).andReturn(uri).anyTimes();
    EasyMock.replay(workspace);

    AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.replay(authorizationService);

    SeriesCatalogUIAdapter adapter = EasyMock.createNiceMock(SeriesCatalogUIAdapter.class);
    EasyMock.expect(adapter.getOrganization()).andReturn(new DefaultOrganization().getId()).anyTimes();
    EasyMock.expect(adapter.getFlavor()).andReturn("creativecommons/series").anyTimes();
    EasyMock.replay(adapter);

    SeriesCatalogUIAdapter seriesAdapter = EasyMock.createNiceMock(SeriesCatalogUIAdapter.class);
    EasyMock.expect(seriesAdapter.getOrganization()).andReturn(new DefaultOrganization().getId()).anyTimes();
    EasyMock.expect(seriesAdapter.getFlavor()).andReturn("dublincore/series").anyTimes();
    EasyMock.replay(seriesAdapter);

    // set up the handler
    operationHandler = new SeriesWorkflowOperationHandler();
    operationHandler.setSeriesService(seriesService);
    operationHandler.setSecurityService(securityService);
    operationHandler.setWorkspace(workspace);
    operationHandler.setAuthorizationService(authorizationService);
    operationHandler.addCatalogUIAdapter(adapter);
    operationHandler.addCatalogUIAdapter(seriesAdapter);
  }

  @Test
  public void testNoSeries() throws Exception {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(mp);

    operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "*");
    operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "true");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Assert.assertEquals(Action.SKIP, result.getAction());
  }

  @Test
  public void testAclOnly() throws Exception {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(mp);

    operation.setConfiguration(SeriesWorkflowOperationHandler.SERIES_PROPERTY, "series1");
    operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "");
    operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "true");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
  }

  @Test
  public void testChangeSeries() throws Exception {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(mp);
    MediaPackage clone = (MediaPackage) mp.clone();

    operation.setConfiguration(SeriesWorkflowOperationHandler.SERIES_PROPERTY, "series1");
    operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "*");
    operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "false");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
    MediaPackage resultingMediapackage = result.getMediaPackage();
    Assert.assertEquals("series1", resultingMediapackage.getSeries());
    Assert.assertEquals("Series 1", resultingMediapackage.getSeriesTitle());
    Assert.assertEquals(clone.getElements().length + 1, resultingMediapackage.getElements().length);
  }

  @Test
  public void testAttachExtendedOnly() throws Exception {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(mp);
    MediaPackage clone = (MediaPackage) mp.clone();

    operation.setConfiguration(SeriesWorkflowOperationHandler.SERIES_PROPERTY, "series1");
    operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "creativecommons/*");
    operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "false");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
    MediaPackage resultingMediapackage = result.getMediaPackage();
    Assert.assertEquals("series1", resultingMediapackage.getSeries());
    Assert.assertEquals("Series 1", resultingMediapackage.getSeriesTitle());
    Assert.assertEquals(clone.getElements().length + 1, resultingMediapackage.getElements().length);
  }

  @Test
  public void testExtraMetadata() throws WorkflowOperationException {
    final EName otherProperty = new EName(DublinCore.TERMS_NS_URI, "my-custom-property");
    final String otherValue = "foobar";

    // Add extra metadata to the series catalog.
    seriesCatalog.set(DublinCore.PROPERTY_LANGUAGE, "Opencastian");
    seriesCatalog.set(otherProperty, otherValue);
    seriesCatalog.set(DublinCore.PROPERTY_CONTRIBUTOR, Arrays.asList(
            new DublinCoreValue[] { DublinCoreValue.mk("Mr. Contry Bute"), DublinCoreValue.mk("Mrs. Jane Doe") }));

    // Prepare "copy metadata" property
    String[] extraMetadata = {
            // Append a full metadata field, with NS
            DublinCore.PROPERTY_LANGUAGE.toString(),
            // Field without namespace
            DublinCore.PROPERTY_CONTRIBUTOR.getLocalName(),
            // Field with a namespace different than the default
            otherProperty.toString(),
            // Field that does not exist in the series catalog
            "does-not-exist" };

    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(mp);
    MediaPackage clone = (MediaPackage) mp.clone();

    operation.setConfiguration(SeriesWorkflowOperationHandler.SERIES_PROPERTY, "series1");
    operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "*");
    operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "false");
    operation.setConfiguration(SeriesWorkflowOperationHandler.COPY_METADATA_PROPERTY,
            StringUtils.join(extraMetadata, ", "));

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
    MediaPackage resultingMediapackage = result.getMediaPackage();

    // Get episode DublinCore
    DublinCoreCatalog episodeCatalog = DublinCores.read(capturedStream.getValue());

    Assert.assertEquals("series1", resultingMediapackage.getSeries());
    Assert.assertEquals("Series 1", resultingMediapackage.getSeriesTitle());
    Assert.assertEquals(clone.getElements().length + 1, resultingMediapackage.getElements().length);

    // Check the extra metadata were copied into the dublincore (only those present in the series catalog)
    Assert.assertTrue(episodeCatalog.hasValue(DublinCore.PROPERTY_CONTRIBUTOR));
    Assert.assertEquals(seriesCatalog.get(DublinCore.PROPERTY_CONTRIBUTOR),
            episodeCatalog.get(DublinCore.PROPERTY_CONTRIBUTOR));
    Assert.assertTrue(episodeCatalog.hasValue(DublinCore.PROPERTY_LANGUAGE));
    Assert.assertEquals(seriesCatalog.get(DublinCore.PROPERTY_LANGUAGE),
            episodeCatalog.get(DublinCore.PROPERTY_LANGUAGE));
    Assert.assertTrue(episodeCatalog.hasValue(otherProperty));
    Assert.assertEquals(seriesCatalog.get(otherProperty), episodeCatalog.get(otherProperty));
    Assert.assertFalse(episodeCatalog.hasValue(new EName(DublinCore.TERMS_NS_URI, "does-not-exist")));
  }

  @Test
  public void testExtraMetadataDefaultNS() throws WorkflowOperationException {

    final EName customProperty = new EName(DublinCores.OC_PROPERTY_NS_URI, "my-custom-property");
    final String customValue = "my-custom-value";

    // Add extra metadata to the series catalog.
    seriesCatalog.set(DublinCore.PROPERTY_LANGUAGE, "Opencastian");
    seriesCatalog.set(DublinCore.PROPERTY_CONTRIBUTOR, Arrays.asList(
            new DublinCoreValue[] { DublinCoreValue.mk("Mr. Contry Bute"), DublinCoreValue.mk("Mrs. Jane Doe") }));
    seriesCatalog.set(customProperty, customValue);

    // Prepare "copy metadata" property
    // All field names without namespace
    // However, in the series metadata, the third one has a different NS than the other two
    String[] extraMetadata = {
            DublinCore.PROPERTY_LANGUAGE.getLocalName(),
            DublinCore.PROPERTY_CONTRIBUTOR.getLocalName(),
            customProperty.getLocalName()
    };

    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(mp);
    MediaPackage clone = (MediaPackage) mp.clone();

    operation.setConfiguration(SeriesWorkflowOperationHandler.SERIES_PROPERTY, "series1");
    operation.setConfiguration(SeriesWorkflowOperationHandler.ATTACH_PROPERTY, "*");
    operation.setConfiguration(SeriesWorkflowOperationHandler.APPLY_ACL_PROPERTY, "false");
    operation.setConfiguration(SeriesWorkflowOperationHandler.COPY_METADATA_PROPERTY,
            StringUtils.join(extraMetadata, ", "));
    // Set the namespace of the third, custom property as the default
    operation.setConfiguration(SeriesWorkflowOperationHandler.DEFAULT_NS_PROPERTY, DublinCores.OC_PROPERTY_NS_URI);

    WorkflowOperationResult result = operationHandler.start(instance, null);
    Assert.assertEquals(Action.CONTINUE, result.getAction());
    MediaPackage resultingMediapackage = result.getMediaPackage();
    Assert.assertEquals("series1", resultingMediapackage.getSeries());
    Assert.assertEquals("Series 1", resultingMediapackage.getSeriesTitle());
    Assert.assertEquals(clone.getElements().length + 1, resultingMediapackage.getElements().length);

    // Get episode DublinCore
    DublinCoreCatalog episodeCatalog = DublinCores.read(capturedStream.getValue());

    // Only the later metadatum should have been resolved. The other had a different namespace.
    Assert.assertFalse(episodeCatalog.hasValue(DublinCore.PROPERTY_CONTRIBUTOR));
    Assert.assertFalse(episodeCatalog.hasValue(DublinCore.PROPERTY_LANGUAGE));
    Assert.assertTrue(episodeCatalog.hasValue(customProperty));
    Assert.assertEquals(seriesCatalog.get(customProperty),
            episodeCatalog.get(customProperty));
  }

}
