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

import org.opencastproject.graphql.type.output.Query;
import org.opencastproject.security.api.Organization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import graphql.annotations.AnnotationsSchemaCreator;
import graphql.annotations.processor.GraphQLAnnotations;
import graphql.schema.GraphQLSchema;

public class SchemaBuilder {

  private static final Logger logger = LoggerFactory.getLogger(SchemaBuilder.class);

  private GraphQLAnnotations annotations;

  private final Organization organization;

  public SchemaBuilder(Organization organization) {
    Objects.requireNonNull(organization, "organization cannot be null");
    this.organization = organization;
  }

  public GraphQLSchema build() {
    this.annotations = new GraphQLAnnotations();
    this.annotations.getContainer().setInputPrefix("");
    this.annotations.getContainer().setInputSuffix("");

    final var builder = GraphQLSchema.newSchema();

    final var annotationsSchema = AnnotationsSchemaCreator.newAnnotationsSchema();

    return annotationsSchema
        .setGraphQLSchemaBuilder(builder)
        .query(Query.class)
        .setAnnotationsProcessor(this.annotations)
        .build();
  }

}
