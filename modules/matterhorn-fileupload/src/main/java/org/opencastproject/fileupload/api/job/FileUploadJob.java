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

import java.util.UUID;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/** A Class representing the information about an upload job.
 * 
 */
@XmlType(name = "uploadjob", namespace = "http://workflow.opencastproject.org")
@XmlRootElement(name = "uploadjob", namespace = "http://workflow.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class FileUploadJob {

  @XmlEnum
  public enum JobState {          // states an upload job can be in
    @XmlEnumValue("READY")
    READY,
    @XmlEnumValue("INPROGRESS")
    INPROGRESS,
    @XmlEnumValue("COMPLETE")
    COMPLETE
  }
  
  @XmlAttribute()
  private String id;                        // this jobs identifier
  @XmlAttribute()
  private JobState state = JobState.READY;  // this jobs state
  @XmlElement(name = "payaload")
  private Payload payload;                  // information about this jobs payload
  @XmlElement(name = "chunksize")
  private int chunksize = -1;               // size of the chunks that are tranfered
  @XmlElement(name = "chunks-total")
  private long chunksTotal = 1;             // total number of chunks the upload consists of
  @XmlElement(name = "current-chunk")
  private Chunk currentChunk = new Chunk(); // information about the current chunk

  public FileUploadJob() {
    this.id = UUID.randomUUID().toString();
    this.payload = new Payload("unknown", -1);
  }

  public FileUploadJob(String filename, long filesize, int chunksize) {
    this.id = UUID.randomUUID().toString();
    this.chunksize = chunksize;
    if (chunksize != -1) {
      chunksTotal = filesize / chunksize;
      if (filesize % chunksize != 0) {
        chunksTotal++;
      }
    }
    this.payload = new Payload(filename, filesize);
  }

  public String getId() {
    return id;
  }

  public JobState getState() {
    return this.state;
  }

  public void setState(JobState state) {
    this.state = state;
  }

  public Payload getPayload() {
    return this.payload;
  }

  public int getChunksize() {
    return chunksize;
  }

  public long getChunksTotal() {
    return chunksTotal;
  }

  public Chunk getCurrentChunk() {
    return currentChunk;
  }

  public void setCurrentChunk(Chunk currentChunk) {
    this.currentChunk = currentChunk;
  }
}
