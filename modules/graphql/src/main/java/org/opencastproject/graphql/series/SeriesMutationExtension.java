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

package org.opencastproject.graphql.series;

import org.opencastproject.graphql.command.CreateSeriesCommand;
import org.opencastproject.graphql.command.UpdateSeriesAclCommand;
import org.opencastproject.graphql.command.UpdateSeriesCommand;
import org.opencastproject.graphql.type.input.AccessControlListInput;
import org.opencastproject.graphql.type.input.GqlCommonSeriesMetadataInput;
import org.opencastproject.graphql.type.input.Mutation;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;

@GraphQLTypeExtension(Mutation.class)
public final class SeriesMutationExtension {

  private SeriesMutationExtension() {
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Create series with metadata and acl")
  public static GqlSeries createSeries(
      @GraphQLName("metadata") @GraphQLNonNull GqlCommonSeriesMetadataInput seriesMetadataInput,
      @GraphQLName("acl") @GraphQLNonNull AccessControlListInput aclInput,
      final DataFetchingEnvironment environment) {
    return CreateSeriesCommand.create(seriesMetadataInput, aclInput)
        .environment(environment)
        .build()
        .execute();
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Update series metadata and optional the acl")
  public static GqlSeries updateSeries(
      @GraphQLName("id") @GraphQLNonNull String id,
      @GraphQLName("metadata") @GraphQLNonNull GqlCommonSeriesMetadataInput seriesMetadataInput,
      @GraphQLName("acl") AccessControlListInput aclInput,
      final DataFetchingEnvironment environment) {
    return UpdateSeriesCommand.create(id, seriesMetadataInput)
        .environment(environment)
        .build()
        .execute();
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Update series acl")
  public static GqlSeries updateSeriesAcl(
      @GraphQLName("id") @GraphQLNonNull String id,
      @GraphQLName("acl") @GraphQLNonNull AccessControlListInput aclInput,
      final DataFetchingEnvironment environment) {
    return UpdateSeriesAclCommand.create(id)
        .environment(environment)
        .build()
        .execute();
  }

}
