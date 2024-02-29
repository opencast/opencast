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

import org.opencastproject.graphql.execution.context.OpencastContextManager;
import org.opencastproject.graphql.schema.SchemaService;
import org.opencastproject.security.api.SecurityService;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.GraphQLSchema;

@Component(
    service = { ExecutionService.class }
)
public class ExecutionService {

  private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

  private final SecurityService securityService;
  private final SchemaService schemaService;
  private final BundleContext bundleContext;

  private final Map<String, GraphQL> organizationGraphQL = new ConcurrentHashMap<>();

  public @interface ExecutionConfiguration {
    int execution_max_query_complexity() default 1000;

    int execution_max_query_depth() default 10;

  }

  private final ExecutionConfiguration config;

  @Activate
  public ExecutionService(
      @Reference SchemaService schemaService,
      @Reference SecurityService securityService,
      BundleContext bundleContext,
      ExecutionConfiguration config
  ) {
    if (config.execution_max_query_complexity() <= 0) {
      throw new IllegalArgumentException("execution_max_query_complexity must be greater than 0");
    }
    if (config.execution_max_query_depth() <= 0) {
      throw new IllegalArgumentException("execution_max_query_depth must be greater than 0");
    }

    this.schemaService = schemaService;
    this.securityService = securityService;
    this.bundleContext = bundleContext;
    this.config = config;
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
    try {
      var context = OpencastContextManager.initiateContext(bundleContext);

      context.setOrganization(securityService.getOrganization());
      context.setUser(securityService.getUser());

      executionInput.getGraphQLContext().put(OpencastContextManager.CONTEXT, context);

      var graphQL = getGraphQL(securityService.getOrganization().getId());

      if (graphQL == null) {
        throw new IllegalStateException(
            "No GraphQL schema found for organization `" + securityService.getOrganization().getId() + "`");
      }

      return graphQL.execute(executionInput);
    } finally {
      OpencastContextManager.clearContext();
    }
  }

  private GraphQL getGraphQL(String organizationId) {
    GraphQL graphQL = organizationGraphQL.get(organizationId);
    GraphQLSchema schema = schemaService.get(organizationId);

    List<Instrumentation> chainedList = new ArrayList<>(
        List.of(new MaxQueryDepthInstrumentation(config.execution_max_query_depth()),
            new MaxQueryComplexityInstrumentation(config.execution_max_query_complexity())));

    if (logger.isTraceEnabled()) {
      logger.trace("Enabling tracing instrumentation for organization `{}`", organizationId);
      chainedList.add(new TracingInstrumentation());
    }

    if (graphQL == null || !schema.equals(graphQL.getGraphQLSchema())) {
      var exceptionHandler = new OpencastDataFetcherExceptionHandler();
      graphQL = GraphQL.newGraphQL(schema)
          .queryExecutionStrategy(new AsyncExecutionStrategy(exceptionHandler))
          .mutationExecutionStrategy(new AsyncSerialExecutionStrategy(exceptionHandler))
          .executionIdProvider(new OrganizationExecutionIdProvider(organizationId))
          .defaultDataFetcherExceptionHandler(exceptionHandler)
          .instrumentation(new ChainedInstrumentation(chainedList))
          .build();
      organizationGraphQL.put(organizationId, graphQL);
    }

    return graphQL;
  }

}
