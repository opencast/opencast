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
package org.opencastproject.capture.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A representation of the devices associated with a capture agent
 */
@XmlType(name = "scheduled-event", namespace = "http://capture.opencastproject.org")
@XmlRootElement(name = "scheduled-event", namespace = "http://capture.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class ScheduledEventImpl implements ScheduledEvent {

  /**
   * The event's title
   */
  @XmlElement(name = "title")
  private String title;

  /**
   * The event's start time as a Unix timestamp
   */
  @XmlElement(name = "start")
  private Long start;

  /**
   * The event's duration in seconds
   */
  @XmlElement(name = "duration")
  private Long duration;

  public ScheduledEventImpl() {
  }

  /**
   * Builds a scheduled event which has JAXB annotations for easy transfer over the wire.
   * 
   * @param title
   *          The title of the event
   * @param start
   *          The start time of the event as a Unix timestamp
   * @param duration
   *          The duration of the event in seconds
   */
  public ScheduledEventImpl(String title, Long start, Long duration) {
    this.title = title;
    this.start = start;
    this.duration = duration;
  }

  /**
   * Get the duration of the event in seconds.
   * 
   * @return The duration of the event in seconds.
   */
  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
    this.duration = duration;
  }

  /**
   * Get the start time of the scheduled event as a Unix timestamp.
   * 
   * @return The start time of the event as a Unix timestamp.
   */
  public Long getStartTime() {
    return start;
  }

  public void setStartTime(Long start) {
    this.start = start;
  }

  /**
   * Get the title of the scheduled event.
   * 
   * @return The title of the scheduled event.
   */
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  static class Adapter extends XmlAdapter<ScheduledEventImpl, ScheduledEvent> {
    public ScheduledEventImpl marshal(ScheduledEvent op) throws Exception {
      return (ScheduledEventImpl) op;
    }

    public ScheduledEvent unmarshal(ScheduledEventImpl op) throws Exception {
      return op;
    }
  }

}
