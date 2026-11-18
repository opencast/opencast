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

public class IBMWatsonCaptionConverterTest {
  private IBMWatsonCaptionConverter converter;
  private InputStream inputStream;

  @BeforeEach
  public void setUp() throws Exception {
    converter = new IBMWatsonCaptionConverter();
  }

  @AfterEach
  public void tearDown() throws Exception {
  }

  @Test
  public void testImportCaptionPush() throws Exception {
    inputStream = IBMWatsonCaptionConverterTest.class.getResourceAsStream("/pulled_transcription.json");
    importCaption();
  }

  @Test
  public void testImportCaptionPull() throws Exception {
    inputStream = IBMWatsonCaptionConverterTest.class.getResourceAsStream("/pushed_transcription.json");
    importCaption();
  }

  private void importCaption() throws Exception {
    List<Caption> captionList = converter.importCaption(inputStream, "");
    Assertions.assertEquals(7, captionList.size());
    Caption caption = captionList.get(0);
    String[] text = caption.getCaption();
    Assertions.assertEquals(1, text.length);
    Assertions.assertEquals(
            "in the earliest days it was a style of programming called imperative programming language ",
            text[0]);
    Time time = caption.getStartTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(0, time.getSeconds());
    Assertions.assertEquals(750, time.getMilliseconds());

    time = caption.getStopTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(5, time.getSeconds());
    Assertions.assertEquals(240, time.getMilliseconds());

    caption = captionList.get(1);
    text = caption.getCaption();
    Assertions.assertEquals(1, text.length);
    Assertions.assertEquals("principal example of that is the language see ", text[0]);
    time = caption.getStartTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(7, time.getSeconds());
    Assertions.assertEquals(460, time.getMilliseconds());

    time = caption.getStopTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(10, time.getSeconds());
    Assertions.assertEquals(150, time.getMilliseconds());

    caption = captionList.get(2);
    text = caption.getCaption();
    Assertions.assertEquals(1, text.length);
    Assertions.assertEquals(
            "it is rather old because Sarah is fact stems from the late 19 seventies but he still use a great deal ",
            text[0]);
    time = caption.getStartTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(10, time.getSeconds());
    Assertions.assertEquals(620, time.getMilliseconds());

    time = caption.getStopTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(18, time.getSeconds());
    Assertions.assertEquals(110, time.getMilliseconds());

    caption = captionList.get(3);
    text = caption.getCaption();
    Assertions.assertEquals(1, text.length);
    Assertions.assertEquals("in fact is the principal programming language that's taught ", text[0]);
    time = caption.getStartTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(18, time.getSeconds());
    Assertions.assertEquals(110, time.getMilliseconds());

    time = caption.getStopTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(20, time.getSeconds());
    Assertions.assertEquals(960, time.getMilliseconds());

    caption = captionList.get(4);
    text = caption.getCaption();
    Assertions.assertEquals(1, text.length);
    Assertions.assertEquals("in a very popular ", text[0]);
    time = caption.getStartTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(21, time.getSeconds());
    Assertions.assertEquals(490, time.getMilliseconds());

    time = caption.getStopTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(22, time.getSeconds());
    Assertions.assertEquals(580, time.getMilliseconds());

    caption = captionList.get(5);
    text = caption.getCaption();
    Assertions.assertEquals(1, text.length);
    Assertions.assertEquals(
        "a computer science course called CS 15 see if it is up to become the largest undergraduate course "
            + "herpetological ",
        text[0]);
    time = caption.getStartTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(23, time.getSeconds());
    Assertions.assertEquals(320, time.getMilliseconds());

    time = caption.getStopTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(28, time.getSeconds());
    Assertions.assertEquals(900, time.getMilliseconds());

    caption = captionList.get(6);
    text = caption.getCaption();
    Assertions.assertEquals(1, text.length);
    Assertions.assertEquals("thing office who are extension ", text[0]);
    time = caption.getStartTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(28, time.getSeconds());
    Assertions.assertEquals(900, time.getMilliseconds());

    time = caption.getStopTime();
    Assertions.assertEquals(0, time.getHours());
    Assertions.assertEquals(0, time.getMinutes());
    Assertions.assertEquals(30, time.getSeconds());
    Assertions.assertEquals(0, time.getMilliseconds());
  }
}
