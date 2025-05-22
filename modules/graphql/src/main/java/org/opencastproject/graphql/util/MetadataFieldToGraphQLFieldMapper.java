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
import org.opencastproject.graphql.type.output.field.GqlDateTimeMetadataField;
import org.opencastproject.graphql.type.output.field.GqlDurationMetadataField;
import org.opencastproject.graphql.type.output.field.GqlListMetadataField;
import org.opencastproject.graphql.type.output.field.GqlLongMetadataField;
import org.opencastproject.graphql.type.output.field.GqlStringMetadataField;
import org.opencastproject.metadata.dublincore.MetadataField;

import java.lang.reflect.InvocationTargetException;

public final class MetadataFieldToGraphQLFieldMapper {

  private MetadataFieldToGraphQLFieldMapper() { }

  public static GqlMetadataFieldInterface mapType(MetadataField field) {
    GqlMetadataFieldInterface gqlField;
    var clazz = mapToClass(field.getType());
    try {
      gqlField = clazz.getConstructor(MetadataField.class).newInstance(field);
    } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    }
    return gqlField;
  }

  public static Class<? extends GqlMetadataFieldInterface> mapToClass(MetadataField.Type type) {
    Class<? extends GqlMetadataFieldInterface> clazz;
    switch (type) {
      case DATE:
      case START_DATE:
        clazz = GqlDateTimeMetadataField.class;
        break;
      case LONG:
        clazz = GqlLongMetadataField.class;
        break;
      case TEXT:
      case ORDERED_TEXT:
      case TEXT_LONG:
        clazz = GqlStringMetadataField.class;
        break;
      case BOOLEAN:
        clazz = GqlBooleanMetadataField.class;
        break;
      case DURATION:
      case START_TIME:
        clazz = GqlDurationMetadataField.class;
        break;
      case MIXED_TEXT:
      case ITERABLE_TEXT:
        clazz = GqlListMetadataField.class;
        break;
      default:
        // Should not happen but fallback to string.
        clazz = GqlStringMetadataField.class;
        break;
    }
    return clazz;
  }
}
