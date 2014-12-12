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
package org.opencastproject.search.impl.persistence;

import static org.opencastproject.search.api.SearchService.CONTRIBUTE_PERMISSION;
import static org.opencastproject.search.api.SearchService.READ_PERMISSION;
import static org.opencastproject.search.api.SearchService.WRITE_PERMISSION;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.spi.PersistenceProvider;

/**
 * Implements {@link SearchServiceDatabase}. Defines permanent storage for series.
 */
public class SearchServiceDatabaseImpl implements SearchServiceDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceDatabaseImpl.class);

  /** Persistence provider set by OSGi */
  protected PersistenceProvider persistenceProvider;

  /** Persistence properties used to create {@link EntityManagerFactory} */
  protected Map<String, Object> persistenceProperties;

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /** The security service */
  protected SecurityService securityService;

  /** Whether the persistence provider has been set */
  private boolean hasPersistence = false;

  /** Whether the persistence properties have been set */
  private boolean hasPersistenceProperties = false;

 /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   * @throws SearchServiceDatabaseException 
   */
  public void activate(ComponentContext cc) throws SearchServiceDatabaseException {
    logger.info("Activating persistence manager for search service");
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.search.impl.persistence",
            persistenceProperties);
    this.hasPersistence = true;
    if (this.hasPersistenceProperties) {
      this.populateSeriesData();
    }
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
   * @throws SearchServiceDatabaseException 
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) throws SearchServiceDatabaseException {
    this.persistenceProperties = persistenceProperties;
    this.hasPersistenceProperties = true;
    if (this.hasPersistence) {
      this.populateSeriesData();
    }
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

  private void populateSeriesData() throws SearchServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      TypedQuery<SearchEntity> q = (TypedQuery<SearchEntity>) em.createNamedQuery("Search.getNoSeries");
      List<SearchEntity> seriesList = q.getResultList();
      for (SearchEntity series : seriesList) {
        String mpSeriesId = MediaPackageParser.getFromXml(series.getMediaPackageXML()).getSeries();
        if (StringUtils.isNotBlank(mpSeriesId) && !mpSeriesId.equals(series.getSeriesId())) {
          logger.info("Fixing missing series ID for episode {}, series is {}", series.getMediaPackageId(), mpSeriesId);
          series.setSeriesId(mpSeriesId);
          em.merge(series);
        }
      }
      tx.commit();
    } catch (Exception e) {
      logger.error("Could not update media package: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SearchServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#deleteMediaPackage(String, Date)
   */
  @Override
  public void deleteMediaPackage(String mediaPackageId, Date deletionDate) throws SearchServiceDatabaseException,
          NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();

      SearchEntity searchEntity = getSearchEntity(mediaPackageId, em);
      if (searchEntity == null)
        throw new NotFoundException("No media package with id=" + mediaPackageId + " exists");

      // Ensure this user is allowed to delete this episode
      String accessControlXml = searchEntity.getAccessControl();
      if (accessControlXml != null) {
        AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
        User currentUser = securityService.getUser();
        Organization currentOrg = securityService.getOrganization();
        if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, WRITE_PERMISSION))
          throw new UnauthorizedException(currentUser + " is not authorized to delete media package " + mediaPackageId);

        searchEntity.setDeletionDate(deletionDate);
        em.merge(searchEntity);
      }
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete episode {}: {}", mediaPackageId, e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SearchServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getAllMediaPackages()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Iterator<Tuple<MediaPackage, String>> getAllMediaPackages() throws SearchServiceDatabaseException {
    List<SearchEntity> searchEntities = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Search.findAll");
      searchEntities = (List<SearchEntity>) query.getResultList();
    } catch (Exception e) {
      logger.error("Could not retrieve all episodes: {}", e.getMessage());
      throw new SearchServiceDatabaseException(e);
    } finally {
      em.close();
    }
    List<Tuple<MediaPackage, String>> mediaPackageList = new LinkedList<Tuple<MediaPackage, String>>();
    try {
      for (SearchEntity entity : searchEntities) {
        MediaPackage mediaPackage = MediaPackageParser.getFromXml(entity.getMediaPackageXML());
        mediaPackageList.add(Tuple.tuple(mediaPackage, entity.getOrganization()));
      }
    } catch (Exception e) {
      logger.error("Could not parse series entity: {}", e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }
    return mediaPackageList.iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getAccessControlList(String)
   */
  @Override
  public AccessControlList getAccessControlList(String mediaPackageId) throws NotFoundException,
          SearchServiceDatabaseException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      SearchEntity entity = getSearchEntity(mediaPackageId, em);
      if (entity == null) {
        throw new NotFoundException("Could not found media package with ID " + mediaPackageId);
      }
      if (entity.getAccessControl() == null) {
        return null;
      } else {
        return AccessControlParser.parseAcl(entity.getAccessControl());
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve ACL {}: {}", mediaPackageId, e.getMessage());
      throw new SearchServiceDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#storeMediaPackage(MediaPackage,
   *      AccessControlList, Date)
   */
  @Override
  public void storeMediaPackage(MediaPackage mediaPackage, AccessControlList acl, Date now)
          throws SearchServiceDatabaseException, UnauthorizedException {
    String mediaPackageXML = MediaPackageParser.getAsXml(mediaPackage);
    String mediaPackageId = mediaPackage.getIdentifier().toString();
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SearchEntity entity = getSearchEntity(mediaPackageId, em);
      if (entity == null) {
        // Create new search entity
        SearchEntity searchEntity = new SearchEntity();
        searchEntity.setOrganization(securityService.getOrganization().getId());
        searchEntity.setMediaPackageId(mediaPackageId);
        searchEntity.setMediaPackageXML(mediaPackageXML);
        searchEntity.setAccessControl(AccessControlParser.toXml(acl));
        searchEntity.setModificationDate(now);
        searchEntity.setSeriesId(mediaPackage.getSeries());
        em.persist(searchEntity);
      } else {
        // Ensure this user is allowed to update this media package
        String accessControlXml = entity.getAccessControl();
        if (accessControlXml != null) {
          AccessControlList accessList = AccessControlParser.parseAcl(accessControlXml);
          User currentUser = securityService.getUser();
          Organization currentOrg = securityService.getOrganization();
          if (!AccessControlUtil.isAuthorized(accessList, currentUser, currentOrg, WRITE_PERMISSION)) {
            throw new UnauthorizedException(currentUser + " is not authorized to update media package "
                    + mediaPackageId);
          }
        }
        entity.setOrganization(securityService.getOrganization().getId());
        entity.setMediaPackageId(mediaPackageId);
        entity.setMediaPackageXML(mediaPackageXML);
        entity.setAccessControl(AccessControlParser.toXml(acl));
        entity.setModificationDate(now);
        entity.setSeriesId(mediaPackage.getSeries());
        em.merge(entity);
      }
      tx.commit();
    } catch (Exception e) {
      logger.error("Could not update media package: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SearchServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getMediaPackage(String)
   */
  @Override
  public MediaPackage getMediaPackage(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SearchEntity episodeEntity = getSearchEntity(mediaPackageId, em);
      if (episodeEntity == null)
        throw new NotFoundException("No episode with id=" + mediaPackageId + " exists");
      // Ensure this user is allowed to read this episode
      String accessControlXml = episodeEntity.getAccessControl();
      if (accessControlXml != null) {
        AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
        User currentUser = securityService.getUser();
        Organization currentOrg = securityService.getOrganization();
        // There are several reasons a user may need to load a episode: to read content, to edit it, or add content
        if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, READ_PERMISSION)
                && !AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, CONTRIBUTE_PERMISSION)
                && !AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, WRITE_PERMISSION)) {
          throw new UnauthorizedException(currentUser + " is not authorized to see episode " + mediaPackageId);
        }
      }
      return MediaPackageParser.getFromXml(episodeEntity.getMediaPackageXML());
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get episode {} from database: {} ", mediaPackageId, e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SearchServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getModificationDate(String)
   */
  @Override
  public Date getModificationDate(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SearchEntity searchEntity = getSearchEntity(mediaPackageId, em);
      if (searchEntity == null)
        throw new NotFoundException("No media package with id=" + mediaPackageId + " exists");
      // Ensure this user is allowed to read this media package
      String accessControlXml = searchEntity.getAccessControl();
      if (accessControlXml != null) {
        AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
        User currentUser = securityService.getUser();
        Organization currentOrg = securityService.getOrganization();
        if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, READ_PERMISSION))
          throw new UnauthorizedException(currentUser + " is not authorized to read media package " + mediaPackageId);
      }
      return searchEntity.getModificationDate();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get modification date {}: {}", mediaPackageId, e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SearchServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getDeletionDate(String)
   */
  @Override
  public Date getDeletionDate(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SearchEntity searchEntity = getSearchEntity(mediaPackageId, em);
      if (searchEntity == null) {
        throw new NotFoundException("No media package with id=" + mediaPackageId + " exists");
      }
      // Ensure this user is allowed to read this media package
      String accessControlXml = searchEntity.getAccessControl();
      if (accessControlXml != null) {
        AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
        User currentUser = securityService.getUser();
        Organization currentOrg = securityService.getOrganization();
        if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, READ_PERMISSION))
          throw new UnauthorizedException(currentUser + " is not authorized to read media package " + mediaPackageId);
      }
      return searchEntity.getDeletionDate();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get deletion date {}: {}", mediaPackageId, e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SearchServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getOrganizationId(String)
   */
  @Override
  public String getOrganizationId(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      SearchEntity searchEntity = getSearchEntity(mediaPackageId, em);
      if (searchEntity == null)
        throw new NotFoundException("No media package with id=" + mediaPackageId + " exists");
      // Ensure this user is allowed to read this media package
      String accessControlXml = searchEntity.getAccessControl();
      if (accessControlXml != null) {
        AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
        User currentUser = securityService.getUser();
        Organization currentOrg = securityService.getOrganization();
        if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, READ_PERMISSION))
          throw new UnauthorizedException(currentUser + " is not authorized to read media package " + mediaPackageId);
      }
      return searchEntity.getOrganization();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get deletion date {}: {}", mediaPackageId, e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new SearchServiceDatabaseException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets a search entity by it's id, using the current organizational context.
   *
   * @param id
   *          the media package identifier
   * @param em
   *          an open entity manager
   * @return the search entity, or null if not found
   */
  private SearchEntity getSearchEntity(String id, EntityManager em) {
    Query q = em.createNamedQuery("Search.findById").setParameter("mediaPackageId", id);
    try {
      return (SearchEntity) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
