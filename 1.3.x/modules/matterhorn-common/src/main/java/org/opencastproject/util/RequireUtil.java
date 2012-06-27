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

package org.opencastproject.util;

/**
 * Utility functions for handling common requirements.
 */
public final class RequireUtil {
  private RequireUtil() {

  }

  /**
   * The given value must not be null.
   * <p/>
   * Example:
   * <pre>
   * class A {
   *   private String a;
   *   A(String a) {
   *     this.a = notNull(a, "a");
   *   }
   * }
   * </pre>
   *
   * @param value
   *          the value to check for null
   * @param valueName
   *          the name of the value; used in error message
   * @return the value, if it is not null
   * @throws IllegalArgumentException in case of <code>value</code> being null
   */
  public static <A> A notNull(A value, String valueName) {
    if (value == null)
      throw new IllegalArgumentException(valueName + " must not be null");
    return value;
  }
}
