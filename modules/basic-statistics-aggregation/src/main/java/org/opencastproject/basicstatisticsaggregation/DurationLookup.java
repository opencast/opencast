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
package org.opencastproject.basicstatisticsaggregation;

import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Resolves a video's total duration in seconds, used for the "70% of video length" part of the view threshold.
 *
 * Looks it up via the elasticsearch index to avoid loading/parsing mediapackages. Also adds in a cache to maybe
 * not tax the index as much.
 *
 * Side note: We are effectively assuming that a video's duration does not change (or not change much) once it is
 * published. This is not technically true, for example a published video could be cut in the editor and republished.
 * But tracking the correct duration would bloat our raw events and add complexity beyond what we consider worth it
 * for a number that is extremely unlikely to ever change.
 */
public class DurationLookup {

  private static final Logger logger = LoggerFactory.getLogger(DurationLookup.class);

  /** Bounds memory use; large enough to comfortably cover everything actively watched at any one time. */
  private static final long MAX_CACHED_ITEMS = 1000;

  private final ElasticsearchIndex index;
  private final OrganizationDirectoryService organizationDirectoryService;
  private final String systemUserName;
  private final Cache<String, Long> cache = CacheBuilder.newBuilder().maximumSize(MAX_CACHED_ITEMS).build();

  public DurationLookup(ElasticsearchIndex index, OrganizationDirectoryService organizationDirectoryService,
      String systemUserName) {
    this.index = index;
    this.organizationDirectoryService = organizationDirectoryService;
    this.systemUserName = systemUserName;
  }

  /**
   * @return the video's duration in seconds, or empty if it could not be determined
   */
  public Optional<Long> getDurationSeconds(String organizationId, String itemId) {
    Long cached = cache.getIfPresent(itemId);
    if (cached != null) {
      return Optional.of(cached);
    }

    Optional<Long> result = lookup(organizationId, itemId);
    result.ifPresent(duration -> cache.put(itemId, duration));
    return result;
  }

  private Optional<Long> lookup(String organizationId, String itemId) {
    try {
      Organization organization = organizationDirectoryService.getOrganization(organizationId);
      User systemUser = SecurityUtil.createSystemUser(systemUserName, organization);
      // Event.duration is set from Dublin Core temporal period math (EventIndexUtils), in milliseconds.
      return index.getEvent(itemId, organization, systemUser)
          .map(Event::getDuration)
          .map(durationMillis -> durationMillis / 1000);
    } catch (Exception e) {
      logger.warn("Could not look up duration for item {} in organization {}", itemId, organizationId, e);
      return Optional.empty();
    }
  }
}
