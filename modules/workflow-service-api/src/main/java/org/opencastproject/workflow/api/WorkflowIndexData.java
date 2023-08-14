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

package org.opencastproject.workflow.api;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

/**
 * Summary of the workflow stats necessary for index rebuild,
 * for use in repopulate and update (immutable)
 */
@Table(name = "oc_workflow")
@Entity
@NamedNativeQueries({
        @NamedNativeQuery(
                name = "WorkflowIndexData.getAll",
                query = "SELECT id, state, mediapackage_id, organization_id FROM oc_workflow ORDER BY mediapackage_id, id DESC",
                resultSetMapping = "DataResult"
        ),
})
@SqlResultSetMapping(
        name = "DataResult",
        entities = {
                @EntityResult(
                        entityClass = WorkflowIndexData.class,
                        fields = {
                                  @FieldResult(name = "id",column = "id"),
                                  @FieldResult(name = "state", column = "state"),
                                  @FieldResult(name = "mediaPackageId", column = "mediapackage_id"),
                                  @FieldResult(name = "organizationId", column = "organization_id")
                        }
                )
})


public class WorkflowIndexData {
  @Id
  private Long id;
  private int state;

  @Column(name = "mediapackage_id", length = 128) //NB: This column definition needs to match WorkflowInstance!
  private String mediaPackageId;

  @Column(name = "organization_id") //NB: This column definition needs to match WorkflowInstance!
  private String organizationId;


  /**
   * Default constructor without any import.
   */
  public WorkflowIndexData() {

  }

  public WorkflowIndexData(Long id, int state, String mediaPackageId, String organizationId) {
    this.id = id;
    this.state = state;
    this.mediaPackageId = mediaPackageId;
    this.organizationId = organizationId;
  }

  public Long getId() {
    return id;
  }

  public int getState() {
    return state;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public String getOrganizationId() {
    return organizationId;
  }

}
