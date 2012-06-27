/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.engageui.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class EngageUITest {

  private TransformerFactory transFact;

  public EngageUITest() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    // create an instance of TransformerFactory
    transFact = TransformerFactory.newInstance();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testPlayerXSL() throws Exception {

	/*File xsltFile = new File(this.getClass().getClassLoader().getResource("ui/xsl/player-hybrid-download.xsl")
            .getFile());

    File xmlFile = new File(this.getClass().getClassLoader().getResource("xml/episode.xml").getFile());

    InputStream expectedStream = this.getClass().getClassLoader().getResourceAsStream(
            "xml" + File.separator + "player-expected.xml");
    BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expectedStream));

    String expected = "";

    String lastLine = "";
    while ((lastLine = expectedReader.readLine()) != null)
      expected += lastLine;

    expected = expected.replaceAll("\n", "").replaceAll("\\s+", " ");

    Source xmlSource = new StreamSource(xmlFile);
    Source xsltSource = new StreamSource(xsltFile);

    OutputStream actualStream = new ByteArrayOutputStream();
    Result result = new StreamResult(actualStream);

    Transformer trans = transFact.newTransformer(xsltSource);
    trans.transform(xmlSource, result);
    String actual = actualStream.toString().replaceAll("\n", "").replaceAll("\\s+", " ");

    expectedStream.close();
    expectedReader.close();
    
    Assert.assertTrue(expected.equals(actual));*/
  }

  @Test
  public void testEpisodesXSL() throws Exception {
    File xsltFile = new File(this.getClass().getClassLoader().getResource("ui/xsl/episodes.xsl").toURI());

    File xmlFile = new File(this.getClass().getClassLoader().getResource("xml/episodes.xml").toURI());

    InputStream expectedStream = this.getClass().getClassLoader().getResourceAsStream(
            "xml" + File.separator + "episodes-expected.xml");
    BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expectedStream));

    String expected = "";

    String lastLine = "";
    while ((lastLine = expectedReader.readLine()) != null)
      expected += lastLine;

    expected = expected.replaceAll("\n", "").replaceAll("\\s+", " ");

    Source xmlSource = new StreamSource(xmlFile);
    Source xsltSource = new StreamSource(xsltFile);

    OutputStream actualStream = new ByteArrayOutputStream();
    Result result = new StreamResult(actualStream);

    Transformer trans = transFact.newTransformer(xsltSource);
    trans.transform(xmlSource, result);
    String actual = actualStream.toString().replaceAll("\n", "").replaceAll("\\s+", " ");

    expectedStream.close();
    expectedReader.close();

    Assert.assertTrue(expected.equals(actual));
  }
}
