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

package org.opencastproject.series.impl.persistence;

import com.entwinemedia.fn.data.Opt;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.series.impl.SeriesServiceDatabase;
import org.opencastproject.series.impl.SeriesServiceDatabaseException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

/**
 * Implements {@link SeriesServiceDatabase}. Defines permanent storage for series.
 */
public class SeriesServiceDatabaseImpl implements SeriesServiceDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceDatabaseImpl.class);

  /** Persistence provider set by OSGi */
  protected PersistenceProvider persistenceProvider;

  /** Persistence properties used to create {@link EntityManagerFactory} */
  protected Map<String, Object> persistenceProperties;

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /** Dublin core service for serializing and deserializing Dublin cores */
  protected DublinCoreCatalogService dcService;

  /** The security service */
  protected SecurityService securityService;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for series");
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.series.impl.persistence",
            persistenceProperties);
  }

  /**
   * Closes entity manager factory.
   *
   * @param cc
   */
  public void deactivate(ComponentContext cc) {
    emf.close();
  }

  /**
   * OSGi callback to set persistence properties.
   *
   * @param persistenceProperties
   *          persistence properties
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * OSGi callback to set persistence provider.
   *
   * @param persistenceProvider
   *          {@link PersistenceProvider} object
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set dublin core catalog service.
   *
   * @param dcService
   *          {@link DublinCoreCatalogService} object
   */
  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Serializes Dublin core catalog and returns it as String.
   *
   * @param dc
   *          {@link DublinCoreCatalog} to be serialized
   * @return String presenting serialized dublin core
   * @throws IOException
   *           if serialization fails
   */
  private String serializeDublinCore(DublinCoreCatalog dc) throws IOException {
    InputStream in = dcService.serialize(dc);

    StringWriter writer = new StringWriter();
    IOUtils.copy(in, writer, "UTF-8");

    return writer.toString();
  }

  /**
   * Parses Dublin core stored as string.
   *
   * @param dcXML
   *          string representation of Dublin core
   * @return parsed {@link DublinCoreCatalog}
   * @throws IOException
   *           if parsing fails
   */
  private DublinCoreCatalog parseDublinCore(String dcXML) throws IOException {
    DublinCoreCatalog dc = dcService.load(IOUtils.toInputStream(dcXML, "UTF-8"));
    return dc;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#deleteSeries(java.lang.String)
   */
  @Override
  public void deleteSeries(String seriesId) throws SeriesServiceDatabaseException, NotFoundException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null) {
        throw new NotFoundException("Series with ID " + seriesId + " does not exist");
      }
      // Ensure this user is allowed to delete this series
      String accessControlXml = entity.getAccessControl();
      if (accessControlXml != null) {
        AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
        User currentUser = securityService.getUser();
        Organization currentOrg = securityService.getOrganization();
        if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString())) {
          throw new UnauthorizedException(currentUser + " is not authorized to update series " + seriesId);
        }
      }
      em.remove(entity);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete series: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#deleteSeriesProperty(java.lang.String)
   */
  @Override
  public void deleteSeriesProperty(String seriesId, String propertyName) throws SeriesServiceDatabaseException,
          NotFoundException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null) {
        throw new NotFoundException("Series with ID " + seriesId + " does not exist");
      }
      Map<String, String> properties = entity.getProperties();
      String propertyValue = properties.get(propertyName);
      if (propertyValue == null) {
        throw new NotFoundException("Series with ID " + seriesId + " doesn't have a property with name '"
                + propertyName + "'");
      }

      if (!userHasWriteAccess(entity)) {
        throw new UnauthorizedException(securityService.getUser() + " is not authorized to delete series " + seriesId
                + " property " + propertyName);
      }

      properties.remove(propertyName);
      entity.setProperties(properties);
      em.merge(entity);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete series: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getAllSeries()
   */
  @SuppressWarnings("unchecked")
  @Override
  public Iterator<Tuple<DublinCoreCatalog, String>> getAllSeries() throws SeriesServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("Series.findAll");
    List<SeriesEntity> seriesEntities = null;
    try {
      seriesEntities = query.getResultList();
    } catch (Exception e) {
      logger.error("Could not retrieve all series: {}", e.getMessage());
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
    List<Tuple<DublinCoreCatalog, String>> seriesList = new LinkedList<Tuple<DublinCoreCatalog, String>>();
    try {
      for (SeriesEntity entity : seriesEntities) {
        DublinCoreCatalog dc = parseDublinCore(entity.getDublinCoreXML());
        seriesList.add(Tuple.tuple(dc, entity.getOrganization()));
      }
    } catch (Exception e) {
      logger.error("Could not parse series entity: {}", e.getMessage());
      throw new SeriesServiceDatabaseException(e);
    }
    return seriesList.iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getAccessControlList(java.lang.String)
   */
  @Override
  public AccessControlList getAccessControlList(String seriesId) throws NotFoundException,
          SeriesServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    try {
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null) {
        throw new NotFoundException("Could not found series with ID " + seriesId);
      }
      if (entity.getAccessControl() == null) {
        return null;
      } else {
        return AccessControlParser.parseAcl(entity.getAccessControl());
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve ACL for series '{}': {}", seriesId, e.getMessage());
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#storeSeries(org.opencastproject.metadata.dublincore.
   * DublinCoreCatalog)
   */
  @Override
  public DublinCoreCatalog storeSeries(DublinCoreCatalog dc) throws SeriesServiceDatabaseException,
          UnauthorizedException {
    if (dc == null) {
      throw new SeriesServiceDatabaseException("Invalid value for Dublin core catalog: null");
    }
    String seriesId = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    String seriesXML;
    try {
      seriesXML = serializeDublinCore(dc);
    } catch (Exception e1) {
      logger.error("Could not serialize Dublin Core: {}", e1);
      throw new SeriesServiceDatabaseException(e1);
    }
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    DublinCoreCatalog newSeries = null;
    try {
      tx.begin();
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null) {
        // no series stored, create new entity
        entity = new SeriesEntity();
        entity.setOrganization(securityService.getOrganization().getId());
        entity.setSeriesId(seriesId);
        entity.setSeries(seriesXML);
        em.persist(entity);
        newSeries = dc;
      } else {
        // Ensure this user is allowed to update this series
        String accessControlXml = entity.getAccessControl();
        if (accessControlXml != null) {
          AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
          User currentUser = securityService.getUser();
          Organization currentOrg = securityService.getOrganization();
          if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString())) {
            throw new UnauthorizedException(currentUser + " is not authorized to update series " + seriesId);
          }
        }
        entity.setSeries(seriesXML);
        em.merge(entity);
      }
      tx.commit();
      return newSeries;
    } catch (Exception e) {
      logger.error("Could not update series: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }

  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getSeries(java.lang.String)
   */
  @Override
  public DublinCoreCatalog getSeries(String seriesId) throws NotFoundException, SeriesServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null) {
        throw new NotFoundException("No series with id=" + seriesId + " exists");
      }
      // Ensure this user is allowed to read this series
      String accessControlXml = entity.getAccessControl();
      if (accessControlXml != null) {
        AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
        User currentUser = securityService.getUser();
        Organization currentOrg = securityService.getOrganization();
        // There are several reasons a user may need to load a series: to read content, to edit it, or add content
        if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.READ.toString())
                && !AccessControlUtil.isAuthorized(acl, currentUser, currentOrg,
                        Permissions.Action.CONTRIBUTE.toString())
                && !AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString())) {
          throw new UnauthorizedException(currentUser + " is not authorized to see series " + seriesId);
        }
      }
      return dcService.load(IOUtils.toInputStream(entity.getDublinCoreXML(), "UTF-8"));
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update series: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getSeriesProperties(java.lang.String)
   */
  @Override
  public Map<String, String> getSeriesProperties(String seriesId) throws NotFoundException,
          SeriesServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null) {
        throw new NotFoundException("No series with id=" + seriesId + " exists");
      }
      if (!userHasReadAccess(entity)) {
        throw new UnauthorizedException(securityService.getUser() + " is not authorized to see series " + seriesId
                + " properties");
      }
      return entity.getProperties();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update series: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getSeriesProperty(java.lang.String, java.lang.String)
   */
  @Override
  public String getSeriesProperty(String seriesId, String propertyName) throws NotFoundException,
          SeriesServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null) {
        throw new NotFoundException("No series with id=" + seriesId + " exists");
      }
      if (!userHasReadAccess(entity)) {
        throw new UnauthorizedException(securityService.getUser() + " is not authorized to see series " + seriesId
                + " properties");
      }
      if (entity.getProperties() == null || StringUtils.isBlank(entity.getProperties().get(propertyName))) {
        throw new NotFoundException("No series property for series with id=" + seriesId + " and property name "
                + propertyName);
      }
      return entity.getProperties().get(propertyName);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update series: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  private boolean userHasWriteAccess(SeriesEntity entity) throws IOException, AccessControlParsingException {
    // Ensure this user is allowed to write this series
    String accessControlXml = entity.getAccessControl();
    if (accessControlXml != null) {
      AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
      User currentUser = securityService.getUser();
      Organization currentOrg = securityService.getOrganization();
      if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString())) {
        return false;
      }
    }
    return true;
  }

  private boolean userHasReadAccess(SeriesEntity entity) throws IOException, AccessControlParsingException {
    // Ensure this user is allowed to read this series
    String accessControlXml = entity.getAccessControl();
    if (accessControlXml != null) {
      AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
      User currentUser = securityService.getUser();
      Organization currentOrg = securityService.getOrganization();
      // There are several reasons a user may need to load a series: to read content, to edit it, or add content
      if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.READ.toString())
              && !AccessControlUtil
                      .isAuthorized(acl, currentUser, currentOrg, Permissions.Action.CONTRIBUTE.toString())
              && !AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString())) {
        return false;
      }
    }
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#storeSeriesAccessControl(java.lang.String,
   * org.opencastproject.security.api.AccessControlList)
   */
  @Override
  public boolean storeSeriesAccessControl(String seriesId, AccessControlList accessControl) throws NotFoundException,
          SeriesServiceDatabaseException {
    if (accessControl == null) {
      logger.error("Access control parameter is <null> for series '{}'", seriesId);
      throw new IllegalArgumentException("Argument for updating ACL for series " + seriesId + " is null");
    }

    String serializedAC;
    try {
      serializedAC = AccessControlParser.toXml(accessControl);
    } catch (Exception e) {
      logger.error("Could not serialize access control parameter: {}", e.getMessage());
      throw new SeriesServiceDatabaseException(e);
    }
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    boolean updated = false;
    try {
      tx.begin();
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null) {
        throw new NotFoundException("Series with ID " + seriesId + " does not exist.");
      }
      if (entity.getAccessControl() != null) {
        // Ensure this user is allowed to update this series
        String accessControlXml = entity.getAccessControl();
        if (accessControlXml != null) {
          AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
          User currentUser = securityService.getUser();
          Organization currentOrg = securityService.getOrganization();
          if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString())) {
            throw new UnauthorizedException(currentUser + " is not authorized to update ACLs on series " + seriesId);
          }
        }
        updated = true;
      }
      entity.setAccessControl(serializedAC);
      em.merge(entity);
      tx.commit();
      return updated;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update series: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  @Override
  public int countSeries() throws SeriesServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("Series.getCount");
    try {
      Long total = (Long) query.getSingleResult();
      return total.intValue();
    } catch (Exception e) {
      logger.error("Could not find number of series.", e);
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  @Override
  public boolean isOptOut(String seriesId) throws NotFoundException, SeriesServiceDatabaseException {
    EntityManager em = emf.createEntityManager();
    try {
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null) {
        throw new NotFoundException("Could not found series with ID " + seriesId);
      }
      return entity.isOptOut();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve opt out status for series '{}': {}", seriesId, ExceptionUtils.getStackTrace(e));
      throw new SeriesServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void updateSeriesProperty(String seriesId, String propertyName, String propertyValue)
          throws NotFoundException, SeriesServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null) {
        throw new NotFoundException("Series with ID " + seriesId + " doesn't exist");
      }

      if (!userHasWriteAccess(entity)) {
        throw new UnauthorizedException(securityService.getUser() + " is not authorized to update series " + seriesId
                + " property " + propertyName + " to " + propertyValue);
      }

      Map<String, String> properties = entity.getProperties();
      properties.put(propertyName, propertyValue);
      entity.setProperties(properties);
      em.merge(entity);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Couldn't update series {} with property: {}:{} because {}", new Object[] { seriesId, propertyName,
              propertyValue, ExceptionUtils.getStackTrace(e) });
      throw new SeriesServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Updates a series' opt out status.
   *
   * @param seriesId
   *          The id of the series to update the opt out status of.
   * @param optOut
   *          Whether to opt out this series or not.
   */
  @Override
  public void updateOptOutStatus(String seriesId, boolean optOut) throws NotFoundException,
          SeriesServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SeriesEntity entity = getSeriesEntity(seriesId, em);
      if (entity == null)
        throw new NotFoundException("Series with ID " + seriesId + " does not exist");

      entity.setOptOut(optOut);
      em.merge(entity);
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      logger.error("Could not update series opted out status: {}", e.getMessage());
      throw new SeriesServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets a series by its ID, using the current organizational context.
   *
   * @param id
   *          the series identifier
   * @param em
   *          an open entity manager
   * @return the series entity, or null if not found
   */
  protected SeriesEntity getSeriesEntity(String id, EntityManager em) {
    String orgId = securityService.getOrganization().getId();
    Query q = em.createNamedQuery("seriesById").setParameter("seriesId", id).setParameter("organization", orgId);
    try {
      return (SeriesEntity) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @Override
  public boolean storeSeriesElement(String seriesId, String type, byte[] data) throws SeriesServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    final boolean success;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SeriesEntity series = getSeriesEntity(seriesId, em);
      if (series == null) {
        success = false;
      } else {
        series.addElement(type, data);
        em.merge(series);
        tx.commit();
        success = true;
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SeriesServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
    return success;
  }

  @Override
  public boolean deleteSeriesElement(String seriesId, String type) throws SeriesServiceDatabaseException {
    final boolean success;
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SeriesEntity series = getSeriesEntity(seriesId, em);
      if (series == null) {
        success = false;
      } else {
        if (series.getElements().containsKey(type)) {
          series.removeElement(type);
          em.merge(series);
          tx.commit();
          success = true;
        } else {
          success = false;
        }
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SeriesServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
    return success;
  }

  @Override
  public Opt<byte[]> getSeriesElement(String seriesId, String type) throws SeriesServiceDatabaseException {
    final Opt<byte[]> data;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      SeriesEntity series = getSeriesEntity(seriesId, em);
      if (series == null) {
        data = Opt.none();
      } else {
        Map<String, byte[]> elements = series.getElements();
        if (elements.containsKey(type))
          data = Opt.some(elements.get(type));
        else {
          data = Opt.none();
        }
      }
    } catch (Exception e) {
      throw new SeriesServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
    return data;
  }

  @Override
  public Opt<Map<String, byte[]>> getSeriesElements(String seriesId) throws SeriesServiceDatabaseException {
    final Opt<Map<String, byte[]>> elements;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      SeriesEntity series = getSeriesEntity(seriesId, em);
      if (series == null) {
        elements = Opt.none();
      } else {
        elements = Opt.some(series.getElements());
      }
    } catch (Exception e) {
      throw new SeriesServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
    return elements;
  }

  @Override
  public boolean existsSeriesElement(String seriesId, String type) throws SeriesServiceDatabaseException {
    return (getSeriesElement(seriesId, type).isSome()) ? true : false;
  }
}
