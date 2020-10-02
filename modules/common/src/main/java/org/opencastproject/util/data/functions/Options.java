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

package org.opencastproject.util.data.functions;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.List;

/** {@link Option} related functions. */
public final class Options {
  private Options() {
  }

  /** m (m a) -&gt; m a */
  public static <A> Option<A> join(Option<Option<A>> a) {
    return a.bind(Functions.<Option<A>> identity());
  }

  public static <A> Function<Option<A>, List<A>> asList() {
    return new Function<Option<A>, List<A>>() {
      @Override
      public List<A> apply(Option<A> a) {
        return a.list();
      }
    };
  }

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

  /** Apply effect <code>e</code> to the result of <code>f</code> which is then returned. */
  public static <A, B> Function<A, Option<B>> foreach(final Function<? super A, ? extends Option<? extends B>> f,
          final Effect<? super B> e) {
    return new Function<A, Option<B>>() {
      @Override
      public Option<B> apply(A a) {
        return (Option<B>) f.apply(a).foreach(e);
      }
    };
  }

}
