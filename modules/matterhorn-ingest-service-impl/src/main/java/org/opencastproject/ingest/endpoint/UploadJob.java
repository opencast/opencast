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


package org.opencastproject.ingest.endpoint;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Holds information about a file upload.
 *
 */
@Entity
@Table(name = "mh_upload")
// FIXME @NamedQueries necessary with only one NamedQuery
@NamedQueries({ @NamedQuery(name = "UploadJob.getByID", query = "SELECT o FROM UploadJob o WHERE o.id = :id") })
public class UploadJob {

  @Id
  @Column(name = "id", length = 128)
  private String id;

  @Lob
  @Column(name = "filename", nullable = false, length = 65535)
  private String filename = "";

  @Column(name = "total", nullable = false)
  private long bytesTotal = -1L;

  @Column(name = "received", nullable = false)
  private long bytesReceived = -1L;

  public UploadJob() {
    this.id = UUID.randomUUID().toString();
  }

  public UploadJob(String filename, long bytesTotal) {
    this.id = UUID.randomUUID().toString();
    this.filename = filename;
    this.bytesTotal = bytesTotal;
  }

  /**
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @param filename
   *          the filename to set
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }

  /**
   * @return the bytesTotal
   */
  public long getBytesTotal() {
    return bytesTotal;
  }

  /**
   * @param bytesTotal
   *          the bytesTotal to set
   */
  public void setBytesTotal(long bytesTotal) {
    this.bytesTotal = bytesTotal;
  }

  /**
   * @return the bytesReceived
   */
  public long getBytesReceived() {
    return bytesReceived;
  }

  /**
   * @param bytesReceived
   *          the bytesReceived to set
   */
  public void setBytesReceived(long bytesReceived) {
    this.bytesReceived = bytesReceived;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }
}
