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

public class IBMWatsonCaptionConverterTest {
  private IBMWatsonCaptionConverter converter;
  private InputStream inputStream;

  @Before
  public void setUp() throws Exception {
    converter = new IBMWatsonCaptionConverter();
  }

  @After
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
    Assert.assertEquals(7, captionList.size());
    Caption caption = captionList.get(0);
    String[] text = caption.getCaption();
    Assert.assertEquals(1, text.length);
    Assert.assertEquals("in the earliest days it was a style of programming called imperative programming language ",
            text[0]);
    Time time = caption.getStartTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(0, time.getSeconds());
    Assert.assertEquals(750, time.getMilliseconds());

    time = caption.getStopTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(5, time.getSeconds());
    Assert.assertEquals(240, time.getMilliseconds());

    caption = captionList.get(1);
    text = caption.getCaption();
    Assert.assertEquals(1, text.length);
    Assert.assertEquals("principal example of that is the language see ", text[0]);
    time = caption.getStartTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(7, time.getSeconds());
    Assert.assertEquals(460, time.getMilliseconds());

    time = caption.getStopTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(10, time.getSeconds());
    Assert.assertEquals(150, time.getMilliseconds());

    caption = captionList.get(2);
    text = caption.getCaption();
    Assert.assertEquals(1, text.length);
    Assert.assertEquals(
            "it is rather old because Sarah is fact stems from the late 19 seventies but he still use a great deal ",
            text[0]);
    time = caption.getStartTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(10, time.getSeconds());
    Assert.assertEquals(620, time.getMilliseconds());

    time = caption.getStopTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(18, time.getSeconds());
    Assert.assertEquals(110, time.getMilliseconds());

    caption = captionList.get(3);
    text = caption.getCaption();
    Assert.assertEquals(1, text.length);
    Assert.assertEquals("in fact is the principal programming language that's taught ", text[0]);
    time = caption.getStartTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(18, time.getSeconds());
    Assert.assertEquals(110, time.getMilliseconds());

    time = caption.getStopTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(20, time.getSeconds());
    Assert.assertEquals(960, time.getMilliseconds());

    caption = captionList.get(4);
    text = caption.getCaption();
    Assert.assertEquals(1, text.length);
    Assert.assertEquals("in a very popular ", text[0]);
    time = caption.getStartTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(21, time.getSeconds());
    Assert.assertEquals(490, time.getMilliseconds());

    time = caption.getStopTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(22, time.getSeconds());
    Assert.assertEquals(580, time.getMilliseconds());

    caption = captionList.get(5);
    text = caption.getCaption();
    Assert.assertEquals(1, text.length);
    Assert.assertEquals(
            "a computer science course called CS 15 see if it is up to become the largest undergraduate course herpetological ",
            text[0]);
    time = caption.getStartTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(23, time.getSeconds());
    Assert.assertEquals(320, time.getMilliseconds());

    time = caption.getStopTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(28, time.getSeconds());
    Assert.assertEquals(900, time.getMilliseconds());

    caption = captionList.get(6);
    text = caption.getCaption();
    Assert.assertEquals(1, text.length);
    Assert.assertEquals("thing office who are extension ", text[0]);
    time = caption.getStartTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(28, time.getSeconds());
    Assert.assertEquals(900, time.getMilliseconds());

    time = caption.getStopTime();
    Assert.assertEquals(0, time.getHours());
    Assert.assertEquals(0, time.getMinutes());
    Assert.assertEquals(30, time.getSeconds());
    Assert.assertEquals(0, time.getMilliseconds());
  }
}
