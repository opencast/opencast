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


package org.opencastproject.util.data;

import static org.opencastproject.util.EqualsUtil.ne;
import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.data.Cells.fcell;
import static org.opencastproject.util.data.Tuple.tuple;

/**
 * Value cell, a mutable data container.
 *
 * Cells provide a pattern that reverses the listener pattern. Instead of propagating changes of a value to registered
 * listeners via callbacks, the dependent object just holds a reference to a cell and pulls the value when needed.
 *
 * Cells must not contain null!
 *
 * @param <A>
 *          the value type
 */
public final class VCell<A> extends Cell<A> {
  private volatile A a;
  private int change;
  private final boolean stable;

  private final Object lock = new Object();

  public VCell(A a, boolean stable) {
    this.a = notNull(a, "a");
    this.stable = stable;
    change = 1;
  }

  /** Constructor function. */
  public static <A> VCell<A> cell(A a) {
    return new VCell<A>(a, true);
  }

  /** Create a cell containing none. */
  public static <A> VCell<Option<A>> ocell() {
    return cell(Option.<A> none());
  }

  /** Get the cell's value. */
  @Override
  public A get() {
    return a;
  }

  @Override
  protected Tuple<A, Object> change() {
    synchronized (lock) {
      if (!stable)
        change += 1;
      return tuple(a, (Object) change);
    }
  }

  /** Set the cell's value. */
  public void set(A a) {
    synchronized (lock) {
      if (ne(a, this.a)) {
        this.a = a;
        change += 1;
      }
    }
  }

  public <B> Cell<B> lift(Function<A, B> f) {
    return fcell(this, f);
  }
}
