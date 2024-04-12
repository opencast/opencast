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
import org.opencastproject.graphql.util.GraphQLObjectMapper;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;

public class UpdateSeriesAclCommand extends AbstractCommand<GqlSeries> {

  private final String seriesId;

  public UpdateSeriesAclCommand(final Builder builder) {
    super(builder);
    this.seriesId = builder.seriesId;
  }

  @Override
  public GqlSeries execute() {
    OpencastContext context = OpencastContextManager.getCurrentContext();
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
      ElasticsearchIndex index = context.getService(ElasticsearchIndex.class);
      return new GqlSeries(
          index.getSeries(seriesId, context.getOrganization().getId(), context.getUser()).get()
      );
    } catch (SearchIndexException e) {
      throw new GraphQLRuntimeException(e);
    }
  }

  public static Builder create(String eventId) {
    return new Builder(eventId);
  }

  public static class Builder extends AbstractCommand.Builder<GqlSeries> {

    private final String seriesId;

    public Builder(String seriesId) {
      this.seriesId = seriesId;
    }

    @Override
    public void validate() {
      super.validate();
      if (seriesId == null || seriesId.isEmpty()) {
        throw new IllegalStateException("Series ID cannot be null or empty");
      }
    }

    @Override
    public UpdateSeriesAclCommand build() {
      validate();
      return new UpdateSeriesAclCommand(this);
    }
  }

}
