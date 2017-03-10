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
package org.opencastproject.assetmanager.impl;

import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.data.Either;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

/**
 * A protected resource.
 */
public final class Protected<A> {
  private final Either<UnauthorizedException, A> wrapped;

  public Protected(Either<UnauthorizedException, A> wrapped) {
    this.wrapped = wrapped;
  }

  public static <A> Protected<A> granted(A a) {
    return new Protected<A>(Either.<UnauthorizedException, A>right(a));
  }

  public static <A> Protected<A> rejected(UnauthorizedException e) {
    return new Protected<A>(Either.<UnauthorizedException, A>left(e));
  }

  public Either<UnauthorizedException, A> get() {
    return wrapped;
  }

  public boolean isGranted() {
    return wrapped.isRight();
  }

  /** Throws an exception if _not_ granted. */
  public A getGranted() {
    return wrapped.right().value();
  }

  public boolean isRejected() {
    return wrapped.isLeft();
  }

  public UnauthorizedException getRejected() {
    return wrapped.left().value();
  }

  public Opt<A> getGrantedOpt() {
    return wrapped.right().toOption().toOpt();
  }

  public static <A> Fn<Protected<A>, Opt<A>> getGrantedOptFn() {
    return new Fn<Protected<A>, Opt<A>>() {
      @Override public Opt<A> apply(Protected<A> p) {
        return p.getGrantedOpt();
      }
    };
  }

  public static <A> Fn<Protected<A>, Either<UnauthorizedException, A>> getFn() {
    return new Fn<Protected<A>, Either<UnauthorizedException, A>>() {
      @Override public Either<UnauthorizedException, A> apply(Protected<A> p) {
        return p.get();
      }
    };
  }
}
