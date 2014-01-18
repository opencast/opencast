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
package org.opencastproject.util.data.functions;

import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;

import java.util.List;

import static org.opencastproject.util.EqualsUtil.eq;

/** Various functions not directly bound to any type. */
public final class Misc {
  private Misc() {
  }

  private static <T extends Throwable, A> A castGeneric(Throwable t) throws T {
    // The cast to T does not happen here but _after_ returning from the method at _assignment_ time
    // But since there is no variable assignment. The Throwable is just thrown.
    throw (T) t;
  }

  /**
   * Throw a checked exception like a RuntimeException removing any needs to declare a throws clause.
   * <p/>
   * This technique has been described by James Iry at
   * http://james-iry.blogspot.de/2010/08/on-removing-java-checked-exceptions-by.html
   */
  public static <A> A chuck(Throwable t) {
    return Misc.<RuntimeException, A> castGeneric(t);
  }

  /** {@link #chuck(Throwable)} as a function. */
  public static <A extends Throwable, B> Function<A, B> chuck() {
    return new Function<A, B>() {
      @Override
      public B apply(Throwable throwable) {
        return chuck(throwable);
      }
    };
  }

  /** Cast from A to B with special treatment of the Number classes. */
  public static <A, B> B cast(A v, Class<B> to) {
    if (Number.class.isAssignableFrom(v.getClass())) {
      if (eq(Integer.class, to)) {
        return (B) ((Object) (((Number) v).intValue()));
      } else if (eq(Long.class, to)) {
        return (B) ((Object) (((Number) v).longValue()));
      } else if (eq(Double.class, to)) {
        return (B) ((Object) (((Number) v).doubleValue()));
      } else if (eq(Float.class, to)) {
        return (B) ((Object) (((Number) v).floatValue()));
      } else if (eq(Short.class, to)) {
        return (B) ((Object) (((Number) v).shortValue()));
      } else if (eq(Byte.class, to)) {
        return (B) ((Object) (((Number) v).byteValue()));
      } else {
        return (B) v;
      }
    } else if (to.isAssignableFrom(v.getClass())) {
      return (B) v;
    } else {
      throw new ClassCastException(v.getClass().getName() + " is not of type " + to.getName());
    }
  }

  /** Cast from A to B. */
  public static <A, B> Function<A, B> cast() {
    return new Function<A, B>() {
      @Override
      public B apply(A a) {
        return (B) a;
      }
    };
  }

  /** Widening cast. */
  public static <A> List<A> widen(List<? extends A> xs) {
    return (List<A>) xs;
  }

  /** Widening cast. */
  public static <A> List<A> widen(Class<A> type, List<? extends A> xs) {
    return (List<A>) xs;
  }

  /** Widening cast. */
  public static <A> Option<A> widen(Option<? extends A> xs) {
    return (Option<A>) xs;
  }

  /** Widening cast. */
  public static <A> Option<A> widen(Class<A> type, Option<? extends A> xs) {
    return (Option<A>) xs;
  }

  public static <A extends Exception> Function0<A> error(final Class<A> cls, final String msg, final Throwable cause) {
    return new Function0.X<A>() {
      @Override
      public A xapply() throws Exception {
        return cls.getConstructor(String.class, Throwable.class).newInstance(msg, cause);
      }
    };
  }

  public static <A extends Exception> Function0<A> error(final Class<A> cls, final String msg) {
    return new Function0.X<A>() {
      @Override
      public A xapply() throws Exception {
        return cls.getConstructor(String.class).newInstance(msg);
      }
    };
  }

  /** Print an object. */
  public static <A> Effect<A> println() {
    return new Effect<A>() {
      @Override
      protected void run(A a) {
        System.out.println(a);
      }
    };
  }

  public static <A> Function<A, A> ifThen(final A predicate, final A b) {
    return new Function<A, A>() {
      @Override
      public A apply(A a) {
        return predicate.equals(a) ? b : a;
      }
    };
  }
}