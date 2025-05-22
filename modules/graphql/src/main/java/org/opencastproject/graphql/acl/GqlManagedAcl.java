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

package org.opencastproject.graphql.acl;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.graphql.type.output.GqlAccessControlList;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLID;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

@GraphQLName(GqlManagedAcl.TYPE_NAME)
public class GqlManagedAcl {

  public static final String TYPE_NAME = "ManagedAcl";

  private final ManagedAcl managedAcl;

  public GqlManagedAcl(ManagedAcl managedAcl) {
    this.managedAcl = managedAcl;
  }

  @GraphQLID
  @GraphQLField
  @GraphQLNonNull
  public Long id() {
    return managedAcl.getId();
  }

  @GraphQLField
  @GraphQLNonNull
  public String name() {
    return managedAcl.getName();
  }

  @GraphQLField
  @GraphQLNonNull
  public String organizationId() {
    return managedAcl.getOrganizationId();
  }

  @GraphQLField
  @GraphQLNonNull
  public GqlAccessControlList acl() {
    return new GqlAccessControlList(managedAcl.getAcl());
  }

}
