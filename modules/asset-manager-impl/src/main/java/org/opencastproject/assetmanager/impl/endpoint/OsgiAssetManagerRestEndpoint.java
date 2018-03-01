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
package org.opencastproject.assetmanager.impl.endpoint;

import org.opencastproject.assetmanager.impl.TieredStorageAssetManager;

import javax.ws.rs.Path;

/** OSGi bound implementation. */
@Path("/")
public class OsgiAssetManagerRestEndpoint extends AbstractTieredStorageAssetManagerRestEndpoint {
  private TieredStorageAssetManager assetManager;

  @Override public TieredStorageAssetManager getAssetManager() {
    return assetManager;
  }

  /** OSGi DI */
  public void setAssetManager(TieredStorageAssetManager assetManager) {
    this.assetManager = assetManager;
  }
}
