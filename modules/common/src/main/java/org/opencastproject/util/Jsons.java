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

import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Prelude;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/** JSON builder based on json-simple. */
public final class Jsons {
  private Jsons() {
  }

  /** Check if a value is not {@link #ZERO_VAL}. */
  public static Boolean notZero(Val val) {
    return !ZERO_VAL.equals(val);
  }

  /** Get the value from a property. */
  public static Val getVal(Prop prop) {
    return prop.getVal();
  }

  /** JSON null. */
  public static final Val NULL = new Val() {
  };

  /** Identity for {@link Val values}. */
  public static final Val ZERO_VAL = new Val() {
  };

  /** Identity for {@link Obj objects}. */
  public static final Obj ZERO_OBJ = obj();

  /** Identity for {@link Arr arrays}. */
  public static final Arr ZERO_ARR = arr();

  public static final class Prop {
    private final String name;
    private final Val val;

    private Prop(String name, Val val) {
      this.name = name;
      this.val = val;
    }

    public String getName() {
      return name;
    }

    public Val getVal() {
      return val;
    }
  }

  // sum type
  public abstract static class Val {
  }

  private static final class SVal extends Val {
    private final Object val;

    private SVal(Object val) {
      this.val = val;
    }

    public Object getVal() {
      return val;
    }
  }

  public static final class Obj extends Val {
    private final List<Prop> props;

    private Obj(List<Prop> props) {
      this.props = props;
    }

    public List<Prop> getProps() {
      return props;
    }

    public Obj append(Obj o) {
      if (!ZERO_OBJ.equals(o))
        return new Obj(Collections.<Prop, List>concat(props, o.getProps()));
      else
        return o;
    }

    public String toJson() {
      return Jsons.toJson(this);
    }
  }

  public static final class Arr extends Val {
    private final List<Val> vals;

    public Arr(List<Val> vals) {
      this.vals = vals;
    }

    public List<Val> getVals() {
      return vals;
    }

    public String toJson() {
      return Jsons.toJson(this);
    }
  }

  //

  public static String toJson(Obj obj) {
    return toJsonSimple(obj).toString();
  }

  public static String toJson(Arr arr) {
    return toJsonSimple(arr).toString();
  }

  private static JSONObject toJsonSimple(Obj obj) {
    JSONObject jo = new JSONObject();
    for (Prop prop : obj.getProps()) {
      jo.put(prop.getName(), toJsonSimple(prop.getVal()));
    }
    return jo;
  }

  private static JSONArray toJsonSimple(Arr arr) {
    JSONArray ja = new JSONArray();
    for (Val val : arr.getVals()) {
      ja.add(toJsonSimple(val));
    }
    return ja;
  }

  private static Object toJsonSimple(Val val) {
    if (val instanceof SVal) {
      return ((SVal) val).getVal();
    }
    if (val instanceof Obj) {
      return toJsonSimple((Obj) val);
    }
    if (val instanceof Arr) {
      return toJsonSimple((Arr) val);
    }
    if (val.equals(NULL)) {
      return null;
    }
    return Prelude.unexhaustiveMatch();
  }

  /** Create an object. */
  public static Obj obj(Prop... ps) {
    List<Prop> filtered = Arrays.stream(ps)
        .filter(p -> notZero(getVal(p)))
        .toList();
    return new Obj(filtered);
  }

  /** Create an array. */
  public static Arr arr(Val... vs) {
    List<Val> filtered = Arrays.stream(vs)
        .filter(Jsons::notZero)
        .toList();
    return new Arr(filtered);
  }

  /** Create an array. */
  public static Arr arr(List<Val> vs) {
    List<Val> filtered = vs.stream()
        .filter(Jsons::notZero)
        .toList();
    return new Arr(filtered);
  }

  public static Val v(Number v) {
    return new SVal(v);
  }

  public static Val v(String v) {
    return new SVal(v);
  }

  public static Val stringVal(String s) {
    return v(s);
  }

  public static Val v(Boolean v) {
    return new SVal(v);
  }

  public static Val v(Date v) {
    return new SVal(DateTimeSupport.toUTC(v.getTime()));
  }

  /** Create a property. */
  public static Prop p(String key, Val val) {
    return new Prop(key, val);
  }

  /** Create a property. Passing none is like setting {@link #ZERO_VAL} which erases the property. */
  public static Prop p(String key, Optional<Val> val) {
    return new Prop(key, val.orElse(ZERO_VAL));
  }

  /** Create a property. Convenience. */
  public static Prop p(String key, Number value) {
    return new Prop(key, v(value));
  }

  /** Create a property. Convenience. */
  public static Prop p(String key, String value) {
    return new Prop(key, v(value));
  }

  /** Create a property. Convenience. */
  public static Prop p(String key, Boolean value) {
    return new Prop(key, v(value));
  }

  /** Merge a list of objects into one (last one wins). */
  public static Obj append(Obj... os) {
    List<Prop> props = Arrays.stream(os)
        .flatMap(o -> o.getProps().stream())
        .toList();
    return new Obj(props);
  }

  /** Append a list of arrays into one. */
  public static Arr append(Arr... as) {
    List<Val> vals = Arrays.stream(as)
        .flatMap(a -> a.getVals().stream())
        .toList();
    return new Arr(vals);
  }
}
