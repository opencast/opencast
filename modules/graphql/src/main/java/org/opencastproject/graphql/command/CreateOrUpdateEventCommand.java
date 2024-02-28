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

package org.opencastproject.graphql.command;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.graphql.event.GqlEvent;
import org.opencastproject.graphql.exception.GraphQLNotFoundException;
import org.opencastproject.graphql.exception.GraphQLRuntimeException;
import org.opencastproject.graphql.exception.GraphQLUnauthorizedException;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.execution.context.OpencastContextManager;
import org.opencastproject.graphql.type.input.GqlCommonEventMetadataInput;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.MetadataList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import java.util.Map;

public class CreateOrUpdateEventCommand extends AbstractCommand<GqlEvent> {

  private final String eventId;

  public CreateOrUpdateEventCommand(final Builder builder) {
    super(builder);
    this.eventId = builder.eventId;
  }

  @Override
  public GqlEvent execute() {
    OpencastContext context = OpencastContextManager.getCurrentContext();
    final ElasticsearchIndex index = context.getService(ElasticsearchIndex.class);
    final IndexService indexService = context.getService(IndexService.class);

    final Map<String, Object> eventMetadata = environment.getArgument("metadata");

    try {
      indexService.updateEventMetadata(eventId, createMetadataList(eventMetadata, indexService), index);
    } catch (IndexServiceException | SearchIndexException e) {
      throw new GraphQLRuntimeException(e);
    } catch (UnauthorizedException e) {
      throw new GraphQLUnauthorizedException(e.getMessage());
    } catch (NotFoundException e) {
      throw new GraphQLNotFoundException(e.getMessage());
    }

    try {
      return new GqlEvent(indexService.getEvent(eventId, index).get());
    } catch (SearchIndexException e) {
      throw new GraphQLRuntimeException(e);
    }
  }

  private MetadataList createMetadataList(Map<String, Object> eventMetadata, IndexService indexService) {
    CommonEventCatalogUIAdapter adapter = (CommonEventCatalogUIAdapter) indexService
        .getCommonEventCatalogUIAdapter();

    MetadataList list = new MetadataList();
    list.add(adapter, adapter.getRawFields());

    final MediaPackageElementFlavor flavor = MediaPackageElementFlavor
        .parseFlavor("dublincore/episode");

    final DublinCoreMetadataCollection collection = list.getMetadataByFlavor(flavor.toString());

    eventMetadata.keySet().forEach(k -> {
      final MetadataField target = collection.getOutputFields().get(k);
      target.setValue(eventMetadata.get(k));
    });

    return list;
  }

  public static Builder create(String eventId, GqlCommonEventMetadataInput eventMetadataInput) {
    return new Builder(eventId, eventMetadataInput);
  }

  public static class Builder extends AbstractCommand.Builder<GqlEvent> {

    private final String eventId;

    private final GqlCommonEventMetadataInput eventMetadataInput;

    public Builder(String eventId, GqlCommonEventMetadataInput eventMetadataInput) {
      this.eventId = eventId;
      this.eventMetadataInput = eventMetadataInput;
    }

    @Override
    public void validate() {
      super.validate();
      if (eventId == null || eventId.isEmpty()) {
        throw new IllegalStateException("Event ID cannot be null or empty");
      }
      if (eventMetadataInput == null) {
        throw new IllegalStateException("Event metadata cannot be null");
      }
    }

    @Override
    public CreateOrUpdateEventCommand build() {
      validate();
      return new CreateOrUpdateEventCommand(this);
    }
  }

}
