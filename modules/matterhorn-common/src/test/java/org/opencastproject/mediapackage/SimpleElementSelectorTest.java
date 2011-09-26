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

import static org.junit.Assert.assertEquals;
import static org.opencastproject.mediapackage.MediaPackageElements.AUDIENCE_SOURCE;
import static org.opencastproject.mediapackage.MediaPackageElements.PRESENTATION_SOURCE;
import static org.opencastproject.mediapackage.MediaPackageElements.PRESENTER_SOURCE;

import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Test case for {@link SimpleElementSelector}.
 */
public class SimpleElementSelectorTest {

  /** The selector to be tested */
  protected AbstractMediaPackageElementSelector<? extends MediaPackageElement> selector = null;

  /** The media package element builder */
  protected final MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance()
          .newElementBuilder();

  /** The media package */
  protected MediaPackage mediaPackage = null;

  /** The presentation track */
  protected Track presentationTrack = null;

  /** The presenter track */
  protected Track presenterTrack = null;

  /** The audience track */
  protected Track audienceTrack = null;

  /** The presentation attachment */
  protected Attachment presentationAttachment = null;

  /** Track uri (needed but irrelevant for this test) */
  protected URI uri = null;

  /** A tag */
  protected String tag = "tag";

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    selector = new SimpleElementSelector();
    selector.addFlavor(PRESENTATION_SOURCE);
    selector.addFlavor(PRESENTER_SOURCE);
    setUpPreliminaries();
  }

  protected void setUpPreliminaries() throws Exception {
    mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    uri = new URI("http://localhost/track.mov");
    presentationTrack = (Track) elementBuilder.elementFromURI(uri, Type.Track, PRESENTATION_SOURCE);
    presentationTrack.addTag(tag);
    mediaPackage.add(presentationTrack);
    presenterTrack = (Track) elementBuilder.elementFromURI(uri, Type.Track, PRESENTATION_SOURCE);
    mediaPackage.add(presenterTrack);
    audienceTrack = (Track) elementBuilder.elementFromURI(uri, Type.Track, AUDIENCE_SOURCE);
    mediaPackage.add(audienceTrack);
    presentationAttachment = (Attachment) elementBuilder.elementFromURI(uri, Type.Attachment, PRESENTATION_SOURCE);
    mediaPackage.add(presentationAttachment);
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#select(org.opencastproject.mediapackage.MediaPackage)}
   * .
   */
  @Test
  public void testSelect() {
    assertEquals(3, selector.select(mediaPackage).size());
    selector.addTag(tag);
    assertEquals(1, selector.select(mediaPackage).size());
    assertEquals(presentationTrack, selector.select(mediaPackage).iterator().next());
    selector.addTag("abc");
    assertEquals(1, selector.select(mediaPackage).size());
    assertEquals(presentationTrack, selector.select(mediaPackage).iterator().next());
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#setFlavors(java.util.List)}.
   */
  @Test
  public void testSetFlavors() {
    List<MediaPackageElementFlavor> flavors = new ArrayList<MediaPackageElementFlavor>();
    flavors.add(AUDIENCE_SOURCE);
    selector.setFlavors(flavors);
    assertEquals(1, selector.getFlavors().length);
    assertEquals(AUDIENCE_SOURCE, selector.getFlavors()[0]);
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#addFlavor(org.opencastproject.mediapackage.MediaPackageElementFlavor)}
   * .
   */
  @Test
  public void testAddFlavorMediaPackageElementFlavor() {
    assertEquals(2, selector.getFlavors().length);
    selector.addFlavor(PRESENTATION_SOURCE);
    assertEquals(2, selector.getFlavors().length);
    selector.addFlavor(AUDIENCE_SOURCE);
    assertEquals(3, selector.getFlavors().length);
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#addFlavor(java.lang.String)}.
   */
  @Test
  public void testAddFlavorString() {
    assertEquals(2, selector.getFlavors().length);
    selector.addFlavor(PRESENTATION_SOURCE.toString());
    assertEquals(2, selector.getFlavors().length);
    selector.addFlavor(AUDIENCE_SOURCE.toString());
    assertEquals(3, selector.getFlavors().length);
    assertEquals(AUDIENCE_SOURCE, selector.getFlavors()[2]);
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#addFlavorAt(int, org.opencastproject.mediapackage.MediaPackageElementFlavor)}
   * .
   */
  @Test
  public void testAddFlavorAtIntMediaPackageElementFlavor() {
    // move a flavor up
    selector.addFlavorAt(0, PRESENTER_SOURCE);
    assertEquals(2, selector.getFlavors().length);
    assertEquals(PRESENTER_SOURCE, selector.getFlavors()[0]);
    assertEquals(PRESENTATION_SOURCE, selector.getFlavors()[1]);
    // add a new flavor
    selector.addFlavorAt(1, AUDIENCE_SOURCE);
    assertEquals(3, selector.getFlavors().length);
    assertEquals(PRESENTER_SOURCE, selector.getFlavors()[0]);
    assertEquals(AUDIENCE_SOURCE, selector.getFlavors()[1]);
    assertEquals(PRESENTATION_SOURCE, selector.getFlavors()[2]);
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#addFlavorAt(int, java.lang.String)}
   * .
   */
  @Test
  public void testAddFlavorAtIntString() {
    // move a flavor up
    selector.addFlavorAt(0, PRESENTER_SOURCE.toString());
    assertEquals(2, selector.getFlavors().length);
    assertEquals(PRESENTER_SOURCE, selector.getFlavors()[0]);
    assertEquals(PRESENTATION_SOURCE, selector.getFlavors()[1]);
    // add a new flavor
    selector.addFlavorAt(1, AUDIENCE_SOURCE.toString());
    assertEquals(3, selector.getFlavors().length);
    assertEquals(PRESENTER_SOURCE, selector.getFlavors()[0]);
    assertEquals(AUDIENCE_SOURCE, selector.getFlavors()[1]);
    assertEquals(PRESENTATION_SOURCE, selector.getFlavors()[2]);
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#removeFlavor(org.opencastproject.mediapackage.MediaPackageElementFlavor)}
   * .
   */
  @Test
  public void testRemoveFlavorMediaPackageElementFlavor() {
    selector.removeFlavor(PRESENTATION_SOURCE);
    assertEquals(1, selector.getFlavors().length);
    assertEquals(PRESENTER_SOURCE, selector.getFlavors()[0]);
    selector.removeFlavor(AUDIENCE_SOURCE);
    assertEquals(1, selector.getFlavors().length);
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#removeFlavor(java.lang.String)}
   * .
   */
  @Test
  public void testRemoveFlavorString() {
    selector.removeFlavor(PRESENTATION_SOURCE.toString());
    assertEquals(1, selector.getFlavors().length);
    assertEquals(PRESENTER_SOURCE, selector.getFlavors()[0]);
    selector.removeFlavor(AUDIENCE_SOURCE.toString());
    assertEquals(1, selector.getFlavors().length);
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#removeFlavorAt(int)}.
   */
  @Test
  public void testRemoveFlavorAt() {
    selector.removeFlavorAt(0);
    assertEquals(1, selector.getFlavors().length);
    assertEquals(PRESENTER_SOURCE, selector.getFlavors()[0]);
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#addTag(java.lang.String)}.
   */
  @Test
  public void testAddTag() {
    selector.addTag(tag);
    assertEquals(1, selector.getTags().length);
    selector.addTag("abc");
    assertEquals(2, selector.getTags().length);
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#removeTag(java.lang.String)}.
   */
  @Test
  public void testRemoveTag() {
    selector.addTag(tag);
    selector.removeTag("abc");
    assertEquals(1, selector.getTags().length);
    selector.removeTag(tag);
    assertEquals(0, selector.getTags().length);
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#clearTags()}.
   */
  @Test
  public void testClearTags() {
    selector.clearTags();
    assertEquals(0, selector.getTags().length);
  }

}
