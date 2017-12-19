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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.XmlNamespaceBinding;
import org.opencastproject.util.XmlNamespaceContext;
import org.opencastproject.util.Xpath;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;

public class DublinCoreXmlFormatTest {
  @Test
  public void readFromFile() throws Exception {
    assertEquals(
        "Land and Vegetation: Key players on the Climate Scene",
        DublinCoreXmlFormat
            .read(IoSupport.classPathResourceAsFile("/dublincore.xml").get())
            .getFirst(DublinCore.PROPERTY_TITLE));
  }

  @Test
  public void readFromStringWithXmlHeader() throws Exception {
    final String xml =
        "<?xml version=\"1.0\"?>\n"
            + "<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\"\n"
            + "            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "            xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "  <dcterms:title>Land and Vegetation: Key players on the Climate Scene</dcterms:title>\n"
            + "  <dcterms:subject>climate, land, vegetation</dcterms:subject>\n"
            + "</dublincore>";
    assertEquals(
        "Land and Vegetation: Key players on the Climate Scene",
        DublinCoreXmlFormat.read(xml).getFirst(DublinCore.PROPERTY_TITLE));
  }

  @Test
  public void readFromStringWithoutXmlHeader() throws Exception {
    final String xml =
        "<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\"\n"
            + "            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "            xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
            + "  <dcterms:title>Land and Vegetation: Key players on the Climate Scene</dcterms:title>\n"
            + "  <dcterms:subject>climate, land, vegetation</dcterms:subject>\n"
            + "</dublincore>";
    assertEquals(
        "Land and Vegetation: Key players on the Climate Scene",
        DublinCoreXmlFormat.read(xml).getFirst(DublinCore.PROPERTY_TITLE));
  }

  @Test
  public void readFromNode() throws Exception {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final Document doc = dbf.newDocumentBuilder().parse(
        IoSupport.classPathResourceAsFile("/matterhorn-inlined-list-records-response.xml").get());
    final NamespaceContext ctx = XmlNamespaceContext.mk(
        XmlNamespaceBinding.mk("inlined", "http://www.opencastproject.org/oai/matterhorn-inlined"),
        XmlNamespaceBinding.mk("mp", "http://mediapackage.opencastproject.org"),
        XmlNamespaceBinding.mk("dc", DublinCores.OC_DC_CATALOG_NS_URI));
    // extract media package
    final Node mpNode = Xpath.mk(doc, ctx).node("//inlined:inlined/mp:mediapackage").get();
    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mpNode);
    assertNotNull(mp);
    assertEquals("10.0000/5820", mp.getIdentifier().toString());
    // extract episode DublinCore
    final Node dcNode = Xpath.mk(doc, ctx).node("//inlined:inlined/inlined:episode-dc/dc:dublincore").get();
    final DublinCoreCatalog dc = DublinCoreXmlFormat.read(dcNode);
    assertNotNull(dc);
    assertEquals("10.0000/5820", DublinCores.mkOpencastEpisode(dc).getDcIdentifier().get());
  }
}
