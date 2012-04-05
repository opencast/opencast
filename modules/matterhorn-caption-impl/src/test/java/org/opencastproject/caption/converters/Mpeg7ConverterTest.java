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

package org.opencastproject.caption.converters;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.caption.api.Caption;
import org.opencastproject.caption.api.CaptionConverterException;
import org.opencastproject.caption.api.IllegalTimeFormatException;
import org.opencastproject.caption.api.Time;
import org.opencastproject.caption.impl.TimeImpl;

import junit.framework.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Test class for Mpeg7 format.
 * 
 */
public class Mpeg7ConverterTest {

  // SubRip converter
  private Mpeg7CaptionConverter converter;
  // mpeg7 sample
  private InputStream inputStream;
  // output stream
  private ByteArrayOutputStream outputStream;
  // expected second segment start time
  private Time time;
  // expected output
  private static final String EXPECTED_LAST_CAPTION = "ENJOYS GETTING QUESTIONS";
  // Captions language
  private static final String LANGUAGE = "en";
  // Sample file
  private static final String FILE = "/sample.mpeg7.xml";

  @Before
  public void setUp() throws IOException, IllegalTimeFormatException {
    converter = new Mpeg7CaptionConverter();
    inputStream = Mpeg7ConverterTest.class.getResourceAsStream(FILE);
    outputStream = new ByteArrayOutputStream();
    time = new TimeImpl(0, 0, 5, 89);
  }

  @Test
  public void testImportAndExport() {
    try {
      // Test import from example file
      List<Caption> collection = testImport(inputStream);

      IOUtils.closeQuietly(inputStream);

      converter.exportCaption(outputStream, collection, LANGUAGE);

      inputStream = new ByteArrayInputStream(outputStream.toByteArray());

      testImport(inputStream);

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  public List<Caption> testImport(InputStream inputStream) {
    List<Caption> collection = null;

    try {
      collection = converter.importCaption(inputStream, LANGUAGE);

      int nbCaption = collection.size();

      // Check size
      Assert.assertEquals(25, nbCaption);

      // Check the last caption value
      Assert.assertEquals(EXPECTED_LAST_CAPTION, collection.get(nbCaption - 1).getCaption()[0]);

      // Check start time from second segment
      Assert.assertEquals(0, collection.get(1).getStartTime().compareTo(time));

    } catch (CaptionConverterException e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }

    return collection;
  }

  @After
  public void tear() throws IOException {
    IOUtils.closeQuietly(inputStream);
    IOUtils.closeQuietly(outputStream);
  }

}
