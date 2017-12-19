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
package org.opencastproject.assetmanager.api;

import static com.entwinemedia.fn.Equality.eq;
import static java.lang.String.format;

import com.entwinemedia.fn.Equality;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class AssetId {
  private final String mpId;
  private final String mpeId;
  private final Version version;

  public AssetId(Version version, String mpId, String mpeId) {
    this.version = version;
    this.mpId = mpId;
    this.mpeId = mpeId;
  }

  public static AssetId mk(Version version, String mpId, String mpeId) {
    return new AssetId(version, mpId, mpeId);
  }

  public String getMediaPackageId() {
    return mpId;
  }

  public String getMediaPackageElementId() {
    return mpeId;
  }

  public Version getVersion() {
    return version;
  }

  @Override public int hashCode() {
    return Equality.hash(version, mpId, mpeId);
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof AssetId && eqFields((AssetId) that));
  }

  private boolean eqFields(AssetId that) {
    return eq(mpId, that.mpId) && eq(mpeId, that.mpeId) && eq(version, that.version);
  }

  @Override public String toString() {
    return format("AssetId(mpId=%s, mpeId=%s, version=%s)", mpId, mpeId, version);
  }
}
