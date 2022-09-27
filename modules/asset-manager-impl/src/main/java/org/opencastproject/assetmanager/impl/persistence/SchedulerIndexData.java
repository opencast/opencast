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

package org.opencastproject.assetmanager.impl.persistence;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Summary of the scheduler stats necessary for index rebuild, for use in repopulate
 */
@Entity
@NamedNativeQueries({
    @NamedNativeQuery(
          name = "SchedulerIndexData.getAll",
          query = "SELECT mediapackage_id, capture_agent_id, recording_state, presenters, start_date, end_date "
                  + "FROM oc_scheduled_extended_event",
          resultSetMapping = "DataResult"
    ),
})
@SqlResultSetMapping(
        name = "DataResult",
        entities = {
                @EntityResult(
                        entityClass = SchedulerIndexData.class,
                        fields = {
                                  @FieldResult(name = "mediaPackageId", column = "mediapackage_id"),
                                  @FieldResult(name = "captureAgentId", column = "capture_agent_id"),
                                  @FieldResult(name = "recordingState", column = "recording_state"),
                                  @FieldResult(name = "presenters", column = "presenters"),
                                  @FieldResult(name = "startDate", column = "start_date"),
                                  @FieldResult(name = "endDate", column = "end_date")
                        }
                )
})


public class SchedulerIndexData {

  @Id
  private String mediaPackageId;
  private String captureAgentId;
  private String recordingState;
  private String presenters;

  @Temporal(TemporalType.TIMESTAMP)
  private Date startDate;
  @Temporal(TemporalType.TIMESTAMP)
  private Date endDate;

  /**
   * Default constructor without any import.
   */
  public SchedulerIndexData() {
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public String getCaptureAgentId() {
    return captureAgentId;
  }

  public Date getStartDate() {
    return startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public String getRecordingState() {
    return recordingState;
  }

  public String getPresenters() {
    return presenters;
  }
}
