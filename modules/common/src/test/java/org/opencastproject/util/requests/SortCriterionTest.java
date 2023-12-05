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

package org.opencastproject.util.requests;

import static org.junit.Assert.assertTrue;

import org.opencastproject.util.requests.SortCriterion.Order;

import org.junit.Assert;
import org.junit.Test;

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
    Assert.assertEquals("name", orderByNameAsc.getFieldName());
    Assert.assertEquals("date", orderByDateAsc.getFieldName());
  }

  /** Test method for {@link SortCriterion#getOrder()} */
  @Test
  public void testGetOrder() {
    Assert.assertEquals(Order.Ascending, orderByNameAsc.getOrder());
    Assert.assertEquals(Order.Descending, orderByNameDesc.getOrder());
  }

  /** Test method for {@link SortCriterion#parse(String)} */
  @Test
  public void testParse() {
    Assert.assertEquals(orderByNameAsc, SortCriterion.parse("name:ASC"));
    Assert.assertEquals(orderByDateDesc, SortCriterion.parse("date:DESC"));
  }

  /** Test method for {@link SortCriterion#parse(String)} */
  @Test(expected = IllegalArgumentException.class)
  public void testParseWithTooManyParts() throws Exception {
    SortCriterion.parse("name:ASC:first");
  }

  /** Test method for {@link SortCriterion#parse(String)} */
  @Test(expected = IllegalArgumentException.class)
  public void testParseWithOnlyOnePart() throws Exception {
    SortCriterion.parse("name:");
  }

  /** Test method for {@link SortCriterion#parse(String)} */
  @Test(expected = IllegalArgumentException.class)
  public void testParseWithInvalidDirection() throws Exception {
    SortCriterion.parse("name:ASCDESC");
  }

  /** Test method for {@link SortCriterion#equals(Object)} */
  @Test
  public void testEquals() {
    assertTrue(orderByNameAsc.equals(new SortCriterion("name", Order.Ascending)));
  }

  /** Test method for {@link SortCriterion#hashCode()} */
  @Test
  public void testHashCode() {
    Assert.assertEquals(orderByNameAsc.hashCode(), new SortCriterion("name", Order.Ascending).hashCode());
  }

  /** Test method for {@link SortCriterion#toString()} */
  @Test
  public void testToString() {
    Assert.assertEquals("name:ASC", orderByNameAsc.toString());
    Assert.assertEquals("date:DESC", orderByDateDesc.toString());
  }

}
