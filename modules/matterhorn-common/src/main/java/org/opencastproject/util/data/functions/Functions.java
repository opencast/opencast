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

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.List;

import static org.opencastproject.util.data.Monadics.mlist;

/** General purpose functions, especially function transformations. */
public final class Functions {
  private Functions() {
  }

  /** Function composition: <code>f . g = f(g(x)) = o(f, g)</code> */
  public static <A, B, C> Function<A, C> o(final Function<B, C> f, final Function<A, B> g) {
    return new Function<A, C>() {
      @Override public C apply(A a) {
        return f.apply(g.apply(a));
      }
    };
  }

  /** Function composition: <code>f . g = f(g) = o(f, g)</code> */
  public static <A, B> Function0<B> o(final Function<A, B> f, final Function0<A> g) {
    return new Function0<B>() {
      @Override public B apply() {
        return f.apply(g.apply());
      }
    };
  }

  /** <code>f . g . h</code> */
  public static <A, B, C, D> Function<A, D> o(final Function<C, D> f, final Function<B, C> g, final Function<A, B> h) {
    return new Function<A, D>() {
      @Override public D apply(A a) {
        return f.apply(g.apply(h.apply(a)));
      }
    };
  }

  /** <code>f . g . h . i</code> */
  public static <A, B, C, D, E> Function<A, E> o(final Function<D, E> f, final Function<C, D> g, final Function<B, C> h,
                                                 final Function<A, B> i) {
    return new Function<A, E>() {
      @Override public E apply(A a) {
        return f.apply(g.apply(h.apply(i.apply(a))));
      }
    };
  }

  /** Left to right compositon: <code>f then g = g(f(x))</code> */
  public static <A, B, C> Function<A, C> then(final Function<A, B> f, final Function<B, C> g) {
    return new Function<A, C>() {
      @Override public C apply(A a) {
        return g.apply(f.apply(a));
      }
    };
  }

  /** Left to right compositon: <code>f then g = g(f)</code> */
  public static <A, B> Function0<B> then(final Function0<A> f, final Function<A, B> g) {
    return new Function0<B>() {
      @Override public B apply() {
        return g.apply(f.apply());
      }
    };
  }

  /** Apply <code>f</code> and ignore its result, then apply <code>g</code>. */
  public static <A, B> Function0<B> then(final Function0<A> f, final Function0<B> g) {
    return new Function0<B>() {
      @Override public B apply() {
        f.apply();
        return g.apply();
      }
    };
  }

  /** Curry a function of arity 2. */
  public static <A, B, C> Function<B, C> curry(final Function2<A, B, C> f, final A a) {
    return new Function<B, C>() {
      @Override
      public C apply(B b) {
        return f.apply(a, b);
      }
    };
  }

  /** Curry a function of arity 2. */
  public static <A, B, C> Function<A, Function<B, C>> curry(final Function2<A, B, C> f) {
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

  /** Uncurry to a function of arity 2. */
  public static <A, B, C> Function2<A, B, C> uncurry(final Function<A, Function<B, C>> f) {
    return new Function2<A, B, C>() {
      @Override
      public C apply(A a, B b) {
        return f.apply(a).apply(b);
      }
    };
  }

  /** Curry a function of arity 1. */
  public static <A, B> Function0<B> curry(final Function<A, B> f, final A a) {
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

  /** Noop function of arity 0. */
  public static <A> Function0<A> noop() {
    return new Function0<A>() {
      @Override
      public A apply() {
        return null;
      }
    };
  }

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
   * Identity function. The type is based on the type of the example object to save some nasty
   * typing, e.g. <code>Function.&lt;Integer&gt;identity()</code> vs. <code>identity(0)</code>
   * <p/>
   * Please note that this constructor is only due to Java's insufficient type inference.
   */
  public static <A> Function<A, A> identity(A example) {
    return identity();
  }

  /**
   * Identity function.
   *
   * @param clazz
   *         to describe the functions's type
   */
  public static <A> Function<A, A> identity(Class<A> clazz) {
    return identity();
  }

  /** Constant function that always returns <code>a</code>. */
  public static <A> Function0<A> constant(final A a) {
    return new Function0<A>() {
      @Override
      public A apply() {
        return a;
      }
    };
  }

  /** Promote function <code>a -&gt; b</code> to an {@link Option}. */
  public static <A, B> Function<Option<A>, Option<B>> liftOpt(final Function<A, B> f) {
    return new Function<Option<A>, Option<B>>() {
      @Override
      public Option<B> apply(Option<A> a) {
        return a.fmap(f);
      }
    };
  }

  /** Create a bound version of <code>f</code> for {@link Option}. */
  public static <A, B> Function<Option<A>, Option<B>> bindOpt(final Function<A, Option<B>> f) {
    return new Function<Option<A>, Option<B>>() {
      @Override
      public Option<B> apply(Option<A> a) {
        return a.bind(f);
      }
    };
  }

  /** Promote function <code>a -&gt; b</code> to a {@link List}. */
  public static <A, B> Function<List<A>, List<B>> liftList(final Function<A, B> f) {
    return new Function<List<A>, List<B>>() {
      @Override
      public List<B> apply(List<A> as) {
        return mlist(as).fmap(f).value();
      }
    };
  }

  /** Create a bound version of <code>f</code> for {@link List}. */
  public static <A, B> Function<List<A>, List<B>> bind(final Function<A, List<B>> f) {
    return new Function<List<A>, List<B>>() {
      @Override
      public List<B> apply(List<A> as) {
        return mlist(as).bind(f).value();
      }
    };
  }

  public static <A, B> List<B> bind(List<A> as, Function<A, List<B>> f) {
    List<B> target = new ArrayList<B>();
    for (A a : as) {
      target.addAll(f.apply(a));
    }
    return target;
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
    return Functions.<RuntimeException, A>castGeneric(t);
  }
}
