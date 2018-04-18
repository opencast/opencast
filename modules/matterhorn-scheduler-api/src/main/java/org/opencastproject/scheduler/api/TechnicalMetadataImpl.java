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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An in-memory construct to represent the technical metadata of an scheduled event
 */
public class TechnicalMetadataImpl implements TechnicalMetadata {

  private String eventId;
  private String agentId;
  private Date startDate;
  private Date endDate;
  private boolean optOut;
  private Set<String> presenters = new HashSet<>();
  private Map<String, String> workflowProperties = new HashMap<>();
  private Map<String, String> agentConfig = new HashMap<>();
  private Opt<Recording> recording;

  /**
   * Builds a representation of the technical metadata.
   *
   * @param eventId
   *          the event identifier
   * @param agentId
   *          the agent identifier
   * @param startDate
   *          the start date
   * @param endDate
   *          the end date
   * @param optOut
   *          the opt out status
   * @param presenters
   *          the list of presenters
   * @param workflowProperties
   *          the workflow properties
   * @param agentConfig
   *          the capture agent configuration
   * @param recording
   *          the recording
   */
  public TechnicalMetadataImpl(String eventId, String agentId, Date startDate, Date endDate, boolean optOut,
          Set<String> presenters, Map<String, String> workflowProperties, Map<String, String> agentConfig,
          Opt<Recording> recording) {
    this.eventId = eventId;
    this.agentId = agentId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.optOut = optOut;
    this.presenters = presenters;
    this.workflowProperties = workflowProperties;
    this.agentConfig = agentConfig;
    this.recording = recording;
  }

  @Override
  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  @Override
  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  @Override
  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  @Override
  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  @Override
  public boolean isOptOut() {
    return optOut;
  }

  public void setOptOut(boolean optOut) {
    this.optOut = optOut;
  }

  @Override
  public Set<String> getPresenters() {
    return presenters;
  }

  public void setPresenters(Set<String> presenters) {
    this.presenters = presenters;
  }

  @Override
  public Opt<Recording> getRecording() {
    return recording;
  }

  public void setRecording(Opt<Recording> recording) {
    this.recording = recording;
  }

  @Override
  public Map<String, String> getWorkflowProperties() {
    return workflowProperties;
  }

  public void setWorkflowProperties(Map<String, String> workflowProperties) {
    this.workflowProperties = workflowProperties;
  }

  @Override
  public Map<String, String> getCaptureAgentConfiguration() {
    return agentConfig;
  }

  public void setCaptureAgentConfiguration(Map<String, String> agentConfig) {
    this.agentConfig = agentConfig;
  }

}
