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


package org.opencastproject.metadata.api.util;

import java.util.Date;

/**
 * An interval in time, possibly unbounded on one end.
 * The interval is described by its bounds which are absolutely placed instants in time with a precision
 * defined by {@link java.util.Date}.
 */
public abstract class Interval {

  private Interval() {
  }

  /**
   * Constructor function for a bounded interval.
   */
  public static Interval boundedInterval(final Date leftBound, final Date rightBound) {
    return new Interval() {
      @Override
      public boolean isLeftInfinite() {
        return false;
      }

      @Override
      public boolean isRightInfinite() {
        return false;
      }

      @Override
      public <A> A fold(Match<A> visitor) {
        return visitor.bounded(leftBound, rightBound);
      }
    };
  }

  /**
   * Constructor function for a right bounded interval, i.e. an interval with an infinite left endpoint.
   */
  public static Interval rightBoundedInterval(final Date rightBound) {
    return new Interval() {
      @Override
      public boolean isLeftInfinite() {
        return true;
      }

      @Override
      public boolean isRightInfinite() {
        return false;
      }

      @Override
      public <A> A fold(Match<A> visitor) {
        return visitor.leftInfinite(rightBound);
      }
    };
  }

  /**
   * Constructor function for a left bounded interval, i.e. an interval with an infinite right endpoint.
   */
  public static Interval leftBoundedInterval(final Date leftBound) {
    return new Interval() {
      @Override
      public boolean isLeftInfinite() {
        return false;
      }

      @Override
      public boolean isRightInfinite() {
        return true;
      }

      @Override
      public <A> A fold(Match<A> visitor) {
        return visitor.rightInfinite(leftBound);
      }
    };
  }

  /**
   * Create an interval from two dates. One of the dates may be null to indicate an infinite endpoint.
   *
   * @throws IllegalArgumentException
   *            if boths bounds are null
   */
  public static Interval fromValues(final Date leftBound, final Date rightBound) {
    if (leftBound != null && rightBound != null)
      return boundedInterval(leftBound, rightBound);
    if (leftBound != null)
      return leftBoundedInterval(leftBound);
    if (rightBound != null)
      return rightBoundedInterval(rightBound);
    throw new IllegalArgumentException("Please give at least one bound");
  }

  /**
   * Test if both endpoints are not infinite.
   */
  public boolean isBounded() {
    return !(isLeftInfinite() || isRightInfinite());
  }

  /**
   * Test if the right endpoint is infinite.
   */
  public abstract boolean isLeftInfinite();

  /**
   * Test if the left endpoint is infinite.
   */
  public abstract boolean isRightInfinite();

  /**
   * Safe decomposition.
   */
  public abstract <A> A fold(Match<A> visitor);

  public interface Match<A> {
    A bounded(Date leftBound, Date rightBound);

    A leftInfinite(Date rightBound);

    A rightInfinite(Date leftBound);
  }

}
