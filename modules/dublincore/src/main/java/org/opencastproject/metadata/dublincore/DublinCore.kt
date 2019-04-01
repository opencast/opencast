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

import org.opencastproject.mediapackage.EName
import javax.annotation.ParametersAreNonnullByDefault

/**
 * This interface is mainly intended to encapsulate Dublin Core metadata, but it is also capable of maintaining
 * proprietary metadata alongside the Dublin Core.
 *
 *
 * Dublin Core is an initiative to create a digital "library card catalog" for the Web. Dublin Core, in its simple form,
 * is made up of 15 metadata (data that describes data) elements that offer expanded cataloging information and improved
 * document indexing for search engine programs.
 *
 *
 * Two forms of Dublin Core exist: `Simple Dublin Core` and `Qualified Dublin Core`. Simple Dublin
 * Core expresses properties as literal strings using just the 15 metadata elements from the Dublin Core Metadata
 * Element Sets. Each element can have multiple values, but order is defined. Values may have an associated language. <br></br>
 * Please see [http://dublincore.org/documents/dces/](http://dublincore.org/documents/dces/) for further
 * information.
 *
 *
 * Qualified Dublin Core increases the specificity of metadata by extending the set of properties (elements) and by
 * adding information about encoding schemes. While enabling searches to be more specific, qualifiers are also more
 * complex and can pose challenges to interoperability. <br></br>
 * Please see [http://dublincore.org/documents/dcmi-terms/](http://dublincore.org/documents/dcmi-terms/) and
 * [http://dublincore.org/documents/dc-xml-guidelines/](http://dublincore.org/documents/dc-xml-guidelines/)
 * Section 5 for further information.
 *
 *
 * <h3>Current limitations</h3>
 *
 *  * This interface assumes that Dublin Core metadata is stored as XML. According to the Dublin Core specification
 * this is not necessary.
 *  * Encoding schemes aren't fully supported yet.
 *
 */
@ParametersAreNonnullByDefault
interface DublinCore {

    /** Get all contained values grouped by property.  */
    val values: Map<EName, List<DublinCoreValue>>

    /** Get all values as a flat list.  */
    val valuesFlat: List<DublinCoreValue>

    /**
     * Return all contained properties.
     *
     * @return a set of property names
     */
    val properties: Set<EName>

    /**
     * Get all values of a property, either in a certain language or in all contained languages.
     *
     * @param property
     * the property qname
     * @param language
     * a language code, [.LANGUAGE_UNDEFINED] or [.LANGUAGE_ANY]
     * @return a list of values which is empty if the property is not set
     */
    operator fun get(property: EName, language: String): List<String>

    /**
     * Get all values of a property no matter what language they have.
     *
     * @param property
     * the property's expanded name
     * @return a list of values
     */
    operator fun get(property: EName): List<DublinCoreValue>

    /**
     * Like [.get] but returns only the first value of the list. This method is intended to be a
     * convenience method for those properties that have only one value.
     *
     *
     * Please note, that if you pass [.LANGUAGE_ANY], values with an [undefined language][.LANGUAGE_UNDEFINED]
     * are returned preferably.
     *
     * @param property
     * the property's expanded name
     * @param language
     * a language code, [.LANGUAGE_UNDEFINED] or [.LANGUAGE_ANY]
     * @return the value or null
     */
    fun getFirst(property: EName, language: String): String?

    /**
     * Get the first value of a property, no matter what language it is in. Like a call of
     * [.getFirst] with `language = [.LANGUAGE_ANY]`. Please not that values with an [undefined][.LANGUAGE_UNDEFINED] are returned preferably.
     *
     * @param property
     * the property's expanded name
     * @return the value or null
     * @see .getFirst
     */
    fun getFirst(property: EName): String?

    /**
     * Get the first value of a property, no matter what language it is in. Like a call of
     * [.getFirst] with `language = [.LANGUAGE_ANY]`. Please not that values with an [undefined][.LANGUAGE_UNDEFINED] are returned preferably.
     *
     * @param property
     * the property's expanded name
     * @return the value or null
     * @see .getFirst
     */
    fun getFirstVal(property: EName): DublinCoreValue?

    /**
     * Return all values separated by a delimiter.
     *
     * @param property
     * the property's expanded name
     * @param language
     * a language code, [.LANGUAGE_UNDEFINED] or [.LANGUAGE_ANY]
     * @param delimiter
     * a delimiter
     * @return the concatenated values or null (FIXME bad API. Should not return null)
     */
    fun getAsText(property: EName, language: String, delimiter: String): String?

    /**
     * Return all languages this property has values in.
     *
     * @param property
     * the property's expanded name
     * @return a set of languages which may be empty in case the property does not have any value. Note that the state of
     * having no language defined ([.LANGUAGE_UNDEFINED]) is treated like a language.
     */
    fun getLanguages(property: EName): Set<String>

    /**
     * Check, if a property has multiple values assigned.
     *
     * @param property
     * the property's expanded name
     * @param language
     * a language code, [.LANGUAGE_UNDEFINED] or [.LANGUAGE_ANY]
     */
    fun hasMultipleValues(property: EName, language: String): Boolean

    /**
     * Check if a property has multiple values, ignoring any language information.
     *
     * @param property
     * the property's expanded name
     */
    fun hasMultipleValues(property: EName): Boolean

    /**
     * Check if a property has at least one value assigned.
     *
     * @param property
     * the property's expanded name
     * @param language
     * a language code, [.LANGUAGE_UNDEFINED] or [.LANGUAGE_ANY]
     */
    fun hasValue(property: EName, language: String): Boolean

    /**
     * Check if a property has at least on value without language information assigned. Like a call of
     * [.hasValue] with `language = [.LANGUAGE_ANY]`
     *
     * @param property
     * the property's expanded name
     */
    fun hasValue(property: EName): Boolean

    /**
     * Set a property to the given value, overwriting an existing value in the given language.
     *
     *
     * Please note that it is not allowed to pass [.LANGUAGE_ANY] as `language`.
     *
     * @param property
     * the property's expanded name
     * @param value
     * the value or null to remove all values of the given language for this property
     * @param language
     * a language code or [.LANGUAGE_UNDEFINED]
     */
    operator fun set(property: EName, value: String?, language: String)

    /**
     * Set a value without language information to a property, overwriting an existing value. This is like calling
     * [.set] with `language = [.LANGUAGE_UNDEFINED]`
     *
     * @param property
     * the property's expanded name
     * @param value
     * the value or null to remove all values of [.LANGUAGE_UNDEFINED] for this property
     */
    operator fun set(property: EName, value: String?)

    /**
     * Set a property to a value, overwriting an existing value.
     *
     * @param property
     * the property's expanded name
     * @param value
     * the value or null to completely remove the property (all values in all languages)
     */
    operator fun set(property: EName, value: DublinCoreValue?)

    /**
     * Set a property to a list of values, overwriting any existing.
     *
     * @param property
     * the property's expanded name
     * @param values
     * the values or an empty list
     */
    operator fun set(property: EName, values: List<DublinCoreValue>)

    /**
     * Add a value to a property.
     *
     *
     * Please note that it is not allowed to pass [.LANGUAGE_ANY] as `language`.
     *
     * @param property
     * the property's expanded name
     * @param value
     * the value
     * @param language
     * a language code or [.LANGUAGE_UNDEFINED]
     */
    fun add(property: EName, value: String, language: String)

    /**
     * Add a value without language information to a property. This is like calling [.add]
     * with `language = [.LANGUAGE_UNDEFINED]`
     *
     * @param property
     * the property's expanded name
     * @param value
     * the value
     */
    fun add(property: EName, value: String)

    /**
     * Add a value to a property.
     *
     * @param property
     * the property's expanded name
     * @param value
     * the value
     */
    fun add(property: EName, value: DublinCoreValue)

    /**
     * Remove values of a property.
     *
     *  *  [.LANGUAGE_ANY]: remove the whole element
     *  *  [.LANGUAGE_UNDEFINED]: remove only values with no language information
     *  * language code: remove values of that language
     *
     *
     * @param property
     * the property's expanded name
     * @param language
     * a language code, [.LANGUAGE_UNDEFINED] or [.LANGUAGE_ANY]
     */
    fun remove(property: EName, language: String)

    /**
     * Remove a complete property.
     *
     * @param property
     * the property's expanded name
     */
    fun remove(property: EName)

    /** Clear the Dublin Core  */
    fun clear()

    companion object {

        /**
         * Namespace name of the `/terms/` namespace. See [http://dublincore.org/documents/dcmi-terms/](http://dublincore.org/documents/dcmi-terms/) for details.
         */
        val TERMS_NS_URI = "http://purl.org/dc/terms/"

        /**
         * Namespace prefix if the `/terms/` namespace. See [http://dublincore.org/documents/dcmi-terms/](http://dublincore.org/documents/dcmi-terms/) for details.
         */
        val TERMS_NS_PREFIX = "dcterms"

        /**
         * Namespace name of the `/elements/1.1/` namespace. See [http://dublincore.org/documents/dces/](http://dublincore.org/documents/dces/) for details.
         */
        val ELEMENTS_1_1_NS_URI = "http://purl.org/dc/elements/1.1/"

        /**
         * Namespace prefix if the `Elements 1.1` namespace. See [http://dublincore.org/documents/dces/](http://dublincore.org/documents/dces/) for details.
         */
        val ELEMENTS_1_1_NS_PREFIX = "dc"

        /**
         * Dublin Core Property <dfn>abstract</dfn> in the /terms/ namespace. See [DCMI Terms Abstract](http://dublincore.org/documents/dcmi-terms/#terms-abstract).
         */
        val PROPERTY_ABSTRACT = EName(TERMS_NS_URI, "abstract")

        /**
         * Dublin Core Property <dfn>accessRights</dfn> in the /terms/ namespace. See [DCMI Terms Access Rights](http://dublincore.org/documents/dcmi-terms/#terms-accessRights).
         */
        val PROPERTY_ACCESS_RIGHTS = EName(TERMS_NS_URI, "accessRights")

        /**
         * Dublin Core Property <dfn>accrualMethod</dfn> in the /terms/ namespace. See [DCMI Terms Accrual Method](http://dublincore.org/documents/dcmi-terms/#terms-accrualMethod).
         */
        val PROPERTY_ACCRUAL_METHOD = EName(TERMS_NS_URI, "accrualMethod")

        /**
         * Dublin Core Property <dfn>accrualPeriodicity</dfn> in the /terms/ namespace. See [DCMI Terms Accrual Periodicity](http://dublincore.org/documents/dcmi-terms/#terms-accrualPeriodicity).
         */
        val PROPERTY_ACCRUAL_PERIODICITY = EName(TERMS_NS_URI, "accrualPeriodicity")

        /**
         * Dublin Core Property <dfn>accrualPolicy</dfn> in the /terms/ namespace. See [DCMI Terms Accrual Policy](http://dublincore.org/documents/dcmi-terms/#terms-accrualPolicy).
         */
        val PROPERTY_ACCRUAL_POLICY = EName(TERMS_NS_URI, "accrualPolicy")

        /**
         * Dublin Core Property <dfn>alternative</dfn> in the /terms/ namespace. See [DCMI Terms Alternative](http://dublincore.org/documents/dcmi-terms/#terms-alternative).
         */
        val PROPERTY_ALTERNATIVE = EName(TERMS_NS_URI, "alternative")

        /**
         * Dublin Core Property <dfn>audience</dfn> in the /terms/ namespace. See [DCMI Terms Audience](http://dublincore.org/documents/dcmi-terms/#terms-audience).
         */
        val PROPERTY_AUDIENCE = EName(TERMS_NS_URI, "audience")

        /**
         * Dublin Core Property <dfn>available</dfn> in the /terms/ namespace. See [DCMI Terms Available](http://dublincore.org/documents/dcmi-terms/#terms-available).
         */
        val PROPERTY_AVAILABLE = EName(TERMS_NS_URI, "available")

        /**
         * Dublin Core Property <dfn>bibliographicCitation</dfn> in the /terms/ namespace. See [DCMI Terms Bibliographic Citation](http://dublincore.org/documents/dcmi-terms/#terms-bibliographicCitation).
         */
        val PROPERTY_BIBLIOGRAPHIC_CITATION = EName(TERMS_NS_URI, "bibliographicCitation")

        /**
         * Dublin Core Property <dfn>conformsTo</dfn> in the /terms/ namespace. See [DCMI Terms Conforms To](http://dublincore.org/documents/dcmi-terms/#terms-conformsTo).
         */
        val PROPERTY_CONFORMS_TO = EName(TERMS_NS_URI, "conformsTo")

        /**
         * Dublin Core Property <dfn>contributor</dfn> in the /terms/ namespace. See [DCMI Terms Contributor](http://dublincore.org/documents/dcmi-terms/#terms-contributor).
         */
        val PROPERTY_CONTRIBUTOR = EName(TERMS_NS_URI, "contributor")

        /**
         * Dublin Core Property <dfn>coverage</dfn> in the /terms/ namespace. See [DCMI Terms Coverage](http://dublincore.org/documents/dcmi-terms/#terms-coverage).
         */
        val PROPERTY_COVERAGE = EName(TERMS_NS_URI, "coverage")

        /**
         * Dublin Core Property <dfn>created</dfn> in the /terms/ namespace. See [DCMI Terms Created](http://dublincore.org/documents/dcmi-terms/#terms-created).
         */
        val PROPERTY_CREATED = EName(TERMS_NS_URI, "created")

        /**
         * Dublin Core Property <dfn>creator</dfn> in the /terms/ namespace. See [DCMI Terms Creator](http://dublincore.org/documents/dcmi-terms/#terms-creator).
         */
        val PROPERTY_CREATOR = EName(TERMS_NS_URI, "creator")

        /**
         * Dublin Core Property <dfn>date</dfn> in the /terms/ namespace. See [DCMI Terms Date](http://dublincore.org/documents/dcmi-terms/#terms-date).
         */
        val PROPERTY_DATE = EName(TERMS_NS_URI, "date")

        /**
         * Dublin Core Property <dfn>dateAccepted</dfn> in the /terms/ namespace. See [DCMI Terms Date Accepted](http://dublincore.org/documents/dcmi-terms/#terms-dateAccepted).
         */
        val PROPERTY_DATE_ACCEPTED = EName(TERMS_NS_URI, "dateAccepted")

        /**
         * Dublin Core Property <dfn>dateCopyrighted</dfn> in the /terms/ namespace. See [DCMI Terms Date Copyrighted](http://dublincore.org/documents/dcmi-terms/#terms-dateCopyrighted).
         */
        val PROPERTY_DATE_COPYRIGHTED = EName(TERMS_NS_URI, "dateCopyrighted")

        /**
         * Dublin Core Property <dfn>dateSubmitted</dfn> in the /terms/ namespace. See [DCMI Terms Date Submitted](http://dublincore.org/documents/dcmi-terms/#terms-dateSubmitted).
         */
        val PROPERTY_DATE_SUBMITTED = EName(TERMS_NS_URI, "dateSubmitted")

        /**
         * Dublin Core Property <dfn>description</dfn> in the /terms/ namespace. See [DCMI Terms Description](http://dublincore.org/documents/dcmi-terms/#terms-description).
         */
        val PROPERTY_DESCRIPTION = EName(TERMS_NS_URI, "description")

        /**
         * Dublin Core Property <dfn>educationLevel</dfn> in the /terms/ namespace. See [DCMI Terms Education Level](http://dublincore.org/documents/dcmi-terms/#terms-educationLevel).
         */
        val PROPERTY_EDUCATION_LEVEL = EName(TERMS_NS_URI, "educationLevel")

        /**
         * Dublin Core Property <dfn>extent</dfn> in the /terms/ namespace. See [DCMI Terms Extent](http://dublincore.org/documents/dcmi-terms/#terms-extent).
         */
        val PROPERTY_EXTENT = EName(TERMS_NS_URI, "extent")

        /**
         * Dublin Core Property <dfn>format</dfn> in the /terms/ namespace. See [DCMI Terms Format](http://dublincore.org/documents/dcmi-terms/#terms-format).
         */
        val PROPERTY_FORMAT = EName(TERMS_NS_URI, "format")

        /**
         * Dublin Core Property <dfn>hasFormat</dfn> in the /terms/ namespace. See [DCMI Terms Has Format](http://dublincore.org/documents/dcmi-terms/#terms-hasFormat).
         */
        val PROPERTY_HAS_FORMAT = EName(TERMS_NS_URI, "hasFormat")

        /**
         * Dublin Core Property <dfn>hasPart</dfn> in the /terms/ namespace. See [DCMI Terms Has Part](http://dublincore.org/documents/dcmi-terms/#terms-hasPart).
         */
        val PROPERTY_HAS_PART = EName(TERMS_NS_URI, "hasPart")

        /**
         * Dublin Core Property <dfn>hasVersion</dfn> in the /terms/ namespace. See [DCMI Terms Has Version](http://dublincore.org/documents/dcmi-terms/#terms-hasVersion).
         */
        val PROPERTY_HAS_VERSION = EName(TERMS_NS_URI, "hasVersion")

        /**
         * Dublin Core Property <dfn>identifier</dfn> in the /terms/ namespace. See [DCMI Terms Identifier](http://dublincore.org/documents/dcmi-terms/#terms-identifier).
         */
        val PROPERTY_IDENTIFIER = EName(TERMS_NS_URI, "identifier")

        /**
         * Dublin Core Property <dfn>instructionalMethod</dfn> in the /terms/ namespace. See [DCMI Terms Instructional Method](http://dublincore.org/documents/dcmi-terms/#terms-instructionalMethod).
         */
        val PROPERTY_INSTRUCTIONAL_METHOD = EName(TERMS_NS_URI, "instructionalMethod")

        /**
         * Dublin Core Property <dfn>isFormatOf</dfn> in the /terms/ namespace. See [DCMI Terms Is Format Of](http://dublincore.org/documents/dcmi-terms/#terms-isFormatOf).
         */
        val PROPERTY_IS_FORMAT_OF = EName(TERMS_NS_URI, "isFormatOf")

        /**
         * Dublin Core Property <dfn>isPartOf</dfn> in the /terms/ namespace. See [DCMI Terms Is Part Of](http://dublincore.org/documents/dcmi-terms/#terms-isPartOf).
         */
        val PROPERTY_IS_PART_OF = EName(TERMS_NS_URI, "isPartOf")

        /**
         * Dublin Core Property <dfn>isReferencedBy</dfn> in the /terms/ namespace. See [DCMI Terms Is Referenced By](http://dublincore.org/documents/dcmi-terms/#terms-isReferencedBy).
         */
        val PROPERTY_IS_REFERENCED_BY = EName(TERMS_NS_URI, "isReferencedBy")

        /**
         * Dublin Core Property <dfn>isReplacedBy</dfn> in the /terms/ namespace. See [DCMI Terms Is Replaced By](http://dublincore.org/documents/dcmi-terms/#terms-isReplacedBy).
         */
        val PROPERTY_IS_REPLACED_BY = EName(TERMS_NS_URI, "isReplacedBy")

        /**
         * Dublin Core Property <dfn>isRequiredBy</dfn> in the /terms/ namespace. See [DCMI Terms Is Required By](http://dublincore.org/documents/dcmi-terms/#terms-isRequiredBy).
         */
        val PROPERTY_IS_REQUIRED_BY = EName(TERMS_NS_URI, "isRequiredBy")

        /**
         * Dublin Core Property <dfn>issued</dfn> in the /terms/ namespace. See [DCMI Terms Issued](http://dublincore.org/documents/dcmi-terms/#terms-issued).
         */
        val PROPERTY_ISSUED = EName(TERMS_NS_URI, "issued")

        /**
         * Dublin Core Property <dfn>isVersionOf</dfn> in the /terms/ namespace. See [DCMI Terms Is Version Of](http://dublincore.org/documents/dcmi-terms/#terms-isVersionOf).
         */
        val PROPERTY_IS_VERSION_OF = EName(TERMS_NS_URI, "isVersionOf")

        /**
         * Dublin Core Property <dfn>language</dfn> in the /terms/ namespace. See [DCMI Terms Language](http://dublincore.org/documents/dcmi-terms/#terms-language).
         */
        val PROPERTY_LANGUAGE = EName(TERMS_NS_URI, "language")

        /**
         * Dublin Core Property <dfn>license</dfn> in the /terms/ namespace. See [DCMI Terms License](http://dublincore.org/documents/dcmi-terms/#terms-license).
         */
        val PROPERTY_LICENSE = EName(TERMS_NS_URI, "license")

        /**
         * Dublin Core Property <dfn>mediator</dfn> in the /terms/ namespace. See [DCMI Terms Mediator](http://dublincore.org/documents/dcmi-terms/#terms-mediator).
         */
        val PROPERTY_MEDIATOR = EName(TERMS_NS_URI, "mediator")

        /**
         * Dublin Core Property <dfn>medium</dfn> in the /terms/ namespace. See [DCMI Terms Medium](http://dublincore.org/documents/dcmi-terms/#terms-medium).
         */
        val PROPERTY_MEDIUM = EName(TERMS_NS_URI, "medium")

        /**
         * Dublin Core Property <dfn>modified</dfn> in the /terms/ namespace. See [DCMI Terms Modified](http://dublincore.org/documents/dcmi-terms/#terms-modified).
         */
        val PROPERTY_MODIFIED = EName(TERMS_NS_URI, "modified")

        /**
         * Dublin Core Property <dfn>provenance</dfn> in the /terms/ namespace. See [DCMI Terms Provenance](http://dublincore.org/documents/dcmi-terms/#terms-provenance).
         */
        val PROPERTY_PROVENANCE = EName(TERMS_NS_URI, "provenance")

        /**
         * Dublin Core Property <dfn>publisher</dfn> in the /terms/ namespace. See [DCMI Terms Publisher](http://dublincore.org/documents/dcmi-terms/#terms-publisher).
         */
        val PROPERTY_PUBLISHER = EName(TERMS_NS_URI, "publisher")

        /**
         * Dublin Core Property <dfn>references</dfn> in the /terms/ namespace. See [DCMI Terms References](http://dublincore.org/documents/dcmi-terms/#terms-references).
         */
        val PROPERTY_REFERENCES = EName(TERMS_NS_URI, "references")

        /**
         * Dublin Core Property <dfn>relation</dfn> in the /terms/ namespace. See [DCMI Terms Relation](http://dublincore.org/documents/dcmi-terms/#terms-relation).
         */
        val PROPERTY_RELATION = EName(TERMS_NS_URI, "relation")

        /**
         * Dublin Core Property <dfn>replaces</dfn> in the /terms/ namespace. See [DCMI Terms Replaces](http://dublincore.org/documents/dcmi-terms/#terms-replaces).
         */
        val PROPERTY_REPLACES = EName(TERMS_NS_URI, "replaces")

        /**
         * Dublin Core Property <dfn>requires</dfn> in the /terms/ namespace. See [DCMI Terms Requires](http://dublincore.org/documents/dcmi-terms/#terms-requires).
         */
        val PROPERTY_REQUIRES = EName(TERMS_NS_URI, "requires")

        /**
         * Dublin Core Property <dfn>rights</dfn> in the /terms/ namespace. See [DCMI Terms Rights](http://dublincore.org/documents/dcmi-terms/#terms-rights).
         */
        val PROPERTY_RIGHTS = EName(TERMS_NS_URI, "rights")

        /**
         * Dublin Core Property <dfn>rightsHolder</dfn> in the /terms/ namespace. See [DCMI Terms Rights Holder](http://dublincore.org/documents/dcmi-terms/#terms-rightsHolder).
         */
        val PROPERTY_RIGHTS_HOLDER = EName(TERMS_NS_URI, "rightsHolder")

        /**
         * Dublin Core Property <dfn>source</dfn> in the /terms/ namespace. See [DCMI Terms Source](http://dublincore.org/documents/dcmi-terms/#terms-source).
         */
        val PROPERTY_SOURCE = EName(TERMS_NS_URI, "source")

        /**
         * Dublin Core Property <dfn>spatial</dfn> in the /terms/ namespace. See [DCMI Terms Spatial](http://dublincore.org/documents/dcmi-terms/#terms-spatial).
         */
        val PROPERTY_SPATIAL = EName(TERMS_NS_URI, "spatial")

        /**
         * Dublin Core Property <dfn>subject</dfn> in the /terms/ namespace. See [DCMI Terms Subject](http://dublincore.org/documents/dcmi-terms/#terms-subject).
         */
        val PROPERTY_SUBJECT = EName(TERMS_NS_URI, "subject")

        /**
         * Dublin Core Property <dfn>tableOfContents</dfn> in the /terms/ namespace. See [DCMI Terms Table Of Contents](http://dublincore.org/documents/dcmi-terms/#terms-tableOfContents).
         */
        val PROPERTY_TABLE_OF_CONTENTS = EName(TERMS_NS_URI, "tableOfContents")

        /**
         * Dublin Core Property <dfn>temporal</dfn> in the /terms/ namespace. See [DCMI Terms Temporal](http://dublincore.org/documents/dcmi-terms/#terms-temporal).
         */
        val PROPERTY_TEMPORAL = EName(TERMS_NS_URI, "temporal")

        /**
         * Dublin Core Property <dfn>title</dfn> in the /terms/ namespace. See [DCMI Terms Title](http://dublincore.org/documents/dcmi-terms/#terms-title).
         */
        val PROPERTY_TITLE = EName(TERMS_NS_URI, "title")

        /**
         * Dublin Core Property <dfn>type</dfn> in the /terms/ namespace. See [DCMI Terms Type](http://dublincore.org/documents/dcmi-terms/#terms-type).
         */
        val PROPERTY_TYPE = EName(TERMS_NS_URI, "type")

        /**
         * Dublin Core Property <dfn>valid</dfn> in the /terms/ namespace. See [DCMI Terms Valid](http://dublincore.org/documents/dcmi-terms/#terms-valid).
         */
        val PROPERTY_VALID = EName(TERMS_NS_URI, "valid")

        /**
         * Syntax encoding scheme `Box`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_BOX = EName(TERMS_NS_URI, "Box")

        /**
         * Syntax encoding scheme `ISO3166`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_ISO3166 = EName(TERMS_NS_URI, "ISO3166")

        /**
         * Syntax encoding scheme `ISO639-1`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_ISO639_2 = EName(TERMS_NS_URI, "ISO639-1")

        /**
         * Syntax encoding scheme `ISO639-3`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_ISO639_3 = EName(TERMS_NS_URI, "ISO639-3")

        /**
         * Syntax encoding scheme `Period`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_PERIOD = EName(TERMS_NS_URI, "Period")

        /**
         * Syntax encoding scheme `Point`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_POINT = EName(TERMS_NS_URI, "Point")

        /**
         * Syntax encoding scheme `RFC1766`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_RFC1766 = EName(TERMS_NS_URI, "RFC1766")

        /**
         * Syntax encoding scheme `RFC3066`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_RFC3066 = EName(TERMS_NS_URI, "RFC3066")

        /**
         * Syntax encoding scheme `RFC4646`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_RFC4646 = EName(TERMS_NS_URI, "RFC4646")

        /**
         * Syntax encoding scheme `URI`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_URI = EName(TERMS_NS_URI, "URI")

        /**
         * Syntax encoding scheme `W3CDTF`. See [http://dublincore.org/documents/dcmi-terms/#H5](http://dublincore.org/documents/dcmi-terms/#H5)
         */
        val ENC_SCHEME_W3CDTF = EName(TERMS_NS_URI, "W3CDTF")

        /**
         * Syntax encoding scheme `ISO8601` used for durations. See [http://en.wikipedia.org/wiki/ISO_8601#Durations](http://en.wikipedia.org/wiki/ISO_8601#Durations)
         */
        val ENC_SCHEME_ISO8601 = EName(TERMS_NS_URI, "ISO8601")

        /* Language constants */

        /** Language code for properties without language information.  */
        val LANGUAGE_UNDEFINED = "__"

        /**
         * Language code that matches any language.
         *
         *
         * Use this code whenever you need values in *all* languages or you don't care about the language. Note that
         * all methods taking this as a legal value for the language parameter are adviced to return at first a value for
         * [.LANGUAGE_UNDEFINED] if multiple values exist and only one value is wanted.
         */
        val LANGUAGE_ANY = "**"
    }
}
