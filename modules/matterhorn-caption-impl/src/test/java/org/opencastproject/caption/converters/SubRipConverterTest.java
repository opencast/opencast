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

import org.opencastproject.caption.api.Caption;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * Test class for SubRip format.
 *
 */
public class SubRipConverterTest {
  
  // SubRip converter
  private SubRipCaptionConverter format;
  // srt sample
  private InputStream inputStream;
  // output stream
  private ByteArrayOutputStream outputStream;
  // expected output
  private String expectedOutput = "1\r\n"
  		+ "00:00:49,520 --> 00:00:52,961\r\n"
  		+ "This is caption testing.\r\n"
  		+ "1. line.";
  
  @Before
  public void setUp() throws IOException {
    format = new SubRipCaptionConverter();
    inputStream = SubRipConverterTest.class.getResourceAsStream("/sample.srt");
    outputStream = new ByteArrayOutputStream();
  }
  
  @Test
  public void testImportAndExport() {
    try {
      // verify parsing and exporting without exceptions
    	List<Caption> collection = format.importCaption(inputStream, null);
      format.exportCaption(outputStream, collection, null);
      Assert.assertTrue(outputStream.toString("UTF-8").startsWith(expectedOutput));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }
  
  @After
  public void tear() throws IOException {
    IOUtils.closeQuietly(inputStream);
    IOUtils.closeQuietly(outputStream);
  }

}
