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

@GraphQLName(SeriesOrderByInput.TYPE_NAME)
@GraphQLDescription("Fields to sort series by. The order of the sort is the same as the order of the fields.")
public class SeriesOrderByInput {

  public static final String TYPE_NAME = "SeriesOrderByInput";

  @GraphQLField
  @GraphQLName("title")
  private OrderDirection title;

  @GraphQLField
  @GraphQLName("subject")
  private OrderDirection subject;

  @GraphQLField
  @GraphQLName("creator")
  private OrderDirection creator;

  @GraphQLField
  @GraphQLName("publishers")
  private OrderDirection publishers;

  @GraphQLField
  @GraphQLName("contributors")
  private OrderDirection contributors;

  @GraphQLField
  @GraphQLName("description")
  private OrderDirection description;

  @GraphQLField
  @GraphQLName("language")
  private OrderDirection language;

  @GraphQLField
  @GraphQLName("rightHolder")
  private OrderDirection rightHolder;

  @GraphQLField
  @GraphQLName("license")
  private OrderDirection license;

  @GraphQLField
  @GraphQLName("created")
  private OrderDirection created;

  public SeriesOrderByInput() {

  }

  public SeriesOrderByInput(
      @GraphQLName("title") OrderDirection title,
      @GraphQLName("subject") OrderDirection subject,
      @GraphQLName("creator") OrderDirection creator,
      @GraphQLName("publishers") OrderDirection publishers,
      @GraphQLName("contributors") OrderDirection contributors,
      @GraphQLName("description") OrderDirection description,
      @GraphQLName("language") OrderDirection language,
      @GraphQLName("rightHolder") OrderDirection rightHolder,
      @GraphQLName("license") OrderDirection license,
      @GraphQLName("created") OrderDirection created) {
    this.title = title;
    this.subject = subject;
    this.creator = creator;
    this.publishers = publishers;
    this.contributors = contributors;
    this.description = description;
    this.language = language;
    this.rightHolder = rightHolder;
    this.license = license;
    this.created = created;
  }

  public OrderDirection getTitle() {
    return title;
  }

  public OrderDirection getSubject() {
    return subject;
  }

  public OrderDirection getCreator() {
    return creator;
  }

  public OrderDirection getPublishers() {
    return publishers;
  }

  public OrderDirection getContributors() {
    return contributors;
  }

  public OrderDirection getDescription() {
    return description;
  }

  public OrderDirection getLanguage() {
    return language;
  }

  public OrderDirection getRightHolder() {
    return rightHolder;
  }

  public OrderDirection getLicense() {
    return license;
  }

  public OrderDirection getCreated() {
    return created;
  }
}
