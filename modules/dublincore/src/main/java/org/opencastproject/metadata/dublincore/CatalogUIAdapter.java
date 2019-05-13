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

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

/**
 * A {@link CatalogUIAdapter} converts between a concrete {@link org.opencastproject.metadata.api.MetadataCatalog}
 * implementation and a {@link MetadataCollection} that
 */
public interface CatalogUIAdapter {

  /**
   * Returns the name of the organization (tenant) this catalog UI adapter belongs to or {@code Opt.none()} if this is a
   * tenant-agnostic adapter.
   *
   * @return The organization name or {@code Opt.none()}
   */
  String getOrganization();

  /**
   * Returns the media type of catalogs managed by this catalog UI adapter.
   *
   * @return The media type of the catalog
   */
  MediaPackageElementFlavor getFlavor();

  /**
   * @return Get the human readable title for this catalog ui adapter for various languages.
   */
  String getUITitle();

  /**
   * Returns all fields of this catalog in a raw data format. This is a good starting point to create a new instance of
   * this catalog.
   *
   * @return The fields with raw data
   */
  MetadataCollection getRawFields();

}
