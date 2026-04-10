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

package org.opencastproject.graphql.type.input;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(PlaylistOrderByInput.TYPE_NAME)
@GraphQLDescription("Fields to sort playlists by. The order of the sort is the same as the order of the fields.")
public class PlaylistOrderByInput {

  public static final String TYPE_NAME = "PlaylistOrderByInput";

  @GraphQLField
  @GraphQLName("title")
  private OrderDirection title;

  @GraphQLField
  @GraphQLName("description")
  private OrderDirection description;

  @GraphQLField
  @GraphQLName("creator")
  private OrderDirection creator;

  @GraphQLField
  @GraphQLName("organization")
  private OrderDirection organization;

  @GraphQLField
  @GraphQLName("updated")
  private OrderDirection updated;

  @GraphQLField
  @GraphQLName("deletionDate")
  private OrderDirection deletionDate;

  public PlaylistOrderByInput() {

  }

  public PlaylistOrderByInput(
      @GraphQLName("title") OrderDirection title,
      @GraphQLName("description") OrderDirection description,
      @GraphQLName("creator") OrderDirection creator,
      @GraphQLName("organization") OrderDirection organization,
      @GraphQLName("updated") OrderDirection updated,
      @GraphQLName("deletionDate") OrderDirection deletionDate) {
    this.title = title;
    this.description = description;
    this.creator = creator;
    this.organization = organization;
    this.updated = updated;
    this.deletionDate = deletionDate;
  }

  public OrderDirection getTitle() {
    return title;
  }

  public OrderDirection getDescription() {
    return description;
  }

  public OrderDirection getCreator() {
    return creator;
  }

  public OrderDirection getOrganization() {
    return organization;
  }

  public OrderDirection getUpdated() {
    return updated;
  }

  public OrderDirection getDeletionDate() {
    return deletionDate;
  }

}
