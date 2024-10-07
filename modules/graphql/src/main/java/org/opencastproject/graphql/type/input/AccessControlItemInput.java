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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import graphql.annotations.annotationTypes.GraphQLConstructor;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(AccessControlItemInput.TYPE_NAME)
public class AccessControlItemInput {

  public static final String TYPE_NAME = "AccessControlItemInput";

  @GraphQLField
  @GraphQLName("role")
  private String role;

  @GraphQLField
  @GraphQLName("action")
  private Set<String> action = Collections.emptySet();


  public AccessControlItemInput() {
  }

  @GraphQLConstructor
  public AccessControlItemInput(
      @GraphQLName("role") String role,
      @GraphQLName("action") List<String> action) {
    this.role = role;
    this.action = new HashSet<>(action);
  }

  public String getRole() {
    return role;
  }

  public Set<String> getAction() {
    return Collections.unmodifiableSet(action);
  }

}
