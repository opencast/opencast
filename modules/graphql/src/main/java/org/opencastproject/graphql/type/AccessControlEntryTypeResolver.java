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

package org.opencastproject.graphql.type;

import org.opencastproject.graphql.type.output.GqlAccessControlGenericEntry;
import org.opencastproject.graphql.type.output.GqlAccessControlGroupEntry;
import org.opencastproject.graphql.type.output.GqlAccessControlUserEntry;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

public class AccessControlEntryTypeResolver implements TypeResolver {
  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment env) {
    Object obj = env.getObject();
    if (obj instanceof GqlAccessControlGroupEntry) {
      return env.getSchema().getObjectType(GqlAccessControlGroupEntry.TYPE_NAME);
    } else if (obj instanceof GqlAccessControlUserEntry) {
      return env.getSchema().getObjectType(GqlAccessControlUserEntry.TYPE_NAME);
    } else {
      return env.getSchema().getObjectType(GqlAccessControlGenericEntry.TYPE_NAME);
    }
  }
}
