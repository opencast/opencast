/**
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


package org.opencastproject.userdirectory.brightspace.client.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UsersResponse {

  private PagingInfo pagingInfo;
  private List<BrightspaceUser> items;

  @JsonCreator
  public UsersResponse(@JsonProperty("PagingInfo") PagingInfo pagingInfo,@JsonProperty("Items") List<BrightspaceUser> items) {
    this.pagingInfo = pagingInfo;
    this.items = items;
  }

  public PagingInfo getPagingInfo() {
    return pagingInfo;
  }

  public List<BrightspaceUser> getItems() {
    return items;
  }
}
