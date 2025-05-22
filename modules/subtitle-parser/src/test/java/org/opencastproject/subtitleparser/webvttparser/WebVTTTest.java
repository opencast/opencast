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

package org.opencastproject.subtitleparser.webvttparser;

import org.opencastproject.subtitleparser.SubtitleParsingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Test the subtitle parsing
 */
public class WebVTTTest {
  protected String inputFilePath;
  protected String inputFileWithBom;
  protected String outputFilePath;

  public WebVTTTest() throws URISyntaxException {
    inputFilePath = new File(getClass().getResource("/testresources/example.vtt").toURI()).getAbsolutePath();
    inputFileWithBom = new File(getClass().getResource("/testresources/example-bom.vtt").toURI()).getAbsolutePath();
    outputFilePath = new File("target/testoutput/output.vtt").getAbsolutePath();
  }

  @Before
  public void setUp() {
    if (new File(outputFilePath).exists())  {
      new File(outputFilePath).delete();
    } else if (!new File(outputFilePath).getParentFile().exists()) {
      new File(outputFilePath).getParentFile().mkdir();
    }
  }

  @Test
  public void parseWithoutException() throws IOException, SubtitleParsingException {
    WebVTTParser parser = new WebVTTParser();
    parser.parse(new FileInputStream(inputFilePath));
  }

  @Test
  public void parseBomWithoutException() throws IOException, SubtitleParsingException {
    WebVTTParser parser = new WebVTTParser();
    parser.parse(new FileInputStream(inputFileWithBom));
  }

  @Test
  public void parseCorrectly() throws IOException, SubtitleParsingException {
    WebVTTParser parser = new WebVTTParser();
    WebVTTSubtitle subtitle = parser.parse(new FileInputStream(inputFilePath));

    assertExampleVtt(subtitle);
  }

  @Test
  public void parseBomCorrectly() throws IOException, SubtitleParsingException {
    WebVTTParser parser = new WebVTTParser();
    WebVTTSubtitle subtitle = parser.parse(new FileInputStream(inputFileWithBom));

    assertExampleVtt(subtitle);
  }

  @Test
  public void writeCorrectly() throws IOException, SubtitleParsingException {
    WebVTTParser parser = new WebVTTParser();
    WebVTTSubtitle subtitle = parser.parse(new FileInputStream(inputFilePath));

    WebVTTWriter writer = new WebVTTWriter();
    writer.write(subtitle, new FileOutputStream(outputFilePath));

    subtitle = parser.parse(new FileInputStream(inputFilePath));

    assertExampleVtt(subtitle);
  }

  private void assertExampleVtt(WebVTTSubtitle subtitle) {
    Assert.assertEquals(subtitle.getRegions().size(), 2);
    Assert.assertEquals(subtitle.getStyle().size(), 1);
    Assert.assertEquals(subtitle.getCues().size(), 4);

    WebVTTSubtitleCue cue = subtitle.getCues().get(0);
    Assert.assertNotNull(cue.getCueSettingsList());
    Assert.assertNotNull(cue.getId());
    Assert.assertEquals(cue.getLines().size(), 1);
    Assert.assertEquals(cue.getStartTime(), 0);
  }

}
