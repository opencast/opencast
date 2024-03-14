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

package org.opencastproject.assetmanager.api.storage;

import org.opencastproject.assetmanager.api.Availability;

import java.io.InputStream;

public interface RemoteAssetStore extends AssetStore {
  // Defines the root directory to be used for any caching done by a remote
  // asset store.  Caches should live as directories under this directory.
  String ASSET_STORE_CACHE_ROOT = "org.opencastproject.assetmanager.storage.cache.rootdir";

  InputStream streamNotReady = new InputStream() {
    @Override
    public int read() {
      return -1;  // end of stream
    }

    @Override
    public int available() {
      return 0;
    }
  };

  Availability getAvailability(StoragePath path) throws AssetStoreException;

  /**
   * Returns an estimate in millisecs of when the element stream might be ready to read
   * @param path to element
   * @return
   * @throws AssetStoreException
   */
  Integer getReadyEstimate(StoragePath path) throws AssetStoreException;
}
