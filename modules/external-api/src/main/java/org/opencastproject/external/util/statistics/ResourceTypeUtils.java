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
package org.opencastproject.external.util.statistics;

import org.opencastproject.statistics.api.ResourceType;

import org.apache.commons.lang3.StringUtils;

/* To ensure that statistics provider resource types returned by the External API are not
   accidentally changed, this utility class performs an exlicit mapping of resource types
   supported by the statistics service to External API statistics resource types.
   Resource types unknown to the External API are mappend to RESOURCE_TYPE_UNKOWN. */
public final class ResourceTypeUtils {

  /* Resource types used by the External API */
  private static final String RESOURCE_TYPE_EPISODE = "episode";
  private static final String RESOURCE_TYPE_SERIES = "series";
  private static final String RESOURCE_TYPE_ORGANIZATION = "organization";

  private static final String RESOURCE_TYPE_UNKNOWN = "unknown";

  private ResourceTypeUtils() {
  }

  public static String toString(ResourceType resourceType) {
    String result;
    switch (resourceType) {
      case EPISODE:
        result = RESOURCE_TYPE_EPISODE;
        break;
      case SERIES:
        result = RESOURCE_TYPE_SERIES;
        break;
      case ORGANIZATION:
        result = RESOURCE_TYPE_ORGANIZATION;
        break;
      default:
        result = RESOURCE_TYPE_UNKNOWN;
        break;
    }
    return result;
  }

  public static ResourceType fromString(String resourceType) throws IllegalArgumentException {
    ResourceType result = null;
    if (StringUtils.isNotBlank(resourceType)) {
      result = Enum.valueOf(ResourceType.class, resourceType.toUpperCase());
    }
    return result;
  }

}
