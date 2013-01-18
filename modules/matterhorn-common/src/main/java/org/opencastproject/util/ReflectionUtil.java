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

import java.lang.reflect.Method;

import static org.opencastproject.util.data.functions.Misc.chuck;

/** Reflection utils. */
public final class ReflectionUtil {
  private ReflectionUtil() {
  }

  /** Call all methods of <code>c</code> on object <code>o</code>. */
  public static <A, B extends A> void run(Class<A> c, B o) {
    try {
      for (Method m : c.getMethods()) {
        m.invoke(o);
      }
    } catch (Exception e) {
      chuck(e);
    }
  }

  /** Helper method for the transfer idiom. */
  public static <A, B> A xfer(A target, Class<B> c, Function<A, ? extends B> f) {
    final B b = f.apply(target);
    run(c, b);
    return target;
  }
}
