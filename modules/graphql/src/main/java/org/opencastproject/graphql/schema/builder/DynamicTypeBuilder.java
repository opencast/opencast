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

package org.opencastproject.graphql.schema.builder;

import org.opencastproject.graphql.provider.GraphQLDynamicTypeProvider;
import org.opencastproject.security.api.Organization;

import java.util.ArrayList;
import java.util.List;

import graphql.annotations.processor.GraphQLAnnotations;

public class DynamicTypeBuilder {

  private final Organization organization;
  private GraphQLAnnotations annotations;

  private List<GraphQLDynamicTypeProvider> dynamicTypeProviders = new ArrayList<>();

  public DynamicTypeBuilder(Organization organization) {
    this.organization = organization;
  }

  public DynamicTypeBuilder withAnnotations(GraphQLAnnotations annotations) {
    this.annotations = annotations;
    return this;
  }

  public DynamicTypeBuilder withDynamicTypeProviders(List<GraphQLDynamicTypeProvider> dynamicTypeProviders) {
    this.dynamicTypeProviders = dynamicTypeProviders;
    return this;
  }

  public void build() {
    if (dynamicTypeProviders != null && !dynamicTypeProviders.isEmpty()) {
      for (final GraphQLDynamicTypeProvider typeProvider : dynamicTypeProviders) {
        if (typeProvider != null && typeProvider.getDynamicOutputTypes(organization, annotations) != null) {
          typeProvider.getDynamicOutputTypes(organization, annotations)
              .forEach((typeName, type) -> {
                annotations.getContainer().getTypeRegistry().put(typeName, type);
              });
        }
        if (typeProvider != null && typeProvider.getDynamicInputTypes(organization, annotations) != null) {
          typeProvider.getDynamicInputTypes(organization, annotations)
              .forEach((typeName, type) -> {
                annotations.getContainer().getTypeRegistry().put(typeName, type);
              });
        }
      }
    }
  }

}
