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

package org.opencastproject.graphql.provider;

import org.opencastproject.graphql.util.AnnotationScanner;

import java.util.Collection;

import graphql.annotations.annotationTypes.GraphQLTypeExtension;

/**
 * This interface extends the GraphQLProvider interface and provides a method for getting extensions.
 * The extensions are a collection of classes that are annotated with the GraphQLTypeExtension annotation.
 */
public interface GraphQLExtensionProvider extends GraphQLProvider {

  /**
   * Provides a default implementation for getting extensions.
   * It uses the AnnotationScanner to find and return a collection of classes that are annotated
   * with the GraphQLTypeExtension annotation.
   *
   * @return A collection of classes that are annotated with the GraphQLTypeExtension annotation.
   */
  default Collection<Class<?>> getExtensions() {
    return AnnotationScanner.findAnnotatedClasses(this.getClass(), GraphQLTypeExtension.class);
  }

}
