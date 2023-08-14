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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Utility function helping to implement equality. */
public final class EqualsUtil {
  private EqualsUtil() {
  }

  /** Check if <code>a</code> and <code>b</code> are equal. Each of them may be null. */
  @Deprecated
  public static boolean eqObj(Object a, Object b) {
    return Objects.equals(a, b);
  }

  /** Check if <code>a</code> and <code>b</code> are equal. Each of them may be null. */
  @Deprecated
  public static boolean eq(Object a, Object b) {
    return Objects.equals(a, b);
  }

  /** Check if <code>a</code> and <code>b</code> are not equal. Each of them may be null. */
  public static boolean ne(Object a, Object b) {
    return !eq(a, b);
  }

  /** Check if <code>a</code> and <code>b</code> have the same class ({@link Object#getClass()}). Each may be null. */
  public static boolean eqClasses(Object a, Object b) {
    return bothNotNull(a, b) && a.getClass().equals(b.getClass());
  }

  /**
   * Compare the (distinct) elements of two lists for equality treating the lists as sets.
   * <p>
   * Sets by definition do not allow multiplicity of elements; a set is a (possibly empty) collection of distinct elements.
   * As Lists may contain non-unique entries, this method removes duplicates before continuing with the comparison check.
   *
   * Examples of
   * 1. equality: {1, 2} = {2, 1} = {1, 1, 2} = {1, 2, 2, 1, 2}, null = null
   * 2. unequal: {1, 2, 2} != {1, 2, 3}, null != {}
   */
  public static boolean eqListUnsorted(List<?> as, List<?> bs) {
    if (as == null || bs == null) {
      return eqObj(as, bs);
    }

    as = as.stream().distinct().collect(Collectors.toList());
    bs = bs.stream().distinct().collect(Collectors.toList());

    if (as.size() != bs.size()) {
      return false;
    }
    for (Object a : as) {
      if (!bs.contains(a)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Compare the elements of two lists one by one.
   *
   * @deprecated use {@link #eqObj(Object, Object)} or {@link java.util.List#equals(Object)}
   */
  public static boolean eqListSorted(List<?> as, List<?> bs) {
    if (as != null && bs != null && as.size() == bs.size()) {
      final Iterator<?> asi = as.iterator();
      final Iterator<?> bsi = bs.iterator();
      while (asi.hasNext() && bsi.hasNext()) {
        if (!asi.next().equals(bsi.next()))
          return false;
      }
      return true;
    } else {
      return eqObj(as, bs);
    }
  }

  /**
   * Compare two maps.
   *
   * @deprecated use {@link #eqObj(Object, Object)} or {@link java.util.Map#equals(Object)}
   */
  public static boolean eqMap(Map<?, ?> as, Map<?, ?> bs) {
    for (Map.Entry<?, ?> ae : as.entrySet()) {
      final Object bv = bs.get(ae.getKey());
      if (bv == null || !eqObj(ae.getValue(), bv))
        return false;
    }
    return true;
  }

  public static boolean bothNotNull(Object a, Object b) {
    return a != null && b != null;
  }

  /**
   * Create a hash code for a list of objects. Each of them may be null.
   * Algorithm adapted from "Programming in Scala, Second Edition", p670.
   */
  @Deprecated
  public static int hash(Object... as) {
    return Objects.hash(as);
  }

}
