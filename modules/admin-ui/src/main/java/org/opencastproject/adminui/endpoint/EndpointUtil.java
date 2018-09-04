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

package org.opencastproject.adminui.endpoint;

import static java.lang.String.format;

import org.opencastproject.adminui.exception.JsonCreationException;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.index.service.resources.list.query.StringListFilter;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fx;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.SimpleSerializer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

public final class EndpointUtil {
  private static final SimpleSerializer serializer = new SimpleSerializer();

  private EndpointUtil() {
  }

  /**
   * Create a streaming response entity. Pass it as an entity parameter to one of the response builder methods like
   * {@link org.opencastproject.util.RestUtil.R#ok(Object)}.
   */
  public static StreamingOutput stream(final Fx<OutputStream> out) {
    return s -> {
      try (OutputStream bs = new BufferedOutputStream(s)) {
        out.apply(bs);
      }
    };
  }

  public static Response ok(JObject json) {
    return Response.ok(stream(serializer.fn.toJson(json)), MediaType.APPLICATION_JSON_TYPE).build();
  }

  public static Response notFound(String msg, Object... args) {
    return Response.status(Status.NOT_FOUND).entity(format(msg, args)).type(MediaType.TEXT_PLAIN_TYPE).build();
  }

  public static String dateDay(Date date) {
    return EncodingSchemeUtils.formatDate(date, Precision.Day);
  }

  public static final Fn<Date, String> fnDay = new Fn<Date, String>() {
    @Override
    public String apply(Date date) {
      return dateDay(date);
    }
  };

  public static String dateSecond(Date date) {
    return EncodingSchemeUtils.formatDate(date, Precision.Second);
  }

  public static final Fn<Date, String> fnSecond = new Fn<Date, String>() {
    @Override
    public String apply(Date date) {
      return dateSecond(date);
    }
  };

  /**
   * Returns a generated JSON object with key-value from given list.
   *
   * Note that JSONObject (and JSON in general) does not preserve key ordering,
   * so while the Map passed to this function may have ordered keys, the resulting
   * JSONObject is not ordered.
   *
   * @param list
   *          The source list for the JSON object
   * @return a JSON object containing the all the key-value as parameter
   * @throws JsonCreationException
   */
  public static <T> JSONObject generateJSONObject(Map<String, T> list) throws JsonCreationException {

    if (list == null) {
      throw new JsonCreationException("List is null");
    }

    JSONObject jsonList = new JSONObject();

    for (Entry<String, T> entry : list.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof String) {
        jsonList.put(entry.getKey(), value);
      } else if (value instanceof JSONObject) {
        jsonList.put(entry.getKey(), value);
      } else if (value instanceof List) {
        Collection collection = (Collection) value;
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(collection);
        jsonList.put(entry.getKey(), jsonArray);
      } else {
        throw new JsonCreationException("Could not deal with " + value);
      }
    }

    return jsonList;
  }

  /**
   * Add the string based filters to the given list query.
   *
   * @param filterString
   *          The string based filters
   * @param query
   *          The query to update with the filters
   */
  public static void addRequestFiltersToQuery(final String filterString, ResourceListQueryImpl query) {
    if (filterString != null) {
      String[] filters = filterString.split(",");
      for (String filter : filters) {
        String[] splitFilter = filter.split(":", 2);
        if (splitFilter != null && splitFilter.length == 2) {
          String key = splitFilter[0].trim();
          String value = splitFilter[1].trim();
          query.addFilter(new StringListFilter(key, value));
        }
      }
    }
  }
}
