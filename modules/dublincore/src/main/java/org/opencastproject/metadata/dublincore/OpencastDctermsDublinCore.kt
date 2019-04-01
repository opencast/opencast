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
package org.opencastproject.metadata.dublincore

import com.entwinemedia.fn.Stream.`$`
import org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_ANY
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_AUDIENCE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CONTRIBUTOR
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATOR
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_DESCRIPTION
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_EXTENT
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_ISSUED
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IS_PART_OF
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LANGUAGE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LICENSE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_PUBLISHER
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_RIGHTS_HOLDER
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SOURCE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SPATIAL
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE
import org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TYPE

import org.opencastproject.mediapackage.EName
import org.opencastproject.metadata.dublincore.Temporal.Match

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.Unit
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.fns.Strings

import org.apache.commons.lang3.StringUtils

import java.util.Date
import java.util.Locale
import java.util.MissingResourceException
import javax.annotation.ParametersAreNonnullByDefault

/**
 * [DublinCoreCatalog] wrapper to deal with DublinCore metadata according to the Opencast schema.
 *
 *
 * <h3>General behaviour</h3>
 *
 *  * Set methods that take a string parameter only execute if the string is not blank.
 *  * Set methods that take a list of strings only execute if the list contains at least one non-blank string.
 *  * Set methods--if executed--replace the whole property with the given value/s.
 *  * Update methods only execute if the parameter is some non-blank string. If executed they
 * behave like a set method and replace all exiting entries.
 *  * Add methods only execute if the parameter is some non-blank string.
 *
 */
@ParametersAreNonnullByDefault
abstract class OpencastDctermsDublinCore private constructor(
        /** Return the wrapped catalog.  */
        val catalog: DublinCoreCatalog) {

    /* ------------------------------------------------------------------------------------------------------------------ */

    var publishers: List<String>
        get() = get(PROPERTY_PUBLISHER)
        set(publishers) = set(PROPERTY_PUBLISHER, publishers)

    /* ------------------------------------------------------------------------------------------------------------------ */

    var rightsHolders: List<String>
        get() = get(PROPERTY_RIGHTS_HOLDER)
        set(rightsHolders) = set(PROPERTY_RIGHTS_HOLDER, rightsHolders)

    /* ------------------------------------------------------------------------------------------------------------------ */

    val license: Opt<String>
        get() = getFirst(PROPERTY_LICENSE)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_IDENTIFIER] property.  */
    val dcIdentifier: Opt<String>
        get() = getFirst(PROPERTY_IDENTIFIER)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_TITLE] property.  */
    val title: Opt<String>
        get() = getFirst(PROPERTY_TITLE)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_DESCRIPTION] property.  */
    val description: Opt<String>
        get() = getFirst(PROPERTY_DESCRIPTION)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get all [DublinCore.PROPERTY_AUDIENCE] properties.  */
    /** Set multiple [DublinCore.PROPERTY_AUDIENCE] properties.  */
    var audiences: List<String>
        get() = get(PROPERTY_AUDIENCE)
        set(audiences) = set(PROPERTY_AUDIENCE, audiences)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_CREATED] property.  */
    val created: Opt<Temporal>
        get() = getFirstVal(PROPERTY_CREATED).map(OpencastMetadataCodec.decodeTemporal)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get all [DublinCore.PROPERTY_CREATOR] properties.  */
    /** Set multiple [DublinCore.PROPERTY_CREATOR] properties.  */
    var creators: List<String>
        get() = get(PROPERTY_CREATOR)
        set(creators) = set(PROPERTY_CREATOR, creators)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_EXTENT] property.  */
    val extent: Opt<Long>
        get() = getFirst(PROPERTY_EXTENT).map(OpencastMetadataCodec.decodeDuration)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_ISSUED] property.  */
    val issued: Opt<Date>
        get() = getFirst(PROPERTY_ISSUED).map(OpencastMetadataCodec.decodeDate)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_LANGUAGE] property.  */
    val language: Opt<String>
        get() = getFirst(PROPERTY_LANGUAGE)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_SPATIAL] property.  */
    val spatial: Opt<String>
        get() = getFirst(PROPERTY_SPATIAL)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_SOURCE] property.  */
    val source: Opt<String>
        get() = getFirst(PROPERTY_SOURCE)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get all [DublinCore.PROPERTY_CONTRIBUTOR] properties.  */
    /** Set multiple [DublinCore.PROPERTY_CONTRIBUTOR] properties.  */
    var contributors: List<String>
        get() = get(PROPERTY_CONTRIBUTOR)
        set(contributors) = set(PROPERTY_CONTRIBUTOR, contributors)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_TEMPORAL] property.  */
    val temporal: Opt<Temporal>
        get() = getFirstVal(PROPERTY_TEMPORAL).map(OpencastMetadataCodec.decodeTemporal)

    /* ------------------------------------------------------------------------------------------------------------------ */

    /** Get the [DublinCore.PROPERTY_TYPE] property split into its components. Components are separated by "/".  */
    val type: Opt<Stream<String>>
        get() = getFirst(PROPERTY_TYPE).map(Strings.split("/"))

    /** Get the [DublinCore.PROPERTY_TYPE] property as a single string.  */
    val typeCombined: Opt<String>
        get() = getFirst(PROPERTY_TYPE)

    private val mkValue = object : Fn<String, DublinCoreValue>() {
        override fun apply(v: String): DublinCoreValue {
            return DublinCoreValue.mk(v)
        }
    }

    fun addPublisher(publisher: String) {
        add(PROPERTY_PUBLISHER, publisher)
    }

    fun removePublishers() {
        catalog.remove(PROPERTY_PUBLISHER)
    }

    fun addRightsHolder(rightsHolder: String) {
        add(PROPERTY_RIGHTS_HOLDER, rightsHolder)
    }

    fun removeRightsHolders() {
        catalog.remove(PROPERTY_RIGHTS_HOLDER)
    }

    fun setLicense(license: String) {
        set(PROPERTY_LICENSE, license)
    }

    fun removeLicense() {
        catalog.remove(PROPERTY_LICENSE)
    }

    /** Set the [DublinCore.PROPERTY_IDENTIFIER] property.  */
    fun setDcIdentifier(id: String) {
        set(PROPERTY_IDENTIFIER, id)
    }

    /** Update the [DublinCore.PROPERTY_IDENTIFIER] property.  */
    fun updateDcIdentifier(id: Opt<String>) {
        update(PROPERTY_IDENTIFIER, id)
    }

    /** Remove the [DublinCore.PROPERTY_IDENTIFIER] property.  */
    fun removeDcIdentifier() {
        catalog.remove(PROPERTY_IDENTIFIER)
    }

    /** Set the [DublinCore.PROPERTY_TITLE] property.  */
    fun setTitle(title: String) {
        set(PROPERTY_TITLE, title)
    }

    /** Update the [DublinCore.PROPERTY_TITLE] property.  */
    fun updateTitle(title: Opt<String>) {
        update(PROPERTY_TITLE, title)
    }

    /** Remove the [DublinCore.PROPERTY_TITLE] property.  */
    fun removeTitle() {
        catalog.remove(PROPERTY_TITLE)
    }

    /** Set the [DublinCore.PROPERTY_DESCRIPTION] property.  */
    fun setDescription(description: String) {
        set(PROPERTY_DESCRIPTION, description)
    }

    /** Update the [DublinCore.PROPERTY_DESCRIPTION] property.  */
    fun updateDescription(description: Opt<String>) {
        update(PROPERTY_DESCRIPTION, description)
    }

    /** Remove the [DublinCore.PROPERTY_DESCRIPTION] property.  */
    fun removeDescription() {
        catalog.remove(PROPERTY_DESCRIPTION)
    }

    /** Set the [DublinCore.PROPERTY_AUDIENCE] property.  */
    fun setAudience(audience: String) {
        set(PROPERTY_AUDIENCE, audience)
    }

    /** Add an [DublinCore.PROPERTY_AUDIENCE] property.  */
    fun addAudience(audience: String) {
        add(PROPERTY_AUDIENCE, audience)
    }

    /** Update the [DublinCore.PROPERTY_AUDIENCE] property.  */
    fun updateAudience(audience: Opt<String>) {
        update(PROPERTY_AUDIENCE, audience)
    }

    /** Remove all [DublinCore.PROPERTY_AUDIENCE] properties.  */
    fun removeAudiences() {
        catalog.remove(PROPERTY_AUDIENCE)
    }

    /** Set the [DublinCore.PROPERTY_CREATED] property. The date is encoded with a precision of [Precision.Day].  */
    fun setCreated(date: Date) {
        // only allow to set a created date, if no start date is set. Otherwise DC created will be changed by changing the
        // start date with setTemporal. Synchronization is not vice versa, as setting DC created to arbitraty dates might
        // have unwanted side effects, like setting the wrong recording time, on imported data, or third-party REST calls.
        if (temporal.isNone) {
            setDate(PROPERTY_CREATED, date, Precision.Day)
        }
    }

    /** Set the [DublinCore.PROPERTY_CREATED] property. The date is encoded with a precision of [Precision.Day].  */
    fun setCreated(t: Temporal) {
        // only allow to set a created date, if no start date is set. Otherwise DC created will be changed by changing the
        // start date with setTemporal. Synchronization is not vice versa, as setting DC created to arbitraty dates might
        // have unwanted side effects, like setting the wrong recording time, on imported data, or third-party REST calls.
        if (temporal.isNone) {
            t.fold(object : Match<Unit> {
                override fun period(period: DCMIPeriod): Unit {
                    setCreated(period.start!!)
                    return Unit.unit
                }

                override fun instant(instant: Date): Unit {
                    setCreated(instant)
                    return Unit.unit
                }

                override fun duration(duration: Long): Unit {
                    return Unit.unit
                }
            })
        }
    }

    /** Remove the [DublinCore.PROPERTY_CREATED] property.  */
    fun removeCreated() {
        catalog.remove(PROPERTY_CREATED)
    }

    /** Set the [DublinCore.PROPERTY_CREATOR] property.  */
    fun setCreator(creator: String) {
        set(PROPERTY_CREATOR, creator)
    }

    /** Add a [DublinCore.PROPERTY_CREATOR] property.  */
    fun addCreator(name: String) {
        add(PROPERTY_CREATOR, name)
    }

    /** Update the [DublinCore.PROPERTY_CREATOR] property.  */
    fun updateCreator(name: Opt<String>) {
        update(PROPERTY_CREATOR, name)
    }

    /** Remove all [DublinCore.PROPERTY_CREATOR] properties.  */
    fun removeCreators() {
        catalog.remove(PROPERTY_CREATOR)
    }

    /** Set the [DublinCore.PROPERTY_EXTENT] property.  */
    fun setExtent(extent: Long) {
        catalog.set(PROPERTY_EXTENT, OpencastMetadataCodec.encodeDuration(extent))
    }

    /** Remove the [DublinCore.PROPERTY_EXTENT] property.  */
    fun removeExtent() {
        catalog.remove(PROPERTY_EXTENT)
    }

    /** Set the [DublinCore.PROPERTY_ISSUED] property.  */
    fun setIssued(date: Date) {
        setDate(PROPERTY_ISSUED, date, Precision.Day)
    }

    /** Update the [DublinCore.PROPERTY_ISSUED] property.  */
    fun updateIssued(date: Opt<Date>) {
        updateDate(PROPERTY_ISSUED, date, Precision.Day)
    }

    /** Remove the [DublinCore.PROPERTY_ISSUED] property.  */
    fun removeIssued() {
        catalog.remove(PROPERTY_ISSUED)
    }

    /**
     * Set the [DublinCore.PROPERTY_LANGUAGE] property.
     * A 2- or 3-letter ISO code. 2-letter ISO codes are tried to convert into a 3-letter code.
     * If this is not possible the provided string is used as is.
     */
    fun setLanguage(lang: String) {
        if (StringUtils.isNotBlank(lang)) {
            var doLang = lang
            if (lang.length == 2) {
                try {
                    doLang = Locale(lang).isO3Language
                } catch (ignore: MissingResourceException) {
                }

            }
            set(PROPERTY_LANGUAGE, doLang)
        }
    }

    /** Remove the [DublinCore.PROPERTY_LANGUAGE] property.  */
    fun removeLanguage() {
        catalog.remove(PROPERTY_LANGUAGE)
    }

    /** Set the [DublinCore.PROPERTY_SPATIAL] property.  */
    fun setSpatial(spatial: String) {
        set(PROPERTY_SPATIAL, spatial)
    }

    /** Update the [DublinCore.PROPERTY_SPATIAL] property.  */
    fun updateSpatial(spatial: Opt<String>) {
        update(PROPERTY_SPATIAL, spatial)
    }

    /** Remove the [DublinCore.PROPERTY_SPATIAL] property.  */
    fun removeSpatial() {
        catalog.remove(PROPERTY_SPATIAL)
    }

    /** Set the [DublinCore.PROPERTY_SOURCE] property.  */
    fun setSource(source: String) {
        set(PROPERTY_SOURCE, source)
    }

    /** Remove the [DublinCore.PROPERTY_SOURCE] property.  */
    fun removeSource() {
        catalog.remove(PROPERTY_SOURCE)
    }

    /** Set the [DublinCore.PROPERTY_CONTRIBUTOR] property.  */
    fun setContributor(contributor: String) {
        set(PROPERTY_CONTRIBUTOR, contributor)
    }

    /** Add a [DublinCore.PROPERTY_CONTRIBUTOR] property.  */
    fun addContributor(contributor: String) {
        add(PROPERTY_CONTRIBUTOR, contributor)
    }

    /** Update the [DublinCore.PROPERTY_CONTRIBUTOR] property.  */
    fun updateContributor(contributor: Opt<String>) {
        update(PROPERTY_CONTRIBUTOR, contributor)
    }

    /** Remove all [DublinCore.PROPERTY_CONTRIBUTOR] properties.  */
    fun removeContributors() {
        catalog.remove(PROPERTY_CONTRIBUTOR)
    }

    /**
     * Set the [DublinCore.PROPERTY_TEMPORAL] property.
     * The dates are encoded with a precision of [Precision.Second].
     */
    fun setTemporal(from: Date, to: Date) {
        setPeriod(PROPERTY_TEMPORAL, from, to, Precision.Second)

        // make sure that DC created is synchronized with start date, as discussed in MH-12250
        setDate(PROPERTY_CREATED, from, Precision.Day)
    }

    /** Remove the [DublinCore.PROPERTY_TEMPORAL] property.  */
    fun removeTemporal() {
        catalog.remove(PROPERTY_TEMPORAL)
    }

    /**
     * Set the [DublinCore.PROPERTY_TYPE] property from a type and a subtype.
     * Type and subtype are separated by "/".
     */
    fun setType(type: String, subtype: String) {
        set(PROPERTY_TYPE, "$type/$subtype")
    }

    /** Set the [DublinCore.PROPERTY_TYPE] property from a single string.  */
    fun setType(type: String) {
        set(PROPERTY_TYPE, type)
    }

    /** Remove the [DublinCore.PROPERTY_TYPE] property.  */
    fun removeType() {
        catalog.remove(PROPERTY_TYPE)
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    class Episode(dc: DublinCoreCatalog) : OpencastDctermsDublinCore(dc) {

        /** Get the [DublinCore.PROPERTY_IS_PART_OF] property.  */
        val isPartOf: Opt<String>
            get() = getFirst(PROPERTY_IS_PART_OF)

        /** Set the [DublinCore.PROPERTY_IS_PART_OF] property.  */
        fun setIsPartOf(seriesID: String) {
            set(PROPERTY_IS_PART_OF, seriesID)
        }

        /** Update the [DublinCore.PROPERTY_IS_PART_OF] property.  */
        fun updateIsPartOf(seriesID: Opt<String>) {
            update(PROPERTY_IS_PART_OF, seriesID)
        }

        /** Remove the [DublinCore.PROPERTY_IS_PART_OF] property.  */
        fun removeIsPartOf() {
            catalog.remove(PROPERTY_IS_PART_OF)
        }

    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    class Series(dc: DublinCoreCatalog) : OpencastDctermsDublinCore(dc)

    /* ------------------------------------------------------------------------------------------------------------------ */

    protected fun setDate(property: EName, date: Date, p: Precision) {
        catalog.set(property, OpencastMetadataCodec.encodeDate(date, p))
    }

    protected fun updateDate(property: EName, date: Opt<Date>, p: Precision) {
        for (d in date) {
            setDate(property, d, p)
        }
    }

    /** Encode with [Precision.Second].  */
    protected fun setPeriod(property: EName, from: Date, to: Date, p: Precision) {
        catalog.set(property, OpencastMetadataCodec.encodePeriod(from, to, p))
    }

    protected operator fun get(property: EName): List<String> {
        return catalog.get(property, LANGUAGE_ANY)
    }

    /** Like [DublinCore.getFirst] but with the result wrapped in an Opt.  */
    protected fun getFirst(property: EName): Opt<String> {
        return Opt.nul(catalog.getFirst(property))
    }

    /** Like [DublinCore.getFirstVal] but with the result wrapped in an Opt.  */
    protected fun getFirstVal(property: EName): Opt<DublinCoreValue> {
        return Opt.nul(catalog.getFirstVal(property))
    }

    protected operator fun set(property: EName, value: String) {
        if (StringUtils.isNotBlank(value)) {
            catalog.set(property, value)
        }
    }

    protected operator fun set(property: EName, values: List<String>) {
        val valuesFiltered = `$`(values).filter(Strings.isNotBlank).map(mkValue).toList()
        if (!valuesFiltered.isEmpty()) {
            catalog.remove(property)
            catalog.set(property, valuesFiltered)
        }
    }

    protected fun add(property: EName, value: String) {
        if (StringUtils.isNotBlank(value)) {
            catalog.add(property, value)
        }
    }

    protected fun update(property: EName, value: Opt<String>) {
        for (v in value) {
            set(property, v)
        }
    }
}
