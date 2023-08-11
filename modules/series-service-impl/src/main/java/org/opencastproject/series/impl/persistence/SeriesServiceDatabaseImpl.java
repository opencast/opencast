/*
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

import static org.opencastproject.db.Queries.namedQuery;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.impl.SeriesServiceDatabase;
import org.opencastproject.series.impl.SeriesServiceDatabaseException;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

/**
 * Implements {@link SeriesServiceDatabase}. Defines permanent storage for series.
 */
@Component(
    property = {
        "service.description=Series Service"
    },
    immediate = true,
    service = { SeriesServiceDatabase.class }
)
public class SeriesServiceDatabaseImpl implements SeriesServiceDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceDatabaseImpl.class);

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.series.impl.persistence";

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** Dublin core service for serializing and deserializing Dublin cores */
  protected DublinCoreCatalogService dcService;

  /** The security service */
  protected SecurityService securityService;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.series.impl.persistence)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for series");
    db = dbSessionFactory.createSession(emf);
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set dublin core catalog service.
   *
   * @param dcService
   *          {@link DublinCoreCatalogService} object
   */
  @Reference
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

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#deleteSeries(java.lang.String)
   */
  @Override
  public void deleteSeries(String seriesId) throws SeriesServiceDatabaseException, NotFoundException {
    try {
      db.execTxChecked(em -> {
        Optional<SeriesEntity> entity = getSeriesEntity(seriesId).apply(em);
        if (entity.isEmpty()) {
          throw new NotFoundException("Series with ID " + seriesId + " does not exist");
        }
        // Ensure this user is allowed to delete this series
        String accessControlXml = entity.get().getAccessControl();
        if (accessControlXml != null) {
          AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
          User currentUser = securityService.getUser();
          Organization currentOrg = securityService.getOrganization();
          if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString())) {
            throw new UnauthorizedException(currentUser + " is not authorized to update series " + seriesId);
          }
        }

        Date now = new Date();
        entity.get().setModifiedDate(now);
        entity.get().setDeletionDate(now);
        em.merge(entity.get());
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete series", e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#deleteSeriesProperty(java.lang.String)
   */
  @Override
  public void deleteSeriesProperty(String seriesId, String propertyName)
          throws SeriesServiceDatabaseException, NotFoundException {
    try {
      db.execTxChecked(em -> {
        Optional<SeriesEntity> entity = getSeriesEntity(seriesId).apply(em);
        if (entity.isEmpty()) {
          throw new NotFoundException("Series with ID " + seriesId + " does not exist");
        }
        Map<String, String> properties = entity.get().getProperties();
        String propertyValue = properties.get(propertyName);
        if (propertyValue == null) {
          throw new NotFoundException(
              "Series with ID " + seriesId + " doesn't have a property with name '" + propertyName + "'");
        }

        if (!userHasWriteAccess(entity.get())) {
          throw new UnauthorizedException(securityService.getUser() + " is not authorized to delete series " + seriesId
              + " property " + propertyName);
        }

        properties.remove(propertyName);
        entity.get().setProperties(properties);
        entity.get().setModifiedDate(new Date());
        em.merge(entity.get());
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete property for series '{}'", seriesId, e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getAllSeries()
   */
  @Override
  public List<SeriesEntity> getAllSeries() throws SeriesServiceDatabaseException {
    try {
      return db.exec(namedQuery.findAll("Series.findAll", SeriesEntity.class));
    } catch (Exception e) {
      logger.error("Could not retrieve all series", e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getAccessControlList(java.lang.String)
   */
  @Override
  public AccessControlList getAccessControlList(String seriesId)
          throws NotFoundException, SeriesServiceDatabaseException {
    try {
      return db.execChecked(em -> {
        Optional<SeriesEntity> entity = getSeriesEntity(seriesId).apply(em);
        if (entity.isEmpty()) {
          throw new NotFoundException("Could not found series with ID " + seriesId);
        }
        if (entity.get().getAccessControl() == null) {
          return null;
        } else {
          return AccessControlParser.parseAcl(entity.get().getAccessControl());
        }
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve ACL for series '{}'", seriesId, e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#storeSeries(org.opencastproject.metadata.dublincore.
   * DublinCoreCatalog)
   */
  @Override
  public DublinCoreCatalog storeSeries(DublinCoreCatalog dc)
          throws SeriesServiceDatabaseException, UnauthorizedException {
    if (dc == null) {
      throw new SeriesServiceDatabaseException("Invalid value for Dublin core catalog: null");
    }

    String seriesId = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    String seriesXML;
    try {
      seriesXML = serializeDublinCore(dc);
    } catch (Exception e1) {
      logger.error("Could not serialize Dublin Core:", e1);
      throw new SeriesServiceDatabaseException(e1);
    }

    try {
      return db.execTxChecked(em -> {
        DublinCoreCatalog newSeries = null;
        Optional<SeriesEntity> entity = getPotentiallyDeletedSeriesEntity(seriesId).apply(em);
        if (entity.isEmpty() || entity.get().isDeleted()) {
          // If the series existed but is marked deleted, we completely delete it
          // here to make sure no remains of the old series linger.
          if (entity.isPresent()) {
            em.remove(entity.get());
            em.flush();
          }

          // no series stored, create new entity
          SeriesEntity newEntity = new SeriesEntity();
          newEntity.setOrganization(securityService.getOrganization().getId());
          newEntity.setSeriesId(seriesId);
          newEntity.setSeries(seriesXML);
          newEntity.setModifiedDate(new Date());
          em.persist(newEntity);
          newSeries = dc;
        } else {
          // Ensure this user is allowed to update this series
          String accessControlXml = entity.get().getAccessControl();
          if (accessControlXml != null) {
            AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
            User currentUser = securityService.getUser();
            Organization currentOrg = securityService.getOrganization();
            if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString())) {
              throw new UnauthorizedException(currentUser + " is not authorized to update series " + seriesId);
            }
          }
          entity.get().setSeries(seriesXML);
          entity.get().setModifiedDate(new Date());
          em.merge(entity.get());
        }
        return newSeries;
      });
    } catch (Exception e) {
      logger.error("Could not update series", e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getSeries(java.lang.String)
   */
  @Override
  public DublinCoreCatalog getSeries(String seriesId) throws NotFoundException, SeriesServiceDatabaseException {
    try {
      return db.execTxChecked(em -> {
        Optional<SeriesEntity> entity = getSeriesEntity(seriesId).apply(em);
        if (entity.isEmpty()) {
          throw new NotFoundException("No series with id=" + seriesId + " exists");
        }
        // Ensure this user is allowed to read this series
        if (entity == null) {
          throw new NotFoundException("No series with id=" + seriesId + " exists");
        }
        return dcService.load(IOUtils.toInputStream(entity.get().getDublinCoreXML(), "UTF-8"));
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve series with ID '{}'", seriesId, e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  @Override
  public List<Series> getAllForAdministrativeRead(Date from, Optional<Date> to, int limit)
          throws SeriesServiceDatabaseException, UnauthorizedException {
    // Validate parameters
    if (limit <= 0) {
      throw new IllegalArgumentException("limit has to be > 0");
    }

    // Make sure the user is actually an administrator of sorts
    User user = securityService.getUser();
    if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole())) {
      throw new UnauthorizedException(user, getClass().getName() + ".getModifiedInRangeForAdministrativeRead");
    }

    // Load series from DB.
    try {
      List<SeriesEntity> result = db.exec(em -> {
        TypedQuery<SeriesEntity> q;
        if (to.isPresent()) {
          if (from.after(to.get())) {
            throw new IllegalArgumentException("`from` is after `to`");
          }

          q = em.createNamedQuery("Series.getAllModifiedInRange", SeriesEntity.class)
              .setParameter("from", from)
              .setParameter("to", to.get())
              .setParameter("organization", user.getOrganization().getId())
              .setMaxResults(limit);
        } else {
          q = em.createNamedQuery("Series.getAllModifiedSince", SeriesEntity.class)
              .setParameter("since", from)
              .setParameter("organization", user.getOrganization().getId())
              .setMaxResults(limit);
        }
        return q.getResultList();
      });

      final List<Series> out = new ArrayList<>();
      for (SeriesEntity entity : result) {
        final Series series = new Series();
        series.setId(entity.getSeriesId());
        series.setOrganization(entity.getOrganization());
        series.setDublinCore(DublinCoreXmlFormat.read(entity.getDublinCoreXML()));
        series.setAccessControl(entity.getAccessControl());
        series.setModifiedDate(entity.getModifiedDate());
        series.setDeletionDate(entity.getDeletionDate());
        out.add(series);
      }

      return out;
    } catch (Exception e) {
      String msg = String.format("Could not retrieve series modified between '%s' and '%s'", from, to);
      throw new SeriesServiceDatabaseException(msg, e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getSeriesProperties(java.lang.String)
   */
  @Override
  public Map<String, String> getSeriesProperties(String seriesId)
          throws NotFoundException, SeriesServiceDatabaseException {
    try {
      return db.execTxChecked(em -> {
        Optional<SeriesEntity> entity = getSeriesEntity(seriesId).apply(em);
        if (entity.isEmpty()) {
          throw new NotFoundException("No series with id=" + seriesId + " exists");
        }
        if (!userHasReadAccess(entity.get())) {
          throw new UnauthorizedException(
              securityService.getUser() + " is not authorized to see series " + seriesId + " properties");
        }
        return entity.get().getProperties();
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve properties of series '{}'", seriesId, e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.series.impl.SeriesServiceDatabase#getSeriesProperty(java.lang.String, java.lang.String)
   */
  @Override
  public String getSeriesProperty(String seriesId, String propertyName)
          throws NotFoundException, SeriesServiceDatabaseException {
    try {
      return db.execTxChecked(em -> {
        Optional<SeriesEntity> entity = getSeriesEntity(seriesId).apply(em);
        if (entity.isEmpty()) {
          throw new NotFoundException("No series with id=" + seriesId + " exists");
        }
        if (!userHasReadAccess(entity.get())) {
          throw new UnauthorizedException(
              securityService.getUser() + " is not authorized to see series " + seriesId + " properties");
        }
        if (entity.get().getProperties() == null
            || StringUtils.isBlank(entity.get().getProperties().get(propertyName))) {
          throw new NotFoundException(
              "No series property for series with id=" + seriesId + " and property name " + propertyName);
        }
        return entity.get().getProperties().get(propertyName);
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve property '{}' of series '{}'", propertyName, seriesId, e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  private boolean userHasWriteAccess(SeriesEntity entity) throws IOException, AccessControlParsingException {
    // Ensure this user is allowed to write this series
    String accessControlXml = entity.getAccessControl();
    if (accessControlXml != null) {
      AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
      User currentUser = securityService.getUser();
      Organization currentOrg = securityService.getOrganization();
      return AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString());
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

      if (currentUser.hasRole(SecurityConstants.GLOBAL_CAPTURE_AGENT_ROLE)) {
        return true;
      }
      // There are several reasons a user may need to load a series: to read content, to edit it, or add content
      return AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.READ.toString())
          || AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.CONTRIBUTE.toString())
          || AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, Permissions.Action.WRITE.toString());
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
  public boolean storeSeriesAccessControl(String seriesId, AccessControlList accessControl)
          throws NotFoundException, SeriesServiceDatabaseException {
    if (accessControl == null) {
      logger.error("Access control parameter is <null> for series '{}'", seriesId);
      throw new IllegalArgumentException("Argument for updating ACL for series " + seriesId + " is null");
    }

    String serializedAC;
    try {
      serializedAC = AccessControlParser.toXml(accessControl);
    } catch (Exception e) {
      logger.error("Could not serialize access control parameter", e);
      throw new SeriesServiceDatabaseException(e);
    }

    try {
      return db.execTxChecked(em -> {
        boolean updated = false;

        Optional<SeriesEntity> entity = getSeriesEntity(seriesId).apply(em);
        if (entity.isEmpty()) {
          throw new NotFoundException("Series with ID " + seriesId + " does not exist.");
        }
        if (entity.get().getAccessControl() != null) {
          // Ensure this user is allowed to update this series
          String accessControlXml = entity.get().getAccessControl();
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
        entity.get().setAccessControl(serializedAC);
        entity.get().setModifiedDate(new Date());
        em.merge(entity.get());
        return updated;
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not store ACL for series '{}'", seriesId, e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  @Override
  public int countSeries() throws SeriesServiceDatabaseException {
    try {
      return db.exec(namedQuery.find("Series.getCount", Long.class)).intValue();
    } catch (Exception e) {
      logger.error("Could not find number of series.", e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  @Override
  public void updateSeriesProperty(String seriesId, String propertyName, String propertyValue)
          throws NotFoundException, SeriesServiceDatabaseException {
    try {
      db.execTxChecked(em -> {
        Optional<SeriesEntity> entity = getSeriesEntity(seriesId).apply(em);
        if (entity.isEmpty()) {
          throw new NotFoundException("Series with ID " + seriesId + " doesn't exist");
        }

        if (!userHasWriteAccess(entity.get())) {
          throw new UnauthorizedException(securityService.getUser() + " is not authorized to update series " + seriesId
              + " property " + propertyName + " to " + propertyValue);
        }

        Map<String, String> properties = entity.get().getProperties();
        properties.put(propertyName, propertyValue);
        entity.get().setProperties(properties);
        entity.get().setModifiedDate(new Date());
        em.merge(entity.get());
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Couldn't update series {} with property: {}:{} because", seriesId, propertyName, propertyValue, e);
      throw new SeriesServiceDatabaseException(e);
    }
  }

  /**
   * Gets a series by its ID, using the current organizational context.
   *
   * @param id
   *          the series identifier
   * @return the series entity, or null if not found or if the series is deleted.
   */
  protected Function<EntityManager, Optional<SeriesEntity>> getSeriesEntity(String id) {
    return em -> getPotentiallyDeletedSeriesEntity(id).apply(em).filter(e -> !e.isDeleted());
  }

  /**
   * Gets a potentially deleted series by its ID, using the current organizational context.
   *
   * @param id
   *          the series identifier
   * @return the series entity, or null if not found
   */
  protected Function<EntityManager, Optional<SeriesEntity>> getPotentiallyDeletedSeriesEntity(String id) {
    String orgId = securityService.getOrganization().getId();
    return namedQuery.findOpt(
        "seriesById",
        SeriesEntity.class,
        Pair.of("seriesId", id),
        Pair.of("organization", orgId)
    );
  }

  @Override
  public boolean storeSeriesElement(String seriesId, String type, byte[] data) throws SeriesServiceDatabaseException {
    try {
      return db.execTx(em -> {
        Optional<SeriesEntity> series = getSeriesEntity(seriesId).apply(em);
        if (series.isEmpty()) {
          return false;
        }
        series.get().addElement(type, data);
        series.get().setModifiedDate(new Date());
        em.merge(series.get());
        return true;
      });
    } catch (Exception e) {
      throw new SeriesServiceDatabaseException(e);
    }
  }

  @Override
  public boolean deleteSeriesElement(String seriesId, String type) throws SeriesServiceDatabaseException {
    try {
      return db.execTx(em -> {
        Optional<SeriesEntity> series = getSeriesEntity(seriesId).apply(em);
        if (series.isEmpty()) {
          return false;
        }

        if (!series.get().getElements().containsKey(type)) {
          return false;
        }

        series.get().removeElement(type);
        series.get().setModifiedDate(new Date());
        em.merge(series.get());
        return true;
      });
    } catch (Exception e) {
      throw new SeriesServiceDatabaseException(e);
    }
  }

  @Override
  public Opt<byte[]> getSeriesElement(String seriesId, String type) throws SeriesServiceDatabaseException {
    try {
      return db.exec(em -> {
        Optional<SeriesEntity> series = getSeriesEntity(seriesId).apply(em);
        if (series.isEmpty()) {
          return Opt.none();
        }

        Map<String, byte[]> elements = series.get().getElements();
        if (!elements.containsKey(type)) {
          return Opt.none();
        }

        return Opt.some(elements.get(type));
      });

    } catch (Exception e) {
      throw new SeriesServiceDatabaseException(e);
    }
  }

  @Override
  public Opt<Map<String, byte[]>> getSeriesElements(String seriesId) throws SeriesServiceDatabaseException {
    try {
      return db.exec(em -> {
        Optional<SeriesEntity> series = getSeriesEntity(seriesId).apply(em);
        if (series.isEmpty()) {
          return Opt.none();
        }
        return Opt.some(series.get().getElements());
      });
    } catch (Exception e) {
      throw new SeriesServiceDatabaseException(e);
    }
  }

  @Override
  public boolean existsSeriesElement(String seriesId, String type) throws SeriesServiceDatabaseException {
    return getSeriesElement(seriesId, type).isSome();
  }
}
