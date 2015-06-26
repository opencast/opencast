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


package org.opencastproject.metadata.dublincore;

import java.util.Date;

/**
 * A time related algebraic data type.
 */
public abstract class Temporal {

  // prohibit inheritance from outside this class
  private Temporal() {
  }

  /**
   * An instant in time.
   */
  public static Temporal instant(final Date instant) {
    if (instant == null)
      throw new IllegalArgumentException("instant must not be null");
    return new Temporal() {
      public Date getInstant() {
        return instant;
      }

      @Override
      public <A> A fold(Match<A> v) {
        return v.instant(instant);
      }
    };
  }

  /**
   * A period in time limited by at least one instant.
   */
  public static Temporal period(final DCMIPeriod period) {
    if (period == null)
      throw new IllegalArgumentException("period must not be null");
    return new Temporal() {
      public DCMIPeriod getPeriod() {
        return period;
      }

      @Override
      public <A> A fold(Match<A> v) {
        return v.period(period);
      }
    };
  }

  /**
   * A time span measured in milliseconds.
   */
  public static Temporal duration(final long duration) {
    if (duration < 0)
      throw new IllegalArgumentException("duration must be positive or zero");
    return new Temporal() {
      public long getDuration() {
        return duration;
      }

      @Override
      public <A> A fold(Match<A> v) {
        return v.duration(duration);
      }
    };
  }

  /**
   * Safe decomposition.
   */
  public abstract <A> A fold(Match<A> v);

  /**
   * Safe temporal decomposition.
   */
  public interface Match<A> {
    A period(DCMIPeriod period);

    A instant(Date instant);

    A duration(long duration);
  }

}
