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
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.util.EventUtils;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.security.api.SecurityService;

import com.entwinemedia.fn.data.Opt;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import graphql.schema.DataFetchingEnvironment;

public class CommonEventMetadataDataFetcher implements ContextDataFetcher<Map<String, Object>> {

  @Override
  public Map<String, Object> get(OpencastContext opencastContext, DataFetchingEnvironment dataFetchingEnvironment) {
    String eventId = ((GqlEvent)dataFetchingEnvironment.getSource()).id();
    SecurityService securityService = opencastContext.getService(SecurityService.class);
    ElasticsearchIndex searchIndex = opencastContext.getService(ElasticsearchIndex.class);
    IndexService indexService = opencastContext.getService(IndexService.class);

    try {
      GqlEvent e = getEvent(searchIndex, eventId, securityService);
      Event event = getEventFromIndexService(indexService, eventId, searchIndex);
      EventCatalogUIAdapter eventCatalogUiAdapter = indexService.getCommonEventCatalogUIAdapter();
      DublinCoreMetadataCollection collection = EventUtils.getEventMetadata(event, eventCatalogUiAdapter);

      return getOutputFields(collection);
    } catch (SearchIndexException | ParseException | IndexServiceException e) {
      throw new GraphQLRuntimeException(OpencastErrorType.InternalError, e);
    }
  }

  private GqlEvent getEvent(ElasticsearchIndex searchIndex, String eventId, SecurityService securityService)
          throws SearchIndexException {
    return searchIndex.getEvent(eventId, securityService.getOrganization().toString(), securityService.getUser())
        .map(GqlEvent::new).orElseThrow(() -> new GraphQLNotFoundException(
                String.format("Could not resolve to a %s with the id of %s", GqlEvent.TYPE_NAME, eventId)));
  }

  private Event getEventFromIndexService(IndexService indexService, String eventId, ElasticsearchIndex searchIndex)
          throws IndexServiceException, SearchIndexException {
    Opt<Event> opt = indexService.getEvent(eventId, searchIndex);
    if (opt.isEmpty()) {
      throw new GraphQLNotFoundException(
          String.format("Could not resolve to a %s with the id of %s", GqlEvent.TYPE_NAME, eventId));
    }
    return opt.get();
  }

  private Map<String, Object> getOutputFields(DublinCoreMetadataCollection collection) {
    Map<String, Object> result = new HashMap<>();
    collection.getOutputFields().values().forEach(f -> {
      Object value = f.getValue();
      String outputID = f.getOutputID();
      MetadataField.Type type = f.getType();

      if (type.equals(MetadataField.Type.DATE)) {
        Date date = (Date) value;
        if (date != null) {
          DateFormat df = new SimpleDateFormat(f.getPattern());
          df.setTimeZone(TimeZone.getTimeZone("UTC"));
          result.put(outputID, df.format(date));
        }
      } else if (type.equals(MetadataField.Type.DURATION)) {
        if (value instanceof String) {
          result.put(outputID, Long.parseLong(((String) value).isEmpty() ? "0" : (String) value));
        } else if (value instanceof Long || value instanceof Integer) {
          result.put(outputID, value);
        }
      } else {
        result.put(outputID, value);
      }
    });
    return result;
  }
}
