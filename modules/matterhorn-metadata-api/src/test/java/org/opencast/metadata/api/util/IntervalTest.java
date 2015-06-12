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


package org.opencast.metadata.api.util;

import org.junit.Test;
import org.opencastproject.metadata.api.util.Interval;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.metadata.api.util.Interval.boundedInterval;
import static org.opencastproject.metadata.api.util.Interval.rightBoundedInterval;
import static org.opencastproject.metadata.api.util.Interval.leftBoundedInterval;

public class IntervalTest {

  @Test
  public void testInterval() {
    Interval closed = boundedInterval(new Date(), new Date());
    assertTrue(closed.isBounded());
    assertFalse(closed.isLeftInfinite());
    assertFalse(closed.isRightInfinite());
    Interval.Match<Integer> visitor = new Interval.Match<Integer>() {
      @Override
      public Integer bounded(Date leftBound, Date rightBound) {
        return 1;
      }

      @Override
      public Integer leftInfinite(Date rightBound) {
        return 2;
      }

      @Override
      public Integer rightInfinite(Date leftBound) {
        return 3;
      }
    };
    assertSame(1, closed.fold(visitor));
    Interval leftOpen = rightBoundedInterval(new Date());
    assertFalse(leftOpen.isBounded());
    assertTrue(leftOpen.isLeftInfinite());
    assertFalse(leftOpen.isRightInfinite());
    assertSame(2, leftOpen.fold(visitor));
    Interval rightOpen = leftBoundedInterval(new Date());
    assertFalse(rightOpen.isBounded());
    assertFalse(rightOpen.isLeftInfinite());
    assertTrue(rightOpen.isRightInfinite());
    assertSame(3, rightOpen.fold(visitor));
  }
}
