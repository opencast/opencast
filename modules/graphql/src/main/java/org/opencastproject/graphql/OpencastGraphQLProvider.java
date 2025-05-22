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

package org.opencastproject.graphql;

import org.opencastproject.graphql.provider.GraphQLAdditionalTypeProvider;
import org.opencastproject.graphql.provider.GraphQLDynamicTypeProvider;
import org.opencastproject.graphql.provider.GraphQLExtensionProvider;
import org.opencastproject.graphql.provider.GraphQLTypeFunctionProvider;
import org.opencastproject.graphql.type.DateTimeFunction;
import org.opencastproject.graphql.type.DurationFunction;
import org.opencastproject.graphql.type.JsonFunction;
import org.opencastproject.graphql.type.MapFunction;
import org.opencastproject.graphql.type.TimeFunction;
import org.opencastproject.graphql.type.input.DublinCoreMetadataInput;
import org.opencastproject.graphql.type.input.GqlCommonEventMetadataInput;
import org.opencastproject.graphql.type.input.GqlCommonSeriesMetadataInput;
import org.opencastproject.graphql.type.output.GqlAccessControlGenericItem;
import org.opencastproject.graphql.type.output.GqlAccessControlGroupItem;
import org.opencastproject.graphql.type.output.GqlAccessControlUserItem;
import org.opencastproject.graphql.type.output.GqlCommonEventMetadata;
import org.opencastproject.graphql.type.output.GqlCommonEventMetadataV2;
import org.opencastproject.graphql.type.output.GqlCommonSeriesMetadata;
import org.opencastproject.graphql.type.output.GqlCommonSeriesMetadataV2;
import org.opencastproject.graphql.type.output.GqlDublinCoreMetadata;
import org.opencastproject.graphql.type.output.field.GqlDurationMetadataField;
import org.opencastproject.graphql.type.output.field.GqlIntMetadataField;
import org.opencastproject.graphql.type.output.field.GqlJsonMetadataField;
import org.opencastproject.graphql.type.output.field.GqlListMetadataField;
import org.opencastproject.graphql.type.output.field.GqlLongMetadataField;
import org.opencastproject.graphql.util.MetadataFieldToGraphQLConverter;
import org.opencastproject.graphql.util.MetadataFieldToGraphQLFieldMapper;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.ConfigurableDCCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.series.CommonSeriesCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import graphql.annotations.processor.GraphQLAnnotations;
import graphql.annotations.processor.typeFunctions.TypeFunction;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

@Component
@ServiceDescription("Opencast GraphQL Provider")
public class OpencastGraphQLProvider implements GraphQLExtensionProvider, GraphQLDynamicTypeProvider,
    GraphQLAdditionalTypeProvider, GraphQLTypeFunctionProvider {

  private final IndexService indexService;

  private final SecurityService securityService;

  @Activate
  public OpencastGraphQLProvider(
      @Reference IndexService indexService,
      @Reference SecurityService securityService
  ) {
    this.indexService = indexService;
    this.securityService = securityService;
  }

  @Override
  public Set<Class<?>> getAdditionalOutputTypes() {
    return Set.of(
        GqlAccessControlUserItem.class,
        GqlAccessControlGroupItem.class,
        GqlAccessControlGenericItem.class,
        GqlJsonMetadataField.class,
        GqlLongMetadataField.class,
        GqlDurationMetadataField.class,
        GqlIntMetadataField.class,
        GqlListMetadataField.class
    );
  }

  @Override
  public Map<String, GraphQLOutputType> getDynamicOutputTypes(Organization organization,
      GraphQLAnnotations graphQLAnnotations) {
    var outputTypes = new HashMap<String, GraphQLOutputType>();
    var oldOrganization = securityService.getOrganization();
    securityService.setOrganization(organization);
    commonEpisodeMetadata(outputTypes, graphQLAnnotations);
    commonEpisodeMetadataV2(outputTypes, graphQLAnnotations);
    commonSeriesMetadata(outputTypes, graphQLAnnotations);
    commonSeriesMetadataV2(outputTypes, graphQLAnnotations);
    securityService.setOrganization(oldOrganization);
    return outputTypes;
  }

  @Override
  public Map<String, GraphQLInputType> getDynamicInputTypes(Organization organization,
      GraphQLAnnotations graphQLAnnotations) {
    var inputTypes = new HashMap<String, GraphQLInputType>();
    var oldOrganization = securityService.getOrganization();
    securityService.setOrganization(organization);
    commonEpisodeMetadataInput(inputTypes, graphQLAnnotations);
    commonSeriesMetadataInput(inputTypes, graphQLAnnotations);
    securityService.setOrganization(oldOrganization);
    return inputTypes;
  }

  private void commonEpisodeMetadataInput(
      HashMap<String, GraphQLInputType> inputTypes,
      GraphQLAnnotations graphQLAnnotations) {

    CommonEventCatalogUIAdapter adapter = (CommonEventCatalogUIAdapter) indexService
        .getCommonEventCatalogUIAdapter();

    commonMetadataInput(inputTypes, graphQLAnnotations, adapter,
        GqlCommonEventMetadataInput.TYPE_NAME,
        GqlCommonEventMetadataInput.class);
  }

  private void commonSeriesMetadataInput(
      HashMap<String, GraphQLInputType> inputTypes,
      GraphQLAnnotations graphQLAnnotations) {

    CommonSeriesCatalogUIAdapter adapter = (CommonSeriesCatalogUIAdapter) indexService
        .getCommonSeriesCatalogUIAdapter();
    commonMetadataInput(inputTypes, graphQLAnnotations, adapter,
        GqlCommonSeriesMetadataInput.TYPE_NAME,
        GqlCommonSeriesMetadataInput.class);
  }

  private void commonMetadataInput(HashMap<String, GraphQLInputType> inputTypes, GraphQLAnnotations graphQLAnnotations,
      ConfigurableDCCatalogUIAdapter adapter, String graphQLTypeName, Class<? extends DublinCoreMetadataInput> clazz) {
    final Set<GraphQLInputObjectField> fieldDefinitions = new HashSet<>();

    adapter.getRawFields().getFields().stream().filter(f -> !f.isReadOnly()).forEach(
        f -> fieldDefinitions.add(
            GraphQLInputObjectField.newInputObjectField()
                .name(f.getOutputID())
                .type((GraphQLInputType) MetadataFieldToGraphQLConverter.convertType(f))
                .description(f.getLabel())
                .build()
        )
    );

    var type = (GraphQLInputObjectType) graphQLAnnotations.getObjectHandler().getTypeRetriever()
        .getGraphQLType(clazz, graphQLAnnotations.getContainer(), true);

    GraphQLInputObjectType transformedObjectType = type
        .transform(builder -> fieldDefinitions.forEach(builder::field));

    inputTypes.put(graphQLTypeName, transformedObjectType);
  }

  private void commonEpisodeMetadata(
      HashMap<String, GraphQLOutputType> outputTypes,
      GraphQLAnnotations graphQLAnnotations) {

    CommonEventCatalogUIAdapter adapter = (CommonEventCatalogUIAdapter) indexService
        .getCommonEventCatalogUIAdapter();

    commonMetadata(outputTypes, graphQLAnnotations, adapter,
        GqlCommonEventMetadata.TYPE_NAME,
        GqlCommonEventMetadata.class);
  }

  private void commonEpisodeMetadataV2(
      HashMap<String, GraphQLOutputType> outputTypes,
      GraphQLAnnotations graphQLAnnotations) {

    CommonEventCatalogUIAdapter adapter = (CommonEventCatalogUIAdapter) indexService
        .getCommonEventCatalogUIAdapter();

    commonMetadataV2(outputTypes, graphQLAnnotations, adapter,
        GqlCommonEventMetadataV2.TYPE_NAME,
        GqlCommonEventMetadataV2.class);
  }

  private void commonSeriesMetadata(
      HashMap<String, GraphQLOutputType> outputTypes,
      GraphQLAnnotations graphQLAnnotations) {

    CommonSeriesCatalogUIAdapter adapter = (CommonSeriesCatalogUIAdapter) indexService
        .getCommonSeriesCatalogUIAdapter();

    commonMetadata(outputTypes, graphQLAnnotations, adapter,
        GqlCommonSeriesMetadata.TYPE_NAME,
        GqlCommonSeriesMetadata.class);
  }

  private void commonSeriesMetadataV2(
      HashMap<String, GraphQLOutputType> outputTypes,
      GraphQLAnnotations graphQLAnnotations) {

    CommonSeriesCatalogUIAdapter adapter = (CommonSeriesCatalogUIAdapter) indexService
        .getCommonSeriesCatalogUIAdapter();

    commonMetadataV2(outputTypes, graphQLAnnotations, adapter,
        GqlCommonSeriesMetadataV2.TYPE_NAME,
        GqlCommonSeriesMetadataV2.class);
  }

  private void commonMetadata(HashMap<String, GraphQLOutputType> outputTypes, GraphQLAnnotations graphQLAnnotations,
      ConfigurableDCCatalogUIAdapter adapter, String graphQLTypeName, Class<? extends GqlDublinCoreMetadata> clazz) {
    final Set<GraphQLFieldDefinition> fieldDefinitions = new HashSet<>();

    for (MetadataField m : adapter.getRawFields().getFields()) {
      GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition();

      builder.name(m.getOutputID());
      builder.type((GraphQLOutputType) MetadataFieldToGraphQLConverter.convertType(m));
      builder.description(m.getLabel());

      fieldDefinitions.add(builder.build());
    }

    var type = (GraphQLObjectType) graphQLAnnotations.getObjectHandler().getTypeRetriever()
        .getGraphQLType(clazz, graphQLAnnotations.getContainer(), false);

    GraphQLObjectType transformedObjectType = type
        .transform(builder -> fieldDefinitions.forEach(builder::field));

    outputTypes.put(graphQLTypeName, transformedObjectType);
  }

  private void commonMetadataV2(HashMap<String, GraphQLOutputType> outputTypes, GraphQLAnnotations graphQLAnnotations,
      ConfigurableDCCatalogUIAdapter adapter, String graphQLTypeName, Class<? extends GqlDublinCoreMetadata> clazz) {
    final Set<GraphQLFieldDefinition> fieldDefinitions = new HashSet<>();

    for (MetadataField m : adapter.getRawFields().getFields()) {
      GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition();

      var outputId = m.getOutputID();

      builder.name(outputId != null ? outputId : m.getInputID());
      var outputType = MetadataFieldToGraphQLFieldMapper.mapToClass(m.getType());
      builder.type((GraphQLOutputType) graphQLAnnotations.getObjectHandler().getTypeRetriever()
          .getGraphQLType(outputType, graphQLAnnotations.getContainer(), false));
      builder.description(m.getLabel());

      fieldDefinitions.add(builder.build());
    }

    var type = (GraphQLObjectType)  graphQLAnnotations.getObjectHandler().getTypeRetriever()
        .getGraphQLType(clazz, graphQLAnnotations.getContainer(), false);

    GraphQLObjectType transformedObjectType = type
        .transform(builder -> fieldDefinitions.forEach(builder::field));

    outputTypes.put(graphQLTypeName, transformedObjectType);
  }

  @Override
  public Set<TypeFunction> getTypeFunctions() {
    return Set.of(
        new DateTimeFunction(),
        new DurationFunction(),
        new JsonFunction(),
        new MapFunction(),
        new TimeFunction()
    );
  }
}
