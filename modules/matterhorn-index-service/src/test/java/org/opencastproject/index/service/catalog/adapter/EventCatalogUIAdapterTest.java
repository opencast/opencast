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
package org.opencastproject.index.service.catalog.adapter;

import static org.opencastproject.index.service.catalog.adapter.CatalogUIAdapterFactory.CONF_FLAVOR_KEY;
import static org.opencastproject.index.service.catalog.adapter.CatalogUIAdapterFactory.CONF_ORGANIZATION_KEY;
import static org.opencastproject.index.service.catalog.adapter.CatalogUIAdapterFactory.CONF_TITLE_KEY;
import static org.junit.Assert.assertThat;

import org.opencastproject.index.service.catalog.adapter.events.ConfigurableEventDCCatalogUIAdapter;
import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TreeMap;

public class EventCatalogUIAdapterTest {
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
  private Properties eventProperties;
  private ListProvidersService listProvidersService;
  private MediaPackage mediapackage;
  private MediaPackageElementFlavor mediaPackageElementFlavor;
  private Workspace workspace;

  @Before
  public void setUp() throws URISyntaxException, NotFoundException, IOException, ListProviderException {
    TreeMap<String, Object> collection = new TreeMap<String, Object>();
    collection.put("Entry 1", "Value 1");
    collection.put("Entry 2", "Value 2");
    collection.put("Entry 3", "Value 3");

    BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.replay(bundleContext);

    listProvidersService = EasyMock.createMock(ListProvidersService.class);
    EasyMock.expect(
            listProvidersService.getList(EasyMock.anyString(), EasyMock.anyObject(ResourceListQueryImpl.class),
                    EasyMock.anyObject(Organization.class))).andReturn(collection).anyTimes();
    EasyMock.replay(listProvidersService);

    eventProperties = new Properties();
    InputStream in = getClass().getResourceAsStream("/catalog-adapter/event.properties");
    eventProperties.load(in);
    in.close();

    mediaPackageElementFlavor = new MediaPackageElementFlavor(FLAVOR_STRING.split("/")[0], FLAVOR_STRING.split("/")[1]);

    URI eventDublincoreURI = getClass().getResource("/catalog-adapter/event-dublincore.xml").toURI();

    workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.get(eventDublincoreURI)).andReturn(new File(eventDublincoreURI));
    EasyMock.replay(workspace);

    Catalog eventCatalog = EasyMock.createMock(Catalog.class);
    EasyMock.expect(eventCatalog.getURI()).andReturn(eventDublincoreURI).anyTimes();
    EasyMock.replay(eventCatalog);
    Catalog[] catalogs = { eventCatalog };
    mediapackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediapackage.getCatalogs(mediaPackageElementFlavor)).andReturn(catalogs).anyTimes();
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
  public void testGetFields() throws Exception {
    String eventJson = IOUtils.toString(getClass().getResource("/catalog-adapter/event.json"));

    ConfigurableEventDCCatalogUIAdapter configurationDublinCoreCatalogUIAdapter = new ConfigurableEventDCCatalogUIAdapter();
    configurationDublinCoreCatalogUIAdapter.setListProvidersService(listProvidersService);
    configurationDublinCoreCatalogUIAdapter.setWorkspace(workspace);
    configurationDublinCoreCatalogUIAdapter.updated(eventProperties);

    AbstractMetadataCollection abstractMetadata = configurationDublinCoreCatalogUIAdapter.getFields(mediapackage);
    assertThat(eventJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(abstractMetadata.toJSON())).allowingAnyArrayOrdering());
  }

  @Ignore
  @Test
  public void testStoreFields() {

  }
}
