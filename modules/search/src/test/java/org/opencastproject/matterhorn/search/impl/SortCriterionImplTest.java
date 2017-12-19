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

package org.opencastproject.matterhorn.search.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.matterhorn.search.SearchQuery.Order;
import org.opencastproject.matterhorn.search.SortCriterion;

import org.junit.Test;

/**
 * Test cases for {@link SortCriterionImpl}
 */
public class SortCriterionImplTest {

  private static final SortCriterion orderByNameAsc = new SortCriterionImpl("name", Order.Ascending);
  private static final SortCriterion orderByNameDesc = new SortCriterionImpl("name", Order.Descending);
  private static final SortCriterion orderByDateAsc = new SortCriterionImpl("date", Order.Ascending);
  private static final SortCriterion orderByDateDesc = new SortCriterionImpl("date", Order.Descending);

  /** Test method for {@link SortCriterionImpl#getFieldName()} */
  @Test
  public void testGetFieldName() {
    assertEquals("name", orderByNameAsc.getFieldName());
    assertEquals("date", orderByDateAsc.getFieldName());
  }

  /** Test method for {@link SortCriterionImpl#getOrder()} */
  @Test
  public void testGetOrder() {
    assertEquals(Order.Ascending, orderByNameAsc.getOrder());
    assertEquals(Order.Descending, orderByNameDesc.getOrder());
  }

  /** Test method for {@link SortCriterionImpl#parse(String)} */
  @Test
  public void testParse() {
    assertEquals(orderByNameAsc, SortCriterionImpl.parse("name:ASC"));
    assertEquals(orderByDateDesc, SortCriterionImpl.parse("date:DESC"));
  }

  /** Test method for {@link SortCriterionImpl#parse(String)} */
  @Test(expected = IllegalArgumentException.class)
  public void testParseWithTooManyParts() throws Exception {
    SortCriterionImpl.parse("name:ASC:first");
  }

  /** Test method for {@link SortCriterionImpl#parse(String)} */
  @Test(expected = IllegalArgumentException.class)
  public void testParseWithOnlyOnePart() throws Exception {
    SortCriterionImpl.parse("name:");
  }

  /** Test method for {@link SortCriterionImpl#parse(String)} */
  @Test(expected = IllegalArgumentException.class)
  public void testParseWithInvalidDirection() throws Exception {
    SortCriterionImpl.parse("name:ASCDESC");
  }

  /** Test method for {@link SortCriterionImpl#equals(Object)} */
  @Test
  public void testEquals() {
    assertTrue(orderByNameAsc.equals(new SortCriterionImpl("name", Order.Ascending)));
  }

  /** Test method for {@link SortCriterionImpl#hashCode()} */
  @Test
  public void testHashCode() {
    assertEquals(orderByNameAsc.hashCode(), new SortCriterionImpl("name", Order.Ascending).hashCode());
  }

  /** Test method for {@link SortCriterionImpl#toString()} */
  @Test
  public void testToString() {
    assertEquals("name:ASC", orderByNameAsc.toString());
    assertEquals("date:DESC", orderByDateDesc.toString());
  }

}
