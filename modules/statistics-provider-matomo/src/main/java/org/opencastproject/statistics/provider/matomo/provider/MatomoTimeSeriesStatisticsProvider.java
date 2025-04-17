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

package org.opencastproject.statistics.provider.matomo.provider;

import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.ResourceType;
import org.opencastproject.statistics.api.TimeSeries;
import org.opencastproject.statistics.api.TimeSeriesProvider;
import org.opencastproject.statistics.provider.matomo.StatisticsProviderMatomoService;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class MatomoTimeSeriesStatisticsProvider extends MatomoStatisticsProvider implements TimeSeriesProvider {

  private static final Logger logger = LoggerFactory.getLogger(MatomoTimeSeriesStatisticsProvider.class);

  private Set<MatomoProviderConfiguration.MatomoProviderSource> sources;

  public MatomoTimeSeriesStatisticsProvider(
      StatisticsProviderMatomoService service,
      String id,
      ResourceType resourceType,
      String title,
      String description,
      Set<MatomoProviderConfiguration.MatomoProviderSource> sources
  ) {
    super(service, id, resourceType, title, description);
    this.sources = sources;
  }

  @Override
  public TimeSeries getValues(String resourceId, Instant from, Instant to, DataResolution resolution, ZoneId zoneId) {
    final String matomoPeriod = dataResolutionToMatomoPeriod(resolution);
    final List<String> labels = new ArrayList<>();
    final List<Double> values = new ArrayList<>();
    final MatomoProviderConfiguration.MatomoProviderSource source = getSource(resolution);

    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    try {
      String url = service.getMatomoApiUrl() + "/index.php?module=API"
          + "&format=json&filter_limit=-1&expanded=1"
          + "&idSite=" + source.getSiteId()
          + "&method=" + source.getMethod()
          + "&date="
          + from.atZone(zoneId).toLocalDate().format(inputFormatter) + ","
          + to.atZone(zoneId).toLocalDate().format(inputFormatter)
          + "&period=" + matomoPeriod;
      if (source.getDimensionId() != null) {
        url += "&idDimension=" + source.getDimensionId();
      }
      if (this.getResourceType() != ResourceType.ORGANIZATION) {
        url += "&label=" + resourceId;
      }

      // send Matomo API token as body parameter
      String tokenAuth = service.getMatomoApiToken();
      String requestBody = "token_auth=" + URLEncoder.encode(tokenAuth, StandardCharsets.UTF_8.name());

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() >= 300) {
        throw new IOException("Matomo API unexpected status code " + response.statusCode() + ": " + url);
      }

      String responseBody = response.body();

      try {
        JsonElement rootElement = JsonParser.parseString(responseBody);
        if (rootElement.isJsonObject()) {
          JsonObject jsonObject = rootElement.getAsJsonObject();
          for (String key : jsonObject.keySet()) {
            // Matomo API returns date in format YYYY-MM-DD, YYYY-MM, or YYYY
            // We need to convert it to YYYY-MM-DDT00:00:00Z
            String date = key;
            if (key.length() == 4) {
              date = key + "-01-01T00:00:00Z"; // add month, day and time
            } else if (key.length() == 7) {
              date = key + "-01T00:00:00Z"; // add day and time
            } else if (key.length() == 10) {
              date = key + "T00:00:00Z"; // add time
            }
            try {
              // check for valid date format YYYY-MM-DDT00:00:00Z
              outputFormatter.parse(date);
              labels.add(date);
            } catch (Exception e) {
              logger.warn("Unexpected date format {}: {}", key, e.getMessage());
              continue;
            }
            // Matomo API returns data either as an array of objects or as a single object
            // If it is an array, we need to find the object with the requested resourceId
            JsonObject matomoData = null;
            if (jsonObject.get(key).isJsonArray()) {
              JsonArray dataArray = jsonObject.get(key).getAsJsonArray();
              if (dataArray != null && dataArray.size() > 0) {
                if (this.getResourceType() != ResourceType.ORGANIZATION) {
                  // Get the data for the requested resourceId (event or series)
                  for (JsonElement element : dataArray) {
                    JsonObject dataElement = element.getAsJsonObject();
                    if (dataElement.has("label") && dataElement.get("label").getAsString().equals(resourceId)) {
                      matomoData = element.getAsJsonObject();
                      break;
                    }
                  }
                } else {
                  // Get the first element of the array if resource type is organization and more than one element
                  // is returned
                  matomoData = dataArray.get(0).getAsJsonObject();
                }
              }
            } else if (jsonObject.get(key).isJsonObject()) {
              matomoData = jsonObject.get(key).getAsJsonObject();
            }
            if (matomoData != null && matomoData.has(source.getAggregationVariable())) {
              logger.debug("Matomo data [date: {}, key: {}, value: {}]",
                  key,
                  source.getAggregationVariable(),
                  matomoData.get(source.getAggregationVariable()));
              values.add(matomoData.get(source.getAggregationVariable()).getAsDouble());
            } else {
              values.add(0.0);
            }
          }
        } else {
          logger.warn("Unexpected JSON format: Root element is not a JSON object.");
        }
      } catch (JsonParseException e) {
        logger.error("Error parsing Matomo API response: {}", e.getMessage());
      }
    } catch (Exception e) {
      logger.error("Error connecting to Matomo API: {}", e.getMessage());
    }

    final Double total = "SUM".equalsIgnoreCase(source.getAggregation())
        ? values.stream().mapToDouble(v -> v).sum()
        : null;
    logger.debug("Labels {}; Values: {}; Total: {}", labels, values, total);
    return new TimeSeries(labels, values, total);
  }

  @Override
  public Set<DataResolution> getDataResolutions() {
    return sources.stream().flatMap(s -> s.getResolutions().stream()).collect(Collectors.toSet());
  }

  private MatomoProviderConfiguration.MatomoProviderSource getSource(DataResolution resolution) {
    return sources.stream()
        .filter(s -> s.getResolutions().contains(resolution))
        .findAny()
        .orElseThrow(() -> new IllegalStateException("No source available for data resolution " + resolution.name()));
  }

}
