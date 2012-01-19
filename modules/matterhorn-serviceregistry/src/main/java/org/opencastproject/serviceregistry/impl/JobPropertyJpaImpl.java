/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
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
@Table(name = "job_context")
public class JobPropertyJpaImpl {

  @Id
  @Column(name = "root_job")
  protected JobJpaImpl rootJob;

  @Id
  @Column(name = "key")
  protected String key;

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
   * @param key
   *          the property key
   * @param value
   *          the property value
   */
  public JobPropertyJpaImpl(JobJpaImpl job, String key, String value) {
    this.rootJob = job;
    this.key = key;
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
  public String getKey() {
    return key;
  }

  /**
   * @param key
   *          the key to set
   */
  public void setKey(String key) {
    this.key = key;
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
