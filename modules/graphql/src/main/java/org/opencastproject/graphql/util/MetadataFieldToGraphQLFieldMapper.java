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


import org.opencastproject.graphql.type.output.GqlMetadataFieldInterface;
import org.opencastproject.graphql.type.output.field.GqlBooleanMetadataField;
import org.opencastproject.graphql.type.output.field.GqlIntMetadataField;
import org.opencastproject.graphql.type.output.field.GqlListMetadataField;
import org.opencastproject.graphql.type.output.field.GqlLongMetadataField;
import org.opencastproject.graphql.type.output.field.GqlStringMetadataField;
import org.opencastproject.metadata.dublincore.MetadataField;

public final class MetadataFieldToGraphQLFieldMapper {

  private MetadataFieldToGraphQLFieldMapper() { }

  public static GqlMetadataFieldInterface mapType(MetadataField field) {
    GqlMetadataFieldInterface gqlField;
    switch (field.getType()) {
      case DATE:
        gqlField = new GqlStringMetadataField(field);
        break;
      case LONG:
        gqlField = new GqlLongMetadataField(field);
        break;
      case TEXT:
        gqlField = new GqlStringMetadataField(field);
        break;
      case BOOLEAN:
        gqlField = new GqlBooleanMetadataField(field);
        break;
      case DURATION:
        gqlField = new GqlIntMetadataField(field);
        break;
      case TEXT_LONG:
        gqlField = new GqlStringMetadataField(field);
        break;
      case MIXED_TEXT:
        gqlField = new GqlListMetadataField(field);
        break;
      case START_DATE:
        gqlField = new GqlStringMetadataField(field);
        break;
      case START_TIME:
        gqlField = new GqlStringMetadataField(field);
        break;
      case ORDERED_TEXT:
        gqlField = new GqlStringMetadataField(field);
        break;
      case ITERABLE_TEXT:
        gqlField = new GqlListMetadataField(field);
        break;
      default:
        // Should not happen but fallback to string.
        gqlField = new GqlStringMetadataField(field);
        break;
    }

    return gqlField;
  }

}
