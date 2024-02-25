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

import org.opencastproject.graphql.provider.GraphQLExtensionProvider;

import java.util.List;

import graphql.annotations.processor.GraphQLAnnotations;

public class ExtensionBuilder {

  private GraphQLAnnotations annotations;

  private List<GraphQLExtensionProvider> extensionProviders;

  public void build() {
    extensionProviders.stream()
        .flatMap(e -> e.getExtensions().stream())
        .forEach(annotations::registerTypeExtension);

  }

  public ExtensionBuilder withAnnotations(GraphQLAnnotations annotations) {
    this.annotations = annotations;
    return this;
  }

  public ExtensionBuilder withExtensionProviders(List<GraphQLExtensionProvider> extensionProviders) {
    this.extensionProviders = extensionProviders;
    return this;
  }

}
