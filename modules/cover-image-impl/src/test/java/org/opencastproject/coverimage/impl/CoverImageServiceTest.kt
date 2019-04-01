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

package org.opencastproject.coverimage.impl

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.xmlmatchers.XmlMatchers.hasXPath
import org.xmlmatchers.transform.XmlConverters.the

import org.opencastproject.coverimage.CoverImageException

import org.apache.commons.io.IOUtils
import org.dom4j.dom.DOMDocument
import org.junit.Test
import org.w3c.dom.Document
import org.xmlmatchers.namespace.SimpleNamespaceContext

import java.io.InputStream
import java.io.StringWriter
import java.io.Writer

import javax.xml.namespace.NamespaceContext
import javax.xml.transform.Result
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * Test class for [AbstractCoverImageService]
 */
class CoverImageServiceTest {

    /**
     * Tests [AbstractCoverImageService.parseXsl]
     */
    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testParseXslNull() {
        AbstractCoverImageService.parseXsl(null)
    }

    /**
     * Tests [AbstractCoverImageService.parseXsl]
     */
    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testParseXslBlankString() {
        AbstractCoverImageService.parseXsl(null)
    }

    /**
     * Tests [AbstractCoverImageService.parseXsl]
     */
    @Test(expected = CoverImageException::class)
    @Throws(Exception::class)
    fun testParseXslInvalidXsl() {
        AbstractCoverImageService.parseXsl("this is not a valid XSL string")
    }

    /**
     * Tests [AbstractCoverImageService.parseXsl]
     */
    @Test
    @Throws(Exception::class)
    fun testParseXsl() {

        val xslIn = this.javaClass.getResourceAsStream("/metadata2svg.xsl")
        val writer = StringWriter()
        IOUtils.copy(xslIn, writer, "UTF-8")
        val xslString = writer.toString()
        val xslDoc = AbstractCoverImageService.parseXsl(xslString)
        assertNotNull(xslDoc)

        val usingNamespaces = SimpleNamespaceContext().withBinding("xsl",
                "http://www.w3.org/1999/XSL/Transform").withBinding("svg", "http://www.w3.org/2000/svg")
        assertThat(the(xslDoc),
                hasXPath("/xsl:stylesheet/xsl:template/svg:svg/svg:defs/svg:linearGradient[@id='lgGray']", usingNamespaces))
    }

    /**
     * Tests [AbstractCoverImageService.transformSvg]
     */
    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testTransformSvgNullSvg() {
        AbstractCoverImageService.transformSvg(null, StreamSource(), DOMDocument(), 0, 0, null)
    }

    /**
     * Tests [AbstractCoverImageService.transformSvg]
     */
    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testTransformSvgNullXmlSource() {
        AbstractCoverImageService.transformSvg(StreamResult(), null, DOMDocument(), 0, 0, null)
    }

    /**
     * Tests [AbstractCoverImageService.transformSvg]
     */
    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testTransformSvgNullXslDoc() {
        AbstractCoverImageService.transformSvg(StreamResult(), StreamSource(), null, 0, 0, null)
    }

    /**
     * Tests [AbstractCoverImageService.transformSvg]
     */
    @Test
    @Throws(Exception::class)
    fun testTransformSvg() {
        val svgWriter = StringWriter()
        val svg = StreamResult(svgWriter)

        val isXml = CoverImageServiceTest::class.java.getResourceAsStream("/metadata.xml")
        val xmlSource = StreamSource(isXml)

        val isXsl = CoverImageServiceTest::class.java.getResourceAsStream("/metadata2svg.xsl")
        val xslDoc = AbstractCoverImageService.parseXsl(IOUtils.toString(isXsl))

        AbstractCoverImageService.transformSvg(svg, xmlSource, xslDoc, 1600, 900, null)

        val svgString = svgWriter.toString()
        val nsContext = SimpleNamespaceContext().withBinding("svg", "http://www.w3.org/2000/svg")
        assertThat(the(svgString), hasXPath("//svg:svg[@width='1600']", nsContext))
        assertThat(the(svgString), hasXPath("//svg:svg/svg:text/svg:tspan", nsContext))
    }
}
