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


package org.opencastproject.mediapackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.util.MimeTypes;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test ccase for the {@link MediaPackageElementBuilderImpl}.
 */
public class MediaPackageElementBuilderTest {

  /** The media package builder */
  private MediaPackageElementBuilder mediaPackageElementBuilder = null;

  /** The test catalog */
  private URI catalogFile = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    mediaPackageElementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.MediaPackageElementBuilderImpl#elementFromURI(java.net.URI,
   *        org.opencastproject.mediapackage.MediaPackageElement.Type, MediaPackageElementFlavor)}.
   */
  @Test
  public void testElementFromFile() {
    try {
      catalogFile = getClass().getResource("/dublincore.xml").toURI();
      MediaPackageElement element = mediaPackageElementBuilder.elementFromURI(catalogFile, Type.Catalog, null);
      assertEquals(Catalog.TYPE, element.getElementType());
    } catch (UnsupportedElementException e) {
      fail(e.getMessage());
    } catch (URISyntaxException e) {
      fail(e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.MediaPackageElementBuilderImpl#newElement(
   *        org.opencastproject.mediapackage.MediaPackageElement.Type type, MediaPackageElementFlavor flavor)}
   * .
   */
  @Test
  public void testNewElement() {
    Object e = mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Catalog, MediaPackageElements.EPISODE);
    assertNotNull(e);
    assertTrue(e instanceof Catalog);
  }

  @Test
  public void testElementFromUriForEachType() throws Exception {
    URI uri = new URI("http://example.com/file");
    MediaPackageElementFlavor flavor = MediaPackageElements.PRESENTER_SOURCE;
    for (Type type : new Type[] { Type.Attachment, Type.Catalog, Type.Track }) {
      MediaPackageElement element = mediaPackageElementBuilder.elementFromURI(uri, type, flavor);
      assertEquals(type, element.getElementType());
      assertEquals(uri, element.getURI());
      assertEquals(flavor, element.getFlavor());
    }
  }

  @Test
  public void testElementFromUriDerivesMimeType() throws Exception {
    URI video = new URI("http://example.com/video.mp4");
    assertEquals(MimeTypes.fromURI(video), mediaPackageElementBuilder.elementFromURI(video, Type.Track, null)
        .getMimeType());
    URI image = new URI("http://example.com/image.png");
    assertEquals(MimeTypes.fromURI(image), mediaPackageElementBuilder.elementFromURI(image).getMimeType());
  }

  @Test
  public void testElementFromUriWithoutTypeIsAttachment() throws Exception {
    URI uri = new URI("http://example.com/file");
    assertTrue(mediaPackageElementBuilder.elementFromURI(uri) instanceof Attachment);
    assertTrue(mediaPackageElementBuilder.elementFromURI(uri, null, null) instanceof Attachment);
    assertTrue(mediaPackageElementBuilder.elementFromURI(uri, null, Attachment.FLAVOR) instanceof Attachment);
  }

  @Test
  public void testElementFromUriWithoutTypeAndOtherFlavor() throws Exception {
    URI uri = new URI("http://example.com/file");
    assertThrows(UnsupportedElementException.class,
        () -> mediaPackageElementBuilder.elementFromURI(uri, null, MediaPackageElements.PRESENTER_SOURCE));
  }

  @Test
  public void testElementFromUriWithUnsupportedType() throws Exception {
    URI uri = new URI("http://example.com/file");
    for (Type type : new Type[] { Type.Manifest, Type.Timeline, Type.Publication, Type.Other }) {
      assertThrows(UnsupportedElementException.class, () -> mediaPackageElementBuilder.elementFromURI(uri, type, null));
    }
  }

  @Test
  public void testNewElementForEachType() {
    MediaPackageElementFlavor flavor = MediaPackageElements.PRESENTER_SOURCE;
    for (Type type : new Type[] { Type.Attachment, Type.Catalog, Type.Track }) {
      MediaPackageElement element = mediaPackageElementBuilder.newElement(type, flavor);
      assertEquals(type, element.getElementType());
      assertEquals(flavor, element.getFlavor());
    }
  }

  @Test
  public void testNewElementWithUnsupportedType() {
    for (Type type : new Type[] { Type.Manifest, Type.Timeline, Type.Publication, Type.Other }) {
      assertNull(mediaPackageElementBuilder.newElement(type, MediaPackageElements.PRESENTER_SOURCE));
    }
  }

}
