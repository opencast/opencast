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
package org.opencastproject.index.service.catalog.adapter.events;

import static org.opencastproject.index.service.catalog.adapter.CatalogUIAdapterFactory.CONF_FLAVOR_KEY;
import static org.opencastproject.index.service.catalog.adapter.CatalogUIAdapterFactory.CONF_ORGANIZATION_KEY;
import static org.opencastproject.util.OsgiUtil.getCfg;

import org.opencastproject.index.service.catalog.adapter.AbstractMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.CatalogUIAdapterConfiguration;
import org.opencastproject.index.service.catalog.adapter.DublinCoreMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.DublinCoreMetadataUtil;
import org.opencastproject.index.service.catalog.adapter.MetadataField;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.util.IoSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Managed service implementation of a {@link AbstractEventsCatalogUIAdapter}
 */
public class ConfigurableEventDCCatalogUIAdapter implements EventCatalogUIAdapter {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ConfigurableEventDCCatalogUIAdapter.class);

  /** The catalog UI adapter configuration */
  private CatalogUIAdapterConfiguration config;

  private Map<String, MetadataField<?>> dublinCoreProperties = new TreeMap<String, MetadataField<?>>();
  private MediaPackageElementFlavor flavor;
  private String organization;
  private String title;

  private ListProvidersService listProvidersService;
  private Workspace workspace;

  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    config = CatalogUIAdapterConfiguration.loadFromDictionary(properties);
    organization = getCfg(properties, CONF_ORGANIZATION_KEY);
    String flavorString = getCfg(properties, CONF_FLAVOR_KEY);
    if (StringUtils.isBlank(flavorString) || flavorString.split("/").length != 2) {
      throw new ConfigurationException(CONF_FLAVOR_KEY, "The flavor " + flavorString
              + " is not a valid flavor. It should be defined as 'type/subtype'");
    }
    flavor = new MediaPackageElementFlavor(flavorString.split("/")[0], flavorString.split("/")[1]);
    title = getCfg(properties, "title");
    dublinCoreProperties = DublinCoreMetadataUtil.getDublinCoreProperties(properties);
    logger.info("Updated dublin core catalog UI adapter {} for flavor {}", getUITitle(), getFlavor());
  }

  @Override
  public DublinCoreMetadataCollection getRawFields() {
    DublinCoreMetadataCollection dublinCoreMetadata = new DublinCoreMetadataCollection();
    Set<String> emptyFields = new TreeSet<String>(dublinCoreProperties.keySet());
    populateEmptyFields(dublinCoreMetadata, emptyFields);
    return dublinCoreMetadata;
  }

  private void populateEmptyFields(DublinCoreMetadataCollection dublinCoreMetadata, Set<String> emptyFields) {
    // Add all of the rest of the fields that didn't have values as empty.
    for (String field : emptyFields) {
      if (dublinCoreProperties.get(field) == null) {
        logger.warn("Skipping field {} because it is not defined in the properties file.", field);
      }
      try {
        dublinCoreMetadata.addField(dublinCoreProperties.get(field), "", getListProvidersService());
      } catch (Exception e) {
        logger.error("Skipping metadata field '{}' because of error: {}", field, ExceptionUtils.getStackTrace(e));
      }
    }
  }

  @Override
  public AbstractMetadataCollection getFields(MediaPackage mediapackage) {
    DublinCoreMetadataCollection dublinCoreMetadata = new DublinCoreMetadataCollection();
    Set<String> emptyFields = new TreeSet<String>(dublinCoreProperties.keySet());
    if (mediapackage != null) {
      for (Catalog catalog : mediapackage.getCatalogs(getFlavor())) {
        getFieldValuesFromCatalog(dublinCoreMetadata, emptyFields, catalog);
      }
    }
    populateEmptyFields(dublinCoreMetadata, emptyFields);
    return dublinCoreMetadata;
  }

  private void getFieldValuesFromCatalog(DublinCoreMetadataCollection dublinCoreMetadata, Set<String> emptyFields,
          Catalog catalog) {
    DublinCoreCatalog dc = DublinCoreUtil.loadDublinCore(getWorkspace(), catalog);
    getFieldValuesFromDublinCoreCatalog(dublinCoreMetadata, emptyFields, dc);
  }

  private void getFieldValuesFromDublinCoreCatalog(DublinCoreMetadataCollection dublinCoreMetadata,
          Set<String> emptyFields, DublinCoreCatalog dc) {
    for (EName propertyKey : dc.getValues().keySet()) {
      for (String metdataFieldKey : dublinCoreProperties.keySet()) {
        MetadataField<?> metadataField = dublinCoreProperties.get(metdataFieldKey);
        String namespace = DublinCore.TERMS_NS_URI;
        if (metadataField.getNamespace().isSome()) {
          namespace = metadataField.getNamespace().get();
        }
        if (namespace.equalsIgnoreCase(propertyKey.getNamespaceURI())
                && metadataField.getInputID().equalsIgnoreCase(propertyKey.getLocalName())) {
          for (DublinCoreValue dublinCoreValue : dc.get(propertyKey)) {
            emptyFields.remove(metdataFieldKey);
            try {
              dublinCoreMetadata.addField(metadataField, dublinCoreValue.getValue(), getListProvidersService());
            } catch (IllegalArgumentException e) {
              logger.error("Skipping metadata field '{}' because of error: {}", metadataField.getInputID(),
                      ExceptionUtils.getStackTrace(e));
            }

          }
        }
      }
    }
  }

  @Override
  public Catalog storeFields(MediaPackage mediaPackage, AbstractMetadataCollection abstractMetadata) {
    Catalog[] catalogs = mediaPackage.getCatalogs(getFlavor());
    final Catalog catalog;
    final DublinCoreCatalog dc;
    final String filename;
    if (catalogs.length == 0) {
      catalog = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .newElement(org.opencastproject.mediapackage.MediaPackageElement.Type.Catalog, getFlavor());
      catalog.setIdentifier(UUID.randomUUID().toString());
      mediaPackage.add(catalog);

      dc = DublinCores.mkSimple();
      dc.addBindings(config.getXmlNamespaceContext());
      dc.setRootTag(new EName(config.getCatalogXmlRootNamespace(), config.getCatalogXmlRootElementName()));
      filename = "dublincore.xml";
    } else {
      catalog = catalogs[0];
      dc = DublinCoreUtil.loadDublinCore(getWorkspace(), catalog);
      filename = FilenameUtils.getName(catalog.getURI().toString());
    }

    DublinCoreMetadataUtil.updateDublincoreCatalog(dc, abstractMetadata);

    URI uri;
    InputStream inputStream = null;
    try {
      inputStream = IOUtils.toInputStream(dc.toXmlString(), "UTF-8");
      uri = getWorkspace().put(mediaPackage.getIdentifier().toString(), catalog.getIdentifier(), filename, inputStream);
      catalog.setURI(uri);
      // setting the URI to a new source so the checksum will most like be invalid
      catalog.setChecksum(null);
    } catch (IOException e) {
      logger.error("Unable to store catalog {} metadata to workspace: {}", catalog, ExceptionUtils.getStackTrace(e));
    } finally {
      IoSupport.closeQuietly(inputStream);
    }
    return catalog;
  }

  @Override
  public String getOrganization() {
    return organization;
  }

  @Override
  public String getUITitle() {
    return title;
  }

  @Override
  public MediaPackageElementFlavor getFlavor() {
    return flavor;
  }

  public void setListProvidersService(ListProvidersService listProvidersService) {
    this.listProvidersService = listProvidersService;
  }

  protected ListProvidersService getListProvidersService() {
    return listProvidersService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  protected Workspace getWorkspace() {
    return workspace;
  }

}
