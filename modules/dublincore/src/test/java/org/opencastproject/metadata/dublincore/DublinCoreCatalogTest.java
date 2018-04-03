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

import static com.entwinemedia.fn.Stream.$;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.XMLCatalogImpl.CatalogEntry;
import org.opencastproject.util.IoSupport;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.FnX;
import com.entwinemedia.fn.Unit;
import com.entwinemedia.fn.data.Opt;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CyclicBarrier;

import javax.xml.XMLConstants;

public class DublinCoreCatalogTest {
  private static final String fooId = "197011";
  private static final EName PROPERTY_FOO_ID = new EName("http://foo.org/metadata", "id");
  private static final EName PROPERTY_FOO_EMPTY = new EName("http://foo.org/metadata", "empty");
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreCatalogTest.class);

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void testLoadFromFile() throws Exception {
    final DublinCoreCatalog dc = read("/dublincore-extended.xml");
    assertEquals(asList("2007-12-05"), dc.get(DublinCore.PROPERTY_MODIFIED, DublinCore.LANGUAGE_UNDEFINED));
    assertEquals(Opt.<EName>none(), dc.get(DublinCore.PROPERTY_TYPE).get(0).getEncodingScheme());
    assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE).size());
    assertEquals(1, dc.get(DublinCore.PROPERTY_TITLE, DublinCore.LANGUAGE_UNDEFINED).size());
    assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE, DublinCore.LANGUAGE_ANY).size());
    assertEquals(1, dc.get(DublinCore.PROPERTY_TITLE, "de").size());
    assertEquals(asList("Loriot", "Harald Juhnke"),
            dc.get(DublinCore.PROPERTY_CONTRIBUTOR, DublinCore.LANGUAGE_UNDEFINED));
    assertEquals(
            "The modified property should be of type W3CDTF.",
            Opt.some(DublinCore.ENC_SCHEME_W3CDTF), dc.get(DublinCore.PROPERTY_MODIFIED).get(0).getEncodingScheme());
    assertEquals(1, dc.get(PROPERTY_FOO_ID).size());
    assertEquals(Opt.<EName>none(), dc.get(PROPERTY_FOO_ID).get(0).getEncodingScheme());
    assertTrue(
            "Property foo:id should be in the list of known properties.",
            dc.getProperties().contains(PROPERTY_FOO_ID));
    assertNull("The DublinCore reader cannot detect the flavor", dc.getFlavor());
  }

  @Test
  public void testLoadNonOpencastDublinCore() throws Exception {
    final DublinCoreCatalog dc = read("/dublincore-non-oc.xml");
    for (DublinCoreValue value: dc.getValuesFlat()) {
      logger.debug("DublinCorevalue " + value.getValue());
    }
    assertEquals(9, dc.getValuesFlat().size());
    assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE).size());
    assertEquals(
            Opt.some(EName.mk("http://lib.org/metadata-enc", "PlainTitle")),
            dc.get(DublinCore.PROPERTY_TITLE).get(0).getEncodingScheme());
    for (CatalogEntry entry : dc.getEntriesSorted()) {
      logger.debug(entry.getEName().toString() + " " + entry.getValue());
    }

    assertTrue("Property foo:id should be in the list of known properties.",
            dc.getProperties().contains(PROPERTY_FOO_ID));
    assertEquals(fooId, dc.getFirstVal(PROPERTY_FOO_ID).getValue());
  }

  @Test
  /**
   *  MH-11621 test optional marshal input XML with intentionally emptied fields
   * @throws Exception
   */
  public void testLoadEmptiedElementsDublinCore() throws Exception {
    boolean includeEmptiedFields = true;
    final DublinCoreCatalog dc = read("/dublincore-non-oc.xml", includeEmptiedFields);
    for (DublinCoreValue value: dc.getValuesFlat()) {
      logger.debug("DublinCorevalue " + value.getValue());
    }
    // additional empty field is part of the count
    assertEquals(10, dc.getValuesFlat().size());
    assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE).size());
    assertEquals(
            Opt.some(EName.mk("http://lib.org/metadata-enc", "PlainTitle")),
            dc.get(DublinCore.PROPERTY_TITLE).get(0).getEncodingScheme());
    for (CatalogEntry entry : dc.getEntriesSorted()) {
      logger.debug(entry.getEName().toString() + " " + entry.getValue());
    }

    assertTrue("Property foo:id should be in the list of known properties.",
            dc.getProperties().contains(PROPERTY_FOO_ID));
    assertEquals(fooId, dc.getFirstVal(PROPERTY_FOO_ID).getValue());
    //  verify that catalog only instantiates properties from catalog
    // and that it instantiates properties with passed in empty values.
    // Passing empty values is how existing values are deleted during metadata update
    assertFalse("Non-passed property dc:temporal should *not* be instantiated.",
            dc.getProperties().contains(DublinCore.PROPERTY_TEMPORAL));
    assertTrue("Property foo:empty should be in the list of known properties.",
            dc.getProperties().contains(PROPERTY_FOO_EMPTY));
    assertEquals("", dc.getFirstVal(PROPERTY_FOO_EMPTY).getValue());
  }

  @Test
  /**
   *  MH-11621 test emptied fields after merge
   * @throws Exception
   */
  public void testMergeEmptiedElementsDublinCore() throws Exception {
    final DublinCoreCatalog dcOrig = read("/dublincore-non-oc.xml");
    boolean isMarshalEmptyFields = true;
    final DublinCoreCatalog dc2 = read("/dublincore-non-oc2.xml", isMarshalEmptyFields);
    for (DublinCoreValue value: dcOrig.getValuesFlat()) {
      logger.debug("DublinCorevalue 1 " + value.getValue());
    }
    for (DublinCoreValue value: dc2.getValuesFlat()) {
      logger.debug("DublinCorevalue 2 " + value.getValue());
    }
    assertEquals("Original value count", 9, dcOrig.getValuesFlat().size());
    assertEquals("Merge catalog value count", 6, dc2.getValuesFlat().size());

    DublinCoreCatalog dc3 = DublinCoreXmlFormat.merge(dc2, dcOrig);
    assertEquals("Merge count (removed 2 values added 1 value)", 8, dc3.getValuesFlat().size());
    assertEquals("Only one title now exists", 1, dc3.get(DublinCore.PROPERTY_TITLE).size());
    assertTrue("A changed Modify value", "2018-01-05".equals(dc3.get(DublinCore.PROPERTY_MODIFIED).get(0).getValue()));
    assertEquals("Not Empty!", dc3.getFirstVal(PROPERTY_FOO_EMPTY).getValue());
    assertEquals("German title was removed", 0, dc3.get(DublinCore.PROPERTY_TITLE, "de").size());
  }

  @Test
  public void testLoadAndSave() throws Exception {
    final DublinCoreCatalog dc = read("/dublincore-extended.xml");
    final File out = testFolder.newFile("dublincore.xml");
    IoSupport.withResource(new FileOutputStream(out), new FnX<FileOutputStream, Unit>() {
      @Override public Unit applyX(FileOutputStream out) throws Exception {
        dc.toXml(out, false);
        return Unit.unit;
      }
    });
    final DublinCoreCatalog reloaded = DublinCoreXmlFormat.read(out);
    assertEquals(
            "The reloaded catalog should have the same amount of properties than the original one.",
            dc.getValues().size(), reloaded.getValues().size());
  }

  @Test
  public void testLoadDublinCoreNoDefaultNs() throws Exception {
    final DublinCoreCatalog dc = read("/dublincore-no-default-ns.xml");
    assertEquals(
            "The catalog should contain 4 properties because empty property are not considered.",
            4, dc.getValuesFlat().size());
    assertEquals("Cutting Test 1", dc.getFirst(DublinCore.PROPERTY_TITLE));
  }

  @Test
  public void testSortingOfCatalogEntries() throws Exception {
    final DublinCoreCatalog dc1 = read("/sorting/dublincore1-1.xml");
    final DublinCoreCatalog dc2 = read("/sorting/dublincore1-2.xml");
    assertEquals(dc1.getEntriesSorted(), dc2.getEntriesSorted());
    // make sure attributes are sorted in the correct order
    List<Map<EName, String>> attributes = $(dc1.getEntriesSorted())
        .map(new Fn<CatalogEntry, Map<EName, String>>() {
          @Override public Map<EName, String> apply(CatalogEntry entry) {
            return entry.getAttributes();
          }
        })
        .toList();
    assertEquals("Attribute order", attributes, list(
        map(),
        map(tuple(EName.mk(XMLConstants.XML_NS_URI, "lang"), "de")),
        map(tuple(EName.mk(XMLConstants.XML_NS_URI, "lang"), "de"),
            tuple(EName.mk(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type"), "string")),
        map(tuple(EName.mk(XMLConstants.XML_NS_URI, "lang"), "en"),
            tuple(EName.mk(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type"), "string")),
        map(),
        map(),
        map(),
        map(tuple(EName.mk(XMLConstants.XML_NS_URI, "lang"), "de"))));
    assertEquals(dc1.toXmlString(), dc2.toXmlString());
    //
    assertEquals(
        read("/sorting/dublincore2-1.xml").toXmlString().trim(),
        IoSupport.loadTxtFromClassPath("/sorting/dublincore2-2.xml", this.getClass()).get().trim());
  }

  @Test
  /**
   *  MH-11621 verify that there are no state issues with static read methods
   */
  public void testMultiThreadedCatalogRead() throws Exception {
    ConcurrentMap<Integer, DublinCoreCatalog> map = new ConcurrentHashMap();
    List<DublinCoreCatalog> list = new ArrayList();
    List<Thread> threadList = new ArrayList();
    int count = 100;
    for (int i = 0; i < count; i++) {
      DublinCoreCatalog cat = read("/dublincore.xml");
      cat.set(DublinCore.PROPERTY_TITLE, "title-" + i);
      list.add(cat);
    }
    DublinCoreCatalog cat = read("/dublincore.xml");
    final CyclicBarrier gate = new CyclicBarrier(count + 1);
    for (int i = 0; i < count; i++) {
      final int index = i;
      cat.set(DublinCore.PROPERTY_TITLE, "title-" + index);
      final String catString = cat.toXmlString();
      threadList.add(new Thread(new Runnable() {
        public void run() {
          try {
            // block until all threads are started
            gate.await();
            logger.debug("Running with " + index + " in " + Thread.currentThread().getId());
            DublinCoreCatalog cat = DublinCoreXmlFormat.read(catString);
            map.put(Integer.valueOf(index), cat);
          } catch (Exception e) {
            org.junit.Assert.fail("Should not have failed reading the catalog!");
          }
        }
      }));
    }
    for (Thread t: threadList) {
      t.start();
    }
    // Last await is launched to unblock all threads
    gate.await();
    int currentCount = 0;
    while (currentCount < count) {
      if (map.get(currentCount) != null) {
        logger.debug("title-" + currentCount + ": " +  map.get(currentCount).getFirst(DublinCore.PROPERTY_TITLE));
        assertEquals("title-" + currentCount, map.get(currentCount).getFirst(DublinCore.PROPERTY_TITLE));
        currentCount++;
      }
    }
  }

  /** Read from the classpath. */
  private DublinCoreCatalog read(String dcFile) throws Exception {
    return DublinCoreXmlFormat.read(IoSupport.classPathResourceAsFile(dcFile).get());
  }

  /**
   *  MH-11621
   * Optional reads that allow instantiating with intentionally passed empty elements
   * used to remove existing DublinCore catalog values during a merge update.
   *  Read from the classpath.
   */
  private DublinCoreCatalog read(String dcFile, boolean includeEmptiedElements) throws Exception {
    return DublinCoreXmlFormat.read(IoSupport.classPathResourceAsFile(dcFile).get(), includeEmptiedElements);
  }
}

