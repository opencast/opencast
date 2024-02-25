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

package org.opencastproject.graphql.datafetcher.series;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.graphql.datafetcher.ContextDataFetcher;
import org.opencastproject.graphql.exception.GraphQLNotFoundException;
import org.opencastproject.graphql.exception.GraphQLRuntimeException;
import org.opencastproject.graphql.exception.OpencastErrorType;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.series.GqlSeries;
import org.opencastproject.security.api.SecurityService;

import java.util.Objects;

import graphql.schema.DataFetchingEnvironment;

public class SeriesDataFetcher implements ContextDataFetcher<GqlSeries> {

  private final String seriesId;

  public SeriesDataFetcher(String seriesId) {
    Objects.requireNonNull(seriesId, "Series identifier must not be null.");
    this.seriesId = seriesId;
  }


  @Override
  public GqlSeries get(OpencastContext opencastContext, DataFetchingEnvironment dataFetchingEnvironment) {
    SecurityService securityService = opencastContext.getService(SecurityService.class);
    ElasticsearchIndex searchIndex = opencastContext.getService(ElasticsearchIndex.class);

    try {
      return searchIndex.getSeries(seriesId, securityService.getOrganization().toString(), securityService.getUser())
          .map(GqlSeries::new).orElseThrow(() -> new GraphQLNotFoundException(
                  String.format("Could not resolve to a %s with the id of %s", GqlSeries.TYPE_NAME, seriesId))
          );
    } catch (SearchIndexException e) {
      throw new GraphQLRuntimeException(OpencastErrorType.InternalError, e);
    }
  }
}
