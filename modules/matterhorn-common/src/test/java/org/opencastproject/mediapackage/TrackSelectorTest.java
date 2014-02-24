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

import org.opencastproject.mediapackage.selector.TrackSelector;

import org.junit.Before;
import org.junit.Test;

/**
 * Test case for {@link TrackSelector}.
 */
public class TrackSelectorTest extends SimpleElementSelectorTest {

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    selector = new TrackSelector();
    selector.addFlavor(PRESENTATION_SOURCE);
    selector.addFlavor(PRESENTER_SOURCE);
    setUpPreliminaries();
  }

  /**
   * Test method for
   * {@link org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#select(org.opencastproject.mediapackage.MediaPackage, boolean)}
   * .
   */
  @Test
  @Override
  public void testSelect() {
    assertEquals(2, selector.select(mediaPackage, true).size());
  }

  @Test
  public void testOnlyFlavorOrSelect() {
    assertEquals(2, selector.select(mediaPackage, false).size());
  }

  @Test
  public void testOnlyTagsAndSelect() {
    TrackSelector tagsSelector = new TrackSelector();
    tagsSelector.addTag(tag);
    assertEquals(1, tagsSelector.select(mediaPackage, true).size());
  }

  @Test
  public void testOnlyTagsOrSelect() {
    TrackSelector tagsSelector = new TrackSelector();
    tagsSelector.addTag(tag);
    assertEquals(1, tagsSelector.select(mediaPackage, false).size());
  }

  @Test
  public void testTagsOrFlavorSelect() {
    TrackSelector tagsSelector = new TrackSelector();
    tagsSelector.addTag(tag);
    tagsSelector.addFlavor(PRESENTATION_SOURCE);
    assertEquals(2, tagsSelector.select(mediaPackage, false).size());
  }

  @Test
  public void testTagsOrFlavorSelect2() {
    TrackSelector tagsSelector = new TrackSelector();
    tagsSelector.addTag(tag);
    tagsSelector.addFlavor(AUDIENCE_SOURCE);
    assertEquals(2, tagsSelector.select(mediaPackage, false).size());
  }

  @Test
  public void testTagsAndFlavorSelect() {
    TrackSelector tagsSelector = new TrackSelector();
    tagsSelector.addTag(tag);
    tagsSelector.addFlavor(PRESENTATION_SOURCE);
    assertEquals(1, tagsSelector.select(mediaPackage, true).size());
  }

  @Test
  public void testNoTagsOrFlavorSelect() {
    assertEquals(3, new TrackSelector().select(mediaPackage, false).size());
  }

  @Test
  public void testNoTagsAndFlavorSelect() {
    assertEquals(3, new TrackSelector().select(mediaPackage, true).size());
  }

}
