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

import org.opencastproject.security.api.Organization;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.GraphQLSchema;

@Component(
    service = {SchemaService.class}
)
@ServiceDescription("GraphQL Schema Service")
public class SchemaService  {

  private static final Logger logger = LoggerFactory.getLogger(SchemaService.class);

  @Activate
  public SchemaService() {

  }

  public GraphQLSchema buildSchema(Organization organization) {
    var schemaBuilder = new SchemaBuilder(organization);

    return schemaBuilder.build();
  }

}
