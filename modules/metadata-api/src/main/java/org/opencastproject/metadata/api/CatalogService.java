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

package org.opencastproject.metadata.api;

import org.opencastproject.mediapackage.Catalog;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public interface CatalogService<T extends MetadataCatalog> {
  /**
   * Returns a new, empty instance of the catalog
   * @return a new catalog
   */
  T newInstance();

  /**
   * Loads the catalog contents from an input stream
   *
   * @param in the catalog data
   * @return the deserialized catalog contents
   * @throws IOException if the content of the catalog can not be loaded
   */
  T load(InputStream in) throws IOException;

  /**
   * Whether the mediapackage catalog is readable by this catalog service.  A true value does not indicate that the
   * catalog will be parsed correctly, only that the catalog's metadata seems to match this service.
   *
   * @param catalog the mediapackage catalog
   * @return whether the catalog is readable
   */
  boolean accepts(Catalog catalog);

  /**
   * Serializes a catalog
   *
   * @param catalog The catalog to serialize
   * @return the (usually xml) stream
   * @throws IOException if the catalog can not be serialized properly
   */
  InputStream serialize(T catalog) throws IOException;
}
