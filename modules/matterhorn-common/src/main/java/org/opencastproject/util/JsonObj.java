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

import org.apache.commons.io.IOUtils;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.cast;
import static org.opencastproject.util.data.functions.Misc.chuck;

/** Accessor for JSON objects aka maps. */
// todo -- think about using specialized Exception (JsonExcpetion ?); handle parse exception in jsonObj(String)
public final class JsonObj {
  private final Map json;

  /** Create a wrapper for a map. */
  public JsonObj(Map json) {
    this.json = json;
  }

  /** Constructor function. */
  public static JsonObj jsonObj(Map json) {
    return new JsonObj(json);
  }

  /** Create a JsonObj from a JSON string. */
  public static JsonObj jsonObj(String json) {
    return new JsonObj(parse(json));
  }

  public static JsonObj mk(InputStream in) throws IOException {
    return new JsonObj(parse(IOUtils.toString(in)));
  }

  /** {@link #jsonObj(java.util.Map)} as a function. */
  public static final Function<Map, JsonObj> jsonObj = new Function<Map, JsonObj>() {
    @Override public JsonObj apply(Map json) {
      return jsonObj(json);
    }
  };

  private static Map parse(String json) {
    try {
      return (Map) new JSONParser().parse(json);
    } catch (ParseException e) {
      return chuck(e);
    }
  }

  public JsonVal val(String key) {
    return new JsonVal(get(Object.class, key));
  }

  public JsonVal valOpt(String key) {
    return new JsonVal(get(Object.class, key));
  }

  public JsonObj obj(String key) {
    return jsonObj(get(Map.class, key));
  }

  public JsonArr arr(String key) {
    return new JsonArr(get(List.class, key));
  }

  /**
   * Get mandatory value of type <code>ev</code>.
   *
   * @return the requested value if it exists and has the required type
   * @deprecated
   */
  public <A> A get(Class<A> ev, String key) {
    final Object v = json.get(key);
    if (v != null) {
      try {
        return cast(v, ev);
      } catch (ClassCastException e) {
        throw new RuntimeException(format("Key %s has not required type %s but %s",
                                          key, ev.getName(), v.getClass().getName()));
      }
    } else {
      throw new RuntimeException(format("Key %s does not exist", key));
    }
  }

  /**
   * Get optional value of type <code>ev</code>.
   *
   * @return some if the value exists and has the required type, none otherwise
   * @deprecated
   */
  public <A> Option<A> opt(Class<A> ev, String key) {
    final Object v = json.get(key);
    if (v != null) {
      try {
        return some(cast(v, ev));
      } catch (ClassCastException e) {
        return none();
      }
    } else {
      return none();
    }
  }

  /**
   * Get mandatory JSON object.
   * @deprecated
   */
  public JsonObj getObj(String key) {
    return jsonObj(get(Map.class, key));
  }

  /**
   * Get an optional JSON object.
   * @deprecated
   */
  public Option<JsonObj> optObj(String key) {
    return opt(Map.class, key).map(jsonObj);
  }
}
