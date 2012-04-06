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

package org.opencastproject.util.data.functions;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;

/**
 * Functions operating on integers.
 */
public final class Integers {
  
  private Integers() {    
  }

  /**
   * Addition.
   */
  public static Function<Integer, Integer> add(final Integer val) {
    return new Function<Integer, Integer>() {
      @Override
      public Integer apply(Integer number) {
        return number + val;
      }
    };
  }

  /**
   * Addition.
   */
  public static Function2<Integer, Integer, Integer> add() {
    return new Function2<Integer, Integer, Integer>() {
      @Override
      public Integer apply(Integer a, Integer b) {
        return a + b;
      }
    };
  }
}
