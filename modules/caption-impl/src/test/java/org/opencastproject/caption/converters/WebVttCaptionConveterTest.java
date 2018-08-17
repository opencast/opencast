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
package org.opencastproject.caption.converters;

import org.opencastproject.caption.api.Caption;
import org.opencastproject.caption.impl.CaptionImpl;
import org.opencastproject.caption.impl.TimeImpl;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for WebVTT format.
 *
 */
public class WebVttCaptionConveterTest {

  private WebVttCaptionConverter format;
  private ByteArrayOutputStream outputStream;

  private static final String CAPTION_LINE = "This is caption testing line ";
  // Expected output
  private String expectedOutput = "WEBVTT\n\n00:00:49.520 --> 00:00:52.961\n" + CAPTION_LINE
          + "1.\n\n00:00:54.123 --> 00:00:56.456\n" + CAPTION_LINE + "2.\n\n";

  @Before
  public void setUp() throws IOException {
    format = new WebVttCaptionConverter();
    outputStream = new ByteArrayOutputStream();
  }

  @After
  public void tearDown() throws IOException {
    IOUtils.closeQuietly(outputStream);
  }

  @Test
  public void testExport() throws Exception {
    List<Caption> captionList = new ArrayList<Caption>();
    String[] captionLines1 = new String[1];
    captionLines1[0] = CAPTION_LINE + "1.";
    captionList.add(new CaptionImpl(new TimeImpl(0, 0, 49, 520), new TimeImpl(0, 0, 52, 961), captionLines1));
    String[] captionLines2 = new String[1];
    captionLines2[0] = CAPTION_LINE + "2.";
    captionList.add(new CaptionImpl(new TimeImpl(0, 0, 54, 123), new TimeImpl(0, 0, 56, 456), captionLines2));

    format.exportCaption(outputStream, captionList, null);
    Assert.assertTrue(outputStream.toString("UTF-8").equals(expectedOutput));
  }
}
