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

package org.opencastproject.series.api;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;

import java.util.Date;


/**
 * An Opencast series.
 */
public class Series {
  /** The ID of this series */
  private String id;

  /** The organization this series belongs to */
  private String organization;

  /** Serialized dublin core metadata catalogue */
  private DublinCoreCatalog dublinCore;

  /** Serialized access control lists */
  private String accessControl;

  /** Date of the last time anything about this series was modified */
  private Date modifiedDate;

  /**
   * Date of the last time this series was deleted, or {@code null} if it is not currently deleted.
   */
  private Date deletionDate;


  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOrganization() {
    return this.organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public DublinCoreCatalog getDublinCore() {
    return this.dublinCore;
  }

  public void setDublinCore(DublinCoreCatalog dublinCore) {
    this.dublinCore = dublinCore;
  }

  public String getAccessControl() {
    return this.accessControl;
  }

  public void setAccessControl(String accessControl) {
    this.accessControl = accessControl;
  }

  public Date getModifiedDate() {
    return this.modifiedDate;
  }

  public void setModifiedDate(Date modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  public Date getDeletionDate() {
    return this.deletionDate;
  }

  public void setDeletionDate(Date deletionDate) {
    this.deletionDate = deletionDate;
  }


  /** Returns whether or not this series is currently deleted. */
  public boolean isDeleted() {
    return deletionDate != null;
  }
}
