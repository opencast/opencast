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

import org.opencastproject.lifecyclemanagement.api.Action;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicyAccessControlEntry;
import org.opencastproject.lifecyclemanagement.api.TargetType;
import org.opencastproject.lifecyclemanagement.api.Timing;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

// TODO: Turn the lifecycle modules into a plugin
// TODO: Comments!
@Entity(name = "LifeCyclePolicy")
@Table(name = "oc_lifecyclepolicy")
@NamedQueries({
    @NamedQuery(
        name = "LifeCyclePolicy.findById",
        query = "SELECT p FROM LifeCyclePolicy p WHERE p.id = :id and p.organization = :organizationId"
    ),
    @NamedQuery(
        name = "LifeCyclePolicy.findActive",
        query = "SELECT p FROM LifeCyclePolicy p WHERE p.isActive = true and p.organization = :organizationId"
    ),
})
public class LifeCyclePolicyImpl implements LifeCyclePolicy {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  private String id;

  @Column(name = "organization", nullable = false, length = 128)
  private String organization;

  @Column(name = "title")
  private String title;

  @Column(name = "target_type")
  private TargetType targetType;

  @Column(name = "action")
  private Action action;

  // JSON
  @Column(name = "action_parameters")
  private String actionParameters;

  @Column(name = "actionDate")
  @Temporal(TemporalType.TIMESTAMP)
  private Date actionDate;

  @Column(name = "cronTrigger")
  private String cronTrigger;

  @Column(name = "timing")
  private Timing timing;

  @Column(name = "isActive")
  private boolean isActive = true;

  @ElementCollection
  @MapKeyColumn(name = "filter_name")
  @Column(name = "filter_value")
  @CollectionTable(name = "example_attributes", joinColumns = @JoinColumn(name = "example_id"))
  private Map<String, String> targetFilters = new HashMap<String, String>();

  @OneToMany(
      fetch = FetchType.LAZY,
      cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE },
      mappedBy = "lifecyclepolicy",
      orphanRemoval = true
  )
  @OrderColumn(name = "position_access_control_entries")
  private List<LifeCyclePolicyAccessControlEntry> accessControlEntries = new ArrayList<>();

  public LifeCyclePolicyImpl() {

  }

  public LifeCyclePolicyImpl(String title, TargetType targetType, Action action, String actionParameters,
      Date actionDate, String cronTrigger, Timing timing,
      Map<String, String> targetFilters, List<LifeCyclePolicyAccessControlEntry> accessControlEntries) {
    this.title = title;
    this.targetType = targetType;
    this.action = action;
    this.actionParameters = actionParameters;
    this.actionDate = actionDate;
    this.cronTrigger = cronTrigger;
    this.timing = timing;
    this.targetFilters = targetFilters;
    for (var accessControlEntry : accessControlEntries) {
      accessControlEntry.setPolicy(this);
    }
    this.accessControlEntries = accessControlEntries;
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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public TargetType getTargetType() {
    return targetType;
  }

  public void setTargetType(TargetType targetType) {
    this.targetType = targetType;
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  public String getActionParameters() {
    return actionParameters;
  }

  public void setActionParameters(String actionParameters) {
    this.actionParameters = actionParameters;
  }

  public Date getActionDate() {
    return actionDate;
  }

  public void setActionDate(Date actionDate) {
    this.actionDate = actionDate;
  }

  public String getCronTrigger() {
    return cronTrigger;
  }

  public void setCronTrigger(String cronTrigger) {
    this.cronTrigger = cronTrigger;
  }

  public Timing getTiming() {
    return timing;
  }

  public void setTiming(Timing timing) {
    this.timing = timing;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public Map<String, String> getTargetFilters() {
    return targetFilters;
  }

  public void setTargetFilters(Map<String, String> targetFilters) {
    this.targetFilters = targetFilters;
  }

  public List<LifeCyclePolicyAccessControlEntry> getAccessControlEntries() {
    return accessControlEntries;
  }

  public void setAccessControlEntries(List<LifeCyclePolicyAccessControlEntry> accessControlEntries) {
    for (var accessControlEntry : accessControlEntries) {
      accessControlEntry.setPolicy(this);
    }
    this.accessControlEntries = accessControlEntries;
  }
}
