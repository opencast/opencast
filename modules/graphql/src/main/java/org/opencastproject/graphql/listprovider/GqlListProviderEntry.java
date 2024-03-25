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

package org.opencastproject.graphql.listprovider;

import java.util.Map;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(GqlListProviderEntry.TYPE_NAME)
public class GqlListProviderEntry {

  public static final String TYPE_NAME = "ListProviderEntry";

  private final String key;
  private final String value;

  public GqlListProviderEntry(Map.Entry<String, String> entry) {
    this.key = entry.getKey();
    this.value = entry.getValue();
  }

  public GqlListProviderEntry(String key, String value) {
    this.key = key;
    this.value = value;
  }

  @GraphQLField
  public String key() {
    return key;
  }

  @GraphQLField
  public String value() {
    return value;
  }

}
