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
package org.opencastproject.index.service.catalog.adapter.series;

import com.entwinemedia.fn.data.Opt;
import org.opencastproject.index.service.catalog.adapter.AbstractMetadataCollection;

/**
 * A {@link SeriesCatalogUIAdapter} converts between a concrete {@link MetadataCatalog} implementation and a
 * {@link AbstractMetadataCollection} that
 */
public interface SeriesCatalogUIAdapter {

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
  String getFlavor();

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
  AbstractMetadataCollection getRawFields();

  /**
   * Returns all fields of this catalog containing the data in an abstract, editable form. If the series cannot be
   * found, {@code Opt.none()} is returned.
   * 
   * @param seriesId
   *          The series identifer
   * @return Get the field names and values for this catalog.
   */
  Opt<AbstractMetadataCollection> getFields(String seriesId);

  /**
   * Store changes made to the fields of the metadata collection in the catalog and return an updated version of it.
   *
   * @param seriesId
   *          The series identifier
   * @param metadata
   *          The new metadata to update the mediapackage with
   * @return true, if the metadata could be saved successfully
   */
  boolean storeFields(String seriesId, AbstractMetadataCollection metadata);

}
