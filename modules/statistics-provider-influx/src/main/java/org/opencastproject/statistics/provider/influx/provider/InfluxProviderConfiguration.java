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

package org.opencastproject.statistics.provider.influx.provider;

import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.ProviderConfiguration;
import org.opencastproject.statistics.api.ResourceType;

import com.google.gson.Gson;

import java.util.Set;

public class InfluxProviderConfiguration extends ProviderConfiguration {
  private static final Gson gson = new Gson();

  public class InfluxProviderSource {
    private String aggregation;
    private String aggregationVariable;
    private String measurement;
    private String resourceIdName;
    private Set<DataResolution> resolutions;

    public InfluxProviderSource()  {
      // needed for gson
    }

    public InfluxProviderSource(
        String aggregation,
        String aggregationVariable,
        String measurement,
        String resourceIdName,
        Set<DataResolution> resolutions
    ) {
      this.aggregation = aggregation;
      this.aggregationVariable = aggregationVariable;
      this.measurement = measurement;
      this.resourceIdName = resourceIdName;
      this.resolutions = resolutions;
    }

    public String getAggregation() {
      return aggregation;
    }

    public String getAggregationVariable() {
      return aggregationVariable;
    }

    public String getMeasurement() {
      return measurement;
    }

    public String getResourceIdName() {
      return resourceIdName;
    }

    public Set<DataResolution> getResolutions() {
      return resolutions;
    }
  }

  private Set<InfluxProviderSource> sources;

  public InfluxProviderConfiguration()  {
    // needed for gson
  }

  public InfluxProviderConfiguration(
      String id,
      String title,
      String description,
      ResourceType resourceType,
      String type,
      Set<InfluxProviderSource> sources
  ) {
    super(id, title, description, resourceType, type);
    this.sources = sources;
  }


  public Set<InfluxProviderSource> getSources() {
    return sources;
  }

  public static InfluxProviderConfiguration fromJson(String json) {
    return gson.fromJson(json, InfluxProviderConfiguration.class);
  }
}
