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

import java.util.Iterator;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.opencastproject.util.data.Option.some;

/**
 * An algebraic data type representing a disjoint union. By convention left is considered to represent an error while
 * right represents a value.
 * <p/>
 * This implementation of <code>Either</code> is much simpler than implementations you may know from other languages or
 * libraries.
 */
public abstract class Either<A, B> {
  private Either() {
  }

  public abstract LeftProjection<A, B> left();

  public abstract RightProjection<A, B> right();

  public abstract <X> X fold(Match<A, B, X> visitor);

  public abstract <X> X fold(Function<? super A, ? extends X> left, Function<? super B, ? extends X> right);

  public abstract boolean isLeft();

  public abstract boolean isRight();

  public interface Match<A, B, X> {
    X left(A a);

    X right(B b);
  }

  /**
   * Left projection of either.
   */
  public abstract class LeftProjection<A, B> implements Iterable<A> {
    private LeftProjection() {
    }

    public abstract Either<A, B> either();

    public abstract <X> Either<X, B> map(Function<A, X> f);

    public abstract <X> Either<X, B> bind(Function<A, Either<X, B>> f);

    public <X> Either<X, B> flatMap(Function<A, Either<X, B>> f) {
      return bind(f);
    }

    public abstract A value();

    public abstract A getOrElse(A right);

    public abstract Option<A> toOption();

    public abstract List<A> toList();
  }

  /**
   * Right projection of either.
   */
  public abstract class RightProjection<A, B> implements Iterable<B> {

    private RightProjection() {
    }

    public abstract Either<A, B> either();

    public abstract <X> Either<A, X> map(Function<B, X> f);

    public abstract <X> Either<A, X> bind(Function<B, Either<A, X>> f);

    public <X> Either<A, X> flatMap(Function<B, Either<A, X>> f) {
      return bind(f);
    }

    public abstract B value();

    public abstract B getOrElse(B left);

    public abstract Option<B> toOption();

    public abstract List<B> toList();
  }

  /**
   * Create a left.
   */
  public static <A, B> Either<A, B> left(final A left) {
    return new Either<A, B>() {
      @Override
      public <C> C fold(Match<A, B, C> visitor) {
        return visitor.left(left);
      }

      @Override
      public LeftProjection<A, B> left() {
        final Either<A, B> self = this;
        return new LeftProjection<A, B>() {
          @Override
          public A value() {
            return left;
          }

          @Override
          public Option<A> toOption() {
            return some(left);
          }

          @Override
          public List<A> toList() {
            return singletonList(left);
          }

          @Override
          public Iterator<A> iterator() {
            return toList().iterator();
          }

          @Override
          public A getOrElse(A right) {
            return left;
          }

          @Override
          public Either<A, B> either() {
            return self;
          }

          @Override
          public <X> Either<X, B> map(Function<A, X> f) {
            return left(f.apply(left));
          }

          @Override
          public <X> Either<X, B> bind(Function<A, Either<X, B>> f) {
            return f.apply(left);
          }
        };
      }

      @Override
      public RightProjection<A, B> right() {
        final Either<A, B> self = this;
        return new RightProjection<A, B>() {
          @Override
          public Either<A, B> either() {
            return self;
          }

          @Override
          public <X> Either<A, X> map(Function<B, X> f) {
            return left(left);
          }

          @Override
          public <X> Either<A, X> bind(Function<B, Either<A, X>> f) {
            return left(left);
          }

          @Override
          public B value() {
            throw new Error("right projection on left does not have a value");
          }

          @Override
          public B getOrElse(B left) {
            return left;
          }

          @Override
          public Option<B> toOption() {
            return Option.none();
          }

          @Override
          public List<B> toList() {
            return emptyList();
          }

          @Override
          public Iterator<B> iterator() {
            return toList().iterator();
          }
        };
      }

      @Override
      public <C> C fold(Function<? super A, ? extends C> leftf, Function<? super B, ? extends C> rightf) {
        return leftf.apply(left);
      }

      @Override
      public boolean isLeft() {
        return true;
      }

      @Override
      public boolean isRight() {
        return false;
      }
    };
  }

  /**
   * Create a right.
   */
  public static <A, B> Either<A, B> right(final B right) {
    return new Either<A, B>() {
      @Override
      public <C> C fold(Match<A, B, C> visitor) {
        return visitor.right(right);
      }

      @Override
      public LeftProjection<A, B> left() {
        final Either<A, B> self = this;
        return new LeftProjection<A, B>() {
          @Override
          public A value() {
            throw new Error("left projection on right does not have a value");
          }

          @Override
          public Option<A> toOption() {
            return Option.none();
          }

          @Override
          public List<A> toList() {
            return emptyList();
          }

          @Override
          public Iterator<A> iterator() {
            return toList().iterator();
          }

          @Override
          public A getOrElse(A right) {
            return right;
          }

          @Override
          public Either<A, B> either() {
            return self;
          }

          @Override
          public <X> Either<X, B> map(Function<A, X> f) {
            return right(right);
          }

          @Override
          public <X> Either<X, B> bind(Function<A, Either<X, B>> f) {
            return right(right);
          }
        };
      }

      @Override
      public RightProjection<A, B> right() {
        final Either<A, B> self = this;
        return new RightProjection<A, B>() {
          @Override
          public Either<A, B> either() {
            return self;
          }

          @Override
          public <X> Either<A, X> map(Function<B, X> f) {
            return right(f.apply(right));
          }

          @Override
          public <X> Either<A, X> bind(Function<B, Either<A, X>> f) {
            return f.apply(right);
          }

          @Override
          public B value() {
            return right;
          }

          @Override
          public B getOrElse(B left) {
            return right;
          }

          @Override
          public Option<B> toOption() {
            return some(right);
          }

          @Override
          public List<B> toList() {
            return singletonList(right);
          }

          @Override
          public Iterator<B> iterator() {
            return toList().iterator();
          }
        };
      }

      @Override
      public <X> X fold(Function<? super A, ? extends X> leftf, Function<? super B, ? extends X> rightf) {
        return rightf.apply(right);
      }

      @Override
      public boolean isLeft() {
        return false;
      }

      @Override
      public boolean isRight() {
        return true;
      }
    };
  }
}
