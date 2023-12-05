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

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Iterators {
  private Iterators() {
  }

  /** Integer range. Upper bound excluded. */
  public static Iterator<Integer> intRangeE(final int start, final int end) {
    return new Iter<Integer>() {
      private int i = start;

      @Override public boolean hasNext() {
        return i < end;
      }

      @Override public Integer next() {
        if (hasNext()) {
          int r = i;
          i++;
          return r;
        } else {
          throw new NoSuchElementException();
        }
      }
    };
  }

  /** An infinite stream of <code>a</code>s. */
  public static <A> Iterator<A> constant(final A a) {
    return new Iter<A>() {
      @Override public boolean hasNext() {
        return true;
      }

      @Override public A next() {
        return a;
      }
    };
  }

  private abstract static class Iter<A> implements Iterator<A> {
    @Override
    public final void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
