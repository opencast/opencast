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

package org.opencastproject.graphql.datafetcher.event;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.graphql.datafetcher.ContextDataFetcher;
import org.opencastproject.graphql.exception.GraphQLNotFoundException;
import org.opencastproject.graphql.exception.GraphQLRuntimeException;
import org.opencastproject.graphql.exception.OpencastErrorType;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.type.output.GqlEvent;
import org.opencastproject.graphql.type.output.field.GqlJsonMetadataField;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.impl.util.EventUtils;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;

import com.entwinemedia.fn.data.Opt;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetchingEnvironment;

public class CommonEventMetadataV2DataFetcher implements ContextDataFetcher<Map<String, GqlJsonMetadataField>> {

  @Override
  public Map<String, GqlJsonMetadataField> get(OpencastContext opencastContext,
      DataFetchingEnvironment dataFetchingEnvironment) {
    String eventId = ((GqlEvent)dataFetchingEnvironment.getSource()).id();
    ElasticsearchIndex searchIndex = opencastContext.getService(ElasticsearchIndex.class);
    IndexService indexService = opencastContext.getService(IndexService.class);
    try {
      Opt<Event> opt = indexService.getEvent(eventId, searchIndex);
      if (opt.isEmpty()) {
        throw new GraphQLNotFoundException(
            String.format("Could not resolve to a %s with the id of %s", GqlEvent.TYPE_NAME, eventId));
      }
      Event event = opt.get();
      EventCatalogUIAdapter eventCatalogUiAdapter = indexService.getCommonEventCatalogUIAdapter();
      Map<String, GqlJsonMetadataField> result = new HashMap<>();
      DublinCoreMetadataCollection collection = EventUtils.getEventMetadata(event, eventCatalogUiAdapter);
      collection.getOutputFields().forEach((key, value) -> result.put(key, new GqlJsonMetadataField(value)));
      return result;
    } catch (SearchIndexException | ParseException e) {
      throw new GraphQLRuntimeException(OpencastErrorType.InternalError, e);
    }
  }
}
