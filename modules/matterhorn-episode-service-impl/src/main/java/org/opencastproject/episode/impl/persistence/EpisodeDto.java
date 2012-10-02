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
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.PersistenceUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.data.functions.Functions.chuck;
import static org.opencastproject.util.persistence.PersistenceUtil.runSingleResultQuery;

/** JPA link to {@link Episode}. */
@Entity(name = "Episode")
@Table(name = "episode_episode")
@NamedQueries({
                      @NamedQuery(name = "Episode.findAll", query = "SELECT e FROM Episode e"),
                      @NamedQuery(name = "Episode.findByIdAndVersion", query = "SELECT e FROM Episode e WHERE e.mediaPackageId=:mediaPackageId AND e.version=:version"),
                      @NamedQuery(name = "Episode.findLatestById", query =
                              "SELECT e FROM Episode e WHERE e.mediaPackageId = :mediaPackageId "
                                      + "AND e.version = (SELECT MAX(e2.version) FROM Episode e2 WHERE e2.mediaPackageId = :mediaPackageId)"),
                      @NamedQuery(name = "Episode.findLatestVersion", query =
                              "SELECT MAX(a.version) FROM Episode a WHERE a.mediaPackageId = :mediaPackageId "),
                      @NamedQuery(name = "Episode.findAllById", query = "SELECT e FROM Episode e WHERE e.mediaPackageId=:mediaPackageId") })
public final class EpisodeDto {
  @Id
  @Column(name = "mediapackage_id", length = 128)
  private String mediaPackageId;

  @Id
  @Column(name = "version")
  private long version;

  @Column(name = "organization_id", length = 128, nullable = false)
  private String organization;

  @Column(name = "deletion_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date deletionDate;

  @Column(name = "modification_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date modificationDate;

  @Lob
  @Column(name = "access_control", length = 65535, nullable = false)
  private String accessControl;

  @Lob
  @Column(name = "mediapackage", length = 65535, nullable = false)
  private String mediaPackageXml;

  public static EpisodeDto create(Episode episode) {
    try {
      final EpisodeDto dto = new EpisodeDto();
      dto.mediaPackageId = episode.getMediaPackage().getIdentifier().toString();
      dto.version = episode.getVersion().value();
      dto.organization = episode.getOrganization();
      for (Date a : episode.getDeletionDate()) dto.deletionDate = a;
      dto.modificationDate = episode.getModificationDate();
      dto.accessControl = AccessControlParser.toXml(episode.getAcl());
      dto.mediaPackageXml = MediaPackageParser.getAsXml(episode.getMediaPackage());
      return dto;
    } catch (Exception e) {
      return chuck(e);
    }
  }

  public Episode toEpisode() {
    try {
      return new Episode(MediaPackageParser.getFromXml(mediaPackageXml),
                         getVersion(),
                         organization,
                         getAcl(),
                         modificationDate,
                         getDeletionDate());
    } catch (Exception e) {
      return chuck(e);
    }
  }

  public static final Function<EpisodeDto, Episode> toEpisode = new Function<EpisodeDto, Episode>() {
    @Override public Episode apply(EpisodeDto dto) {
      return dto.toEpisode();
    }
  };

  // some shortcut accessors for those cases where a complete conversion into an Episode is a waste

  public Version getVersion() {
    return Version.version(version);
  }

  public AccessControlList getAcl() {
    try {
      return AccessControlParser.parseAcl(accessControl);
    } catch (Exception e) {
      return chuck(e);
    }
  }

  public Option<Date> getDeletionDate() {
    return option(deletionDate);
  }

  // not sure about this.
  public void setDeletionDate(Date d) {
    deletionDate = d;
  }

  // finders

  /** Find an episode by media package id and version. */
  public static Option<EpisodeDto> findByIdAndVersion(EntityManager em, String mediaPackageId, Version version) {
    return runSingleResultQuery(em, "Episode.findByIdAndVersion",
                                tuple("version", version.value()),
                                tuple("mediaPackageId", mediaPackageId));
  }

  /** Find the latest version identifier of a media package. */
  public static Option<Version> findLatestVersion(EntityManager em, String mediaPackageId) {
    for (Long version : PersistenceUtil.<Long>runSingleResultQuery(em, "Episode.findLatestVersion",
                                                                   tuple("mediaPackageId", mediaPackageId))) {
      return some(Version.version(version));
    }
    return none();
  }

  public static Option<EpisodeDto> findLatestById(EntityManager em, String mediaPackageId) {
    return runSingleResultQuery(em, "Episode.findLatestById", tuple("mediaPackageId", mediaPackageId));
  }

  public static List<EpisodeDto> findAllById(EntityManager em, String mediaPackageId) {
    return PersistenceUtil.findAll(em, "Episode.findAllById", tuple("mediaPackageId", mediaPackageId));
  }

  public static List<EpisodeDto> findAll(EntityManager em) {
    return PersistenceUtil.findAll(em, "Episode.findAll");
  }
}
