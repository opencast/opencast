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

import org.opencastproject.graphql.schema.SchemaService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryListener;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.AsyncSerialExecutionStrategy;

@Component(
    service = { ExecutionService.class, OrganizationDirectoryListener.class }
)
public class ExecutionService implements OrganizationDirectoryListener {

  private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

  private final Map<String, OrganizationEnvironment> organizationEnvironment =
      new ConcurrentHashMap<>(8, 0.9f, 1);
  private final OrganizationDirectoryService organizationDirectoryService;
  private final SecurityService securityService;
  private final SchemaService schemaService;
  private final BundleContext context;

  @Activate
  public ExecutionService(
      @Reference SchemaService schemaService,
      @Reference OrganizationDirectoryService organizationDirectoryService,
      @Reference SecurityService securityService,
      BundleContext context
  ) {
    this.schemaService = schemaService;
    this.organizationDirectoryService = organizationDirectoryService;
    this.securityService = securityService;
    this.context = context;

    for (Organization organization : organizationDirectoryService.getOrganizations()) {
      organizationRegistered(organization);
    }
  }

  @Override
  public void organizationRegistered(Organization organization) {
    logger.info("Building GraphQL schema for organization {}", organization.getId());
    var executionIdProvider = new OrganizationExecutionIdProvider(organization.getId());
    GraphQL graphQL = GraphQL.newGraphQL(schemaService.buildSchema(organization))
        .queryExecutionStrategy(new AsyncExecutionStrategy())
        .mutationExecutionStrategy(new AsyncSerialExecutionStrategy())
        .executionIdProvider(executionIdProvider)
        .build();
    organizationEnvironment.put(organization.getId(), new OrganizationEnvironment(organization.getId(), graphQL));
  }

  @Override
  public void organizationUnregistered(Organization organization) {
    logger.info("Removing GraphQL schema for organization {}", organization.getId());
    organizationEnvironment.remove(organization.getId());
  }

  @Override
  public void organizationUpdated(Organization organization) {
    logger.trace("Organization {} updated", organization.getId());
  }

  public ExecutionResult execute(
      String query,
      String operationName,
      Map<String, Object> variables,
      Map<String, Object> extensions) {

    Map<?, Object> context = new HashMap<>();

    return execute(ExecutionInput.newExecutionInput()
        .query(query)
        .operationName(operationName)
        .variables((variables != null) ? variables : Collections.emptyMap())
        .extensions((extensions != null) ? extensions : Collections.emptyMap())
        .graphQLContext(context)
        .build());
  }

  public ExecutionResult execute(ExecutionInput executionInput) {
    OrganizationEnvironment environment = organizationEnvironment.get(securityService.getOrganization().getId());

    if (environment == null) {
      throw new IllegalStateException("No GraphQL schema found for organization '"
          + securityService.getOrganization().getId() + "'");
    }

    return environment.getGraphQL().execute(executionInput);
  }
}
