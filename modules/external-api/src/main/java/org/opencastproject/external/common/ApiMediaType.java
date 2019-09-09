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

public final class ApiMediaType {

  public static final String VERSION_1_3_0 = "application/v1.3.0+json";
  public static final String VERSION_1_2_0 = "application/v1.2.0+json";
  public static final String VERSION_1_1_0 = "application/v1.1.0+json";
  public static final String VERSION_1_0_0 = "application/v1.0.0+json";
  public static final String JSON = "application/json";

  private static final String APPLICATION_ANY = "application/*";
  private static final String ANY = "*/*";

  private final ApiVersion version;
  private final ApiFormat format;
  private final String externalForm;

  private ApiMediaType(ApiVersion version, ApiFormat format, String externalForm) {
    this.version = version;
    this.format = format;
    this.externalForm = externalForm;
  }

  public static ApiMediaType parse(String acceptHeader) throws ApiMediaTypeException {
    /* MH-12802: The External API does not support content negotiation */
    ApiMediaType mediaType;
    if (acceptHeader == null || acceptHeader.contains(VERSION_1_3_0) || acceptHeader.contains(JSON)
    || acceptHeader.contains(APPLICATION_ANY) || acceptHeader.contains(ANY)) {
      mediaType = new ApiMediaType(ApiVersion.VERSION_1_3_0, ApiFormat.JSON, VERSION_1_3_0);
    } else if (acceptHeader.contains(VERSION_1_2_0)) {
      mediaType = new ApiMediaType(ApiVersion.VERSION_1_2_0, ApiFormat.JSON, VERSION_1_2_0);
    } else if (acceptHeader.contains(VERSION_1_1_0)) {
      mediaType = new ApiMediaType(ApiVersion.VERSION_1_1_0, ApiFormat.JSON, VERSION_1_1_0);
    } else if (acceptHeader.contains(VERSION_1_0_0)) {
      mediaType = new ApiMediaType(ApiVersion.VERSION_1_0_0, ApiFormat.JSON, VERSION_1_0_0);
    } else {
      throw ApiMediaTypeException.invalidVersion(acceptHeader);
    }
    return mediaType;
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
