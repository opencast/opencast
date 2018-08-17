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
package org.opencastproject.mediapackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.mediapackage.XMLCatalogImpl.CatalogEntry;

import org.junit.Test;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class XMLCatalogImplTest {
  /**
   * Two catalog entries shall be considered equal if
   * the have the same ename, the same value and the same <em>set</em> of attributes.
   */
  @Test
  public void testEqualityOfCatalogEntries() throws Exception {
    final Map<EName, String> a1 = map(
        tuple(EName.mk("http://lang.org", "lang"), "en"),
        tuple(EName.mk("http://value.org", "value"), "value"),
        tuple(EName.mk("http://type.org", "type"), "string"));
    final Map<EName, String> a2 = map(
        tuple(EName.mk("http://type.org", "type"), "string"),
        tuple(EName.mk("http://lang.org", "lang"), "en"),
        tuple(EName.mk("http://value.org", "value"), "value"));
    final CatalogEntry c1 = new TestImpl().mkCatalogEntry(EName.mk("http://extron.com", "extron"), "value", a1);
    final CatalogEntry c2 = new TestImpl().mkCatalogEntry(EName.mk("http://extron.com", "extron"), "value", a2);
    assertEquals(c1, c2);
    final CatalogEntry c3 = new TestImpl().mkCatalogEntry(EName.mk("http://extron.com", "extron2"), "value", a2);
    assertNotEquals(c1, c3);
  }

  private static final class TestImpl extends XMLCatalogImpl {
    @Override public Document toXml() throws ParserConfigurationException, TransformerException, IOException {
      return null;
    }

    @Override public String toJson() throws IOException {
      return null;
    }
  }
}
