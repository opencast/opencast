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

package org.opencastproject.coverimage.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.xmlmatchers.XmlMatchers.hasXPath;
import static org.xmlmatchers.transform.XmlConverters.the;

import org.opencastproject.coverimage.CoverImageException;
import org.opencastproject.util.XmlSafeParser;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xmlmatchers.namespace.SimpleNamespaceContext;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;

/**
 * Test class for {@link AbstractCoverImageService}
 */
public class CoverImageServiceTest {

  /**
   * Tests {@link AbstractCoverImageService#parseXsl(String)}
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseXslNull() throws Exception {
    AbstractCoverImageService.parseXsl(null);
  }

  /**
   * Tests {@link AbstractCoverImageService#parseXsl(String)}
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseXslBlankString() throws Exception {
    AbstractCoverImageService.parseXsl(null);
  }

  /**
   * Tests {@link AbstractCoverImageService#parseXsl(String)}
   */
  @Test(expected = CoverImageException.class)
  public void testParseXslInvalidXsl() throws Exception {
    AbstractCoverImageService.parseXsl("this is not a valid XSL string");
  }

  /**
   * Tests {@link AbstractCoverImageService#parseXsl(String)}
   */
  @Test
  public void testParseXsl() throws Exception {

    InputStream xslIn = this.getClass().getResourceAsStream("/metadata2svg.xsl");
    StringWriter writer = new StringWriter();
    IOUtils.copy(xslIn, writer, "UTF-8");
    String xslString = writer.toString();
    Document xslDoc = AbstractCoverImageService.parseXsl(xslString);
    assertNotNull(xslDoc);

    NamespaceContext usingNamespaces = new SimpleNamespaceContext().withBinding("xsl",
            "http://www.w3.org/1999/XSL/Transform").withBinding("svg", "http://www.w3.org/2000/svg");
    assertThat(the(xslDoc),
            hasXPath("/xsl:stylesheet/xsl:template/svg:svg/svg:defs/svg:linearGradient[@id='lgGray']",
              usingNamespaces));
  }

  /**
   * Tests {@link AbstractCoverImageService#transformSvg(Result, Source, Document, int, int, String)}
   */
  @Test(expected = IllegalArgumentException.class)
  public void testTransformSvgNullSvg() throws Exception {
    Document doc = XmlSafeParser.newDocumentBuilderFactory().newDocumentBuilder().newDocument();
    AbstractCoverImageService.transformSvg(null, new InputSource(), doc, 0, 0, null);
  }


  /**
   * Tests {@link AbstractCoverImageService#transformSvg(Result, Source, Document, int, int, String)}
   */
  @Test(expected = IllegalArgumentException.class)
  public void testTransformSvgNullXmlSource() throws Exception {
    Document doc = XmlSafeParser.newDocumentBuilderFactory().newDocumentBuilder().newDocument();
    AbstractCoverImageService.transformSvg(new StreamResult(), null, doc, 0, 0, null);
  }

  /**
   * Tests {@link AbstractCoverImageService#transformSvg(Result, Source, Document, int, int, String)}
   */
  @Test(expected = IllegalArgumentException.class)
  public void testTransformSvgNullXslDoc() throws Exception {
    AbstractCoverImageService.transformSvg(new StreamResult(), new InputSource(), null, 0, 0, null);
  }

  /**
   * Tests {@link AbstractCoverImageService#transformSvg(Result, Source, Document, int, int, String)}
   */
  @Test
  public void testTransformSvg() throws Exception {
    Writer svgWriter = new StringWriter();
    Result svg = new StreamResult(svgWriter);

    InputStream isXml = CoverImageServiceTest.class.getResourceAsStream("/metadata.xml");
    InputSource xmlSource = new InputSource(isXml);

    InputStream isXsl = CoverImageServiceTest.class.getResourceAsStream("/metadata2svg.xsl");
    Document xslDoc = AbstractCoverImageService.parseXsl(IOUtils.toString(isXsl));

    AbstractCoverImageService.transformSvg(svg, xmlSource, xslDoc, 1600, 900, null);

    String svgString = svgWriter.toString();
    NamespaceContext nsContext = new SimpleNamespaceContext().withBinding("svg", "http://www.w3.org/2000/svg");
    assertThat(the(svgString), hasXPath("//svg:svg[@width='1600']", nsContext));
    assertThat(the(svgString), hasXPath("//svg:svg/svg:text/svg:tspan", nsContext));
  }
}
