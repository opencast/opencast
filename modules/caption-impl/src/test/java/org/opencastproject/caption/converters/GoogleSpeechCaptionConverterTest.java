/*
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

/**
 *
 * @author franck
 */
public class GoogleSpeechCaptionConverterTest {

  private GoogleSpeechCaptionConverter converter;
  private InputStream inputStream;

  @BeforeEach
  public void setUp() throws Exception {
    converter = new GoogleSpeechCaptionConverter();
  }

  @AfterEach
  public void tearDown() throws Exception {
  }

  @Test
  public void testImportCaption() throws Exception {
    inputStream = GoogleSpeechCaptionConverterTest.class.getResourceAsStream("/pulled_google_transcription.json");
    importCaption();
  }

  private void importCaption() throws Exception {
    List<Caption> captionList = converter.importCaption(inputStream, "80");
    Assertions.assertEquals(3, captionList.size());
    Caption caption = captionList.get(0);
    String[] text = caption.getCaption();
    Assertions.assertEquals(1, text.length);
    Assertions.assertEquals("what we need to see TV device that requires that already has been practising talking ",
            text[0]);
    Time time = caption.getStartTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(0, time.getSeconds());
    Assertions.assertEquals(0, time.getMilliseconds());

    time = caption.getStopTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(37, time.getSeconds());
    Assertions.assertEquals(0, time.getMilliseconds());

    caption = captionList.get(1);
    text = caption.getCaption();
    Assertions.assertEquals(1, text.length);
    Assertions.assertEquals("about safeguarding. ", text[0]);
    time = caption.getStartTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(37, time.getSeconds());
    Assertions.assertEquals(0, time.getMilliseconds());

    time = caption.getStopTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(38, time.getSeconds());
    Assertions.assertEquals(700, time.getMilliseconds());

    caption = captionList.get(2);
    text = caption.getCaption();
    Assertions.assertEquals(1, text.length);
    Assertions.assertEquals("I'm going on social work so I'm going to get through ", text[0]);
    time = caption.getStartTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(1, time.getMinutes());
    Assertions.assertEquals(32, time.getSeconds());
    Assertions.assertEquals(400, time.getMilliseconds());

    time = caption.getStopTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(2, time.getMinutes());
    Assertions.assertEquals(24, time.getSeconds());
    Assertions.assertEquals(600, time.getMilliseconds());
  }
}
