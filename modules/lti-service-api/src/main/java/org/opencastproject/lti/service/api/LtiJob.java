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
package org.opencastproject.lti.service.api;

/**
 * Represents a job for the LTI user interface
 */
public final class LtiJob {
  private final String title;
  private final String status;

  /**
   * Construct a job
   * @param title The job title
   * @param status The job status
   */
  public LtiJob(final String title, final String status) {
    this.title = title;
    this.status = status;
  }

  /**
   * Get the job title
   * @return The job title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Get the job status
   * @return The job status
   */
  public String getStatus() {
    return status;
  }
}
