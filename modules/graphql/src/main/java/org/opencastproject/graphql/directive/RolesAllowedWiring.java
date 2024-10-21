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

import org.opencastproject.graphql.exception.GraphQLUnauthorizedException;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.execution.context.OpencastContextManager;
import org.opencastproject.security.api.SecurityService;

import graphql.annotations.directives.AnnotationsDirectiveWiring;
import graphql.annotations.directives.AnnotationsWiringEnvironment;
import graphql.annotations.processor.util.CodeRegistryUtil;
import graphql.schema.GraphQLFieldDefinition;

public class RolesAllowedWiring implements AnnotationsDirectiveWiring {

  @Override
  public GraphQLFieldDefinition onField(AnnotationsWiringEnvironment environment) {
    GraphQLFieldDefinition field = (GraphQLFieldDefinition) environment.getElement();
    String[] hasRole = environment.getDirective().toAppliedDirective().getArgument("roles").getValue();
    CodeRegistryUtil.wrapDataFetcher(field, environment, (((dataFetchingEnvironment, value) -> {
      OpencastContext context = OpencastContextManager.getCurrentContext();
      SecurityService securityService = context.getService(SecurityService.class);
      for (String role : hasRole) {
        if (securityService != null && securityService.getUser().hasRole(role)) {
          break;
        }
        throw new GraphQLUnauthorizedException("The current user is not authorized to access this resource.");
      }

      return value;
    })));
    return field;
  }

}
