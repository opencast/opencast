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

package org.opencastproject.util.data;

import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.data.functions.Misc.chuck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The option type encapsulates on optional value. It contains either some value or is empty. Please make sure to NEVER
 * wrap null into a some. Instead use none.
 */
// todo clean up the mix of abstract methods and concrete implementations based on the isSome() decision
public abstract class Option<A> implements Iterable<A> {
  private Option() {
  }

  /** Safe decomposition of the option type. */
  public abstract <B> B fold(Match<A, B> visitor);

  /** Safe decomposition of the option type using functions. */
  public <B> B fold(Function<A, B> some, Function0<B> none) {
    return isSome() ? some.apply(get()) : none.apply();
  }

  public abstract Option<A> foreach(Function<? super A, Void> f);

  public abstract <B> Option<B> fmap(Function<? super A, ? extends B> f);

  public <B> Option<B> map(Function<? super A, ? extends B> f) {
    return fmap(f);
  }

  /** Monadic bind operation <code>m a -> (a -> m b) -> m b</code>. */
  public abstract <B> Option<B> bind(Function<A, Option<B>> f);

  /** @see org.opencastproject.util.data.functions.Functions#bind(Function) */
  public <B> Option<B> flatMap(Function<A, Option<B>> f) {
    return bind(f);
  }

  public abstract boolean isSome();

  public boolean isNone() {
    return !isSome();
  }

  /** If this is some return <code>some</code>. Like {@link #bind(Function)} but ignores the option's content. */
  public <B> Option<B> andThen(Option<B> some) {
    return isSome() ? some : Option.<B> none();
  }

  /** If this is some return <code>some</code>. Like {@link #map(Function)} but ignores the option's content. */
  public <B> Option<B> andThenV(B some) {
    return isSome() ? some(some) : Option.<B> none();
  }

  /** Lazy version of {@link #andThen(Option)}. */
  public <B> Option<B> andThen(Function0<Option<B>> some) {
    return isSome() ? some.apply() : Option.<B> none();
  }

  /** Lazy version of {@link #andThenV(Object)}. */
  public <B> Option<B> andThenV(Function0<B> some) {
    return isSome() ? some(some.apply()) : Option.<B> none();
  }

  /** If this is none return <code>none</code> else this. */
  public Option<A> orElse(Option<A> none) {
    return isSome() ? this : none;
  }

  /** Lazy version of {@link #orElse(Option)}. */
  public Option<A> orElse(Function0<Option<A>> none) {
    return isSome() ? this : none.apply();
  }

  /** Throw <code>none</code> if none. */
  public <T extends Throwable> Option<A> orError(T none) throws T {
    if (isSome())
      return this;
    else
      throw none;
  }

  /** Throw <code>none</code> if none. */
  public <T extends Throwable> Option<A> orError(Class<T> none) throws T {
    if (isSome())
      return this;
    else {
      T t;
      try {
        t = none.newInstance();
      } catch (InstantiationException e) {
        return chuck(new Error("Error creating exception", e));
      } catch (IllegalAccessException e) {
        return chuck(new Error("Error creating exception", e));
      }
      throw t;
    }
  }

  /** Throw exception returned by <code>none</code> if none. */
  public <T extends Throwable> Option<A> orError(Function0<T> none) throws T {
    if (isSome())
      return this;
    else
      throw none.apply();
  }

  public <B> Option<Tuple<A, B>> and(Option<B> b) {
    if (isSome() && b.isSome()) {
      return some(tuple(get(), b.get()));
    } else {
      return none();
    }
  }

  /** Get the contained value or throw an exception. */
  public abstract A get();

  /** Get the contained value in case of being "some" or return parameter <code>none</code> otherwise. */
  public abstract A getOrElse(A none);

  /** Get the contained value in case of being "some" or return the result of evaluating <code>none</code> otherwise. */
  public abstract A getOrElse(Function0<A> none);

  /** To interface with legacy applications or frameworks that still use <code>null</code> values. */
  public abstract A getOrElseNull();

  /** Transform the option into a monadic list. */
  public abstract Monadics.ListMonadic<A> mlist();

  /** Transform an option into a list, either with a single element or an empty list. */
  public abstract List<A> list();

  /**
   * Left projection of this option. If the option is <code>some</code> return the value in an
   * {@link Either#left(Object)} else return <code>right</code> in an {@link Either#right(Object)}.
   */
  public abstract <B> Either<A, B> left(B right);

  /**
   * Right projection of this optio. If the option is <code>some</code> return the value in an
   * {@link Either#left(Object)} else return <code>right</code> in an {@link Either#right(Object)}.
   */
  public abstract <B> Either<B, A> right(B left);

  /** Inversion. If some return none. If none return some(zero). */
  public Option<A> inv(A zero) {
    return isSome() ? Option.<A> none() : some(zero);
  }

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object o);

  // -- constructor functions

  /** Create a new some. */
  public static <A> Option<A> some(final A a) {
    if (a == null)
      throw new Error("null must not be wrapped in a some");
    return new Option<A>() {
      @Override
      public <B> B fold(Match<A, B> visitor) {
        return visitor.some(a);
      }

      @Override
      public Option<A> foreach(Function<? super A, Void> f) {
        f.apply(a);
        return this;
      }

      @Override
      public <B> Option<B> fmap(Function<? super A, ? extends B> f) {
        B b = f.apply(a);
        return some(b);
      }

      @Override
      public <B> Option<B> bind(Function<A, Option<B>> f) {
        return f.apply(a);
      }

      @Override
      public boolean isSome() {
        return true;
      }

      @Override
      public A get() {
        return a;
      }

      @Override
      public A getOrElse(A none) {
        return a;
      }

      @Override
      public A getOrElse(Function0<A> none) {
        return a;
      }

      @Override
      public A getOrElseNull() {
        return a;
      }

      @Override
      public Iterator<A> iterator() {
        return Collections.singletonList(a).iterator();
      }

      @Override
      public Monadics.ListMonadic<A> mlist() {
        return Monadics.mlist(list());
      }

      @Override
      public List<A> list() {
        return Collections.singletonList(a);
      }

      @Override
      public <B> Either<A, B> left(B right) {
        return Either.left(a);
      }

      @Override
      public <B> Either<B, A> right(B left) {
        return Either.right(a);
      }

      @Override
      public int hashCode() {
        // since an Option should NEVER contain any null this is safe
        return a.hashCode();
      }

      @Override
      public boolean equals(Object o) {
        if (o instanceof Option) {
          Option<?> opt = (Option<?>) o;
          // since an Option should NEVER contain any null this is safe
          return opt.isSome() && a.equals(opt.get());
        } else {
          return false;
        }
      }

      @Override
      public String toString() {
        return "Some(" + a + ")";
      }
    };
  }

  /** Create a new none. */
  public static <A> Option<A> none() {
    return new Option<A>() {
      @Override
      public <B> B fold(Match<A, B> visitor) {
        return visitor.none();
      }

      @Override
      public Option<A> foreach(Function<? super A, Void> f) {
        return this;
      }

      @Override
      public <B> Option<B> fmap(Function<? super A, ? extends B> f) {
        return none();
      }

      @Override
      public <B> Option<B> bind(Function<A, Option<B>> f) {
        return none();
      }

      @Override
      public boolean isSome() {
        return false;
      }

      @Override
      public A get() {
        throw new IllegalStateException("a none does not contain a value");
      }

      @Override
      public A getOrElse(A none) {
        return none;
      }

      @Override
      public A getOrElse(Function0<A> none) {
        return none.apply();
      }

      @Override
      public A getOrElseNull() {
        return null;
      }

      @Override
      public Iterator<A> iterator() {
        return new ArrayList<A>().iterator();
      }

      @Override
      public Monadics.ListMonadic<A> mlist() {
        return Monadics.<A> mlist(list());
      }

      @Override
      public List<A> list() {
        return Collections.emptyList();
      }

      @Override
      public <B> Either<A, B> left(B right) {
        return Either.right(right);
      }

      @Override
      public <B> Either<B, A> right(B left) {
        return Either.left(left);
      }

      @Override
      public int hashCode() {
        return -1;
      }

      @Override
      public boolean equals(Object o) {
        return o instanceof Option && ((Option) o).isNone();
      }

      @Override
      public String toString() {
        return "None";
      }
    };
  }

  /**
   * Create a none with the type of <code>example</code>. This saves some nasty typing, e.g.
   * <code>Option.&lt;String&gt;none()</code> vs. <code>none("")</code>.
   * <p/>
   * Please note that this constructor is only due to Java's insufficient type inference.
   */
  public static <A> Option<A> none(A example) {
    return none();
  }

  /** Create a none with the given type. */
  public static <A> Option<A> none(Class<A> clazz) {
    return none();
  }

  /** Wrap an arbitrary object into an option with <code>null</code> being mapped to none. */
  public static <A> Option<A> option(A a) {
    if (a != null)
      return some(a);
    else
      return none();
  }

  /** {@link #option(Object)} as a function. */
  public static <A> Function<A, Option<A>> option() {
    return new Function<A, Option<A>>() {
      @Override
      public Option<A> apply(A a) {
        return option(a);
      }
    };
  }

  /**
   * Use this function in <code>getOrElse</code> if it is an error being none.
   *
   * @deprecated use {@link #orError(Throwable)} or {@link #orElse(Function0)} instead since it saves the need for
   *             creating new objects just for the sake of type soundness. Java unfortunately lacks a bottom type.
   */
  public static <A> Function0<A> error(final String message) {
    return new Function0<A>() {
      @Override
      public A apply() {
        throw new Error(message);
      }
    };
  }

  /**
   * Create an equals function.
   *
   * <pre>
   *   some("abc").map(eq("bcd")).getOrElse(false) // false
   *   some("abc").map(eq("abc")).getOrElse(false) // true
   * </pre>
   */
  public static Function<String, Boolean> eq(final String compare) {
    return new Function<String, Boolean>() {
      @Override
      public Boolean apply(String s) {
        return compare.equals(s);
      }
    };
  }

  public interface Match<A, B> {
    B some(A a);

    B none();
  }

  /** Effect match. */
  public abstract static class EMatch<A> implements Match<A, Void> {
    @Override
    public final Void some(A a) {
      esome(a);
      return null;
    }

    @Override
    public final Void none() {
      enone();
      return null;
    }

    protected abstract void esome(A a);

    protected abstract void enone();
  }
}
