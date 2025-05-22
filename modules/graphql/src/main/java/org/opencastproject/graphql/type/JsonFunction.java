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

package org.opencastproject.graphql.type;

import java.lang.reflect.AnnotatedType;

import graphql.annotations.processor.ProcessingElementsContainer;
import graphql.annotations.processor.typeFunctions.TypeFunction;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLType;

public class JsonFunction implements TypeFunction {
  @Override
  public String getTypeName(Class<?> aClass, AnnotatedType annotatedType) {
    return ExtendedScalars.Json.getName();
  }

  @Override
  public boolean canBuildType(Class<?> aClass, AnnotatedType annotatedType) {
    return aClass == Object.class;
  }

  @Override
  public GraphQLType buildType(boolean input, Class<?> aClass, AnnotatedType annotatedType,
      ProcessingElementsContainer container) {
    return ExtendedScalars.Json;
  }

}
