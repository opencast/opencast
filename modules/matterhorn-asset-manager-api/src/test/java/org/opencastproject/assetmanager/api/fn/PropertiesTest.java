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
package org.opencastproject.assetmanager.api.fn;

import static com.entwinemedia.fn.Stream.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.assetmanager.api.fn.Properties.mkProperty;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Value;

import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class PropertiesTest {
  static final Property pb1 = mkProperty("mp-1", "org.opencastproject.approval", "approved", Value.mk(false));
  static final Property ps1 = mkProperty("mp-1", "org.opencastproject.approval", "comment", Value.mk("Bad audio"));
  static final Property pd1 = mkProperty("mp-2", "org.opencastproject.approval", "date", Value.mk(new Date(0)));
  static final Property ps2 = mkProperty("mp-3", "org.opencastproject.comment", "comment", Value.mk("Hello world"));
  static final Property pl1 = mkProperty("mp-3", "org.opencastproject.comment", "count", Value.mk(1L));

  static final Stream<Property> ps = $(pb1, ps1, pd1, ps2, pl1);

  @Test
  @Parameters({
          "unknown.namespace | 0",
          "org.opencastproject.approval | 3",
          "org.opencastproject.comment | 2"})
  public void testByNamespace(String namespace, int expectedCount) throws Exception {
    assertEquals(expectedCount, filterCount(Properties.byNamespace(namespace)));
  }

  @Test
  @Parameters({
          "unknown.name | 0",
          "approved | 1",
          "date | 1",
          "comment | 2"})
  public void testByPropertyName(String propertyName, int expectedCount) throws Exception {
    assertEquals(expectedCount, filterCount(Properties.byPropertyName(propertyName)));
  }

  @Test
  @Parameters({
          "unknown.name | approved | 0",
          "org.opencastproject.approval | approved | 1",
          "org.opencastproject.approval | date | 1",
          "org.opencastproject.approval | comment | 1"})

  public void testByFqnName(String namespace, String propertyName, int expectedCount) throws Exception {
    assertEquals(expectedCount, filterCount(Properties.byFqnName(PropertyName.mk(namespace, propertyName))));
  }

  @Test
  @Parameters({
          "mp-x | 0",
          "mp-1 | 2",
          "mp-2 | 1",
          "mp-3 | 2"})
  public void testByMediaPackageId(String mpId, int expectedCount) throws Exception {
    assertEquals(expectedCount, filterCount(Properties.byMediaPackageId(mpId)));
  }

  @Test
  public void testGetValue() throws Exception {
    assertEquals(
            $(Value.mk(false), Value.mk("Bad audio"), Value.mk(new Date(0)), Value.mk("Hello world"), Value.mk(1L)).toList(),
            ps.map(Properties.getValue).toList());
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetValueWithExpectedTypeTypesDoNotMatch() throws Exception {
    ps.map(Properties.getValue(Value.STRING)).toList();
  }

  @Test
  public void testGetValueWithExpectedType() throws Exception {
    assertEquals(
            $("Bad audio", "Hello world").toList(),
            ps.filter(Properties.byPropertyName("comment")).map(Properties.getValue(Value.STRING)).toList());
  }

  @Test
  public void testGetValueFold() throws Exception {
    assertEquals("Bad audio", ps.apply(Properties.getValue(Value.STRING, "comment")));
    assertEquals(false, ps.apply(Properties.getValue(Value.BOOLEAN, "approved")));
    assertEquals(new Date(0), ps.apply(Properties.getValue(Value.DATE, "date")));
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetValueFoldNotFound() throws Exception {
    ps.apply(Properties.getValue(Value.STRING, "unknown"));
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetValueFoldTypeDoesNotMatch() throws Exception {
    ps.apply(Properties.getValue(Value.BOOLEAN, "comment"));
  }

  @Test
  public void testGetValueFoldOpt() throws Exception {
    assertEquals(Opt.some("Bad audio"), ps.apply(Properties.getValueOpt(Value.STRING, "comment")));
    assertEquals(Opt.some(false), ps.apply(Properties.getValueOpt(Value.BOOLEAN, "approved")));
    assertEquals(Opt.some(new Date(0)), ps.apply(Properties.getValueOpt(Value.DATE, "date")));
  }

  @Test
  public void testGetValueFoldOptNotFound() throws Exception {
    assertEquals(Opt.none(), ps.apply(Properties.getValueOpt(Value.STRING, "unknown")));
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetValueFoldOptTypeDoesNotMatch() throws Exception {
    assertEquals(Opt.none(), ps.apply(Properties.getValueOpt(Value.BOOLEAN, "comment")));
  }

  @Test
  public void testGetStrings() throws Exception {
    assertEquals($("Bad audio", "Hello world").toList(), ps.apply(Properties.getStrings("comment")).toList());
    assertEquals($("Hello world").toList(), ps.apply(Properties.getStrings(PropertyName.mk("org.opencastproject.comment", "comment"))).toList());
  }

  @Test
  public void testGetStringsNotFound() throws Exception {
    assertTrue(ps.apply(Properties.getStrings("unknown")).isEmpty());
    assertTrue(ps.apply(Properties.getStrings(PropertyName.mk("org.opencastproject.approval", "unknown"))).isEmpty());
  }

  @Test
  public void testGetBoolean() throws Exception {
    assertEquals(false, ps.apply(Properties.getBoolean("approved")));
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetBooleanNotFound() throws Exception {
    ps.apply(Properties.getBoolean("unknown"));
  }

  @Test
  public void testGetString() throws Exception {
    assertEquals("Bad audio", ps.apply(Properties.getString("comment")));
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetStringNotFound() throws Exception {
    ps.apply(Properties.getString("unknown"));
  }

  @Test
  public void testGetDate() throws Exception {
    assertEquals(new Date(0), ps.apply(Properties.getDate("date")));
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetDateNotFound() throws Exception {
    ps.apply(Properties.getDate("unknown"));
  }

  @Test
  public void testGetLong() throws Exception {
    assertEquals((Long) 1L, ps.apply(Properties.getLong("count")));
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testGetNotFoundLong() throws Exception {
    ps.apply(Properties.getLong("unknown"));
  }

  @Test
  public void testGetStringOpt() throws Exception {
    assertEquals(Opt.some("Bad audio"), ps.apply(Properties.getStringOpt("comment")));
    assertEquals(Opt.none(), ps.apply(Properties.getStringOpt("unknown")));
  }

  @Test
  public void testGetDateOpt() throws Exception {
    assertEquals(Opt.some(new Date(0)), ps.apply(Properties.getDateOpt("date")));
    assertEquals(Opt.none(), ps.apply(Properties.getDateOpt("unknown")));
  }

  @Test
  public void testGetLongOpt() throws Exception {
    assertEquals(Opt.some(1L), ps.apply(Properties.getLongOpt("count")));
    assertEquals(Opt.none(), ps.apply(Properties.getLongOpt("unknown")));
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  private int filterCount(Pred<Property> p) {
    return ps.filter(p).toList().size();
  }
}
