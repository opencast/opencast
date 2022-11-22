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

package org.opencastproject.series.impl;

import static org.opencastproject.util.EqualsUtil.bothNotNull;
import static org.opencastproject.util.EqualsUtil.eqListSorted;
import static org.opencastproject.util.EqualsUtil.eqListUnsorted;
import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.util.AccessInformationUtil;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.series.Series;
import org.opencastproject.elasticsearch.index.rebuild.AbstractIndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexProducer;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildException;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildService;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.message.broker.api.series.SeriesItem;
import org.opencastproject.message.broker.api.update.SeriesUpdateHandler;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.series.impl.persistence.SeriesEntity;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Implements {@link SeriesService}. Uses {@link SeriesServiceDatabase} for permanent storage and
 * {@link ElasticsearchIndex} for searching.
 */
@Component(
    property = {
        "service.description=Series Service"
    },
    immediate = true,
    service = { SeriesService.class, IndexProducer.class }
)
public class SeriesServiceImpl extends AbstractIndexProducer implements SeriesService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceImpl.class);

  private static final String THEME_PROPERTY_NAME = "theme";

  /** Persistent storage */
  protected SeriesServiceDatabase persistence;

  /** The security service */
  protected SecurityService securityService;

  /** The organization directory */
  protected OrganizationDirectoryService orgDirectory;

  /** The system user name */
  private String systemUserName;

  /** The API index */
  private ElasticsearchIndex index;

  private AclServiceFactory aclServiceFactory;

  private ArrayList<SeriesUpdateHandler> updateHandlers = new ArrayList<>();

  /** OSGi callback for setting persistance. */
  @Reference
  public void setPersistence(SeriesServiceDatabase persistence) {
    this.persistence = persistence;
  }

  /** OSGi callback for setting the security service. */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback for setting the organization directory service. */
  @Reference
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  /** OSGi callbacks for settings and removing handlers. */
  @Reference(
      policy = ReferencePolicy.DYNAMIC,
      cardinality = ReferenceCardinality.MULTIPLE,
      unbind = "removeMessageHandler"
  )
  public void addMessageHandler(SeriesUpdateHandler handler) {
    this.updateHandlers.add(handler);
  }

  public void removeMessageHandler(SeriesUpdateHandler handler) {
    this.updateHandlers.remove(handler);
  }

  /** OSGi callbacks for setting the API index. */
  @Reference
  public void setElasticsearchIndex(ElasticsearchIndex index) {
    this.index = index;
  }

  @Reference
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  /**
   * Activates Series Service. Checks whether we are using synchronous or asynchronous indexing. If
   * asynchronous is used, Executor service is set. If index is empty, persistent storage is queried
   * if it contains any series. If that is the case, series are retrieved and indexed.
   */
  @Activate
  public void activate(ComponentContext cc) throws Exception {
    logger.info("Activating Series Service");
    systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
  }

  @Override
  public DublinCoreCatalog updateSeries(DublinCoreCatalog dc) throws SeriesException, UnauthorizedException {
    try {
      for (DublinCoreCatalog dublinCore : isNew(notNull(dc, "dc"))) {
        final String id = dublinCore.getFirst(DublinCore.PROPERTY_IDENTIFIER);

        if (!dublinCore.hasValue(DublinCore.PROPERTY_CREATED)) {
          DublinCoreValue date = EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute);
          dublinCore.set(DublinCore.PROPERTY_CREATED, date);
          logger.debug("Setting series creation date to '{}'", date.getValue());
        }

        if (dublinCore.hasValue(DublinCore.PROPERTY_TITLE)) {
          if (dublinCore.getFirst(DublinCore.PROPERTY_TITLE).length() > 255) {
            dublinCore.set(DublinCore.PROPERTY_TITLE, dublinCore.getFirst(DublinCore.PROPERTY_TITLE).substring(0, 255));
            logger.warn("Title was longer than 255 characters. Cutting excess off.");
          }
        }

        logger.debug("Updating series {}", id);
        // update API index
        updateSeriesMetadataInIndex(id, dublinCore);

        // Make sure store to persistence comes after index, return value can be null
        DublinCoreCatalog updated = persistence.storeSeries(dublinCore);

        // still sent for other asynchronous updates
        triggerEventHandlers(SeriesItem.updateCatalog(dublinCore));
        return (updated == null) ? null : dublinCore;
      }
      return dc;
    } catch (Exception e) {
      throw new SeriesException(e);
    }
  }

  /** Check if <code>dc</code> is new and, if so, return an updated version ready to store. */
  private Option<DublinCoreCatalog> isNew(DublinCoreCatalog dc) throws SeriesServiceDatabaseException {
    final String id = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    if (id != null) {
      try {
        return equals(persistence.getSeries(id), dc) ? Option.none() : some(dc);
      } catch (NotFoundException e) {
        return some(dc);
      }
    } else {
      logger.info("Series Dublin Core does not contain identifier, generating one");
      dc.set(DublinCore.PROPERTY_IDENTIFIER, UUID.randomUUID().toString());
      return some(dc);
    }
  }

  @Override
  public boolean updateAccessControl(final String seriesId, final AccessControlList accessControl)
          throws NotFoundException, SeriesException {
    return updateAccessControl(seriesId, accessControl, false);
  }

  // todo method signature does not fit the three different possible return values
  @Override
  public boolean updateAccessControl(final String seriesId, final AccessControlList accessControl,
          boolean overrideEpisodeAcl)
          throws NotFoundException, SeriesException {
    if (StringUtils.isEmpty(seriesId)) {
      throw new IllegalArgumentException("Series ID parameter must not be null or empty.");
    }
    if (accessControl == null) {
      throw new IllegalArgumentException("ACL parameter must not be null");
    }
    if (needsUpdate(seriesId, accessControl) || overrideEpisodeAcl) {
      logger.debug("Updating ACL of series {}", seriesId);
      boolean updated;

      try {
        updated = persistence.storeSeriesAccessControl(seriesId, accessControl);
        //update API index
        updateSeriesAclInIndex(seriesId, accessControl);
        // still sent for other asynchronous updates
        triggerEventHandlers(SeriesItem.updateAcl(seriesId, accessControl, overrideEpisodeAcl));
      } catch (SeriesServiceDatabaseException e) {
        logger.error("Could not update series {} with access control rules", seriesId, e);
        throw new SeriesException(e);
      }
      return updated;
    } else {
      // todo not the right return code
      return true;
    }
  }

  /** Check if <code>acl</code> needs to be updated for the given series. */
  private boolean needsUpdate(String seriesId, AccessControlList acl) throws SeriesException {
    try {
      return !equals(persistence.getAccessControlList(seriesId), acl);
    } catch (NotFoundException e) {
      return true;
    } catch (SeriesServiceDatabaseException e) {
      throw new SeriesException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.series.api.SeriesService#deleteSeries(java.lang.String)
   */
  @Override
  public void deleteSeries(final String seriesID) throws SeriesException, NotFoundException {
    try {
      persistence.deleteSeries(seriesID);
      // remove from API index
      removeSeriesFromIndex(seriesID);
      // still sent for other asynchronous updates
      triggerEventHandlers(SeriesItem.delete(seriesID));
    } catch (SeriesServiceDatabaseException e1) {
      logger.error("Could not delete series with id {} from persistence storage", seriesID);
      throw new SeriesException(e1);
    }
  }

  @Override
  public DublinCoreCatalog getSeries(String seriesID) throws SeriesException, NotFoundException {
    try {
      return persistence.getSeries(seriesID);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to execute search query: {}", e.getMessage());
      throw new SeriesException(e);
    }
  }

  @Override
  public List<org.opencastproject.series.api.Series> getAllForAdministrativeRead(
      Date from,
      Optional<Date> to,
      int limit
  ) throws SeriesException, UnauthorizedException {
    try {
      return persistence.getAllForAdministrativeRead(from, to, limit);
    } catch (SeriesServiceDatabaseException e) {
      String msg = String.format(
          "Exception while reading all series in range %s to %s from persistence storage",
          from,
          to
      );
      throw new SeriesException(msg, e);
    }
  }

  public AccessControlList getSeriesAccessControl(String seriesID) throws NotFoundException, SeriesException {
    try {
      return persistence.getAccessControlList(seriesID);
    } catch (SeriesServiceDatabaseException e) {
      throw new SeriesException("Failed to execute search query", e);
    }
  }

  @Override
  public int getSeriesCount() throws SeriesException {
    try {
      return persistence.countSeries();
    } catch (SeriesServiceDatabaseException e) {
      throw new SeriesException("Failed to execute search query", e);
    }
  }

  @Override
  public Map<String, String> getSeriesProperties(String seriesID)
          throws SeriesException, NotFoundException, UnauthorizedException {
    try {
      return persistence.getSeriesProperties(seriesID);
    } catch (SeriesServiceDatabaseException e) {
      throw new SeriesException(String.format("Failed to get series properties for series with id '%s'", seriesID), e);
    }
  }

  @Override
  public String getSeriesProperty(String seriesID, String propertyName)
          throws SeriesException, NotFoundException, UnauthorizedException {
    try {
      return persistence.getSeriesProperty(seriesID, propertyName);
    } catch (SeriesServiceDatabaseException e) {
      String msg = String.format(
              "Failed to get series property for series with series id '%s' and property name '%s'",
              seriesID,
              propertyName
      );
      throw new SeriesException(msg, e);
    }
  }

  @Override
  public void updateSeriesProperty(String seriesID, String propertyName, String propertyValue)
          throws SeriesException, NotFoundException, UnauthorizedException {
    try {
      persistence.updateSeriesProperty(seriesID, propertyName, propertyValue);

      // update API index
      if (propertyName.equals(THEME_PROPERTY_NAME)) {
        updateThemePropertyInIndex(seriesID, Optional.ofNullable(propertyValue));
      }
    } catch (SeriesServiceDatabaseException e) {
      String msg = String.format(
              "Failed to get series property for series with series id '%s' and property name '%s' and value '%s'",
              seriesID,
              propertyName,
              propertyValue
      );
      throw new SeriesException(msg, e);
    }
  }

  @Override
  public void deleteSeriesProperty(String seriesID, String propertyName)
          throws SeriesException, NotFoundException, UnauthorizedException {
    try {
      persistence.deleteSeriesProperty(seriesID, propertyName);

      // update API index
      if (propertyName.equals(THEME_PROPERTY_NAME)) {
        updateThemePropertyInIndex(seriesID, Optional.empty());
      }
    } catch (SeriesServiceDatabaseException e) {
      String msg = String.format(
              "Failed to delete series property for series with series id '%s' and property name '%s'",
              seriesID,
              propertyName
      );
      throw new SeriesException(msg, e);
    }
  }

  /**
   * Define equality on DublinCoreCatalogs. Two DublinCores are considered equal if they have the same properties and if
   * each property has the same values in the same order.
   * <p>
   * Note: As long as http://opencast.jira.com/browse/MH-8759 is not fixed, the encoding scheme of values is not
   * considered.
   * <p>
   * Implementation Note: DublinCores should not be compared by their string serialization since the ordering of
   * properties is not defined and cannot be guaranteed between serializations.
   */
  public static boolean equals(DublinCoreCatalog a, DublinCoreCatalog b) {
    final Map<EName, List<DublinCoreValue>> av = a.getValues();
    final Map<EName, List<DublinCoreValue>> bv = b.getValues();
    if (av.size() == bv.size()) {
      for (Map.Entry<EName, List<DublinCoreValue>> ave : av.entrySet()) {
        if (!eqListSorted(ave.getValue(), bv.get(ave.getKey()))) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Define equality on AccessControlLists. Two AccessControlLists are considered equal if they contain the exact same
   * entries no matter in which order.
   */
  public static boolean equals(AccessControlList a, AccessControlList b) {
    return bothNotNull(a, b) && eqListUnsorted(a.getEntries(), b.getEntries());
  }

  @Override
  public Opt<Map<String, byte[]>> getSeriesElements(String seriesId) throws SeriesException {
    try {
      return persistence.getSeriesElements(seriesId);
    } catch (SeriesServiceDatabaseException e) {
      throw new SeriesException(e);
    }
  }

  @Override
  public Opt<byte[]> getSeriesElementData(String seriesId, String type) throws SeriesException {
    try {
      return persistence.getSeriesElement(seriesId, type);
    } catch (SeriesServiceDatabaseException e) {
      throw new SeriesException(e);
    }
  }

  @Override
  public boolean addSeriesElement(String seriesID, String type, byte[] data) throws SeriesException {
    try {
      if (persistence.existsSeriesElement(seriesID, type)) {
        return false;
      } else {
        return persistence.storeSeriesElement(seriesID, type, data);
      }
    } catch (SeriesServiceDatabaseException e) {
      throw new SeriesException(e);
    }
  }

  @Override
  public boolean updateSeriesElement(String seriesID, String type, byte[] data) throws SeriesException {
    try {
      if (persistence.existsSeriesElement(seriesID, type) && persistence.storeSeriesElement(seriesID, type, data)) {
        triggerEventHandlers(SeriesItem.updateElement(seriesID, type, new String(data, StandardCharsets.UTF_8)));
        return true;
      } else {
        return false;
      }
    } catch (SeriesServiceDatabaseException e) {
      throw new SeriesException(e);
    }
  }

  @Override
  public boolean deleteSeriesElement(String seriesID, String type) throws SeriesException {
    try {
      if (persistence.existsSeriesElement(seriesID, type)) {
        return persistence.deleteSeriesElement(seriesID, type);
      } else {
        return false;
      }
    } catch (SeriesServiceDatabaseException e) {
      throw new SeriesException(e);
    }
  }

  @Override
  public void repopulate() throws IndexRebuildException {
    try {
      List<SeriesEntity> databaseSeries = persistence.getAllSeries();
      final int total = databaseSeries.size();
      int current = 1;
      logIndexRebuildBegin(logger, index.getIndexName(), total, "series");

      for (SeriesEntity series: databaseSeries) {
        Organization organization = orgDirectory.getOrganization(series.getOrganization());
        User systemUser = SecurityUtil.createSystemUser(systemUserName, organization);
        SecurityUtil.runAs(securityService, organization, systemUser,
                () -> {
                  String seriesId = series.getSeriesId();
                  logger.trace("Adding series {} for organization {} to the {} index.", seriesId,
                          series.getOrganization(), index.getIndexName());
                  List<Function<Optional<Series>, Optional<Series>>> updateFunctions = new ArrayList<>();

                  DublinCoreCatalog catalog;
                  try {
                    catalog = DublinCoreXmlFormat.read(series.getDublinCoreXML());
                    updateFunctions.add(getMetadataUpdateFunction(seriesId, catalog, organization.getId()));
                  } catch (IOException | ParserConfigurationException | SAXException e) {
                    logger.error("Could not read dublincore XML of series {}.", seriesId, e);
                    return;
                  }

                  String aclStr = series.getAccessControl();
                  if (StringUtils.isNotBlank(aclStr)) {
                    try {
                      AccessControlList acl = AccessControlParser.parseAcl(aclStr);
                      updateFunctions.add(getAclUpdateFunction(seriesId, acl, organization.getId()));
                    } catch (Exception ex) {
                      logger.error("Unable to parse ACL of series {}.", seriesId, ex);
                    }
                  }

                  try {
                    Map<String, String> properties = persistence.getSeriesProperties(seriesId);
                    updateFunctions.add(getThemePropertyUpdateFunction(seriesId,
                            Optional.ofNullable(properties.get(THEME_PROPERTY_NAME)), organization.getId()));
                  } catch (NotFoundException | SeriesServiceDatabaseException e) {
                    logger.error("Error reading properties of series {}", seriesId, e);
                  }

                  // do the actual index update
                  updateSeriesInIndex(seriesId, organization.getId(),
                          updateFunctions.toArray(new Function[0]));

                });
        logIndexRebuildProgress(logger, index.getIndexName(), total, current);
        current++;
      }
    } catch (Exception e) {
      logIndexRebuildError(logger, index.getIndexName(), e);
      throw new IndexRebuildException(index.getIndexName(), getService(), e);
    }
  }

  private void triggerEventHandlers(SeriesItem item) {
    while (updateHandlers.size() != 1) {
      logger.warn("Expecting 1 handler, but {} are registered.  Waiting 10s then retrying...", updateHandlers.size());
      try {
        Thread.sleep(10000L);
      } catch (InterruptedException e) { /* swallow this, nothing to do */ }
    }
    for (SeriesUpdateHandler handler : updateHandlers) {
      handler.execute(item);
    }
  }

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.Series;
  }

  /**
   * Remove series from API index.
   *
   * @param seriesId
   *          The series id
   * @param index
   *          The API index to update
   */
  private void removeSeriesFromIndex(String seriesId) {
    String orgId = securityService.getOrganization().getId();
    logger.debug("Removing series {} from the {} index.", seriesId, index.getIndexName());

    try {
      index.deleteSeries(seriesId, orgId);
      logger.debug("Series {} removed from the {} index.", seriesId, index.getIndexName());
    } catch (SearchIndexException e) {
      logger.error("Series {} couldn't be removed from the {} index.", seriesId, index.getIndexName(), e);
    }
  }

  /**
   * Update series metadata in API index. Also update events if series title has changed (optional).
   *
   * @param seriesId
   *          The series id
   * @param index
   *          The API index to update
   * @param dc
   *          The dublin core catalog
   */
  private void updateSeriesMetadataInIndex(String seriesId, DublinCoreCatalog dc) {
    String orgId = securityService.getOrganization().getId();
    logger.debug("Updating metadata of series {} in the {} index.", seriesId, index.getIndexName());

    // update series
    Function<Optional<Series>, Optional<Series>> updateFunction = getMetadataUpdateFunction(seriesId, dc, orgId);
    updateSeriesInIndex(seriesId, orgId, updateFunction);
  }

  /**
   * Get the function to update the metadata for a series in an API index.
   *
   * @param seriesId
   *          The series id
   * @param dc
   *          The dublin core catalog
   * @param orgId
   *          The id of the current organization
   * @return the function to do the update
   */
  private Function<Optional<Series>, Optional<Series>> getMetadataUpdateFunction(String seriesId, DublinCoreCatalog dc,
          String orgId) {
    return (Optional<Series> seriesOpt) -> {
      Series series = seriesOpt.orElse(new Series(seriesId, orgId));

      // only for new series
      if (!seriesOpt.isPresent()) {
        series.setCreator(securityService.getUser().getName());
      }

      series.setTitle(dc.getFirst(DublinCoreCatalog.PROPERTY_TITLE));
      series.setDescription(dc.getFirst(DublinCore.PROPERTY_DESCRIPTION));
      series.setSubject(dc.getFirst(DublinCore.PROPERTY_SUBJECT));
      series.setLanguage(dc.getFirst(DublinCoreCatalog.PROPERTY_LANGUAGE));
      series.setLicense(dc.getFirst(DublinCoreCatalog.PROPERTY_LICENSE));
      series.setRightsHolder(dc.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER));
      String createdDateStr = dc.getFirst(DublinCoreCatalog.PROPERTY_CREATED);
      if (createdDateStr != null) {
        series.setCreatedDateTime(EncodingSchemeUtils.decodeDate(createdDateStr));
      }
      series.setPublishers(dc.get(DublinCore.PROPERTY_PUBLISHER, DublinCore.LANGUAGE_ANY));
      series.setContributors(dc.get(DublinCore.PROPERTY_CONTRIBUTOR, DublinCore.LANGUAGE_ANY));
      series.setOrganizers(dc.get(DublinCoreCatalog.PROPERTY_CREATOR, DublinCore.LANGUAGE_ANY));
      return Optional.of(series);
    };
  }

  /**
   * Update series acl in API index.
   *
   * @param seriesId
   *          The series id
   * @param index
   *          The API index to update
   * @param acl
   *          The acl to update
   */
  private void updateSeriesAclInIndex(String seriesId, AccessControlList acl) {
    String orgId = securityService.getOrganization().getId();
    logger.debug("Updating ACL of series {} in the {} index.", seriesId, index.getIndexName());
    Function<Optional<Series>, Optional<Series>> updateFunction = getAclUpdateFunction(seriesId, acl, orgId);
    updateSeriesInIndex(seriesId, orgId, updateFunction);
  }

  /**
   * Get the function to update the acl for a series in an API index.
   *
   * @param seriesId
   *          The series id
   * @param acl
   *          The acl to update
   * @param orgId
   *          The id of the current organization
   * @return the function to do the update
   */
  private Function<Optional<Series>, Optional<Series>> getAclUpdateFunction(String seriesId, AccessControlList acl,
          String orgId) {
    return (Optional<Series> seriesOpt) -> {
      Series series = seriesOpt.orElse(new Series(seriesId, orgId));

      List<ManagedAcl> acls = aclServiceFactory.serviceFor(securityService.getOrganization()).getAcls();
      Option<ManagedAcl> managedAcl = AccessInformationUtil.matchAcls(acls, acl);
      if (managedAcl.isSome()) {
        series.setManagedAcl(managedAcl.get().getName());
      }

      series.setAccessPolicy(AccessControlParser.toJsonSilent(acl));
      return Optional.of(series);
    };
  }

  /**
   * Update series theme property in an API index.
   *
   * @param seriesId
   *          The series id
   * @param propertyValueOpt
   *          The value of the property (optional)
   * @param index
   *          The API index to update
   */
  private void updateThemePropertyInIndex(String seriesId, Optional<String> propertyValueOpt) {
    String orgId = securityService.getOrganization().getId();
    logger.debug("Updating theme property of series {} in the {} index.", seriesId, index.getIndexName());
    Function<Optional<Series>, Optional<Series>> updateFunction =
            getThemePropertyUpdateFunction(seriesId, propertyValueOpt, orgId);
    updateSeriesInIndex(seriesId, orgId, updateFunction);
  }

  /**
   * Get the function to update the theme property for a series in an API index.
   *
   * @param seriesId
   *          The series id
   * @param propertyValueOpt
   *          The value of the property (optional)
   * @param orgId
   *          The id of the current organization
   * @return the function to do the update
   */
  private Function<Optional<Series>, Optional<Series>> getThemePropertyUpdateFunction(String seriesId,
          Optional<String> propertyValueOpt, String orgId) {
    return (Optional<Series> seriesOpt) -> {
      Series series = seriesOpt.orElse(new Series(seriesId, orgId));
      if (propertyValueOpt.isPresent()) {
        series.setTheme(Long.valueOf(propertyValueOpt.get()));
      } else {
        series.setTheme(null);
      }
      return Optional.of(series);
    };
  }

  /**
   * Update a series in an API index.
   *
   * @param seriesId
   *          The series id
   * @param updateFunctions
   *          The function(s) to do the actual updating
   * @param orgId
   *          The id of the current organization
   * @return the updated series (optional)
   */
  @SafeVarargs
  private  Optional<Series> updateSeriesInIndex(String seriesId, String orgId,
          Function<Optional<Series>, Optional<Series>>... updateFunctions) {
    User user = securityService.getUser();
    Function<Optional<Series>, Optional<Series>> updateFunction = Arrays.stream(updateFunctions)
            .reduce(Function.identity(), Function::andThen);

    try {
      Optional<Series> seriesOpt = index.addOrUpdateSeries(seriesId, updateFunction, orgId, user);
      logger.debug("Series {} updated in the {} index", seriesId, index.getIndexName());
      return seriesOpt;
    } catch (SearchIndexException e) {
      logger.error("Series {} couldn't be updated in the {} index.", seriesId, index.getIndexName(), e);
      return Optional.empty();
    }
  }
}
