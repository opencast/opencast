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
package org.opencastproject.speechtotext.async.persistence;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.job.jpa.JpaJob;

import java.util.Date;

public class SpeechToTextTestUtil {

  protected SpeechToTextTestUtil() {
  }

  public static JpaJob createJob(DBSession db, Date dateCreated, Job.Status status, long jobId) {
    Job job = new JobImpl(jobId);
    job.setCreator("John Harvard");
    job.setOrganization("mh_default_org");
    job.setStatus(status);
    job.setQueueTime(500L);
    job.setRunTime(1000L);
    job.setDateCreated(dateCreated);
    if (!status.isActive()) {
      job.setDateCompleted(new Date());
    }
    JpaJob jpaJob = db.execTx(namedQuery.persistOrUpdate(JpaJob.from(job)));
    return jpaJob;
  }

}
