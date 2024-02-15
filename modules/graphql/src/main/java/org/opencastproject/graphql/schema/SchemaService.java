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

package org.opencastproject.graphql.schema;

import org.opencastproject.graphql.providers.GraphQLAdditionalTypeProvider;
import org.opencastproject.graphql.providers.GraphQLExtensionProvider;
import org.opencastproject.graphql.providers.GraphQLFieldVisibilityProvider;
import org.opencastproject.graphql.providers.GraphQLMutationProvider;
import org.opencastproject.graphql.providers.GraphQLQueryProvider;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryListener;
import org.opencastproject.security.api.OrganizationDirectoryService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import graphql.schema.GraphQLSchema;

@Component(
    service = {SchemaService.class, OrganizationDirectoryListener.class}
)
@ServiceDescription("GraphQL Schema Service")
public class SchemaService implements OrganizationDirectoryListener {

  public @interface Config {

    int schemaUpdateTriggerDelay() default 2000;

  }

  private static final Logger logger = LoggerFactory.getLogger(SchemaService.class);

  private final OrganizationDirectoryService organizationDirectoryService;

  private final Map<String, GraphQLSchema> schemas = new ConcurrentHashMap<>(8, 0.9f, 1);

  private final List<GraphQLQueryProvider> queryProviders = new CopyOnWriteArrayList<>();

  private final List<GraphQLMutationProvider> mutationProviders = new CopyOnWriteArrayList<>();

  private final List<GraphQLExtensionProvider> extensionsProviders = new CopyOnWriteArrayList<>();

  private final List<GraphQLAdditionalTypeProvider> additionalTypesProviders = new CopyOnWriteArrayList<>();

  private final List<GraphQLFieldVisibilityProvider> fieldVisibilityProviders = new CopyOnWriteArrayList<>();

  private final ScheduledExecutorService schemaUpdateExecutor;
  private final int schemaUpdateTriggerDelay;
  private ScheduledFuture<?> schemaUpdateFuture;

  @Activate
  public SchemaService(
      @Reference OrganizationDirectoryService organizationDirectoryService,
      final Config config
  ) {
    if (config.schemaUpdateTriggerDelay() < 0) {
      throw new IllegalArgumentException("Schema update trigger delay must be greater than or equal to 0");
    }
    this.organizationDirectoryService = organizationDirectoryService;
    this.schemaUpdateTriggerDelay = config.schemaUpdateTriggerDelay();
    schemaUpdateExecutor = Executors.newSingleThreadScheduledExecutor(new SchemaUpdateThreadFactory());
    triggerSchemaUpdate();
  }

  public GraphQLSchema get(String organizationId) {
    return schemas.get(organizationId);
  }

  public GraphQLSchema buildSchema(Organization organization) {
    var schemaBuilder = new SchemaBuilder(organization);

    return schemaBuilder.build();
  }

  private void triggerSchemaUpdate() {
    organizationDirectoryService.getOrganizations().forEach(this::triggerSchemaUpdate);
  }

  private void triggerSchemaUpdate(Organization organization) {
    if (schemaUpdateFuture != null) {
      schemaUpdateFuture.cancel(true);
    }
    schemaUpdateFuture = schemaUpdateExecutor.schedule(
        () -> updateSchema(organization),
        schemaUpdateTriggerDelay,
        TimeUnit.MILLISECONDS
    );
  }

  public void updateSchema(Organization organization) {
    schemas.put(organization.getId(), buildSchema(organization));
  }

  @Override
  public void organizationRegistered(Organization organization) {
    logger.info("Trigger GraphQL schema update for organization {}", organization.getId());
    triggerSchemaUpdate(organization);
  }

  @Override
  public void organizationUnregistered(Organization organization) {
    logger.info("Removing GraphQL schema for organization {}", organization.getId());
    schemas.remove(organization.getId());
  }

  @Override
  public void organizationUpdated(Organization organization) {
    logger.trace("Organization {} updated", organization.getId());
    triggerSchemaUpdate(organization);
  }

  public static class SchemaUpdateThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
      var thread = new Thread(r, "GraphQL-Schema-Update-" + threadNumber.getAndIncrement());
      thread.setDaemon(true);
      return thread;
    }

  }

}
