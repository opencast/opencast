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

package org.opencastproject.mediapackage.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.elementbuilder.TrackBuilderPlugin;
import org.opencastproject.util.IoSupport;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;

/**
 * Test case to Test the implementation of {@link TrackImpl}.
 */
public class TrackTest {

  /** The track to test */
  private TrackImpl track = null;

  /** HTTP track url */
  private URI httpUrl = null;

  /** RTMP track url */
  private URI rtmpUrl = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    httpUrl = new URI("http://downloads.opencastproject.org/media/movie.m4v");
    rtmpUrl = new URI("rtmp://downloads.opencastproject.org/media/movie.m4v");
    track = TrackImpl.fromURI(httpUrl);
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.track.TrackImpl#fromURI(URI)}.
   */
  @Test
  public void testFromURL() {
    // track = TrackImpl.fromURL(httpUrl);
    // track = TrackImpl.fromURL(rtmpUrl);
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.track.TrackImpl#setDuration(long)}.
   */
  @Test
  public void testSetDuration() {
    track.setDuration(null);
    track.setDuration(new Long(10));
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.track.TrackImpl#getDuration()}.
   */
  @Test
  public void testGetDuration() {
    assertEquals(null, track.getDuration());
  }

  /**
   * Test method for
   * {@link PresenterTrackBuilderPlugin#accept(URI, org.opencastproject.mediapackage.MediaPackageElement.Type, org.opencastproject.mediapackage.MediaPackageElementFlavor)}
   *
   * @throws Exception
   */
  @Test
  public void testPresenterTrackAccept() throws Exception {
    assertTrue(new TrackBuilderPlugin().accept(new URI("uri"), Track.TYPE, MediaPackageElements.PRESENTER_SOURCE));
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.track.TrackImpl#getStreams()}.
   */
  @Test
  @Ignore
  public void testGetStreams() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.track.TrackImpl#addStream(org.opencastproject.mediapackage.track.AbstractStreamImpl)}
   * .
   */
  @Test
  @Ignore
  public void testAddStream() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.track.TrackImpl#getDescription()}.
   */
  @Test
  @Ignore
  public void testGetDescription() {
    fail("Not yet implemented"); // TODO
  }

  @Test
  public void testFlavorMarshalling() throws Exception {
    track.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    JAXBContext context = JAXBContext.newInstance(
            "org.opencastproject.mediapackage:org.opencastproject.mediapackage.track",
            MediaPackage.class.getClassLoader());
    Marshaller marshaller = context.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(track, writer);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    InputStream inputStream = IOUtils.toInputStream(writer.toString(), "UTF-8");
    try {
      TrackImpl t1 = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class).getValue();
      Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t1.getFlavor());
    } finally {
      IoSupport.closeQuietly(inputStream);
    }

    // Now again without namespaces
    String xml = "<oc:track xmlns:oc=\"http://mediapackage.opencastproject.org\" type=\"presentation/source\"><oc:tags/><oc:url>http://downloads.opencastproject.org/media/movie.m4v</oc:url><oc:duration>-1</oc:duration></oc:track>";
    inputStream = IOUtils.toInputStream(xml);
    try {
      TrackImpl t2 = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class).getValue();
      Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t2.getFlavor());
      Assert.assertEquals("http://downloads.opencastproject.org/media/movie.m4v", t2.getURI().toString());
    } finally {
      IoSupport.closeQuietly(inputStream);
    }

    // Get the xml from the object itself
    String xmlFromTrack = MediaPackageElementParser.getAsXml(track);
    Assert.assertTrue(xmlFromTrack.contains(MediaPackageElements.PRESENTATION_SOURCE.toString()));

    // And finally, using the element builder
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(IOUtils.toInputStream(xml));

    Track t3 = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromManifest(doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
    Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t3.getFlavor());
    Assert.assertEquals("http://downloads.opencastproject.org/media/movie.m4v", t3.getURI().toURL().toExternalForm());
  }

  @Test
  public void testLiveMarshalling() throws Exception {
    track.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    track.setLive(true);
    JAXBContext context = JAXBContext.newInstance(
            "org.opencastproject.mediapackage:org.opencastproject.mediapackage.track",
            MediaPackage.class.getClassLoader());
    Marshaller marshaller = context.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(track, writer);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    InputStream inputStream = IOUtils.toInputStream(writer.toString(), "UTF-8");
    try {
      TrackImpl t1 = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class).getValue();
      Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t1.getFlavor());
      Assert.assertEquals(true, t1.isLive());
    } finally {
      IoSupport.closeQuietly(inputStream);
    }

    // Now again without namespaces
    String xml = "<oc:track xmlns:oc=\"http://mediapackage.opencastproject.org\" type=\"presentation/source\"><oc:tags/><oc:url>http://downloads.opencastproject.org/media/movie.m4v</oc:url><oc:duration>-1</oc:duration><oc:live>true</oc:live></oc:track>";
    inputStream = IOUtils.toInputStream(xml);
    try {
      TrackImpl t2 = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class).getValue();
      Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t2.getFlavor());
      Assert.assertEquals("http://downloads.opencastproject.org/media/movie.m4v", t2.getURI().toString());
      Assert.assertEquals(true, t2.isLive());
    } finally {
      IoSupport.closeQuietly(inputStream);
    }

    // Get the xml from the object itself
    String xmlFromTrack = MediaPackageElementParser.getAsXml(track);
    Assert.assertTrue(xmlFromTrack.contains(MediaPackageElements.PRESENTATION_SOURCE.toString()));
    Assert.assertTrue(xmlFromTrack.replaceAll("\\b+", "").contains("<live>true</live>"));

    // And finally, using the element builder
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(IOUtils.toInputStream(xml));

    Track t3 = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromManifest(doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
    Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t3.getFlavor());
    Assert.assertEquals("http://downloads.opencastproject.org/media/movie.m4v", t3.getURI().toURL().toExternalForm());
    Assert.assertEquals(true, t3.isLive());
  }

}
