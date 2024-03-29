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
package org.opencastproject.transcription.persistence;

import static org.opencastproject.db.Queries.namedQuery;

import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAttribute;

@Entity(name = "TranscriptionJobControl")
@Table(name = "oc_transcription_service_job")
@NamedQueries({
    @NamedQuery(
        name = "TranscriptionJobControl.findByMediaPackage",
        query = "SELECT jc FROM TranscriptionJobControl jc "
            + "WHERE jc.mediaPackageId = :mediaPackageId ORDER BY jc.dateCreated DESC"
    ),
    @NamedQuery(
        name = "TranscriptionJobControl.findByMediaPackageTrackAndStatus",
        query = "SELECT jc FROM TranscriptionJobControl jc "
            + "WHERE jc.mediaPackageId = :mediaPackageId AND jc.trackId = :trackId AND jc.status IN :status"
    ),
    @NamedQuery(
        name = "TranscriptionJobControl.findByJob",
        query = "SELECT jc FROM TranscriptionJobControl jc "
            + "WHERE jc.transcriptionJobId = :transcriptionJobId"
    ),
    @NamedQuery(
        name = "TranscriptionJobControl.findByStatus",
        query = "SELECT jc FROM TranscriptionJobControl jc "
            + "WHERE jc.status IN :status"
    ),
})
public class TranscriptionJobControlDto implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", length = 128)
  @XmlAttribute
  private long id;

  @Column(name = "mediapackage_id", nullable = false, length = 128)
  private String mediaPackageId;

  @Column(name = "track_id", nullable = false, length = 128)
  private String trackId;

  @Column(name = "job_id", nullable = false, length = 128)
  private String transcriptionJobId;

  @Column(name = "date_created", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCreated;

  @Column(name = "date_expected")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateExpected;

  @Column(name = "date_completed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCompleted;

  @Column(name = "status", length = 128)
  private String status;

  @Column(name = "track_duration", nullable = false)
  private long trackDuration;

  @Column(name = "provider_id", nullable = false)
  private long providerId;

  public TranscriptionJobControlDto() {
  }

  /**
   * Constructor with all fields.
   */
  public TranscriptionJobControlDto(String mediaPackageId, String trackId, String transcriptionJobId, Date dateCreated,
          Date dateExpected, Date dateCompleted, String status, long trackDuration, long providerId) {
    super();
    this.mediaPackageId = mediaPackageId;
    this.trackId = trackId;
    this.transcriptionJobId = transcriptionJobId;
    this.dateCreated = dateCreated;
    this.dateExpected = dateExpected;
    this.dateCompleted = dateCompleted;
    this.status = status;
    this.trackDuration = trackDuration;
    this.providerId = providerId;
  }

  /**
   * Convert into business object.
   */
  public TranscriptionJobControl toTranscriptionJobControl() {
    return new TranscriptionJobControl(
        mediaPackageId, trackId, transcriptionJobId, dateCreated, dateExpected, dateCompleted, status,
        trackDuration, providerId);
  }

  /**
   * Store new job control
   */
  public static Function<EntityManager, TranscriptionJobControlDto> storeQuery(
      String mediaPackageId,
      String trackId,
      String transcriptionJobId,
      String jobStatus,
      long trackDuration,
      Date dateExpected,
      long providerId) {
    return em -> {
      TranscriptionJobControlDto dto = new TranscriptionJobControlDto(mediaPackageId, trackId, transcriptionJobId,
          new Date(), dateExpected, null, jobStatus, trackDuration, providerId);
      em.persist(dto);
      return dto;
    };
  }

  /**
   * Find a job control by its number.
   */
  public static Function<EntityManager, Optional<TranscriptionJobControlDto>> findByJobQuery(String jobId) {
    return namedQuery.findOpt(
        "TranscriptionJobControl.findByJob",
        TranscriptionJobControlDto.class,
        Pair.of("transcriptionJobId", jobId)
    );
  }

  /**
   * Find all job controls by media package.
   */
  public static Function<EntityManager, List<TranscriptionJobControlDto>> findByMediaPackageQuery(
      final String mediaPackageId) {
    return namedQuery.findAll(
        "TranscriptionJobControl.findByMediaPackage",
        TranscriptionJobControlDto.class,
        Pair.of("mediaPackageId", mediaPackageId)
    );
  }

  /**
   * Find all job controls by status.
   */
  public static Function<EntityManager, List<TranscriptionJobControlDto>> findByStatusQuery(final String... status) {
    if (status == null) {
      throw new IllegalArgumentException("status is null");
    }
    Collection<String> statusCol = new HashSet<>(Arrays.asList(status));
    return namedQuery.findAll(
        "TranscriptionJobControl.findByStatus",
        TranscriptionJobControlDto.class,
        Pair.of("status", statusCol)
    );
  }

  /**
   * Find all job controls by media package and status.
   */
  public static Function<EntityManager, List<TranscriptionJobControlDto>> findByMediaPackageTrackAndStatusQuery(
      final String mediaPackageId, String trackId, final String... status) {
    Collection<String> statusCol = new HashSet<>(Arrays.asList(status));
    return namedQuery.findAll(
        "TranscriptionJobControl.findByMediaPackageTrackAndStatus",
        TranscriptionJobControlDto.class,
        Pair.of("mediaPackageId", mediaPackageId),
        Pair.of("trackId", trackId),
        Pair.of("status", statusCol)
    );
  }

  /**
   * Update job status
   */
  public static Consumer<EntityManager> updateStatusQuery(String jobId, String status) {
    return em -> {
      TranscriptionJobControlDto dto = findByJobQuery(jobId).apply(em)
          .orElseThrow(NoResultException::new);
      dto.setStatus(status);
      if (TranscriptionJobControl.Status.TranscriptionComplete.name().equals(status)) {
        dto.setDateCompleted(new Date());
      }
      em.merge(dto);
    };
  }

  public static Consumer<EntityManager> delete(String jobId) {
    return em -> findByJobQuery(jobId)
        .apply(em)
        .ifPresent(em::remove);
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }
}
