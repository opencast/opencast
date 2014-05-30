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
import java.lang.reflect.Modifier;

import static org.opencastproject.util.data.functions.Misc.chuck;

/** Reflection utils. */
public final class ReflectionUtil {
    private ReflectionUtil() {
    }

    /**
     * Simple helper to avoid unnecessary <code>return null;</code> statements when using {@link #run(Object)},
     * {@link #run(Class, Object)} or {@link #xfer(Object, Class, org.opencastproject.util.data.Function)}.
     * Just wrap your expression like this <code>return call(expr)</code>.
     */
    public static <A> A call(Object expression) {
        return null;
    }

    public static boolean bcall(Object expression) {
        return false;
    }

    public static long lcall(Object expression) {
        return 0L;
    }

    public static int icall(Object expression) {
        return 0;
    }

    public static double dcall(Object expression) {
        return 0D;
    }

    /**
     * Call all parameterless methods of <code>c</code> on object <code>o</code>.
     * <p/>
     * Does not call any bridge, synthetic or native methods and also no methods declared in Object.
     */
    public static <A, B extends A> void run(Class<A> c, B o) {
        runInternal(c.getMethods(), o);
    }

    /**
     * Call all parameterless methods of object <code>a</code>.
     * <p/>
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

    /**
     * Helper method for the transfer idiom.
     * <p/>
     * Take <code>target</code> and apply it to <code>f</code>. Take the result and call all
     * parameterless methods on it that are described by class <code>source</code>. This way <code>f</code>
     * is able to create a transfer object that sets all properties of <code>target</code>.
     * <p/>
     * The advantage of this style is that each time the <code>source</code> class changes the compiler
     * will yield an error if a field is missed in the transfer. The common getter/setter or constructor idiom
     * does not detect errors of this kind at compile time.
     */
    public static <A, B> A xfer(A target, Class<B> source, Function<A, ? extends B> f) {
        final B b = f.apply(target);
        run(source, b);
        return target;
    }
}
