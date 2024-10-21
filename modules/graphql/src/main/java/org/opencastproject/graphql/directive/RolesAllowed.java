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

package org.opencastproject.graphql.directive;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.directives.definition.DirectiveLocations;
import graphql.annotations.annotationTypes.directives.definition.GraphQLDirectiveDefinition;
import graphql.introspection.Introspection;

@GraphQLName("rolesAllowed")
@GraphQLDescription("This directive is used to specify the authorization level required to access a field.")
@GraphQLDirectiveDefinition(wiring = RolesAllowedWiring.class)
@DirectiveLocations({ Introspection.DirectiveLocation.FIELD_DEFINITION})
@Retention(RetentionPolicy.RUNTIME)
public @interface RolesAllowed {

  @GraphQLName("roles")
  @GraphQLDescription("The role required to access the field.")
  String[] value();

}
