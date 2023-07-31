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

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;

@Component(
    immediate = true,
    name = "org.opencastproject.transcription.persistence.TranscriptionDatabase",
    service = TranscriptionDatabase.class,
    property = {
        "service.description=Transcription Persistence"
    }
)
public class TranscriptionDatabaseImpl implements TranscriptionDatabase {

  /**
   * Logging utilities
   */
  private static final Logger logger = LoggerFactory.getLogger(TranscriptionDatabaseImpl.class);

  /**
   * Factory used to create entity managers for transactions
   */
  protected EntityManagerFactory emf;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  private final long noProviderId = -1;

  /**
   * OSGi callback.
   */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for transcription service");
    db = dbSessionFactory.createSession(emf);
  }

  @Reference(target = "(osgi.unit.name=org.opencastproject.transcription.persistence)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  public TranscriptionJobControl storeJobControl(String mpId, String trackId, String jobId, String jobStatus,
          long trackDuration, Date dateExpected, String provider) throws TranscriptionDatabaseException {
    long providerId = getProviderId(provider);
    if (providerId != noProviderId) {
      try {
        return db.execTx(TranscriptionJobControlDto.storeQuery(mpId, trackId, jobId, jobStatus, trackDuration,
                dateExpected, providerId))
            .toTranscriptionJobControl();
      } catch (Exception e) {
        throw new TranscriptionDatabaseException(e);
      }
    }
    return null;
  }

  @Override
  public TranscriptionProviderControl storeProviderControl(String provider) throws TranscriptionDatabaseException {
    try {
      TranscriptionProviderControlDto dto = db.execTx(TranscriptionProviderControlDto.storeProviderQuery(provider));
      logger.info("Transcription provider '{}' stored", provider);
      return dto.toTranscriptionProviderControl();
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

  @Override
  public void deleteJobControl(String jobId) throws TranscriptionDatabaseException {
    try {
      db.execTx(TranscriptionJobControlDto.delete(jobId));
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

  @Override
  public void updateJobControl(String jobId, String jobStatus) throws TranscriptionDatabaseException {
    try {
      db.execTx(TranscriptionJobControlDto.updateStatusQuery(jobId, jobStatus));
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

  @Override
  public TranscriptionJobControl findByJob(String jobId) throws TranscriptionDatabaseException {
    try {
      return db.exec(TranscriptionJobControlDto.findByJobQuery(jobId))
          .map(TranscriptionJobControlDto::toTranscriptionJobControl)
          .orElse(null);
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

  @Override
  public List<TranscriptionJobControl> findByMediaPackage(String mpId) throws TranscriptionDatabaseException {
    try {
      return db.exec(TranscriptionJobControlDto.findByMediaPackageQuery(mpId)).stream()
          .map(TranscriptionJobControlDto::toTranscriptionJobControl)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

  @Override
  public List<TranscriptionJobControl> findByStatus(String... status) throws TranscriptionDatabaseException {
    try {
      return db.exec(TranscriptionJobControlDto.findByStatusQuery(status)).stream()
          .map(TranscriptionJobControlDto::toTranscriptionJobControl)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

  @Override
  public List<TranscriptionJobControl> findByMediaPackageTrackAndStatus(String mpId, String trackId, String... status)
          throws TranscriptionDatabaseException {
    try {
      return db.exec(TranscriptionJobControlDto.findByMediaPackageTrackAndStatusQuery(mpId, trackId, status)).stream()
          .map(TranscriptionJobControlDto::toTranscriptionJobControl)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

  @Override
  public TranscriptionProviderControl findIdByProvider(String provider) throws TranscriptionDatabaseException {
    try {
      TranscriptionProviderControl tpc = db.exec(TranscriptionProviderControlDto.findIdByProviderQuery(provider))
          .map(TranscriptionProviderControlDto::toTranscriptionProviderControl)
          .orElse(null);
      if (tpc != null) {
        return tpc;
      }

      // store provider and retrieve id
      TranscriptionProviderControl dto = storeProviderControl(provider);
      return db.exec(TranscriptionProviderControlDto.findIdByProviderQuery(provider))
          .map(TranscriptionProviderControlDto::toTranscriptionProviderControl)
          .orElse(null);
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

  @Override
  public TranscriptionProviderControl findProviderById(Long id) throws TranscriptionDatabaseException {
    try {
      return db.exec(TranscriptionProviderControlDto.findProviderByIdQuery(id))
          .map(TranscriptionProviderControlDto::toTranscriptionProviderControl)
          .orElse(null);
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

  private long getProviderId(String provider) throws TranscriptionDatabaseException {
    TranscriptionProviderControl providerInfo = findIdByProvider(provider);
    if (providerInfo != null) {
      return providerInfo.getId();
    }
    return noProviderId;
  }
}
