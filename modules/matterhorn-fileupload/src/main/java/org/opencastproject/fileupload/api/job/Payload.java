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

/** A class representing the information about the payload of an upload job.
 * 
 */
@XmlType(name = "payload", namespace = "http://workflow.opencastproject.org")
@XmlRootElement(name = "payload", namespace = "http://workflow.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class Payload {

  @XmlElement(name = "filename")
  String filename;                    // name of the uploaded file
  @XmlElement(name = "totalsize")
  long totalsize;                     // size of the file
  @XmlElement(name = "currentsize")
  long currentsize;                   // number of bytes that have already been (successfully) recieved

  public Payload() {
    this.filename = "unknown";
    this.totalsize = -1;
    this.currentsize = 0;
  }

  public Payload(String filename, long size) {
    this.filename = filename;
    this.totalsize = size;
    this.currentsize = 0;
  }

  public String getFilename() {
    return filename;
  }

  public long getTotalSize() {
    return totalsize;
  }
  
  public void setTotalSize(long totalsize) {
    this.totalsize = totalsize;
  }

  public long getCurrentSize() {
    return currentsize;
  }

  public void setCurrentSize(long size) {
    this.currentsize = size;
  }
}
