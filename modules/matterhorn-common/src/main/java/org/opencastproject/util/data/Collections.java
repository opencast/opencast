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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.opencastproject.util.data.Option.some;

/**
 * This class provides functions to ease and secure the handling of collections by supporting a type safe -- at least to
 * the extent Java's type system allows -- immutable and more functional style.
 * <p/>
 * Note that all functions do <i>not</i> mutate input collections unless otherwise stated.
 */
public final class Collections {
  private Collections() {
  }

  @SuppressWarnings("unchecked")
  private static <A, B> Collection<A> buildFrom(Collection<B> as) {
    try {
      return as.getClass().newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException("Type " + as.getClass() + " needs a parameterless constructor");
    }
  }

  /**
   * Get a value from a map, creating and adding a new one, if the value is missing, i.e. it is null.
   * 
   * @param c
   *          creates the missing value
   */
  public static <K, V> V getOrCreate(Map<K, V> map, K key, Creator<V> c) {
    V v = map.get(key);
    if (v == null) {
      v = c.create();
      map.put(key, v);
    }
    return v;
  }

  /**
   * Get a value from a map, creating and adding a new one, if the value is missing, i.e. it is null.
   */
  public static <K, V> V getOrCreate(Map<K, V> map, K key, Function0<V> f) {
    V v = map.get(key);
    if (v == null) {
      v = f.apply();
      map.put(key, v);
    }
    return v;
  }

  /**
   * Apply a function <code>f</code> to all elements of collection <code>as</code> to produce a new collection
   * <code>bs</code>.
   * <p/>
   * An (empty) instance of the target collection has to be provided explicitly.
   * 
   * @param as
   *          the source collection
   * @param bs
   *          the (empty) target collection
   * @param f
   *          the function to apply to each element of <code>as</code>
   * @deprecated use {@link Monadics}
   */
  public static <A, B, M extends Collection<B>> M map(Collection<A> as, M bs, Function<A, B> f) {
    for (A x : as) {
      bs.add(f.apply(x));
    }
    return bs;
  }

  /**
   * Apply a binary function (operator) to a start value and all elements of the list in turn.
   * <p/>
   * Example: (+) 0 [1, 2, 3] -> (((0 + 1) + 2) + 3)
   * 
   * @deprecated use {@link Monadics}
   */
  public static <A, B> B foldl(Collection<A> as, B start, Function2<B, A, B> f) {
    B fold = start;
    for (A a : as) {
      fold = f.apply(fold, a);
    }
    return fold;
  }

  /**
   * Apply a function <code>f</code> to all elements of collection <code>as</code> to produce a new collection
   * <code>bs</code>.
   * <p/>
   * The type of collection <code>as</code> needs a parameterless constructor.
   * <p/>
   * Please note that since java does not support higher-order polymorphism -- which is needed to capture the type of
   * the collection -- some casting on the client side may still be necessary.
   * 
   * @throws RuntimeException
   *           if the target collection cannot be created
   * @deprecated use {@link Monadics}
   */
  public static <A, B> Collection<B> map(Collection<A> as, Function<A, B> f) {
    Collection<B> b = buildFrom(as);
    for (A x : as) {
      b.add(f.apply(x));
    }
    return b;
  }

  /**
   * Apply a function <code>f</code> to all elements of collection <code>as</code> to produce a new collection
   * <code>bs</code> by concatenating the results.
   * <p/>
   * The type of collection <code>as</code> needs a parameterless constructor.
   * <p/>
   * Please note that since java does not support higher-order polymorphism -- which is needed to capture the type of
   * the collection -- some casting on the client side may still be necessary.
   * 
   * @throws RuntimeException
   *           if the result collection cannot be created
   * @deprecated use {@link Monadics}
   */
  public static <A, B> Collection<B> flatMap(Collection<A> as, Function<A, Collection<B>> f) {
    Collection<B> bs = buildFrom(as);
    for (A a : as) {
      bs.addAll(f.apply(a));
    }
    return bs;
  }

  /**
   * Exactly like {@link #flatMap(java.util.Collection, Function)} but you have to provide the target collection
   * yourself.
   * 
   * @deprecated use {@link Monadics}
   */
  public static <A, B, M extends Collection<B>> M flatMap(Collection<A> as, M bs, Function<A, Collection<B>> f) {
    for (A a : as) {
      bs.addAll(f.apply(a));
    }
    return bs;
  }

  /**
   * Returns the first element in <code>as</code> that satisfies a predicate <code>p</code>.
   * 
   * @deprecated use {@link Monadics}
   */
  public static <A> Option<A> find(Collection<A> as, Predicate<A> p) {
    for (A x : as) {
      if (p.apply(x))
        return some(x);
    }
    return Option.none();
  }

  /**
   * Tests if at least one element in <code>as</code> satisfies predicate <code>p</code>.
   * 
   * @deprecated use {@link Monadics}
   */
  public static <A> boolean exists(Collection<A> as, Predicate<A> p) {
    for (A a : as) {
      if (p.apply(a))
        return true;
    }
    return false;
  }

  /**
   * Return a new collection containing only the elements that satisfy predicate <code>p</code>.
   * <p/>
   * The type of collection <code>as</code> needs a parameterless constructor.
   * 
   * @deprecated use {@link Monadics}
   */
  public static <A, M extends Collection<A>> M filter(M as, Predicate<A> p) {
    @SuppressWarnings("unchecked")
    final M filtered = (M) buildFrom(as);
    for (A a : as) {
      if (p.apply(a))
        filtered.add(a);
    }
    return filtered;
  }

  /** Return the head of list <code>as</code> or <code>none</code>. */
  public static <A> Option<A> head(List<A> as) {
    if (!as.isEmpty()) {
      return some(as.get(0));
    } else {
      return Option.none();
    }
  }

  /** Return the last element of the list. */
  public static <A> Option<A> last(List<A> as) {
    return as.size() > 0 ? some(as.get(as.size() - 1)) : Option.<A> none();
  }

  /** Return the last element of the array. */
  public static <A> Option<A> last(A[] as) {
    return as.length > 0 ? some(as[as.length - 1]) : Option.<A> none();
  }

  /** Make a string from a collection separating each element by <code>sep</code>. */
  public static String mkString(Collection<?> as, String sep) {
    final StringBuilder b = new StringBuilder();
    for (Object a : as)
      b.append(a).append(sep);
    return b.substring(0, Math.max(b.length() - sep.length(), 0));
  }

  /** Append source collection <code>as</code> to <code>target</code>. */
  public static <A, T extends Collection<A>, S extends Iterable<? extends A>> T appendTo(T target, S as) {
    for (A a : as)
      target.add(a);
    return target;
  }

  /** Append source collections <code>as</code> to <code>target</code>. */
  public static <A, T extends Collection<A>, S extends Iterable<? extends A>> T appendToM(T target, S... as) {
    for (S s : as) {
      for (A a : s)
        target.add(a);
    }
    return target;
  }

  /** Append source collections <code>as</code> to <code>target</code>. */
  public static <A, T extends Collection<A>, X extends A> T appendToA(T target, X... as) {
    java.util.Collections.addAll(target, as);
    return target;
  }

  /** Concatenates two iterables into a new list. */
  public static <A, M extends Iterable<? extends A>> List<A> concat(M as, M bs) {
    List<A> x = new ArrayList<A>();
    for (A a : as)
      x.add(a);
    for (A b : bs)
      x.add(b);
    return x;
  }

  /** Concatenates two lists. */
  public static <A> List<A> concat(List<? extends A> as, List<? extends A> bs) {
    List<A> x = new ArrayList<A>();
    for (A a : as)
      x.add(a);
    for (A b : bs)
      x.add(b);
    return x;
  }

  /**
   * Merge two maps where <code>b</code> takes precedence.
   *
   * @return a new immutable map
   */
  public static <A, B> Map<A, B> merge(Map<? extends A, ? extends B> a, Map<? extends A, ? extends B> b) {
    final Map<A, B> x = new HashMap<A, B>();
    x.putAll(a);
    x.putAll(b);
    return java.util.Collections.unmodifiableMap(x);
  }

  /**
   * Merge two sets into one. <code>b</code> takes precedence over <code>a</code>.
   *
   * @return a new immutable set
   */
  public static <A> Set<A> merge(Set<? extends A> a, Set<? extends A> b) {
    final Set<A> x = new HashSet<A>();
    x.addAll(a);
    x.addAll(b);
    return java.util.Collections.unmodifiableSet(x);
  }

  /** Drain all elements of <code>as</code> into a list. */
  public static <A> List<A> toList(Iterator<? extends A> as) {
    final List<A> t = new ArrayList<A>();
    while (as.hasNext()) {
      t.add(as.next());
    }
    return t;
  }

  /** Create a list of tuples (K, V) from a map. */
  public static <K, V> List<Tuple<K, V>> toList(Map<K, V> map) {
    List<Tuple<K, V>> list = new ArrayList<Tuple<K, V>>();
    for (Entry<K, V> entry : map.entrySet()) {
      list.add(Tuple.tuple(entry.getKey(), entry.getValue()));
    }
    return list;
  }

  public static <K, V> Map<K, V> toList(Tuple<? extends K, ? extends V>... ts) {
    final Map<K, V> map = new HashMap<K, V>(ts.length);
    for (Tuple<? extends K, ? extends V> t : ts) {
      map.put(t.getA(), t.getB());
    }
    return map;
  }

  /** Drain all elements of <code>as</code> into a list. */
  public static <A> List<A> toList(Collection<A> as) {
    return new ArrayList<A>(as);
  }

  /** Return nil if <code>a</code> is null or a list containing <code>a</code> otherwise. */
  @SuppressWarnings("unchecked")
  public static <A> List<A> toList(A a) {
    return a != null ? list(a) : Collections.<A> nil();
  }

  /** Return the list as is or nil, if <code>as</code> is null. */
  public static <A> List<A> mkList(List<A> as) {
    return as != null ? as : Collections.<A> nil();
  }

  /** Create a list from an array. */
  public static <A> List<A> list(A... as) {
    final List<A> t = new ArrayList<A>();
    java.util.Collections.addAll(t, as);
    return t;
  }

  /** The empty list. */
  @SuppressWarnings("unchecked")
  public static <A> List<A> nil() {
    return java.util.Collections.EMPTY_LIST;
  }

  /** The empty list. */
  @SuppressWarnings("unchecked")
  public static <A> List<A> nil(Class<A> type) {
    return java.util.Collections.EMPTY_LIST;
  }

  /** Construct a new list by prepending an element to a given list. */
  public static <A> List<A> cons(A a, List<? extends A> as) {
    final List<A> target = new ArrayList<A>(as.size() + 1);
    target.add(a);
    target.addAll(as);
    return target;
  }

  /** Create a set from an array. */
  public static <A> Set<A> set(A... as) {
    final Set<A> t = new HashSet<A>(as.length);
    java.util.Collections.addAll(t, as);
    return t;
  }

  /** Create a set from a list. */
  public static <A> Set<A> toSet(List<A> as) {
    Set<A> r = new HashSet<A>(as.size());
    for (A a : as)
      r.add(a);
    return r;
  }

  /** Create a map from a list of tuples (K, V). */
  public static <K, V> Map<K, V> map(Tuple<? extends K, ? extends V>... ts) {
    final Map<K, V> map = new HashMap<K, V>(ts.length);
    for (Tuple<? extends K, ? extends V> t : ts) {
      map.put(t.getA(), t.getB());
    }
    return map;
  }

  /** Create a sorted map from a list of tuples (K, V) based on the natural ordering of K. */
  public static <K, V> SortedMap<K, V> smap(Tuple<? extends K, ? extends V>... ts) {
    final SortedMap<K, V> map = new TreeMap<K, V>();
    for (Tuple<? extends K, ? extends V> t : ts) {
      map.put(t.getA(), t.getB());
    }
    return map;
  }

  /** Create a dictionary from a list of tuples (K, V). */
  public static <K, V> Dictionary<K, V> dict(Tuple<? extends K, ? extends V>... ts) {
    final Dictionary<K, V> dict = new Hashtable<K, V>(ts.length);
    for (Tuple<? extends K, ? extends V> t : ts) {
      dict.put(t.getA(), t.getB());
    }
    return dict;
  }

  /** Create properties from a list of tuples (K, V). */
  public static Properties properties(Tuple<String, String>... ts) {
    Properties a = new Properties();
    for (Tuple<String, String> t : ts) {
      a.setProperty(t.getA(), t.getB());
    }
    return a;
  }

  /** Convert a properties object into a typed immutable map. */
  public static Map<String, String> toMap(final Properties p) {
    final Map<String, String> m = new HashMap<String, String>();
    for (Map.Entry e : p.entrySet()) {
      m.put(e.getKey().toString(), e.getValue().toString());
    }
    return java.util.Collections.unmodifiableMap(m);
  }

  /**
   * Partition a list after some predicate <code>group</code> into <code>map</code>.
   * <p/>
   * Use e.g. <code>ArrayListMultimap.create()</code> to create a multimap.
   *
   * @see #groupBy(Iterable, Function)
   */
  public static <K, V> Multimap<K, V> groupBy(Multimap<K, V> map,
                                              Iterable<? extends V> values,
                                              Function<? super V, ? extends K> group) {
    for (V value : values) {
      final K key = group.apply(value);
      map.put(key, value);
    }
    return map;
  }

  /**
   * Partition a list after some predicate <code>group</code> into <code>map</code>.
   *
   * @return an {@link ImmutableMultimap}
   * @see #groupBy(com.google.common.collect.Multimap, Iterable, Function)
   */
  public static <K, V> ImmutableMultimap<K, V> groupBy(Iterable<? extends V> values,
                                                       Function<? super V, ? extends K> group) {
    final ImmutableMultimap.Builder<K, V> map = ImmutableMultimap.builder();
    for (V value : values) {
      final K key = group.apply(value);
      map.put(key, value);
    }
    return map.build();
  }

  /**
   * Partition a list after some predicate <code>group</code> into <code>map</code>.
   */
  public static <K, V, X> Multimap<K, V> makeMap(Multimap<K, V> map,
                                                 Iterable<? extends X> values,
                                                 Function<? super X, Tuple<K, V>> group) {
    for (X value : values) {
      final Tuple<K, V> entry = group.apply(value);
      map.put(entry.getA(), entry.getB());
    }
    return map;
  }

  /** Partition a list in chunks of size <code>size</code>. The last chunk may be smaller. */
  public static <A> List<List<A>> grouped(List<A> as, int size) {
    final List<List<A>> grouped = new ArrayList<List<A>>((as.size() / size) + 1);
    List<A> group = new ArrayList<A>(size);
    grouped.add(group);
    int count = size;
    for (A a : as) {
      if (count == 0) {
        group = new ArrayList<A>(size);
        grouped.add(group);
        count = size;
      }
      group.add(a);
      count--;
    }
    return grouped;
  }

  /** Create a list of unique elements determined by a given criteria. */
  public static <A, B> Collection<A> unique(List<A> as, Function<A, B> criteria) {
    final Map<B, A> unique = new HashMap<B, A>();
    for (A a : as) {
      unique.put(criteria.apply(a), a);
    }
    return unique.values();
  }

  /**
   * Partition a list after some predicate <code>keyGen</code>. The partition function has to make sure that keys are
   * unique per list element because each key holds only one value. Later values overwrite newer ones.
   * <p/>
   * The resulting map is an immutable {@link java.util.HashMap}.
   * 
   * @see #asMap(java.util.Map, java.util.List, Function)
   */
  public static <K, V> Map<K, V> asMap(List<V> values, Function<V, K> keyGen) {
    return java.util.Collections.unmodifiableMap(asMap(new HashMap<K, V>(), values, keyGen));
  }

  /**
   * Partition a list after some predicate <code>keyGen</code> into <code>map</code>. The partition function has to make
   * sure that keys are unique per list element because each key holds only one value. Later values overwrite newer
   * ones.
   * 
   * @see #asMap(java.util.List, Function)
   */
  public static <K, V> Map<K, V> asMap(Map<K, V> map, List<V> values, Function<V, K> keyGen) {
    for (V value : values) {
      final K key = keyGen.apply(value);
      map.put(key, value);
    }
    return map;
  }

  /** Create an array from a collection. */
  @SuppressWarnings("unchecked")
  public static <A, B extends A> A[] toArray(Class<A> elemType, Collection<B> a) {
    return a.toArray((A[]) Array.newInstance(elemType, a.size()));
  }

  /** Create an iterator form an array. */
  public static <A> Iterator<A> iterator(final A... as) {
    return new Iterator<A>() {
      private int i = 0;

      @Override
      public boolean hasNext() {
        return as.length > i;
      }

      @Override
      public A next() {
        if (i < as.length) {
          return as[i++];
        } else {
          throw new NoSuchElementException();
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /** Create an iterator that repeats <code>a</code> for the said times. */
  public static <A, X extends A> Iterator<A> repeat(final X a, final int times) {
    return new Iterator<A>() {
      private int count = times;

      @Override
      public boolean hasNext() {
        return count > 0;
      }

      @Override
      public A next() {
        count--;
        return a;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /** Join two iterators. */
  public static <A> Iterator<A> join(final Iterator<A> a, final Iterator<A> b) {
    return new Iterator<A>() {
      @Override
      public boolean hasNext() {
        return a.hasNext() || b.hasNext();
      }

      @Override
      public A next() {
        return a.hasNext() ? a.next() : b.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Make an Iterator usable in a for comprehension like this:
   * 
   * <pre>
   *   Iterator&lt;A&gt; as = ...
   *   for (A a : forc(as)) {
   *     ...
   *   }
   * </pre>
   */
  public static <A> Iterable<A> forc(final Iterator<A> as) {
    return new Iterable<A>() {
      @Override public Iterator<A> iterator() {
        return as;
      }
    };
  }

  public static <A> Function<Option<A>, List<A>> optionToList() {
    return new Function<Option<A>, List<A>>() {
      @Override
      public List<A> apply(Option<A> as) {
        return as.list();
      }
    };
  }

  public static <A, B> Function<A[], List<B>> flatMapArrayToList(final Function<A, List<B>> f) {
    return new Function<A[], List<B>>() {
      @Override
      public List<B> apply(A[] as) {
        return Monadics.mlist(as).bind(f).value();
      }
    };
  }

  /** Turn an option into an iterator. */
  public static <A> Function<Option<A>, Iterator<A>> optionToIterator() {
    return new Function<Option<A>, Iterator<A>>() {
      @Override
      public Iterator<A> apply(Option<A> as) {
        return as.iterator();
      }
    };
  }

  /** Functional version of {@link org.opencastproject.util.data.Arrays#head(A[])}. */
  public static <A> Function<List<A>, Option<A>> head() {
    return new Function<List<A>, Option<A>>() {
      @Override
      public Option<A> apply(List<A> as) {
        return Collections.head(as);
      }
    };
  }

  /** Sort a list. */
  public static <A extends Comparable> Function<List<A>, List<A>> sort() {
    return new Function<List<A>, List<A>>() {
      @Override
      public List<A> apply(List<A> as) {
        List<A> asCopy = new ArrayList<A>(as);
        java.util.Collections.sort(asCopy);
        return asCopy;
      }
    };
  }

  /** Create a function that checks if its argument is contained in <code>as</code>. */
  public static <A> Function<A, Boolean> containedIn(final List<A> as) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        return as.contains(a);
      }
    };
  }

  /** Curried version of {@link List#contains(Object)}. */
  public static <A> Function<List<A>, Function<A, Boolean>> containedIn() {
    return new Function<List<A>, Function<A, Boolean>>() {
      @Override
      public Function<A, Boolean> apply(final List<A> as) {
        return containedIn(as);
      }
    };
  }

  public static <A> Function<Option<A>, A> getOrElse(final A a) {
    return new Function<Option<A>, A>() {
      @Override
      public A apply(Option<A> ao) {
        return ao.getOrElse(a);
      }
    };
  }

  /** Concat (aka flatten) a collection of collections by concatenating them all. [[a]] -> [a] */
  public static <A, M extends Collection<? extends Collection<A>>> List<A> concat(M as) {
    final List<A> target = new ArrayList<A>(as.size());
    for (Collection<A> a : as) {
      target.addAll(a);
    }
    return target;
  }
}
