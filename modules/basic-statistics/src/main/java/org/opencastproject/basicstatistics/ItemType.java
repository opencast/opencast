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
package org.opencastproject.basicstatistics;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Type of item that a statistics event refers to.
 *
 * Each type has a database ID, a fixed integer that is used for storage, and an API name, which is used in the HTTP
 * APIs. The Java constant names are neither, so they can be renamed freely.
 *
 * Assigned database IDs must never be changed or reused, as that would require a database migration. Changing an API
 * name is a breaking API change.
 */
public enum ItemType {
  VIDEO(1, "video"),
  SERIES(2, "series"),
  PLAYLIST(3, "playlist");

  private static final Map<Short, ItemType> BY_DB_ID = Arrays.stream(values())
      .collect(Collectors.toMap(ItemType::getDbId, Function.identity()));
  private static final Map<String, ItemType> BY_API_NAME = Arrays.stream(values())
      .collect(Collectors.toMap(ItemType::getApiName, Function.identity()));

  private final short dbId;
  private final String apiName;

  ItemType(int dbId, String apiName) {
    this.dbId = (short) dbId;
    this.apiName = apiName;
  }

  public short getDbId() {
    return dbId;
  }

  public String getApiName() {
    return apiName;
  }

  /**
   * @return the type with the given database ID, or empty if there is none
   */
  public static Optional<ItemType> fromDbId(short dbId) {
    return Optional.ofNullable(BY_DB_ID.get(dbId));
  }

  /**
   * @return the type with the given API name, or empty if there is none
   */
  public static Optional<ItemType> fromApiName(String apiName) {
    return Optional.ofNullable(BY_API_NAME.get(apiName));
  }
}
