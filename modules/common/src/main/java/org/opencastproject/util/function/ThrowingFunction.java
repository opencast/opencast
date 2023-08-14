/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.util.function;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a function that accepts one argument and produces a result.
 * Unlike {@code Function}, this implementation can throw a checked exception.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Throwable> {
  /**
   * Applies this function to the given argument.
   *
   * @param t the function argument
   * @return the function result
   */
  R apply(T t) throws E;

  /**
   * Returns a composed function that first applies the {@code before}
   * function to its input, and then applies this function to the result.
   * If evaluation of either function throws an exception, it is relayed to
   * the caller of the composed function.
   *
   * @param <V> the type of input to the {@code before} function, and to the
   *           composed function
   * @param before the function to apply before this function is applied
   * @return a composed function that first applies the {@code before}
   * function and then applies this function
   * @throws NullPointerException if before is null
   *
   * @see #andThen(Function)
   */
  default <V> ThrowingFunction<V, R, ? extends Throwable> compose(Function<? super V, ? extends T> before) {
    Objects.requireNonNull(before);
    return (V v) -> apply(before.apply(v));
  }

  /**
   * Returns a composed function that first applies the {@code before}
   * function to its input, and then applies this function to the result.
   * If evaluation of either function throws an exception, it is relayed to
   * the caller of the composed function.
   *
   * @param <V> the type of input to the {@code before} function, and to the
   *           composed function
   * @param before the function to apply before this function is applied
   * @return a composed function that first applies the {@code before}
   * function and then applies this function
   * @throws NullPointerException if before is null
   *
   * @see #andThen(Function)
   */
  default <V> ThrowingFunction<V, R, ? extends Throwable> compose(ThrowingFunction<? super V, ? extends T, ? extends Throwable> before) {
    Objects.requireNonNull(before);
    return (V v) -> apply(before.apply(v));
  }

  /**
   * Returns a composed function that first applies this function to
   * its input, and then applies the {@code after} function to the result.
   * If evaluation of either function throws an exception, it is relayed to
   * the caller of the composed function.
   *
   * @param <V> the type of output of the {@code after} function, and of the
   *           composed function
   * @param after the function to apply after this function is applied
   * @return a composed function that first applies this function and then
   * applies the {@code after} function
   * @throws NullPointerException if after is null
   *
   * @see #compose(Function)
   */
  default <V> ThrowingFunction<T, V, ? extends Throwable> andThen(Function<? super R, ? extends V> after) {
    Objects.requireNonNull(after);
    return (T t) -> after.apply(apply(t));
  }

  /**
   * Returns a composed function that first applies this function to
   * its input, and then applies the {@code after} function to the result.
   * If evaluation of either function throws an exception, it is relayed to
   * the caller of the composed function.
   *
   * @param <V> the type of output of the {@code after} function, and of the
   *           composed function
   * @param after the function to apply after this function is applied
   * @return a composed function that first applies this function and then
   * applies the {@code after} function
   * @throws NullPointerException if after is null
   *
   * @see #compose(Function)
   */
  default <V> ThrowingFunction<T, V, ? extends Throwable> andThen(ThrowingFunction<? super R, ? extends V, ? extends Throwable> after) {
    Objects.requireNonNull(after);
    return (T t) -> after.apply(apply(t));
  }

  /**
   * Returns a function that always returns its input argument.
   *
   * @param <T> the type of the input and output objects to the function
   * @return a function that always returns its input argument
   */
  static <T> ThrowingFunction<T, T, ? extends Throwable> identity() {
    return t -> t;
  }
}
