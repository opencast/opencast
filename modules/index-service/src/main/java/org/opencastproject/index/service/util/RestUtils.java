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

package org.opencastproject.index.service.util;

import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.requests.SortCriterion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Utils method for the Rest Endpoint implementation
 */
public final class RestUtils {
  private static final Logger logger = LoggerFactory.getLogger(RestUtils.class);

  private RestUtils() {
  }

  /**
   * Create an OK (200) response with the given JSON as body
   *
   * @param json
   *          the JSON string to add to the response body.
   * @return an OK response
   */
  public static Response okJson(JsonElement json) {
    return Response.ok(json.toString(), MediaType.APPLICATION_JSON_TYPE).build();
  }

  /**
   * Create an CONFLICT (409) response with the given JSON as body
   *
   * @param json
   *          the JSON string to add to the response body.
   * @return an OK response
   */
  public static Response conflictJson(JsonElement json) {
    return Response.status(Status.CONFLICT)
        .entity(json.toString())
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
  }

  /**
   * Create a NOT FOUND (404) response with the given messages and arguments
   *
   * @param msg
   * @param args
   * @return a NOT FOUND response
   */
  public static Response notFound(String msg, Object... args) {
    return Response.status(Status.NOT_FOUND)
        .entity(String.format(msg, args))
        .type(MediaType.TEXT_PLAIN_TYPE)
        .build();
  }

  /**
   * Create a NOT FOUND (404) response with the given JSON as body
   *
   * @param json
   *          the JSON string to add to the response body.
   * @return a NOT FOUND response
   */
  public static Response notFoundJson(JsonElement json) {
    return Response.status(Status.NOT_FOUND)
        .entity(json.toString())
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
  }

  /**
   * Create an INTERNAL SERVER ERROR (500) response with the given JSON as body
   *
   * @param json
   *          the JSON string to add to the response body.
   * @return an INTERNAL SERVER ERROR response
   */
  public static Response serverErrorJson(JsonElement json) {
    return Response.status(Status.INTERNAL_SERVER_ERROR)
        .entity(json.toString())
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
  }

  /**
   * Return the given list of value with the standard format for JSON list value with offset, limit and total
   * information. See also
   * {@link org.opencastproject.index.service.util.RestUtils#okJsonList(List, Optional, Optional, long)}.
   *
   * @param jsonList
   *          The list of value to return
   * @param offset
   *          The result offset
   * @param limit
   *          The maximal list size
   * @param total
   *          The amount of available items in the system
   * @return a {@link Response} with an JSON object formatted like above as body.
   * @throws IllegalArgumentException
   *           if the value list is null
   */
  public static Response okJsonList(List<JsonObject> jsonList, int offset, int limit, long total) {
    return okJsonList(jsonList, Optional.of(offset), Optional.of(limit), total);
  }

  /**
   * Return the given list of value with the standard format for JSON list value with offset, limit and total
   * information. The JSON object in the response body has the following format:
   *
   * <pre>
   * {
   *  results: [
   *    // array containing all the object from the given list
   *  ],
   *  count: 12, // The number of item returned (size of the given list)
   *  offset: 2, // The result offset (given parameter)
   *  limit: 12, // The maximal size of the list (given parameter)
   *  total: 123 // The total number of items available in the system (given parameter)
   * }
   * </pre>
   *
   * Limit and offset are optional.
   *
   * @param jsonList
   *          The list of value to return
   * @param optOffset
   *          The result offset (optional)
   * @param optLimit
   *          The maximal list size (optional)
   * @param total
   *          The amount of available items in the system
   * @return a {@link Response} with an JSON object formatted like above as body.
   * @throws IllegalArgumentException
   *           if the value list is null
   */
  public static Response okJsonList(List<JsonObject> jsonList, Optional<Integer> optOffset, Optional<Integer> optLimit,
      long total) {
    if (jsonList == null) {
      throw new IllegalArgumentException("The list of values must not be null.");
    }

    JsonObject responseJson = new JsonObject();

    JsonArray resultsArray = new JsonArray();
    for (JsonObject obj : jsonList) {
      resultsArray.add(obj);
    }
    responseJson.add("results", resultsArray);
    responseJson.addProperty("count", jsonList.size());
    responseJson.addProperty("total", total);

    if (optOffset.isPresent()) {
      responseJson.addProperty("offset", optOffset.get());
    }
    if (optLimit.isPresent()) {
      responseJson.addProperty("limit", optLimit.get());
    }

    return okJson(responseJson);
  }

  /**
   * Parse a sort query parameter to a set of {@link SortCriterion}. The parameter has to be of the following form:
   * {@code <field name>:ASC|DESC}
   *
   * @param sort
   *          the parameter string to parse (will be checked if blank)
   * @return a set of sort criterion, never {@code null}
   */
  public static ArrayList<SortCriterion> parseSortQueryParameter(String sort) throws WebApplicationException {
    ArrayList<SortCriterion> sortOrders = new ArrayList<>();

    if (StringUtils.isNotBlank(sort)) {
      StringTokenizer tokenizer = new StringTokenizer(sort, ",");
      while (tokenizer.hasMoreTokens()) {
        try {
          sortOrders.add(SortCriterion.parse(tokenizer.nextToken()));
        } catch (IllegalArgumentException e) {
          throw new WebApplicationException(Status.BAD_REQUEST);
        }
      }
    }

    return sortOrders;
  }

  /**
   * Parse the UTC format date range string to two Date objects to represent a range of dates.
   * <p>
   * Sample UTC date range format string:<br>
   * i.e. yyyy-MM-ddTHH:mm:ssZ/yyyy-MM-ddTHH:mm:ssZ e.g. 2014-09-27T16:25Z/2014-09-27T17:55Z
   * </p>
   *
   * @param fromToDateRange
   *          The string that represents the UTC formed date range.
   * @return A Tuple with the two Dates
   * @throws IllegalArgumentException
   *           Thrown if the input string is malformed
   */
  public static Tuple<Date, Date> getFromAndToDateRange(String fromToDateRange) {
    String[] dates = fromToDateRange.split("/");
    if (dates.length != 2) {
      logger.warn("The date range '{}' is malformed", fromToDateRange);
      throw new IllegalArgumentException("The date range string is malformed");
    }

    Date fromDate = null;
    try {
      fromDate = new Date(DateTimeSupport.fromUTC(dates[0]));
    } catch (Exception e) {
      logger.warn("Unable to parse from date parameter '{}'", dates[0]);
      throw new IllegalArgumentException("Unable to parse from date parameter");
    }

    Date toDate = null;
    try {
      toDate = new Date(DateTimeSupport.fromUTC(dates[1]));
    } catch (Exception e) {
      logger.warn("Unable to parse to date parameter '{}'", dates[1]);
      throw new IllegalArgumentException("Unable to parse to date parameter");
    }

    return new Tuple<Date, Date>(fromDate, toDate);
  }

  /**
   * Parse the filter to a {@link Map}
   *
   * @param filter
   *          the filters
   * @return the map of filter name and values
   */
  public static Map<String, String> parseFilter(String filter) {
    Map<String, String> filters = new HashMap<>();
    if (StringUtils.isNotBlank(filter)) {
      for (String f : filter.split(",")) {
        String[] filterTuple = f.split(":");
        if (filterTuple.length < 2) {
          logger.debug("No value for filter '{}' in filters list: {}", filterTuple[0], filter);
          continue;
        }
        // use substring because dates also contain : so there might be more than two parts
        filters.put(filterTuple[0].trim(), URLDecoder.decode(f.substring(filterTuple[0].length() + 1).trim(),
            StandardCharsets.UTF_8));
      }
    }
    return filters;
  }

  public static String getJsonString(Object jsonObject) {
    return JSONValue.toJSONString(jsonObject);
  }

  /**
   * Get a {@link String} value from a {@link JsonArray} ignoring errors.
   *
   * @param json
   *          The {@link JsonArray} to convert to a {@link String}
   * @return The {@link String} representation of the {@link JsonArray} or an empty string if there was an error.
   */
  public static String getJsonStringSilent(JsonArray json) {
    try {
      return json.toString();
    } catch (Exception e) {
      return "";
    }
  }


}
