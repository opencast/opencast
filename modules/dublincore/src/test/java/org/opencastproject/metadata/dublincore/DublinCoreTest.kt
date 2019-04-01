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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.opencastproject.metadata.dublincore.DublinCore.ENC_SCHEME_URI
import org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_ANY
import org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_UNDEFINED
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CONTRIBUTOR
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATOR
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_FORMAT
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LANGUAGE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LICENSE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_PUBLISHER
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE
import org.opencastproject.metadata.dublincore.DublinCore.TERMS_NS_URI
import org.opencastproject.metadata.dublincore.DublinCores.OC_PROPERTY_PROMOTED
import org.opencastproject.util.UrlSupport.uri

import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageReferenceImpl
import org.opencastproject.mediapackage.NamespaceBindingException
import org.opencastproject.metadata.api.MediaPackageMetadata
import org.opencastproject.util.MimeType
import org.opencastproject.util.UnknownFileTypeException
import org.opencastproject.util.XmlNamespaceContext
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.data.Opt

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.util.Date

import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Test class for the dublin core implementation.
 */
class DublinCoreTest {

    /**
     * The test catalog
     */
    private var catalogFile: File? = null
    private var catalogFile2: File? = null

    /** Temp files for test catalogs  */
    private var dcTempFile1: File? = null
    private val dcTempFile2: File? = null

    private var service: DublinCoreCatalogService? = null

    @Rule
    var testFolder = TemporaryFolder()

    /**
     * @throws java.lang.Exception
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        catalogFile = File(this.javaClass.getResource(catalogName).toURI())
        val tmpCatalogFile = testFolder.newFile()
        FileUtils.copyFile(catalogFile!!, tmpCatalogFile)
        val tmpCatalogFile2 = testFolder.newFile()
        FileUtils.copyFile(catalogFile!!, tmpCatalogFile2)
        catalogFile2 = File(this.javaClass.getResource(catalogName2).toURI())
        if (!catalogFile!!.exists() || !catalogFile!!.canRead())
            throw Exception("Unable to access test catalog")
        if (!catalogFile2!!.exists() || !catalogFile2!!.canRead())
            throw Exception("Unable to access test catalog 2")
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject<URI>())).andReturn(catalogFile).anyTimes()
        EasyMock.expect(workspace.get(EasyMock.anyObject<URI>(), EasyMock.anyBoolean()))
                .andReturn(tmpCatalogFile).andReturn(tmpCatalogFile2)
        EasyMock.expect<InputStream>(workspace.read(EasyMock.anyObject<URI>())).andAnswer { FileInputStream(catalogFile!!) }.anyTimes()
        EasyMock.replay(workspace)
        service = DublinCoreCatalogService()
        service!!.setWorkspace(workspace)
    }

    /**
     * @throws java.io.IOException
     */
    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.deleteQuietly(dcTempFile1)
        FileUtils.deleteQuietly(dcTempFile2)
    }

    /**
     * Test method for [org.opencastproject.metadata.dublincore.DublinCoreCatalog.fromFile] .
     */
    @Test
    @Throws(Exception::class)
    fun testFromFile() {
        var dc: DublinCoreCatalog? = null
        val `in` = FileInputStream(catalogFile!!)
        dc = DublinCores.read(`in`)
        IOUtils.closeQuietly(`in`)

        // Check if the fields are available
        assertEquals("ETH Zurich, Switzerland", dc.getFirst(PROPERTY_PUBLISHER, LANGUAGE_UNDEFINED))
        assertEquals("Land and Vegetation: Key players on the Climate Scene",
                dc.getFirst(PROPERTY_TITLE, DublinCore.LANGUAGE_UNDEFINED))
        assertNotNull(dc.getFirst(PROPERTY_TITLE))
        assertNull(dc.getFirst(PROPERTY_TITLE, "fr"))
        // Test custom metadata element
        assertEquals("true", dc.getFirst(OC_PROPERTY_PROMOTED))
    }

    /**
     * Test method for [DublinCoreCatalogList.parse] with an XML String
     */
    @Test
    @Throws(Exception::class)
    fun testParseDublinCoreListXML() {
        val dublinCoreListString = IOUtils.toString(javaClass.getResourceAsStream(xmlCatalogListName), "UTF-8")
        val catalogList = DublinCoreCatalogList.parse(dublinCoreListString)
        Assert.assertEquals(2, catalogList.totalCount)
        Assert.assertEquals("Land 1", catalogList.catalogList[0].getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED))
        Assert.assertEquals("Land 2", catalogList.catalogList[1].getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED))
    }

    /**
     * Test method for [DublinCoreCatalogList.parse] with a JSON String
     */
    @Test
    @Throws(Exception::class)
    fun testParseDublinCoreListJSON() {
        val dublinCoreListString = IOUtils.toString(javaClass.getResourceAsStream(jsonCatalogListName), "UTF-8")
        val catalogList = DublinCoreCatalogList.parse(dublinCoreListString)
        Assert.assertEquals(2, catalogList.totalCount)
        Assert.assertEquals("Land 1", catalogList.catalogList[0].getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED))
        Assert.assertEquals("Land 2", catalogList.catalogList[1].getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED))
    }

    /**
     * Test method for [DublinCoreCatalog.toJson] .
     */
    @Test
    @Throws(Exception::class)
    fun testJson() {
        var dc: DublinCoreCatalog? = null
        val `in` = FileInputStream(catalogFile!!)
        dc = DublinCores.read(`in`)
        IOUtils.closeQuietly(`in`)

        val jsonString = dc.toJson()

        val parser = JSONParser()
        val jsonObject = parser.parse(jsonString) as JSONObject

        val dcTerms = jsonObject[TERMS_NS_URI] as JSONObject
        assertNotNull(dcTerms)

        val titleArray = dcTerms["title"] as JSONArray
        assertEquals("Three titles should be present", 3, titleArray.size.toLong())

        val subjectArray = dcTerms["subject"] as JSONArray
        assertEquals("The subject should be present", 1, subjectArray.size.toLong())

        val fromJson = DublinCores.read(IOUtils.toInputStream(jsonString))
        assertEquals(3, fromJson.getLanguages(PROPERTY_TITLE).size.toLong())
        assertEquals("video/x-dv", fromJson.getFirst(PROPERTY_FORMAT))
        assertEquals("eng", fromJson.getFirst(PROPERTY_LANGUAGE))
        assertEquals("2007-12-05", fromJson.getFirst(PROPERTY_CREATED))

        // Serialize again, and we should get the same json
        val jsonRoundtrip = fromJson.toJson()
        assertEquals(jsonString, jsonRoundtrip)
    }

    /**
     * Test method for saving the catalog.
     */
    @Test
    fun testNewInstance() {
        try {
            // Read the sample catalog
            val `in` = FileInputStream(catalogFile!!)
            val dcSample = DublinCores.read(`in`)
            IOUtils.closeQuietly(`in`)

            // Create a new catalog and fill it with a few fields
            val dcNew = DublinCores.mkOpencastEpisode().catalog
            dcTempFile1 = testFolder.newFile()

            // Add the required fields
            dcNew.add(PROPERTY_IDENTIFIER, dcSample.getFirst(PROPERTY_IDENTIFIER)!!)
            dcNew.add(PROPERTY_TITLE, dcSample.getFirst(PROPERTY_TITLE, DublinCore.LANGUAGE_UNDEFINED)!!,
                    DublinCore.LANGUAGE_UNDEFINED)

            // Add an additional field
            dcNew.add(PROPERTY_PUBLISHER, dcSample.getFirst(PROPERTY_PUBLISHER)!!)

            // Add a null-value field
            try {
                dcNew.add(PROPERTY_CONTRIBUTOR, (null as String?)!!)
                fail()
            } catch (ignore: IllegalArgumentException) {
            }

            // Add a field with an encoding scheme
            dcNew.add(PROPERTY_LICENSE, DublinCoreValue.mk("http://www.opencastproject.org/license",
                    DublinCore.LANGUAGE_UNDEFINED, ENC_SCHEME_URI))
            // Don't forget to bind the namespace...
            dcNew.addBindings(XmlNamespaceContext.mk("octest", "http://www.opencastproject.org/octest"))
            dcNew.add(OC_PROPERTY_PROMOTED, DublinCoreValue.mk("true", DublinCore.LANGUAGE_UNDEFINED,
                    EName("http://www.opencastproject.org/octest", "Boolean")))
            try {
                dcNew.add(OC_PROPERTY_PROMOTED, DublinCoreValue.mk("true", DublinCore.LANGUAGE_UNDEFINED,
                        EName.mk("http://www.opencastproject.org/enc-scheme", "Boolean")))
                fail()
            } catch (e: NamespaceBindingException) {
                // Ok. This exception is expected to occur
            }

            // Store the catalog
            val transfac = TransformerFactory.newInstance()
            val trans = transfac.newTransformer()
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            trans.setOutputProperty(OutputKeys.METHOD, "xml")
            val sw = FileWriter(dcTempFile1!!)
            val result = StreamResult(sw)
            val source = DOMSource(dcNew.toXml())
            trans.transform(source, result)

            // Re-read the saved catalog and test for its content
            val dcNewFromDisk = DublinCores.read(dcTempFile1!!.toURI().toURL().openStream())
            assertEquals(dcSample.getFirst(PROPERTY_IDENTIFIER), dcNewFromDisk.getFirst(PROPERTY_IDENTIFIER))
            assertEquals(dcSample.getFirst(PROPERTY_TITLE, "en"), dcNewFromDisk.getFirst(PROPERTY_TITLE, "en"))
            assertEquals(dcSample.getFirst(PROPERTY_PUBLISHER), dcNewFromDisk.getFirst(PROPERTY_PUBLISHER))

        } catch (e: IOException) {
            fail("Error creating the catalog: " + e.message)
        } catch (e: ParserConfigurationException) {
            fail("Error creating a parser for the catalog: " + e.message)
        } catch (e: TransformerException) {
            fail("Error saving the catalog: " + e.message)
        }

    }

    /**
     * Tests overwriting of values.
     */
    @Test
    fun testOverwriting() {
        // Create a new catalog and fill it with a few fields
        var dcNew: DublinCoreCatalog? = null
        dcNew = DublinCores.mkOpencastEpisode().catalog
        dcNew!!.set(PROPERTY_TITLE, "Title 1")
        assertEquals("Title 1", dcNew.getFirst(PROPERTY_TITLE))

        dcNew.set(PROPERTY_TITLE, "Title 2")
        assertEquals("Title 2", dcNew.getFirst(PROPERTY_TITLE))

        dcNew.set(PROPERTY_TITLE, "Title 3", "de")
        assertEquals("Title 2", dcNew.getFirst(PROPERTY_TITLE))
        assertEquals("Title 3", dcNew.getFirst(PROPERTY_TITLE, "de"))
        dcNew = null
    }

    @Test
    @Throws(NoSuchAlgorithmException::class, IOException::class, UnknownFileTypeException::class)
    fun testVarious() {
        val dc = DublinCores.mkOpencastEpisode().catalog
        // Add a title
        dc.add(PROPERTY_TITLE, "Der alte Mann und das Meer")
        assertEquals("Der alte Mann und das Meer", dc.getFirst(PROPERTY_TITLE))
        assertEquals(1, dc.get(PROPERTY_TITLE, LANGUAGE_UNDEFINED).size.toLong())
        assertEquals(1, dc.get(PROPERTY_TITLE).size.toLong())
        // Overwrite the title
        dc.set(PROPERTY_TITLE, "La Peste")
        assertEquals("La Peste", dc.getFirst(PROPERTY_TITLE))
        assertEquals(1, dc.get(PROPERTY_TITLE).size.toLong())

        dc.set(PROPERTY_TITLE, "Die Pest", "de")
        assertEquals(2, dc.get(PROPERTY_TITLE).size.toLong())
        assertEquals(1, dc.get(PROPERTY_TITLE, LANGUAGE_UNDEFINED).size.toLong())

        // Remove the title without language code
        dc.remove(PROPERTY_TITLE, LANGUAGE_UNDEFINED)
        // The german title is now the only remaining title so we should get it here
        assertEquals("Die Pest", dc.getFirst(PROPERTY_TITLE))
        assertNotNull(dc.getFirst(PROPERTY_TITLE, "de"))
        assertNull(dc.getFirst(PROPERTY_TITLE, "fr"))
        assertEquals(1, dc.get(PROPERTY_TITLE).size.toLong())
        assertTrue(dc.hasValue(PROPERTY_TITLE))
        assertFalse(dc.hasMultipleValues(PROPERTY_TITLE))

        // Add a german title (does not make sense...)
        dc.add(PROPERTY_TITLE, "nonsense", "de")
        assertEquals(2, dc.get(PROPERTY_TITLE, "de").size.toLong())
        assertEquals(2, dc.get(PROPERTY_TITLE).size.toLong())

        // Now restore the orginal french title
        dc.set(PROPERTY_TITLE, "La Peste")
        assertEquals(3, dc.get(PROPERTY_TITLE).size.toLong())

        // And get rid of the german ones
        dc.remove(PROPERTY_TITLE, "de")
        assertEquals(0, dc.get(PROPERTY_TITLE, "de").size.toLong())
        assertNull(dc.getFirst(PROPERTY_TITLE, "de"))
        assertEquals(1, dc.get(PROPERTY_TITLE).size.toLong())

        // No contributor is set so expect null
        assertNull(dc.getFirst(PROPERTY_CONTRIBUTOR))
        // ... but expect an empty list here
        assertNotNull(dc.get(PROPERTY_CONTRIBUTOR))
    }

    @Test
    @Throws(NoSuchAlgorithmException::class, IOException::class, UnknownFileTypeException::class)
    fun testVarious2() {
        val dc = DublinCores.mkOpencastEpisode().catalog
        dc.add(PROPERTY_TITLE, "The Lord of the Rings")
        dc.add(PROPERTY_TITLE, "Der Herr der Ringe", "de")
        assertEquals(2, dc.getLanguages(PROPERTY_TITLE).size.toLong())

        assertEquals("The Lord of the Rings; Der Herr der Ringe", dc.getAsText(PROPERTY_TITLE, LANGUAGE_ANY, "; "))
        assertNull(dc.getAsText(PROPERTY_CONTRIBUTOR, LANGUAGE_ANY, "; "))

        dc.remove(PROPERTY_TITLE, "de")
        assertEquals(1, dc.getLanguages(PROPERTY_TITLE).size.toLong())

        dc.remove(PROPERTY_TITLE)

        assertNull(dc.getAsText(PROPERTY_TITLE, LANGUAGE_ANY, "; "))
    }

    @Test
    @Throws(NoSuchAlgorithmException::class, IOException::class, UnknownFileTypeException::class)
    fun testVarious3() {
        val dc = DublinCores.mkOpencastEpisode().catalog
        dc.add(PROPERTY_CONTRIBUTOR, "Heinz Strunk")
        dc.add(PROPERTY_CONTRIBUTOR, "Rocko Schamoni")
        dc.add(PROPERTY_CONTRIBUTOR, "Jacques Palminger")
        assertTrue(dc.hasValue(PROPERTY_CONTRIBUTOR))
        assertTrue(dc.hasValue(PROPERTY_CONTRIBUTOR, LANGUAGE_UNDEFINED))
        // assertTrue(dc.hasMultipleValues(PROPERTY_TITLE));
        assertEquals(3, dc.get(PROPERTY_CONTRIBUTOR).size.toLong())

        dc.add(PROPERTY_CONTRIBUTOR, "Klaus Allofs", "de")
        dc.add(PROPERTY_CONTRIBUTOR, "Karl-Heinz Rummenigge", "de")
        assertTrue(dc.hasValue(PROPERTY_CONTRIBUTOR, "de"))
        assertTrue(dc.hasMultipleValues(PROPERTY_CONTRIBUTOR, "de"))
        assertEquals(2, dc.get(PROPERTY_CONTRIBUTOR, "de").size.toLong())
        assertEquals(5, dc.get(PROPERTY_CONTRIBUTOR).size.toLong())

        dc.set(PROPERTY_CONTRIBUTOR, "Hans Manzke")
        assertEquals(1, dc.get(PROPERTY_CONTRIBUTOR, LANGUAGE_UNDEFINED).size.toLong())
        assertEquals(3, dc.get(PROPERTY_CONTRIBUTOR).size.toLong())
    }

    @Test
    @Throws(NoSuchAlgorithmException::class, IOException::class, UnknownFileTypeException::class)
    fun testVarious4() {
        val dc = DublinCores.mkOpencastEpisode().catalog
        dc.add(PROPERTY_TITLE, "deutsch", "de")
        dc.add(PROPERTY_TITLE, "english", "en")
        assertNull(dc.getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED))
        assertNotNull(dc.getFirst(PROPERTY_TITLE, LANGUAGE_ANY))
        assertNotNull(dc.getFirst(PROPERTY_TITLE))

        dc.add(PROPERTY_TITLE, "undefined")
        assertEquals("undefined", dc.getFirst(PROPERTY_TITLE, LANGUAGE_UNDEFINED))
        assertEquals("undefined", dc.getFirst(PROPERTY_TITLE))
        assertEquals("deutsch", dc.getFirst(PROPERTY_TITLE, "de"))
    }

    @Test
    fun testSet() {
        val dc = DublinCores.mkOpencastEpisode().catalog
        dc.set(PROPERTY_CREATOR,
                Arrays.asList(DublinCoreValue.mk("Klaus"), DublinCoreValue.mk("Peter"), DublinCoreValue.mk("Carl", "en")))
        assertEquals(2, dc.get(PROPERTY_CREATOR, LANGUAGE_UNDEFINED).size.toLong())
        assertEquals(3, dc.get(PROPERTY_CREATOR).size.toLong())
        assertEquals("Klaus", dc.get(PROPERTY_CREATOR, LANGUAGE_UNDEFINED)[0])
    }

    @Test
    @Throws(Exception::class)
    fun testMediaPackageMetadataExtraction() {
        // Load the DC catalog
        var `in`: FileInputStream? = null
        var catalog: DublinCoreCatalog? = null
        try {
            `in` = FileInputStream(catalogFile!!)
            catalog = service!!.load(`in`)
        } finally {
            IOUtils.closeQuietly(`in`)
        }

        // Create a mediapackage containing the DC catalog
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        mp.add(catalogFile!!.toURI(), Catalog.TYPE, MediaPackageElements.EPISODE)
        mp.add(catalogFile2!!.toURI(), Catalog.TYPE, MediaPackageElements.SERIES)
        val metadata = service!!.getMetadata(mp)

        assertEquals("Mediapackage metadata title not extracted from DC properly",
                catalog!!.getFirst(DublinCore.PROPERTY_TITLE), metadata.title)
    }

    // todo fix http://opencast.jira.com/browse/MH-8759 then remove @Ignore
    @Ignore
    @Test
    fun testPreserveEncodingScheme() {
        val dc = DublinCores.mkOpencastEpisode().catalog
        val `val` = DublinCoreValue.mk("http://www.opencastproject.org/license", "en", ENC_SCHEME_URI)
        dc.add(PROPERTY_LICENSE, `val`)
        assertEquals(1, dc.get(PROPERTY_LICENSE).size.toLong())
        assertEquals(`val`, dc.get(PROPERTY_LICENSE)[0])
        assertEquals(Opt.some(ENC_SCHEME_URI), dc.get(PROPERTY_LICENSE)[0].encodingScheme)
    }

    @Test
    @Throws(Exception::class)
    fun testSerializeDublinCore() {
        var dc: DublinCoreCatalog? = null
        val `in` = FileInputStream(catalogFile2!!)
        dc = DublinCores.read(`in`)
        IOUtils.closeQuietly(`in`)

        val inputXml = IOUtils.toString(FileInputStream(catalogFile2!!), "UTF-8")
        Assert.assertTrue(inputXml.contains("xmlns:dcterms=\"http://purl.org/dc/terms/\""))

        Assert.assertTrue(dc.toXmlString().contains("xmlns:dcterms=\"http://purl.org/dc/terms/\""))

        val extent = EncodingSchemeUtils.encodeDuration(3000)
        dc.set(DublinCore.PROPERTY_EXTENT, extent)

        Assert.assertTrue(dc.toXmlString().contains("xmlns:dcterms=\"http://purl.org/dc/terms/\""))

        val date = EncodingSchemeUtils.encodeDate(Date(), Precision.Minute)
        dc.set(DublinCore.PROPERTY_CREATED, date)

        Assert.assertTrue(dc.toXmlString().contains("xmlns:dcterms=\"http://purl.org/dc/terms/\""))
    }

    // test that exceptions are thrown correctly
    @Test
    fun testEncodingSchemeUtilsExceptions() {
        val period = DCMIPeriod(java.util.Date(), java.util.Date())
        val precision = Precision.Year
        try {
            EncodingSchemeUtils.encodePeriod(null, precision)
            Assert.fail("Exceptions should be thrown on null values.")
        } catch (e: Exception) {
            Assert.assertFalse(e is NullPointerException)
        }

        try {
            EncodingSchemeUtils.encodePeriod(period, null)
            Assert.fail("Exceptions should be thrown on null values.")
        } catch (e: Exception) {
            Assert.assertFalse(e is NullPointerException)
        }

        try {
            EncodingSchemeUtils.encodePeriod(null, null)
            Assert.fail("Exceptions should be thrown on null values.")
        } catch (e: Exception) {
            Assert.assertFalse(e is NullPointerException)
        }

    }

    @Test
    @Ignore
    @Throws(Exception::class)
    // this test should verify serialization/deserialization works for a fairly minimal case
    // waiting on https://opencast.jira.com/browse/MH-9733
    fun testSerializationDeserializationOfCatalogs() {
        val impl = DublinCores.mkOpencastEpisode().catalog
        impl.addTag("bob")
        impl.set(impl.PROPERTY_PUBLISHER, "test")
        val service = DublinCoreCatalogService()
        val newImpl = service.load(service.serialize(impl))
        Assert.assertEquals(impl, newImpl)
    }

    @Test
    @Throws(Exception::class)
    // test for null values on various methods on the DublinCoreCatalog, they should
    // generally return an exception
    fun testForNullsInDublinCoreCatalogImpl() {
        val dc = DublinCores.mkOpencastEpisode().catalog
        try {
            val `val`: DublinCoreValue? = null
            dc.add(EasyMock.createNiceMock(org.opencastproject.mediapackage.EName::class.java), `val`!!)
        } catch (e: Exception) {
            // throw assertion if it happens to be a nullpointer, never a null pointer!
            Assert.assertFalse(e is NullPointerException)
        }

        try {
            dc.add(null!!, EasyMock.createNiceMock<Any>(DublinCoreValue::class.java) as DublinCoreValue)
        } catch (e: Exception) {
            // throw assertion if it happens to be a nullpointer, never a null pointer!
            Assert.assertFalse(e is NullPointerException)
        }

        try {
            val val2: String? = null
            dc.add(EasyMock.createNiceMock(org.opencastproject.mediapackage.EName::class.java), val2!!)
        } catch (e: Exception) {
            // throw assertion if it happens to be a nullpointer, never a null pointer!
            Assert.assertFalse(e is NullPointerException)
        }

        try {
            dc.add(null!!, "")
        } catch (e: Exception) {
            // throw assertion if it happens to be a nullpointer, never a null pointer!
            Assert.assertFalse(e is NullPointerException)
        }

        try {
            val val2: String? = null
            dc.add(EasyMock.createNiceMock(org.opencastproject.mediapackage.EName::class.java), val2!!, val2)
        } catch (e: Exception) {
            // throw assertion if it happens to be a nullpointer, never a null pointer!
            Assert.assertFalse(e is NullPointerException)
        }

        try {
            val val2: String? = null
            dc.add(EasyMock.createNiceMock(org.opencastproject.mediapackage.EName::class.java), "", val2!!)
        } catch (e: Exception) {
            // throw assertion if it happens to be a nullpointer, never a null pointer!
            Assert.assertFalse(e is NullPointerException)
        }

        try {
            dc.add(null!!, "", "")
        } catch (e: Exception) {
            // throw assertion if it happens to be a nullpointer, never a null pointer!
            Assert.assertFalse(e is NullPointerException)
        }

    }

    @Test
    fun testClone() {
        val dc = DublinCores.mkOpencastEpisode().catalog
        val mimeType = MimeType.mimeType("text", "xml")
        dc.mimeType = MimeType.mimeType("text", "xml")
        dc.reference = MediaPackageReferenceImpl("type", "identifier")
        dc.setURI(uri("http://localhost"))
        assertNotNull(dc.mimeType)
        assertEquals(mimeType, dc.mimeType)
        // clone
        val dcClone = dc.clone() as DublinCoreCatalog
        assertEquals("The mime type should be cloned", dc.mimeType, dcClone.mimeType)
        assertEquals("The flavor should be cloned", dc.flavor, dcClone.flavor)
        assertEquals("The values should be cloned", dc.values, dcClone.values)
        assertNull("The URI should not be cloned", dcClone.getURI())
        assertNull("A media package reference should not be cloned.", dcClone.reference)
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(DublinCoreTest::class.java)

        /**
         * The catalog name
         */
        private val catalogName = "/dublincore.xml"
        private val catalogName2 = "/dublincore2.xml"

        /** The XML catalog list name  */
        private val xmlCatalogListName = "/dublincore-list.xml"

        /** The JSON catalog list name  */
        private val jsonCatalogListName = "/dublincore-list.json"
    }
}
