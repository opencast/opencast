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

import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.data.Collections.toList;
import static org.opencastproject.util.data.Iterators.intRangeE;

public class IteratorsTest {
  @Test
  public void testIntRangeE1() {
    assertFalse(intRangeE(0, 0).hasNext());
    assertTrue(intRangeE(0, 1).hasNext());
    assertEquals(10, toList(intRangeE(0, 10)).size());
  }

  @Test(expected = NoSuchElementException.class)
  public void testIntRangeE2() {
    intRangeE(0, 0).next();
  }
}
