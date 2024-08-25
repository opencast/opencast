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
package org.opencastproject.metadata.dublincore;

import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import java.util.Map;

/**
 * A {@link CatalogUIAdapter} converts between a concrete {@link org.opencastproject.metadata.api.MetadataCatalog}
 * implementation and a {@link DublinCoreMetadataCollection} that
 */
public interface CatalogUIAdapter {

  /**
   * Wildcard to specify tenant-agnostic catalog UI adaptors.
   */
  String ORGANIZATION_WILDCARD = "*";

  /**
   * Returns the name of the organization (tenant) this catalog UI adapter belongs to or {@value #ORGANIZATION_WILDCARD} if this is a
   * tenant-agnostic adapter.
   *
   * @return The organization name or {@value #ORGANIZATION_WILDCARD}
   */
  String getOrganization();

  /**
   * Check if the catalog UI adapter belongs to a tenant.
   *
   * @return true if adapter belongs to tenant.
   */
  boolean handlesOrganization(String organization);

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
  DublinCoreMetadataCollection getRawFields();

  /**
   * Returns all fields of this catalog in a raw data format. Allows to hand over custom queries to fill the collection
   * of a metadata field (defined by its output id) from a list provider.
   *
   * @param collectionQueryOverrides
   *          A map of custom list provider queries mapped to metadata fields by the output id. Can be empty.
   * @return The fields with raw data
   */
  DublinCoreMetadataCollection getRawFields(Map<String, ResourceListQuery> collectionQueryOverrides);

  /**
   * Returns all fields of this catalog in a raw data format. Allows to hand over custom queries to fill the collection
   * of a metadata field (defined by its output id) from a list provider.
   *
   * @param collectionQueryOverride
   *          A custom list provider query mapped to every metadata field.
   * @return The fields with raw data
   */
  DublinCoreMetadataCollection getRawFields(ResourceListQuery collectionQueryOverride);
}
