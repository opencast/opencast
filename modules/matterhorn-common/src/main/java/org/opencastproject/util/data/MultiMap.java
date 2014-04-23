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
package org.opencastproject.util.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MultiMap<A, B> {
  private final Map<A, List<B>> map;

  public MultiMap(Map<A, List<B>> map) {
    this.map = map;
  }

  public static <A, B> MultiMap<A, B> multiHashMapWithArrayList() {
    return new MultiMap<A, B>(new HashMap<A, List<B>>()) {
      @Override
      protected List<B> newList() {
        return new ArrayList<B>();
      }
    };
  }

  public Map<A, List<B>> value() {
    return map;
  }

  public MultiMap<A, B> put(A key, B value) {
    List<B> current = map.get(key);
    if (current == null) {
      current = newList();
      map.put(key, current);
    }
    current.add(value);
    return this;
  }

  public MultiMap<A, B> putAll(A key, List<B> values) {
    List<B> current = map.get(key);
    if (current == null) {
      current = newList();
      map.put(key, current);
    }
    current.addAll(values);
    return this;
  }

  protected abstract List<B> newList();
}
