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

import org.opencastproject.graphql.type.resolver.AccessControlEntryTypeResolver;
import org.opencastproject.security.api.AccessControlEntry;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeResolver;

@GraphQLName(GqlAccessControlEntry.TYPE_NAME)
@GraphQLTypeResolver(AccessControlEntryTypeResolver.class)
public interface GqlAccessControlEntry {

  String TYPE_NAME = "AccessControlEntry";

  AccessControlEntry getAccessControlEntry();

  @GraphQLField
  default String role() {
    return getAccessControlEntry().getRole();
  }

  @GraphQLField
  default String action() {
    return getAccessControlEntry().getAction();
  }

  @GraphQLField
  default boolean allow() {
    return getAccessControlEntry().isAllow();
  }

}
