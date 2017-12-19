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
package org.opencastproject.scheduler.impl.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for storing scheduled last modified dates in persistence storage.
 */
@Entity(name = "LastModified")
@NamedQueries({
        @NamedQuery(name = "LastModified.findAll", query = "SELECT e FROM LastModified e "),
        @NamedQuery(name = "LastModified.findById", query = "SELECT e FROM LastModified e WHERE e.captureAgentId = :agentId"),
        @NamedQuery(name = "LastModified.countAll", query = "SELECT COUNT(e) FROM LastModified e ") })
@Table(name = "mh_scheduled_last_modified", uniqueConstraints = { @UniqueConstraint(columnNames = { "capture_agent_id" }) })
public class LastModifiedDto {

  /** Capture agent ID */
  @Id
  @Column(name = "capture_agent_id", length = 255)
  protected String captureAgentId;

  @Column(name = "last_modified")
  @Temporal(TemporalType.TIMESTAMP)
  protected Date lastModifiedDate;

  /**
   * Default constructor without any import.
   */
  public LastModifiedDto() {
  }

  /**
   * Returns the capture agent ID.
   *
   * @return the capture agent ID
   */
  public String getCaptureAgentId() {
    return captureAgentId;
  }

  /**
   * Sets the capture agent ID.
   *
   * @param captureAgentId
   *          the capture agent ID
   */
  public void setCaptureAgentId(String captureAgentId) {
    this.captureAgentId = captureAgentId;
  }

  /**
   * Returns the last modified date
   *
   * @return the review date
   */
  public Date getLastModifiedDate() {
    return lastModifiedDate;
  }

  /**
   * Sets the last modified date
   *
   * @param lastModifiedDate
   *          the last modified date
   */
  public void setLastModifiedDate(Date lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

}
