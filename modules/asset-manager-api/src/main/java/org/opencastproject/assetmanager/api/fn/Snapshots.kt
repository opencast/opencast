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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.assetmanager.api.fn

import org.opencastproject.assetmanager.api.Snapshot
import org.opencastproject.assetmanager.api.Version
import org.opencastproject.mediapackage.MediaPackage

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt

import java.util.Date

/**
 * Utility functions for dealing with [Snapshot]s.
 */
object Snapshots {

    /**
     * Get the version of a snapshot.
     *
     * @see Snapshot.getVersion
     */
    val getVersion: Fn<Snapshot, Version> = object : Fn<Snapshot, Version>() {
        override fun apply(a: Snapshot): Version {
            return a.version
        }
    }

    /**
     * Get the media package of a snapshot.
     *
     * @see Snapshot.getMediaPackage
     */
    val getMediaPackage: Fn<Snapshot, MediaPackage> = object : Fn<Snapshot, MediaPackage>() {
        override fun apply(a: Snapshot): MediaPackage {
            return a.mediaPackage
        }
    }

    /**
     * Get the media package id of a snapshot.
     *
     * @see Snapshot.getMediaPackage
     */
    val getMediaPackageId: Fn<Snapshot, String> = object : Fn<Snapshot, String>() {
        override fun apply(a: Snapshot): String {
            return a.mediaPackage.identifier.toString()
        }
    }

    /**
     * Get the organization ID of a snapshot.
     *
     * @see Snapshot.getOrganizationId
     */
    val getOrganizationId: Fn<Snapshot, String> = object : Fn<Snapshot, String>() {
        override fun apply(a: Snapshot): String {
            return a.organizationId
        }
    }

    /**
     * Get the series ID of a snapshot.
     */
    val getSeriesId: Fn<Snapshot, Opt<String>> = object : Fn<Snapshot, Opt<String>>() {
        override fun apply(a: Snapshot): Opt<String> {
            return Opt.nul(a.mediaPackage.series)
        }
    }

    /**
     * Get the archival date of a snapshot.
     */
    val getArchivalDate: Fn<Snapshot, Date> = object : Fn<Snapshot, Date>() {
        override fun apply(a: Snapshot): Date {
            return a.archivalDate
        }
    }
}
