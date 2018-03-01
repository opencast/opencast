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
package org.opencastproject.assetmanager.storage.impl.fs;

import static org.opencastproject.util.IoSupport.file;

import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OsgiFileSystemAssetStore extends AbstractFileSystemAssetStore {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(OsgiFileSystemAssetStore.class);

  /** Configuration key for the default Opencast storage directory. A value is optional. */
  public static final String CFG_OPT_STORAGE_DIR = "org.opencastproject.storage.dir";

  /**
   * The default store directory name.
   * Will be used in conjunction with {@link #CFG_OPT_STORAGE_DIR} if {@link #CFG_OPT_STORAGE_DIR} is not set.
   */
  private static final String DEFAULT_STORE_DIRECTORY = "archive";

  /** Configuration key for the archive root directory. */
  public static final String CONFIG_STORE_ROOT_DIR = "org.opencastproject.episode.rootdir";

  /** The root directory for storing files */
  private String rootDirectory;

  /** The workspace */
  private Workspace workspace;

  @Override protected Workspace getWorkspace() {
    return workspace;
  }

  @Override
  protected String getRootDirectory() {
    return rootDirectory;
  }

  /**
   * OSGi DI.
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Service activator, called via declarative services configuration.
   *
   * @param cc
   *          the component context
   */
  public void activate(final ComponentContext cc) throws IllegalStateException, IOException {
    storeType = (String) cc.getProperties().get(AssetStore.STORE_TYPE_PROPERTY);
    logger.info("{} is: {}", AssetStore.STORE_TYPE_PROPERTY, storeType);

    rootDirectory = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_STORE_ROOT_DIR));
    if (rootDirectory == null) {
      final String storageDir = StringUtils.trimToNull(cc.getBundleContext().getProperty(CFG_OPT_STORAGE_DIR));
      if (storageDir == null)
        throw new IllegalArgumentException("Storage directory must be set");
      rootDirectory = PathSupport.concat(storageDir, DEFAULT_STORE_DIRECTORY);
    }
    mkDirs(file(rootDirectory));
    logger.info("Start asset manager files system store at " + rootDirectory);
  }
}
