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

package org.opencastproject.search.impl.persistence;

import static org.opencastproject.db.Queries.namedQuery;
import static org.opencastproject.security.api.Permissions.Action.CONTRIBUTE;
import static org.opencastproject.security.api.Permissions.Action.READ;
import static org.opencastproject.security.api.Permissions.Action.WRITE;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_CAPTURE_AGENT_ROLE;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

/**
 * Implements {@link SearchServiceDatabase}. Defines permanent storage for series.
 */
@Component(
    immediate = true,
    service = SearchServiceDatabase.class,
    property = {
        "service.description=Search Service Persistence"
    }
)
public class SearchServiceDatabaseImpl implements SearchServiceDatabase {

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.search.impl.persistence";

  private static final String CONFIG_EPISODE_ID_ROLE = "org.opencastproject.episode.id.role.access";

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceDatabaseImpl.class);

  private boolean episodeRoleId = false;

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** The security service */
  protected SecurityService securityService;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.search.impl.persistence)")
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
   * @throws SearchServiceDatabaseException
   */
  @Activate
  public void activate(ComponentContext cc) throws SearchServiceDatabaseException {
    logger.info("Activating persistence manager for search service");
    db = dbSessionFactory.createSession(emf);
    this.populateSeriesData();

    episodeRoleId = BooleanUtils.toBoolean(Objects.toString(
        cc.getBundleContext().getProperty(CONFIG_EPISODE_ID_ROLE), "false"));
    logger.debug("Usage of episode ID roles is set to {}", episodeRoleId);
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

  private void populateSeriesData() throws SearchServiceDatabaseException {
    try {
      db.execTxChecked(em -> {
        TypedQuery<SearchEntity> q = em.createNamedQuery("Search.getNoSeries", SearchEntity.class);
        List<SearchEntity> seriesList = q.getResultList();
        for (SearchEntity series : seriesList) {
          String mpSeriesId = MediaPackageParser.getFromXml(series.getMediaPackageXML()).getSeries();
          if (StringUtils.isNotBlank(mpSeriesId) && !mpSeriesId.equals(series.getSeriesId())) {
            logger.info("Fixing missing series ID for episode {}, series is {}", series.getMediaPackageId(),
                mpSeriesId);
            series.setSeriesId(mpSeriesId);
            em.merge(series);
          }
        }
      });
    } catch (Exception e) {
      logger.error("Could not update media package: {}", e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#deleteMediaPackage(String, Date)
   */
  @Override
  public void deleteMediaPackage(String mediaPackageId, Date deletionDate) throws SearchServiceDatabaseException,
          NotFoundException, UnauthorizedException {
    try {
      db.execTxChecked(em -> {
        Optional<SearchEntity> searchEntity = getSearchEntityQuery(mediaPackageId).apply(em);
        if (searchEntity.isEmpty()) {
          throw new NotFoundException("No media package with id=" + mediaPackageId + " exists");
        }

        // Ensure this user is allowed to delete this episode
        User currentUser = securityService.getUser();
        Organization currentOrg = securityService.getOrganization();
        MediaPackage searchMp = MediaPackageParser.getFromXml(searchEntity.get().getMediaPackageXML());
        String accessControlXml = searchEntity.get().getAccessControl();

        // allow ca users to retract live publications without putting them into the ACL
        if (!(searchMp.isLive() && currentUser.hasRole(GLOBAL_CAPTURE_AGENT_ROLE)) && accessControlXml != null) {
          AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
          if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, WRITE.toString(), mediaPackageId)) {
            throw new UnauthorizedException(
                currentUser + " is not authorized to delete media package " + mediaPackageId);
          }
        }

        searchEntity.get().setDeletionDate(deletionDate);
        searchEntity.get().setModificationDate(deletionDate);
        em.merge(searchEntity.get());
      });
    } catch (NotFoundException | UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete episode {}: {}", mediaPackageId, e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#countMediaPackages()
   */
  @Override
  public int countMediaPackages() throws SearchServiceDatabaseException {
    try {
      return db.exec(namedQuery.find("Search.getCount", Long.class)).intValue();
    } catch (Exception e) {
      logger.error("Could not find number of mediapackages", e);
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getAllMediaPackages(int, int)
   */
  @Override
  public Stream<Tuple<MediaPackage, String>> getAllMediaPackages(int pagesize, int offset)
          throws SearchServiceDatabaseException {
    List<SearchEntity> searchEntities;
    try {
      int firstResult = pagesize * offset;
      searchEntities = db.exec(namedQuery.findSome("Search.findAll", firstResult, pagesize, SearchEntity.class));
    } catch (Exception e) {
      logger.error("Could not retrieve all episodes: {}", e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }

    try {
      return searchEntities.stream()
            .map(entity -> {
              try {
                MediaPackage mediaPackage = MediaPackageParser.getFromXml(entity.getMediaPackageXML());
                return Tuple.tuple(mediaPackage, entity.getOrganization().getId());
              } catch (Exception e) {
                logger.error("Could not parse series entity: {}", e.getMessage());
                throw new RuntimeException(e);
              }
            });
    } catch (Exception e) {
      logger.error("Could not parse series entity: {}", e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getAccessControlList(String)
   */
  @Override
  public AccessControlList getAccessControlList(String mediaPackageId) throws NotFoundException,
          SearchServiceDatabaseException {
    try {
      Optional<SearchEntity> entity = db.exec(getSearchEntityQuery(mediaPackageId));
      if (entity.isEmpty()) {
        throw new NotFoundException("Could not found media package with ID " + mediaPackageId);
      }
      if (entity.get().getAccessControl() == null) {
        return null;
      } else {
        return AccessControlParser.parseAcl(entity.get().getAccessControl());
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve ACL {}", mediaPackageId, e);
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getAccessControlLists(String, String...)
   */
  @Override
  public Collection<AccessControlList> getAccessControlLists(final String seriesId, String ... excludeIds)
          throws SearchServiceDatabaseException {
    List<String> excludes = Arrays.asList(excludeIds);
    List<AccessControlList> accessControlLists = new ArrayList<>();
    try {
      List<SearchEntity> result = db.exec(namedQuery.findAll(
          "Search.findBySeriesId",
          SearchEntity.class,
          Pair.of("seriesId", seriesId)
      ));
      for (SearchEntity entity: result) {
        if (entity.getAccessControl() != null && !excludes.contains(entity.getMediaPackageId())) {
          accessControlLists.add(AccessControlParser.parseAcl(entity.getAccessControl()));
        }
      }
    } catch (IOException | AccessControlParsingException e) {
      throw new SearchServiceDatabaseException(e);
    }
    return accessControlLists;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getSeries(String)
   */
  public Collection<Pair<Organization, MediaPackage>> getSeries(final String seriesId)
          throws SearchServiceDatabaseException {
    List<Pair<Organization, MediaPackage>> episodes = new ArrayList<>();
    EntityManager em = emf.createEntityManager();
    TypedQuery<SearchEntity> q = em.createNamedQuery("Search.findBySeriesId", SearchEntity.class)
        .setParameter("seriesId", seriesId);
    try {
      for (SearchEntity entity: q.getResultList()) {
        if (entity.getMediaPackageXML() != null) {
          episodes.add(Pair.of(
              entity.getOrganization(),
              MediaPackageParser.getFromXml(entity.getMediaPackageXML())));
        }
      }
    } catch (MediaPackageException e) {
      throw new SearchServiceDatabaseException(e);
    } finally {
      em.close();
    }
    return episodes;
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
    try {
      db.execTxChecked(em -> {
        Optional<SearchEntity> entity = getSearchEntityQuery(mediaPackageId).apply(em);
        if (entity.isEmpty()) {
          // Create new search entity
          SearchEntity searchEntity = new SearchEntity();
          searchEntity.setOrganization(securityService.getOrganization());
          searchEntity.setMediaPackageId(mediaPackageId);
          searchEntity.setMediaPackageXML(mediaPackageXML);
          searchEntity.setAccessControl(AccessControlParser.toXml(acl));
          searchEntity.setModificationDate(now);
          searchEntity.setSeriesId(mediaPackage.getSeries());
          em.persist(searchEntity);
        } else {
          // Ensure this user is allowed to update this media package
          // If user has ROLE_EPISODE_<ID>_WRITE, no further permission checks are necessary
          String accessControlXml = entity.get().getAccessControl();
          if (accessControlXml != null && entity.get().getDeletionDate() == null) {
            AccessControlList accessList = AccessControlParser.parseAcl(accessControlXml);
            User currentUser = securityService.getUser();
            Organization currentOrg = securityService.getOrganization();
            if (!AccessControlUtil.isAuthorized(accessList, currentUser, currentOrg, WRITE.toString(),
                mediaPackageId)) {
              throw new UnauthorizedException(currentUser + " is not authorized to update media package "
                  + mediaPackageId);
            }
          }
          entity.get().setOrganization(securityService.getOrganization());
          entity.get().setMediaPackageId(mediaPackageId);
          entity.get().setMediaPackageXML(mediaPackageXML);
          entity.get().setAccessControl(AccessControlParser.toXml(acl));
          entity.get().setModificationDate(now);
          entity.get().setDeletionDate(null);
          entity.get().setSeriesId(mediaPackage.getSeries());
          em.merge(entity.get());
        }
      });
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not update media package: {}", e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getMediaPackage(String)
   */
  @Override
  public MediaPackage getMediaPackage(String mediaPackageId)
          throws NotFoundException, SearchServiceDatabaseException, UnauthorizedException {
    try {
      return db.execTxChecked(em -> {
        Optional<SearchEntity> episodeEntity = getSearchEntityQuery(mediaPackageId).apply(em);
        if (episodeEntity.isEmpty() || episodeEntity.get().getDeletionDate() != null) {
          throw new NotFoundException("No episode with id=" + mediaPackageId + " exists");
        }

        String accessControlXml = episodeEntity.get().getAccessControl();
        if (accessControlXml != null) {
          AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
          User currentUser = securityService.getUser();
          Organization currentOrg = securityService.getOrganization();
          // There are several reasons a user may need to load a episode: to read content, to edit it, or add content
          if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, READ.toString(), mediaPackageId)
                  && !AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, CONTRIBUTE.toString(),
              mediaPackageId)
                  && !AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, WRITE.toString(), mediaPackageId)) {
            throw new UnauthorizedException(currentUser + " is not authorized to see episode " + mediaPackageId);
          }
        }
        return MediaPackageParser.getFromXml(episodeEntity.get().getMediaPackageXML());
      });
    } catch (NotFoundException | UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get episode {} from database: {} ", mediaPackageId, e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getModificationDate(String)
   */
  @Override
  public Date getModificationDate(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException {
    try {
      return db.execTxChecked(em -> {
        Optional<SearchEntity> searchEntity = getSearchEntityQuery(mediaPackageId).apply(em);
        if (searchEntity.isEmpty()) {
          throw new NotFoundException("No media package with id=" + mediaPackageId + " exists");
        }
        // Ensure this user is allowed to read this media package
        String accessControlXml = searchEntity.get().getAccessControl();
        if (accessControlXml != null) {
          AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
          User currentUser = securityService.getUser();
          Organization currentOrg = securityService.getOrganization();
          if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, READ.toString(), mediaPackageId)) {
            throw new UnauthorizedException(
                currentUser + " is not authorized to read media package " + mediaPackageId);
          }
        }
        return searchEntity.get().getModificationDate();
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get modification date {}: {}", mediaPackageId, e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getDeletionDate(String)
   */
  @Override
  public Date getDeletionDate(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException {
    try {
      return db.execTxChecked(em -> {
        Optional<SearchEntity> searchEntity = getSearchEntityQuery(mediaPackageId).apply(em);
        if (searchEntity.isEmpty()) {
          throw new NotFoundException("No media package with id=" + mediaPackageId + " exists");
        }
        // Ensure this user is allowed to read this media package
        String accessControlXml = searchEntity.get().getAccessControl();
        if (accessControlXml != null) {
          AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
          User currentUser = securityService.getUser();
          Organization currentOrg = securityService.getOrganization();
          if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, READ.toString(), mediaPackageId)) {
            throw new UnauthorizedException(
                currentUser + " is not authorized to read media package " + mediaPackageId);
          }
        }
        return searchEntity.get().getDeletionDate();
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get deletion date {}: {}", mediaPackageId, e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#isAvailable(String)
   */
  public boolean isAvailable(String mediaPackageId) throws SearchServiceDatabaseException {
    try {
      return db.execTxChecked(em -> {
        Optional<SearchEntity> searchEntity = getSearchEntityQuery(mediaPackageId).apply(em);
        return searchEntity.stream().anyMatch(entity -> entity.getDeletionDate() == null);
      });
    } catch (Exception e) {
      logger.error("Error while checking if mediapackage {} exists in database: {}", mediaPackageId, e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.impl.persistence.SearchServiceDatabase#getOrganizationId(String)
   */
  @Override
  public String getOrganizationId(String mediaPackageId) throws NotFoundException, SearchServiceDatabaseException {
    try {
      return db.execTxChecked(em -> {
        Optional<SearchEntity> searchEntity = getSearchEntityQuery(mediaPackageId).apply(em);
        if (searchEntity.isEmpty()) {
          throw new NotFoundException("No media package with id=" + mediaPackageId + " exists");
        }
        // Ensure this user is allowed to read this media package
        String accessControlXml = searchEntity.get().getAccessControl();
        if (accessControlXml != null) {
          AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
          User currentUser = securityService.getUser();
          Organization currentOrg = securityService.getOrganization();
          if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, READ.toString(), mediaPackageId)) {
            throw new UnauthorizedException(
                currentUser + " is not authorized to read media package " + mediaPackageId);
          }
        }
        return searchEntity.get().getOrganization().getId();
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not get deletion date {}: {}", mediaPackageId, e.getMessage());
      throw new SearchServiceDatabaseException(e);
    }
  }

  /**
   * Gets a search entity by it's id, using the current organizational context.
   *
   * @param id
   *          the media package identifier
   * @return the search entity, or null if not found
   */
  private Function<EntityManager, Optional<SearchEntity>> getSearchEntityQuery(String id) {
    return namedQuery.findOpt(
        "Search.findById",
        SearchEntity.class,
        Pair.of("mediaPackageId", id)
    );
  }
}
