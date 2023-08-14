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
package org.opencastproject.adminui.impl;

import org.opencastproject.statistics.api.DataResolution;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public final class ProviderQuery {
  private String providerId;
  private Instant from;
  private Instant to;
  private String resourceId;
  private DataResolution dataResolution;

  public ProviderQuery(RawProviderQuery query) {
    this.setProviderId(query.getProviderId());
    this.setFrom(query.getFrom());
    this.setTo(query.getTo());
    if (this.to.compareTo(this.from) <= 0) {
      throw new IllegalArgumentException("'from' date must be before 'to' date");
    }
    this.setDataResolution(query.getDataResolution());
    this.setResourceId(query.getResourceId());
  }

  public ProviderQuery(String providerId, String from, String to, String dataResolution, String resourceId) {
    this.setProviderId(providerId);
    this.setFrom(from);
    this.setTo(to);
    if (this.to.compareTo(this.from) <= 0) {
      throw new IllegalArgumentException("'from' date must be before 'to' date");
    }
    this.setDataResolution(dataResolution);
    this.setResourceId(resourceId);
  }

  private void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  private void setFrom(String from) {
    if (isNotIso8601Utc(from)) {
      throw new IllegalArgumentException("Missing value for 'from' or not in ISO 8601 UTC format");
    }
    this.from = Instant.parse(from);
  }

  private void setTo(String to) {
    if (isNotIso8601Utc(to)) {
      throw new IllegalArgumentException("Missing value for 'to' or not in ISO 8601 UTC format");
    }
    this.to = Instant.parse(to);
  }

  private void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  private void setDataResolution(String dataResolution) {
    Optional<DataResolution> resolution = dataResolutionFromJson(dataResolution);
    if (!resolution.isPresent()) {
      throw new IllegalArgumentException("illegal value for 'resolution'");
    }
    this.dataResolution = resolution.get();
  }

  public String getProviderId() {
    return providerId;
  }

  public Instant getFrom() {
    return from;
  }

  public Instant getTo() {
    return to;
  }

  public String getResourceId() {
    return resourceId;
  }

  public DataResolution getDataResolution() {
    return dataResolution;
  }

  private static boolean isNotIso8601Utc(final String value) {
    try {
      Instant.parse(value);
      return false;
    } catch (DateTimeParseException e) {
      return true;
    }
  }

  private static Optional<DataResolution> dataResolutionFromJson(final String dataResolution) {
    try {
      return Optional.of(Enum.valueOf(DataResolution.class, dataResolution.toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
