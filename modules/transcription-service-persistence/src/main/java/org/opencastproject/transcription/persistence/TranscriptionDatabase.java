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
package org.opencastproject.transcription.persistence;

import java.util.Date;
import java.util.List;

/*
 * Interface for transcription service database.
 */
public interface TranscriptionDatabase {

  /*
   * Store transcription service job
   */
  TranscriptionJobControl storeJobControl(String mpId, String trackId, String jobId, String jobStatus,
          long trackDuration, Date dateExpected, String provider) throws TranscriptionDatabaseException;

  /*
   * Store transcription provider
   */
  TranscriptionProviderControl storeProviderControl(String provider) throws TranscriptionDatabaseException;

  /*
   * Delete transcription job
   */
  void deleteJobControl(String jobId) throws TranscriptionDatabaseException;

  /*
   * Update transcription job
   */
  void updateJobControl(String jobId, String jobStatus) throws TranscriptionDatabaseException;

  /*
   * Get transcription job by job Id
   */
  TranscriptionJobControl findByJob(String jobId) throws TranscriptionDatabaseException;

  /*
   * Get transcription service job list by mediapackage Id
   */
  List<TranscriptionJobControl> findByMediaPackage(String mpId) throws TranscriptionDatabaseException;

  /*
   * Get transcription service job list by transcription status
   */
  List<TranscriptionJobControl> findByStatus(String... status) throws TranscriptionDatabaseException;

  /*
   * Get transcription service job list by mediapackage Id, track Id and transcription status
   */
  List<TranscriptionJobControl> findByMediaPackageTrackAndStatus(String mpId, String trackId, String... status)
    throws TranscriptionDatabaseException;

  /*
   * Find transcription provider ID by provider name
   */
  TranscriptionProviderControl findIdByProvider(String provider) throws TranscriptionDatabaseException;

  /*
   * Find transcription provider name by provider ID
   */
  TranscriptionProviderControl findProviderById(Long id) throws TranscriptionDatabaseException;
}
