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

import static org.opencastproject.util.data.functions.Misc.cast;

import java.util.List;
import java.util.Map;

public final class JsonVal {
  private final Object val;

  public JsonVal(Object val) {
    this.val = val;
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

  public static String asString(Object o) {
    return cast(o, String.class);
  }
  public static Integer asInteger(Object o) {
    return cast(o, Integer.class);
  }
  public static Long asLong(Object o) {
    return cast(o, Long.class);
  }
  public static Float asFloat(Object o) {
    return cast(o, Float.class);
  }
  public static Double asDouble(Object o) {
    return cast(o, Double.class);
  }
  public static Boolean asBoolean(Object o) {
    return cast(o, Boolean.class);
  }
  public static JsonObj asJsonObj(Object o) {
    return JsonObj.jsonObj((Map) o);
  }
  public static JsonArr asJsonArr(Object o) {
    return new JsonArr((List) o);
  }
  public static JsonVal asJsonVal(Object o) {
    return new JsonVal(o);
  }
}
