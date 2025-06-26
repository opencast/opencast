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

package org.opencastproject.maintenance.impl.perstistance;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * TODO
 */
@Entity(name = "MaintenanceEntity")
@Table(name = "oc_maintenance")
@NamedQueries({
    @NamedQuery(
        name = "Maintenance.getSingleEntry",
        query = "select m from MaintenanceEntity m" // if it more than one, throw an exception later
    )
})
public class MaintenanceEntity {

  @Id
  private Long id;

  @Column(name = "activate_maintenance")
  private boolean activateMaintenance = false;

  @Column(name = "activate_readonly")
  private boolean activateReadOnly = false;

  @Column(name = "start_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private LocalDateTime startDate = null;

  @Column(name = "end_date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private LocalDateTime endDate = null;

  /**
   * Default constructor without any import.
   */
  public MaintenanceEntity() {
  }

  // ----------------------------------------------------------------------------
  // Getters and setters
  // ----------------------------------------------------------------------------

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public boolean isActivateMaintenance() {
    return activateMaintenance;
  }

  public void setActivateMaintenance(boolean activateMaintenance) {
    this.activateMaintenance = activateMaintenance;
  }

  public boolean isActivateReadOnly() {
    return activateReadOnly;
  }

  public void setActivateReadOnly(boolean activateReadOnly) {
    this.activateReadOnly = activateReadOnly;
  }

  public LocalDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDateTime startDate) {
    this.startDate = startDate;
  }

  public LocalDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDateTime endDate) {
    this.endDate = endDate;
  }

  public boolean fieldsEqual(MaintenanceEntity anotherMaintenanceEntity) {
    return this.activateMaintenance == anotherMaintenanceEntity.isActivateMaintenance()
        && this.activateReadOnly == anotherMaintenanceEntity.isActivateReadOnly()
        && this.startDate.equals(anotherMaintenanceEntity.getStartDate())
        && this.endDate.equals(anotherMaintenanceEntity.getEndDate());
  }
}