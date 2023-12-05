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

package org.opencastproject.util;

import org.opencastproject.job.api.Job;

/**
 * This exception is thrown by the {@link org.opencastproject.job.api.JobBarrier} and indicates that the job has been
 * canceled.
 */
public class JobCanceledException extends RuntimeException {

  /** Serial version uid */
  private static final long serialVersionUID = -326050100245540889L;

  public JobCanceledException(Job job) {
    super(job + " has been canceled during service shutdown and will be rescheduled");
  }

}
