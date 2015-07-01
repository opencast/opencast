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

import org.opencastproject.index.IndexProducer;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.index.AbstractIndexProducer;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.message.broker.api.index.IndexRecreateObject.Service;
import org.opencastproject.message.broker.api.series.SeriesItem;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.FunctionException;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Implements {@link SeriesService}. Uses {@link SeriesServiceDatabase} for permanent storage and
 * {@link SeriesServiceIndex} for searching.
 */
public class SeriesServiceImpl extends AbstractIndexProducer implements SeriesService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceImpl.class);

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

  /** The message broker service receiver */
  protected MessageReceiver messageReceiver;

  /** The system user name */
  private String systemUserName;

  /** OSGi callback for setting index. */
  public void setIndex(SeriesServiceIndex index) {
    this.index = index;
  }

  /** OSGi callback for setting persistance. */
  public void setPersistence(SeriesServiceDatabase persistence) {
    this.persistence = persistence;
  }

  /** OSGi callback for setting the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback for setting the organization directory service. */
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  /** OSGi callback for setting the message sender. */
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /** OSGi callback for setting the message receiver. */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /**
   * Activates Series Service. Checks whether we are using synchronous or asynchronous indexing. If asynchronous is
   * used, Executor service is set. If index is empty, persistent storage is queried if it contains any series. If that
   * is the case, series are retrieved and indexed.
   */
  public void activate(ComponentContext cc) throws Exception {
    logger.info("Activating Series Service");
    systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    populateSolr(systemUserName);
    super.activate();
  }

  /** If the solr index is empty, but there are series in the database, populate the solr index. */
  private void populateSolr(String systemUserName) {
    long instancesInSolr = 0L;
    try {
      instancesInSolr = index.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    if (instancesInSolr == 0L) {
      try {
        Iterator<Tuple<DublinCoreCatalog, String>> databaseSeries = persistence.getAllSeries();
        if (databaseSeries.hasNext()) {
          logger.info("The series index is empty. Populating it now with series");
          while (databaseSeries.hasNext()) {
            Tuple<DublinCoreCatalog, String> series = databaseSeries.next();

            // Run as the superuser so we get all series, regardless of organization or role
            Organization organization = orgDirectory.getOrganization(series.getB());
            securityService.setOrganization(organization);
            securityService.setUser(SecurityUtil.createSystemUser(systemUserName, organization));

            index.updateIndex(series.getA());
            String id = series.getA().getFirst(DublinCore.PROPERTY_IDENTIFIER);
            AccessControlList acl = persistence.getAccessControlList(id);
            if (acl != null)
              index.updateSecurityPolicy(id, acl);
            index.updateOptOutStatus(id, persistence.isOptOut(id));
          }
          logger.info("Finished populating series search index");
        }
      } catch (Exception e) {
        logger.warn("Unable to index series instances: {}", e);
        throw new ServiceException(e.getMessage());
      } finally {
        securityService.setOrganization(null);
        securityService.setUser(null);
      }
    }
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
        try {
          index.updateOptOutStatus(id, persistence.isOptOut(id));
        } catch (NotFoundException ignore) {
          // Ignore not found since this is the first indexing
        }
        // Make sure store to persistence comes after index, return value can be null
        DublinCoreCatalog updated = persistence.storeSeries(dublinCore);
        messageSender.sendObjectMessage(SeriesItem.SERIES_QUEUE, MessageSender.DestinationType.Queue,
                SeriesItem.updateCatalog(dublinCore));
        return (updated == null) ? null : dublinCore;
      }
      return dc;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  private static Error rethrow(Exception e) throws SeriesException, UnauthorizedException {
    if (e instanceof FunctionException) {
      final Throwable cause = e.getCause();
      if (cause instanceof UnauthorizedException) {
        throw ((UnauthorizedException) cause);
      } else {
        throw new SeriesException(e);
      }
    } else {
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

  // todo method signature does not fit the three different possible return values
  @Override
  public boolean updateAccessControl(final String seriesId, final AccessControlList accessControl)
          throws NotFoundException, SeriesException {
    if (StringUtils.isEmpty(seriesId)) {
      throw new IllegalArgumentException("Series ID parameter must not be null or empty.");
    }
    if (accessControl == null) {
      throw new IllegalArgumentException("ACL parameter must not be null");
    }
    if (needsUpdate(seriesId, accessControl)) {
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
        messageSender.sendObjectMessage(SeriesItem.SERIES_QUEUE, MessageSender.DestinationType.Queue,
                SeriesItem.updateAcl(seriesId, accessControl));
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
      this.persistence.deleteSeries(seriesID);
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
      logger.error("Exception occurred while retrieving access control rules for series {}: {}", seriesID,
              e.getMessage());
      throw new SeriesException(e);
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
  public boolean isOptOut(String seriesId) throws NotFoundException, SeriesException {
    try {
      return index.isOptOut(seriesId);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occured while getting opt out status of series '{}': {}", seriesId,
              ExceptionUtils.getStackTrace(e));
      throw new SeriesException(e);
    }
  }

  @Override
  public void updateOptOutStatus(String seriesId, boolean optOut) throws NotFoundException, SeriesException {
    try {
      persistence.updateOptOutStatus(seriesId, optOut);
      index.updateOptOutStatus(seriesId, optOut);
      messageSender.sendObjectMessage(SeriesItem.SERIES_QUEUE, MessageSender.DestinationType.Queue,
              SeriesItem.updateOptOut(seriesId, optOut));
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to update opt out status of series with id '{}': {}", seriesId,
              ExceptionUtils.getStackTrace(e));
      throw new SeriesException(e);
    }
  }

  @Override
  public Map<String, String> getSeriesProperties(String seriesID) throws SeriesException, NotFoundException,
          UnauthorizedException {
    try {
      return persistence.getSeriesProperties(seriesID);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to get series properties for series with id '{}': {}", seriesID,
              ExceptionUtils.getStackTrace(e));
      throw new SeriesException(e);
    }
  }

  @Override
  public String getSeriesProperty(String seriesID, String propertyName) throws SeriesException, NotFoundException,
          UnauthorizedException {
    try {
      return persistence.getSeriesProperty(seriesID, propertyName);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to get series property for series with series id '{}' and property name '{}': {}",
              new Object[] { seriesID, propertyName, ExceptionUtils.getStackTrace(e) });
      throw new SeriesException(e);
    }
  }

  @Override
  public void updateSeriesProperty(String seriesID, String propertyName, String propertyValue) throws SeriesException,
          NotFoundException, UnauthorizedException {
    try {
      persistence.updateSeriesProperty(seriesID, propertyName, propertyValue);
      messageSender.sendObjectMessage(SeriesItem.SERIES_QUEUE, MessageSender.DestinationType.Queue,
              SeriesItem.updateProperty(seriesID, propertyName, propertyValue));
    } catch (SeriesServiceDatabaseException e) {
      logger.error(
              "Failed to get series property for series with series id '{}' and property name '{}' and value '{}': {}",
              new Object[] { seriesID, propertyName, propertyValue, ExceptionUtils.getStackTrace(e) });
      throw new SeriesException(e);
    }
  }

  @Override
  public void deleteSeriesProperty(String seriesID, String propertyName) throws SeriesException, NotFoundException,
          UnauthorizedException {
    try {
      persistence.deleteSeriesProperty(seriesID, propertyName);
      messageSender.sendObjectMessage(SeriesItem.SERIES_QUEUE, MessageSender.DestinationType.Queue,
              SeriesItem.updateProperty(seriesID, propertyName, null));
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to delete series property for series with series id '{}' and property name '{}': {}",
              new Object[] { seriesID, propertyName, ExceptionUtils.getStackTrace(e) });
      throw new SeriesException(e);
    }
  }

  /**
   * Define equality on DublinCoreCatalogs. Two DublinCores are considered equal if they have the same properties and if
   * each property has the same values in the same order.
   * <p/>
   * Note: As long as http://opencast.jira.com/browse/MH-8759 is not fixed, the encoding scheme of values is not
   * considered.
   * <p/>
   * Implementation Note: DublinCores should not be compared by their string serialization since the ordering of
   * properties is not defined and cannot be guaranteed between serializations.
   */
  public static boolean equals(DublinCoreCatalog a, DublinCoreCatalog b) {
    final Map<EName, List<DublinCoreValue>> av = a.getValues();
    final Map<EName, List<DublinCoreValue>> bv = b.getValues();
    if (av.size() == bv.size()) {
      for (Map.Entry<EName, List<DublinCoreValue>> ave : av.entrySet()) {
        if (!eqListSorted(ave.getValue(), bv.get(ave.getKey())))
          return false;
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
      if (persistence.existsSeriesElement(seriesID, type)) {
        return persistence.storeSeriesElement(seriesID, type, data);
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
  public void repopulate(final String indexName) {
    final String destinationId = SeriesItem.SERIES_QUEUE_PREFIX + WordUtils.capitalize(indexName);
    try {
      final int total = persistence.countSeries();
      logger.info("Re-populating '{}' index with series. There are {} series to add to the index.", indexName, total);
      Iterator<Tuple<DublinCoreCatalog, String>> databaseSeries = persistence.getAllSeries();
      final int[] current = new int[1];
      current[0] = 1;
      while (databaseSeries.hasNext()) {
        final Tuple<DublinCoreCatalog, String> series = databaseSeries.next();
        Organization organization = orgDirectory.getOrganization(series.getB());
        SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(systemUserName, organization),
                new Function0.X<Void>() {
                  @Override
                  public Void xapply() throws Exception {
                    String id = series.getA().getFirst(DublinCore.PROPERTY_IDENTIFIER);
                    logger.trace("Adding series '{}' for org '{}'", id, series.getB());
                    messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                            SeriesItem.updateCatalog(series.getA()));

                    AccessControlList acl = persistence.getAccessControlList(id);
                    if (acl != null) {
                      messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                              SeriesItem.updateAcl(id, acl));
                    }
                    messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                            SeriesItem.updateOptOut(id, persistence.isOptOut(id)));
                    for (Entry<String, String> property : persistence.getSeriesProperties(id).entrySet()) {
                      messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                              SeriesItem.updateProperty(id, property.getKey(), property.getValue()));
                    }
                    messageSender.sendObjectMessage(IndexProducer.RESPONSE_QUEUE, MessageSender.DestinationType.Queue,
                            IndexRecreateObject
                                    .update(indexName, IndexRecreateObject.Service.Series, total, current[0]));
                    current[0] += 1;
                    return null;
                  }
                });
      }
      logger.info("Finished populating '{}' index with series.", indexName);
    } catch (Exception e) {
      logger.warn("Unable to index series instances: {}", e);
      throw new ServiceException(e.getMessage());
    }

    Organization organization = new DefaultOrganization();
    SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(systemUserName, organization),
            new Effect0() {
              @Override
              protected void run() {
                messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                        IndexRecreateObject.end(indexName, IndexRecreateObject.Service.Series));
              }
            });
  }

  @Override
  public MessageReceiver getMessageReceiver() {
    return messageReceiver;
  }

  @Override
  public Service getService() {
    return Service.Series;
  }

  @Override
  public String getClassName() {
    return SeriesServiceImpl.class.getName();
  }

}
