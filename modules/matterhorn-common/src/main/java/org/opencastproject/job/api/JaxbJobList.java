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

package org.opencastproject.job.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A wrapper for job collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "jobs", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "jobs", namespace = "http://job.opencastproject.org")
public class JaxbJobList {
  /** A list of jobs */
  @XmlElement(name = "job")
  protected List<JaxbJob> jobs = new ArrayList<JaxbJob>();

  public JaxbJobList() {
  }

  public JaxbJobList(JaxbJob job) {
    this.jobs.add(job);
  }

  public JaxbJobList(Collection<Job> jobs) {
    if (jobs != null) {
      for (Job job : jobs) {
        this.jobs.add((JaxbJob) job);
      }
    }
  }

  /**
   * @return the jobs
   */
  public List<JaxbJob> getJobs() {
    return jobs;
  }

  /**
   * @param jobs
   *          the jobs to set
   */
  public void setJobs(List<JaxbJob> jobs) {
    this.jobs = jobs;
  }

  public void add(Job job) {
    if (job instanceof JaxbJob) {
      jobs.add((JaxbJob) job);
    } else {
      throw new IllegalArgumentException("Jobs must be an instance of JaxbJob");
    }
  }
}
