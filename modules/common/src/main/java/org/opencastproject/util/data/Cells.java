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
import static org.opencastproject.util.data.Tuple.tuple;

public final class Cells {
  private Cells() {
  }

  abstract static class FCell<A> extends Cell<A> {
    protected A a;
    protected Object change = new Object();

    protected final Object lock = new Object();

    protected abstract A calc();

    @Override public A get() {
      synchronized (lock) {
        return calc();
      }
    }

    @Override public <B> Cell<B> lift(Function<A, B> f) {
      return fcell(this, f);
    }

    @Override protected Tuple<A, Object> change() {
      synchronized (lock) {
        return tuple(calc(), change);
      }
    }
  }

  /** Create a memo cell that calculates <code>f</code> once and then returns the value. */
  public static <A> Cell<A> memo(final Function0<A> f) {
    return new FCell<A>() {
      @Override protected A calc() {
        if (a == null) {
          a = f.apply();
        }
        return a;
      }
    };
  }

  public static <B, A> Cell<A> fcell(final Cell<B> master, final Function<B, A> f) {
    return new FCell<A>() {
      @Override protected A calc() {
        final Tuple<B, Object> mChange = master.change();
        if (ne(mChange.getB(), change)) {
          a = f.apply(mChange.getA());
          change = mChange.getB();
        }
        return a;
      }
    };
  }

  public static <B, C, A> Cell<A> fcell(final Cell<B> masterB, final Cell<C> masterC, final Function2<B, C, A> f) {
    return new FCell<A>() {
      @Override protected A calc() {
        final Tuple<B, Object> mChangeB = masterB.change();
        final Tuple<C, Object> mChangeC = masterC.change();
        final Tuple<Object, Object> mChange = tuple(mChangeB.getB(), mChangeC.getB());
        if (ne(mChange, change)) {
          a = f.apply(mChangeB.getA(), mChangeC.getA());
          change = mChange;
        }
        return a;
      }
    };
  }
}
