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
package org.opencastproject.util;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import java.util.Hashtable;
import java.util.Map;

public class OsgiUtilTest {

  @Test
  public void testFilterDictionary() throws Exception {
    final Hashtable<String, String> h = new Hashtable<>();
    h.put("w.p.key1", "1");
    h.put("x", "2");
    h.put("w.p.key2", "2");
    h.put("y", "2");
    final Map<String, String> f = OsgiUtil.filterByPrefix(h, "w.p.");
    assertEquals(2, f.size());
    assertEquals("1", f.get("key1"));
    assertEquals("2", f.get("key2"));
  }

  @Test
  public void testGetConfigAsInt() throws Exception {
    final Hashtable<String, String> h = new Hashtable<>();
    h.put("a", "");
    h.put("b", "2");
    h.put("c", "d");

    try {
      OsgiUtil.getCfgAsInt(h, "a");
      Assert.fail();
    } catch (org.osgi.service.cm.ConfigurationException e) {
      Assert.assertNotNull(e);
    }

    assertEquals(2, OsgiUtil.getCfgAsInt(h, "b"));

    try {
      OsgiUtil.getCfgAsInt(h, "c");
      Assert.fail();
    } catch (org.osgi.service.cm.ConfigurationException e) {
      Assert.assertNotNull(e);
    }
  }

}
