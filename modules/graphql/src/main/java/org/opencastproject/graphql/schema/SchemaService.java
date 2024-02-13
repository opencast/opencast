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

import org.opencastproject.graphql.providers.GraphQLProvider;
import org.opencastproject.security.api.Organization;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import graphql.schema.GraphQLSchema;

@Component(
    service = {SchemaService.class}
)
@ServiceDescription("GraphQL Schema Service")
public class SchemaService  {

  public @interface Config {
    int schemaUpdateTriggerDelay() default 2000;
  }

  private static final Logger logger = LoggerFactory.getLogger(SchemaService.class);

  private final ScheduledExecutorService schemaUpdateExecutor;

  private final List<GraphQLProvider> providers = new CopyOnWriteArrayList<>();



  @Activate
  public SchemaService(final Config config) {
    if (config.schemaUpdateTriggerDelay() < 0) {
      throw new IllegalArgumentException("Schema update trigger delay must be greater than or equal to 0");
    }
    schemaUpdateExecutor = Executors.newSingleThreadScheduledExecutor(new SchemaUpdateThreadFactory());

  }

  public GraphQLSchema buildSchema(Organization organization) {
    var schemaBuilder = new SchemaBuilder(organization);

    return schemaBuilder.build();
  }

  private triggerSchemaUpdate() {
    if (schemaUpdateTask != null) {
      schemaUpdateTask.cancel();
    }
    schemaUpdateTask = new SchemaUpdateTask();

  }

  @Reference(
      cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.DYNAMIC
  )
  public void addProvider(GraphQLProvider provider) {
    providers.add(provider);
  }

  public void removeProvider(GraphQLProvider provider) {
    providers.remove(provider);
  }


  public static class SchemaUpdateTask extends TimerTask {
    @Override
    public void run() {

    }
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
