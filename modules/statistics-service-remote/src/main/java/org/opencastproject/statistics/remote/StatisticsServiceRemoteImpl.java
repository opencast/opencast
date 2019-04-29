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

package org.opencastproject.statistics.remote;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.ResourceType;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsService;
import org.opencastproject.statistics.api.TimeSeries;
import org.opencastproject.statistics.api.TimeSeriesProvider;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Path;

/**
 * A proxy to a remote series service.
 */
@Path("/")
@RestService(name = "statisticsservice", title = "Statistics Service Remote", abstractText = "This service provides statistics.", notes = {
    "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
    "If the service is down or not working it will return a status 503, this means the the underlying service is "
        + "not working and is either restarting or has failed",
    "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"})
public class StatisticsServiceRemoteImpl extends RemoteBase implements StatisticsService {

  private static final JSONParser jsonParser = new JSONParser();

  public StatisticsServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Set<StatisticsProvider> getProviders() {
    final HttpGet get = new HttpGet("providers.json");
    final HttpResponse response = getResponse(get, SC_OK);
    try {
      if (response != null) {
        return jsonToProviders(EntityUtils.toString(response.getEntity(), UTF_8));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to get providers from remote service");
  }

  @Override
  public Set<StatisticsProvider> getProviders(ResourceType resourceType) {
    return getProviders().stream().filter(p -> p.getResourceType().equals(resourceType)).collect(Collectors.toSet());
  }

  @Override
  public Optional<StatisticsProvider> getProvider(String providerId) {
    return getProviders().stream().filter(p -> p.getId().equals(providerId)).findAny();
  }

  @Override
  public TimeSeries getTimeSeriesData(StatisticsProvider provider, String resourceId, Instant from, Instant to, DataResolution resolution, ZoneId zoneId) {
    final List<NameValuePair> queryStringParams = new ArrayList<>();
    queryStringParams.add(new BasicNameValuePair("resourceId", resourceId));
    queryStringParams.add(new BasicNameValuePair("from", from.toString()));
    queryStringParams.add(new BasicNameValuePair("to", to.toString()));
    queryStringParams.add(new BasicNameValuePair("resolution", resolution.name()));
    queryStringParams.add(new BasicNameValuePair("zoneId", zoneId.getId()));
    final HttpGet get = new HttpGet("timeseries/" + provider.getId() + ".json?" + URLEncodedUtils.format(queryStringParams, UTF_8));
    final HttpResponse response = getResponse(get, SC_OK);
    try {
      if (response != null) {
        return jsonToTimeSeries(EntityUtils.toString(response.getEntity(), UTF_8));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to get time series data from remote service");
  }

  @SuppressWarnings("unchecked")
  private TimeSeries jsonToTimeSeries(String json) throws ParseException {
    final JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
    final JSONArray labelsJson = (JSONArray) jsonObject.get("labels");
    final JSONArray valuesJson = (JSONArray) jsonObject.get("values");
    final List<String> labels = new ArrayList<>();
    final List<Double> values = new ArrayList<>();
    for (int i = 0; i < labelsJson.size(); i++) {
      labels.add((String) labelsJson.get(i));
      values.add((Double) valuesJson.get(i));
    }
    return new TimeSeries(labels, values, (Double) jsonObject.getOrDefault("total", null));
  }

  private Set<StatisticsProvider> jsonToProviders(String json) throws ParseException, JSONException {
    final JSONArray providersJson = (JSONArray) jsonParser.parse(json);
    final Set<StatisticsProvider> providers = new HashSet<>();
    for (Object object : providersJson) {
      JSONObject jsonObject = (JSONObject) object;
      final String idJson = (String) jsonObject.get("id");
      final String titleJson = (String) jsonObject.get("title");
      final String descriptionJson = (String) jsonObject.get("description");
      final String resourceTypeJson = (String) jsonObject.get("resourceType");
      if (jsonObject.containsKey("dataResolutions")) {
        final JSONArray resolutionsJson = (JSONArray) jsonObject.get("dataResolutions");
        providers.add(new TimeSeriesProvider() {
          @Override
          public String getId() {
            return idJson;
          }

          @Override
          public ResourceType getResourceType() {
            return ResourceType.valueOf(resourceTypeJson);
          }

          @Override
          public String getTitle() {
            return titleJson;
          }

          @Override
          public String getDescription() {
            return descriptionJson;
          }

          @Override
          public TimeSeries getValues(String resourceId, Instant from, Instant to, DataResolution resolution, ZoneId zoneId) {
            throw new IllegalStateException("This method should never get called");
          }

          @Override
          public Set<DataResolution> getDataResolutions() {
            final Set<DataResolution> result = new HashSet<>();
            for (Object obj : resolutionsJson) {
              result.add(DataResolution.valueOf((String) obj));
            }
            return result;
          }
        });
      } else {
        providers.add(new StatisticsProvider() {
          @Override
          public String getId() {
            return idJson;
          }

          @Override
          public ResourceType getResourceType() {
            return ResourceType.valueOf(resourceTypeJson);
          }

          @Override
          public String getTitle() {
            return titleJson;
          }

          @Override
          public String getDescription() {
            return descriptionJson;
          }
        });
      }
    }
    return providers;
  }
}
