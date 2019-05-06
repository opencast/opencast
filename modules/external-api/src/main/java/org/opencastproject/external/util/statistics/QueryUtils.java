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

import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.api.StatisticsService;
import org.opencastproject.statistics.api.TimeSeries;
import org.opencastproject.statistics.api.TimeSeriesProvider;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class QueryUtils {

  private QueryUtils() {
  }

  public static class Query {
    private StatisticsProvider provider;
    private Parameters parameters;

    Query(StatisticsProvider provider, Parameters parameters) {
      this.provider = provider;
      this.parameters = parameters;
    }

    public StatisticsProvider getProvider() {
      return provider;
    }

    public Parameters getParameters() {
      return parameters;
    }
  }

  public static class QueryResult {
    private Query query;
    protected JSONObject data;

    QueryResult(Query query) {
      this.query = query;
      this.data = new JSONObject();
    }

    public JSONObject getData() {
      return data;
    }

    public JSONObject get() {
      JSONObject result = new JSONObject();
      // Add provider information
      JSONObject providerObj = new JSONObject();
      providerObj.put("identifier", query.getProvider().getId());
      providerObj.put("type", StatisticsProviderUtils.typeOf(query.getProvider()));
      providerObj.put("resourceType", ResourceTypeUtils.toString(query.getProvider().getResourceType()));
      result.put("provider", providerObj);

      // Add parameter information
      result.put("parameters", query.getParameters().get());

      // Add query result data
      result.put("data", getData());
      return result;
    }
  }

  public static class QueryResultTimeSeries extends QueryResult {

    QueryResultTimeSeries(Query query, TimeSeries timeseries) {
      super(query);
      data.put("labels", timeseries.getLabels());
      data.put("values", timeseries.getValues());
      if (timeseries.getTotal().isPresent()) {
        data.put("total", timeseries.getTotal().getAsDouble());
      }
    }
  }

  public static class Parameters {

    private String resourceId;
    private JSONObject raw;

    Parameters(String resourceId, JSONObject raw) {
      this.resourceId = resourceId;
      this.raw = raw;
    }

    public String getResourceId() {
      return resourceId;
    }

    public JSONObject get() {
      return raw;
    }

    void validate() { }
  }

   public static class TimeSeriesParameters extends Parameters {

    private Instant from;
    private Instant to;
    private DataResolution dataResolution;

    TimeSeriesParameters(String resourceId, JSONObject raw) {
      super(resourceId, raw);
    }

    public Instant getFrom() {
      return from;
    }

    void setFrom(String from) {
      try {
        this.from = Instant.parse(from);
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Parameter 'from' not in ISO 8601 UTC format");
      }
    }

    public Instant getTo() {
      return to;
    }

    void setTo(String to) {
      try {
        this.to = Instant.parse(to);
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Parameter 'to' not in ISO 8601 UTC format");
      }
    }

    public DataResolution getDataResolution() {
      return dataResolution;
    }

    void setDataResolution(String dataResolution) {
      Optional<DataResolution> resolution = DataResolutionUtils.fromString(dataResolution);
      if (!resolution.isPresent()) {
        throw new IllegalArgumentException("Illegal value for 'resolution'");
      }
      this.dataResolution = resolution.get();
    }

    void validate() {
      super.validate();
      if (this.to.compareTo(this.from) <= 0) {
        throw new IllegalArgumentException("'from' date must be before 'to' date");
      }
    }
  }

  public static List<Query> parse(String queryString, StatisticsService statisticsService) {

    if (StringUtils.isBlank(queryString)) {
      throw new IllegalArgumentException("No query data provided");
    }

    JSONParser parser = new JSONParser();
    JSONArray queriesJson;
    try {
      queriesJson = (JSONArray) parser.parse(queryString);
    } catch (ParseException e) {
      throw new IllegalArgumentException("JSON malformed");
    }

    List<Query> queries = new ArrayList<>();

    queriesJson.forEach(item -> {
        Query query = parseQuery((JSONObject) item, statisticsService);
        queries.add(query);
    });

    return queries;
  }

  private static Query parseQuery(JSONObject queryJson, StatisticsService statisticsService) {

    // Get the mandatory provider identifier
    JSONObject providerJson = (JSONObject) queryJson.get("provider");
    String providerId = getField(providerJson, "identifier", "Identifier of provider is missing");

    Optional<StatisticsProvider> provider = statisticsService.getProvider(providerId);
    if (!provider.isPresent()) {
      throw new IllegalArgumentException("Provider not found");
    }

    // Get the query parameters
    JSONObject parametersJson = (JSONObject) queryJson.get("parameters");
    Parameters parameters = parseParameters(parametersJson, provider.get());

    return new Query(provider.get(), parameters);
  }

  public static Parameters parseParameters(JSONObject parametersJson, StatisticsProvider provider) {

    Parameters result;

    String resourceId = getField(parametersJson, "resourceId", "Parameter 'resourceId' is missing");

    // The other parameters are specific to statistics provider implementations
    if (provider instanceof TimeSeriesProvider) {
      TimeSeriesParameters p = new TimeSeriesParameters(resourceId, parametersJson);
      p.setFrom(getField(parametersJson, "from", "Parameter 'from' is missing"));
      p.setTo(getField(parametersJson, "to", "Parameter 'to' is missing"));
      p.setDataResolution(getField(parametersJson, "dataResolution", "Parameter 'dataResolution' is missing"));
      result = p;
    } else {
      result = new Parameters(resourceId, parametersJson);
      // Currently, we don't support other parameter types
    }
    result.validate();
    return result;
  }

  private static String getField(JSONObject object, String field, String exceptionMessage) {
    String value;
    try {
      value = (String) object.get(field);
    } catch (Exception e) {
      throw new IllegalArgumentException(exceptionMessage);
    }
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException(exceptionMessage);
    }
    return value;
  }

  public static JSONObject execute(Query query) {
    QueryResult result;
    StatisticsProvider provider = query.getProvider();
    if (provider instanceof TimeSeriesProvider) {
      TimeSeriesParameters p = (TimeSeriesParameters) query.getParameters();
      TimeSeriesProvider tsp = (TimeSeriesProvider) provider;
      TimeSeries timeseries = tsp.getValues(p.getResourceId(), p.getFrom(), p.getTo(), p.getDataResolution(),
          ZoneId.systemDefault());
      result = new QueryResultTimeSeries(query, timeseries);
    } else {
      result = new QueryResult(query);
    }
    return result.get();
  }

}
