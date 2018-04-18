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
package org.opencastproject.scheduler.impl.persistence;

import org.opencastproject.util.EqualsUtil;

import java.io.Serializable;

public class EventIdPK implements Serializable {

  /**
   * Serial UUID
   */
  private static final long serialVersionUID = 5277574531617973229L;

  private String mediaPackageId;
  private String organization;

  public EventIdPK() {
  }

  public EventIdPK(String mediaPackageId, String organization) {
    this.mediaPackageId = mediaPackageId;
    this.organization = organization;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public String getOrganization() {
    return organization;
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(mediaPackageId, organization);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (!(obj instanceof EventIdPK))
      return false;
    EventIdPK pk = (EventIdPK) obj;
    return EqualsUtil.eq(pk.getMediaPackageId(), mediaPackageId) && EqualsUtil.eq(pk.getOrganization(), organization);
  }

}
