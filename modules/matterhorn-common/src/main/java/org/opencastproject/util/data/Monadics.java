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

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static java.lang.StrictMath.min;
import static java.util.Arrays.asList;
import static org.opencastproject.util.data.Collections.iterator;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

public final class Monadics {

  private Monadics() {
  }

  // we need to define a separate interface for each container type
  // since Java lacks higher-order polymorphism (higher-kinded type) so we cannot
  // abstract over the container type like this
  //
  // interface Monadic<A, M, M<A>> {
  //   <B> M<B> map(Function<A, B> f);
  // }

  /** The list monad. */
  public abstract static class ListMonadic<A> implements Iterable<A> {

    private ListMonadic() {
    }

    /** Alias for {@link #fmap(Function) fmap}. */
    public final <B> ListMonadic<B> map(Function<A, B> f) {
      return fmap(f);
    }

    /**
     * Apply <code>f</code> to each elements building a new list. This is the list functor.
     *
     * @see #map(Function)
     */
    public abstract <B> ListMonadic<B> fmap(Function<A, B> f);

    /** Alias for {@link #bind(Function)}. */
    public final <B, M extends Collection<B>> ListMonadic<B> flatMap(Function<A, M> f) {
      return bind(f);
    }

    /**
     * Monadic bind <code>m a -&gt; (a -&gt; m b) -&gt m b</code>.
     * Apply <code>f</code> to each elements concatenating the results into a new list.
     */
    public abstract <B, M extends Collection<B>> ListMonadic<B> bind(Function<A, M> f);

    /** Fold the list from left to right applying binary operator <code>f</code> starting with <code>zero</code>. */
    public abstract <B> B foldl(B zero, Function2<B, A, B> f);

    /** Reduce the list from left to right applying binary operator <code>f</code>. The list must not be empty. */
    public abstract A reducel(Function2<A, A, A> f);

    /** Append <code>a</code> to the list. */
    public abstract <M extends Collection<A>> ListMonadic<A> concat(M m);

    /** Retain all elements satisfying predicate <code>p</code>. */
    public abstract ListMonadic<A> filter(Function<A, Boolean> p);

    /** Return the first element satisfying predicate <code>p</code>. */
    public abstract Option<A> find(Function<A, Boolean> p);

    /** Check if at least one element satisfies predicate <code>p</code>. */
    public abstract boolean exists(Function<A, Boolean> p);

    /** Apply side effect <code>e</code> to each element. */
    public abstract ListMonadic<A> each(Function<A, Void> e);

    /** Apply side effect <code>e</code> to each element. Indexed version. */
    public abstract ListMonadic<A> eachIndex(Function2<A, Integer, Void> e);

    public abstract <B, M extends Collection<B>> ListMonadic<Tuple<A, B>> zip(M m);

    public abstract <B> ListMonadic<Tuple<A, B>> zip(B[] bs);

    public abstract <B> ListMonadic<Tuple<A, B>> zip(Iterator<B> bs);

    public abstract ListMonadic<A> sort(Comparator<A> c);

    /** Return the head of the list. */
    public abstract Option<A> head();

    /** Limit the list to the first <code>n</code> elements. */
    public abstract ListMonadic<A> take(int n);

    /** Process the wrapped list en bloc. */
    public abstract <B> ListMonadic<B> inspect(Function<List<A>, List<B>> f);

    /** Unwrap. */
    public abstract List<A> value();
  }

  /** The iterator monad. */
  public abstract static class IteratorMonadic<A> implements Iterable<A> {

    private IteratorMonadic() {
    }

    /** Alias for {@link #fmap(Function)}. */
    public final <B> IteratorMonadic<B> map(Function<A, B> f) {
      return fmap(f);
    }

    /** Apply <code>f</code> to each element. */
    public abstract <B> IteratorMonadic<B> fmap(Function<A, B> f);

    /** Apply <code>f</code> to each element. The function also receives the element's index. */
    public abstract <B> IteratorMonadic<B> mapIndex(Function2<A, Integer, B> f);

    /** Alias for {@link #bind(Function)}. */
    public final <B> IteratorMonadic<B> flatMap(Function<A, Iterator<B>> f) {
      return bind(f);
    }

    /** Monadic bind. Apply <code>f</code> to each elements concatenating the results. */
    public abstract <B> IteratorMonadic<B> bind(Function<A, Iterator<B>> f);

//    /**
//     * Apply <code>f</code> to each elements concatenating the results into a new list.
//     */
//    <B, BB extends Collection<B>> IteratorMonadic<B> flatMap(Function<A, BB> f);

    /** Fold the elements applying binary operator <code>f</code> starting with <code>zero</code>. */
    public abstract <B> B fold(B zero, Function2<B, A, B> f);

    /** Reduce the elements applying binary operator <code>f</code>. The iterator must not be empty. */
    public abstract A reduce(Function2<A, A, A> f);

//    /**
//     * Append <code>a</code> to the list.
//     */
//    <M extends Collection<A>> ListMonadic<A> concat(M a);

    /** Retain all elements satisfying predicate <code>p</code>. */
    public abstract IteratorMonadic<A> filter(Function<A, Boolean> p);

    /** Limit iteration to the first <code>n</code> elements. */
    public abstract IteratorMonadic<A> take(int n);

    /** Apply side effect <code>e</code> to each element. */
    public abstract IteratorMonadic<A> each(Function<A, Void> e);

    /** Apply side effect <code>e</code> to each element. Indexed version of {@link #each(Function)}. */
    public abstract IteratorMonadic<A> eachIndex(Function2<A, Integer, Void> e);

    /**
     * Return the head of the iterator.
     * <em>ATTENTION:</em> This method is not pure since it has the
     * side effect of taking and wrapping the next element of the wrapped iterator.
     */
    public abstract Option<A> next();

    /** Return the wrapped iterator. */
    public abstract Iterator<A> value();

    /** Evaluate to a list. */
    public abstract List<A> eval();
  }

  private static <A> List<A> newList() {
    return new ArrayList<A>();
  }

  private static <A> List<A> newList(int size) {
    return new ArrayList<A>(size);
  }

  /** Constructor function optimized for lists. */
  public static <A> ListMonadic<A> mlist(final List<A> as) {
    return new ListMonadic<A>() {
      @Override
      public <B> ListMonadic<B> fmap(Function<A, B> f) {
        List<B> target = newList(as.size());
        for (A a : as) {
          target.add(f.apply(a));
        }
        return mlist(target);
      }

      @Override
      public <B, M extends Collection<B>> ListMonadic<B> bind(Function<A, M> f) {
        List<B> target = newList();
        for (A a : as) {
          target.addAll(f.apply(a));
        }
        return mlist(target);
      }

      @Override
      public ListMonadic<A> filter(Function<A, Boolean> p) {
        List<A> target = newList(as.size());
        for (A a : as) {
          if (p.apply(a)) {
            target.add(a);
          }
        }
        return mlist(target);
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
      public ListMonadic<A> take(int n) {
        return mlist(as.subList(0, min(as.size(), n)));
      }

      @Override
      public <M extends Collection<A>> ListMonadic<A> concat(M m) {
        List<A> target = newList(as.size() + m.size());
        target.addAll(as);
        target.addAll(m);
        return mlist(target);
      }

      @Override
      public <B> ListMonadic<B> inspect(Function<List<A>, List<B>> f) {
        return mlist(f.apply(as));
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
      public <B, M extends Collection<B>> ListMonadic<Tuple<A, B>> zip(M m) {
        final List<Tuple<A, B>> target = newList(min(as.size(), m.size()));
        final Iterator<A> asi = as.iterator();
        final Iterator<B> mi = m.iterator();
        while (asi.hasNext() && mi.hasNext()) {
          target.add(tuple(asi.next(), mi.next()));
        }
        return mlist(target);
      }

      @Override
      public <B> ListMonadic<Tuple<A, B>> zip(B[] bs) {
        final List<Tuple<A, B>> target = newList(min(as.size(), bs.length));
        int i = 0;
        final Iterator<A> asi = as.iterator();
        while (asi.hasNext() && i < bs.length) {
          target.add(tuple(asi.next(), bs[i++]));
        }
        return mlist(target);
      }

      @Override
      public <B> ListMonadic<Tuple<A, B>> zip(Iterator<B> bs) {
        final List<Tuple<A, B>> target = newList(as.size());
        final Iterator<A> asi = as.iterator();
        while (asi.hasNext() && bs.hasNext()) {
          target.add(tuple(asi.next(), bs.next()));
        }
        return mlist(target);
      }

      @Override public ListMonadic<A> sort(Comparator<A> c) {
        final List<A> target = newList(as.size());
        target.addAll(as);
        java.util.Collections.sort(target, c);
        return mlist(target);
      }

      @Override
      public Iterator<A> iterator() {
        return as.iterator();
      }

      @Override
      public List<A> value() {
        return as;
      }
    };
  }

  /** Constructor function optimized for arrays. */
  public static <A> ListMonadic<A> mlist(final A... as) {
    return new ListMonadic<A>() {
      @Override
      public <B> ListMonadic<B> fmap(Function<A, B> f) {
        List<B> target = newList(as.length);
        for (A a : as) {
          target.add(f.apply(a));
        }
        return mlist(target);
      }

      @Override
      public <B, BB extends Collection<B>> ListMonadic<B> bind(Function<A, BB> f) {
        List<B> target = newList();
        for (A a : as) {
          target.addAll(f.apply(a));
        }
        return mlist(target);
      }

      @Override
      public ListMonadic<A> filter(Function<A, Boolean> p) {
        List<A> target = newList(as.length);
        for (A a : as) {
          if (p.apply(a)) {
            target.add(a);
          }
        }
        return mlist(target);
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
      public ListMonadic<A> take(int n) {
        return (ListMonadic<A>) mlist(ArrayUtils.subarray(as, 0, n));
      }

      @Override
      public <AA extends Collection<A>> ListMonadic<A> concat(AA aa) {
        List<A> target = newList(as.length + aa.size());
        for (A a : as) {
          target.add(a);
        }
        target.addAll(aa);
        return mlist(target);
      }

      @Override
      public <B> ListMonadic<B> inspect(Function<List<A>, List<B>> f) {
        return mlist(f.apply(value()));
      }

      @Override
      public ListMonadic<A> each(Function<A, Void> e) {
        for (A a : as) {
          e.apply(a);
        }
        return mlist(as);
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
      public <B, M extends Collection<B>> ListMonadic<Tuple<A, B>> zip(M m) {
        final List<Tuple<A, B>> target = newList(min(as.length, m.size()));
        int i = 0;
        final Iterator<B> mi = m.iterator();
        while (i < as.length && mi.hasNext()) {
          target.add(tuple(as[i++], mi.next()));
        }
        return mlist(target);
      }

      @Override
      public <B> ListMonadic<Tuple<A, B>> zip(B[] bs) {
        final List<Tuple<A, B>> target = newList(min(as.length, bs.length));
        int i = 0;
        while (i < as.length && i < bs.length) {
          target.add(tuple(as[i], bs[i]));
          i++;
        }
        return mlist(target);
      }

      @Override
      public <B> ListMonadic<Tuple<A, B>> zip(Iterator<B> bs) {
        final List<Tuple<A, B>> target = newList(as.length);
        int i = 0;
        while (i < as.length && bs.hasNext()) {
          target.add(tuple(as[i++], bs.next()));
        }
        return mlist(target);
      }

      @Override public ListMonadic<A> sort(Comparator<A> c) {
        final List<A> target = list(as);
        java.util.Collections.sort(target, c);
        return mlist(target);
      }

      @Override
      public Iterator<A> iterator() {
        return Collections.iterator(as);
      }

      @Override
      public List<A> value() {
        return asList(as);
      }
    };
  }

  /** Constructor function optimized for iterators. */
  public static <A> ListMonadic<A> mlist(final Iterator<A> as) {
    return new ListMonadic<A>() {
      @Override
      public <B> ListMonadic<B> fmap(Function<A, B> f) {
        List<B> target = newList();
        while (as.hasNext()) {
          target.add(f.apply(as.next()));
        }
        return mlist(target);
      }

      @Override
      public <B, BB extends Collection<B>> ListMonadic<B> bind(Function<A, BB> f) {
        List<B> target = newList();
        while (as.hasNext()) {
          target.addAll(f.apply(as.next()));
        }
        return mlist(target);
      }

      @Override
      public ListMonadic<A> filter(Function<A, Boolean> p) {
        List<A> target = newList();
        while (as.hasNext()) {
          A a = as.next();
          if (p.apply(a)) {
            target.add(a);
          }
        }
        return mlist(target);
      }

      @Override
      public Option<A> find(Function<A, Boolean> p) {
        for (A a : Collections.forc(as)) {
          if (p.apply(a))
            return some(a);
        }
        return none();
      }

      @Override
      public boolean exists(Function<A, Boolean> p) {
        for (A a : Collections.forc(as)) {
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
      public ListMonadic<A> take(final int n) {
        return mlist(new Iter<A>() {
          private int count = 0;

          @Override
          public boolean hasNext() {
            return count < n && as.hasNext();
          }

          @Override
          public A next() {
            if (count < n) {
              count++;
              return as.next();
            } else {
              throw new NoSuchElementException();
            }
          }
        });
      }

      @Override
      public <M extends Collection<A>> ListMonadic<A> concat(M m) {
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
      public <B, M extends Collection<B>> ListMonadic<Tuple<A, B>> zip(M m) {
        final List<Tuple<A, B>> target = newList(m.size());
        final Iterator<B> mi = m.iterator();
        while (as.hasNext() && mi.hasNext()) {
          target.add(tuple(as.next(), mi.next()));
        }
        return mlist(target);
      }

      @Override
      public <B> ListMonadic<Tuple<A, B>> zip(B[] bs) {
        final List<Tuple<A, B>> target = newList(bs.length);
        int i = 0;
        while (as.hasNext() && i < bs.length) {
          target.add(tuple(as.next(), bs[i++]));
        }
        return mlist(target);
      }

      @Override
      public <B> ListMonadic<Tuple<A, B>> zip(Iterator<B> bs) {
        final List<Tuple<A, B>> target = newList();
        while (as.hasNext() && bs.hasNext()) {
          target.add(tuple(as.next(), bs.next()));
        }
        return mlist(target);
      }

      @Override public Iterator<A> iterator() {
        return as;
      }

      @Override public ListMonadic<A> sort(Comparator<A> c) {
        throw new UnsupportedOperationException();
      }

      @Override
      public List<A> value() {
        return Collections.toList(as);
      }
    };
  }

  /** Constructor function optimized for iterators. */
  public static <A> IteratorMonadic<A> mlazy(final Iterator<A> as) {
    return new IteratorMonadic<A>() {
      @Override
      public <B> IteratorMonadic<B> fmap(final Function<A, B> f) {
        return mlazy(new Iter<B>() {
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
        return mlazy(new Iter<B>() {
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
      public <B> IteratorMonadic<B> bind(final Function<A, Iterator<B>> f) {
        return mlazy(new Iter<B>() {
          @Override
          public boolean hasNext() {
            return step.hasNext() || step().hasNext();
          }

          @Override
          public B next() {
            if (step.hasNext()) {
              return step.next();
            } else {
              return step().next();
            }
          }

          // iterator state management
          private Iterator<B> step = Monadics.emptyIter();

          private Iterator<B> step() {
            while (!step.hasNext() && as.hasNext()) {
              step = f.apply(as.next());
            }
            return step;
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
      public IteratorMonadic<A> take(final int n) {
        return mlazy(new Iter<A>() {
          private int count = 0;

          @Override
          public boolean hasNext() {
            return count < n && as.hasNext();
          }

          @Override
          public A next() {
            if (count < n) {
              count++;
              return as.next();
            } else {
              throw new NoSuchElementException();
            }
          }
        });
      }

      @Override
      public IteratorMonadic<A> each(final Function<A, Void> e) {
        return mlazy(new Iter<A>() {
          @Override
          public boolean hasNext() {
            return as.hasNext();
          }

          @Override
          public A next() {
            final A a = as.next();
            e.apply(a);
            return a;
          }
        });
      }

      @Override
      public IteratorMonadic<A> eachIndex(final Function2<A, Integer, Void> e) {
        return mlazy(new Iter<A>() {
          private int i = 0;

          @Override
          public boolean hasNext() {
            return as.hasNext();
          }

          @Override
          public A next() {
            final A a = as.next();
            e.apply(a, i++);
            return a;
          }
        });
      }

      @Override
      public Option<A> next() {
        return as.hasNext() ? some(as.next()) : Option.<A>none();
      }

      @Override
      public Iterator<A> iterator() {
        return as;
      }

      @Override
      public Iterator<A> value() {
        return as;
      }

      @Override
      public List<A> eval() {
        return Collections.toList(as);
      }
    };
  }

  /** Constructor function optimized for lists. */
  public static <A> IteratorMonadic<A> mlazy(final List<A> as) {
    return mlazy(as.iterator());
  }

  /** Constructor function optimized for arrays. */
  public static <A> IteratorMonadic<A> mlazy(A... as) {
    return mlazy(iterator(as));
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
