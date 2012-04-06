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
 * Function of arity 0, i.e. a constant function.
 *
 * @see X
 */
public abstract class Function0<A> {

  /**
   * Apply function yielding a constant value. Don't be tempted to become impure!
   */
  public abstract A apply();

  /**
   * Version of {@link Function0} that allows for throwing a checked exception.
   */
  public abstract static class X<A> extends Function0<A> {

    @Override
    public A apply() {
      try {
        return xapply();
      } catch (Exception e) {
        throw new FunctionException(e);
      }
    }

    /**
     * Apply function yielding a constant value. Don't be tempted to become impure!
     * The application may throw an exception which gets transformed into a {@link FunctionException}.
     * To change this behaviour also override {@link #apply()}.
     */
    public abstract A xapply() throws Exception;
  }
}
