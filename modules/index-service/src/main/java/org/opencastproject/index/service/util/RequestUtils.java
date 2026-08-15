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

import org.opencastproject.list.api.DefaultResourceListQuery;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ListProvidersService;

import com.google.common.net.MediaType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class RequestUtils {
  public static final String ID_JSON_KEY = "id";
  public static final String VALUE_JSON_KEY = "value";
  public static final String REQUIRED_JSON_KEY = "required";

  private RequestUtils() {
  }

  /**
   * Get a {@link Map} of metadata fields from a JSON array.
   *
   * @param json
   *          The json input.
   * @return A {@link Map} of the metadata fields ids and values.
   * @throws JsonSyntaxException
   *           Thrown if the json is malformed.
   */
  public static Map<String, String> getKeyValueMap(String json) {
    JsonArray updatedFields = JsonParser.parseString(json).getAsJsonArray();
    Map<String, String> fieldMap = new TreeMap<String, String>();
    for (JsonElement element : updatedFields) {
      JsonObject field = element.getAsJsonObject();
      String id = asPlainString(field.get(ID_JSON_KEY), "");
      String value = asPlainString(field.get(VALUE_JSON_KEY), "");
      String requiredStr = asPlainString(field.get(REQUIRED_JSON_KEY), "false");
      boolean required = Boolean.parseBoolean(requiredStr);

      if (StringUtils.trimToNull(id) != null && (StringUtils.trimToNull(value) != null || !required)) {
        fieldMap.put(id, value);
      } else {
        throw new IllegalArgumentException(String.format(
                "One of the metadata fields is missing an id or value. The id was '%s' and the value was '%s'.", id,
                value));
      }
    }
    return fieldMap;
  }

  /**
   * Render a value the way the previous parser did, i.e. the plain text of a primitive rather than its JSON
   * representation, falling back to <code>fallback</code> when the member is absent or null.
   */
  private static String asPlainString(JsonElement value, String fallback) {
    if (value == null || value.isJsonNull()) {
      return fallback;
    }
    return value.isJsonPrimitive() ? value.getAsString() : value.toString();
  }

  /**
   * Check if an uploaded asset conforms to the configured list of accepted file types.
   *
   * @param assetUploadId
   *          The id of the uploaded asset
   * @param mediaType
   *          The media type sent by the browser
   * @param listProvidersService
   *          The ListProviderService to get the configured accepted types from
   *
   * @return true if the given mediatype is accepted, false otherwise.
   */
  public static boolean typeIsAccepted(String fileName, String assetUploadId, MediaType mediaType,
          ListProvidersService listProvidersService) {
    if (mediaType.is(MediaType.OCTET_STREAM)) {
      // No detailed info, so we have to accept...
      return true;
    }

    // get file extension with .
    String fileExtension = null;
    int dot = fileName.lastIndexOf('.');
    if (dot != -1) {
      fileExtension = fileName.substring(dot);
    }

    try {
      final Collection<String> assetUploadJsons = listProvidersService.getList("eventUploadAssetOptions",
          new DefaultResourceListQuery(),false).values();
      for (String assetUploadJson: assetUploadJsons) {
        if (!assetUploadJson.startsWith("{") || !assetUploadJson.endsWith("}")) {
          // Ignore non-json-values
          continue;
        }
        final JsonObject assetUpload = JsonParser.parseString(assetUploadJson).getAsJsonObject();
        if (assetUploadId.equals(asPlainString(assetUpload.get("id"), null))) {
          final List<String> accepts = Arrays.stream(asPlainString(assetUpload.get("accept"), "*/*").split(","))
              .map(String::trim).collect(Collectors.toList());
          for (String accept : accepts) {
            if (accept.contains("/") && mediaType.is(MediaType.parse(accept))) {
              return true;
            } else if (fileExtension != null && accept.contains(".") && fileExtension.equalsIgnoreCase(accept)) {
              return true;
            }
          }
          return false;
        }
      }
    } catch (ListProviderException e) {
      throw new IllegalArgumentException("Invalid assetUploadId: " + assetUploadId);
    } catch (JsonParseException e) {
      throw new IllegalStateException("cannot parse json list provider for asset upload Id " + assetUploadId, e);
    }
    throw new IllegalArgumentException("Invalid assetUploadId: " + assetUploadId);
  }

}
