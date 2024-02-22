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

package org.opencastproject.graphql.type.output.field;

import static org.opencastproject.graphql.type.output.field.GqlLongMetadataField.TYPE_NAME;

import org.opencastproject.graphql.type.output.GqlMetadataFieldInterface;
import org.opencastproject.metadata.dublincore.MetadataField;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(GqlLongMetadataField.TYPE_NAME)
public class GqlLongMetadataField implements GqlMetadataFieldInterface {

  public static final String TYPE_NAME = "GqlLongMetadataField";

  private final MetadataField metadataField;

  public GqlLongMetadataField(MetadataField metadataField) {
    this.metadataField = metadataField;
  }

  @Override
  public MetadataField getMetadataField() {
    return metadataField;
  }

  @GraphQLField
  @GraphQLName("value")
  public Long getValue() {
    Object o = metadataField.getValue();
    if (o instanceof Long) {
      return (Long)(metadataField.getValue());
    } else {
      return null;
    }
  }

}
