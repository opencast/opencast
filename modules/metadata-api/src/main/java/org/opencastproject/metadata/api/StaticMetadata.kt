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


package org.opencastproject.metadata.api

import org.opencastproject.metadata.api.util.Interval
import org.opencastproject.util.data.NonEmptyList
import org.opencastproject.util.data.Option

import java.util.Date

/**
 * Provides access to a commonly accepted set of metadata.
 *
 *
 * Please note that there is *no* default implementation with setters for each field available
 * to enforce a different style of usage. Whenever you need to return `StaticMetadata`
 * create an anonymous implementation with each getter implementation annotated with `@Override`.
 * This way the compiler helps you to ensure that each field is actually set. When it comes to refactoring this
 * interface, say a field is added and anotherone gets removed a simple compiler run detects all places you
 * need to change in your client code to adjust to the new schema. So it is highly recommended to stay
 * away from the traditional setter idiom.
 */
interface StaticMetadata {

    val id: Option<String>

    val created: Option<Date>

    val extent: Option<Long>

    val language: Option<String>

    val isPartOf: Option<String>

    val replaces: Option<String>

    val type: Option<String>

    val available: Option<Interval>

    val temporalPeriod: Option<Array<Date>>

    val temporalInstant: Option<Date>

    val temporalDuration: Option<Long>

    val titles: NonEmptyList<MetadataValue<String>>

    val subjects: List<MetadataValue<String>>

    val creators: List<MetadataValue<String>>

    val publishers: List<MetadataValue<String>>

    val contributors: List<MetadataValue<String>>

    val description: List<MetadataValue<String>>

    val rightsHolders: List<MetadataValue<String>>

    val spatials: List<MetadataValue<String>>

    val accessRights: List<MetadataValue<String>>

    val licenses: List<MetadataValue<String>>

}
