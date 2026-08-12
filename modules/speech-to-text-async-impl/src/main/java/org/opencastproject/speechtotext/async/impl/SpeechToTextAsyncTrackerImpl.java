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
package org.opencastproject.speechtotext.async.impl;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.jpa.JpaJob;
import org.opencastproject.speechtotext.async.api.SpeechToTextAsyncException;
import org.opencastproject.speechtotext.async.api.SpeechToTextAsyncTracker;
import org.opencastproject.speechtotext.async.persistence.SpeechToTextControl;
import org.opencastproject.speechtotext.async.persistence.SpeechToTextDatabase;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    immediate = true,
    service = {
        SpeechToTextAsyncTracker.class
    },
    property = {
        "service.description=Speech to Text Async Tracker"
    })
public class SpeechToTextAsyncTrackerImpl implements SpeechToTextAsyncTracker {

  private SpeechToTextDatabase database;

  /**
   * {@inheritDoc}
   */
  @Override
  public void track(Job job, String mediaPackageId, long workflowId) throws SpeechToTextAsyncException {
    database.storeSpeechToTextControl(mediaPackageId, workflowId, JpaJob.from(job));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void untrack(Job job) throws SpeechToTextAsyncException {
    database.updateStatusByJob(SpeechToTextControl.Status.Done, JpaJob.from(job));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void untrackAll(long workflowId) throws SpeechToTextAsyncException {
    database.deleteByWorkflow(workflowId);
  }

  @Reference
  public void setSpeechToTextDatabase(SpeechToTextDatabase database) {
    this.database = database;
  }
}
