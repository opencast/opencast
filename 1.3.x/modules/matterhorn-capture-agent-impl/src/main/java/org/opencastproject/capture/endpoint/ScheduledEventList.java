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
package org.opencastproject.capture.endpoint;

import org.opencastproject.capture.api.ScheduledEvent;
import org.opencastproject.capture.api.ScheduledEventImpl;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/** ScheduledEventList is used by CaptureRestService to deliver the list of ScheduledEvents 
 * to the rest endpoint. **/
@XmlRootElement(name = "events")
public class ScheduledEventList {

  @XmlElement(name = "event")
  protected List<ScheduledEventImpl> events;

  /** Create a blank list. **/
  public ScheduledEventList() {
    this.events = new LinkedList<ScheduledEventImpl>();
  }

  /**
   * Pass in a list of the api ScheduledEvent objects
   * 
   * @param eventLists
   *          A list of the interface class ScheduledEvent that we will change each instance to ScheduledEventImpl.
   * **/
  public ScheduledEventList(List<ScheduledEvent> eventList) {
    this.events = new LinkedList<ScheduledEventImpl>();
    this.setEvents(eventList);
  }

  /** Take a list of api ScheduledEvents and turn them into ScheduledEventImpls**/
  public void setEvents(List<ScheduledEvent> eventList) {
    if (eventList == null) {
      return;
    }
    if (!this.events.isEmpty()) {
      this.events.clear();
    }
    for (ScheduledEvent e : eventList) {
      this.events.add((ScheduledEventImpl) e);
    }
  }
}
