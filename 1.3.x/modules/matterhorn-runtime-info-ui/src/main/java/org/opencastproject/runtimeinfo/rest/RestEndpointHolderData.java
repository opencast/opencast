/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.runtimeinfo.rest;


import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * Represents a group of endpoints.
 */
public class RestEndpointHolderData {

  /**
   * Name of this group of endpoints.
   */
  private String name;

  /**
   * Title of this group of endpoints to be shown on the documentation page.
   */
  private String title;

  /**
   * List of endpoints in this group.
   */
  private List<RestEndpointData> endpoints;

  /**
   * @param name
   *          name of this endpoint holder
   * @param title
   *          title of this endpoint holder (to be shown on the documentation page)
   * @throws IllegalArgumentException
   *           if name is null, name is not alphanumeric or title is null
   */
  public RestEndpointHolderData(String name, String title) throws IllegalArgumentException {
    if (!DocData.isValidName(name)) {
      throw new IllegalArgumentException("Name must not be null and must be alphanumeric.");
    }
    if (title == null) {
      throw new IllegalArgumentException("Title must not be null.");
    }
    this.name = name;
    this.title = title;
  }

  /**
   * Add an endpoint to this holder and make sure the endpoints are sorted by their names.
   * 
   * @param endpoint
   *          an endpoint to be added to this holder
   */
  public void addEndPoint(RestEndpointData endpoint) {
    if (endpoint != null) {
      if (endpoints == null) {
        endpoints = new Vector<RestEndpointData>();
      }
      endpoints.add(endpoint);
      Collections.sort(endpoints);
    }
  }

  /**
   * Return a string representation of this RestEndpointHolderData object.
   * 
   * @return a string representation of this RestEndpointHolderData object
   */
  @Override
  public String toString() {
    return "HOLD:" + name + ":" + endpoints;
  }

  /**
   * Returns a copy of this RestEndpointHolderData object.
   * 
   * @return a copy of this RestEndpointHolderData object
   */
  public RestEndpointHolderData duplicate() {
    return new RestEndpointHolderData(name, title);
  }

  /**
   * Returns a copy of this RestEndpointHolderData object.
   * 
   * @return a copy of this RestEndpointHolderData object
   * 
   * @throws CloneNotSupportedException
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    return duplicate();
  }

  /**
   * Gets the name of this endpoint holder.
   * 
   * @return the name of this endpoint holder
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the title of this endpoint holder.
   * 
   * @return the title of this endpoint holder
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the list of endpoints in this endpoint holder.
   * 
   * @return the list of endpoints in this endpoint holder
   */
  public List<RestEndpointData> getEndpoints() {
    if (endpoints == null) {
      endpoints = new Vector<RestEndpointData>(0);
    }
    return endpoints;
  }

}
