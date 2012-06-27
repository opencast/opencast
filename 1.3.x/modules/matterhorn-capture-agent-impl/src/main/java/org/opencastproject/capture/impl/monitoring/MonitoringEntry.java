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
package org.opencastproject.capture.impl.monitoring;

public class MonitoringEntry {

  public enum MONITORING_TYPE {

    AUDIO, // Audio monitoring device type
    VIDEO, // Video monitoring device type
    AV,    // Mixed monitoring device type
    UNKNOWN // Unknown monitoring device type        
  }
  private String friendlyName = null;
  private MONITORING_TYPE type = MONITORING_TYPE.UNKNOWN;
  private String location = null;

  public MonitoringEntry(String friendlyName, MONITORING_TYPE type, String location) {
    this.friendlyName = friendlyName;
    this.type = type;
    this.location = location;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  public String getLocation() {
    return location;
  }

  public MONITORING_TYPE getType() {
    return type;
  }
}
