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

package org.opencastproject.graphql.datafetcher;

import org.opencastproject.graphql.util.GraphQLObjectMapper;

import java.util.Objects;

import graphql.schema.DataFetchingEnvironment;

public abstract class ParameterDataFetcher<T> implements ContextDataFetcher<T> {

  protected <E> E parseObjectParam(String name, Class<E> clazz, final DataFetchingEnvironment environment) {
    final Object param = environment.getArgument(name);
    if (param == null) {
      return null;
    }
    return GraphQLObjectMapper.newInstance().convertValue(param, clazz);
  }

  protected <K> K parseParam(final String name, K defaultValue, final DataFetchingEnvironment environment) {
    return Objects.requireNonNullElse(environment.getArgument(name), defaultValue);
  }

}
