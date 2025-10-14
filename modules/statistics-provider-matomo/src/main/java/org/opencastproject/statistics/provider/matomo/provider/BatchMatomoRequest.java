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
import org.opencastproject.statistics.provider.matomo.StatisticsProviderMatomoService;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BatchMatomoRequest {
  private static final Logger logger = LoggerFactory.getLogger(BatchMatomoRequest.class);

  private final StatisticsProviderMatomoService service;
  private final String method;
  private final Map<String, MatomoTimeSeriesStatisticsProvider> providers;
  private final Map<String, String> aggregationVariables;
  private final Map<String, String> aggregationTypes;

  // Cache entries expire after 5 minutes by default
  private static final long DEFAULT_CACHE_DURATION_MS = 5 * 60 * 1000;
  // Cache cleanup every minute
  private static final long CLEANUP_INTERVAL_MS = 60 * 1000;

  private final long cacheDurationMs;
  private final Map<CacheKey, CacheEntry> resultCache;
  private long lastCleanupTime;

  // Cache entry holding API response and timestamp
  private static class CacheEntry {
    private final JsonObject apiResponse;
    private final long timestamp;

    CacheEntry(JsonObject apiResponse) {
      this.apiResponse = apiResponse;
      this.timestamp = System.currentTimeMillis();
    }

    boolean isExpired(long maxAgeMs) {
      return System.currentTimeMillis() - timestamp > maxAgeMs;
    }

    JsonObject getApiResponse() {
      return apiResponse;
    }
  }

  // Key class for caching results based on request parameters.
  private static class CacheKey {
    private final String resourceId;
    private final Instant from;
    private final Instant to;
    private final String period;
    private final String siteId;
    private final String dimensionId;
    private final ZoneId zoneId;
    private final DataResolution resolution;

    CacheKey(String resourceId, Instant from, Instant to, String period,
                    String siteId, String dimensionId, ZoneId zoneId, DataResolution resolution) {
      this.resourceId = resourceId;
      this.from = from;
      this.to = to;
      this.period = period;
      this.siteId = siteId;
      this.dimensionId = dimensionId;
      this.zoneId = zoneId;
      this.resolution = resolution;
    }

    // Need to override equals when using as key in ConcurrentHashMap.
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CacheKey cacheKey = (CacheKey) o;
      // Compare all fields for equality. Objects.equals handles nulls. resolution is enum, so direct comparison.
      return Objects.equals(resourceId, cacheKey.resourceId)
             && Objects.equals(from, cacheKey.from)
             && Objects.equals(to, cacheKey.to)
             && Objects.equals(period, cacheKey.period)
             && Objects.equals(siteId, cacheKey.siteId)
             && Objects.equals(dimensionId, cacheKey.dimensionId)
             && Objects.equals(zoneId, cacheKey.zoneId)
             && resolution == cacheKey.resolution;
    }

    // need to override hashCode when equals is overridden. ConcurrentHashMap uses hashCode to find entries.
    @Override
    public int hashCode() {
      return Objects.hash(resourceId, from, to, period, siteId, dimensionId, zoneId, resolution);
    }
  }

  public BatchMatomoRequest(StatisticsProviderMatomoService service, String method) {
    this(service, method, DEFAULT_CACHE_DURATION_MS);
  }

  public BatchMatomoRequest(StatisticsProviderMatomoService service, String method, long cacheDurationMs) {
    this.service = service;
    this.method = method;
    this.providers = new HashMap<>();
    this.aggregationVariables = new HashMap<>();
    this.aggregationTypes = new HashMap<>();
    this.resultCache = new ConcurrentHashMap<>(); // Thread-safe cache
    this.cacheDurationMs = cacheDurationMs;
    this.lastCleanupTime = System.currentTimeMillis();
  }

  // cleanup expired cache entries after CLEANUP_INTERVAL_MS
  private void cleanupExpiredEntries() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
      resultCache.entrySet().removeIf(entry -> entry.getValue().isExpired(cacheDurationMs));
      lastCleanupTime = currentTime;
      logger.debug("Performed cache cleanup. Cache size: {}", resultCache.size());
    }
  }

  public void addProvider(
      MatomoTimeSeriesStatisticsProvider provider,
      String aggregationVariable,
      String aggregationType) {
    this.providers.put(provider.getId(), provider);
    this.aggregationVariables.put(provider.getId(), aggregationVariable);
    this.aggregationTypes.put(provider.getId(), aggregationType);
  }

  // Process the API response and extract time series for each provider
  private Map<String, TimeSeries> processApiResponse(
      JsonObject apiResponse,
      String resourceId,
      DataResolution resolution) {

    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    Map<String, TimeSeries> results = new HashMap<>();

    for (Map.Entry<String, MatomoTimeSeriesStatisticsProvider> entry : providers.entrySet()) {
      String providerId = entry.getKey();
      String aggregationVariable = aggregationVariables.get(providerId);

      List<String> labels = new ArrayList<>();
      List<Double> values = new ArrayList<>();

      for (String matomoDateStr : apiResponse.keySet()) {
        // Convert date format
        String date = matomoDateStr;
        if (matomoDateStr.length() == 4) {
          date = matomoDateStr + "-01-01T00:00:00Z";
        } else if (matomoDateStr.length() == 7) {
          date = matomoDateStr + "-01T00:00:00Z";
        } else if (matomoDateStr.length() == 10) {
          date = matomoDateStr + "T00:00:00Z";
        }

        try {
          outputFormatter.parse(date);
          labels.add(date);
        } catch (Exception e) {
          logger.warn("Unexpected date format {}: {}", matomoDateStr, e.getMessage());
          continue;
        }

        JsonObject matomoData = null;
        if (apiResponse.get(matomoDateStr).isJsonArray()) {
          // There are cases where Matomo returns multiple entries even when filtering by label.
          // In that case we need to find the entry matching the resourceId
          // (which is the event ID or series ID depending on resource type)
          // Otherwise take the first entry (should be only one)
          JsonArray dataArray = apiResponse.get(matomoDateStr).getAsJsonArray();
          if (dataArray != null && dataArray.size() > 0) {
            if (entry.getValue().getResourceType() != ResourceType.ORGANIZATION) {
              for (JsonElement element : dataArray) {
                JsonObject dataElement = element.getAsJsonObject();
                if (dataElement.has("label") && dataElement.get("label").getAsString().equals(resourceId)) {
                  matomoData = element.getAsJsonObject();
                  break;
                }
              }
            } else {
              matomoData = dataArray.get(0).getAsJsonObject();
            }
          }
        } else if (apiResponse.get(matomoDateStr).isJsonObject()) {
          matomoData = apiResponse.get(matomoDateStr).getAsJsonObject();
        }

        if (matomoData != null && matomoData.has(aggregationVariable)) {
          logger.debug("Matomo data for provider '{}' [{}, {}: {}]",
              providerId,
              matomoDateStr,
              aggregationVariable,
              matomoData.get(aggregationVariable));
          values.add(matomoData.get(aggregationVariable).getAsDouble());
        } else {
          // filling up 0.0 values where no data for date available
          values.add(0.0);
        }
      }

      // Calculate total only if aggregation type is SUM
      final Double total = "SUM".equalsIgnoreCase(aggregationTypes.get(providerId))
          ? values.stream().mapToDouble(v -> v).sum()
          : null;

      TimeSeries timeSeries = new TimeSeries(labels, values, total);
      results.put(providerId, timeSeries);
    }
    return results;
  }

  // Execute the batch request and return results for all providers
  public Map<String, TimeSeries> executeRequest(
      String resourceId,
      Instant from,
      Instant to,
      String matomoPeriod,
      String siteId,
      String dimensionId,
      ZoneId zoneId,
      DataResolution resolution) {

    // Periodically cleanup expired entries
    cleanupExpiredEntries();

    // Create cache key for this request
    CacheKey cacheKey = new CacheKey(resourceId, from, to, matomoPeriod,
        siteId, dimensionId, zoneId, resolution);

    // Check if we have valid cached results for these parameters
    CacheEntry cacheEntry = resultCache.get(cacheKey);
    if (cacheEntry != null) {
      if (!cacheEntry.isExpired(cacheDurationMs)) {
        // Process cached response
        logger.debug("Using cached Matomo API response for resourceId: {}, method: {}", resourceId, method);
        return processApiResponse(cacheEntry.getApiResponse(), resourceId, resolution);
      } else {
        // Remove expired entry
        resultCache.remove(cacheKey);
      }
    }

    // Make API request if no valid cache exists
    JsonObject apiResponse = null;
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    Map<String, TimeSeries> results = new HashMap<>();
    String url = "";

    try {
      url = service.getMatomoApiUrl() + "/index.php?module=API"
          + "&format=json&filter_limit=-1&expanded=1"
          + "&idSite=" + siteId
          + "&method=" + method
          + "&date="
          + from.atZone(zoneId).toLocalDate().format(inputFormatter) + ","
          + to.atZone(zoneId).toLocalDate().format(inputFormatter)
          + "&period=" + matomoPeriod;

      if (dimensionId != null) {
        url += "&idDimension=" + dimensionId;
      }

      if (!providers.isEmpty() && providers.values().iterator().next().getResourceType() != ResourceType.ORGANIZATION) {
        url += "&label=" + resourceId;
      }

      String tokenAuth = service.getMatomoApiToken();
      String requestBody = "token_auth=" + URLEncoder.encode(tokenAuth, StandardCharsets.UTF_8.name());
      logger.debug("Sending Matomo API requestf or resourceId: {}, method: {}: {}", resourceId, method, url);
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() >= 300) {
        logger.error("Matomo API unexpected status code " + response.statusCode() + ": " + url);
      } else {
        String responseBody = response.body();

        try {
          JsonElement rootElement = JsonParser.parseString(responseBody);
          if (rootElement.isJsonObject()) {
            apiResponse = rootElement.getAsJsonObject();
          } else {
            logger.error("Unexpected JSON format: Root element is not a JSON object.");
          }
        } catch (JsonParseException e) {
          logger.error("Error parsing Matomo API response {}: {}", url, e.getMessage());
        }
      }
    } catch (Exception e) {
      logger.error("Error connecting to Matomo API {}: {}", url, e.getMessage());
    }

    if (apiResponse == null || apiResponse.entrySet().isEmpty()) {
      // Don't break everything if there is just a problem with one provider, return dummy value instead.
      logger.error("Because of errors connecting to Matomo returning empty TimeSeries for providers {}.",
          providers.keySet());
      for (Map.Entry<String, MatomoTimeSeriesStatisticsProvider> entry : providers.entrySet()) {
        String providerId = entry.getKey();
        TimeSeries timeSeries = new TimeSeries(
            new ArrayList<>(Arrays.asList(from.toString())),
            new ArrayList<>(Arrays.asList(0.0)),
            null);
        results.put(providerId, timeSeries);
      }
    } else {
      // Store API response in cache and process response
      logger.debug("Write Matomo API response to cache and process results.");
      resultCache.put(cacheKey, new CacheEntry(apiResponse));
      results = processApiResponse(apiResponse, resourceId, resolution);
    }
    return results;
  }
}
