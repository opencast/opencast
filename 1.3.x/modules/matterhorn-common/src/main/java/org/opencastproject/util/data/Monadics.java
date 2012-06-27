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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static java.util.Arrays.asList;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

public final class Monadics {

  private Monadics() {
  }

  // we need to define a separate interface for each container type
  // since Java lacks higher-order polymorphism so we cannot
  // abstract over the container type like this
  //
  // interface Monadic<A, CC, CC<A>> {
  //   <B> CC<B> map(Function<A, B> f);
  //   CC<B> value();
  // }

  public interface ListMonadic<A> {
    /**
     * Apply <code>f</code> to each elements building a new list.
     */
    <B> ListMonadic<B> map(Function<A, B> f);

    /**
     * Apply <code>f</code> to each elements concatenating the results into a new list.
     * This is equal to monadic bind.
     */
    <B, BB extends Collection<B>> ListMonadic<B> flatMap(Function<A, BB> f);

    /**
     * Fold the list from left to right applying binary operator <code>f</code> starting with <code>zero</code>.
     */
    <B> B foldl(B zero, Function2<B, A, B> f);

    /**
     * Reduce the list from left to right applying binary operator <code>f</code>. The list must not be empty.
     */
    A reducel(Function2<A, A, A> f);

    /**
     * Append <code>a</code> to the list.
     */
    <AA extends Collection<A>> ListMonadic<A> concat(AA a);

    /**
     * Retain all elements satisfying predicate <code>p</code>.
     */
    ListMonadic<A> filter(Function<A, Boolean> p);

    /**
     * Return the first element satisfying predicate <code>p</code>.
     */
    Option<A> find(Function<A, Boolean> p);

    /**
     * Check if at least one element satisfies predicate <code>p</code>.
     */
    boolean exists(Function<A, Boolean> p);

    /**
     * Apply side effect <code>e</code> to each element.
     */
    ListMonadic<A> each(Function<A, Void> e);

    /**
     * Apply side effect <code>e</code> to each element. Indexed version.
     */
    ListMonadic<A> eachIndex(Function2<A, Integer, Void> e);

    /**
     * Return the head of the list.
     */
    Option<A> head();

    /**
     * Process the wrapped list en bloc.
     */
    <B> ListMonadic<B> inspect(Function<List<A>, List<B>> f);

    /**
     * Unwrap.
     */
    List<A> value();
  }

  public interface IteratorMonadic<A> {
    /**
     * Apply <code>f</code> to each element building a new iterator.
     */
    <B> IteratorMonadic<B> map(Function<A, B> f);

    /**
     * Apply <code>f</code> to each element building a new iterator. The function
     * also receives the element's index.
     */
    <B> IteratorMonadic<B> mapIndex(Function2<A, Integer, B> f);

    /**
     * Apply <code>f</code> to each elements concatenating the results.
     * This is equal to monadic bind.
     */
    <B> IteratorMonadic<B> flatMap(Function<A, Iterator<B>> f);

//    /**
//     * Apply <code>f</code> to each elements concatenating the results into a new list.
//     */
//    <B, BB extends Collection<B>> IteratorMonadic<B> flatMap(Function<A, BB> f);

    /**
     * Fold the elements applying binary operator <code>f</code> starting with <code>zero</code>.
     */
    <B> B fold(B zero, Function2<B, A, B> f);

    /**
     * Reduce the elements applying binary operator <code>f</code>. The iterator must not be empty.
     */
    A reduce(Function2<A, A, A> f);

//    /**
//     * Append <code>a</code> to the list.
//     */
//    <AA extends Collection<A>> ListMonadic<A> concat(AA a);

    /**
     * Retain all elements satisfying predicate <code>p</code>.
     */
    IteratorMonadic<A> filter(Function<A, Boolean> p);

    /**
     * Apply side effect <code>e</code> to each element.
     */
    IteratorMonadic<A> each(Function<A, Void> e);

    /**
     * Apply side effect <code>e</code> to each element. Indexed version.
     */
    IteratorMonadic<A> eachIndex(Function2<A, Integer, Void> e);

    /**
     * Return the head of the iterator. <em>ATTENTION:</em> This method is not pure since it has the
     * side effect of taking and wrapping the next element of the wrapped iterator.
     */
    Option<A> next();

    /**
     * Unwrap.
     */
    Iterator<A> value();

    /**
     * Evaluate to a list.
     */
    List<A> eval();
  }

// todo implement constructor functions
//  interface ArrayMonadic<A> {
//    <B> ArrayMonadic<B> map(Function<A, B> f);
//    <B> ArrayMonadic<B> flatMap(Function<A, Collection<B>> f);
//    ArrayMonadic<A> filter(Function<A, Boolean> p);
//    A[] value();
//  }

  /**
   * Constructor function optimized for lists.
   * This is monadic return.
   */
  public static <A> ListMonadic<A> list(final List<A> as) {
    return new ListMonadic<A>() {
      @Override
      public <B> ListMonadic<B> map(Function<A, B> f) {
        List<B> target = new ArrayList<B>(as.size());
        for (A a : as) {
          target.add(f.apply(a));
        }
        return list(target);
      }

      @Override
      public <B, BB extends Collection<B>> ListMonadic<B> flatMap(Function<A, BB> f) {
        List<B> target = new ArrayList<B>();
        for (A a : as) {
          target.addAll(f.apply(a));
        }
        return list(target);
      }

      @Override
      public ListMonadic<A> filter(Function<A, Boolean> p) {
        List<A> target = new ArrayList<A>(as.size());
        for (A a : as) {
          if (p.apply(a)) {
            target.add(a);
          }
        }
        return list(target);
      }

      @Override
      public Option<A> find(Function<A, Boolean> p) {
        for (A a : as) {
          if (p.apply(a))
            return some(a);
        }
        return none();
      }

      @Override
      public boolean exists(Function<A, Boolean> p) {
        for (A a : as) {
          if (p.apply(a))
            return true;
        }
        return false;
      }

      @Override
      public <B> B foldl(B zero, Function2<B, A, B> f) {
        B fold = zero;
        for (A a : as) {
          fold = f.apply(fold, a);
        }
        return fold;
      }

      @Override
      public A reducel(Function2<A, A, A> f) {
        if (as.size() == 0) {
          throw new RuntimeException("Cannot reduce an empty list");
        } else {
          A fold = as.get(0);
          for (int i = 1; i < as.size(); i++) {
            fold = f.apply(fold, as.get(i));
          }
          return fold;
        }
      }

      @Override
      public Option<A> head() {
        return !as.isEmpty() ? some(as.get(0)) : Option.<A>none();
      }

      @Override
      public <AA extends Collection<A>> ListMonadic<A> concat(AA aa) {
        List<A> target = new ArrayList<A>(as.size() + aa.size());
        target.addAll(as);
        target.addAll(aa);
        return list(target);
      }

      @Override
      public <B> ListMonadic<B> inspect(Function<List<A>, List<B>> f) {
        return list(f.apply(as));
      }

      @Override
      public ListMonadic<A> each(Function<A, Void> e) {
        for (A a : as) {
          e.apply(a);
        }
        return this;
      }

      @Override
      public ListMonadic<A> eachIndex(Function2<A, Integer, Void> e) {
        int i = 0;
        for (A a : as) {
          e.apply(a, i++);
        }
        return this;
      }

      @Override
      public List<A> value() {
        return as;
      }
    };
  }

  /**
   * Constructor function optimized for var args.
   * This is monadic return.
   */
  public static <A> ListMonadic<A> listva(A... as) {
    return list(as);
  }

  /**
   * Constructor function optimized for arrays.
   * This is monadic return.
   */
  public static <A> ListMonadic<A> list(final A[] as) {
    return new ListMonadic<A>() {
      @Override
      public <B> ListMonadic<B> map(Function<A, B> f) {
        List<B> target = new ArrayList<B>(as.length);
        for (A a : as) {
          target.add(f.apply(a));
        }
        return list(target);
      }

      @Override
      public <B, BB extends Collection<B>> ListMonadic<B> flatMap(Function<A, BB> f) {
        List<B> target = new ArrayList<B>();
        for (A a : as) {
          target.addAll(f.apply(a));
        }
        return list(target);
      }

      @Override
      public ListMonadic<A> filter(Function<A, Boolean> p) {
        List<A> target = new ArrayList<A>(as.length);
        for (A a : as) {
          if (p.apply(a)) {
            target.add(a);
          }
        }
        return list(target);
      }

      @Override
      public Option<A> find(Function<A, Boolean> p) {
        for (A a : as) {
          if (p.apply(a))
            return some(a);
        }
        return none();
      }

      @Override
      public boolean exists(Function<A, Boolean> p) {
        for (A a : as) {
          if (p.apply(a))
            return true;
        }
        return false;
      }

      @Override
      public <B> B foldl(B zero, Function2<B, A, B> f) {
        B fold = zero;
        for (A a : as) {
          fold = f.apply(fold, a);
        }
        return fold;
      }

      @Override
      public A reducel(Function2<A, A, A> f) {
        if (as.length == 0) {
          throw new RuntimeException("Cannot reduce an empty list");
        } else {
          A fold = as[0];
          for (int i = 1; i < as.length; i++) {
            fold = f.apply(fold, as[i]);
          }
          return fold;
        }
      }

      @Override
      public Option<A> head() {
        return as.length != 0 ? some(as[0]) : Option.<A>none();
      }

      @Override
      public <AA extends Collection<A>> ListMonadic<A> concat(AA aa) {
        List<A> target = new ArrayList<A>(as.length + aa.size());
        for (A a : as) {
          target.add(a);
        }
        target.addAll(aa);
        return list(target);
      }

      @Override
      public <B> ListMonadic<B> inspect(Function<List<A>, List<B>> f) {
        return list(f.apply(value()));
      }

      @Override
      public ListMonadic<A> each(Function<A, Void> e) {
        for (A a : as) {
          e.apply(a);
        }
        return list(as);
      }

      @Override
      public ListMonadic<A> eachIndex(Function2<A, Integer, Void> e) {
        int i = 0;
        for (A a : as) {
          e.apply(a, i++);
        }
        return this;
      }

      @Override
      public List<A> value() {
        return asList(as);
      }
    };
  }

  /**
   * Constructor function optimized for iterators.
   * - head
   * - concat
   * - inspect
   */
  public static <A> ListMonadic<A> list(final Iterator<A> as) {
    return new ListMonadic<A>() {
      @Override
      public <B> ListMonadic<B> map(Function<A, B> f) {
        List<B> target = new ArrayList<B>();
        while (as.hasNext()) {
          target.add(f.apply(as.next()));
        }
        return list(target);
      }

      @Override
      public <B, BB extends Collection<B>> ListMonadic<B> flatMap(Function<A, BB> f) {
        List<B> target = new ArrayList<B>();
        while (as.hasNext()) {
          target.addAll(f.apply(as.next()));
        }
        return list(target);
      }

      @Override
      public ListMonadic<A> filter(Function<A, Boolean> p) {
        List<A> target = new ArrayList<A>();
        while (as.hasNext()) {
          A a = as.next();
          if (p.apply(a)) {
            target.add(a);
          }
        }
        return list(target);
      }

      @Override
      public Option<A> find(Function<A, Boolean> p) {
        for (A a : forc(as)) {
          if (p.apply(a))
            return some(a);
        }
        return none();
      }

      @Override
      public boolean exists(Function<A, Boolean> p) {
        for (A a : forc(as)) {
          if (p.apply(a))
            return true;
        }
        return false;
      }

      @Override
      public <B> B foldl(B zero, Function2<B, A, B> f) {
        B fold = zero;
        while (as.hasNext()) {
          fold = f.apply(fold, as.next());
        }
        return fold;
      }

      @Override
      public A reducel(Function2<A, A, A> f) {
        if (!as.hasNext()) {
          throw new RuntimeException("Cannot reduce an empty iterator");
        } else {
          A fold = as.next();
          while (as.hasNext()) {
            fold = f.apply(fold, as.next());
          }
          return fold;
        }
      }

      @Override
      public Option<A> head() {
        throw new UnsupportedOperationException();
      }

      @Override
      public <AA extends Collection<A>> ListMonadic<A> concat(AA aa) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <B> ListMonadic<B> inspect(Function<List<A>, List<B>> f) {
        throw new UnsupportedOperationException();
      }

      @Override
      public ListMonadic<A> each(Function<A, Void> e) {
        while (as.hasNext()) {
          e.apply(as.next());
        }
        return this;
      }

      @Override
      public ListMonadic<A> eachIndex(Function2<A, Integer, Void> e) {
        int i = 0;
        while (as.hasNext()) {
          e.apply(as.next(), i++);
        }
        return this;
      }

      @Override
      public List<A> value() {
        return toList(as);
      }
    };
  }

  // --

  /**
   * Value constructor. This is monadic return.
   */
  public static <A> IteratorMonadic<A> lazy(final Iterator<A> as) {
    return new IteratorMonadic<A>() {
      @Override
      public <B> IteratorMonadic<B> map(final Function<A, B> f) {
        return lazy(new Iter<B>() {
          @Override
          public boolean hasNext() {
            return as.hasNext();
          }

          @Override
          public B next() {
            return f.apply(as.next());
          }
        });
      }

      @Override
      public <B> IteratorMonadic<B> mapIndex(final Function2<A, Integer, B> f) {
        return lazy(new Iter<B>() {
          private int i = 0;

          @Override
          public boolean hasNext() {
            return as.hasNext();
          }

          @Override
          public B next() {
            return f.apply(as.next(), i++);
          }
        });
      }

      @Override
      public <B> IteratorMonadic<B> flatMap(final Function<A, Iterator<B>> f) {
        return lazy(new Iter<B>() {
          @Override
          public boolean hasNext() {
            return apply().fold(new Option.Match<Iterator<B>, Boolean>() {
              @Override
              public Boolean some(Iterator<B> ignore) {
                return true;
              }

              @Override
              public Boolean none() {
                return false;
              }
            });
          }

          @Override
          public B next() {
            return apply().fold(new Option.Match<Iterator<B>, B>() {
              @Override
              public B some(Iterator<B> bb) {
                return bb.next();
              }

              @Override
              public B none() {
                throw new NoSuchElementException();
              }
            });
          }

          private Option<Iterator<B>> current = some(Monadics.<B>emptyIter());

          private Option<Iterator<B>> apply() {
            current = current.flatMap(new Function<Iterator<B>, Option<Iterator<B>>>() {
              @Override
              public Option<Iterator<B>> apply(Iterator<B> c) {
                if (c.hasNext()) {
                  return some(c);
                } else {
                  if (as.hasNext()) {
                    return some(f.apply(as.next()));
                  } else {
                    return none();
                  }
                }
              }
            });
            return current;
          }
        });
      }

      @Override
      public <B> B fold(B zero, Function2<B, A, B> f) {
        throw new UnsupportedOperationException();
      }

      @Override
      public A reduce(Function2<A, A, A> f) {
        throw new UnsupportedOperationException();
      }

      @Override
      public IteratorMonadic<A> filter(Function<A, Boolean> p) {
        throw new UnsupportedOperationException();
      }

      @Override
      public IteratorMonadic<A> each(final Function<A, Void> e) {
        return lazy(new Iter<A>() {
          @Override
          public boolean hasNext() {
            return as.hasNext();
          }

          @Override
          public A next() {
            final A a = as.next();
            e.apply(as.next());
            return a;
          }
        });
      }

      @Override
      public IteratorMonadic<A> eachIndex(final Function2<A, Integer, Void> e) {
        return lazy(new Iter<A>() {
          private int i = 0;

          @Override
          public boolean hasNext() {
            return as.hasNext();
          }

          @Override
          public A next() {
            final A a = as.next();
            e.apply(as.next(), i++);
            return a;
          }
        });
      }

      @Override
      public Option<A> next() {
        return as.hasNext() ? some(as.next()) : Option.<A>none();
      }

      @Override
      public Iterator<A> value() {
        return as;
      }

      @Override
      public List<A> eval() {
        return toList(as);
      }
    };
  }

  public static <A> IteratorMonadic<A> lazy(final List<A> as) {
    return lazy(as.iterator());
  }

  public static <A> Iterator<A> toIter(A... as) {
    return asList(as).iterator();
  }

  /**
   * Make an Iterator usable in a for comprehension like this:
   * <pre>
   *   Iterator&lt;A&gt; as = ...
   *   for (A a : forc(as)) {
   *     ...
   *   }
   * </pre>
   */
  public static <A> Collection<A> forc(final Iterator<A> as) {
    return new AbstractCollection<A>() {
      @Override
      public Iterator<A> iterator() {
        return as;
      }

      @Override
      public int size() {
        return -1;
      }
    };
  }

  //
  // Helper
  //

  public static <A> List<A> toList(Iterator<A> as) {
    List<A> ass = new ArrayList<A>();
    while (as.hasNext()) {
      ass.add(as.next());
    }
    return ass;
  }


  private abstract static class Iter<A> implements Iterator<A> {
    @Override
    public final void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private static <A> Iterator<A> emptyIter() {
    return new Iter<A>() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public A next() {
        throw new NoSuchElementException();
      }
    };
  }
}
