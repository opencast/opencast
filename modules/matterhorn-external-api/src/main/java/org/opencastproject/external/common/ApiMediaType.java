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
package org.opencastproject.external.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ApiMediaType {

  private static final String VERSION_REG_EX_PATTERN = "[v][0-9]+\\.[0-9]+\\.[0-9]+";

  private final ApiVersion version;
  private final ApiFormat format;
  private final String externalForm;

  public static ApiMediaType parse(String mediaType) throws ApiMediaTypeException {
    return new ApiMediaType(extractVersion(mediaType), extractFormat(mediaType), mediaType);
  }

  private static ApiVersion extractVersion(String mediaType) throws ApiMediaTypeException {
    Matcher matcher = Pattern.compile(VERSION_REG_EX_PATTERN).matcher(mediaType);
    if (matcher.find()) {
      String versionPart = mediaType.substring(matcher.start(), matcher.end());
      try {
        return ApiVersion.of(versionPart);
      } catch (Exception e) {
        throw ApiMediaTypeException.invalidVersion(mediaType);
      }
    } else {
      return ApiVersion.VERSION_UNDEFINED;
    }
  }

  private static ApiFormat extractFormat(String mediaType) {
    final String subtype = extractSubtype(mediaType);
    final String format;
    if (subtype.contains("+")) {
      format = subtype.substring(subtype.indexOf("+") + 1);
    } else {
      format = subtype;
    }

    return ApiFormat.valueOf(format.toUpperCase());
  }

  private static String extractSubtype(String mediaType) {
    return mediaType.substring(mediaType.indexOf("/") + 1);
  }

  private ApiMediaType(ApiVersion version, ApiFormat format, String externalForm) {
    this.version = version;
    this.format = format;
    this.externalForm = externalForm;
  }

  public ApiFormat getFormat() {
    return format;
  }

  public ApiVersion getVersion() {
    return version;
  }

  public String toExternalForm() {
    return externalForm;
  }

}
