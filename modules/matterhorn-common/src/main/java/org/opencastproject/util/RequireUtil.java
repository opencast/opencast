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

import org.apache.commons.lang3.StringUtils;

/**
 * Utility functions for handling common requirements.
 */
public final class RequireUtil {
  private RequireUtil() {

  }

  /**
   * Require an expression to hold true.
   *
   * @param expr
   *          the expression
   * @param exprName
   *          the name of the expression used to create an error message in case <code>expr</code> evaluates to false
   * @throws IllegalArgumentException
   *          in case <code>expr</code> evaluates to false
   */
  public static void requireTrue(boolean expr, String exprName) {
    if (!expr) {
      throw new IllegalArgumentException("Requirement '" + exprName + "' must hold true");
    }
  }

  /**
   * Require an expression to be false.
   *
   * @param expr
   *         the expression
   * @param exprName
   *         the name of the expression used to create an error message in case <code>expr</code> evaluates to false
   * @throws IllegalArgumentException
   *         in case <code>expr</code> evaluates to true
   */
  public static void requireFalse(boolean expr, String exprName) {
    if (expr) {
      throw new IllegalArgumentException("Requirement '" + exprName + "' must be false");
    }
  }

  /**
   * The given value must not be null.
   * <p/>
   * Example:
   *
   * <pre>
   * class A {
   *   private String a;
   *
   *   A(String a) {
   *     this.a = notNull(a, &quot;a&quot;);
   *   }
   * }
   * </pre>
   *
   * @param value
   *          the value to check for null
   * @param valueName
   *          the name of the value; used in error message
   * @return the value, if it is not null
   * @throws IllegalArgumentException
   *           in case of <code>value</code> being null
   */
  public static <A> A notNull(A value, String valueName) {
    if (value == null)
      throw new IllegalArgumentException(valueName + " must not be null");
    return value;
  }

  /**
   * The given value must not be null or empty.
   * <p/>
   * Example:
   *
   * <pre>
   * class A {
   *   private String a;
   *
   *   A(String a) {
   *     this.a = notEmpty(a, &quot;a&quot;);
   *   }
   * }
   * </pre>
   *
   * @param value
   *          the value to check for emptiness
   * @param valueName
   *          the name of the value; used in error message
   * @return the value, if it is not empty
   * @throws IllegalArgumentException
   *           in case of <code>value</code> being empty
   */
  public static String notEmpty(String value, String valueName) {
    if (StringUtils.isEmpty(value))
      throw new IllegalArgumentException(valueName + " must not be null or empty");
    return value;
  }

  /**
   * The given string value must not be blank, empty nor {@code null}. Otherwise, an {@link IllegalArgumentException} is
   * thrown.
   *
   * @param value
   *          the value to check for not being empty
   * @param valueName
   *          the name of the value
   * @return the value, if not blank
   */
  public static String requireNotBlank(String value, String valueName) {
    if (StringUtils.isBlank(value))
      throw new IllegalArgumentException(valueName + " must not be null or blank");
    return value;
  }

  /** The value may be null but if it is not null it must not be of size 0. */
  public static String nullOrNotEmpty(String value, String valueName) {
    if (value != null && value.length() == 0)
      throw new IllegalArgumentException(valueName + " must either be null or not empty");
    return value;
  }

  public static double between(double value, double min, double max) {
    if (min <= value && value <= max)
      return value;
    throw new IllegalArgumentException(value + " must be between " + min + " and " + max);
  }

  public static int min(int value, int min) {
    if (min <= value)
      return value;
    throw new IllegalArgumentException(value + " must not be smaller than " + min);
  }
}
