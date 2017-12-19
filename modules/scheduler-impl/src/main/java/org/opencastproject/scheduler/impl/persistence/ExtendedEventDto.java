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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for storing extended scheduled event information in persistence storage.
 */
@IdClass(EventIdPK.class)
@Entity(name = "ExtendedEvent")
@NamedQueries({
        // Job queries
        @NamedQuery(name = "ExtendedEvent.findAll", query = "SELECT e FROM ExtendedEvent e WHERE e.organization = :org"),
        @NamedQuery(name = "ExtendedEvent.countAll", query = "SELECT COUNT(e) FROM ExtendedEvent e WHERE e.organization = :org")
        // @NamedQuery(name = "ExtendedEvent.countRespones", query =
        // "SELECT COUNT(e) FROM ExtendedEvent e WHERE e.reviewDate IS NOT NULL"),
        // @NamedQuery(name = "ExtendedEvent.countConfirmed", query =
        // "SELECT COUNT(e) FROM ExtendedEvent e WHERE e.reviewStatus =
        // org.opencastproject.scheduler.api.SchedulerService$ReviewStatus.CONFIRMED"),
        // @NamedQuery(name = "ExtendedEvent.countConfirmedByDateRange", query =
        // "SELECT COUNT(e) FROM ExtendedEvent e WHERE e.reviewDate >= :start AND e.reviewDate < :end AND e.reviewStatus
        // = org.opencastproject.scheduler.api.SchedulerService$ReviewStatus.CONFIRMED"),
        // @NamedQuery(name = "ExtendedEvent.countUnconfirmed", query =
        // "SELECT COUNT(e) FROM ExtendedEvent e WHERE e.reviewStatus =
        // org.opencastproject.scheduler.api.SchedulerService$ReviewStatus.UNCONFIRMED")
})
@Table(name = "mh_scheduled_extended_event", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "mediapackage_id", "organization" }) })
public class ExtendedEventDto {

  /** Event ID, primary key */
  @Id
  @Column(name = "mediapackage_id", length = 128)
  private String mediaPackageId;

  /** Organization id, primary key */
  @Id
  @Column(name = "organization", length = 128)
  private String organization;

  /**
   * Default constructor without any import.
   */
  public ExtendedEventDto() {
  }

  /**
   * Returns the mediapackage ID.
   *
   * @return the mediapackage ID
   */
  public String getMediaPackageId() {
    return mediaPackageId;
  }

  /**
   * Sets the mediapackage ID.
   *
   * @param mediaPackageId
   *          the mediapackage ID
   */
  public void setMediaPackageId(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
  }

  /**
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * @param organization
   *          the organization to set
   */
  public void setOrganization(String organization) {
    this.organization = organization;
  }

}
