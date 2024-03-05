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

package org.opencastproject.graphql.type.output;

import org.opencastproject.graphql.type.resolver.GqlMetadataFieldInterfaceResolver;
import org.opencastproject.metadata.dublincore.MetadataField;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeResolver;

@GraphQLName(GqlMetadataFieldInterface.TYPE_NAME)
@GraphQLTypeResolver(GqlMetadataFieldInterfaceResolver.class)
@GraphQLDescription("The metadata field interface provides common fields for all metadata fields.")
public interface GqlMetadataFieldInterface {

  String TYPE_NAME = "GqlMetadataFieldInterface";

  MetadataField getMetadataField();

  @GraphQLField
  default String id() {
    return getMetadataField().getOutputID();
  }
  @GraphQLField
  default String label() {
    return getMetadataField().getLabel();
  }

  @GraphQLField
  default MetadataField.Type type() {
    return getMetadataField().getType();
  }

  @GraphQLField
  default boolean readOnly() {
    return getMetadataField().isReadOnly();
  }

  @GraphQLField
  default boolean required() {
    return getMetadataField().isRequired();
  }

  @GraphQLField
  default String listProvider() {
    return getMetadataField().getListprovider();
  }

  @GraphQLField
  default String collectionId() {
    return getMetadataField().getCollectionID();
  }

  @GraphQLField
  default Integer order() {
    return getMetadataField().getOrder();
  }

}
