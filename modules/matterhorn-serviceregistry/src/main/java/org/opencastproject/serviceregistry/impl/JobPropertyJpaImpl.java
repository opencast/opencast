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

package org.opencastproject.serviceregistry.impl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * A JPA annotated version of the JaxbJobContext.
 *
 */
@Entity(name = "JobContext")
@Table(name = "mh_job_context")
public class JobPropertyJpaImpl {

  @Id
  @Column(name = "id")
  protected JobJpaImpl rootJob;

  @Id
  @Column(name = "name", length = 255)
  protected String name;

  @Lob
  @Column(name = "value", length = 65535)
  protected String value;

  /**
   * Default constructor needed by JPA
   */
  public JobPropertyJpaImpl() {
  }

  /**
   * Creates a new job context property.
   *
   * @param job
   *          the root job
   * @param name
   *          the property name
   * @param value
   *          the property value
   */
  public JobPropertyJpaImpl(JobJpaImpl job, String name, String value) {
    this.rootJob = job;
    this.name = name;
    this.value = value;
  }

  /**
   * @return the job
   */
  public JobJpaImpl getJob() {
    return rootJob;
  }

  /**
   * @param job
   *          the job to set
   */
  public void setJob(JobJpaImpl job) {
    this.rootJob = job;
  }

  /**
   * @return the key
   */
  public String getName() {
    return name;
  }

  /**
   * @param name
   *          the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * @param value
   *          the value to set
   */
  public void setValue(String value) {
    this.value = value;
  }

}
