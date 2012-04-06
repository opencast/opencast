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
public abstract class Effect2<A, B> extends Function2<A, B, Void> {

  @Override
  public Void apply(A a, B b) {
    run(a, b);
    return null;
  }

  /**
   * Run the side effect.
   */
  protected abstract void run(A a, B b);

  /**
   * Version of {@link Effect0} that allows for throwing a checked exception.
   */
  public abstract static class X<A, B> extends Effect2<A, B> {

    @Override
    protected void run(A a, B b) {
      try {
        xrun(a, b);
      } catch (Exception e) {
        throw new FunctionException(e);
      }
    }

    /**
     * Run the side effect. An exception may be thrown which gets transformed into a
     * {@link FunctionException}. To change this behaviour also override {@link #run(Object, Object)}.
     */
    protected abstract void xrun(A a, B b) throws Exception;
  }
}
