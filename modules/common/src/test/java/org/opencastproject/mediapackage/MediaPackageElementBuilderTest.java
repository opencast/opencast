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
import static org.junit.Assert.fail;

import org.opencastproject.mediapackage.MediaPackageElement.Type;

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
   * Test method for {@link org.opencastproject.mediapackage.MediaPackageElementBuilderImpl#elementFromURI(java.net.URI, org.opencastproject.mediapackage.MediaPackageElement.Type, MediaPackageElementFlavor)}.
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
   * {@link org.opencastproject.mediapackage.MediaPackageElementBuilderImpl#newElement(org.opencastproject.mediapackage.MediaPackageElement.Type type, MediaPackageElementFlavor flavor)}
   * .
   */
  @Test
  public void testNewElement() {
    Object e = mediaPackageElementBuilder.newElement(MediaPackageElement.Type.Catalog, MediaPackageElements.EPISODE);
    assertNotNull(e);
    assertTrue(e instanceof Catalog);
  }

}
