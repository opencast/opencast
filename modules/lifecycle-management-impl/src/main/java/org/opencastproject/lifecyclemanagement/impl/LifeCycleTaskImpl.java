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

package org.opencastproject.lifecyclemanagement.impl;

import org.opencastproject.lifecyclemanagement.api.LifeCycleTask;
import org.opencastproject.lifecyclemanagement.api.Status;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity(name = "LifeCycleTask")
@Table(name = "oc_lifecycletask")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NamedQueries({
    @NamedQuery(
        name = "LifeCycleTask.findById",
        query = "SELECT p FROM LifeCycleTask p WHERE p.id = :id and p.organization = :organizationId"
    ),
    @NamedQuery(
        name = "LifeCycleTask.findByTargetId",
        query = "SELECT p FROM LifeCycleTask p WHERE p.targetId = :targetId and p.organization = :organizationId"
    ),
    @NamedQuery(
        name = "LifeCycleTask.withStatus",
        query = "SELECT p FROM LifeCycleTask p WHERE p.status = :status and p.organization = :organizationId"
    ),
})
public class LifeCycleTaskImpl implements LifeCycleTask {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  private String id;

  @Column(name = "organization", nullable = false, length = 128)
  private String organization;

  @Column(name = "lifeCyclePolicyId")
  private String lifeCyclePolicyId;

  @Column(name = "targetId")
  private String targetId;

  @Column(name = "status")
  private Status status;

  public LifeCycleTaskImpl() {

  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getLifeCyclePolicyId() {
    return lifeCyclePolicyId;
  }

  public void setLifeCyclePolicyId(String lifeCyclePolicyId) {
    this.lifeCyclePolicyId = lifeCyclePolicyId;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
