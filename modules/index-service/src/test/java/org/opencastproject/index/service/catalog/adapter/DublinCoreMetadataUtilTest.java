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

package org.opencastproject.index.service.catalog.adapter;

import static org.junit.Assert.assertEquals;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.MetadataField;

import org.junit.Test;

public class DublinCoreMetadataUtilTest {

  private static final String EXISTING_TITLE = "An existing title";

  /**
   * Builds an untouched text field, i.e. one carrying the blank default value a raw catalog field has before
   * anything was submitted for it.
   */
  private static MetadataField rawTextField(String inputID, boolean required) {
    return new MetadataField(inputID, null, "LABEL." + inputID, false, required, "", null, MetadataField.Type.TEXT,
            null, null, 0, null, null, null, null);
  }

  private static DublinCoreCatalog catalogWithTitle() {
    final DublinCoreCatalog dc = DublinCores.mkSimple();
    dc.set(DublinCore.PROPERTY_TITLE, EXISTING_TITLE);
    return dc;
  }

  /**
   * A partial update only carries the fields the caller actually changed. Required fields that were not submitted
   * keep their blank default value in the collection, but the catalog still holds them, so the update must go
   * through. See the admin UI, which only sends changed fields to /admin-ng/series/{id}/metadata.
   */
  @Test
  public void testPartialUpdateLeavesUntouchedRequiredFieldAlone() {
    final DublinCoreCatalog dc = catalogWithTitle();

    final DublinCoreMetadataCollection metadata = new DublinCoreMetadataCollection();
    metadata.addField(rawTextField("title", true));
    final MetadataField description = rawTextField("description", false);
    description.setValue("A new description");
    metadata.addField(description);

    DublinCoreMetadataUtil.updateDublincoreCatalog(dc, metadata);

    assertEquals(EXISTING_TITLE, dc.getFirst(DublinCore.PROPERTY_TITLE));
    assertEquals("A new description", dc.getFirst(DublinCore.PROPERTY_DESCRIPTION));
  }

  /**
   * Blanking a required field is an explicit request to clear it and must still be rejected, even though the
   * catalog currently holds a value for it.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testClearingRequiredFieldExpectsIllegalArgumentException() {
    final DublinCoreCatalog dc = catalogWithTitle();

    final DublinCoreMetadataCollection metadata = new DublinCoreMetadataCollection();
    final MetadataField title = rawTextField("title", true);
    title.setValue("");
    metadata.addField(title);

    DublinCoreMetadataUtil.updateDublincoreCatalog(dc, metadata);
  }

  /**
   * Writing into a catalog that does not hold the required field either must be rejected, which is what keeps
   * creation without a title from silently succeeding.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testMissingRequiredFieldOnEmptyCatalogExpectsIllegalArgumentException() {
    final DublinCoreCatalog dc = DublinCores.mkSimple();

    final DublinCoreMetadataCollection metadata = new DublinCoreMetadataCollection();
    metadata.addField(rawTextField("title", true));

    DublinCoreMetadataUtil.updateDublincoreCatalog(dc, metadata);
  }
}
