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
import org.opencastproject.graphql.event.GqlEvent;
import org.opencastproject.graphql.exception.GraphQLNotFoundException;
import org.opencastproject.graphql.exception.GraphQLRuntimeException;
import org.opencastproject.graphql.exception.GraphQLUnauthorizedException;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.execution.context.OpencastContextManager;
import org.opencastproject.graphql.type.input.AccessControlListInput;
import org.opencastproject.graphql.util.GraphQLObjectMapper;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

public class UpdateEventAclCommand extends AbstractCommand<GqlEvent> {

  private final String eventId;

  public UpdateEventAclCommand(final Builder builder) {
    super(builder);
    this.eventId = builder.eventId;
  }

  @Override
  public GqlEvent execute() {
    OpencastContext context = OpencastContextManager.getCurrentContext();
    final ElasticsearchIndex index = context.getService(ElasticsearchIndex.class);
    final IndexService indexService = context.getService(IndexService.class);

    final AccessControlListInput aclInput = GraphQLObjectMapper.newInstance()
        .convertValue(environment.getArgument("acl"), AccessControlListInput.class);

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
      indexService.updateEventAcl(eventId, acl, index);
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


  public static Builder create(String eventId) {
    return new Builder(eventId);
  }

  public static class Builder extends AbstractCommand.Builder<GqlEvent> {

    private final String eventId;

    public Builder(String eventId) {
      this.eventId = eventId;
    }

    @Override
    public void validate() {
      super.validate();
      if (eventId == null || eventId.isEmpty()) {
        throw new IllegalStateException("Event ID cannot be null or empty");
      }
    }

    @Override
    public UpdateEventAclCommand build() {
      validate();
      return new UpdateEventAclCommand(this);
    }
  }

}
