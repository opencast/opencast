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
package org.opencastproject.assetmanager.api.fn;

import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.mediapackage.MediaPackage;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import java.util.Date;

/**
 * Utility functions for dealing with {@link Snapshot}s.
 */
public final class Snapshots {
  private Snapshots() {
  }

  /**
   * Get the version of a snapshot.
   *
   * @see Snapshot#getVersion()
   */
  public static final Fn<Snapshot, Version> getVersion = new Fn<Snapshot, Version>() {
    @Override public Version apply(Snapshot a) {
      return a.getVersion();
    }
  };

  /**
   * Get the media package of a snapshot.
   *
   * @see Snapshot#getMediaPackage()
   */
  public static final Fn<Snapshot, MediaPackage> getMediaPackage = new Fn<Snapshot, MediaPackage>() {
    @Override public MediaPackage apply(Snapshot a) {
      return a.getMediaPackage();
    }
  };

  /**
   * Get the media package id of a snapshot.
   *
   * @see Snapshot#getMediaPackage()
   */
  public static final Fn<Snapshot, String> getMediaPackageId = new Fn<Snapshot, String>() {
    @Override public String apply(Snapshot a) {
      return a.getMediaPackage().getIdentifier().toString();
    }
  };

  /**
   * Get the organization ID of a snapshot.
   *
   * @see Snapshot#getOrganizationId()
   */
  public static final Fn<Snapshot, String> getOrganizationId = new Fn<Snapshot, String>() {
    @Override public String apply(Snapshot a) {
      return a.getOrganizationId();
    }
  };

  /**
   * Get the series ID of a snapshot.
   */
  public static final Fn<Snapshot, Opt<String>> getSeriesId = new Fn<Snapshot, Opt<String>>() {
    @Override public Opt<String> apply(Snapshot a) {
      return Opt.nul(a.getMediaPackage().getSeries());
    }
  };

  /**
   * Get the archival date of a snapshot.
   */
  public static final Fn<Snapshot, Date> getArchivalDate = new Fn<Snapshot, Date>() {
    @Override public Date apply(Snapshot a) {
      return a.getArchivalDate();
    }
  };
}
