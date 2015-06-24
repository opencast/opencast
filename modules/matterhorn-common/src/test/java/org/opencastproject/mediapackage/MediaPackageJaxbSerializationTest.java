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

package org.opencastproject.mediapackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class MediaPackageJaxbSerializationTest {

  @Test
  public void testManifestSerialization() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = getClass().getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/manifest-simple.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);

      assertEquals(0, mediaPackage.getTracks().length);
      assertEquals(1, mediaPackage.getCatalogs().length);
      assertEquals(0, mediaPackage.getAttachments().length);

      assertEquals("dublincore/episode", mediaPackage.getCatalogs()[0].getFlavor().toString());
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @Test
  public void testJaxbSerialization() throws Exception {
    // Build a media package
    MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackage mp = new MediaPackageImpl(new IdImpl("123"));
    Attachment attachment = (Attachment) elementBuilder.elementFromURI(
            new URI("http://opencastproject.org/index.html"), Type.Attachment, Attachment.FLAVOR);
    attachment.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, "123456abcd"));
    mp.add(attachment);
    Catalog cat1 = (Catalog) elementBuilder.elementFromURI(new URI("http://opencastproject.org/index.html"),
            Catalog.TYPE, MediaPackageElements.EPISODE);
    cat1.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, "7891011abcd"));
    mp.add(cat1);

    Catalog cat2 = (Catalog) elementBuilder.elementFromURI(new URI("http://opencastproject.org/index.html"),
            Catalog.TYPE, MediaPackageElements.EPISODE);
    cat2.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, "7891011abcd"));
    mp.addDerived(cat2, cat1);

    TrackImpl track = (TrackImpl) elementBuilder.elementFromURI(new URI("http://opencastproject.org/video.mpg"),
            Track.TYPE, MediaPackageElements.PRESENTER_SOURCE);
    track.addStream(new VideoStreamImpl("video-stream-1"));
    track.addStream(new VideoStreamImpl("video-stream-2"));
    mp.add(track);

    // Serialize the media package
    String xml = MediaPackageParser.getAsXml(mp);
    assertNotNull(xml);

    // Serialize the media package as JSON
    String json = MediaPackageParser.getAsJSON(mp);
    assertNotNull(json);

    // Deserialize the media package
    MediaPackage deserialized = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(IOUtils.toInputStream(xml, "UTF-8"));

    // Ensure that the deserialized mediapackage is correct
    assertEquals(2, deserialized.getCatalogs().length);
    assertEquals(1, deserialized.getAttachments().length);
    assertEquals(1, deserialized.getTracks().length);
    assertEquals(2, deserialized.getTracks()[0].getStreams().length);
    assertEquals(1, deserialized.getCatalogs(new MediaPackageReferenceImpl(cat1)).length);
  }

  /**
   * JAXB produces xml with an xsi:type="" attribute on the root element. Be sure that we can unmarshall objects without
   * that attribute.
   */
  @Test
  public void testJaxbWithoutXsi() throws Exception {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mediapackage start=\"0\" id=\"123\" duration=\"0\" xmlns=\"http://mediapackage.opencastproject.org\"><metadata><catalog type=\"dublincore/episode\"><mimetype>text/xml</mimetype><tags/><checksum type=\"md5\">7891011abcd</checksum><url>http://opencastproject.org/index.html</url></catalog></metadata><attachments><attachment id=\"attachment-1\"><tags/><checksum type=\"md5\">123456abcd</checksum><url>http://opencastproject.org/index.html</url></attachment></attachments></mediapackage>";
    MediaPackage deserialized = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(IOUtils.toInputStream(xml, "UTF-8"));
    assertEquals(2, deserialized.getElements().length);

    String elementXml = "<track id=\"track-1\" type=\"presentation/source\"><mimetype>video/mpeg</mimetype>"
            + "<url>http://localhost:8080/workflow/samples/screen.mpg</url></track>";
    MediaPackageElement element = MediaPackageElementParser.getFromXml(elementXml);
    assertEquals("track-1", element.getIdentifier());
    assertEquals(MediaPackageElements.PRESENTATION_SOURCE, element.getFlavor());
    assertEquals("http://localhost:8080/workflow/samples/screen.mpg", element.getURI().toString());
  }

  @Test
  public void testJaxbUnmarshallingFromFile() throws Exception {
    InputStream in = null;
    try {
      in = this.getClass().getResourceAsStream("/manifest.xml");
      MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(in);
      assertEquals(2, mp.getTracks().length);
      assertTrue(mp.getTracks()[0].hasVideo());
      assertTrue(!mp.getTracks()[0].hasAudio());
      assertTrue(mp.getTracks()[1].hasAudio());
      assertTrue(!mp.getTracks()[1].hasVideo());
      assertEquals(3, mp.getCatalogs().length);
      assertEquals(2, mp.getAttachments().length);
      assertEquals(1, mp.getPublications().length);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Test
  public void testUmlaut() throws Exception {
    String title = "Ökologie";
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    MediaPackage original = builder.createNew();
    original.setTitle(title);
    original.setSeriesTitle("s1");
    String xml = MediaPackageParser.getAsXml(original);
    assertTrue(xml.indexOf(title) > 0);
    MediaPackage unmarshalled = builder.loadFromXml(xml);
    assertEquals(title, unmarshalled.getTitle());
    assertEquals("s1", unmarshalled.getSeriesTitle());
  }
}
