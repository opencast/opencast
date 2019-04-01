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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.metadata.dublincore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.util.IoSupport
import org.opencastproject.util.XmlNamespaceBinding
import org.opencastproject.util.XmlNamespaceContext
import org.opencastproject.util.Xpath

import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Node

import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory

class DublinCoreXmlFormatTest {
    @Test
    @Throws(Exception::class)
    fun readFromFile() {
        assertEquals(
                "Land and Vegetation: Key players on the Climate Scene",
                DublinCoreXmlFormat
                        .read(IoSupport.classPathResourceAsFile("/dublincore.xml").get())
                        .getFirst(DublinCore.PROPERTY_TITLE))
    }

    @Test
    @Throws(Exception::class)
    fun readFromStringWithXmlHeader() {
        val xml = ("<?xml version=\"1.0\"?>\n"
                + "<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\"\n"
                + "            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "            xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
                + "  <dcterms:title>Land and Vegetation: Key players on the Climate Scene</dcterms:title>\n"
                + "  <dcterms:subject>climate, land, vegetation</dcterms:subject>\n"
                + "</dublincore>")
        assertEquals(
                "Land and Vegetation: Key players on the Climate Scene",
                DublinCoreXmlFormat.read(xml).getFirst(DublinCore.PROPERTY_TITLE))
    }

    @Test
    @Throws(Exception::class)
    fun readFromStringWithoutXmlHeader() {
        val xml = ("<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\"\n"
                + "            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "            xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
                + "  <dcterms:title>Land and Vegetation: Key players on the Climate Scene</dcterms:title>\n"
                + "  <dcterms:subject>climate, land, vegetation</dcterms:subject>\n"
                + "</dublincore>")
        assertEquals(
                "Land and Vegetation: Key players on the Climate Scene",
                DublinCoreXmlFormat.read(xml).getFirst(DublinCore.PROPERTY_TITLE))
    }

    @Test
    @Throws(Exception::class)
    fun readFromNode() {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val doc = dbf.newDocumentBuilder().parse(
                IoSupport.classPathResourceAsFile("/matterhorn-inlined-list-records-response.xml").get())
        val ctx = XmlNamespaceContext.mk(
                XmlNamespaceBinding.mk("inlined", "http://www.opencastproject.org/oai/matterhorn-inlined"),
                XmlNamespaceBinding.mk("mp", "http://mediapackage.opencastproject.org"),
                XmlNamespaceBinding.mk("dc", DublinCores.OC_DC_CATALOG_NS_URI))
        // extract media package
        val mpNode = Xpath.mk(doc, ctx).node("//inlined:inlined/mp:mediapackage").get()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mpNode)
        assertNotNull(mp)
        assertEquals("10.0000/5820", mp.identifier.toString())
        // extract episode DublinCore
        val dcNode = Xpath.mk(doc, ctx).node("//inlined:inlined/inlined:episode-dc/dc:dublincore").get()
        val dc = DublinCoreXmlFormat.read(dcNode)
        assertNotNull(dc)
        assertEquals("10.0000/5820", DublinCores.mkOpencastEpisode(dc).dcIdentifier.get())
    }

    @Test
    @Throws(Exception::class)
    fun readWithEmptyAndMergeTest() {
        val emtpy = ""
        val delimiter = "|"
        val irishTitle = "Talamh agus Fásra: Príomh-imreoirí ar an Radharc Aeráide"
        val irishLang = "ga"
        val germanLang = "de"
        val fooId = "197011"
        val eNamepropertyFooId = EName("http://foo.org/metadata", "id")
        val marshalEmpty = true

        val catalogWithEmpty = DublinCoreXmlFormat
                .read(IoSupport.classPathResourceAsFile("/dublincore.xml").get(), marshalEmpty)
        val catalogNoEmpty = DublinCoreXmlFormat
                .read(IoSupport.classPathResourceAsFile("/dublincore.xml").get(), !marshalEmpty)

        // Loading with empty and not empty
        assertEquals(emtpy, catalogWithEmpty.getFirst(DublinCore.PROPERTY_SPATIAL))
        assertNull(catalogNoEmpty.getFirst(DublinCore.PROPERTY_SPATIAL))

        val mergedEmptyNoEmpty = DublinCoreXmlFormat.merge(catalogWithEmpty, catalogNoEmpty)

        // Extra empty values have no effect on merge, original is not affected
        assertEquals(catalogNoEmpty, mergedEmptyNoEmpty)
        assertEquals(irishTitle, catalogNoEmpty.getAsText(DublinCore.PROPERTY_TITLE, irishLang, delimiter))
        assertNull(catalogNoEmpty.getFirst(DublinCore.PROPERTY_SPATIAL))

        val catalogToMerge = DublinCoreXmlFormat
                .read(IoSupport.classPathResourceAsFile("/dublincore-extended.xml").get(), marshalEmpty)

        assertEquals("lab2", catalogToMerge.getFirst(DublinCore.PROPERTY_SPATIAL))

        val mergedToMergeNoEmpty = DublinCoreXmlFormat.merge(catalogToMerge, catalogNoEmpty)

        // Merging catalog with different values removes empty values and adds or updates non-empty values
        assertEquals("lab2", mergedToMergeNoEmpty.getFirst(DublinCore.PROPERTY_SPATIAL))
        assertNull(mergedToMergeNoEmpty.getAsText(DublinCore.PROPERTY_TITLE, irishLang, delimiter))
        assertNotNull(mergedToMergeNoEmpty.getAsText(DublinCore.PROPERTY_TITLE, germanLang, delimiter))
        assertTrue("Property foo:id should be in the list of known properties.",
                mergedToMergeNoEmpty.properties.contains(eNamepropertyFooId))
        assertEquals(fooId, mergedToMergeNoEmpty.getFirstVal(eNamepropertyFooId)!!.value)
    }
}
