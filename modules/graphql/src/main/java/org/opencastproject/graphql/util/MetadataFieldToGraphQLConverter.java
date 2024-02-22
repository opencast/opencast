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

package org.opencastproject.graphql.util;


import org.opencastproject.metadata.dublincore.MetadataField;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;

public final class MetadataFieldToGraphQLConverter {

  private MetadataFieldToGraphQLConverter() { }

  public static GraphQLType convertType(MetadataField field) {
    GraphQLType graphQLType;
    switch (field.getType()) {
      case DATE:
        graphQLType = Scalars.GraphQLString;
        break;
      case LONG:
        graphQLType = ExtendedScalars.GraphQLLong;
        break;
      case TEXT:
        graphQLType = Scalars.GraphQLString;
        break;
      case BOOLEAN:
        graphQLType = Scalars.GraphQLBoolean;
        break;
      case DURATION:
        graphQLType = Scalars.GraphQLInt;
        break;
      case TEXT_LONG:
        graphQLType = Scalars.GraphQLString;
        break;
      case MIXED_TEXT:
        graphQLType = GraphQLList.list(Scalars.GraphQLString);
        break;
      case START_DATE:
        graphQLType = Scalars.GraphQLString;
        break;
      case START_TIME:
        graphQLType = Scalars.GraphQLString;
        break;
      case ORDERED_TEXT:
        graphQLType = Scalars.GraphQLString;
        break;
      case ITERABLE_TEXT:
        graphQLType = GraphQLList.list(Scalars.GraphQLString);
        break;
      default:
        // Should not happen but fallback to string.
        graphQLType = Scalars.GraphQLString;
        break;
    }

    return field.isRequired() ? GraphQLNonNull.nonNull(graphQLType) : graphQLType;
  }

}
