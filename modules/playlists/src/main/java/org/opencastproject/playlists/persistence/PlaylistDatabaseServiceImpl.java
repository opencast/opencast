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
package org.opencastproject.playlists.persistence;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Component(
    immediate = true,
    service = { PlaylistDatabaseService.class },
    property = {
        "service.description=Playlist Database Service"
    }
)
public class PlaylistDatabaseServiceImpl implements PlaylistDatabaseService {

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.playlists";
  private static final Logger logger = LoggerFactory.getLogger(PlaylistDatabaseServiceImpl.class);
  /** Factory used to create {@link EntityManager}s for transactions */
  private EntityManagerFactory emf;

  private DBSessionFactory dbSessionFactory;
  private DBSession db;

  /** The security service */
  protected SecurityService securityService;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.playlists)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  @Reference(name = "security-service")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for playlists");
    db = dbSessionFactory.createSession(emf);
  }

  /**
   * {@inheritDoc}
   * @see PlaylistDatabaseService#getPlaylist(String)
   */
  @Override
  public Playlist getPlaylist(String playlistId) throws NotFoundException, PlaylistDatabaseException {
    return getPlaylist(playlistId, securityService.getOrganization().getId());
  }

  /**
   * {@inheritDoc}
   * @see PlaylistDatabaseService#getPlaylist(String, String)
   */
  @Override
  public Playlist getPlaylist(String playlistId, String orgId) throws NotFoundException, PlaylistDatabaseException {
    try {
      return db.execTxChecked(em -> {
        Optional<Playlist> playlist = getPlaylistById(playlistId, orgId).apply(em);
        if (playlist.isEmpty()) {
          throw new NotFoundException("No playlist with id=" + playlistId + " exists");
        }
        return playlist.get();
      });
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retrieve playlist with ID '{}'", playlistId, e);
      throw new PlaylistDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see PlaylistDatabaseService#getPlaylists(int, int, SortCriterion)
   */
  @Override
  public List<Playlist> getPlaylists(int limit, int offset, SortCriterion sortCriterion)
          throws PlaylistDatabaseException {

    try {
      return db.exec(em -> {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Playlist> criteriaQuery = criteriaBuilder.createQuery(Playlist.class);
        Root<Playlist> from = criteriaQuery.from(Playlist.class);
        CriteriaQuery<Playlist> select = criteriaQuery.select(from)
            .where(criteriaBuilder.isNull(from.get("deletionDate")));

        if (sortCriterion.getOrder().equals(SortCriterion.Order.Ascending)) {
          criteriaQuery.orderBy(criteriaBuilder.asc(from.get(sortCriterion.getFieldName())));
        } else if (sortCriterion.getOrder().equals(SortCriterion.Order.Descending)) {
          criteriaQuery.orderBy(criteriaBuilder.desc(from.get(sortCriterion.getFieldName())));
        }

        TypedQuery<Playlist> allQuery = em.createQuery(select);

        allQuery.setMaxResults(limit);
        allQuery.setFirstResult(offset);

        return allQuery.getResultList();
      });
    } catch (Exception e) {
      throw new PlaylistDatabaseException("Error fetching playlists from database", e);
    }
  }

  public List<Playlist> getAllForAdministrativeRead(Date startDate, Date endDate, int limit)
          throws PlaylistDatabaseException {
    try {
      return db.exec(em -> {
        final var criteriaBuilder = em.getCriteriaBuilder();
        final var criteriaQuery = criteriaBuilder.createQuery(Playlist.class);
        final var from = criteriaQuery.from(Playlist.class);
        final var select = criteriaQuery.select(from)
            .where(
                criteriaBuilder.greaterThanOrEqualTo(from.get("updated"), startDate),
                criteriaBuilder.lessThan(from.get("updated"), endDate)
            )
            .orderBy(criteriaBuilder.asc(from.get("updated")));

        return em.createQuery(select)
            .setMaxResults(limit)
            .getResultList();
      });
    } catch (Exception e) {
      throw new PlaylistDatabaseException("Error fetching playlists from database", e);
    }
  }

  /**
   * {@inheritDoc}
   * @see PlaylistDatabaseService#updatePlaylist(Playlist, String)
   */
  @Override
  public Playlist updatePlaylist(Playlist playlist, String orgId) throws PlaylistDatabaseException {
    try {
      return db.execTx(em -> {
        Optional<Playlist> fromDb = getPlaylistById(playlist.getId(), orgId).apply(em);
        playlist.setUpdated(new Date());
        if (fromDb.isEmpty()) {
          em.persist(playlist);
        } else {
          return em.merge(playlist);
        }

        return playlist;
      });
    } catch (Exception e) {
      throw new PlaylistDatabaseException("Could not update playlist with ID '" + playlist.getId() + "'", e);
    }
  }

  /**
   * {@inheritDoc}
   * @see PlaylistDatabaseService#deletePlaylist(Playlist, String)
   */
  @Override
  public Playlist deletePlaylist(Playlist playlist, String orgId) throws PlaylistDatabaseException {
    try {
      return db.execTx(em -> {
        Optional<Playlist> fromDb = getPlaylistById(playlist.getId(), orgId).apply(em);
        if (fromDb.isPresent()) {
          Date now = new Date();
          playlist.setUpdated(now);
          playlist.setDeletionDate(now);
          em.merge(playlist);
        }
        logger.debug("Playlist with id {} was deleted.", playlist.getId());
        return playlist;
      });
    } catch (Exception e) {
      throw new PlaylistDatabaseException("Could not delete playlist with ID '" + playlist.getId() + "'", e);
    }
  }

  /**
   * Gets a non-deleted playlist by its id
   *
   * @param playlistId the playlist identifier
   * @param orgId the organisation identifier
   * @return the playlist in an optional
   */
  protected Function<EntityManager, Optional<Playlist>> getPlaylistById(String playlistId, String orgId) {
    return em -> getPotentiallyDeletedPlaylist(playlistId, orgId).apply(em).filter(e -> !e.isDeleted());
  }

  /**
   * Gets a potentially deleted series by its ID, using the current organizational context.
   *
   * @param playlistId the playlist identifier
   * @param orgId the organisation identifier
   * @return the playlist in an optional
   */
  protected Function<EntityManager, Optional<Playlist>> getPotentiallyDeletedPlaylist(String playlistId, String orgId) {
    return namedQuery.findOpt(
        "Playlist.findById",
        Playlist.class,
        Pair.of("id", playlistId),
        Pair.of("organizationId", orgId)
    );
  }
}
