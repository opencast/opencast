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

import org.opencastproject.util.data.functions.Functions;

import static org.opencastproject.util.data.functions.Misc.chuck;

/**
 * Function of arity 2.
 *
 * @see X
 */
public abstract class Function2<A, B, C> {

  /**
   * Apply function to <code>a</code> and <code>b</code>.
   */
  public abstract C apply(A a, B b);

  /**
   * Currying.
   */
  public Function<B, C> curry(final A a) {
    return Functions.curry(this, a);
  }

  /**
   * Currying.
   */
  public Function<A, Function<B, C>> curry() {
    return Functions.curry(this);
  }

  /**
   * Version of {@link Function2} that allows for throwing a checked exception.
   */
  public abstract static class X<A, B, C> extends Function2<A, B, C> {

    @Override
    public C apply(A a, B b) {
      try {
        return xapply(a, b);
      } catch (Exception e) {
        return chuck(e);
      }
    }

    /**
     * Apply function to <code>a</code>. Any thrown exception gets "chucked" so that you may
     * catch them as is. See {@link Functions#chuck(Throwable)} for details.
     */
    public abstract C xapply(A a, B b) throws Exception;
  }
}
