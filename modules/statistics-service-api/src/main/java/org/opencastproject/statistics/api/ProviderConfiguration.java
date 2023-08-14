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

package org.opencastproject.statistics.api;

/**
 * Model for the json configurations files for statistics providers.
 */
public abstract class ProviderConfiguration {
  private String id;
  private String title;
  private String description;
  private ResourceType resourceType;
  private String type;

  public ProviderConfiguration() {
    // needed for gson
  }

  public ProviderConfiguration(
      String id,
      String title,
      String description,
      ResourceType resourceType,
      String type
  ) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.resourceType = resourceType;
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public ResourceType getResourceType() {
    return resourceType;
  }

  public String getType() {
    return type;
  }
}
