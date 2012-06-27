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
package org.opencastproject.deliver.itunesu;

/**
 * The class representation of iTunes tracks.
 */
public class ITunesTrack 
{
  /** name */
  private String name;
  /** handle */
  private String handle;
  /** song/feature-movie */
  private String kind;
  /** duration in milliseconds */
  private int duration;

  /** constructor */
  public ITunesTrack(String name, String handle, String kind, int duration)
  {
    this.name = name;
    this.handle = handle;
    this.kind = kind;
    this.duration = duration;
  }

  // **** Accessors

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHandle() {
    return handle;
  }

  public void setHandle(String handle) {
    this.handle = handle;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  // **** String Representation

  @Override
  public String toString() {
    return "ITunesTrack{" +
           "name='" + name + '\'' +
           ", handle='" + handle + '\'' +
           ", kind='" + kind + '\'' +
           ", duration=" + duration +
           "}";
  }
}
