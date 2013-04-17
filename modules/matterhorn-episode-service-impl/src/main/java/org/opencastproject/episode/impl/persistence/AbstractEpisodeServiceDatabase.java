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

import org.opencastproject.episode.api.Version;
import org.opencastproject.episode.impl.PartialMediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.Iterator;

import static org.opencastproject.episode.api.Version.FIRST;
import static org.opencastproject.episode.api.Version.version;
import static org.opencastproject.episode.impl.StoragePath.spath;
import static org.opencastproject.episode.impl.persistence.EpisodeDto.findAll;
import static org.opencastproject.episode.impl.persistence.EpisodeDto.findAllById;
import static org.opencastproject.episode.impl.persistence.EpisodeDto.findByIdAndVersion;
import static org.opencastproject.episode.impl.persistence.EpisodeDto.findLatestById;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.functions.Misc.chuck;

/** Implements {@link EpisodeServiceDatabase}. */
public abstract class AbstractEpisodeServiceDatabase implements EpisodeServiceDatabase {
  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(OsgiEpisodeServiceDatabase.class);

  protected abstract PersistenceEnv getPenv();

  protected abstract SecurityService getSecurityService();

  @Override
  public boolean deleteEpisode(final String mediaPackageId, final Date deletionDate) throws EpisodeServiceDatabaseException {
    return tx(new Function<EntityManager, Boolean>() {
      @Override public Boolean apply(EntityManager em) {
        AssetDto.deleteByMediaPackageId(em, mediaPackageId);
        for (EpisodeDto episodeDto : findAllById(em, mediaPackageId)) {
          episodeDto.setDeletionDate(deletionDate);
          em.merge(episodeDto);
          return true;
        }
        return false;
      }
    });
  }

  @Override
  public Option<Episode> getEpisode(final String mediaPackageId, final Version version) throws EpisodeServiceDatabaseException {
    return tx(new Function<EntityManager, Option<Episode>>() {
      @Override
      public Option<Episode> apply(EntityManager em) {
        return findByIdAndVersion(em, mediaPackageId, version).map(EpisodeDto.toEpisode);
      }
    });
  }

  @Override
  public Option<Episode> getLatestEpisode(final String mediaPackageId) throws EpisodeServiceDatabaseException {
    return tx(new Function<EntityManager, Option<Episode>>() {
      @Override
      public Option<Episode> apply(EntityManager em) {
        return findLatestById(em, mediaPackageId).map(EpisodeDto.toEpisode);
      }
    });
  }

  @Override
  public Iterator<Episode> getAllEpisodes() throws EpisodeServiceDatabaseException {
    // todo implement database paging
    return tx(new Function<EntityManager, Iterator<Episode>>() {
      @Override
      public Iterator<Episode> apply(EntityManager em) {
        return mlist(findAll(em)).map(EpisodeDto.toEpisode).iterator();
      }
    });
  }

  @Override
  public Version claimVersion(final String mpId) throws EpisodeServiceDatabaseException {
    return tx(new Function<EntityManager, Version>() {
      @Override
      public Version apply(final EntityManager em) {
        return VersionClaimDto.findLast(em, mpId).fold(new Option.Match<VersionClaimDto, Version>() {
          @Override
          public Version some(VersionClaimDto dto) {
            final Version claimed = version(dto.getLastClaimed().value() + 1);
            VersionClaimDto.update(em, mpId, claimed);
            return claimed;
          }

          @Override
          public Version none() {
            em.persist(VersionClaimDto.create(mpId, FIRST));
            return FIRST;
          }
        });
      }
    });
  }

  @Override
  public Option<Boolean> isLatestVersion(final String mediaPackageId, final Version version) throws EpisodeServiceDatabaseException {
    return tx(new Function<EntityManager, Option<Boolean>>() {
      @Override
      public Option<Boolean> apply(EntityManager em) {
        return findLatestById(em, mediaPackageId).map(new Function<EpisodeDto, Boolean>() {
          @Override
          public Boolean apply(EpisodeDto dto) {
            return dto.getVersion().equals(version);
          }
        });
      }
    });
  }

  @Override
  public Option<Date> getDeletionDate(final String mediaPackageId) throws EpisodeServiceDatabaseException {
    return tx(new Function<EntityManager, Option<Date>>() {
      @Override
      public Option<Date> apply(EntityManager em) {
        return findLatestById(em, mediaPackageId).fold(new Option.Match<EpisodeDto, Option<Date>>() {
          @Override
          public Option<Date> some(EpisodeDto episodeDto) {
            return episodeDto.getDeletionDate();
          }

          @Override
          public Option<Date> none() {
            return chuck(new NotFoundException("No episode with id=" + mediaPackageId + " exists"));
          }
        });
      }
    });
  }

 @Override
  public void storeEpisode(final PartialMediaPackage pmp, final AccessControlList acl, final Date now, final Version version)
          throws EpisodeServiceDatabaseException {
    final String orgId = getSecurityService().getOrganization().getId();
    tx(new Effect<EntityManager>() {
      @Override
      public void run(EntityManager em) {
        // Create new episode entity
        final EpisodeDto episodeDto = EpisodeDto.create(new Episode(pmp.getMediaPackage(),
                                                                    version,
                                                                    orgId,
                                                                    acl,
                                                                    now,
                                                                    Option.<Date>none()));
        em.persist(episodeDto);
        // create assets
        final String mpId = pmp.getMediaPackage().getIdentifier().toString();
        for (MediaPackageElement e : pmp.getPartial()) {
          final AssetDto a = AssetDto.create(e.getURI(),
                                             spath(orgId, mpId, version, e.getIdentifier()),
                                             e.getChecksum().toString());
          em.persist(a);
        }
      }
    });
  }

  @Override
  public Option<Asset> findAssetByChecksum(String checksum) throws EpisodeServiceDatabaseException {
    return tx(AssetDto.findOneByChecksum(checksum)).map(AssetDto.toAsset);
  }

  /** Run transactional with exception handling. */
  private <A> A tx(Function<EntityManager, A> f) {
    try {
      return getPenv().tx(f);
    } catch (Exception e) {
      logger.error("Error accessing episode service database", e);
      return chuck(new EpisodeServiceDatabaseException(e));
    }
  }
}
