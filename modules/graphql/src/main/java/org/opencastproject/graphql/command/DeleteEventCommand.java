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

import org.opencastproject.graphql.event.GqlDeleteEventPayload;
import org.opencastproject.graphql.exception.GraphQLNotFoundException;
import org.opencastproject.graphql.exception.GraphQLUnauthorizedException;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.execution.context.OpencastContextManager;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

public class DeleteEventCommand extends AbstractCommand<GqlDeleteEventPayload> {

  private final String id;

  public DeleteEventCommand(final Builder builder) {
    super(builder);
    this.id = builder.id;
  }

  @Override
  public GqlDeleteEventPayload execute() {
    OpencastContext context = OpencastContextManager.getCurrentContext();
    final IndexService indexService = context.getService(IndexService.class);
    try {
      indexService.removeEvent(this.id);
    } catch (UnauthorizedException e) {
      throw new GraphQLUnauthorizedException(e.getMessage());
    } catch (NotFoundException e) {
      throw new GraphQLNotFoundException(e.getMessage());
    } return new GqlDeleteEventPayload(id);
  }

  public static Builder create(String id) {
    return new Builder(id);
  }

  public static class Builder extends AbstractCommand.Builder<GqlDeleteEventPayload> {

    private final String id;

    public Builder(String id) {
      this.id = id;
    }

    @Override
    public void validate() {
      super.validate();

      if (id == null) {
        throw new IllegalArgumentException("Id can not be null.");
      }
    }

    @Override
    public DeleteEventCommand build() {
      validate();
      return new DeleteEventCommand(this);
    }
  }

}
