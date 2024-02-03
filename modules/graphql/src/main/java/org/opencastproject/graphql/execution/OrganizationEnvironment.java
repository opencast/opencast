/*
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

package org.opencastproject.graphql.execution;

import java.util.Objects;

import graphql.GraphQL;

public class OrganizationEnvironment {

  private final String organizationId;

  private final GraphQL graphQL;

  public OrganizationEnvironment(String organizationId, GraphQL graphQL) {
    this.organizationId = organizationId;
    this.graphQL = graphQL;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public GraphQL getGraphQL() {
    return graphQL;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrganizationEnvironment that = (OrganizationEnvironment) o;
    return Objects.equals(organizationId, that.organizationId) && Objects.equals(graphQL, that.graphQL);
  }

  @Override
  public int hashCode() {
    return Objects.hash(organizationId, graphQL);
  }

}
