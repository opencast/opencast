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

package org.opencastproject.archive.base.persistence;

/**
 * IdClass for EpisodeDto
 */
class EpisodeDtoId {
  private String mediaPackageId;
  private long version;

  EpisodeDtoId(String mediaPackageId, long version) {
    this.mediaPackageId = mediaPackageId;
    this.version = version;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public void setMediaPackageId(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }
}
