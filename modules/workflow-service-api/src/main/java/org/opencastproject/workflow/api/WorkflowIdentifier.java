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

import java.util.Objects;

/**
 * Represents a workflow's "primary key", for use in maps and sets (immutable)
 */
public final class WorkflowIdentifier {
  private final String id;
  private final String organization;

  public WorkflowIdentifier(String id, String organization) {
    this.id = id;
    this.organization = organization;
  }

  public String getId() {
    return id;
  }

  public String getOrganization() {
    return organization;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    WorkflowIdentifier that = (WorkflowIdentifier) o;
    return id.equals(that.id) && Objects.equals(organization, that.organization);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, organization);
  }

  public String toString() {
    if (organization == null) {
      return id;
    }
    return String.format("%s/%s", organization, id);
  }

  /**
   * Return a new identifier without an organization (idempotent)
   * @return See above
   */
  public WorkflowIdentifier withoutOrganization() {
    return new WorkflowIdentifier(id, null);
  }
}
