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
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ID;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.RequireUtil.notNull;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
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
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link SeriesService}. Uses {@link SeriesServiceDatabase} for permanent storage and
 * {@link SeriesServiceIndex} for searching.
 * 
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

  /**
   * OSGi callback for setting index.
   * 
   * @param index
   */
  public void setIndex(SeriesServiceIndex index) {
    this.index = index;
  }

  /**
   * OSGi callback for setting persistance.
   * 
   * @param persistence
   */
  public void setPersistence(SeriesServiceDatabase persistence) {
    this.persistence = persistence;
  }

  /**
   * OSGi callback for setting the security service.
   * 
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback for setting the event admin.
   * 
   * @param eventAdmin
   */
  public void setEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  /**
   * Activates Series Service. Checks whether we are using synchronous or asynchronous indexing. If asynchronous is
   * used, Executor service is set. If index is empty, persistent storage is queried if it contains any series. If that
   * is the case, series are retrieved and indexed.
   * 
   * @param cc
   *          ComponentContext
   * @throws Exception
   */
  public void activate(ComponentContext cc) throws Exception {
    logger.info("Activating Series Service");

    try {
      // Run as the superuser so we get all series, regardless of organization or role
      securityService.setOrganization(new DefaultOrganization());
      securityService.setUser(new User("seriesadmin", DEFAULT_ORGANIZATION_ID, new String[] { GLOBAL_ADMIN_ROLE }));
      populateSolr();
    } finally {
      securityService.setOrganization(null);
      securityService.setUser(null);
    }
  }

  /**
   * If the solr index is empty, but there are series in the database, populate the solr index.
   */
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
            index.index(series);
            String id = series.getFirst(DublinCore.PROPERTY_IDENTIFIER);
            AccessControlList acl = persistence.getAccessControlList(id);
            if (acl != null) {
              index.index(id, acl);
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

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.series.api.SeriesService#updateSeries(org.opencastproject.metadata.dublincore.DublinCoreCatalog
   * )
   */
  @Override
  public DublinCoreCatalog updateSeries(final DublinCoreCatalog dc) throws SeriesException, UnauthorizedException {
    if (dc == null) {
      throw new IllegalArgumentException("DC argument for updating series must not be null");
    }

    String identifier = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    if (identifier == null) {
      logger.info("Series Dublin Core does not contain Identifier, generating one");
      identifier = UUID.randomUUID().toString();
      dc.set(DublinCore.PROPERTY_IDENTIFIER, identifier);
    }

    DublinCoreCatalog newSeries;
    try {
      newSeries = persistence.storeSeries(dc);
    } catch (SeriesServiceDatabaseException e1) {
      logger.error("Could not store series {}: {}", dc, e1);
      throw new SeriesException(e1);
    }

    try {
      index.index(dc);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Unable to index series {}: {}", dc.getFirst(DublinCore.PROPERTY_IDENTIFIER), e.getMessage());
      throw new SeriesException(e);
    }

    String xml = null;
    try {
      xml = dc.toXmlString();
    } catch (IOException e) {
      throw new SeriesException(e);
    }
    sendEvent(SERIES_TOPIC, identifier, xml);

    return newSeries;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#updateAccessControl(java.lang.String,
   * org.opencastproject.security.api.AccessControlList)
   */
  @Override
  public boolean updateAccessControl(final String seriesID, final AccessControlList accessControl)
          throws NotFoundException, SeriesException {
    if (StringUtils.isEmpty(seriesID)) {
      throw new IllegalArgumentException("Series ID parameter must not be null or empty.");
    }
    if (accessControl == null) {
      throw new IllegalArgumentException("ACL parameter must not be null");
    }

    boolean updated;
    // try updating it in persistence first - not found is thrown if it doesn't exist
    try {
      updated = persistence.storeSeriesAccessControl(seriesID, accessControl);
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Could not update series {} with access control rules: {}", seriesID, e.getMessage());
      throw new SeriesException(e);
    }

    try {
      index.index(seriesID, accessControl);
    } catch (Exception e) {
      logger.error("Could not update series {} with access control rules: {}", seriesID, e.getMessage());
      throw new SeriesException(e);
    }

    String xml = null;
    try {
      xml = AccessControlParser.toXml(accessControl);
    } catch (IOException e) {
      throw new SeriesException(e);
    }
    sendEvent(SERIES_ACL_TOPIC, seriesID, xml);

    return updated;
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

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#getSeries(org.opencastproject.series.api.SeriesQuery)
   */
  @Override
  public DublinCoreCatalogList getSeries(SeriesQuery query) throws SeriesException {
    try {
      List<DublinCoreCatalog> result = index.search(query);
      DublinCoreCatalogList dcList = new DublinCoreCatalogList();
      dcList.setCatalogCount(getSeriesCount());
      dcList.setCatalogList(result);
      return dcList;
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Failed to execute search query: {}", e.getMessage());
      throw new SeriesException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#getSeries(java.lang.String)
   */
  @Override
  public DublinCoreCatalog getSeries(String seriesID) throws SeriesException, NotFoundException {
    try {
      return this.persistence.getSeries(notNull(seriesID, "seriesID"));
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occured while retrieving series {}: {}", seriesID, e.getMessage());
      throw new SeriesException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.api.SeriesService#getSeriesAccessControl(java.lang.String)
   */
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
  
  public int getSeriesCount() throws SeriesException {
    try {
      return persistence.countSeries();
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Exception occured while counting series.", e);
      throw new SeriesException(e);
    }
  }
}
