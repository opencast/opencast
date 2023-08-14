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

package org.opencastproject.util.data.functions;

import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.data.Function;

/** Boolean functions. */
public final class Booleans {
  private Booleans() {
  }

  public static <A> Function<A, Boolean> eq(final A a) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A x) {
        return EqualsUtil.eq(x, a);
      }
    };
  }

  public static <A extends Comparable<A>> Function<A, Boolean> lt(final A a) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A x) {
        return x.compareTo(a) < 0;
      }
    };
  }

  public static <A extends Comparable<A>> Function<A, Boolean> gt(final A a) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A x) {
        return x.compareTo(a) > 0;
      }
    };
  }

  /** A function that always returns true. */
  public static <A> Function<A, Boolean> yes() {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        return true;
      }
    };
  }

  public static <A> Function<A, Boolean> not(Function<A, Boolean> f) {
    return not.o(f);
  }

  public static final Function<Boolean, Boolean> not = new Function<Boolean, Boolean>() {
    @Override
    public Boolean apply(Boolean a) {
      return !a;
    }
  };
}
