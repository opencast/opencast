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
public abstract class Effect0 extends Function0<Void> {

  @Override
  public final Void apply() {
    run();
    return null;
  }

  /**
   * Run the side effect.
   */
  protected abstract void run();

  /**
   * Version of {@link Effect0} that allows for throwing a checked exception.
   */
  public abstract static class X extends Effect0 {

    @Override
    protected void run() {
      try {
        xrun();
      } catch (Exception e) {
        throw new FunctionException(e);
      }
    }

    /**
     * Run the side effect. An exception may be thrown which gets transformed into a
     * {@link FunctionException}. To change this behaviour also override {@link #run()}.
     */
    protected abstract void xrun() throws Exception;
  }
}
