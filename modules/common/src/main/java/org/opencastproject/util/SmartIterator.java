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

package org.opencastproject.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for applying limit and offset to a map or collection
 */
public class SmartIterator<A> {
  private final int limit;
  private final int offset;

  public SmartIterator(int limit, int offset) {
    this.limit = limit;
    this.offset = offset;
  }

  /**
   * Apply limit and offset to a map of value type {@link A}
   *
   * @param map
   *          the map
   * @return the filtered map
   */
  public Map<String, A> applyLimitAndOffset(Map<String, A> map) {
    return limited(map.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
  }

  /**
   * Apply limit and offset to a collection of type {@link A}
   *
   * @param unfilteredCollection
   *          the collection
   * @return the filtered list
   */
  public List<A> applyLimitAndOffset(Collection<A> unfilteredCollection) {
    return limited(unfilteredCollection.stream()).collect(Collectors.toCollection(ArrayList::new));
  }

  /** Skip {@link #offset} elements, then take at most {@link #limit} of what remains ({@code 0} means no cap). */
  private <T> Stream<T> limited(Stream<T> stream) {
    if (limit < 0) {
      return Stream.empty();
    }
    Stream<T> skipped = stream.skip(Math.max(offset, 0));
    return limit == 0 ? skipped : skipped.limit(limit);
  }
}
