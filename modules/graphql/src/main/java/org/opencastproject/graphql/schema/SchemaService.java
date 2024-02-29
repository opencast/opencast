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

import org.opencastproject.graphql.provider.GraphQLAdditionalTypeProvider;
import org.opencastproject.graphql.provider.GraphQLDynamicTypeProvider;
import org.opencastproject.graphql.provider.GraphQLExtensionProvider;
import org.opencastproject.graphql.provider.GraphQLFieldVisibilityProvider;
import org.opencastproject.graphql.provider.GraphQLMutationProvider;
import org.opencastproject.graphql.provider.GraphQLQueryProvider;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryListener;
import org.opencastproject.security.api.OrganizationDirectoryService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
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

  @ObjectClassDefinition
  public @interface SchemaConfiguration {

    int schema_update_trigger_delay() default 2000;

  }

  private static final Logger logger = LoggerFactory.getLogger(SchemaService.class);

  private final OrganizationDirectoryService organizationDirectoryService;

  private final Map<String, GraphQLSchema> schemas = new ConcurrentHashMap<>(8, 0.9f, 1);

  private final List<GraphQLQueryProvider> queryProviders = new CopyOnWriteArrayList<>();

  private final List<GraphQLMutationProvider> mutationProviders = new CopyOnWriteArrayList<>();

  private final List<GraphQLExtensionProvider> extensionsProviders = new CopyOnWriteArrayList<>();

  private final List<GraphQLAdditionalTypeProvider> additionalTypesProviders = new CopyOnWriteArrayList<>();

  private final List<GraphQLFieldVisibilityProvider> fieldVisibilityProviders = new CopyOnWriteArrayList<>();

  private final List<GraphQLDynamicTypeProvider> dynamicTypeProviders = new CopyOnWriteArrayList<>();

  private final ScheduledExecutorService schemaUpdateExecutor;
  private final int schemaUpdateTriggerDelay;

  private final Map<Organization, ScheduledFuture<?>> scheduledFutureMap = new ConcurrentHashMap<>();

  @Activate
  public SchemaService(
      @Reference OrganizationDirectoryService organizationDirectoryService,
      final SchemaConfiguration config
  ) {
    if (config.schema_update_trigger_delay() < 0) {
      throw new IllegalArgumentException("Schema update trigger delay must be greater than or equal to 0");
    }
    this.organizationDirectoryService = organizationDirectoryService;
    this.schemaUpdateTriggerDelay = config.schema_update_trigger_delay();
    schemaUpdateExecutor = Executors.newSingleThreadScheduledExecutor(new SchemaUpdateThreadFactory());
    triggerSchemaUpdate();
  }

  @Deactivate
  protected void dispose() {
    scheduledFutureMap.forEach((organization, scheduledFuture) -> {
      scheduledFuture.cancel(true);
    });
    schemaUpdateExecutor.shutdown();
  }

  public GraphQLSchema get(String organizationId) {
    return schemas.get(organizationId);
  }

  public GraphQLSchema buildSchema(Organization organization) {
    logger.info("Building GraphQL schema for organization {}", organization.getId());
    var schemaBuilder = new SchemaBuilder(organization)
        .extensionProviders(extensionsProviders)
        .dynamicTypeProviders(dynamicTypeProviders)
        .queryProviders(queryProviders)
        .mutationProviders(mutationProviders)
        .codeRegistryProviders(additionalTypesProviders)
        .additionalTypeProviders(additionalTypesProviders)
        .fieldVisibilityProviders(fieldVisibilityProviders);
    return schemaBuilder.build();
  }

  private void triggerSchemaUpdate() {
    try {
      organizationDirectoryService.getOrganizations().forEach(this::triggerSchemaUpdate);
    } catch (RejectedExecutionException e) {
      logger.debug("Scheduler [shutdown: {}, terminated: {}] does not except jobs, skipping schema update trigger.",
          schemaUpdateExecutor.isShutdown(),
          schemaUpdateExecutor.isTerminated()
      );
    }
  }

  private void triggerSchemaUpdate(Organization organization) {
    ScheduledFuture<?> future = scheduledFutureMap.get(organization);
    if (future != null) {
      future.cancel(true);
    }

    scheduledFutureMap.put(organization,
        schemaUpdateExecutor.schedule(() -> updateSchema(organization), schemaUpdateTriggerDelay,
            TimeUnit.MILLISECONDS));
  }

  public void updateSchema(Organization organization) {
    try {
      schemas.put(organization.getId(), buildSchema(organization));
    } catch (Throwable t) {
      logger.error("Error building GraphQL schema for organization {}", organization.getId(), t);
    }
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

  @Reference(
      policy = ReferencePolicy.DYNAMIC,
      cardinality = ReferenceCardinality.MULTIPLE
  )
  public void bindQueryProvider(GraphQLQueryProvider provider) {
    queryProviders.add(provider);
    triggerSchemaUpdate();
  }

  public void unbindQueryProvider(GraphQLQueryProvider provider) {
    queryProviders.remove(provider);
    triggerSchemaUpdate();
  }

  @Reference(
      policy = ReferencePolicy.DYNAMIC,
      cardinality = ReferenceCardinality.MULTIPLE
  )
  public void bindMutationProvider(GraphQLMutationProvider provider) {
    mutationProviders.add(provider);
    triggerSchemaUpdate();
  }

  public void unbindMutationProvider(GraphQLMutationProvider provider) {
    mutationProviders.remove(provider);
    triggerSchemaUpdate();
  }

  @Reference(
      policy = ReferencePolicy.DYNAMIC,
      cardinality = ReferenceCardinality.MULTIPLE
  )
  public void bindExtensionProvider(GraphQLExtensionProvider provider) {
    extensionsProviders.add(provider);
    triggerSchemaUpdate();
  }

  public void unbindExtensionProvider(GraphQLExtensionProvider provider) {
    extensionsProviders.remove(provider);
    triggerSchemaUpdate();
  }

  @Reference(
      policy = ReferencePolicy.DYNAMIC,
      cardinality = ReferenceCardinality.MULTIPLE
  )
  public void bindAdditionalTypeProvider(GraphQLAdditionalTypeProvider provider) {
    additionalTypesProviders.add(provider);
    triggerSchemaUpdate();
  }

  public void unbindAdditionalTypeProvider(GraphQLAdditionalTypeProvider provider) {
    additionalTypesProviders.remove(provider);
    triggerSchemaUpdate();
  }

  @Reference(
      policy = ReferencePolicy.DYNAMIC,
      cardinality = ReferenceCardinality.MULTIPLE
  )
  public void bindFieldVisibilityProvider(GraphQLFieldVisibilityProvider provider) {
    fieldVisibilityProviders.add(provider);
    triggerSchemaUpdate();
  }

  public void unbindFieldVisibilityProvider(GraphQLFieldVisibilityProvider provider) {
    fieldVisibilityProviders.remove(provider);
    triggerSchemaUpdate();
  }

  @Reference(
      policy = ReferencePolicy.DYNAMIC,
      cardinality = ReferenceCardinality.MULTIPLE
  )
  public void bindDynamicTypeProvider(GraphQLDynamicTypeProvider provider) {
    dynamicTypeProviders.add(provider);
    triggerSchemaUpdate();
  }

  public void unbindDynamicTypeProvider(GraphQLDynamicTypeProvider provider) {
    dynamicTypeProviders.remove(provider);
    triggerSchemaUpdate();
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
