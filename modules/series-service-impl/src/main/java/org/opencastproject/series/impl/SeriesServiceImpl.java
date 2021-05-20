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
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.elasticsearch.index.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.index.rebuild.AbstractIndexProducer;
import org.opencastproject.index.rebuild.IndexProducer;
import org.opencastproject.index.rebuild.IndexRebuildException;
import org.opencastproject.index.rebuild.IndexRebuildService;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.series.SeriesItem;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
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
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.series.impl.persistence.SeriesEntity;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Implements {@link SeriesService}. Uses {@link SeriesServiceDatabase} for permanent storage and
 * {@link SeriesServiceIndex} for searching.
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

  /** Index for searching */
  protected SeriesServiceIndex index;

  /** Persistent storage */
  protected SeriesServiceDatabase persistence;

  /** The security service */
  protected SecurityService securityService;

  /** The organization directory */
  protected OrganizationDirectoryService orgDirectory;

  /** The message broker service sender */
  protected MessageSender messageSender;

  /** The system user name */
  private String systemUserName;

  /** The Elasticsearch indices */
  private AbstractSearchIndex adminUiIndex;
  private AbstractSearchIndex externalApiIndex;

  private AclServiceFactory aclServiceFactory;

  /** OSGi callback for setting index. */
  @Reference(name = "series-index")
  public void setIndex(SeriesServiceIndex index) {
    this.index = index;
  }

  /** OSGi callback for setting persistance. */
  @Reference(name = "series-persistence")
  public void setPersistence(SeriesServiceDatabase persistence) {
    this.persistence = persistence;
  }

  /** OSGi callback for setting the security service. */
  @Reference(name = "security-service")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback for setting the organization directory service. */
  @Reference(name = "orgDirectory")
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  /** OSGi callback for setting the message sender. */
  @Reference(name = "message-broker-sender")
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /** OSGi callbacks for setting the Elasticsearch indices. */
  @Reference(name = "admin-ui-index", target = "(index.name=adminui)")
  public void setAdminUiIndex(AbstractSearchIndex index) {
    this.adminUiIndex = index;
  }

  @Reference(name = "external-api-index", target = "(index.name=externalapi)")
  public void setExternalApiIndex(AbstractSearchIndex index) {
    this.externalApiIndex = index;
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
    populateSolr(systemUserName);
  }

  /** If the solr index is empty, but there are series in the database, populate the solr index. */
  private void populateSolr(String systemUserName) {
    long instancesInSolr;
    try {
      instancesInSolr = index.count();
    } catch (Exception e) {
      throw new IllegalStateException("Repopulating series Solr index failed", e);
    }
    if (instancesInSolr != 0L) {
      return;
    }

    logger.info("The series index is empty. Populating it now with series");
    List<SeriesEntity> allSeries = null;
    try {
      allSeries = persistence.getAllSeries();
    } catch (SeriesServiceDatabaseException ex) {
      throw new ServiceException("Unable to get all series from the database", ex);
    }
    final int total = allSeries.size();
    if (total == 0) {
      logger.info("No series found. Repopulating index finished.");
      return;
    }

    int current = 0;
    for (SeriesEntity series: allSeries) {
      current++;
      try {
        // Run as the superuser so we get all series, regardless of organization or role
        Organization organization = orgDirectory.getOrganization(series.getOrganization());
        securityService.setOrganization(organization);
        securityService.setUser(SecurityUtil.createSystemUser(systemUserName, organization));

        index.updateIndex(DublinCoreXmlFormat.read(series.getDublinCoreXML()));
        String aclStr = series.getAccessControl();
        if (StringUtils.isNotBlank(aclStr)) {
          AccessControlList acl = AccessControlParser.parseAcl(aclStr);
          index.updateSecurityPolicy(series.getSeriesId(), acl);
        }
      } catch (Exception ex) {
        logger.error("Unable to repopulate index for series {}", series.getSeriesId(), ex);
      } finally {
        securityService.setOrganization(null);
        securityService.setUser(null);
      }
      // log progress
      if (current % 100 == 0) {
        logger.info("Indexing series {}/{} ({} percent done)", current, total, current * 100 / total);
      }
    }
    logger.info("Finished populating series search index");
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
        index.updateIndex(dublinCore);
        try {
          final AccessControlList acl = persistence.getAccessControlList(id);
          if (acl != null) {
            index.updateSecurityPolicy(id, acl);
          }
        } catch (NotFoundException ignore) {
          // Ignore not found since this is the first indexing
        }
        // Make sure store to persistence comes after index, return value can be null
        DublinCoreCatalog updated = persistence.storeSeries(dublinCore);
        // update ES indices
        updateSeriesMetadataInIndex(id, adminUiIndex, dublinCore, true);
        updateSeriesMetadataInIndex(id, externalApiIndex, dublinCore, true);
        // still sent for other asynchronous updates
        messageSender.sendObjectMessage(SeriesItem.SERIES_QUEUE, MessageSender.DestinationType.Queue,
                SeriesItem.updateCatalog(dublinCore));
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
        return equals(persistence.getSeries(id), dc) ? Option.<DublinCoreCatalog> none() : some(dc);
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
      // not found is thrown if it doesn't exist
      try {
        index.updateSecurityPolicy(seriesId, accessControl);
      } catch (SeriesServiceDatabaseException e) {
        logger.error("Could not update series {} with access control rules: {}", seriesId, e.getMessage());
        throw new SeriesException(e);
      }

      try {
        updated = persistence.storeSeriesAccessControl(seriesId, accessControl);
        //update ES indices
        updateSeriesAclInIndex(seriesId, adminUiIndex, accessControl);
        updateSeriesAclInIndex(seriesId, externalApiIndex, accessControl);
        // still sent for other asynchronous updates
        messageSender.sendObjectMessage(SeriesItem.SERIES_QUEUE, MessageSender.DestinationType.Queue,
                SeriesItem.updateAcl(seriesId, accessControl, overrideEpisodeAcl));
      } catch (SeriesServiceDatabaseException e) {
        logger.error("Could not update series {} with access control rules: {}", seriesId, e.getMessage());
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
      // remove from ES indices
      removeSeriesFromIndex(seriesID, adminUiIndex);
      removeSeriesFromIndex(seriesID, externalApiIndex);
      // still sent for other asynchronous updates
      messageSender.sendObjectMessage(SeriesItem.SERIES_QUEUE, MessageSender.DestinationType.Queue,
              SeriesItem.delete(seriesID));
    } catch (SeriesServiceDatabaseException e1) {
      logger.error("Could not delete series with id {} from persistence storage", seriesID);
      throw new SeriesException(e1);
    }

    try {
      index.delete(seriesID);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Unable to delete series with id {}: {}", seriesID, e.getMessage());
      throw new SeriesException(e);
    }
  }

  @Override
  public DublinCoreCatalogList getSeries(SeriesQuery query) throws SeriesException {
    try {
      return index.search(query);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to execute search query: {}", e.getMessage());
      throw new SeriesException(e);
    }
  }

  @Override
  public Map<String, String> getIdTitleMapOfAllSeries() throws SeriesException, UnauthorizedException {
    try {
      return index.queryIdTitleMap();
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to execute search query: {}", e.getMessage());
      throw new SeriesException(e);
    }
  }

  @Override
  public DublinCoreCatalog getSeries(String seriesID) throws SeriesException, NotFoundException {
    try {
      return index.getDublinCore(notNull(seriesID, "seriesID"));
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occured while retrieving series {}: {}", seriesID, e.getMessage());
      throw new SeriesException(e);
    }
  }

  @Override
  public AccessControlList getSeriesAccessControl(String seriesID) throws NotFoundException, SeriesException {
    try {
      return index.getAccessControl(notNull(seriesID, "seriesID"));
    } catch (SeriesServiceDatabaseException e) {
      throw new SeriesException(
          String.format("Exception occurred while retrieving access control rules for series %s", seriesID), e);
    }
  }

  @Override
  public int getSeriesCount() throws SeriesException {
    try {
      return (int) index.count();
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occured while counting series.", e);
      throw new SeriesException(e);
    }
  }

  @Override
  public Map<String, String> getSeriesProperties(String seriesID)
          throws SeriesException, NotFoundException, UnauthorizedException {
    try {
      return persistence.getSeriesProperties(seriesID);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to get series properties for series with id '{}'", seriesID, e);
      throw new SeriesException(e);
    }
  }

  @Override
  public String getSeriesProperty(String seriesID, String propertyName)
          throws SeriesException, NotFoundException, UnauthorizedException {
    try {
      return persistence.getSeriesProperty(seriesID, propertyName);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to get series property for series with series id '{}' and property name '{}'", seriesID,
              propertyName, e);
      throw new SeriesException(e);
    }
  }

  @Override
  public void updateSeriesProperty(String seriesID, String propertyName, String propertyValue)
          throws SeriesException, NotFoundException, UnauthorizedException {
    try {
      persistence.updateSeriesProperty(seriesID, propertyName, propertyValue);

      // update ES indices
      if (propertyName.equals(THEME_PROPERTY_NAME)) {
        updateThemePropertyInIndex(seriesID, Optional.ofNullable(propertyValue), adminUiIndex);
        updateThemePropertyInIndex(seriesID, Optional.ofNullable(propertyValue), externalApiIndex);
      }
    } catch (SeriesServiceDatabaseException e) {
      logger.error(
              "Failed to get series property for series with series id '{}' and property name '{}' and value '{}'",
              seriesID, propertyName, propertyValue, e);
      throw new SeriesException(e);
    }
  }

  @Override
  public void deleteSeriesProperty(String seriesID, String propertyName)
          throws SeriesException, NotFoundException, UnauthorizedException {
    try {
      persistence.deleteSeriesProperty(seriesID, propertyName);

      // update ES indices
      if (propertyName.equals(THEME_PROPERTY_NAME)) {
        updateThemePropertyInIndex(seriesID, Optional.empty(), adminUiIndex);
        updateThemePropertyInIndex(seriesID, Optional.empty(), externalApiIndex);
      }
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to delete series property for series with series id '{}' and property name '{}'",
              seriesID, propertyName, e);
      throw new SeriesException(e);
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
        messageSender.sendObjectMessage(SeriesItem.SERIES_QUEUE, MessageSender.DestinationType.Queue,
                SeriesItem.updateElement(seriesID, type, new String(data, StandardCharsets.UTF_8)));
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
  public void repopulate(final AbstractSearchIndex index) throws IndexRebuildException {
    final String destinationId = SeriesItem.SERIES_QUEUE_PREFIX + index.getIndexName().substring(0, 1).toUpperCase()
            + index.getIndexName().substring(1);
    try {
      final int total = persistence.countSeries();
      logIndexRebuildBegin(logger, index.getIndexName(), total, "series");
      List<SeriesEntity> databaseSeries = persistence.getAllSeries();
      int current = 1;
      for (SeriesEntity series: databaseSeries) {
        Organization organization = orgDirectory.getOrganization(series.getOrganization());
        User systemUser = SecurityUtil.createSystemUser(systemUserName, organization);
        SecurityUtil.runAs(securityService, organization, systemUser,
                () -> {
                  String id = series.getSeriesId();
                  logger.trace("Adding series '{}' for org '{}'", id, series.getOrganization());
                  DublinCoreCatalog catalog;
                  try {
                    catalog = DublinCoreXmlFormat.read(series.getDublinCoreXML());
                  } catch (IOException | ParserConfigurationException | SAXException e) {
                    logger.error("Could not read dublincore XML", e);
                    return;
                  }
                  updateSeriesMetadataInIndex(id, index, catalog, false);

                  String aclStr = series.getAccessControl();
                  if (StringUtils.isNotBlank(aclStr)) {
                    try {
                      AccessControlList acl = AccessControlParser.parseAcl(aclStr);
                      updateSeriesAclInIndex(id, index, acl);
                    } catch (Exception ex) {
                      logger.error("Unable to parse series {} access control list", id, ex);
                    }
                  }
                  try {
                    Map<String, String> properties = persistence.getSeriesProperties(id);
                    if (properties.containsKey(THEME_PROPERTY_NAME)) {
                      updateThemePropertyInIndex(id, Optional.ofNullable(properties.get(THEME_PROPERTY_NAME)), index);
                    }
                  } catch (NotFoundException | SeriesServiceDatabaseException e) {
                    logger.error("Error requesting series properties", e);
                  }
                });
        logIndexRebuildProgress(logger, index.getIndexName(), total, current);
        current++;
      }
    } catch (Exception e) {
      logIndexRebuildError(logger, index.getIndexName(), e);
      throw new IndexRebuildException(index.getIndexName(), getService(), e);
    }
  }

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.Series;
  }

  /**
   * Remove series from Elasticsearch index.
   *
   * @param seriesId
   *          The series id
   * @param index
   *          The Elasticsearch index to update
   */
  private void removeSeriesFromIndex(String seriesId, AbstractSearchIndex index) {
    String orgId = securityService.getOrganization().getId();
    logger.debug("Received Delete Series Event {} for index {}", seriesId, index.getIndexName());

    try {
      index.delete(Series.DOCUMENT_TYPE, seriesId, orgId);
      logger.debug("Series {} removed from search index", seriesId);
    } catch (SearchIndexException e) {
      logger.error("Error deleting the series {} from the search index", seriesId, e);
    }
  }

  /**
   * Update series metadata in Elasticsearch index. Also update events if series title has changed (optional).
   *
   * @param seriesId
   *          The series id
   * @param index
   *          The Elasticsearch index to update
   * @param dc
   *          The dublin core catalog
   */
  private void updateSeriesMetadataInIndex(String seriesId, AbstractSearchIndex index, DublinCoreCatalog dc,
          boolean updateEvents) {
    String orgId = securityService.getOrganization().getId();
    User user = securityService.getUser();
    logger.debug("Received Update Series for index {}", index.getIndexName());

    Function<Optional<Series>, Optional<Series>> updateFunction = (Optional<Series> seriesOpt) -> {
      Series series = seriesOpt.orElse(new Series(seriesId, orgId));
      series.setCreator(securityService.getUser().getName());

      // update from dublin core catalog
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

    // update series
    Optional<Series> updatedSeriesOpt;
    try {
      updatedSeriesOpt = index.addOrUpdateSeries(seriesId, updateFunction, orgId,
              user);
      logger.debug("Series {} updated in the search index", seriesId);
    } catch (SearchIndexException e) {
      logger.error("Error storing the series {} to the search index", seriesId, e);
      return;
    }

    // update series title for events
    if (updateEvents && updatedSeriesOpt.isPresent() && updatedSeriesOpt.get().isSeriesTitleUpdated()) {
      Series updatedSeries = updatedSeriesOpt.get();
      SearchResult<Event> events;
      try {
        events = index.getByQuery(
                new EventSearchQuery(orgId, user).withoutActions().withSeriesId(updatedSeries.getIdentifier()));
      } catch (SearchIndexException e) {
        logger.error("Error requesting the events of the series {} from the index", seriesId, e);
        return;
      }

      for (SearchResultItem<Event> searchResultItem : events.getItems()) {
        String eventId = searchResultItem.getSource().getIdentifier();

        Function<Optional<Event>, Optional<Event>> eventUpdateFunction = (Optional<Event> eventOpt) -> {
          if (eventOpt.isPresent() && eventOpt.get().getSeriesId().equals(updatedSeries.getIdentifier())) {
            Event event = eventOpt.get();
            event.setSeriesName(updatedSeries.getTitle());
            return Optional.of(event);
          }
          return Optional.empty();
        };

        try {
          index.addOrUpdateEvent(eventId, eventUpdateFunction, orgId, user);
          logger.debug("Series title of series {} updated for event {} in the index", seriesId, eventId);
        } catch (SearchIndexException e) {
          logger.error("Error updating the series title for event {} to the index", eventId, e);
        }
      }
    }
  }

  /**
   * Update series acl in Elasticsearch index.
   *
   * @param seriesId
   *          The series id
   * @param index
   *          The Elasticsearch index to update
   * @param acl
   *          The acl to update
   */
  private void updateSeriesAclInIndex(String seriesId, AbstractSearchIndex index, AccessControlList acl) {
    String orgId = securityService.getOrganization().getId();
    User user = securityService.getUser();
    logger.debug("Received Update Series ACL for index {}", index.getIndexName());

    Function<Optional<Series>, Optional<Series>> updateFunction = (Optional<Series> seriesOpt) -> {
      Series series = seriesOpt.orElse(new Series(seriesId, orgId));

      List<ManagedAcl> acls = aclServiceFactory.serviceFor(securityService.getOrganization()).getAcls();
      Option<ManagedAcl> managedAcl = AccessInformationUtil.matchAcls(acls, acl);
      if (managedAcl.isSome())
        series.setManagedAcl(managedAcl.get().getName());

      series.setAccessPolicy(AccessControlParser.toJsonSilent(acl));
      return Optional.of(series);
    };

    try {
      index.addOrUpdateSeries(seriesId, updateFunction, orgId, user);
      logger.debug("Series {} updated in the search index", seriesId);
    } catch (SearchIndexException e) {
      logger.error("Error storing the series {} to the search index", seriesId, e);
    }
  }

  /**
   * Update series theme property in Elasticsearch index.
   *
   * @param seriesId
   *          The series id
   * @param propertyValueOpt
   *          The value of the property (optional)
   * @param index
   *          The Elasticsearch index to update
   */
  private void updateThemePropertyInIndex(String seriesId, Optional<String> propertyValueOpt,
          AbstractSearchIndex index) {
    String orgId = securityService.getOrganization().getId();
    User user = securityService.getUser();
    logger.debug("Updating theme property of series {} in index {}", seriesId, index.getIndexName());

    Function<Optional<Series>, Optional<Series>> updateFunction = (Optional<Series> seriesOpt) -> {
      Series series = seriesOpt.orElse(new Series(seriesId, orgId));
      if (propertyValueOpt.isPresent()) {
        series.setTheme(Long.valueOf(propertyValueOpt.get()));
      } else {
        series.setTheme(null);
      }
      return Optional.of(series);
    };

    try {
      index.addOrUpdateSeries(seriesId, updateFunction, orgId, user);
      logger.debug("Series {} updated in the search index", seriesId);
    } catch (SearchIndexException e) {
      logger.error("Error storing the series {} to the search index", seriesId, e);
    }
  }
}
