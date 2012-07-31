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
package org.opencastproject.fileupload.api.job;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A class representing the information about the current chunk in an upload job.
 * 
 */
@XmlType(name = "chunk", namespace = "http://fileupload.opencastproject.org")
@XmlRootElement(name = "chunk", namespace = "http://fileupload.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class Chunk {

  @XmlElement(name = "number")
  private int number = -1; // number of the current chunk
  @XmlElement(name = "bytes-recieved")
  private long recieved = 0; // number of bytes of the current chunk that have already been recieved

  public Chunk() {
  }

  public Chunk(int number, long recieved) {
    this.number = number;
    this.recieved = recieved;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public int incrementNumber() {
    return ++number;
  }

  public long getRecieved() {
    return recieved;
  }

  public void setRecieved(long recieved) {
    this.recieved = recieved;
  }
}
