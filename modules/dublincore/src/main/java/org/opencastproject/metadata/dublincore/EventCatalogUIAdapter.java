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

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;

/**
 * An interface class to support the creation of data providers.
 */
public interface EventCatalogUIAdapter extends CatalogUIAdapter {

  /**
   * @return Get the field names and values for the catalogs of this media package.
   */
  DublinCoreMetadataCollection getFields(MediaPackage mediapackage);

  /**
   * Store a change in the metadata into the media package as a {@link Catalog}
   *
   * @param mediapackage
   *          The media package to update
   * @param metadataCollection
   *          The new metadata to update the media package with
   * @return the stored catalog
   */
  Catalog storeFields(MediaPackage mediapackage, DublinCoreMetadataCollection metadataCollection);
}
