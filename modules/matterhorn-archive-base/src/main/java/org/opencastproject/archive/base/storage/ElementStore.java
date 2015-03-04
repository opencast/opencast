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
package org.opencastproject.archive.base.storage;

import org.opencastproject.archive.base.StoragePath;
import org.opencastproject.storage.StorageUsage;
import org.opencastproject.util.data.Option;

import java.io.InputStream;

/**
 * Versioned storage for binary resources.
 * <p/>
 * The ElementStore is designed to be as simple as possible, so that it must not have any additional logic or persistent
 * storage of metadata.
 */
public interface ElementStore extends StorageUsage {

  /** Add the content of <code>soure</code> under the given path. */
  void put(StoragePath path, Source source) throws ElementStoreException;

  /**
   * Copy a resource to a new location.
   * 
   * @return true, if the selected resource could be found and copied
   */
  boolean copy(StoragePath from, StoragePath to) throws ElementStoreException;

  /** Get an input stream to a resource. */
  Option<InputStream> get(StoragePath path) throws ElementStoreException;

  /** Check if a resource exists. */
  boolean contains(StoragePath path) throws ElementStoreException;

  /**
   * Delete all selected resources.
   * 
   * @return true, if the selected resources could be found and deleted
   */
  boolean delete(DeletionSelector sel) throws ElementStoreException;
}
