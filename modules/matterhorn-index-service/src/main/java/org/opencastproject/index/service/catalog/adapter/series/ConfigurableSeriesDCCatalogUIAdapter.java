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
package org.opencastproject.index.service.catalog.adapter.series;

import static java.util.Objects.requireNonNull;
import static org.opencastproject.util.OsgiUtil.getCfg;

import org.opencastproject.index.service.catalog.adapter.AbstractMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.CatalogUIAdapterConfiguration;
import org.opencastproject.index.service.catalog.adapter.DublinCoreMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.DublinCoreMetadataUtil;
import org.opencastproject.index.service.catalog.adapter.MetadataField;
import org.opencastproject.index.service.catalog.adapter.events.ConfigurableEventDCCatalogUIAdapter;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.RequireUtil;

import com.entwinemedia.fn.data.Opt;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A series catalog UI adapter that is managed by a configuration.
 */
public class ConfigurableSeriesDCCatalogUIAdapter implements SeriesCatalogUIAdapter {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ConfigurableEventDCCatalogUIAdapter.class);

  /** The configuration dublincore catalog UI adapter service PID */
  public static final String PID = "org.opencastproject.index.service.catalog.adapter";

  /* The collection of keys to read from the OSGI configuration file */
  public static final String CONF_TYPE_KEY = "type";
  public static final String CONF_ORGANIZATION_KEY = "organization";
  public static final String CONF_FLAVOR_KEY = "flavor";
  public static final String CONF_TITLE_KEY = "title";

  private SeriesService seriesService;
  private SecurityService securityService;

  /** The catalog UI adapter configuration */
  private CatalogUIAdapterConfiguration config;

  /** The organization name */
  private String organization;

  /** The flavor of this catalog */
  private MediaPackageElementFlavor flavor;

  /** The title of this catalog */
  private String title;

  /** Reference to the list providers service */
  private ListProvidersService listProvidersService;

  /** The metadata fields for all properties of the underlying DublinCore */
  private Map<String, MetadataField<?>> dublinCoreProperties;

  @Override
  public String getOrganization() {
    return organization;
  }

  @Override
  public String getFlavor() {
    return flavor.toString();
  }

  @Override
  public String getUITitle() {
    return title;
  }

  @Override
  public AbstractMetadataCollection getRawFields() {
    DublinCoreMetadataCollection dublinCoreMetadata = new DublinCoreMetadataCollection();
    Set<String> emptyFields = new TreeSet<String>(dublinCoreProperties.keySet());
    populateEmptyFields(dublinCoreMetadata, emptyFields);
    return dublinCoreMetadata;
  }

  @Override
  public Opt<AbstractMetadataCollection> getFields(String seriesId) {
    final Opt<DublinCoreCatalog> optDCCatalog = loadDublinCoreCatalog(RequireUtil.requireNotBlank(seriesId, "seriesId"));
    if (optDCCatalog.isSome()) {
      DublinCoreMetadataCollection dublinCoreMetadata = new DublinCoreMetadataCollection();
      Set<String> emptyFields = new TreeSet<String>(dublinCoreProperties.keySet());
      getFieldValuesFromDublinCoreCatalog(dublinCoreMetadata, emptyFields, optDCCatalog.get());
      populateEmptyFields(dublinCoreMetadata, emptyFields);
      return Opt.some((AbstractMetadataCollection) dublinCoreMetadata);
    } else {
      return Opt.none();
    }
  }

  @Override
  public boolean storeFields(String seriesId, AbstractMetadataCollection metadata) {
    final Opt<DublinCoreCatalog> optDCCatalog = loadDublinCoreCatalog(RequireUtil.requireNotBlank(seriesId, "seriesId"));
    if (optDCCatalog.isSome()) {
      final DublinCoreCatalog dc = optDCCatalog.get();
      DublinCoreMetadataUtil.updateDublincoreCatalog(dc, metadata);
      saveDublinCoreCatalog(seriesId, dc);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Reconfigures the {@link SeriesCatalogUIAdapter} instance with an updated set of configuration properties;
   *
   * @param properties
   *          the configuration properties
   * @throws ConfigurationException
   *           if there is a configuration error
   */
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    config = CatalogUIAdapterConfiguration.loadFromDictionary(properties);
    organization = getCfg(properties, CONF_ORGANIZATION_KEY);
    flavor = MediaPackageElementFlavor.parseFlavor(getCfg(properties, CONF_FLAVOR_KEY));
    title = getCfg(properties, CONF_TITLE_KEY);
    dublinCoreProperties = DublinCoreMetadataUtil.getDublinCoreProperties(properties);
  }

  /** OSGi callback to set list provider service instance */
  public void setListProvidersService(ListProvidersService listProvidersService) {
    this.listProvidersService = listProvidersService;
  }

  /** Return the {@link SeriesService} to get access to the metadata catalogs of a series */
  protected SeriesService getSeriesService() {
    return seriesService;
  }

  /** OSGi callback to bind instance of {@link SeriesService} */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** Return instance of {@link SecurityService}. */
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /** OSGi callback to bind instance of {@link SecurityService} */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  protected Opt<DublinCoreCatalog> loadDublinCoreCatalog(String seriesId) {
    try {
      Opt<byte[]> seriesElementData = getSeriesService().getSeriesElementData(requireNonNull(seriesId),
              flavor.getType());
      if (seriesElementData.isSome()) {
        InputStream is = null;
        try {
          is = new ByteArrayInputStream(seriesElementData.get());
          return Opt.some(DublinCores.read(is));
        } finally {
          IOUtils.closeQuietly(is);
        }
      } else {
        final DublinCoreCatalog dc = DublinCores.mkStandard();
        dc.addBindings(config.getXmlNamespaceContext());
        dc.setRootTag(new EName(config.getCatalogXmlRootNamespace(), config.getCatalogXmlRootElementName()));
        return Opt.some(dc);
      }
    } catch (SeriesException e) {
      logger.error("Error while loading DublinCore catalog of series '{}': {}", seriesId,
              ExceptionUtils.getStackTrace(e));
      return Opt.none();
    }
  }

  protected boolean saveDublinCoreCatalog(String seriesId, DublinCoreCatalog dc) {
    try {
      final byte[] dcData = dc.toXmlString().getBytes("UTF-8");
      if (getSeriesService().getSeriesElementData(seriesId, flavor.getType()).isSome()) {
        return getSeriesService().updateSeriesElement(seriesId, flavor.getType(), dcData);
      } else {
        return getSeriesService().addSeriesElement(seriesId, flavor.getType(), dcData);
      }
    } catch (IOException e) {
      logger.error("Error while serializing the dublin core catalog to XML: {}", ExceptionUtils.getStackTrace(e));
      return false;
    } catch (SeriesException e) {
      logger.error("Error while saving the series element: {}", ExceptionUtils.getStackTrace(e));
      return false;
    }
  }

  private void populateEmptyFields(DublinCoreMetadataCollection dublinCoreMetadata, Set<String> emptyFields) {
    // Add all of the rest of the fields that didn't have values as empty.
    for (String field : emptyFields) {
      if (dublinCoreProperties.get(field) == null) {
        logger.warn("Skipping field {} because it is not defined in the properties file.", field);
      }
      try {
        dublinCoreMetadata.addField(dublinCoreProperties.get(field), "", listProvidersService);
      } catch (Exception e) {
        logger.error("Skipping metadata field '{}' because of error: {}", field, ExceptionUtils.getStackTrace(e));
      }
    }
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
              dublinCoreMetadata.addField(metadataField, dublinCoreValue.getValue(), listProvidersService);
            } catch (IllegalArgumentException e) {
              logger.error("Skipping metadata field '{}' because of error: {}", metadataField.getInputID(),
                      ExceptionUtils.getStackTrace(e));
            }

          }
        }
      }
    }
  }

}
