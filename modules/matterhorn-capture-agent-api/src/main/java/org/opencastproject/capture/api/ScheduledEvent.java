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

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A representation of the devices associated with a capture agent
 */
@XmlJavaTypeAdapter(ScheduledEventImpl.Adapter.class)
public interface ScheduledEvent {
  
  /**
   * Get the duration of the event in seconds.
   * 
   * @return The duration of the event in seconds.
   */
  Long getDuration();

  /**
   * Get the start time of the scheduled event as a Unix timestamp.
   * 
   * @return The start time of the event as a Unix timestamp.
   */
  Long getStartTime();

  /**
   * Get the title of the scheduled event.
   * 
   * @return The title of the scheduled event.
   */
  String getTitle();
  
}
