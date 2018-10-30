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
package org.opencastproject.assetmanager.impl;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.mediapackage.MediaPackage;

import com.entwinemedia.fn.data.Opt;

public class AssetManagerDecorator implements AssetManager {
  protected final AssetManager delegate;

  public AssetManagerDecorator(AssetManager delegate) {
    this.delegate = delegate;
  }

  @Override public Snapshot takeSnapshot(String owner, MediaPackage mp) {
    return owner == null ? delegate.takeSnapshot(mp) : delegate.takeSnapshot(owner, mp);
  }

  @Override public Snapshot takeSnapshot(MediaPackage mp) {
    return takeSnapshot(null, mp);
  }

  @Override public Opt<Asset> getAsset(Version version, String mpId, String mpeId) {
    return delegate.getAsset(version, mpId, mpeId);
  }

  @Override public void setAvailability(Version version, String mpId, Availability availability) {
    delegate.setAvailability(version, mpId, availability);
  }

  @Override public boolean setProperty(Property property) {
    return delegate.setProperty(property);
  }

  @Override public AQueryBuilder createQuery() {
    return delegate.createQuery();
  }

  @Override public Opt<Version> toVersion(String version) {
    return delegate.toVersion(version);
  }
}
