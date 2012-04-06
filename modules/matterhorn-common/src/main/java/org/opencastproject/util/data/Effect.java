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

/**
 * Run a side effect.
 *
 * @see X
 */
public abstract class Effect<A> extends Function<A, Void> {

  @Override
  public final Void apply(A a) {
    run(a);
    return null;
  }

  /**
   * Run the side effect.
   */
  protected abstract void run(A a);

  /**
   * Version of {@link Effect} that allows for throwing a checked exception.
   */
  public abstract static class X<A> extends Effect<A> {

    @Override
    protected void run(A a) {
      try {
        xrun(a);
      } catch (Exception e) {
        throw new FunctionException(e);
      }
    }

    /**
     * Run the side effect. An exception may be thrown which gets transformed into a
     * {@link FunctionException}. To change this behaviour also override {@link #run(Object)}.
     */
    protected abstract void xrun(A a) throws Exception;
  }
}
