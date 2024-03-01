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

package org.opencastproject.graphql.type.output;

import org.opencastproject.security.api.AccessControlList;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(GqlAccessControlList.TYPE_NAME)
@GraphQLDescription("Access control list")
public class GqlAccessControlList {

  public static final String TYPE_NAME = "GqlAccessControlList";

  private final AccessControlList accessControlList;

  public GqlAccessControlList(AccessControlList accessControlList) {
    this.accessControlList = accessControlList;
  }

  @GraphQLField
  public GqlAccessControlEntry[] entries() {
    return accessControlList.getEntries().stream()
        .map(GqlAccessControlGenericEntry::new)
        .toArray(GqlAccessControlEntry[]::new);
  }


}
