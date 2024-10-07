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

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.graphql.exception.GraphQLNotFoundException;
import org.opencastproject.graphql.exception.GraphQLRuntimeException;
import org.opencastproject.graphql.exception.GraphQLUnauthorizedException;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.execution.context.OpencastContextManager;
import org.opencastproject.graphql.series.GqlSeries;
import org.opencastproject.graphql.type.input.AccessControlListInput;
import org.opencastproject.graphql.type.input.GqlCommonSeriesMetadataInput;
import org.opencastproject.graphql.util.GraphQLObjectMapper;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.series.CommonSeriesCatalogUIAdapter;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.MetadataList;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;

import java.util.Map;

public class UpdateSeriesCommand extends AbstractCommand<GqlSeries> {

  private final String seriesId;

  public UpdateSeriesCommand(final Builder builder) {
    super(builder);
    this.seriesId = builder.seriesId;
  }

  @Override
  public GqlSeries execute() {
    OpencastContext context = OpencastContextManager.getCurrentContext();
    final ElasticsearchIndex index = context.getService(ElasticsearchIndex.class);
    final IndexService indexService = context.getService(IndexService.class);

    final Map<String, Object> seriesMetadata = environment.getArgument("metadata");
    try {
      indexService.updateAllSeriesMetadata(this.seriesId,
          createMetadataList(seriesMetadata, indexService), index);
    } catch (IndexServiceException e) {
      throw new GraphQLRuntimeException(e);
    } catch (UnauthorizedException e) {
      throw new GraphQLUnauthorizedException(e.getMessage());
    } catch (NotFoundException e) {
      throw new GraphQLNotFoundException(e.getMessage());
    }

    final SeriesService seriesService = context.getService(SeriesService.class);

    final AccessControlListInput aclInput = GraphQLObjectMapper.newInstance()
        .convertValue(environment.getArgument("acl"), AccessControlListInput.class);
    if (aclInput != null) {
      try {
        AccessControlList acl = new AccessControlList();
        for (var entry : aclInput.getEntries()) {
          for (var action : entry.getAction()) {
            acl.getEntries().add(new AccessControlEntry(entry.getRole(), action, true));
          }
        }

        if (aclInput.getManagedAclId() != null) {
          AclService aclService = context.getService(AclServiceFactory.class)
              .serviceFor(context.getService(SecurityService.class).getOrganization());
          aclService.getAcl(aclInput.getManagedAclId())
              .ifPresent(value -> acl.merge(value.getAcl()));
        }
        seriesService.updateAccessControl(seriesId, acl);
      } catch (UnauthorizedException e) {
        throw new GraphQLUnauthorizedException(e.getMessage());
      } catch (NotFoundException e) {
        throw new GraphQLNotFoundException(e.getMessage());
      } catch (SeriesException e) {
        throw new GraphQLRuntimeException(e);
      }
    }

    try {
      return new GqlSeries(
          index.getSeries(seriesId, context.getOrganization().getId(), context.getUser()).get()
      );
    } catch (SearchIndexException e) {
      throw new GraphQLRuntimeException(e);
    }
  }

  private MetadataList createMetadataList(Map<String, Object> seriesMetadata, IndexService indexService) {
    CommonSeriesCatalogUIAdapter adapter = (CommonSeriesCatalogUIAdapter) indexService
        .getCommonSeriesCatalogUIAdapter();

    MetadataList list = new MetadataList();
    list.add(adapter, adapter.getRawFields());

    final MediaPackageElementFlavor flavor = MediaPackageElementFlavor
        .parseFlavor("dublincore/series");

    final DublinCoreMetadataCollection collection = list.getMetadataByFlavor(flavor.toString());

    seriesMetadata.keySet().forEach(k -> {
      final MetadataField target = collection.getOutputFields().get(k);
      target.setValue(seriesMetadata.get(k));
    });

    return list;
  }

  public static Builder create(String seriesId, GqlCommonSeriesMetadataInput seriesMetadataInput) {
    return new Builder(seriesId, seriesMetadataInput);
  }

  public static class Builder extends AbstractCommand.Builder<GqlSeries> {

    private final String seriesId;

    private final GqlCommonSeriesMetadataInput seriesMetadataInput;


    public Builder(String seriesId, GqlCommonSeriesMetadataInput seriesMetadataInput) {
      this.seriesId = seriesId;
      this.seriesMetadataInput = seriesMetadataInput;
    }

    @Override
    public void validate() {
      super.validate();
      if (seriesId == null || seriesId.isEmpty()) {
        throw new IllegalStateException("Series ID cannot be null or empty");
      }
      if (seriesMetadataInput == null) {
        throw new IllegalStateException("Series metadata cannot be null");
      }
    }

    @Override
    public UpdateSeriesCommand build() {
      validate();
      return new UpdateSeriesCommand(this);
    }
  }

}
