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
 * Function of arity 0, i.e. a constant function.
 *
 * @see X
 */
public abstract class Function0<A> {

  /**
   * Apply function yielding a constant value. Don't be tempted to become impure!
   */
  public abstract A apply();

  /** Apply this function, then pass the result to <code>f</code>. */
  public <B> Function0<B> then(final Function<A, B> f) {
    return Functions.then(Function0.this, f);
  }

  /** Apply this function and ignore its result, then apply <code>f</code>. */
  public <B> Function0<B> then(final Function0<B> f) {
    return Functions.then(Function0.this, f);
  }

  /** Turn a Function0 into an Effect0 discarding its return value. */
  public Effect0 toEffect() {
    return new Effect0() {
      @Override protected void run() {
        Function0.this.apply();
      }
    };
  }

  /**
   * Version of {@link Function0} that allows for throwing a checked exception.
   */
  public abstract static class X<A> extends Function0<A> {

    @Override
    public A apply() {
      try {
        return xapply();
      } catch (Exception e) {
        return chuck(e);
      }
    }

    /**
     * Apply function to <code>a</code>. Any thrown exception gets "chucked" so that you may
     * catch them as is. See {@link Functions#chuck(Throwable)} for details.
     */
    public abstract A xapply() throws Exception;
  }
}
