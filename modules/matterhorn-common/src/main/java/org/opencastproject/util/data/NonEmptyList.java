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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Non empty list. Also immutable.
 * 
 * @param <A>
 *          content type
 */
public final class NonEmptyList<A> implements List<A> {
  private final List<A> lst;

  public NonEmptyList(A a, A... as) {
    lst = new ArrayList<A>();
    lst.add(a);
    lst.addAll(Arrays.asList(as));
  }

  /**
   * @throws IllegalArgumentException
   *           collection is empty
   */
  public NonEmptyList(Collection<A> as) throws IllegalArgumentException {
    if (as.isEmpty())
      throw new IllegalArgumentException("Collection must not be empty");
    lst = new ArrayList<A>();
    lst.addAll(as);
  }

  @Override
  public int size() {
    return lst.size();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean contains(Object o) {
    return lst.contains(o);
  }

  @Override
  public Iterator<A> iterator() {
    final Iterator<A> it = lst.iterator();
    return new Iterator<A>() {
      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public A next() {
        return it.next();
      }

      @Override
      public void remove() {
        throw err();
      }
    };
  }

  @Override
  public Object[] toArray() {
    return lst.toArray();
  }

  @Override
  public <T> T[] toArray(T[] ts) {
    return lst.toArray(ts);
  }

  @Override
  public boolean add(A a) {
    throw new RuntimeException("List is immutable");
  }

  @Override
  public boolean remove(Object o) {
    throw new RuntimeException("List is immutable");
  }

  @Override
  public boolean containsAll(Collection<?> objects) {
    return lst.containsAll(objects);
  }

  @Override
  public boolean addAll(Collection<? extends A> as) {
    throw err();
  }

  @Override
  public boolean addAll(int i, Collection<? extends A> as) {
    throw err();
  }

  @Override
  public boolean removeAll(Collection<?> objects) {
    throw err();
  }

  @Override
  public boolean retainAll(Collection<?> objects) {
    throw err();
  }

  @Override
  public void clear() {
    throw err();
  }

  @Override
  public A get(int i) {
    return lst.get(i);
  }

  @Override
  public A set(int i, A a) {
    throw err();
  }

  @Override
  public void add(int i, A a) {
    throw err();
  }

  @Override
  public A remove(int i) {
    throw err();
  }

  @Override
  public int indexOf(Object o) {
    return lst.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return lst.lastIndexOf(o);
  }

  @Override
  public ListIterator<A> listIterator() {
    return listIterator(0);
  }

  @Override
  public ListIterator<A> listIterator(int i) {
    final ListIterator<A> it = lst.listIterator();
    return new ListIterator<A>() {
      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public A next() {
        return it.next();
      }

      @Override
      public boolean hasPrevious() {
        return it.hasPrevious();
      }

      @Override
      public A previous() {
        return it.previous();
      }

      @Override
      public int nextIndex() {
        return it.nextIndex();
      }

      @Override
      public int previousIndex() {
        return it.previousIndex();
      }

      @Override
      public void remove() {
        throw err();
      }

      @Override
      public void set(A a) {
        throw err();
      }

      @Override
      public void add(A a) {
        throw err();
      }
    };
  }

  @Override
  public List<A> subList(int i, int i1) {
    throw err();
  }

  private RuntimeException err() {
    return new RuntimeException("List is immutable");
  }
}
