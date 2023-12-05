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


package org.opencastproject.util.data;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.util.data.functions.Functions;

import com.entwinemedia.fn.Fn;

/**
 * Function of arity 1.
 *
 * A general note on function implementations: Each function has an associated X version with the only difference that a
 * checked exception may be thrown on application. This behaviour could be nicely combined into the main function class
 * but with the cost of having no abstract method. Some IDEs such as IntelliJ support nice code folding for SAM (single
 * abstract method) classes.
 *
 * @see X
 */
public abstract class Function<A, B> {
  /** Apply function to <code>a</code>. */
  public abstract B apply(A a);

  /** Currying. */
  public Function0<B> curry(final A a) {
    return Functions.curry(this, a);
  }

  /** Currying. */
  public Function<A, Function0<B>> curry() {
    return Functions.curry(this);
  }

  /** Function composition. <code>g.o(f).apply(x) == g(f(x))</code> */
  public <C> Function<C, B> o(final Function<? super C, ? extends A> f) {
    return Functions.o(this, f);
  }

  /** Function composition. <code>g.o(f).apply() == g(f())</code> */
  public Function0<B> o(final Function0<? extends A> f) {
    return Functions.o(this, f);
  }

  /** Left to right composition: <code>f then g = g(f(x))</code> */
  public <C> Function<A, C> then(final Function<? super B, ? extends C> f) {
    return Functions.then(this, f);
  }

  /** @see Functions#rethrow(Function, Function) */
  public Function<A, B> rethrow(final Function<? super Exception, ? extends Exception> transformer) {
    return Functions.rethrow(this, transformer);
  }

  /** @see Functions#handle(Function, Function) */
  public Function<A, B> handle(final Function<? super Exception, B> handler) {
    return Functions.handle(this, handler);
  }

  /** @see Functions#either(Function, Function) */
  public <X> Function<A, Either<X, B>> either(final Function<Exception, X> handler) {
    return Functions.either(this, handler);
  }

  /** Turn this function into an effect by discarding its result. */
  public Effect<A> toEffect() {
    return Functions.toEffect(this);
  }

  public Fn<A, B> toFn() {
    return new Fn<A, B>() {
      @Override
      public B apply(A a) {
        return Function.this.apply(a);
      }
    };
  }

  /** Version of {@link Function} that allows for throwing a checked exception. */
  public abstract static class X<A, B> extends Function<A, B> {
    @Override
    public final B apply(A a) {
      try {
        return xapply(a);
      } catch (Exception e) {
        return chuck(e);
      }
    }

    /**
     * Apply function to <code>a</code>. Any thrown exception gets "chucked" so that you may catch them as is. See
     * {@link Functions#chuck(Throwable)} for details.
     */
    protected abstract B xapply(A a) throws Exception;

  }

}
