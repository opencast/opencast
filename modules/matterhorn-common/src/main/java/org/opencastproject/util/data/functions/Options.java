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
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;

import java.util.List;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/** {@link Option} related functions. */
public final class Options {
  private Options() {
  }

  public static <A> Function<Option<A>, List<A>> asList() {
    return new Function<Option<A>, List<A>>() {
      @Override public List<A> apply(Option<A> a) {
        return a.list();
      }
    };
  }

  public static <A, B> Function<A, Option<B>> never() {
    return new Function<A, Option<B>>() {
      @Override
      public Option<B> apply(A a) {
        return none();
      }
    };
  }

  public static <A> Function0<Option<A>> never2() {
    return new Function0<Option<A>>() {
      @Override
      public Option<A> apply() {
        return none();
      }
    };
  }

  /** Function that turns <code>true</code> into <code>some(true)</code> and false into <code>none</code>. */
  private static Function<Boolean, Option<Boolean>> toOption = new Function<Boolean, Option<Boolean>>() {
    @Override
    public Option<Boolean> apply(Boolean a) {
      return a ? some(true) : Option.<Boolean> none();
    }
  };
}
