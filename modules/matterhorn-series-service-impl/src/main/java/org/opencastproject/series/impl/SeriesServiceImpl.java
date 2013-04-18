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

import static org.opencastproject.event.EventAdminConstants.ID;
import static org.opencastproject.event.EventAdminConstants.PAYLOAD;
import static org.opencastproject.event.EventAdminConstants.SERIES_ACL_TOPIC;
import static org.opencastproject.event.EventAdminConstants.SERIES_TOPIC;
import static org.opencastproject.util.EqualsUtil.bothNotNull;
import static org.opencastproject.util.EqualsUtil.eqListSorted;
import static org.opencastproject.util.EqualsUtil.eqListUnsorted;
import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.FunctionException;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

  /** The organization directory */
  protected OrganizationDirectoryService orgDirectory;

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

  /** OSGi callback for setting the organization directory service. */
  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
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
    String systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    populateSolr(systemUserName);
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
            if (acl != null) {
              index.updateSecurityPolicy(id, acl);
            }
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
        logger.debug("Updating series {}", id);
        index.updateIndex(dublinCore);
        try {
          final AccessControlList acl = persistence.getAccessControlList(id);
          if (acl != null)
            index.updateSecurityPolicy(id, acl);
        } catch (NotFoundException ignore) {
        }
        // Make sure store to persistence comes after index, return value can be null
        DublinCoreCatalog updated = persistence.storeSeries(dublinCore);
        sendEvent(SERIES_TOPIC, id, dublinCore.toXmlString());
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
      } catch (SeriesServiceDatabaseException e) {
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
   *          the event topic
   * @param objectId
   *          the series identifier
   * @param payload
   *          the event payload
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
}
