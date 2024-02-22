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

import org.opencastproject.graphql.datafetcher.ContextDataFetcher;
import org.opencastproject.graphql.exception.GraphQLNotFoundException;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.type.output.GqlSeries;
import org.opencastproject.graphql.type.output.field.GqlJsonMetadataField;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;

import com.entwinemedia.fn.data.Opt;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetchingEnvironment;

public class CommonSeriesMetadataV2DataFetcher implements ContextDataFetcher<Map> {

  @Override
  public Map get(OpencastContext opencastContext, DataFetchingEnvironment dataFetchingEnvironment) {
    String seriesId = ((GqlSeries)dataFetchingEnvironment.getSource()).id();
    IndexService indexService = opencastContext.getService(IndexService.class);

    Map<String, GqlJsonMetadataField> result = new HashMap<>();
    Opt<DublinCoreMetadataCollection> optSeries = indexService.getCommonSeriesCatalogUIAdapter().getFields(seriesId);
    if (optSeries.isNone()) {
      throw new GraphQLNotFoundException("Series with id `" + seriesId + "` not found.");
    }

    optSeries.get().getOutputFields().forEach((key, value) -> result.put(key, new GqlJsonMetadataField(value)));
    return result;
  }
}
