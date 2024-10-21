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

import org.opencastproject.graphql.directive.RolesAllowed;
import org.opencastproject.graphql.provider.GraphQLAdditionalTypeProvider;
import org.opencastproject.graphql.provider.GraphQLDynamicTypeProvider;
import org.opencastproject.graphql.provider.GraphQLExtensionProvider;
import org.opencastproject.graphql.provider.GraphQLFieldVisibilityProvider;
import org.opencastproject.graphql.provider.GraphQLMutationProvider;
import org.opencastproject.graphql.provider.GraphQLQueryProvider;
import org.opencastproject.graphql.provider.GraphQLTypeFunctionProvider;
import org.opencastproject.graphql.schema.builder.AdditionalTypeBuilder;
import org.opencastproject.graphql.schema.builder.DynamicTypeBuilder;
import org.opencastproject.graphql.schema.builder.ExtensionBuilder;
import org.opencastproject.graphql.schema.builder.TypeFunctionBuilder;
import org.opencastproject.graphql.type.input.Mutation;
import org.opencastproject.graphql.type.output.Query;
import org.opencastproject.security.api.Organization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import graphql.annotations.AnnotationsSchemaCreator;
import graphql.annotations.processor.GraphQLAnnotations;
import graphql.schema.GraphQLSchema;

public class SchemaBuilder {

  private static final Logger logger = LoggerFactory.getLogger(SchemaBuilder.class);

  private GraphQLAnnotations annotations;

  private final Organization organization;

  private final ExtensionBuilder extensionBuilder;

  private final DynamicTypeBuilder dynamicTypeBuilder;

  private final AdditionalTypeBuilder additionalTypeBuilder;

  private final TypeFunctionBuilder typeFunctionBuilder;

  public SchemaBuilder(Organization organization) {
    Objects.requireNonNull(organization, "organization cannot be null");
    this.organization = organization;
    this.dynamicTypeBuilder = new DynamicTypeBuilder(organization);
    this.extensionBuilder = new ExtensionBuilder();
    this.additionalTypeBuilder = new AdditionalTypeBuilder();
    this.typeFunctionBuilder = new TypeFunctionBuilder();
  }

  public GraphQLSchema build() {
    this.annotations = new GraphQLAnnotations();
    this.annotations.getContainer().setInputPrefix("");
    this.annotations.getContainer().setInputSuffix("");

    final var builder = GraphQLSchema.newSchema();

    typeFunctionBuilder.withAnnotations(annotations).build();

    extensionBuilder.withAnnotations(annotations).build();

    dynamicTypeBuilder.withAnnotations(annotations).build();

    final var annotationsSchema = AnnotationsSchemaCreator.newAnnotationsSchema();
    additionalTypeBuilder.withAnnotationSchema(annotationsSchema).build();

    return annotationsSchema
        .setGraphQLSchemaBuilder(builder)
        .query(Query.class)
        .mutation(Mutation.class)
        .setAnnotationsProcessor(this.annotations)
        .directive(RolesAllowed.class)
        .build();
  }

  public SchemaBuilder extensionProviders(List<GraphQLExtensionProvider> extensionProviders) {
    extensionBuilder.withExtensionProviders(extensionProviders);
    return this;
  }

  public SchemaBuilder dynamicTypeProviders(List<GraphQLDynamicTypeProvider> dynamicTypeProviders) {
    dynamicTypeBuilder.withDynamicTypeProviders(dynamicTypeProviders);
    return this;
  }

  public SchemaBuilder queryProviders(List<GraphQLQueryProvider> queryProviders) {
    return this;
  }

  public SchemaBuilder mutationProviders(List<GraphQLMutationProvider> mutationProviders) {
    return this;
  }

  public SchemaBuilder codeRegistryProviders(List<GraphQLAdditionalTypeProvider> additionalTypesProviders) {
    return this;
  }

  public SchemaBuilder additionalTypeProviders(List<GraphQLAdditionalTypeProvider> additionalTypesProviders) {
    additionalTypeBuilder.withAdditionalTypeProviders(additionalTypesProviders);
    return this;
  }

  public SchemaBuilder fieldVisibilityProviders(List<GraphQLFieldVisibilityProvider> fieldVisibilityProviders) {
    return this;
  }

  public SchemaBuilder typeFunctionProviders(List<GraphQLTypeFunctionProvider> typeFunctionProviders) {
    typeFunctionBuilder.withTypeFunctionProviders(typeFunctionProviders);
    return this;
  }
}
