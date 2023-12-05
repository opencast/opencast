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

package org.opencastproject.runtimeinfo.rest;

import java.util.ArrayList;
import java.util.List;

public class RestFormData {

  /**
   * This indicates whether the form submission should be an ajax submit or a normal submit.
   */
  private boolean ajaxSubmit = false;

  /**
   * If this is true then the upload contains a body parameter (i.e. a file upload option).
   */
  private boolean fileUpload = false;

  /**
   * This indicates whether this test form is for a basic endpoint which has no parameters.
   */
  private boolean basic = false;

  /**
   * The form parameters.
   */
  private List<RestParamData> items;

  /**
   * Constructor which will auto-populate the form using the data in the endpoint, this will enable the ajax submit if
   * it is possible to do so.
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
    ajaxSubmit = true;
    items = new ArrayList<RestParamData>(3);
    boolean hasUpload = false;
    if (endpoint.getPathParams() != null) {
      for (RestParamData param : endpoint.getPathParams()) {
        param.setRequired(true);
        items.add(param);
      }
    }
    if (endpoint.getRequiredParams() != null) {
      for (RestParamData param : endpoint.getRequiredParams()) {
        param.setRequired(true);
        if (RestParamData.Type.FILE.name().equalsIgnoreCase(param.getType())) {
          hasUpload = true;
        }
        items.add(param);
      }
    }
    if (endpoint.getOptionalParams() != null) {
      for (RestParamData param : endpoint.getOptionalParams()) {
        param.setRequired(false);
        if (RestParamData.Type.FILE.name().equalsIgnoreCase(param.getType())) {
          hasUpload = true;
        }
        items.add(param);
      }
    }
    if (endpoint.getBodyParam() != null) {
      RestParamData param = endpoint.getBodyParam();
      param.setRequired(true);
      if (RestParamData.Type.FILE.name().equalsIgnoreCase(param.getType())) {
        hasUpload = true;
      }
      items.add(param);
    }
    if (hasUpload) {
      fileUpload = true;
      ajaxSubmit = false;
    }
    if (items.isEmpty() && endpoint.isGetMethod()) {
      basic = true;
    }
  }

  /**
   * Returns true if this form has no parameter in it, false if there is parameter in it.
   *
   * @return a boolean indicating whether this form contains any parameter
   */
  public boolean isEmpty() {
    boolean empty = true;
    if (items != null && !items.isEmpty()) {
      empty = false;
    }
    return empty;
  }

  /**
   * Controls whether the form will be submitted via ajax and the content rendered on the page, NOTE that uploading any
   * files or downloading any content that is binary will require not using ajax submit, also note that there may be
   * other cases where ajax submission will fail to work OR where normal submission will fail to work (using PUT/DELETE)
   *
   * @param ajaxSubmit
   *          a boolean indicating whether ajax submit is used
   */
  public void setAjaxSubmit(boolean ajaxSubmit) {
    this.ajaxSubmit = ajaxSubmit;
  }

  /**
   * Set this to true if the file contains a file upload control, this will be determined automatically for
   * auto-generated forms.
   *
   * @param fileUpload
   *          a boolean indicating whether there is file upload in this test
   */
  public void setFileUpload(boolean fileUpload) {
    this.fileUpload = fileUpload;
    if (fileUpload) {
      ajaxSubmit = false;
    }
  }

  /**
   * Returns a string representation of this form.
   *
   * @return a string representation of this form
   */
  @Override
  public String toString() {
    if (items == null) {
      return "FORM:items=0";
    }
    return "FORM:items=" + items.size();
  }

  /**
   * Returns whether the form submission should be an ajax submission or a normal submission.
   *
   * @return a boolean indicating whether the form submission should be an ajax submission or a normal submission
   */
  public boolean isAjaxSubmit() {
    return ajaxSubmit;
  }

  /**
   * Returns whether the form contains a body parameter (i.e. a file upload option).
   *
   * @return a boolean indicating whether the form contains a body parameter (i.e. a file upload option)
   */
  public boolean isFileUpload() {
    return fileUpload;
  }

  /**
   * Returns whether this form is for a basic endpoint which has no parameters.
   *
   * @return a boolean indicating whether this form is for a basic endpoint which has no parameters
   */
  public boolean isBasic() {
    return basic;
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
