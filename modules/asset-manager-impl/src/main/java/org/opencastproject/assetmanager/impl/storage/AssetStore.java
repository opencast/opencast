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
package org.opencastproject.assetmanager.impl.storage;

import org.opencastproject.storage.StorageUsage;

import com.entwinemedia.fn.data.Opt;

import java.io.InputStream;

/**
 * Versioned storage for binary resources.
 * <p>
 * The ElementStore is designed to be as simple as possible, so that it must not have any additional logic or persistent
 * storage of metadata.
 */
public interface AssetStore extends StorageUsage {

  String STORE_TYPE_PROPERTY = "store.type";

  /** Add the content of <code>soure</code> under the given path. */
  void put(StoragePath path, Source source) throws AssetStoreException;

  /**
   * Copy a resource to a new location.
   *
   * @return true, if the selected resource could be found and copied
   */
  boolean copy(StoragePath from, StoragePath to) throws AssetStoreException;

  /** Get an input stream to a resource. */
  Opt<InputStream> get(StoragePath path) throws AssetStoreException;

  /** Check if a resource exists. */
  boolean contains(StoragePath path) throws AssetStoreException;

  /**
   * Delete all selected resources.
   *
   * @return true, if the selected resources could be found and deleted
   */
  boolean delete(DeletionSelector sel) throws AssetStoreException;

  /**
   * Returns the store.type property
   *
   * @return store type
   */
  String getStoreType();
}
