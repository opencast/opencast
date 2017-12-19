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
package org.opencastproject.transcription.ibmwatson.persistence;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

public class TranscriptionDatabase {
  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(TranscriptionDatabase.class);

  /** Persistence provider set by OSGi */
  private PersistenceProvider persistenceProvider;

  /** Factory used to create entity managers for transactions */
  protected EntityManagerFactory emf;

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for transcription service");
  }

  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /** OSGi callback to set persistence provider. */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  public TranscriptionJobControl storeJobControl(String mpId, String trackId, String jobId, String jobStatus,
          long trackDuration) throws TranscriptionDatabaseException {
    TranscriptionJobControlDto dto = TranscriptionJobControlDto.store(emf.createEntityManager(), mpId, trackId, jobId,
            jobStatus, trackDuration);
    if (dto != null)
      return dto.toTranscriptionJobControl();
    return null;
  }

  public void deleteJobControl(String jobId) throws TranscriptionDatabaseException {
    TranscriptionJobControlDto.delete(emf.createEntityManager(), jobId);
  }

  public void updateJobControl(String jobId, String jobStatus) throws TranscriptionDatabaseException {
    TranscriptionJobControlDto.updateStatus(emf.createEntityManager(), jobId, jobStatus);
  }

  public TranscriptionJobControl findByJob(String jobId) throws TranscriptionDatabaseException {
    TranscriptionJobControlDto dto = TranscriptionJobControlDto.findByJob(emf.createEntityManager(), jobId);
    if (dto != null)
      return dto.toTranscriptionJobControl();
    return null;
  }

  public List<TranscriptionJobControl> findByMediaPackage(String mpId) throws TranscriptionDatabaseException {
    List<TranscriptionJobControlDto> list = TranscriptionJobControlDto.findByMediaPackage(emf.createEntityManager(),
            mpId);
    List<TranscriptionJobControl> resultList = new ArrayList<TranscriptionJobControl>();
    for (TranscriptionJobControlDto dto : list) {
      resultList.add(dto.toTranscriptionJobControl());
    }
    return resultList;
  }

  public List<TranscriptionJobControl> findByStatus(String... status) throws TranscriptionDatabaseException {
    List<TranscriptionJobControlDto> list = TranscriptionJobControlDto.findByStatus(emf.createEntityManager(), status);
    List<TranscriptionJobControl> resultList = new ArrayList<TranscriptionJobControl>();
    for (TranscriptionJobControlDto dto : list) {
      resultList.add(dto.toTranscriptionJobControl());
    }
    return resultList;
  }

}
