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
package org.opencastproject.engage.theodul.api;

/**
 * Interface for the EngagePluginRegistration
 * Describes and engage plugin and stores its values
 */
public interface EngagePluginRegistration {

  /**
   * Returns the description of the engage plugin
   * @return description
   */
  String getDescription();

  /**
   * Returns the ID of the engage plugin
   * @return ID
   */
  int getId();

  /**
   * Returns the name of the engage plugin
   * @return name
   */
  String getName();

  /**
   * Returns the REST path of the engage plugin
   * @return REST path
   */
  String getRestPath();

  /**
   * Returns the static resource path of the engage plugin
   * @return static resource path
   */
  String getStaticPath();

  /**
   * Returns whether this engage plugin has a REST endpoint
   * @return true if this plugin has a REST endpoint
   */
  boolean hasRestEndpoint();

  /**
   * Returns whether this engage plugin has a static resource path
   * @return true if this plugin has a static resource path, false else
   */
  boolean hasStaticResources();

}
