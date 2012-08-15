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
package org.opencastproject.episode.impl.persistence;

import static org.opencastproject.episode.api.EpisodeService.CONTRIBUTE_PERMISSION;
import static org.opencastproject.episode.api.EpisodeService.READ_PERMISSION;
import static org.opencastproject.episode.api.EpisodeService.WRITE_PERMISSION;
import static org.opencastproject.episode.api.Version.version;
import static org.opencastproject.episode.impl.StoragePath.spath;
import static org.opencastproject.episode.impl.persistence.EpisodeDto.findByIdAndVersion;
import static org.opencastproject.util.data.Collections.mkString;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Tuple3.tuple3;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.episode.api.Version;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple3;
import org.opencastproject.util.data.functions.Booleans;
import org.opencastproject.util.persistence.PersistenceEnv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Implements {@link EpisodeServiceDatabase}. Defines permanent storage for series.
 */
public abstract class AbstractEpisodeServiceDatabase implements EpisodeServiceDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(OsgiEpisodeServiceDatabase.class);

  protected abstract PersistenceEnv getPenv();

  protected abstract SecurityService getSecurityService();

  @Override
  public void deleteEpisode(final String mediaPackageId, final Date deletionDate) throws NotFoundException,
          EpisodeServiceDatabaseException {
    tx(new Effect.X<EntityManager>() {
      @Override protected void xrun(EntityManager em) throws Exception {
        List<EpisodeDto> episodesDto = getEpisodesEntity(mediaPackageId, em);
        if (episodesDto.isEmpty())
          throw new NotFoundException("No episodes with id=" + mediaPackageId + " exists");
        for (EpisodeDto episodeDto : episodesDto) {
          protect(episodeDto, mediaPackageId, WRITE_PERMISSION);
          episodeDto.setDeletionDate(deletionDate);
          em.merge(episodeDto);
        }
      }
    });
  }

  @Override
  public AccessControlList getAccessControlList(final String mediaPackageId, final Version version) throws NotFoundException,
          EpisodeServiceDatabaseException {
    return tx(new Function.X<EntityManager, AccessControlList>() {
      @Override protected AccessControlList xapply(EntityManager em) throws Exception {
        return findByIdAndVersion(em, mediaPackageId, version).fold(new Option.Match<EpisodeDto, AccessControlList>() {
          @Override public AccessControlList some(EpisodeDto dto) {
            try {
              return dto.getAccessControl() != null ? AccessControlParser.parseAcl(dto.getAccessControl()) : null;
            } catch (Exception e) {
              return chuck(new EpisodeServiceDatabaseException(e));
            }
          }

          @Override public AccessControlList none() {
            return chuck(new NotFoundException("No episode with id=" + mediaPackageId + " exists"));
          }
        });
      }
    });
  }

  @Override
  public Option<EpisodeDto> getEpisode(final String mediaPackageId, final Version version) throws EpisodeServiceDatabaseException {
    return tx(new Function.X<EntityManager, Option<EpisodeDto>>() {
      @Override protected Option<EpisodeDto> xapply(EntityManager em) throws Exception {
        return findByIdAndVersion(em, mediaPackageId, version).map(new Function<EpisodeDto, EpisodeDto>() {
          @Override public EpisodeDto apply(EpisodeDto episodeDto) {
            return protect(episodeDto, mediaPackageId, READ_PERMISSION, CONTRIBUTE_PERMISSION, WRITE_PERMISSION);
          }
        });
      }
    });
  }

  @Override
  public Iterator<Tuple3<MediaPackage, Version, String>> getAllEpisodes() throws EpisodeServiceDatabaseException {
    // todo implement database paging
    return tx(new Function<EntityManager, Iterator<Tuple3<MediaPackage, Version, String>>>() {
      @Override public Iterator<Tuple3<MediaPackage, Version, String>> apply(EntityManager em) {
        Query query = em.createNamedQuery("Episode.findAll");
        final List<EpisodeDto> episodeDtos = (List<EpisodeDto>) query.getResultList();
        return mlist(episodeDtos).map(new Function.X<EpisodeDto, Tuple3<MediaPackage, Version, String>>() {
          @Override public Tuple3<MediaPackage, Version, String> xapply(EpisodeDto dto) throws Exception {
            final MediaPackage mp = MediaPackageParser.getFromXml(dto.getMediaPackageXML());
            return tuple3(mp, version(dto.getVersion()), dto.getOrganization());
          }
        }).iterator();
      }
    });
  }

  @Override public Version claimVersion(final String mpId) throws EpisodeServiceDatabaseException {
    return tx(new Function<EntityManager, Version>() {
      @Override public Version apply(EntityManager em) {
        long newVersion = VersionDto.findLast(em, mpId).map(new Function<VersionDto, Long>() {
          @Override public Long apply(VersionDto dto) {
            return dto.getLastVersion() + 1;
          }
        }).getOrElse(0L);
        em.merge(VersionDto.create(mpId, newVersion));
        return version(newVersion);
      }
    });
  }

  @Override
  public boolean isLatestVersion(final String mediaPackageId, final Version version) throws NotFoundException,
          EpisodeServiceDatabaseException {
    return tx(new Function.X<EntityManager, Boolean>() {
      @Override public Boolean xapply(EntityManager em) throws Exception {
        return findByIdAndVersion(em, mediaPackageId, version).fold(new Option.Match<EpisodeDto, Boolean>() {
          @Override public Boolean some(EpisodeDto dto) {
            return protect(dto, mediaPackageId, READ_PERMISSION).isLatestVersion();
          }

          @Override public Boolean none() {
            return chuck(new NotFoundException("No episode with id=" + mediaPackageId + " exists"));
          }
        });
      }
    });
  }

  /** Return <code>dto</code> if access is allowed, else throw an UnauthorizedException. */
  public EpisodeDto protect(EpisodeDto dto, String mpId, String... actions) {
    final String accessControlXml = dto.getAccessControl();
    if (accessControlXml != null) {
      final AccessControlList acl = new Function0.X<AccessControlList>() {
        @Override public AccessControlList xapply() throws Exception {
          return AccessControlParser.parseAcl(accessControlXml);
        }
      } .apply();
      final User currentUser = getSecurityService().getUser();
      final Organization currentOrg = getSecurityService().getOrganization();
      final boolean authorized = mlist(actions).map(new Function<String, Boolean>() {
        @Override public Boolean apply(String action) {
          return AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, action);
        }
      }).foldl(false, Booleans.or);
      if (!authorized)
        return chuck(new UnauthorizedException(currentUser + " is not authorized to do a " + mkString(actions, ",") + " action on episode " + mpId));
    }
    return dto;
  }

  @Override
  public boolean getLockState(final String mediaPackageId) throws NotFoundException, EpisodeServiceDatabaseException {
    return tx(new Function.X<EntityManager, Boolean>() {
      @Override public Boolean xapply(EntityManager em) throws Exception {
        EpisodeDto episodeDto = getLatestEpisodeEntity(mediaPackageId, em);
        if (episodeDto == null) {
          throw new NotFoundException("No episode with id=" + mediaPackageId + " exists");
        }
        // Ensure this user is allowed to read this episode
        String accessControlXml = episodeDto.getAccessControl();
        if (accessControlXml != null) {
          AccessControlList acl = AccessControlParser.parseAcl(accessControlXml);
          User currentUser = getSecurityService().getUser();
          Organization currentOrg = getSecurityService().getOrganization();
          if (!AccessControlUtil.isAuthorized(acl, currentUser, currentOrg, READ_PERMISSION))
            throw new UnauthorizedException(currentUser + " is not authorized to read episode " + mediaPackageId);
        }
        return episodeDto.isLocked();
      }
    });
  }

  @Override
  public Date getModificationDate(final String mediaPackageId, final Version version) throws NotFoundException,
          EpisodeServiceDatabaseException {
    return tx(new Function.X<EntityManager, Date>() {
      @Override public Date xapply(EntityManager em) throws Exception {
        return findByIdAndVersion(em, mediaPackageId, version).fold(new Option.Match<EpisodeDto, Date>() {
          @Override public Date some(EpisodeDto dto) {
            return protect(dto, mediaPackageId, READ_PERMISSION).getModificationDate();
          }

          @Override public Date none() {
            return chuck(new NotFoundException("No episode with id=" + mediaPackageId + " exists"));
          }
        });
      }
    });
  }

  @Override
  public String getOrganizationId(final String mediaPackageId, final Version version) throws NotFoundException,
          EpisodeServiceDatabaseException {
    return tx(new Function.X<EntityManager, String>() {
      @Override public String xapply(EntityManager em) throws Exception {
        return findByIdAndVersion(em, mediaPackageId, version).fold(new Option.Match<EpisodeDto, String>() {
          @Override public String some(EpisodeDto dto) {
            return protect(dto, mediaPackageId, READ_PERMISSION).getOrganization();
          }

          @Override public String none() {
            return chuck(new NotFoundException("No episode with id=" + mediaPackageId + " exists"));
          }
        });
      }
    });
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.episode.impl.persistence.EpisodeServiceDatabase#getDeletionDate(String)
   */
  @Override
  public Date getDeletionDate(final String mediaPackageId) throws NotFoundException, EpisodeServiceDatabaseException {
    return tx(new Function.X<EntityManager, Date>() {
      @Override public Date xapply(EntityManager em) throws Exception {
        EpisodeDto episodeDto = getLatestEpisodeEntity(mediaPackageId, em);
        if (episodeDto == null) {
          throw new NotFoundException("No episode with id=" + mediaPackageId + " exists");
        }
        return protect(episodeDto, mediaPackageId, READ_PERMISSION).getDeletionDate();
      }
    });
  }

  @Override
  public void lockEpisode(final String mediaPackageId, final boolean lock) throws NotFoundException,
          EpisodeServiceDatabaseException {
    tx(new Effect.X<EntityManager>() {
      @Override protected void xrun(EntityManager em) throws Exception {
        List<EpisodeDto> episodes = getEpisodesEntity(mediaPackageId, em);
        if (episodes.isEmpty())
          throw new NotFoundException("No episodes with id=" + mediaPackageId + " exists");
        for (EpisodeDto episode : episodes) {
          episode.setLocked(lock);
          em.merge(episode);
        }
      }
    });
  }

  @Override
  public void storeEpisode(final MediaPackage mediaPackage, final AccessControlList acl, final Date now, final Version version)
          throws EpisodeServiceDatabaseException {
    final String mediaPackageXML = MediaPackageParser.getAsXml(mediaPackage);
    final String mediaPackageId = mediaPackage.getIdentifier().toString();
    final String orgId = getSecurityService().getOrganization().getId();

    tx(new Effect.X<EntityManager>() {
      @Override protected void xrun(EntityManager em) throws Exception {
        final EpisodeDto latestDto = getLatestEpisodeEntity(mediaPackageId, em);
        if (latestDto != null) {
          latestDto.setLatestVersion(false);
          em.merge(latestDto);
        }

        // Create new episode entity
        final EpisodeDto episodeDto = new EpisodeDto();
        episodeDto.setOrganization(orgId);
        episodeDto.setMediaPackageId(mediaPackageId);
        episodeDto.setMediaPackageXML(mediaPackageXML);
        episodeDto.setAccessControl(AccessControlParser.toXml(acl));
        episodeDto.setVersion(version.value());
        episodeDto.setLocked(true);
        episodeDto.setModificationDate(now);
        episodeDto.setLatestVersion(true);
        em.persist(episodeDto);

        // create assets
        for (MediaPackageElement e : mediaPackage.getElements()) {
          final AssetDto a = AssetDto.create(e.getURI(), spath(orgId, mediaPackageId, version, e.getIdentifier()), e.getChecksum().toString());
          em.persist(a);
        }
      }
    });
  }

  @Override
  public Option<Asset> findAssetByElementIdAndChecksum(String mediaPackageElementId, String checksum)
          throws EpisodeServiceDatabaseException {
    return tx(AssetDto.findByElementIdAndChecksum(mediaPackageElementId, checksum)).map(AssetDto.toAsset);
  }

  /**
   * Gets the episode with the latest version by its media package id, using the current organizational context.
   * 
   * @param mediaPackageId
   *          the episode media package id
   * @param em
   *          an open entity manager
   * @return the episode entity
   * @throws NotFoundException
   *           if the episode not exists
   */
  private EpisodeDto getLatestEpisodeEntity(String mediaPackageId, EntityManager em) {
    Query q = em.createNamedQuery("Episode.findLatestById").setParameter("mediaPackageId", mediaPackageId);
    try {
      return (EpisodeDto) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Gets all episodes by its media package id, using the current organizational context.
   * 
   * @param mediaPackageId
   *          the episode media package id
   * @param em
   *          an open entity manager
   * @return the episode entity
   * @throws NotFoundException
   *           if the episode not exists
   */
  @SuppressWarnings("unchecked")
  private List<EpisodeDto> getEpisodesEntity(String mediaPackageId, EntityManager em) {
    Query q = em.createNamedQuery("Episode.findAllById").setParameter("mediaPackageId", mediaPackageId);
    try {
      return q.getResultList();
    } catch (NoResultException e) {
      return Collections.EMPTY_LIST;
    }
  }

  /** Run transactional with exception handling. */
  private <A> A tx(Function<EntityManager, A> f) {
    try {
      return getPenv().tx(f);
    } catch (Exception e) {
      if (e instanceof NotFoundException) {
        logger.error("Error accessing episode service database", e);
        return chuck(e);
      } else {
        logger.error("Error accessing episode service database", e);
        return chuck(new EpisodeServiceDatabaseException(e));
      }
    }
  }

}
