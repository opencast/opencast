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
package org.opencastproject.mediapackage;

import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;

import junit.framework.Assert;

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

      Assert.assertEquals(0, mediaPackage.getTracks().length);
      Assert.assertEquals(1, mediaPackage.getCatalogs().length);
      Assert.assertEquals(0, mediaPackage.getAttachments().length);

      Assert.assertEquals("dublincore/episode", mediaPackage.getCatalogs()[0].getFlavor().toString());
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
    Assert.assertNotNull(xml);

    // Deserialize the media package
    MediaPackage deserialized = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(IOUtils.toInputStream(xml, "UTF-8"));

    // Ensure that the deserialized mediapackage is correct
    Assert.assertEquals(2, deserialized.getCatalogs().length);
    Assert.assertEquals(1, deserialized.getAttachments().length);
    Assert.assertEquals(1, deserialized.getTracks().length);
    Assert.assertEquals(2, deserialized.getTracks()[0].getStreams().length);
    Assert.assertEquals(1, deserialized.getCatalogs(new MediaPackageReferenceImpl(cat1)).length);
  }

  /**
   * JAXB produces xml with an xsi:type="" attribute on the root element. Be sure that we can unmarshall objects without
   * that attribute.
   */
  @Test
  public void testJaxbWithoutXsi() throws Exception {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns2:mediapackage start=\"0\" id=\"123\" duration=\"0\" xmlns:ns2=\"http://mediapackage.opencastproject.org\"><metadata><catalog type=\"dublincore/episode\"><mimetype>text/xml</mimetype><tags/><checksum type=\"md5\">7891011abcd</checksum><url>http://opencastproject.org/index.html</url></catalog></metadata><attachments><attachment id=\"attachment-1\"><tags/><checksum type=\"md5\">123456abcd</checksum><url>http://opencastproject.org/index.html</url></attachment></attachments></ns2:mediapackage>";
    MediaPackage deserialized = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(IOUtils.toInputStream(xml, "UTF-8"));
    Assert.assertEquals(2, deserialized.getElements().length);

    String elementXml = "<track id=\"track-1\" type=\"presentation/source\"><mimetype>video/mpeg</mimetype>"
            + "<url>http://localhost:8080/workflow/samples/screen.mpg</url></track>";
    MediaPackageElement element = MediaPackageElementParser.getFromXml(elementXml);
    Assert.assertEquals("track-1", element.getIdentifier());
    Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, element.getFlavor());
    Assert.assertEquals("http://localhost:8080/workflow/samples/screen.mpg", element.getURI().toString());
  }

  @Test
  public void testJaxbUnmarshallingFromFile() throws Exception {
    InputStream in = null;
    try {
      in = this.getClass().getResourceAsStream("/manifest.xml");
      MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(in);
      Assert.assertEquals(2, mp.getTracks().length);
      Assert.assertTrue(mp.getTracks()[0].hasVideo());
      Assert.assertTrue(!mp.getTracks()[0].hasAudio());
      Assert.assertTrue(mp.getTracks()[1].hasAudio());
      Assert.assertTrue(!mp.getTracks()[1].hasVideo());
      Assert.assertEquals(3, mp.getCatalogs().length);
      Assert.assertEquals(2, mp.getAttachments().length);
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
    Assert.assertTrue(xml.indexOf(title) > 0);
    MediaPackage unmarshalled = builder.loadFromXml(xml);
    Assert.assertEquals(title, unmarshalled.getTitle());
    Assert.assertEquals("s1", unmarshalled.getSeriesTitle());
  }
}
