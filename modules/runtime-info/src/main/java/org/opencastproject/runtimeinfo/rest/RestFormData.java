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

package org.opencastproject.runtimeinfo.rest;

import java.util.ArrayList;
import java.util.List;

/**
 * The parameters of a single endpoint, in the order in which they are rendered in the testing form of the REST
 * documentation.
 */
public class RestFormData {

  /**
   * The form parameters.
   */
  private final List<RestParamData> items;

  /**
   * Constructor which will auto-populate the form using the data in the endpoint.
   *
   * @param endpoint
   *          a RestEndpointData object populated with all parameters it needs
   * @throws IllegalArgumentException
   *           when endpoint is null
   */
  public RestFormData(RestEndpointData endpoint) throws IllegalArgumentException {
    if (endpoint == null) {
      throw new IllegalArgumentException("Endpoint must not be null.");
    }
    items = new ArrayList<>(3);
    if (endpoint.getPathParams() != null) {
      for (RestParamData param : endpoint.getPathParams()) {
        param.setRequired(true);
        items.add(param);
      }
    }
    if (endpoint.getRequiredParams() != null) {
      for (RestParamData param : endpoint.getRequiredParams()) {
        param.setRequired(true);
        items.add(param);
      }
    }
    if (endpoint.getOptionalParams() != null) {
      for (RestParamData param : endpoint.getOptionalParams()) {
        param.setRequired(false);
        items.add(param);
      }
    }
    if (endpoint.getBodyParam() != null) {
      RestParamData param = endpoint.getBodyParam();
      param.setRequired(true);
      items.add(param);
    }
  }

  /**
   * Returns a string representation of this form.
   *
   * @return a string representation of this form
   */
  @Override
  public String toString() {
    return "FORM:items=" + items.size();
  }

  /**
   * Returns the list of form parameters.
   *
   * @return a list of form parameters
   */
  public List<RestParamData> getItems() {
    return items;
  }

}
