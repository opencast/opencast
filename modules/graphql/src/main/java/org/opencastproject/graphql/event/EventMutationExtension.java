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

package org.opencastproject.graphql.event;

import org.opencastproject.graphql.command.DeleteEventCommand;
import org.opencastproject.graphql.command.UpdateEventCommand;
import org.opencastproject.graphql.type.input.GqlCommonEventMetadataInput;
import org.opencastproject.graphql.type.input.Mutation;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;

@GraphQLTypeExtension(Mutation.class)
public final class EventMutationExtension {

  private EventMutationExtension() {
  }

  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Update event metadata")
  public static GqlEvent updateEvent(
      @GraphQLName("id") @GraphQLNonNull String id,
      @GraphQLName("metadata") @GraphQLNonNull GqlCommonEventMetadataInput eventMetadataInput,
      final DataFetchingEnvironment environment) {
    return UpdateEventCommand
        .create(id, eventMetadataInput)
        .environment(environment)
        .build()
        .execute();
  }

  @GraphQLField
  @GraphQLDescription("Delete event")
  public static GqlDeleteEventPayload deleteEvent(
      @GraphQLName("id") @GraphQLNonNull String id,
      final DataFetchingEnvironment environment) {
    return DeleteEventCommand
        .create(id)
        .environment(environment)
        .build()
        .execute();
  }

}
