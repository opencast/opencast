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

import org.opencastproject.util.data.Function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.functions.Misc.cast;

public final class JsonArr implements Iterable<JsonVal> {
  private final List<Object> val;

  public JsonArr(List arr) {
    this.val = new ArrayList<Object>(arr);
  }

  public JsonVal val(int index) {
    return new JsonVal(val.get(index));
  }

  public JsonObj obj(int index) {
    return new JsonObj((Map) val.get(index));
  }

  public JsonArr arr(int index) {
    return new JsonArr((List) val.get(index));
  }

  public <A> List<A> as(Function<Object, A> converter) {
    return mlist(val).map(converter).value();
  }

  public List<JsonVal> get() {
    return mlist(val).map(JsonVal.asJsonVal).value();
  }

  @Override
  public Iterator<JsonVal> iterator() {
    return mlist(val).map(JsonVal.asJsonVal).iterator();
  }

  private static <A> Function<Object, A> caster(final Class<A> ev) {
    return new Function<Object, A>() {
      @Override public A apply(Object o) {
        return cast(o, ev);
      }
    };
  }
}
