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
package org.opencastproject.lifecyclemanagement.api;

import org.opencastproject.elasticsearch.index.objects.event.EventSearchQueryField;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Represents a life cycle policy in Opencast
 */
public interface LifeCyclePolicy {

  /**
   * Gets the policy's identifier
   *
   * @return the policy identifier
   */
  String getId();

  /**
   * Sets the policy's identifier
   *
   * @param id the policy identifier
   */
  void setId(String id);

  /**
   * Gets the policy's organization identifier
   *
   * @return the organization identifier
   */
  String getOrganization();

  /**
   * Sets the policy's organization identifier
   *
   * @param organization the organization identifier
   */
  void setOrganization(String organization);

  /**
   * Gets the policy's title
   *
   * @return the title
   */
  String getTitle();

  /**
   * Sets the policy's title
   *
   * @param title the title
   */
  void setTitle(String title);

  /**
   * Gets the type of entity this policy targets
   *
   * @return the {@link TargetType}
   */
  TargetType getTargetType();

  /**
   * Sets the type of entity this policy targets
   *
   * @param targetType the {@link TargetType}
   */
  void setTargetType(TargetType targetType);

  /**
   * Gets the action to be performed when the policy is applied
   *
   * @return the {@link Action}
   */
  Action getAction();

  /**
   * Sets the action to be performed when the policy is applied
   *
   * @param action the {@link Action}
   */
  void setAction(Action action);

  /**
   * Gets the parameters for the policy's {@link Action}
   *
   * @return the action parameters
   */
  String getActionParameters();

  /**
   * Sets the parameters for the policy's {@link Action}
   *
   * @param actionParameters the action parameters
   */
  void setActionParameters(String actionParameters);

  /**
   * Gets the date at which the policy's action should be performed
   *
   * @return the action date
   */
  Date getActionDate();

  /**
   * Sets the date at which the policy's action should be performed
   *
   * @param actionDate the action date
   */
  void setActionDate(Date actionDate);

  /**
   * Gets the cron expression that triggers the policy for a {@link Timing#REPEATING} policy
   *
   * @return the cron trigger expression
   */
  String getCronTrigger();

  /**
   * Sets the cron expression that triggers the policy for a {@link Timing#REPEATING} policy
   *
   * @param cronTrigger the cron trigger expression
   */
  void setCronTrigger(String cronTrigger);

  /**
   * Gets the policy's {@link Timing}
   *
   * @return the timing
   */
  Timing getTiming();

  /**
   * Sets the policy's {@link Timing}
   *
   * @param timing the timing
   */
  void setTiming(Timing timing);

  /**
   * Returns whether the policy is currently active
   *
   * @return true if the policy is active
   */
  boolean isActive();

  /**
   * Sets whether the policy is currently active
   *
   * @param active true if the policy is active
   */
  void setActive(boolean active);

  /**
   * Returns whether the policy was created from a configuration file
   *
   * @return true if the policy was created from configuration
   */
  boolean isCreatedFromConfig();

  /**
   * Sets whether the policy was created from a configuration file
   *
   * @param createdFromConfig true if the policy was created from configuration
   */
  void setCreatedFromConfig(boolean createdFromConfig);

  /**
   * Gets the filters used to determine which entities this policy targets
   *
   * @return the target filters
   */
  Map<String, Map<String, EventSearchQueryField<String>>> getTargetFilters();

  /**
   * Sets the filters used to determine which entities this policy targets
   *
   * @param targetFilters the target filters
   */
  void setTargetFilters(Map<String, Map<String, EventSearchQueryField<String>>> targetFilters);

  /**
   * Gets the access control entries restricting who may manage this policy
   *
   * @return the list of {@link LifeCyclePolicyAccessControlEntry}s
   */
  List<LifeCyclePolicyAccessControlEntry> getAccessControlEntries();

  /**
   * Sets the access control entries restricting who may manage this policy
   *
   * @param accessControlEntries the list of {@link LifeCyclePolicyAccessControlEntry}s
   */
  void setAccessControlEntries(List<LifeCyclePolicyAccessControlEntry> accessControlEntries);
}
