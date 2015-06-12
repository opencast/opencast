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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.util.IoSupport;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.FnX;
import com.entwinemedia.fn.Unit;
import com.entwinemedia.fn.data.Opt;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class DublinCoreCatalogTest {
  private static final EName PROPERTY_FOO_ID = new EName("http://foo.org/metadata", "id");

  @Test
  public void testLoadFromFile() throws Exception {
    final DublinCoreCatalog dc = load(IoSupport.classPathResourceAsFile("/dublincore-extended.xml").get());
    assertEquals(asList("2007-12-05"), dc.get(DublinCore.PROPERTY_MODIFIED, DublinCore.LANGUAGE_UNDEFINED));
    assertEquals(Opt.<EName>none(), dc.get(DublinCore.PROPERTY_TYPE).get(0).getEncodingScheme());
    assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE).size());
    assertEquals(1, dc.get(DublinCore.PROPERTY_TITLE, DublinCore.LANGUAGE_UNDEFINED).size());
    assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE, DublinCore.LANGUAGE_ANY).size());
    assertEquals(1, dc.get(DublinCore.PROPERTY_TITLE, "de").size());
    assertEquals(
            asList("Harald Juhnke", "Loriot"),
            dc.get(DublinCore.PROPERTY_CONTRIBUTOR, DublinCore.LANGUAGE_UNDEFINED));
    assertEquals(
            "The modified property should be of type W3CDTF.",
            Opt.some(DublinCore.ENC_SCHEME_W3CDTF), dc.get(DublinCore.PROPERTY_MODIFIED).get(0).getEncodingScheme());
    assertEquals(1, dc.get(PROPERTY_FOO_ID).size());
    assertEquals(Opt.<EName>none(), dc.get(PROPERTY_FOO_ID).get(0).getEncodingScheme());
    assertTrue(
            "Property foo:id should be in the list of known properties.",
            dc.getProperties().contains(PROPERTY_FOO_ID));
  }

  @Test
  public void testLoadNonOpencastDublinCore() throws Exception {
    final DublinCoreCatalog dc = load(IoSupport.classPathResourceAsFile("/dublincore-non-oc.xml").get());
    assertEquals(9, dc.getValuesFlat().size());
    assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE).size());
    assertEquals(
            Opt.some(EName.mk("http://lib.org/metadata-enc", "PlainTitle")),
            dc.get(DublinCore.PROPERTY_TITLE).get(0).getEncodingScheme());
  }

  @Test
  public void testLoadAndSave() throws Exception {
    final DublinCoreCatalog dc = load(IoSupport.classPathResourceAsFile("/dublincore-extended.xml").get());
    final File out = File.createTempFile("dublincore", "xml");
    IoSupport.withResource(new FileOutputStream(out), new FnX<FileOutputStream, Unit>() {
      @Override public Unit apx(FileOutputStream out) throws Exception {
        dc.toXml(out, false);
        return Unit.unit;
      }
    });
    final DublinCoreCatalog reloaded = load(out);
    assertEquals(
            "The reloaded catalog should have the same amount of properties than the original one.",
            dc.getValues().size(), reloaded.getValues().size());
  }

  @Test
  public void testLoadDublinCoreNoDefaultNs() throws Exception {
    final DublinCoreCatalog dc = load(IoSupport.classPathResourceAsFile("/dublincore-no-default-ns.xml").get());
    assertEquals(
            "The catalog should contain 4 properties because empty property are not considered.",
            4, dc.getValuesFlat().size());
    assertEquals("Cutting Test 1", dc.getFirst(DublinCore.PROPERTY_TITLE));
  }

  private DublinCoreCatalog load(File catalog) throws Exception {
    return IoSupport.withResource(
            new FileInputStream(catalog),
            new Fn<InputStream, DublinCoreCatalog>() {
              @Override public DublinCoreCatalog ap(InputStream in) {
                return DublinCores.read(in);
              }
            });
  }
}
