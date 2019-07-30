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
import org.opencastproject.caption.api.Time;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

/**
 *
 * @author franck
 */
public class GoogleSpeechCaptionConverterTest {

  private GoogleSpeechCaptionConverter converter;
  private InputStream inputStream;

  @Before
  public void setUp() throws Exception {
    converter = new GoogleSpeechCaptionConverter();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testImportCaption() throws Exception {
    inputStream = GoogleSpeechCaptionConverterTest.class.getResourceAsStream("/pulled_google_transcription.json");
    importCaption();
  }

  private void importCaption() throws Exception {
    List<Caption> captionList = converter.importCaption(inputStream, "80");
    Assert.assertEquals(3, captionList.size());
    Caption caption = captionList.get(0);
    String[] text = caption.getCaption();
    Assert.assertEquals(1, text.length);
    Assert.assertEquals("what we need to see TV device that requires that already has been practising talking ",
            text[0]);
    Time time = caption.getStartTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(0, time.getSeconds());
    Assert.assertEquals(0, time.getMilliseconds());

    time = caption.getStopTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(37, time.getSeconds());
    Assert.assertEquals(0, time.getMilliseconds());

    caption = captionList.get(1);
    text = caption.getCaption();
    Assert.assertEquals(1, text.length);
    Assert.assertEquals("about safeguarding. ", text[0]);
    time = caption.getStartTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(37, time.getSeconds());
    Assert.assertEquals(0, time.getMilliseconds());

    time = caption.getStopTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(38, time.getSeconds());
    Assert.assertEquals(700, time.getMilliseconds());

    caption = captionList.get(2);
    text = caption.getCaption();
    Assert.assertEquals(1, text.length);
    Assert.assertEquals("I'm going on social work so I'm going to get through ", text[0]);
    time = caption.getStartTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(1, time.getMinutes());
    Assert.assertEquals(32, time.getSeconds());
    Assert.assertEquals(400, time.getMilliseconds());

    time = caption.getStopTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(2, time.getMinutes());
    Assert.assertEquals(24, time.getSeconds());
    Assert.assertEquals(600, time.getMilliseconds());
  }
}
