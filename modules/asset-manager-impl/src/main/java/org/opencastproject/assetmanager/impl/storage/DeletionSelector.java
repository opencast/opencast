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

import static com.entwinemedia.fn.Equality.eq;
import static java.lang.String.format;

import org.opencastproject.assetmanager.api.Version;

import com.entwinemedia.fn.Equality;
import com.entwinemedia.fn.data.Opt;

import java.io.Serializable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

@ParametersAreNonnullByDefault
@Immutable
public final class DeletionSelector implements Serializable {
  private static final long serialVersionUID = 217139112650188934L;

  private final String mediaPackageId;
  private final String organizationId;
  private final Opt<Version> version;

  public DeletionSelector(String organizationId, String mediaPackageId, Opt<Version> version) {
    this.mediaPackageId = mediaPackageId;
    this.organizationId = organizationId;
    this.version = version;
  }

  public static DeletionSelector delete(String organizationId, String mediaPackageId, Version version) {
    return new DeletionSelector(organizationId, mediaPackageId, Opt.some(version));
  }

  public static DeletionSelector deleteAll(String organizationId, String mediaPackageId) {
    return new DeletionSelector(organizationId, mediaPackageId, Opt.<Version>none());
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public Opt<Version> getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof DeletionSelector && eqFields((DeletionSelector) that));
  }

  private boolean eqFields(DeletionSelector that) {
    return eq(this.mediaPackageId, that.mediaPackageId)
            && eq(this.organizationId, that.organizationId)
            && eq(this.version, that.version);
  }

  @Override
  public int hashCode() {
    return Equality.hash(mediaPackageId, organizationId, version);
  }

  @Override public String toString() {
    return format("DeletionSelector(org=%s,mp=%s,v=%s)", organizationId, mediaPackageId, version);
  }
}
