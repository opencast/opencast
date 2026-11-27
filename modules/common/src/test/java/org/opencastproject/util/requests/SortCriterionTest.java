/*
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

package org.opencastproject.util.requests;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.opencastproject.util.requests.SortCriterion.Order;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link SortCriterion}
 */
public class SortCriterionTest {

  private static final SortCriterion orderByNameAsc = new SortCriterion("name", Order.Ascending);
  private static final SortCriterion orderByNameDesc = new SortCriterion("name", Order.Descending);
  private static final SortCriterion orderByDateAsc = new SortCriterion("date", Order.Ascending);
  private static final SortCriterion orderByDateDesc = new SortCriterion("date", Order.Descending);

  /** Test method for {@link SortCriterion#getFieldName()} */
  @Test
  public void testGetFieldName() {
    Assertions.assertEquals("name", orderByNameAsc.getFieldName());
    Assertions.assertEquals("date", orderByDateAsc.getFieldName());
  }

  /** Test method for {@link SortCriterion#getOrder()} */
  @Test
  public void testGetOrder() {
    Assertions.assertEquals(Order.Ascending, orderByNameAsc.getOrder());
    Assertions.assertEquals(Order.Descending, orderByNameDesc.getOrder());
  }

  /** Test method for {@link SortCriterion#parse(String)} */
  @Test
  public void testParse() {
    Assertions.assertEquals(orderByNameAsc, SortCriterion.parse("name:ASC"));
    Assertions.assertEquals(orderByDateDesc, SortCriterion.parse("date:DESC"));
  }

  /** Test method for {@link SortCriterion#parse(String)} */
  @Test
  public void testParseWithTooManyParts() throws Exception {
    assertThrows(IllegalArgumentException.class, () ->
        SortCriterion.parse("name:ASC:first"));
  }

  /** Test method for {@link SortCriterion#parse(String)} */
  @Test
  public void testParseWithOnlyOnePart() throws Exception {
    assertThrows(IllegalArgumentException.class, () ->
        SortCriterion.parse("name:"));
  }

  /** Test method for {@link SortCriterion#parse(String)} */
  @Test
  public void testParseWithInvalidDirection() throws Exception {
    assertThrows(IllegalArgumentException.class, () ->
        SortCriterion.parse("name:ASCDESC"));
  }

  /** Test method for {@link SortCriterion#equals(Object)} */
  @Test
  public void testEquals() {
    assertTrue(orderByNameAsc.equals(new SortCriterion("name", Order.Ascending)));
  }

  /** Test method for {@link SortCriterion#hashCode()} */
  @Test
  public void testHashCode() {
    Assertions.assertEquals(orderByNameAsc.hashCode(), new SortCriterion("name", Order.Ascending).hashCode());
  }

  /** Test method for {@link SortCriterion#toString()} */
  @Test
  public void testToString() {
    Assertions.assertEquals("name:ASC", orderByNameAsc.toString());
    Assertions.assertEquals("date:DESC", orderByDateDesc.toString());
  }

}
