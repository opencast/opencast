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
import static org.junit.Assert.assertThat;
import static org.xmlmatchers.XmlMatchers.isEquivalentTo;
import static org.xmlmatchers.transform.XmlConverters.the;

import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.util.MimeType;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

/** Test cases for {@link PublicationImpl} */
public class PublicationImplTest {

  private Publication pub;
  private Track track1;
  private Attachment attachment1;
  private Catalog catalog1;

  @Before
  public void setUpTests() throws Exception {
    pub = PublicationImpl.publication("p-1", "test", new URI(
            "http://publications.com/videos/1/index.html"), MimeType.mimeType("text", "html"));
    track1 = TrackImpl.fromURI(new URI("http://publications.com/video.mp4"));
    track1.setIdentifier("t-1");
    attachment1 = AttachmentImpl.fromURI(new URI("http://publications.com/image.png"));
    attachment1.setIdentifier("a-1");
    attachment1.setSize(100L);
    catalog1 = CatalogImpl.fromURI(new URI("http://publications.com/dublincore.xml"));
    catalog1.setIdentifier("c-1");
  }

  @Test
  public void testGetTracksInitiallyEmpty() throws Exception {
    assertEquals(0, pub.getTracks().length);
  }

  @Test
  public void testGetPreviouslyAddedTrack() throws Exception {
    pub.addTrack(track1);
    assertEquals(1, pub.getTracks().length);
    assertEquals(track1, pub.getTracks()[0]);
  }

  @Test
  public void testGetAttachmentsInitiallyEmpty() throws Exception {
    assertEquals(0, pub.getAttachments().length);
  }

  @Test
  public void testGetPreviouslyAddedAttachment() throws Exception {
    pub.addAttachment(attachment1);
    assertEquals(1, pub.getAttachments().length);
    assertEquals(attachment1, pub.getAttachments()[0]);
  }

  @Test
  public void testGetCatalogsInitiallyEmpty() throws Exception {
    assertEquals(0, pub.getCatalogs().length);
  }

  @Test
  public void testGetPreviouslyAddedCatalog() throws Exception {
    pub.addCatalog(catalog1);
    assertEquals(1, pub.getCatalogs().length);
    assertEquals(catalog1, pub.getCatalogs()[0]);
  }

  @Test
  public void testXmlSerialization() throws Exception {
    pub.addTrack(track1);
    pub.addAttachment(attachment1);
    pub.addCatalog(catalog1);

    final String xml = MediaPackageElementParser.getAsXml(pub);

    final String blueprint = IOUtils.toString(PublicationImpl.class
            .getResourceAsStream("/publication-with-elements.xml"));
    assertThat(the(xml), isEquivalentTo(the(blueprint)));
  }

  @Test
  public void testXmlDeserialization() throws Exception {
    final Publication pubFromXml = (Publication) MediaPackageElementParser.getFromXml(IOUtils
            .toString(PublicationImpl.class.getResourceAsStream("/publication-with-elements.xml")));

    assertEquals(1, pubFromXml.getCatalogs().length);
    assertEquals(1, pubFromXml.getTracks().length);
    assertEquals(1, pubFromXml.getAttachments().length);
  }

}
