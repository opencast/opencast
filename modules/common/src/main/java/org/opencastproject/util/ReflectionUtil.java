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

import static org.opencastproject.util.data.functions.Misc.chuck;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Reflection utils. */
public final class ReflectionUtil {
    private ReflectionUtil() {
    }

    /**
     * Simple helper to avoid unnecessary <code>return null;</code> statements when using {@link #run(Object)},
     * {@link #run(Class, Object)}.
     * Just wrap your expression like this <code>return call(expr)</code>.
     */
    public static <A> A call(Object expression) {
        return null;
    }

    /**
     * Call all parameterless methods of <code>c</code> on object <code>o</code>.
     * <p>
     * Does not call any bridge, synthetic or native methods and also no methods declared in Object.
     */
    public static <A, B extends A> void run(Class<A> c, B o) {
        runInternal(c.getMethods(), o);
    }

    /**
     * Call all parameterless methods of object <code>a</code>.
     * <p>
     * Does not call any bridge, synthetic or native methods and also no methods declared in Object.
     */
    public static <A> void run(A a) {
        runInternal(a.getClass().getMethods(), a);
    }

    private static <A> void runInternal(Method[] ms, A a) {
        try {
            for (Method m : ms) {
                if (m.getParameterTypes().length == 0
                        && !m.getDeclaringClass().equals(Object.class)
                        && !m.isBridge() && !m.isSynthetic()
                        && !Modifier.isNative(m.getModifiers())) {
                    m.invoke(a);
                }
            }
        } catch (Exception e) {
            chuck(e);
        }
    }

}
