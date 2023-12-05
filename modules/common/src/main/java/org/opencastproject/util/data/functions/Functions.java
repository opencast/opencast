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

import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;

import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Predicate;

/** General purpose functions, especially function transformations. */
public final class Functions {
  private Functions() {
  }

  /** Function composition: <code>f . g = f(g(x)) = o(f, g)</code> */
  public static <A, B, C> Function<A, C> o(final Function<? super B, ? extends C> f,
          final Function<? super A, ? extends B> g) {
    return new Function<A, C>() {
      @Override
      public C apply(A a) {
        return f.apply(g.apply(a));
      }
    };
  }

  /** Function composition: <code>f . g = f(g) = o(f, g)</code> */
  public static <A, B> Function0<B> o(final Function<? super A, ? extends B> f, final Function0<? extends A> g) {
    return new Function0<B>() {
      @Override
      public B apply() {
        return f.apply(g.apply());
      }
    };
  }

  /** Left to right composition: <code>f then g = g(f(x))</code> */
  public static <A, B, C> Function<A, C> then(final Function<? super A, ? extends B> f,
          final Function<? super B, ? extends C> g) {
    return new Function<A, C>() {
      @Override
      public C apply(A a) {
        return g.apply(f.apply(a));
      }
    };
  }

  /** Left to right composition: <code>f then g = g(f)</code> */
  public static <A, B> Function0<B> then(final Function0<? extends A> f, final Function<? super A, ? extends B> g) {
    return new Function0<B>() {
      @Override
      public B apply() {
        return g.apply(f.apply());
      }
    };
  }

  /** Apply <code>f</code> and ignore its result, then apply <code>g</code>. */
  public static <A, B> Function0<B> then(final Function0<? extends A> f, final Function0<? extends B> g) {
    return new Function0<B>() {
      @Override
      public B apply() {
        f.apply();
        return g.apply();
      }
    };
  }

  /**
   * Create a new function from <code>f</code> decorated with an exception transformer. Any exception that occurs during
   * application of <code>f</code> is passed to <code>transformer</code> whose return value is then being thrown.
   */
  public static <A, B> Function<A, B> rethrow(final Function<? super A, ? extends B> f,
          final Function<? super Exception, ? extends Exception> transformer) {
    return new Function<A, B>() {
      @Override
      public B apply(A a) {
        try {
          return f.apply(a);
        } catch (Exception e) {
          return chuck(transformer.apply(e));
        }
      }
    };
  }

  /**
   * Create a new function from <code>f</code> decorated with an exception handler. Any exception that occurs during
   * application of <code>f</code> is passed to <code>handler</code> whose return value is then being returned.
   */
  public static <A, B> Function<A, B> handle(final Function<? super A, ? extends B> f,
          final Function<? super Exception, ? extends B> handler) {
    return new Function<A, B>() {
      @Override
      public B apply(A a) {
        try {
          return f.apply(a);
        } catch (Exception e) {
          return handler.apply(e);
        }
      }
    };
  }

  /**
   * Create a new function from <code>f</code> decorated with an exception handler. The new function either returns the
   * value of <code>f</code> or in case of an exception being thrown on the application of <code>f</code> the return
   * value of <code>handler</code>.
   */
  public static <A, B, C> Function<A, Either<C, B>> either(final Function<? super A, ? extends B> f,
          final Function<? super Exception, ? extends C> handler) {
    return new Function<A, Either<C, B>>() {
      @Override
      public Either<C, B> apply(A a) {
        try {
          B b = f.apply(a);
          return right(b);
        } catch (Exception e) {
          C c = handler.apply(e);
          return left(c);
        }
      }
    };
  }

  /** Curry a function of arity 2. */
  // todo rename since its actually partial application
  public static <A, B, C> Function<B, C> curry(final Function2<? super A, ? super B, ? extends C> f, final A a) {
    return new Function<B, C>() {
      @Override
      public C apply(B b) {
        return f.apply(a, b);
      }
    };
  }

  /** Curry a function of arity 2. (a, b) -gt; c =gt; a -gt; b -gt; c */
  public static <A, B, C> Function<A, Function<B, C>> curry(final Function2<? super A, ? super B, ? extends C> f) {
    return new Function<A, Function<B, C>>() {
      @Override
      public Function<B, C> apply(final A a) {
        return new Function<B, C>() {
          @Override
          public C apply(B b) {
            return f.apply(a, b);
          }
        };
      }
    };
  }

  /** Uncurry to a function of arity 2. a -gt; b -gt; c =gt; (a, b) -gt; c */
  public static <A, B, C> Function2<A, B, C> uncurry(final Function<? super A, Function<B, C>> f) {
    return new Function2<A, B, C>() {
      @Override
      public C apply(A a, B b) {
        return f.apply(a).apply(b);
      }
    };
  }

  /** Curry a function of arity 1. */
  public static <A, B> Function0<B> curry(final Function<? super A, ? extends B> f, final A a) {
    return new Function0<B>() {
      @Override
      public B apply() {
        return f.apply(a);
      }
    };
  }

  /** Curry a function of arity 1. */
  public static <A, B> Function<A, Function0<B>> curry(final Function<A, B> f) {
    return new Function<A, Function0<B>>() {
      @Override
      public Function0<B> apply(final A a) {
        return new Function0<B>() {
          @Override
          public B apply() {
            return f.apply(a);
          }
        };
      }
    };
  }

  /** Flip arguments of a function of arity 2. */
  public static <A, B, C> Function2<B, A, C> flip(final Function2<? super A, ? super B, ? extends C> f) {
    return new Function2<B, A, C>() {
      @Override
      public C apply(B b, A a) {
        return f.apply(a, b);
      }
    };
  }

  /** Turn a function into an effect by discarding its result. */
  public static <A, B> Effect<A> toEffect(final Function<? super A, ? extends B> f) {
    return new Effect<A>() {
      @Override
      protected void run(A a) {
        f.apply(a);
      }
    };
  }

  public static <A> Predicate<A> toPredicate(final Function<? super A, Boolean> f) {
    return new Predicate<A>() {
      @Override
      public Boolean apply(A a) {
        return f.apply(a);
      }
    };
  }

  /** Noop effect. */
  public static final Effect0 noop = new Effect0() {
    @Override
    protected void run() {
    }
  };

  /** Identity function. */
  public static <A> Function<A, A> identity() {
    return new Function<A, A>() {
      @Override
      public A apply(A a) {
        return a;
      }
    };
  }

  /**
   * Identity function. The type is based on the type of the example object to save some nasty typing, e.g.
   * <code>Function.&lt;Integer&gt;identity()</code> vs. <code>identity(0)</code>
   *
   * Please note that this constructor is only due to Java's insufficient type inference.
   */
  public static <A> Function<A, A> identity(A example) {
    return identity();
  }

  /**
   * Identity function.
   *
   * @param clazz
   *          to describe the functions's type
   */
  public static <A> Function<A, A> identity(Class<A> clazz) {
    return identity();
  }

  /** Create an effect that runs its argument. */
  public static final Effect<Effect0> run = new Effect<Effect0>() {
    @Override
    protected void run(Effect0 e) {
      e.apply();
    }
  };

  /** Pure functions are covariant in their result type. */
  public static <A, B> Function<A, B> co(Function<? super A, ? extends B> f) {
    return (Function<A, B>) f;
  }

  /** Pure functions are contravariant in their argument type. */
  public static <A, B> Function<A, B> contra(Function<? super A, B> f) {
    return (Function<A, B>) f;
  }

  /** Pure functions are covariant in their result type and contravariant in their argument type. */
  public static <A, B> Function<A, B> variant(Function<? super A, ? extends B> f) {
    return (Function<A, B>) f;
  }

  private static <T extends Throwable, A> A castGeneric(Throwable t) throws T {
    // The cast to T does not happen here but _after_ returning from the method at _assignment_ time
    // But since there is no variable assignment. The Throwable is just thrown.
    throw (T) t;
  }

  /**
   * Throw a checked exception like a RuntimeException removing any needs to declare a throws clause.
   *
   * This technique has been described by James Iry at
   * http://james-iry.blogspot.de/2010/08/on-removing-java-checked-exceptions-by.html
   */
  public static <A> A chuck(Throwable t) {
    return Functions.<RuntimeException, A> castGeneric(t);
  }

  /** Convert function <code>f</code> into a guava function. */
  public static <A, B> com.google.common.base.Function<A, B> toGuava(final Function<? super A, ? extends B> f) {
    return new com.google.common.base.Function<A, B>() {
      @Override
      public B apply(A a) {
        return f.apply(a);
      }
    };
  }

}
