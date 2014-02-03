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

import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.functions.Misc.chuck;

/**
 * Function of arity 1.
 * <p/>
 * A general note on function implementations: Each function has an associated X version with
 * the only difference that a checked exception may be thrown on application. This behaviour could
 * be nicely combined into the main function class but with the cost of having no abstract method. Some
 * IDEs such as IntelliJ support nice code folding for SAM (single abstract method) classes.
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
  public <C> Function<C, B> o(final Function<C, A> f) {
    return Functions.o(Function.this, f);
  }

  /** Function composition. <code>g.o(f).apply() == g(f())</code> */
  public Function0<B> o(final Function0<A> f) {
    return Functions.o(Function.this, f);
  }

  /** Turn this function into an effect by discarding its result. */
  public Effect<A> toEffect() {
    return Functions.toEffect(this);
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
     * Apply function to <code>a</code>. Any thrown exception gets "chucked" so that you may
     * catch them as is. See {@link Functions#chuck(Throwable)} for details.
     */
    protected abstract B xapply(A a) throws Exception;
  }

  /** Version of {@link Function} that allows for throwing a checked exception. */
  public abstract static class Xe<A, B, Err> extends Function<A, Either<Err, B>> {
    private final Function<Exception, Err> toErr;

    protected Xe(Function<Exception, Err> toErr) {
      this.toErr = toErr;
    }

    @Override
    public final Either<Err, B> apply(A a) {
      try {
        return right(xapply(a));
      } catch (Exception e) {
        return left(toErr.apply(e));
      }
    }

    protected abstract B xapply(A a) throws Exception;
  }
}
