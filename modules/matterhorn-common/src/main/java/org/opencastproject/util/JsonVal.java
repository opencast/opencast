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

package org.opencastproject.util;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.functions.Options;
import org.opencastproject.util.data.functions.Strings;

import java.util.List;
import java.util.Map;

import static org.opencastproject.util.data.functions.Misc.cast;

public final class JsonVal {
  private final Object val;

  public JsonVal(Object val) {
    this.val = val;
  }

  public <A> A as(Function<Object, ? extends A> converter) {
    return converter.apply(val);
  }

  public boolean isObj() {
    return val instanceof Map;
  }

  public boolean isArr() {
    return val instanceof List;
  }

  public Object get() {
    return val;
  }

  public static final Function<Object, String> asString = caster(String.class);
  public static final Function<Object, Integer> asInteger = caster(Integer.class);
  public static final Function<Object, Long> asLong = caster(Long.class);
  public static final Function<Object, Float> asFloat = caster(Float.class);
  public static final Function<Object, Double> asDouble = caster(Double.class);
  public static final Function<Object, Boolean> asBoolean = caster(Boolean.class);
  public static final Function<Object, JsonObj> asJsonObj = new Function<Object, JsonObj>() {
    @Override public JsonObj apply(Object o) {
      return JsonObj.jsonObj((Map) o);
    }
  };
  public static final Function<Object, JsonArr> asJsonArr = new Function<Object, JsonArr>() {
    @Override public JsonArr apply(Object o) {
      return new JsonArr((List) o);
    }
  };
  public static final Function<Object, JsonVal> asJsonVal = new Function<Object, JsonVal>() {
    @Override public JsonVal apply(Object o) {
      return new JsonVal(o);
    }
  };
  public static final Function<Object, Integer> stringAsInteger = Options.<Integer>getF().o(Strings.toInt.o(asString));

  private static <A> Function<Object, A> caster(final Class<A> ev) {
    return new Function<Object, A>() {
      @Override public A apply(Object o) {
        return cast(o, ev);
      }
    };
  }
}
