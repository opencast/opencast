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

import com.entwinemedia.fn.Stream.`$`
import java.util.Arrays.asList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.opencastproject.util.data.Collections.list
import org.opencastproject.util.data.Collections.map
import org.opencastproject.util.data.Tuple.tuple

import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.XMLCatalogImpl.CatalogEntry
import org.opencastproject.util.IoSupport

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.FnX
import com.entwinemedia.fn.Unit
import com.entwinemedia.fn.data.Opt

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CyclicBarrier

import javax.xml.XMLConstants

class DublinCoreCatalogTest {

    @Rule
    var testFolder = TemporaryFolder()

    @Test
    @Throws(Exception::class)
    fun testLoadFromFile() {
        val dc = read("/dublincore-extended.xml")
        assertEquals(asList("2007-12-05"), dc.get(DublinCore.PROPERTY_MODIFIED, DublinCore.LANGUAGE_UNDEFINED))
        assertEquals(Opt.none<EName>(), dc.get(DublinCore.PROPERTY_TYPE)[0].encodingScheme)
        assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE).size.toLong())
        assertEquals(1, dc.get(DublinCore.PROPERTY_TITLE, DublinCore.LANGUAGE_UNDEFINED).size.toLong())
        assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE, DublinCore.LANGUAGE_ANY).size.toLong())
        assertEquals(1, dc.get(DublinCore.PROPERTY_TITLE, "de").size.toLong())
        assertEquals(asList("Loriot", "Harald Juhnke"),
                dc.get(DublinCore.PROPERTY_CONTRIBUTOR, DublinCore.LANGUAGE_UNDEFINED))
        assertEquals(
                "The modified property should be of type W3CDTF.",
                Opt.some(DublinCore.ENC_SCHEME_W3CDTF), dc.get(DublinCore.PROPERTY_MODIFIED)[0].encodingScheme)
        assertEquals(1, dc.get(PROPERTY_FOO_ID).size.toLong())
        assertEquals(Opt.none<EName>(), dc.get(PROPERTY_FOO_ID)[0].encodingScheme)
        assertTrue(
                "Property foo:id should be in the list of known properties.",
                dc.properties.contains(PROPERTY_FOO_ID))
        assertNull("The DublinCore reader cannot detect the flavor", dc.flavor)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadNonOpencastDublinCore() {
        val dc = read("/dublincore-non-oc.xml")
        for (value in dc.valuesFlat) {
            logger.debug("DublinCorevalue " + value.value)
        }
        assertEquals(9, dc.valuesFlat.size.toLong())
        assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE).size.toLong())
        assertEquals(
                Opt.some(EName.mk("http://lib.org/metadata-enc", "PlainTitle")),
                dc.get(DublinCore.PROPERTY_TITLE)[0].encodingScheme)
        for (entry in dc.entriesSorted) {
            logger.debug(entry.eName.toString() + " " + entry.value)
        }

        assertTrue("Property foo:id should be in the list of known properties.",
                dc.properties.contains(PROPERTY_FOO_ID))
        assertEquals(fooId, dc.getFirstVal(PROPERTY_FOO_ID)!!.value)
    }

    @Test
    @Throws(Exception::class)
            /**
             * MH-11621 test optional marshal input XML with intentionally emptied fields
             * @throws Exception
             */
    fun testLoadEmptiedElementsDublinCore() {
        val includeEmptiedFields = true
        val dc = read("/dublincore-non-oc.xml", includeEmptiedFields)
        for (value in dc.valuesFlat) {
            logger.debug("DublinCorevalue " + value.value)
        }
        // additional empty field is part of the count
        assertEquals(10, dc.valuesFlat.size.toLong())
        assertEquals(2, dc.get(DublinCore.PROPERTY_TITLE).size.toLong())
        assertEquals(
                Opt.some(EName.mk("http://lib.org/metadata-enc", "PlainTitle")),
                dc.get(DublinCore.PROPERTY_TITLE)[0].encodingScheme)
        for (entry in dc.entriesSorted) {
            logger.debug(entry.eName.toString() + " " + entry.value)
        }

        assertTrue("Property foo:id should be in the list of known properties.",
                dc.properties.contains(PROPERTY_FOO_ID))
        assertEquals(fooId, dc.getFirstVal(PROPERTY_FOO_ID)!!.value)
        //  verify that catalog only instantiates properties from catalog
        // and that it instantiates properties with passed in empty values.
        // Passing empty values is how existing values are deleted during metadata update
        assertFalse("Non-passed property dc:temporal should *not* be instantiated.",
                dc.properties.contains(DublinCore.PROPERTY_TEMPORAL))
        assertTrue("Property foo:empty should be in the list of known properties.",
                dc.properties.contains(PROPERTY_FOO_EMPTY))
        assertEquals("", dc.getFirstVal(PROPERTY_FOO_EMPTY)!!.value)
    }

    @Test
    @Throws(Exception::class)
            /**
             * MH-11621 test emptied fields after merge
             * @throws Exception
             */
    fun testMergeEmptiedElementsDublinCore() {
        val dcOrig = read("/dublincore-non-oc.xml")
        val isMarshalEmptyFields = true
        val dc2 = read("/dublincore-non-oc2.xml", isMarshalEmptyFields)
        for (value in dcOrig.valuesFlat) {
            logger.debug("DublinCorevalue 1 " + value.value)
        }
        for (value in dc2.valuesFlat) {
            logger.debug("DublinCorevalue 2 " + value.value)
        }
        assertEquals("Original value count", 9, dcOrig.valuesFlat.size.toLong())
        assertEquals("Merge catalog value count", 6, dc2.valuesFlat.size.toLong())

        val dc3 = DublinCoreXmlFormat.merge(dc2, dcOrig)
        assertEquals("Merge count (removed 2 values added 1 value)", 8, dc3.valuesFlat.size.toLong())
        assertEquals("Only one title now exists", 1, dc3.get(DublinCore.PROPERTY_TITLE).size.toLong())
        assertTrue("A changed Modify value", "2018-01-05" == dc3.get(DublinCore.PROPERTY_MODIFIED)[0].value)
        assertEquals("Not Empty!", dc3.getFirstVal(PROPERTY_FOO_EMPTY)!!.value)
        assertEquals("German title was removed", 0, dc3.get(DublinCore.PROPERTY_TITLE, "de").size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testLoadAndSave() {
        val dc = read("/dublincore-extended.xml")
        val out = testFolder.newFile("dublincore.xml")
        IoSupport.withResource(FileOutputStream(out), object : FnX<FileOutputStream, Unit>() {
            @Throws(Exception::class)
            override fun applyX(out: FileOutputStream): Unit {
                dc.toXml(out, false)
                return Unit.unit
            }
        })
        val reloaded = DublinCoreXmlFormat.read(out)
        assertEquals(
                "The reloaded catalog should have the same amount of properties than the original one.",
                dc.values.size.toLong(), reloaded.values.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testLoadDublinCoreNoDefaultNs() {
        val dc = read("/dublincore-no-default-ns.xml")
        assertEquals(
                "The catalog should contain 4 properties because empty property are not considered.",
                4, dc.valuesFlat.size.toLong())
        assertEquals("Cutting Test 1", dc.getFirst(DublinCore.PROPERTY_TITLE))
    }

    @Test
    @Throws(Exception::class)
    fun testSortingOfCatalogEntries() {
        val dc1 = read("/sorting/dublincore1-1.xml")
        val dc2 = read("/sorting/dublincore1-2.xml")
        assertEquals(dc1.entriesSorted, dc2.entriesSorted)
        // make sure attributes are sorted in the correct order
        val attributes = `$`(dc1.entriesSorted)
                .map(object : Fn<CatalogEntry, Map<EName, String>>() {
                    override fun apply(entry: CatalogEntry): Map<EName, String> {
                        return entry.getAttributes()
                    }
                })
                .toList()
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
                map(tuple(EName.mk(XMLConstants.XML_NS_URI, "lang"), "de"))))
        assertEquals(dc1.toXmlString(), dc2.toXmlString())
        //
        assertEquals(
                read("/sorting/dublincore2-1.xml").toXmlString().trim { it <= ' ' },
                IoSupport.loadTxtFromClassPath("/sorting/dublincore2-2.xml", this.javaClass).get().trim { it <= ' ' })
    }

    @Test
    @Throws(Exception::class)
            /**
             * MH-11621 verify that there are no state issues with static read methods
             */
    fun testMultiThreadedCatalogRead() {
        val map = ConcurrentHashMap()
        val list = ArrayList()
        val threadList = ArrayList()
        val count = 100
        for (i in 0 until count) {
            val cat = read("/dublincore.xml")
            cat.set(DublinCore.PROPERTY_TITLE, "title-$i")
            list.add(cat)
        }
        val cat = read("/dublincore.xml")
        val gate = CyclicBarrier(count + 1)
        for (i in 0 until count) {
            cat.set(DublinCore.PROPERTY_TITLE, "title-$i")
            val catString = cat.toXmlString()
            threadList.add(Thread(Runnable {
                try {
                    // block until all threads are started
                    gate.await()
                    logger.debug("Running with " + i + " in " + Thread.currentThread().id)
                    val cat = DublinCoreXmlFormat.read(catString)
                    map.put(Integer.valueOf(i), cat)
                } catch (e: Exception) {
                    org.junit.Assert.fail("Should not have failed reading the catalog!")
                }
            }))
        }
        for (t in threadList) {
            t.start()
        }
        // Last await is launched to unblock all threads
        gate.await()
        var currentCount = 0
        while (currentCount < count) {
            if (map.get(currentCount) != null) {
                logger.debug("title-" + currentCount + ": " + map.get(currentCount).getFirst(DublinCore.PROPERTY_TITLE))
                assertEquals("title-$currentCount", map.get(currentCount).getFirst(DublinCore.PROPERTY_TITLE))
                currentCount++
            }
        }
    }

    /** Read from the classpath.  */
    @Throws(Exception::class)
    private fun read(dcFile: String): DublinCoreCatalog {
        return DublinCoreXmlFormat.read(IoSupport.classPathResourceAsFile(dcFile).get())
    }

    /**
     * MH-11621
     * Optional reads that allow instantiating with intentionally passed empty elements
     * used to remove existing DublinCore catalog values during a merge update.
     * Read from the classpath.
     */
    @Throws(Exception::class)
    private fun read(dcFile: String, includeEmptiedElements: Boolean): DublinCoreCatalog {
        return DublinCoreXmlFormat.read(IoSupport.classPathResourceAsFile(dcFile).get(), includeEmptiedElements)
    }

    companion object {
        private val fooId = "197011"
        private val PROPERTY_FOO_ID = EName("http://foo.org/metadata", "id")
        private val PROPERTY_FOO_EMPTY = EName("http://foo.org/metadata", "empty")
        private val logger = LoggerFactory.getLogger(DublinCoreCatalogTest::class.java)
    }
}

