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
package org.opencastproject.metadata.dublincore;

import com.entwinemedia.fn.data.Opt;

/**
 * A {@link SeriesCatalogUIAdapter} converts between a concrete {@link org.opencastproject.metadata.api.MetadataCatalog}
 * implementation and a {@link MetadataCollection}
 */
public interface SeriesCatalogUIAdapter extends CatalogUIAdapter {

  /**
   * Returns all fields of this catalog containing the data in an abstract, editable form. If the series cannot be
   * found, {@code Opt.none()} is returned.
   *
   * @param seriesId
   *          The series identifer
   * @return Get the field names and values for this catalog.
   */
  Opt<MetadataCollection> getFields(String seriesId);

  /**
   * Store changes made to the fields of the metadata collection in the catalog and return if successful.
   *
   * @param seriesId
   *          The series identifier
   * @param metadata
   *          The new metadata to update the media package with
   * @return true, if the metadata could be saved successfully
   */
  boolean storeFields(String seriesId, MetadataCollection metadata);
}
