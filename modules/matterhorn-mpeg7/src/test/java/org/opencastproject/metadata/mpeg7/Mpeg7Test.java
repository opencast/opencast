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


package org.opencastproject.metadata.mpeg7;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.util.FileSupport;

import de.schlichtherle.io.FileOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.Rectangle;
import java.io.File;
import java.io.InputStream;
import java.net.URI;

/**
 * Test class for the Mpeg-7 catalog implementation.
 */
public class Mpeg7Test {

  /** The catalog name */
  private String catalogName = "/mpeg7.xml";

  /** The test catalog */
  private File catalogFile = null;

  /** Temp file for mpeg7 catalog contents */
  private File mpeg7TempFile = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    catalogFile = new File(this.getClass().getResource(catalogName).toURI());
    if (!catalogFile.exists() || !catalogFile.canRead())
      throw new Exception("Unable to access mpeg-7 test catalog '" + catalogName + "'");
  }

  /**
   * @throws java.io.File.IOException
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(mpeg7TempFile);
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.mpeg7.Mpeg7CatalogImpl#fromFile(java.io.File)} .
   */
  @Test
  public void testFromFile() throws Exception {
    Mpeg7Catalog mpeg7 = new Mpeg7CatalogImpl(catalogFile.toURI().toURL().openStream());
    testContent(mpeg7);
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.mpeg7.Mpeg7CatalogImpl#save()}.
   */
  @Test
  public void testNewInstance() throws Exception {
    // Read the sample catalog
    Mpeg7Catalog mpeg7Sample = new Mpeg7CatalogImpl(catalogFile.toURI().toURL().openStream());

    // Create a new catalog and fill it with a few fields
    mpeg7TempFile = new File(FileSupport.getTempDirectory(), Long.toString(System.currentTimeMillis()));

    // TODO: Add sample tracks to new catalog
    // TODO: Add sample video segments to new catalog
    // TODO: Add sample annotations to new catalog

    // Store the mpeg7 catalog contents in the temp file
    Mpeg7CatalogService mpeg7Service = new Mpeg7CatalogService();
    InputStream in = mpeg7Service.serialize(mpeg7Sample);
    FileOutputStream out = new FileOutputStream(mpeg7TempFile);
    IOUtils.copy(in, out);

    // Re-read the saved catalog and test for its content
    Mpeg7Catalog mpeg7NewFromDisk = new Mpeg7CatalogImpl(mpeg7TempFile.toURI().toURL().openStream());

    // TODO: Test content
    testContent(mpeg7NewFromDisk);
  }

  /**
   * Tests the contents of the sample catalog mpeg7.xml.
   */
  @SuppressWarnings("unchecked")
  protected void testContent(Mpeg7Catalog mpeg7) {
    // Check presence of content
    assertTrue(mpeg7.hasAudioContent());
    assertTrue(mpeg7.hasVideoContent());
    assertFalse(mpeg7.hasAudioVisualContent());

    // Check content size
    assertTrue(mpeg7.getMultimediaContent(MultimediaContent.Type.AudioType).size() == 1);
    assertTrue(mpeg7.getMultimediaContent(MultimediaContent.Type.VideoType).size() == 2);

    // Check tracks
    assertNotNull(mpeg7.getAudioById("track-1"));
    assertNotNull(mpeg7.getVideoById("track-2"));
    assertNotNull(mpeg7.getVideoById("track-3"));

    //
    // Check audio track (track-1)
    //

    MultimediaContentType track1 = mpeg7.getAudioById("track-1");
    MediaTime audioMediaTime = track1.getMediaTime();

    // Media locator
    assertEquals(track1.getMediaLocator().getMediaURI(), URI.create("file:tracks/audio.pcm"));
    // Media time point
    assertEquals(0, audioMediaTime.getMediaTimePoint().getDay());
    assertEquals(0, audioMediaTime.getMediaTimePoint().getHour());
    assertEquals(0, audioMediaTime.getMediaTimePoint().getMinutes());
    assertEquals(0, audioMediaTime.getMediaTimePoint().getSeconds());
    assertEquals(25, audioMediaTime.getMediaTimePoint().getFractionsPerSecond());
    assertEquals(0, audioMediaTime.getMediaTimePoint().getNFractions());
    // Media duration
    assertEquals(0, audioMediaTime.getMediaDuration().getDays());
    assertEquals(1, audioMediaTime.getMediaDuration().getHours());
    assertEquals(30, audioMediaTime.getMediaDuration().getMinutes());
    assertEquals(0, audioMediaTime.getMediaDuration().getSeconds());
    // Segments
    assertFalse(track1.getTemporalDecomposition().segments().hasNext());

    //
    // Check video track (track-2)
    //

    MultimediaContentType track2 = mpeg7.getVideoById("track-2");
    MediaTime v1MediaTime = track2.getMediaTime();

    // Media locator
    assertEquals(track2.getMediaLocator().getMediaURI(), URI.create("file:tracks/presentation.mp4"));
    // Media time point
    assertEquals(0, v1MediaTime.getMediaTimePoint().getDay());
    assertEquals(0, v1MediaTime.getMediaTimePoint().getHour());
    assertEquals(0, v1MediaTime.getMediaTimePoint().getMinutes());
    assertEquals(0, v1MediaTime.getMediaTimePoint().getSeconds());
    assertEquals(25, v1MediaTime.getMediaTimePoint().getFractionsPerSecond());
    assertEquals(0, v1MediaTime.getMediaTimePoint().getNFractions());
    // Media duration
    assertEquals(0, v1MediaTime.getMediaDuration().getDays());
    assertEquals(1, v1MediaTime.getMediaDuration().getHours());
    assertEquals(30, v1MediaTime.getMediaDuration().getMinutes());
    assertEquals(0, v1MediaTime.getMediaDuration().getSeconds());
    // Segments
    TemporalDecomposition<VideoSegment> v1Decomposition = (TemporalDecomposition<VideoSegment>) track2
            .getTemporalDecomposition();
    assertFalse(v1Decomposition.hasGap());
    assertFalse(v1Decomposition.isOverlapping());
    assertEquals(v1Decomposition.getCriteria(), TemporalDecomposition.DecompositionCriteria.Temporal);
    assertTrue(v1Decomposition.segments().hasNext());
    // Segment track-2.segment-1
    VideoSegment v1Segment1 = v1Decomposition.getSegmentById("track-2.segment-1");
    assertNotNull(v1Segment1);
    MediaTime segment1MediaTime = v1Segment1.getMediaTime();
    // Media time point
    assertEquals(0, segment1MediaTime.getMediaTimePoint().getDay());
    assertEquals(0, segment1MediaTime.getMediaTimePoint().getHour());
    assertEquals(0, segment1MediaTime.getMediaTimePoint().getMinutes());
    assertEquals(0, segment1MediaTime.getMediaTimePoint().getSeconds());
    assertEquals(25, segment1MediaTime.getMediaTimePoint().getFractionsPerSecond());
    assertEquals(0, segment1MediaTime.getMediaTimePoint().getNFractions());
    // Media duration
    assertEquals(0, segment1MediaTime.getMediaDuration().getDays());
    assertEquals(1, segment1MediaTime.getMediaDuration().getHours());
    assertEquals(7, segment1MediaTime.getMediaDuration().getMinutes());
    assertEquals(35, segment1MediaTime.getMediaDuration().getSeconds());
    // Text annotations
    assertTrue(v1Segment1.hasTextAnnotations());
    assertTrue(v1Segment1.hasTextAnnotations(0.4f, 0.5f));
    assertFalse(v1Segment1.hasTextAnnotations(0.8f, 0.8f));
    assertTrue(v1Segment1.hasTextAnnotations("de"));
    assertFalse(v1Segment1.hasTextAnnotations("fr"));
    // Keywords
    TextAnnotation textAnnotation = v1Segment1.textAnnotations().next();
    assertEquals("Armin", textAnnotation.keywordAnnotations().next().getKeyword());
    assertEquals("Hint Armin", textAnnotation.freeTextAnnotations().next().getText());
    // Spaciotemporal decomposition
    SpatioTemporalDecomposition stdecomposition = v1Segment1.getSpatioTemporalDecomposition();
    assertNotNull(stdecomposition);
    assertTrue(stdecomposition.hasGap());
    assertFalse(stdecomposition.isOverlapping());
    // VideoText
    assertEquals(1, stdecomposition.getVideoText().length);
    VideoText videoText = stdecomposition.getVideoText("text1");
    assertNotNull(videoText);
    SpatioTemporalLocator locator = videoText.getSpatioTemporalLocator();
    assertNotNull(locator);
    MediaTime locatorMediaTime = locator.getMediaTime();
    assertNotNull(locatorMediaTime);
    assertEquals(MediaRelTimePointImpl.parseTimePoint("T00:00:00:0F25"), locatorMediaTime.getMediaTimePoint());
    assertEquals(MediaDurationImpl.parseDuration("PT01H07M35S"), locatorMediaTime.getMediaDuration());
    Textual textual = videoText.getText();
    assertNotNull(textual);
    assertEquals("Text", textual.getText());
    assertEquals("en", textual.getLanguage());
    Rectangle boundingBox = videoText.getBoundary();
    assertNotNull(boundingBox);
    assertEquals(10, (int) boundingBox.getX());
    assertEquals(150, (int) boundingBox.getWidth());
    assertEquals(20, (int) boundingBox.getY());
    assertEquals(15, (int) boundingBox.getHeight());

    //
    // Check video track (track-3)
    //

    MultimediaContentType track3 = mpeg7.getVideoById("track-3");
    MediaTime v2MediaTime = track3.getMediaTime();

    // Media locator
    assertEquals(track3.getMediaLocator().getMediaURI(), URI.create("file:tracks/presenter.mpg"));
    // Media time point
    assertEquals(0, v2MediaTime.getMediaTimePoint().getDay());
    assertEquals(0, v2MediaTime.getMediaTimePoint().getHour());
    assertEquals(0, v2MediaTime.getMediaTimePoint().getMinutes());
    assertEquals(0, v2MediaTime.getMediaTimePoint().getSeconds());
    assertEquals(25, v2MediaTime.getMediaTimePoint().getFractionsPerSecond());
    assertEquals(0, v2MediaTime.getMediaTimePoint().getNFractions());
    // Media duration
    assertEquals(0, v2MediaTime.getMediaDuration().getDays());
    assertEquals(1, v2MediaTime.getMediaDuration().getHours());
    assertEquals(30, v2MediaTime.getMediaDuration().getMinutes());
    assertEquals(0, v2MediaTime.getMediaDuration().getSeconds());
    // Segments
    TemporalDecomposition<VideoSegment> v2Decomposition = (TemporalDecomposition<VideoSegment>) track3
            .getTemporalDecomposition();
    assertFalse(v2Decomposition.segments().hasNext());

  }

}
