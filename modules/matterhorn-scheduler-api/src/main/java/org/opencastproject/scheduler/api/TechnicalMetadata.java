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
package org.opencastproject.scheduler.api;

import com.entwinemedia.fn.data.Opt;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Defines the technical metadata of an scheduled event
 */
public interface TechnicalMetadata {

  /**
   * Returns the event identifier
   *
   * @return the event identifier
   */
  String getEventId();

  /**
   * Returns the agent identifier
   *
   * @return the agent identifier
   */
  String getAgentId();

  /**
   * Returns the start date
   *
   * @return the start date
   */
  Date getStartDate();

  /**
   * Returns the end date
   *
   * @return the end date
   */
  Date getEndDate();

  /**
   * Returns the opt out status
   *
   * @return the opt out status
   */
  boolean isOptOut();

  /**
   * Returns the list of presenters
   *
   * @return the list of presenters
   */
  Set<String> getPresenters();

  /**
   * Returns the optional recording
   *
   * @return the optional recording
   */
  Opt<Recording> getRecording();

  /**
   * Returns the workflow properties
   *
   * @return the workflow properties
   */
  Map<String, String> getWorkflowProperties();

  /**
   * Returns the capture agent configuration
   *
   * @return the capture agent configuration
   */
  Map<String, String> getCaptureAgentConfiguration();

}
