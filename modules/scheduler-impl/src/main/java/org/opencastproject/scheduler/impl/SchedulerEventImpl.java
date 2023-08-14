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
package org.opencastproject.scheduler.impl;

import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.TechnicalMetadata;

/**
 * An in-memory construct to represent the scheduled event
 */
public class SchedulerEventImpl implements SchedulerEvent {

  private String eventId;
  private String version;
  private MediaPackage mediaPackage;
  private TechnicalMetadata technicalMetadata;

  /**
   * Builds a representation of the technical metadata.
   *
   * @param eventId
   *          the event identifier
   * @param version
   *          the version of the event
   * @param mediaPackage
   *          the mediapackage
   * @param technicalMetadata
   *          the technical metadata
   */
  public SchedulerEventImpl(String eventId, String version, MediaPackage mediaPackage,
          TechnicalMetadata technicalMetadata) {
    notEmpty(eventId, "eventId");
    notEmpty(version, "version");
    notNull(mediaPackage, "mediaPackage");
    notNull(technicalMetadata, "technicalMetadata");
    this.eventId = eventId;
    this.version = version;
    this.mediaPackage = mediaPackage;
    this.technicalMetadata = technicalMetadata;
  }

  @Override
  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    notEmpty(eventId, "eventId");
    this.eventId = eventId;
  }

  @Override
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    notEmpty(version, "version");
    this.version = version;
  }

  @Override
  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  public void setMediaPackage(MediaPackage mediaPackage) {
    notNull(mediaPackage, "mediaPackage");
    this.mediaPackage = mediaPackage;
  }

  @Override
  public TechnicalMetadata getTechnicalMetadata() {
    return technicalMetadata;
  }

  public void setTechnicalMetadata(TechnicalMetadata technicalMetadata) {
    notNull(technicalMetadata, "technicalMetadata");
    this.technicalMetadata = technicalMetadata;
  }

}
