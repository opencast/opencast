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

package org.opencastproject.index.service.catalog.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.index.service.catalog.adapter.CatalogUIAdapterFactory.CONF_FLAVOR_KEY;
import static org.opencastproject.index.service.catalog.adapter.CatalogUIAdapterFactory.CONF_ORGANIZATION_KEY;
import static org.opencastproject.index.service.catalog.adapter.CatalogUIAdapterFactory.CONF_TITLE_KEY;

import org.opencastproject.index.service.catalog.adapter.events.ConfigurableEventDCCatalogUIAdapter;
import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PropertiesUtil;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class DublinCoreCatalogUIAdapterTest {
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreCatalogUIAdapterTest.class);
  private static final String TEMPORAL_DUBLIN_CORE_KEY = "temporal";
  private static final String INPUT_PERIOD = "start=2014-11-04T19:35:19Z; end=2014-11-04T20:48:23Z; scheme=W3C-DTF;";
  private static final String CHANGED_DURATION_PERIOD = "start=2014-11-04T19:35:19Z; end=2014-11-04T20:18:23Z; scheme=W3C-DTF;";
  private static final String CHANGED_START_DATE_PERIOD = "start=2013-10-29T19:35:19Z; end=2013-10-29T20:48:23Z; scheme=W3C-DTF;";
  private static final String CHANGED_START_TIME_PERIOD = "start=2014-11-04T18:35:19Z; end=2014-11-04T19:48:23Z; scheme=W3C-DTF;";

  private static final String label = "The Label";
  private static final String title = "title";
  private static final String type = "text";
  private static final String readOnly = "true";
  private static final String required = "true";
  private static final String listProvider = "list-provider";
  private static final String collectionID = "collection id";

  private static final String ORGANIZATION_STRING = "theOrganization";
  private static final String FLAVOR_STRING = "type/subtype";
  private static final String TITLE_STRING = "Event Metadata";

  private Dictionary<String, String> dictionary;
  private Dictionary<String, String> eventProperties;
  private ListProvidersService listProvidersService;
  private MediaPackage mediapackage;
  private MediaPackageElementFlavor mediaPackageElementFlavor;
  private Workspace workspace;

  private Capture<InputStream> writtenCatalog;
  private File outputCatalog;
  private URI eventDublincoreURI;

  private EName temporalEname = new EName(DublinCore.TERMS_NS_URI, TEMPORAL_DUBLIN_CORE_KEY);

  private FileInputStream startDateTimeDurationCatalog;
  private DublinCoreCatalog dc;
  private MetadataField<String> startDateMetadataField;
  private MetadataField<String> durationMetadataField;

  private DublinCoreMetadataCollection metadata;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws URISyntaxException, NotFoundException, IOException, ListProviderException {
    startDateTimeDurationCatalog = new FileInputStream(new File(getClass().getResource(
            "/catalog-adapter/start-date-time-duration.xml").toURI()));
    dc = DublinCores.read(startDateTimeDurationCatalog);
    metadata = new DublinCoreMetadataCollection();
    startDateMetadataField = MetadataField.createTemporalStartDateMetadata(TEMPORAL_DUBLIN_CORE_KEY,
            Opt.some("startDate"), "START_DATE_LABEL", false, false, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Opt.<Integer> none(),
            Opt.<String> none());
    durationMetadataField = MetadataField.createDurationMetadataField(TEMPORAL_DUBLIN_CORE_KEY, Opt.some("duration"),
            "DURATION_LABEL", false, false, Opt.<Integer> none(), Opt.<String> none());
    TreeMap<String, String> collection = new TreeMap<String, String>();
    collection.put("Entry 1", "Value 1");
    collection.put("Entry 2", "Value 2");
    collection.put("Entry 3", "Value 3");

    BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.replay(bundleContext);

    listProvidersService = EasyMock.createMock(ListProvidersService.class);
    EasyMock.expect(
            listProvidersService.getList(EasyMock.anyString(), EasyMock.anyObject(ResourceListQueryImpl.class),
                    EasyMock.anyBoolean())).andReturn(collection).anyTimes();
    EasyMock.replay(listProvidersService);

    Properties props = new Properties();
    InputStream in = getClass().getResourceAsStream("/catalog-adapter/event.properties");
    props.load(in);
    in.close();

    eventProperties = PropertiesUtil.toDictionary(props);

    mediaPackageElementFlavor = new MediaPackageElementFlavor(FLAVOR_STRING.split("/")[0], FLAVOR_STRING.split("/")[1]);

    eventDublincoreURI = getClass().getResource("/catalog-adapter/dublincore.xml").toURI();

    outputCatalog = testFolder.newFile("out.xml");

    Capture<String> mediapackageIDCapture = EasyMock.newCapture();
    Capture<String> catalogIDCapture = EasyMock.newCapture();
    Capture<String> filenameCapture = EasyMock.newCapture();
    writtenCatalog = EasyMock.newCapture();

    workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.read(eventDublincoreURI))
            .andAnswer(() -> new FileInputStream(new File(eventDublincoreURI)));
    EasyMock.expect(
            workspace.put(EasyMock.capture(mediapackageIDCapture), EasyMock.capture(catalogIDCapture),
                    EasyMock.capture(filenameCapture), EasyMock.capture(writtenCatalog))).andReturn(
            outputCatalog.toURI());
    EasyMock.replay(workspace);

    Catalog eventCatalog = EasyMock.createMock(Catalog.class);
    EasyMock.expect(eventCatalog.getIdentifier()).andReturn("CatalogID").anyTimes();
    EasyMock.expect(eventCatalog.getURI()).andReturn(eventDublincoreURI).anyTimes();
    eventCatalog.setURI(outputCatalog.toURI());
    EasyMock.expectLastCall();
    eventCatalog.setChecksum(null);
    EasyMock.expectLastCall();
    EasyMock.replay(eventCatalog);
    Catalog[] catalogs = { eventCatalog };

    Id id = EasyMock.createMock(Id.class);
    EasyMock.replay(id);

    mediapackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediapackage.getCatalogs(mediaPackageElementFlavor)).andReturn(catalogs).anyTimes();
    EasyMock.expect(mediapackage.getIdentifier()).andReturn(id).anyTimes();
    EasyMock.replay(mediapackage);

    dictionary = new Hashtable<String, String>();

    dictionary.put(CONF_ORGANIZATION_KEY, ORGANIZATION_STRING);
    dictionary.put(CONF_FLAVOR_KEY, FLAVOR_STRING);
    dictionary.put(CONF_TITLE_KEY, TITLE_STRING);

    dictionary.put(MetadataField.CONFIG_PROPERTY_PREFIX + ".title." + MetadataField.CONFIG_INPUT_ID_KEY, title);
    dictionary.put(MetadataField.CONFIG_PROPERTY_PREFIX + ".title." + MetadataField.CONFIG_LABEL_KEY, label);
    dictionary.put(MetadataField.CONFIG_PROPERTY_PREFIX + ".title." + MetadataField.CONFIG_TYPE_KEY, type);
    dictionary.put(MetadataField.CONFIG_PROPERTY_PREFIX + ".title." + MetadataField.CONFIG_READ_ONLY_KEY, readOnly);
    dictionary.put(MetadataField.CONFIG_PROPERTY_PREFIX + ".title." + MetadataField.CONFIG_REQUIRED_KEY, required);
    dictionary.put(MetadataField.CONFIG_PROPERTY_PREFIX + ".title." + MetadataField.CONFIG_LIST_PROVIDER_KEY,
            listProvider);
    dictionary.put(MetadataField.CONFIG_PROPERTY_PREFIX + ".title." + MetadataField.CONFIG_COLLECTION_ID_KEY,
            collectionID);
  }

  @Test
  public void testGetDublinCorePropertyName() {
    String name = "title";
    assertEquals(
            name,
            DublinCoreMetadataUtil.getDublinCorePropertyName(
                    MetadataField.CONFIG_PROPERTY_PREFIX + "." + name + "." + MetadataField.CONFIG_INPUT_ID_KEY).get());
    assertEquals(Opt.none(),
            DublinCoreMetadataUtil.getDublinCorePropertyName(MetadataField.CONFIG_PROPERTY_PREFIX + "." + name + "."));
    assertEquals(Opt.none(),
            DublinCoreMetadataUtil.getDublinCorePropertyName(name + "." + MetadataField.CONFIG_INPUT_ID_KEY));
    assertEquals(
            Opt.none(),
            DublinCoreMetadataUtil.getDublinCorePropertyName("Irrelevant." + name + "."
                    + MetadataField.CONFIG_INPUT_ID_KEY));
  }

  @Test
  public void testGetDublinCorePropertyKey() {
    String name = "title";
    assertEquals(
            MetadataField.CONFIG_INPUT_ID_KEY,
            DublinCoreMetadataUtil.getDublinCorePropertyKey(
                    MetadataField.CONFIG_PROPERTY_PREFIX + "." + name + "." + MetadataField.CONFIG_INPUT_ID_KEY).get());
  }

  @Test
  public void testGetDublinCoreProperties() {
    Map<String, MetadataField<?>> dublinCoreProperties = DublinCoreMetadataUtil.getDublinCoreProperties(dictionary);
    assertEquals(1, dublinCoreProperties.size());
    List<MetadataField<?>> metadataFields = new ArrayList<MetadataField<?>>(dublinCoreProperties.values());
    assertEquals(title, metadataFields.get(0).getInputID());
    assertEquals(label, metadataFields.get(0).getLabel());
    assertEquals(MetadataField.Type.TEXT, metadataFields.get(0).getType());
    assertEquals(true, metadataFields.get(0).isReadOnly());
    assertEquals(true, metadataFields.get(0).isRequired());
    assertEquals(listProvider, metadataFields.get(0).getListprovider().get());
    assertEquals(collectionID, metadataFields.get(0).getCollectionID().get());
  }

  @Test
  public void testStoreFields() throws Exception {
    String temporal = "temporal";
    String expectedTemporal = "start=2016-03-01T09:27:35Z; end=2016-03-01T11:43:12Z; scheme=W3C-DTF;";
    String expectedTitle = "New Value for Title";
    String expectedMissing = "New Value for Missing";

    ConfigurableEventDCCatalogUIAdapter configurationDublinCoreCatalogUIAdapter = new ConfigurableEventDCCatalogUIAdapter();
    configurationDublinCoreCatalogUIAdapter.setListProvidersService(listProvidersService);
    configurationDublinCoreCatalogUIAdapter.setWorkspace(workspace);
    configurationDublinCoreCatalogUIAdapter.updated(eventProperties);

    DublinCoreMetadataCollection dublinCoreMetadata = new DublinCoreMetadataCollection();

    MetadataField<String> titleField = MetadataField.createTextMetadataField(title, Opt.some(title),
            "New Label for Title", true, false, Opt.<Boolean> none(), Opt.<Map<String, String>> none(),
            Opt.<String> none(), Opt.<Integer> none(), Opt.<String> none());
    dublinCoreMetadata.addField(titleField, expectedTitle, listProvidersService);

    MetadataField<String> missingField = MetadataField.createTextMetadataField("missing", Opt.<String> none(),
            "The Missing's Label", false, false, Opt.<Boolean> none(), Opt.<Map<String, String>> none(),
            Opt.<String> none(), Opt.<Integer> none(), Opt.<String> none());
    dublinCoreMetadata.addField(missingField, expectedMissing, listProvidersService);

    MetadataField<String> durationField = MetadataField.createDurationMetadataField(temporal, Opt.some("duration"),
            label, true, true, Opt.<Integer> none(), Opt.<String> none());
    dublinCoreMetadata.addField(durationField, "02:15:37", listProvidersService);

    MetadataField<String> startDate = MetadataField.createTemporalStartDateMetadata(temporal, Opt.some("startDate"),
            label, true, true, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Opt.<Integer> none(), Opt.<String> none());
    dublinCoreMetadata.addField(startDate, "2016-03-01T09:27:35.000Z", listProvidersService);

    configurationDublinCoreCatalogUIAdapter.storeFields(mediapackage, dublinCoreMetadata);
    assertTrue(writtenCatalog.hasCaptured());

    DublinCoreCatalog updatedCatalog = DublinCores.read(writtenCatalog.getValue());

    assertEquals(expectedTitle, updatedCatalog.get(new EName(DublinCore.TERMS_NS_URI, "title")).get(0).getValue());
    assertEquals(expectedMissing, updatedCatalog.get(new EName(DublinCore.TERMS_NS_URI, "missing")).get(0).getValue());
    assertEquals(expectedTemporal, updatedCatalog.get(new EName(DublinCore.TERMS_NS_URI, temporal)).get(0).getValue());
  }

  @Test
  public void testGetDuration() {
    int startHour = 19;
    int startMinute = 35;
    int startSecond = 19;
    int endHour = 20;
    int endMinute = 48;
    int endSecond = 23;

    // Test a none() period
    Opt<DCMIPeriod> emptyPeriod = Opt.<DCMIPeriod> none();
    Long result = DublinCoreMetadataUtil.getDuration(emptyPeriod);
    assertEquals(new Long(0L), result);

    DateTime periodStart = new DateTime(2014, 11, 04, startHour, startMinute, startSecond, DateTimeZone.UTC);
    DateTime periodEnd = new DateTime(2014, 11, 04, endHour, endMinute, endSecond, DateTimeZone.UTC);

    // Test a period that is missing the start date
    DCMIPeriod withoutStartPeriod = new DCMIPeriod(null, periodEnd.toDate());
    result = DublinCoreMetadataUtil.getDuration(Opt.some(withoutStartPeriod));
    assertEquals(new Long(0L), result);

    // Test a period with a start but no end.
    DCMIPeriod withoutEndPeriod = new DCMIPeriod(periodStart.toDate(), null);
    result = DublinCoreMetadataUtil.getDuration(Opt.some(withoutEndPeriod));
    assertEquals(new Long(0L), result);

    // Test a period with both a start and an end.
    DCMIPeriod standardPeriod = new DCMIPeriod(periodStart.toDate(), periodEnd.toDate());
    result = DublinCoreMetadataUtil.getDuration(Opt.some(standardPeriod));
    assertEquals(new Long(4384000L), result);
  }

  @Test
  public void testSetStartDateInputEmptyValueExpectsNoChange() throws IOException, URISyntaxException {
    metadata.addField(startDateMetadataField, "2013-10-29T19:35:19.000Z", listProvidersService);
    DublinCoreMetadataUtil.setStartDate(dc, startDateMetadataField, temporalEname);
    List<DublinCoreValue> result = dc.get(temporalEname);
    assertEquals(1, result.size());
    assertEquals(INPUT_PERIOD, result.get(0).getValue());
  }

  @Test
  public void testSetTemporalStartDateInputNewValueExpectsChange() throws IOException, URISyntaxException {
    startDateMetadataField.setValue("2013-10-29T19:35:19.000Z");
    metadata.addField(startDateMetadataField, "2013-10-29T19:35:19.000Z", listProvidersService);
    DublinCoreMetadataUtil.setStartDate(dc, startDateMetadataField, temporalEname);
    List<DublinCoreValue> result = dc.get(temporalEname);
    assertEquals(1, result.size());
    assertEquals(CHANGED_START_DATE_PERIOD, result.get(0).getValue());
  }

  @Test
  public void testSetDurationInputEmptyValueExpectsNoChange() throws IOException, URISyntaxException {
    metadata.addField(durationMetadataField, "", listProvidersService);
    DublinCoreMetadataUtil.setStartDate(dc, startDateMetadataField, temporalEname);
    DublinCoreMetadataUtil.setDuration(dc, durationMetadataField, temporalEname);
    List<DublinCoreValue> result = dc.get(temporalEname);
    assertEquals(1, result.size());
    assertEquals(INPUT_PERIOD, result.get(0).getValue());
  }

  @Test
  public void testSetDurationInputNewValueExpectsChange() throws IOException, URISyntaxException {
    metadata.addField(durationMetadataField, "2584000", listProvidersService);
    DublinCoreMetadataUtil.setDuration(dc, metadata.getOutputFields().get("duration"), temporalEname);
    List<DublinCoreValue> result = dc.get(temporalEname);
    assertEquals(1, result.size());
    assertEquals(CHANGED_DURATION_PERIOD, result.get(0).getValue());
  }

  @Test
  public void testUpdateDublincoreCatalog() throws IOException, URISyntaxException {
    FileInputStream fis = new FileInputStream(new File(getClass().getResource(
            "/catalog-adapter/start-date-time-duration.xml").toURI()));
    DublinCoreCatalog catalog = DublinCores.read(fis);
    MetadataField<String> startDate = MetadataField.createTemporalStartDateMetadata(TEMPORAL_DUBLIN_CORE_KEY,
            Opt.some("startDate"), "START_DATE_LABEL", false, false, "yyyy-MM-dd", Opt.<Integer> none(),
            Opt.<String> none());
    DublinCoreMetadataCollection metadata = new DublinCoreMetadataCollection();
    metadata.addField(startDate, "2014-11-01", listProvidersService);
    DublinCoreMetadataUtil.updateDublincoreCatalog(catalog, metadata);
    logger.info("Catalog:" + catalog.toXmlString());
  }
}
