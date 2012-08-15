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

import static org.opencastproject.util.RequireUtil.notNull;

/**
 * A cell is a mutable data container.
 * <p/>
 * Cells provide a pattern that reverses the listener pattern. Instead of propagating changes
 * of a value to registered listeners via callbacks, the dependend object just holds a reference
 * to a cell and pulls the value when needed.
 * <p/>
 * Cells must not contain null!
 *
 * @param <A>
 *         the value type
 */
public class Cell<A> {

  // volatile so that updates happen in a synchronized manner.
  private volatile A a;

  public Cell(A a) {
    this.a = notNull(a, "a");
  }

  /** Get the cell's content. */
  public A get() {
    return a;
  }

  /** Set the cell's content. */
  public void set(A a) {
    this.a = a;
  }

  /** Constructor function. */
  public static <A> Cell<A> cell(A a) {
    return new Cell<A>(a);
  }

//  public void preSet(A newVal, A current) {
//  }
//
//  public void postChange(A current) {
//  }
//
//  public void preChange(A newVal, A current) {
//  }
}
