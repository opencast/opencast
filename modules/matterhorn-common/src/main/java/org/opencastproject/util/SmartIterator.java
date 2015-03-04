/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility class for applying limit and offset to a map or collection
 */
public class SmartIterator<A> {
  private int limit;
  private int offset;
  private Iterator<?> iterator;

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
    iterator = map.entrySet().iterator();

    Map<String, A> filteredMap = new HashMap<String, A>();
    int i = 0;
    while (isRecordRequired(filteredMap.size())) {
      Entry<String, A> item = (Entry<String, A>) iterator.next();
      if (i++ >= offset) {
        filteredMap.put(item.getKey(), item.getValue());
      }
    }
    return filteredMap;
  }

  private boolean isRecordRequired(int filteredMapSize) {
    return (filteredMapSize < limit || limit == 0) && iterator.hasNext();
  }

  /**
   * Apply limit and offset to a collection of type {@link A}
   *
   * @param unfilteredCollection
   *          the collection
   * @return the filtered list
   */
  public List<A> applyLimitAndOffset(Collection<A> unfilteredCollection) {
    iterator = unfilteredCollection.iterator();
    List<A> filteredList = new ArrayList<A>();
    int i = 0;
    while (isRecordRequired(filteredList.size())) {
      A nextItem = (A) iterator.next();
      if (i++ >= offset) {
        filteredList.add(nextItem);
      }
    }
    return filteredList;
  }
}
