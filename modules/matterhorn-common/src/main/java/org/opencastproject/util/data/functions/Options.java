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

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.List;

/** {@link Option} related functions. */
public final class Options {
  private Options() {
  }

  public static <A> Function<Option<A>, List<A>> asList() {
    return new Function<Option<A>, List<A>>() {
      @Override
      public List<A> apply(Option<A> a) {
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
  public static final Function<Boolean, Option<Boolean>> toOption = new Function<Boolean, Option<Boolean>>() {
    @Override
    public Option<Boolean> apply(Boolean a) {
      return a ? some(true) : Option.<Boolean> none();
    }
  };

  /** Returns some(message) if predicate is false, none otherwise. */
  public static Option<String> toOption(boolean predicate, String message) {
    return predicate ? Option.<String> none() : some(message);
  }

  /** Sequence a list of options. [Option a] -&gt; Option [a] */
  public static <A> Option<List<A>> sequenceOpt(List<Option<A>> as) {
    final List<A> seq = mlist(as).foldl(new ArrayList<A>(), new Function2<List<A>, Option<A>, List<A>>() {
      @Override
      public List<A> apply(List<A> sum, Option<A> o) {
        for (A a : o) {
          sum.add(a);
          return sum;
        }
        return sum;
      }
    });
    return some(seq);
  }

}
