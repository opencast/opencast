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
package org.opencastproject.archive.base;

import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;

import java.util.List;

/** A protected resource. */
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

  public List<A> getPassedAsList() {
    return wrapped.right().toOption().list();
  }

  public static <A> Function<Protected<A>, List<A>> getPassedAsListf() {
    return new Function<Protected<A>, List<A>>() {
      @Override public List<A> apply(Protected<A> p) {
        return p.getPassedAsList();
      }
    };
  }

  public static <A> Function<Protected<A>, Either<UnauthorizedException, A>> getf() {
    return new Function<Protected<A>, Either<UnauthorizedException, A>>() {
      @Override public Either<UnauthorizedException, A> apply(Protected<A> p) {
        return p.get();
      }
    };
  }
}
