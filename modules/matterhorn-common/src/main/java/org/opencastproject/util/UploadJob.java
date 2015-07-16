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
package org.opencastproject.util;

import java.util.UUID;

/**
 * Holds information about a file upload.
 */
public class UploadJob {

  private String id;

  private String filename = "";

  private long bytesTotal = -1L;

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
