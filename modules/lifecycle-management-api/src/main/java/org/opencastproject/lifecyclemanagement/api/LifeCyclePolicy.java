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

public interface LifeCyclePolicy {
  String getId();

  void setId(String id);

  String getOrganization();

  void setOrganization(String organization);

  String getTitle();

  void setTitle(String title);

  TargetType getTargetType();

  void setTargetType(TargetType targetType);

  Action getAction();

  void setAction(Action action);

  String getActionParameters();

  void setActionParameters(String actionParameters);

  Date getActionDate();

  void setActionDate(Date actionDate);

  String getCronTrigger();

  void setCronTrigger(String cronTrigger);

  Timing getTiming();

  void setTiming(Timing timing);

  boolean isActive() ;

  void setActive(boolean active);

  Map<String, EventSearchQueryField<String>> getTargetFilters();

  void setTargetFilters(Map<String, EventSearchQueryField<String>> targetFilters);

  List<LifeCyclePolicyAccessControlEntry> getAccessControlEntries();

  void setAccessControlEntries(List<LifeCyclePolicyAccessControlEntry> accessControlEntries);
}
