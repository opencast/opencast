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
package org.opencastproject.adminui.impl;

/**
 * The provider query as it appears in the JSON document, to be parsed by GSON.
 *
 * Things like dates and enums are not validated in this class. This is done by converting it into a
 * {@link ProviderQuery}.
 */
public final class RawProviderQuery {
  private String providerId;
  private String from;
  private String to;
  private String resourceId;
  private String dataResolution;

  // Needed for gson deserialization
  public RawProviderQuery() {
  }

  public String getProviderId() {
    return providerId;
  }

  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getDataResolution() {
    return dataResolution;
  }
}
