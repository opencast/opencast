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
package org.opencastproject.scheduler.impl.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Entity object for storing events in persistence storage. Event ID is stored as primary key, DUBLIN_CORE field is used
 * to store serialized Dublin core and 'CA_METADATA' is used to store capture agent specific metadata.
 * 
 */
@Entity(name = "EventEntity")
@NamedQueries({
  // Job queries
  @NamedQuery(name = "Event.findAll", query = "SELECT e FROM EventEntity e ")
})
@Table(name = "mh_scheduled_event")
public class EventEntity {

  /** Event ID, primary key */
  @Id
  @Column(name = "id")
  protected Long eventId;

  /** Serialized Dublin core */
  @Lob
  @Column(name = "dublin_core", length = 65535)
  protected String dublinCoreXML;

  /** Serialized Capture agent metadata */
  @Lob
  @Column(name = "capture_agent_metadata", length = 65535)
  protected String captureAgentMetadata;

  /**
   * Default constructor without any import.
   */
  public EventEntity() {
  }

  /**
   * Returns event ID.
   * 
   * @return event ID
   */
  public Long getEventId() {
    return eventId;
  }

  /**
   * Sets event ID.
   * 
   * @param eventId
   */
  public void setEventId(Long eventId) {
    this.eventId = eventId;
  }

  /**
   * Returns serialized Dublin core.
   * 
   * @return serialized Dublin core
   */
  public String getEventDublinCore() {
    return dublinCoreXML;
  }

  /**
   * Sets serialized Dublin core.
   * 
   * @param dublinCoreXML
   *          serialized Dublin core
   */
  public void setEventDublinCore(String dublinCoreXML) {
    this.dublinCoreXML = dublinCoreXML;
  }

  /**
   * Returns serialized capture agent metadata
   * 
   * @return serialized metadata
   */
  public String getCaptureAgentMetadata() {
    return captureAgentMetadata;
  }

  /**
   * Sets serialized capture agent metadata
   * 
   * @param captureAgentMetadata
   *          serialized metadata
   */
  public void setCaptureAgentMetadata(String captureAgentMetadata) {
    this.captureAgentMetadata = captureAgentMetadata;
  }
}
