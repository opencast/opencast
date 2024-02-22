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

package org.opencastproject.graphql.type.output;

import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Publication;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

public class GqlPublication implements GqlMediaPackageElement {

  private final Publication publication;

  public GqlPublication(Publication publication) {
    this.publication = publication;
  }

  @GraphQLField
  public String uri() {
    return publication.getURI().toString();
  }

  @GraphQLField
  public String channel() {
    return publication.getChannel();
  }

  @GraphQLField
  @GraphQLDescription("Return tracks filterable by tags")
  public List<GqlTrack> tracks(@GraphQLName("tags") List<String> tags) {
    return Arrays.stream(publication.getTracks())
        .filter(e -> tags == null || Arrays.stream(e.getTags()).anyMatch(tags::contains))
        .map(GqlTrack::new)
        .collect(Collectors.toList());
  }

  @Override
  public MediaPackageElement getElement() {
    return publication;
  }
}
