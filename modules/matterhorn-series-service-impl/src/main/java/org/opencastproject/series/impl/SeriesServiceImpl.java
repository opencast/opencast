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
package org.opencastproject.series.impl;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.FunctionException;
import org.opencastproject.util.data.Option;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.opencastproject.event.EventAdminConstants.ID;
import static org.opencastproject.event.EventAdminConstants.PAYLOAD;
import static org.opencastproject.event.EventAdminConstants.SERIES_ACL_TOPIC;
import static org.opencastproject.event.EventAdminConstants.SERIES_TOPIC;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ID;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.EqualsUtil.bothNotNull;
import static org.opencastproject.util.EqualsUtil.eqListSorted;
import static org.opencastproject.util.EqualsUtil.eqListUnsorted;
import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.data.Option.some;

/**
 * Implements {@link SeriesService}. Uses {@link SeriesServiceDatabase} for permanent storage and
 * {@link SeriesServiceIndex} for searching.
 */
public class SeriesServiceImpl implements SeriesService {

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceImpl.class);

  /** Index for searching */
  protected SeriesServiceIndex index;

  /** Persistent storage */
  protected SeriesServiceDatabase persistence;

  /** The security service */
  protected SecurityService securityService;

  /** The OSGI event admin service */
  protected EventAdmin eventAdmin;

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

  /** OSGi callback for setting the event admin. */
  public void setEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  /**
   * Activates Series Service. Checks whether we are using synchronous or asynchronous indexing. If asynchronous is
   * used, Executor service is set. If index is empty, persistent storage is queried if it contains any series. If that
   * is the case, series are retrieved and indexed.
   */
  public void activate(ComponentContext cc) throws Exception {
    logger.info("Activating Series Service");

    try {
      // Run as the superuser so we get all series, regardless of organization or role
      securityService.setOrganization(new DefaultOrganization());
      securityService.setUser(new User("seriesadmin", DEFAULT_ORGANIZATION_ID, new String[]{GLOBAL_ADMIN_ROLE}));
      populateSolr();
    } finally {
      securityService.setOrganization(null);
      securityService.setUser(null);
    }
  }

  /** If the solr index is empty, but there are series in the database, populate the solr index. */
  private void populateSolr() {
    long instancesInSolr = 0L;
    try {
      instancesInSolr = this.index.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    if (instancesInSolr == 0L) {
      try {
        DublinCoreCatalog[] databaseSeries = persistence.getAllSeries();
        if (databaseSeries.length != 0) {
          logger.info("The series index is empty. Populating it now with {} series",
                      Integer.valueOf(databaseSeries.length));
          for (DublinCoreCatalog series : databaseSeries) {
            index.updateIndex(series);
            String id = series.getFirst(DublinCore.PROPERTY_IDENTIFIER);
            AccessControlList acl = persistence.getAccessControlList(id);
            if (acl != null) {
              index.updateSecurityPolicy(id, acl);
            }
          }
          logger.info("Finished populating series search index");
        }
      } catch (Exception e) {
        logger.warn("Unable to index series instances: {}", e);
        throw new ServiceException(e.getMessage());
      }
    }
  }

  @Override
  public DublinCoreCatalog updateSeries(final DublinCoreCatalog dc) throws SeriesException, UnauthorizedException {
    try {
      return isNew(notNull(dc, "dc")).map(new Function.X<DublinCoreCatalog, DublinCoreCatalog>() {
        @Override public DublinCoreCatalog xapply(DublinCoreCatalog dc) throws
                SeriesServiceDatabaseException, UnauthorizedException, IOException {
          final String id = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);
          logger.debug("Updating series {}", id);
          persistence.storeSeries(dc);
          index.updateIndex(dc);
          try {
            final AccessControlList acl = persistence.getAccessControlList(id);
            if (acl != null)
              index.updateSecurityPolicy(id, acl);
          } catch (NotFoundException ignore) {
          }
          sendEvent(SERIES_TOPIC, id, dc.toXmlString());
          return dc;
        }
      }).getOrElse(dc);
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
        return equals(persistence.getSeries(id), dc) ? Option.<DublinCoreCatalog>none() : some(dc);
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
      // try updating it in persistence first - not found is thrown if it doesn't exist
      try {
        updated = persistence.storeSeriesAccessControl(seriesId, accessControl);
      } catch (SeriesServiceDatabaseException e) {
        logger.error("Could not update series {} with access control rules: {}", seriesId, e.getMessage());
        throw new SeriesException(e);
      }

      try {
        index.updateSecurityPolicy(seriesId, accessControl);
      } catch (Exception e) {
        logger.error("Could not update series {} with access control rules: {}", seriesId, e.getMessage());
        throw new SeriesException(e);
      }

      String xml = null;
      try {
        xml = AccessControlParser.toXml(accessControl);
      } catch (IOException e) {
        throw new SeriesException(e);
      }
      sendEvent(SERIES_ACL_TOPIC, seriesId, xml);
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

  /**
   * Sends an OSGI Event.
   *
   * @param topic
   *         the event topic
   * @param objectId
   *         the series identifier
   * @param payload
   *         the event payload
   */
  private void sendEvent(String topic, String objectId, String payload) {
    Dictionary<String, String> eventProperties = new Hashtable<String, String>();
    eventProperties.put(ID, objectId);
    eventProperties.put(PAYLOAD, payload);
    eventAdmin.postEvent(new Event(topic, eventProperties));
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
      return this.persistence.getSeries(notNull(seriesID, "seriesID"));
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occured while retrieving series {}: {}", seriesID, e.getMessage());
      throw new SeriesException(e);
    }
  }

  @Override
  public AccessControlList getSeriesAccessControl(String seriesID) throws NotFoundException, SeriesException {
    try {
      return persistence.getAccessControlList(notNull(seriesID, "seriesID"));
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occurred while retrieving access control rules for series {}: {}", seriesID,
                   e.getMessage());
      throw new SeriesException(e);
    }
  }

  @Override
  public int getSeriesCount() throws SeriesException {
    try {
      return persistence.countSeries();
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occured while counting series.", e);
      throw new SeriesException(e);
    }
  }

  /**
   * Define equality on DublinCoreCatalogs.
   * Two DublinCores are considered equal if they have the same properties and if each property
   * has the same values in the same order.
   * <p/>
   * Note: As long as http://opencast.jira.com/browse/MH-8759 is not fixed, the encoding scheme of values
   * is not considered.
   * <p/>
   * Implementation Note: DublinCores should not be compared by their string serialization since the ordering
   * of properties is not defined and cannot be guaranteed between serializations.
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
   * Define equality on AccessControlLists.
   * Two AccessControlLists are considered equal if they contain the exact same entries no matter in which order.
   */
  public static boolean equals(AccessControlList a, AccessControlList b) {
    return bothNotNull(a, b) && eqListUnsorted(a.getEntries(), b.getEntries());
  }
}
