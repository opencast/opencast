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
package org.opencastproject.graphql.playlist.type.input;

import graphql.annotations.annotationTypes.GraphQLConstructor;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(PlaylistMetadataInput.TYPE_NAME)
@GraphQLDescription("Input type for playlist metadata, including title and description.")
public class PlaylistMetadataInput {

  public static final String TYPE_NAME = "PlaylistMetadataInput";

  @GraphQLField
  @GraphQLName("title")
  private String title;

  @GraphQLField
  @GraphQLName("description")
  private String description;

  public PlaylistMetadataInput() {
  }

  @GraphQLConstructor
  public PlaylistMetadataInput(
      @GraphQLName("title") String title,
      @GraphQLName("description") String description) {
    this.title = title;
    this.description = description;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

}
