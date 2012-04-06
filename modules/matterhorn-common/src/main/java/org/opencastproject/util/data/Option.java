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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The option type encapsulates on optional value. It contains either some value or is empty.
 * Please make sure to NEVER wrap null into a some. Instead use none.
 */
public abstract class Option<A> implements Iterable<A> {

  private Option() {
  }

  /**
   * Safe decomposition of the option type.
   */
  public abstract <B> B fold(Match<A, B> visitor);

  public abstract void foreach(Function<A, Void> f);

  public abstract <B> Option<B> fmap(Function<A, B> f);

  public <B> Option<B> map(Function<A, B> f) {
    return fmap(f);
  }

  /**
   * Monadic bind operation <code>m a -> (a -> m b) -> m b</code>.
   */
  public abstract <B> Option<B> bind(Function<A, Option<B>> f);

  /**
   * @see org.opencastproject.util.data.functions.Functions#bind(Function
   */
  public <B> Option<B> flatMap(Function<A, Option<B>> f) {
    return bind(f);
  }

  public abstract boolean isSome();

  public boolean isNone() {
    return !isSome();
  }

  /**
   * If this is none return <code>node</code> else this.
   */
  public Option<A> orElse(Option<A> none) {
    return isSome() ? this : none;
  }

  /**
   * Get the contained value or throw an exception.
   */
  public abstract A get();

  /**
   * Get the contained value in case of being "some" or return parameter <code>none</code> otherwise.
   */
  public abstract A getOrElse(A none);

  /**
   * Get the contained value in case of being "some" or return the result of
   * evaluating <code>none</code> otherwise.
   */
  public abstract A getOrElse(Function0<A> none);

  /**
   * Transform the option into a monadic list.
   */
  public abstract Monadics.ListMonadic<A> mlist();

  /**
   * Transform an option into a list, either with a single element or an empty list.
   */
  public abstract List<A> list();

  /**
   * Left projection of this option. If the option is <code>some</code> return the
   * value in an {@link Either#left(Object)} else return <code>right</code> in an {@link Either#right(Object)}.
   */
  public abstract <B> Either<A, B> left(B right);

  /**
   * Right projection of this optio. If the option is <code>some</code> return the
   * value in an {@link Either#left(Object)} else return <code>right</code> in an {@link Either#right(Object)}.
   */
  public abstract <B> Either<B, A> right(B left);

  /**
   * Use this function in <code>getOrElse</code> if it is an error being none.
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
  
  /**
   * Create a new some.
   */
  public static <A> Option<A> some(final A a) {
    if (a == null)
      throw new Error("null must not be wrapped in a some");
    return new Option<A>() {
      @Override
      public <B> B fold(Match<A, B> visitor) {
        return visitor.some(a);
      }

      @Override
      public void foreach(Function<A, Void> f) {
        f.apply(a);
      }

      @Override
      public <B> Option<B> fmap(Function<A, B> f) {
        return some(f.apply(a));
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

  /**
   * Create a new none.
   */
  public static <A> Option<A> none() {
    return new Option<A>() {
      @Override
      public <B> B fold(Match<A, B> visitor) {
        return visitor.none();
      }

      @Override
      public void foreach(Function<A, Void> f) {
      }

      @Override
      public <B> Option<B> fmap(Function<A, B> f) {
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
      public Iterator<A> iterator() {
        return new ArrayList<A>().iterator();
      }

      @Override
      public Monadics.ListMonadic<A> mlist() {
        return Monadics.<A>mlist(list());
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
   * Wrap an arbitrary object into an option with <code>null</code> being mapped to none.
   */
  public static <A> Option<A> option(A a) {
    if (a != null)
      return some(a);
    else
      return none();
  }

  public interface Match<A, B> {
    B some(A a);

    B none();
  }
}
