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

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Enum utility methods.
 */
public final class EnumSupport {

  private EnumSupport() {
  }

  /**
   * Support method to help enums implement an enhanced <code>valueOf(String)</code> method, that does not throw an
   * IllegalArgumentException in case of incoming values, that do not match any of the enum's values.
   * 
   * @param enumClass
   *          the enum's class
   * @param value
   *          the value to look up
   * @return the matching enum value or null if none matches
   */
  @SuppressWarnings("unchecked")
  public static <E extends Enum<?>> E fromString(Class<E> enumClass, String value) {
    if (value == null)
      return null;
    value = value.trim();
    if (value.length() == 0)
      return null;
    Method m = null;
    try {
      m = enumClass.getDeclaredMethod("valueOf", String.class);
    } catch (NoSuchMethodException ignore) {
    }
    try {
      m.setAccessible(true);
    } catch (SecurityException ignore) {
    }
    try {
      E enumConstant = (E) m.invoke(null, value);
      return enumConstant;
    } catch (IllegalAccessException ignore) {
    } catch (InvocationTargetException ignore) {
    }
    return null;
  }

  /** Create a function to parse a string into an Enum value. */
  public static <A extends Enum> Function<String, Option<A>> parseEnum(final A e) {
    return new Function<String, Option<A>>() {
      @Override public Option<A> apply(String s) {
        try {
          return some((A) Enum.valueOf(e.getClass(), s));
        } catch (Exception ex) {
          return none();
        }
      }
    };
  }
}
