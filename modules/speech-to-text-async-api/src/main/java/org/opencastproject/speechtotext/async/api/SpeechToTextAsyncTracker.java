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
package org.opencastproject.speechtotext.async.api;

import org.opencastproject.job.api.Job;

public interface SpeechToTextAsyncTracker {
  /** Workflow configuration name to store jobs in */
  String JOBS_WORKFLOW_CONFIGURATION = "speech-to-text-jobs";
  /** Collection where input files are placed when running STT asynchronously */
  String STT_ASYNC_COLLECTION = "stt-async";

  /**
   * Start tracking all jobs created by a speech-to-text woh.
   *
   * @param job
   *          the STT job
   * @param mediaPackageId
   *          the media package id
   * @param workflowId
   *          the workflow that started the jobs
   * @throws SpeechToTextAsyncException
   *           if an exception occurs
   */
  void track(Job job, String mediaPackageId, long workflowId) throws SpeechToTextAsyncException;

  /**
   * Stop tracking this STT job (subtitles have already been attached or tracking was abandoned.
   *
   * @param job
   *          the STT job
   * @throws SpeechToTextAsyncException
   *           if an exception occurs
   */
  void untrack(Job job) throws SpeechToTextAsyncException;

  /**
   * Stop tracking all STT jobs created by a workflow.
   *
   * @param workflowId
   *          the workflow id
   * @throws SpeechToTextAsyncException
   *           if an exception occurs
   */
  void untrackAll(long workflowId) throws SpeechToTextAsyncException;
}
