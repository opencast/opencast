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

package org.opencastproject.graphql.listprovider;

import org.opencastproject.graphql.defaultvalue.DefaultFalse;
import org.opencastproject.graphql.exception.GraphQLNotFoundException;
import org.opencastproject.graphql.exception.GraphQLRuntimeException;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.execution.context.OpencastContextManager;
import org.opencastproject.graphql.type.output.Query;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.impl.ListProviderNotFoundException;
import org.opencastproject.list.impl.ResourceListQueryImpl;
import org.opencastproject.list.query.StringListFilter;

import graphql.annotations.annotationTypes.GraphQLDefaultValue;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.schema.DataFetchingEnvironment;

@GraphQLTypeExtension(Query.class)
public final class ListProviderQueryExtension {

  private ListProviderQueryExtension() {
  }
  @GraphQLField
  @GraphQLNonNull
  @GraphQLDescription("Returns list provider")
  public static GqlListProvider listProvider(
      @GraphQLName("name") @GraphQLNonNull String name,
      @GraphQLName("limit") Integer limit,
      @GraphQLName("offset") Integer offset,
      @GraphQLName("filter") String filter,
      @GraphQLName("inverse") @GraphQLDescription("Exchange") @GraphQLDefaultValue(DefaultFalse.class) Boolean inverse,
      final DataFetchingEnvironment environment) {
    final OpencastContext context = OpencastContextManager.enrichContext(environment);

    var listProvider = context.getService(ListProvidersService.class);
    var resourceListQuery = new ResourceListQueryImpl();
    resourceListQuery.setLimit(limit);
    resourceListQuery.setOffset(offset);
    addRequestFiltersToQuery(filter, resourceListQuery);

    try {
      return new GqlListProvider(listProvider.getList(name, resourceListQuery, inverse),
          listProvider.isTranslatable(name));
    } catch (ListProviderNotFoundException e) {
      throw new GraphQLNotFoundException(e.getMessage());
    } catch (ListProviderException e) {
      throw new GraphQLRuntimeException(e);
    }
  }

  private static void addRequestFiltersToQuery(String filterString, ResourceListQueryImpl query) {
    if (filterString == null) {
      return;
    }

    for (String filter : filterString.split(",")) {
      String[] splitFilter = filter.split(":", 2);
      if (splitFilter.length == 2) {
        query.addFilter(new StringListFilter(splitFilter[0].trim(), splitFilter[1].trim()));
      }
    }
  }

}
