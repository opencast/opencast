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

package org.opencastproject.index.service.util;

import static com.entwinemedia.fn.data.json.Jsons.a;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.j;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static java.lang.String.format;

import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.matterhorn.search.impl.SortCriterionImpl;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.data.Tuple;

import com.entwinemedia.fn.Fx;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.SimpleSerializer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

/**
 * Utils method for the Rest Endpoint implementation
 */
public final class RestUtils {
  private static final Logger logger = LoggerFactory.getLogger(RestUtils.class);

  private static final SimpleSerializer serializer = new SimpleSerializer();

  private RestUtils() {
  }

  /**
   * Create an OK (200) response with the given JSON as body
   *
   * @param json
   *          the JSON string to add to the response body.
   * @return an OK response
   */
  public static Response okJson(JValue json) {
    return Response.ok(stream(serializer.toJsonFx(json)), MediaType.APPLICATION_JSON_TYPE).build();
  }

  /**
   * Create an CONFLICT (409) response with the given JSON as body
   *
   * @param json
   *          the JSON string to add to the response body.
   * @return an OK response
   */
  public static Response conflictJson(JValue json) {
    return Response.status(Status.CONFLICT).entity(stream(serializer.toJsonFx(json)))
            .type(MediaType.APPLICATION_JSON_TYPE).build();
  }

  /**
   * Create a NOT FOUND (404) response with the given messages and arguments
   *
   * @param msg
   * @param args
   * @return a NOT FOUND response
   */
  public static Response notFound(String msg, Object... args) {
    return Response.status(Status.NOT_FOUND).entity(format(msg, args)).type(MediaType.TEXT_PLAIN_TYPE).build();
  }

  /**
   * Create a INTERNAL SERVER ERROR (500) response with the given messages and arguments
   *
   * @param msg
   * @param args
   * @return an INTERNAL SERVER ERROR response
   */
  public static Response serverError(String msg, Object... args) {
    return Response.status(Status.INTERNAL_SERVER_ERROR).entity(format(msg, args)).type(MediaType.TEXT_PLAIN_TYPE)
            .build();
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
  public static Response okJsonList(List<JValue> jsonList, int offset, int limit, long total) {
    if (jsonList == null)
      throw new IllegalArgumentException("The list of value must not be null.");
    JValue respone = j(f("results", a(jsonList)), f("count", v(jsonList.size())), f("offset", v(offset)),
            f("limit", v(limit)), f("total", v(total)));

    return okJson(respone);
  }

  /**
   * Create a streaming response entity. Pass it as an entity parameter to one of the response builder methods like
   * {@link org.opencastproject.util.RestUtil.R#ok(Object)}.
   */
  public static StreamingOutput stream(final Fx<Writer> out) {
    return new StreamingOutput() {
      @Override
      public void write(OutputStream s) throws IOException, WebApplicationException {
        final Writer writer = new BufferedWriter(new OutputStreamWriter(s));
        out.ap(writer);
        writer.flush();
      }
    };
  }

  /**
   * Parse a sort query parameter to a set of {@link SortCriterion}. The parameter has to be of the following form:
   * {@code <field name>:ASC|DESC}
   *
   * @param sort
   *          the parameter string to parse
   * @return a set of sort criterion, never {@code null}
   */
  public static Set<SortCriterion> parseSortQueryParameter(String sort) throws WebApplicationException {
    Set<SortCriterion> sortOrders = new HashSet<SortCriterion>();

    StringTokenizer tokenizer = new StringTokenizer(sort, ",");
    while (tokenizer.hasMoreTokens()) {
      try {
        sortOrders.add(SortCriterionImpl.parse(tokenizer.nextToken()));
      } catch (IllegalArgumentException e) {
        throw new WebApplicationException(Status.BAD_REQUEST);
      }
    }

    return sortOrders;
  }

  /**
   * Parse the UTC format date range string to two Date objects to represent a range of dates.
   * <p>
   * Sample UTC date range format string:<br />
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
          logger.info("No value for filter {} in filters list: {}", filterTuple[0], filter);
          continue;
        }
        filters.put(filterTuple[0], f.substring(filterTuple[0].length() + 1));
      }
    }
    return filters;
  }

  public static String getJsonString(JValue json) throws WebApplicationException, IOException {
    OutputStream output = new OutputStream() {
      private StringBuilder string = new StringBuilder();

      @Override
      public void write(int b) throws IOException {
        this.string.append((char) b);
      }

      @Override
      public String toString() {
        return this.string.toString();
      }
    };

    stream(serializer.toJsonFx(json)).write(output);

    return output.toString();
  }
}
