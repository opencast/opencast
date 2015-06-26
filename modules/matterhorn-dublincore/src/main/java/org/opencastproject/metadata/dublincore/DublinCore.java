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


package org.opencastproject.metadata.dublincore;

import org.opencastproject.mediapackage.EName;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This interface is mainly intended to encapsulate Dublin Core metadata, but it is also capable of maintaining
 * proprietary metadata alongside the Dublin Core.
 * <p/>
 * Dublin Core is an initiative to create a digital "library card catalog" for the Web. Dublin Core, in its simple form,
 * is made up of 15 metadata (data that describes data) elements that offer expanded cataloging information and improved
 * document indexing for search engine programs.
 * <p/>
 * Two forms of Dublin Core exist: <def>Simple Dublin Core</def> and <def>Qualified Dublin Core</def>. Simple Dublin
 * Core expresses properties as literal strings using just the 15 metadata elements from the Dublin Core Metadata
 * Element Sets. Each element can have multiple values, but order is defined. Values may have an associated language. <br>
 * Please see <a href="http://dublincore.org/documents/dces/">http://dublincore.org/documents/dces/</a> for further
 * information.
 * <p/>
 * Qualified Dublin Core increases the specificity of metadata by extending the set of properties (elements) and by
 * adding information about encoding schemes. While enabling searches to be more specific, qualifiers are also more
 * complex and can pose challenges to interoperability. <br>
 * Please see <a href="http://dublincore.org/documents/dcmi-terms/">http://dublincore.org/documents/dcmi-terms/</a> and
 * <a href="http://dublincore.org/documents/dc-xml-guidelines/">http://dublincore.org/documents/dc-xml-guidelines/</a>
 * Section 5 for further information.
 * <p/>
 * <h3>Current limitations</h3>
 * <ul>
 * <li>This interface assumes that Dublin Core metadata is stored as XML. According to the Dublin Core specification
 * this is not necessary.
 * <li>Encoding schemes aren't fully supported yet.
 * </ul>
 */
@ParametersAreNonnullByDefault
public interface DublinCore {

  /**
   * Namespace name of the <def>/terms/</def> namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/">http://dublincore.org/documents/dcmi-terms/</a> for details.
   */
  String TERMS_NS_URI = "http://purl.org/dc/terms/";

  /**
   * Namespace prefix if the <def>/terms/</def> namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/">http://dublincore.org/documents/dcmi-terms/</a> for details.
   */
  String TERMS_NS_PREFIX = "dcterms";

  /**
   * Namespace name of the <def>/elements/1.1/</def> namespace. See <a
   * href="http://dublincore.org/documents/dces/">http://dublincore.org/documents/dces/</a> for details.
   */
  String ELEMENTS_1_1_NS_URI = "http://purl.org/dc/elements/1.1/";

  /**
   * Namespace prefix if the <def>Elements 1.1</def> namespace. See <a
   * href="http://dublincore.org/documents/dces/">http://dublincore.org/documents/dces/</a> for details.
   */
  String ELEMENTS_1_1_NS_PREFIX = "dc";

  /**
   * Dublin Core Property <dfn>abstract</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-abstract">DCMI Terms Abstract</a>.
   */
  EName PROPERTY_ABSTRACT = new EName(TERMS_NS_URI, "abstract");

  /**
   * Dublin Core Property <dfn>accessRights</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-accessRights">DCMI Terms Access Rights</a>.
   */
  EName PROPERTY_ACCESS_RIGHTS = new EName(TERMS_NS_URI, "accessRights");

  /**
   * Dublin Core Property <dfn>accrualMethod</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-accrualMethod">DCMI Terms Accrual Method</a>.
   */
  EName PROPERTY_ACCRUAL_METHOD = new EName(TERMS_NS_URI, "accrualMethod");

  /**
   * Dublin Core Property <dfn>accrualPeriodicity</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-accrualPeriodicity">DCMI Terms Accrual Periodicity</a>.
   */
  EName PROPERTY_ACCRUAL_PERIODICITY = new EName(TERMS_NS_URI, "accrualPeriodicity");

  /**
   * Dublin Core Property <dfn>accrualPolicy</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-accrualPolicy">DCMI Terms Accrual Policy</a>.
   */
  EName PROPERTY_ACCRUAL_POLICY = new EName(TERMS_NS_URI, "accrualPolicy");

  /**
   * Dublin Core Property <dfn>alternative</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-alternative">DCMI Terms Alternative</a>.
   */
  EName PROPERTY_ALTERNATIVE = new EName(TERMS_NS_URI, "alternative");

  /**
   * Dublin Core Property <dfn>audience</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-audience">DCMI Terms Audience</a>.
   */
  EName PROPERTY_AUDIENCE = new EName(TERMS_NS_URI, "audience");

  /**
   * Dublin Core Property <dfn>available</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-available">DCMI Terms Available</a>.
   */
  EName PROPERTY_AVAILABLE = new EName(TERMS_NS_URI, "available");

  /**
   * Dublin Core Property <dfn>bibliographicCitation</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-bibliographicCitation">DCMI Terms Bibliographic Citation</a>.
   */
  EName PROPERTY_BIBLIOGRAPHIC_CITATION = new EName(TERMS_NS_URI, "bibliographicCitation");

  /**
   * Dublin Core Property <dfn>conformsTo</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-conformsTo">DCMI Terms Conforms To</a>.
   */
  EName PROPERTY_CONFORMS_TO = new EName(TERMS_NS_URI, "conformsTo");

  /**
   * Dublin Core Property <dfn>contributor</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-contributor">DCMI Terms Contributor</a>.
   */
  EName PROPERTY_CONTRIBUTOR = new EName(TERMS_NS_URI, "contributor");

  /**
   * Dublin Core Property <dfn>coverage</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-coverage">DCMI Terms Coverage</a>.
   */
  EName PROPERTY_COVERAGE = new EName(TERMS_NS_URI, "coverage");

  /**
   * Dublin Core Property <dfn>created</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-created">DCMI Terms Created</a>.
   */
  EName PROPERTY_CREATED = new EName(TERMS_NS_URI, "created");

  /**
   * Dublin Core Property <dfn>creator</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-creator">DCMI Terms Creator</a>.
   */
  EName PROPERTY_CREATOR = new EName(TERMS_NS_URI, "creator");

  /**
   * Dublin Core Property <dfn>date</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-date">DCMI Terms Date</a>.
   */
  EName PROPERTY_DATE = new EName(TERMS_NS_URI, "date");

  /**
   * Dublin Core Property <dfn>dateAccepted</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-dateAccepted">DCMI Terms Date Accepted</a>.
   */
  EName PROPERTY_DATE_ACCEPTED = new EName(TERMS_NS_URI, "dateAccepted");

  /**
   * Dublin Core Property <dfn>dateCopyrighted</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-dateCopyrighted">DCMI Terms Date Copyrighted</a>.
   */
  EName PROPERTY_DATE_COPYRIGHTED = new EName(TERMS_NS_URI, "dateCopyrighted");

  /**
   * Dublin Core Property <dfn>dateSubmitted</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-dateSubmitted">DCMI Terms Date Submitted</a>.
   */
  EName PROPERTY_DATE_SUBMITTED = new EName(TERMS_NS_URI, "dateSubmitted");

  /**
   * Dublin Core Property <dfn>description</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-description">DCMI Terms Description</a>.
   */
  EName PROPERTY_DESCRIPTION = new EName(TERMS_NS_URI, "description");

  /**
   * Dublin Core Property <dfn>educationLevel</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-educationLevel">DCMI Terms Education Level</a>.
   */
  EName PROPERTY_EDUCATION_LEVEL = new EName(TERMS_NS_URI, "educationLevel");

  /**
   * Dublin Core Property <dfn>extent</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-extent">DCMI Terms Extent</a>.
   */
  EName PROPERTY_EXTENT = new EName(TERMS_NS_URI, "extent");

  /**
   * Dublin Core Property <dfn>format</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-format">DCMI Terms Format</a>.
   */
  EName PROPERTY_FORMAT = new EName(TERMS_NS_URI, "format");

  /**
   * Dublin Core Property <dfn>hasFormat</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-hasFormat">DCMI Terms Has Format</a>.
   */
  EName PROPERTY_HAS_FORMAT = new EName(TERMS_NS_URI, "hasFormat");

  /**
   * Dublin Core Property <dfn>hasPart</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-hasPart">DCMI Terms Has Part</a>.
   */
  EName PROPERTY_HAS_PART = new EName(TERMS_NS_URI, "hasPart");

  /**
   * Dublin Core Property <dfn>hasVersion</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-hasVersion">DCMI Terms Has Version</a>.
   */
  EName PROPERTY_HAS_VERSION = new EName(TERMS_NS_URI, "hasVersion");

  /**
   * Dublin Core Property <dfn>identifier</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-identifier">DCMI Terms Identifier</a>.
   */
  EName PROPERTY_IDENTIFIER = new EName(TERMS_NS_URI, "identifier");

  /**
   * Dublin Core Property <dfn>instructionalMethod</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-instructionalMethod">DCMI Terms Instructional Method</a>.
   */
  EName PROPERTY_INSTRUCTIONAL_METHOD = new EName(TERMS_NS_URI, "instructionalMethod");

  /**
   * Dublin Core Property <dfn>isFormatOf</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-isFormatOf">DCMI Terms Is Format Of</a>.
   */
  EName PROPERTY_IS_FORMAT_OF = new EName(TERMS_NS_URI, "isFormatOf");

  /**
   * Dublin Core Property <dfn>isPartOf</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-isPartOf">DCMI Terms Is Part Of</a>.
   */
  EName PROPERTY_IS_PART_OF = new EName(TERMS_NS_URI, "isPartOf");

  /**
   * Dublin Core Property <dfn>isReferencedBy</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-isReferencedBy">DCMI Terms Is Referenced By</a>.
   */
  EName PROPERTY_IS_REFERENCED_BY = new EName(TERMS_NS_URI, "isReferencedBy");

  /**
   * Dublin Core Property <dfn>isReplacedBy</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-isReplacedBy">DCMI Terms Is Replaced By</a>.
   */
  EName PROPERTY_IS_REPLACED_BY = new EName(TERMS_NS_URI, "isReplacedBy");

  /**
   * Dublin Core Property <dfn>isRequiredBy</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-isRequiredBy">DCMI Terms Is Required By</a>.
   */
  EName PROPERTY_IS_REQUIRED_BY = new EName(TERMS_NS_URI, "isRequiredBy");

  /**
   * Dublin Core Property <dfn>issued</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-issued">DCMI Terms Issued</a>.
   */
  EName PROPERTY_ISSUED = new EName(TERMS_NS_URI, "issued");

  /**
   * Dublin Core Property <dfn>isVersionOf</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-isVersionOf">DCMI Terms Is Version Of</a>.
   */
  EName PROPERTY_IS_VERSION_OF = new EName(TERMS_NS_URI, "isVersionOf");

  /**
   * Dublin Core Property <dfn>language</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-language">DCMI Terms Language</a>.
   */
  EName PROPERTY_LANGUAGE = new EName(TERMS_NS_URI, "language");

  /**
   * Dublin Core Property <dfn>license</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-license">DCMI Terms License</a>.
   */
  EName PROPERTY_LICENSE = new EName(TERMS_NS_URI, "license");

  /**
   * Dublin Core Property <dfn>mediator</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-mediator">DCMI Terms Mediator</a>.
   */
  EName PROPERTY_MEDIATOR = new EName(TERMS_NS_URI, "mediator");

  /**
   * Dublin Core Property <dfn>medium</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-medium">DCMI Terms Medium</a>.
   */
  EName PROPERTY_MEDIUM = new EName(TERMS_NS_URI, "medium");

  /**
   * Dublin Core Property <dfn>modified</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-modified">DCMI Terms Modified</a>.
   */
  EName PROPERTY_MODIFIED = new EName(TERMS_NS_URI, "modified");

  /**
   * Dublin Core Property <dfn>provenance</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-provenance">DCMI Terms Provenance</a>.
   */
  EName PROPERTY_PROVENANCE = new EName(TERMS_NS_URI, "provenance");

  /**
   * Dublin Core Property <dfn>publisher</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-publisher">DCMI Terms Publisher</a>.
   */
  EName PROPERTY_PUBLISHER = new EName(TERMS_NS_URI, "publisher");

  /**
   * Dublin Core Property <dfn>references</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-references">DCMI Terms References</a>.
   */
  EName PROPERTY_REFERENCES = new EName(TERMS_NS_URI, "references");

  /**
   * Dublin Core Property <dfn>relation</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-relation">DCMI Terms Relation</a>.
   */
  EName PROPERTY_RELATION = new EName(TERMS_NS_URI, "relation");

  /**
   * Dublin Core Property <dfn>replaces</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-replaces">DCMI Terms Replaces</a>.
   */
  EName PROPERTY_REPLACES = new EName(TERMS_NS_URI, "replaces");

  /**
   * Dublin Core Property <dfn>requires</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-requires">DCMI Terms Requires</a>.
   */
  EName PROPERTY_REQUIRES = new EName(TERMS_NS_URI, "requires");

  /**
   * Dublin Core Property <dfn>rights</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-rights">DCMI Terms Rights</a>.
   */
  EName PROPERTY_RIGHTS = new EName(TERMS_NS_URI, "rights");

  /**
   * Dublin Core Property <dfn>rightsHolder</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-rightsHolder">DCMI Terms Rights Holder</a>.
   */
  EName PROPERTY_RIGHTS_HOLDER = new EName(TERMS_NS_URI, "rightsHolder");

  /**
   * Dublin Core Property <dfn>source</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-source">DCMI Terms Source</a>.
   */
  EName PROPERTY_SOURCE = new EName(TERMS_NS_URI, "source");

  /**
   * Dublin Core Property <dfn>spatial</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-spatial">DCMI Terms Spatial</a>.
   */
  EName PROPERTY_SPATIAL = new EName(TERMS_NS_URI, "spatial");

  /**
   * Dublin Core Property <dfn>subject</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-subject">DCMI Terms Subject</a>.
   */
  EName PROPERTY_SUBJECT = new EName(TERMS_NS_URI, "subject");

  /**
   * Dublin Core Property <dfn>tableOfContents</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-tableOfContents">DCMI Terms Table Of Contents</a>.
   */
  EName PROPERTY_TABLE_OF_CONTENTS = new EName(TERMS_NS_URI, "tableOfContents");

  /**
   * Dublin Core Property <dfn>temporal</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-temporal">DCMI Terms Temporal</a>.
   */
  EName PROPERTY_TEMPORAL = new EName(TERMS_NS_URI, "temporal");

  /**
   * Dublin Core Property <dfn>title</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-title">DCMI Terms Title</a>.
   */
  EName PROPERTY_TITLE = new EName(TERMS_NS_URI, "title");

  /**
   * Dublin Core Property <dfn>type</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-type">DCMI Terms Type</a>.
   */
  EName PROPERTY_TYPE = new EName(TERMS_NS_URI, "type");

  /**
   * Dublin Core Property <dfn>valid</dfn> in the /terms/ namespace. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#terms-valid">DCMI Terms Valid</a>.
   */
  EName PROPERTY_VALID = new EName(TERMS_NS_URI, "valid");

  /**
   * Syntax encoding scheme <def>Box</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_BOX = new EName(TERMS_NS_URI, "Box");

  /**
   * Syntax encoding scheme <def>ISO3166</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_ISO3166 = new EName(TERMS_NS_URI, "ISO3166");

  /**
   * Syntax encoding scheme <def>ISO639-1</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_ISO639_2 = new EName(TERMS_NS_URI, "ISO639-1");

  /**
   * Syntax encoding scheme <def>ISO639-3</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_ISO639_3 = new EName(TERMS_NS_URI, "ISO639-3");

  /**
   * Syntax encoding scheme <def>Period</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_PERIOD = new EName(TERMS_NS_URI, "Period");

  /**
   * Syntax encoding scheme <def>Point</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_POINT = new EName(TERMS_NS_URI, "Point");

  /**
   * Syntax encoding scheme <def>RFC1766</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_RFC1766 = new EName(TERMS_NS_URI, "RFC1766");

  /**
   * Syntax encoding scheme <def>RFC3066</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_RFC3066 = new EName(TERMS_NS_URI, "RFC3066");

  /**
   * Syntax encoding scheme <def>RFC4646</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_RFC4646 = new EName(TERMS_NS_URI, "RFC4646");

  /**
   * Syntax encoding scheme <def>URI</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_URI = new EName(TERMS_NS_URI, "URI");

  /**
   * Syntax encoding scheme <def>W3CDTF</def>. See <a
   * href="http://dublincore.org/documents/dcmi-terms/#H5">http://dublincore.org/documents/dcmi-terms/#H5</a>
   */
  EName ENC_SCHEME_W3CDTF = new EName(TERMS_NS_URI, "W3CDTF");

  /**
   * Syntax encoding scheme <def>ISO8601</def> used for durations. See <a
   * href="http://en.wikipedia.org/wiki/ISO_8601#Durations">http://en.wikipedia.org/wiki/ISO_8601#Durations</a>
   */
  EName ENC_SCHEME_ISO8601 = new EName(TERMS_NS_URI, "ISO8601");

  /* Language constants */

  /** Language code for properties without language information. */
  String LANGUAGE_UNDEFINED = "__";

  /**
   * Language code that matches any language.
   * <p/>
   * Use this code whenever you need values in <em>all</em> languages or you don't care about the language. Note that
   * all methods taking this as a legal value for the language parameter are adviced to return at first a value for
   * {@link #LANGUAGE_UNDEFINED} if multiple values exist and only one value is wanted.
   */
  String LANGUAGE_ANY = "**";

  /**
   * Get all values of a property, either in a certain language or in all contained languages.
   *
   * @param property
   *         the property qname
   * @param language
   *         a language code, {@link #LANGUAGE_UNDEFINED} or {@link #LANGUAGE_ANY}
   * @return a list of values which is empty if the property is not set
   */
  List<String> get(EName property, String language);

  /**
   * Get all values of a property no matter what language they have.
   *
   * @param property
   *         the property's expanded name
   * @return a list of values
   */
  List<DublinCoreValue> get(EName property);

  /** Get all contained values grouped by property. */
  Map<EName, List<DublinCoreValue>> getValues();

  /** Get all values as a flat list. */
  List<DublinCoreValue> getValuesFlat();

  /**
   * Like {@link #get(EName, String)} but returns only the first value of the list. This method is intended to be a
   * convenience method for those properties that have only one value.
   * <p/>
   * Please note, that if you pass {@link #LANGUAGE_ANY}, values with an {@link #LANGUAGE_UNDEFINED undefined language}
   * are returned preferably.
   *
   * @param property
   *         the property's expanded name
   * @param language
   *         a language code, {@link #LANGUAGE_UNDEFINED} or {@link #LANGUAGE_ANY}
   * @return the value or null
   */
  @Nullable String getFirst(EName property, String language);

  /**
   * Get the first value of a property, no matter what language it is in. Like a call of
   * {@link #getFirst(EName, String)} with <code>language = {@link #LANGUAGE_ANY}</code>. Please not that values with an {@link #LANGUAGE_UNDEFINED undefined
   * language} are returned preferably.
   *
   * @param property
   *         the property's expanded name
   * @return the value or null
   * @see #getFirst(EName, String)
   */
  @Nullable String getFirst(EName property);

  /**
   * Get the first value of a property, no matter what language it is in. Like a call of
   * {@link #getFirst(EName, String)} with <code>language = {@link #LANGUAGE_ANY}</code>. Please not that values with an {@link #LANGUAGE_UNDEFINED undefined
   * language} are returned preferably.
   *
   * @param property
   *         the property's expanded name
   * @return the value or null
   * @see #getFirst(EName, String)
   */
  @Nullable DublinCoreValue getFirstVal(EName property);

  /**
   * Return all values separated by a delimiter.
   *
   * @param property
   *         the property's expanded name
   * @param language
   *         a language code, {@link #LANGUAGE_UNDEFINED} or {@link #LANGUAGE_ANY}
   * @param delimiter
   *         a delimiter
   * @return the concatenated values or null (FIXME bad API. Should not return null)
   */
  @Nullable String getAsText(EName property, String language, String delimiter);

  /**
   * Return all languages this property has values in.
   *
   * @param property
   *         the property's expanded name
   * @return a set of languages which may be empty in case the property does not have any value. Note that the state of
   *         having no language defined ({@link #LANGUAGE_UNDEFINED}) is treated like a language.
   */
  Set<String> getLanguages(EName property);

  /**
   * Check, if a property has multiple values assigned.
   *
   * @param property
   *         the property's expanded name
   * @param language
   *         a language code, {@link #LANGUAGE_UNDEFINED} or {@link #LANGUAGE_ANY}
   */
  boolean hasMultipleValues(EName property, String language);

  /**
   * Check if a property has multiple values, ignoring any language information.
   *
   * @param property
   *         the property's expanded name
   */
  boolean hasMultipleValues(EName property);

  /**
   * Check if a property has at least one value assigned.
   *
   * @param property
   *         the property's expanded name
   * @param language
   *         a language code, {@link #LANGUAGE_UNDEFINED} or {@link #LANGUAGE_ANY}
   */
  boolean hasValue(EName property, String language);

  /**
   * Check if a property has at least on value without language information assigned. Like a call of
   * {@link #hasValue(EName)} with <code>language = {@link #LANGUAGE_ANY}</code>
   *
   * @param property
   *         the property's expanded name
   */
  boolean hasValue(EName property);

  /**
   * Set a property to the given value, overwriting an existing value in the given language.
   * <p/>
   * Please note that it is not allowed to pass {@link #LANGUAGE_ANY} as <code>language</code>.
   *
   * @param property
   *         the property's expanded name
   * @param value
   *         the value or null to remove all values of the given language for this property
   * @param language
   *         a language code or {@link #LANGUAGE_UNDEFINED}
   */
  void set(EName property, @Nullable String value, String language);

  /**
   * Set a value without language information to a property, overwriting an existing value. This is like calling
   * {@link #set(EName, String, String)} with <code>language = {@link #LANGUAGE_UNDEFINED}</code>
   *
   * @param property
   *         the property's expanded name
   * @param value
   *         the value or null to remove all values of {@link #LANGUAGE_UNDEFINED} for this property
   */
  void set(EName property, @Nullable String value);

  /**
   * Set a property to a value, overwriting an existing value.
   *
   * @param property
   *         the property's expanded name
   * @param value
   *         the value or null to completely remove the property (all values in all languages)
   */
  void set(EName property, @Nullable DublinCoreValue value);

  /**
   * Set a property to a list of values, overwriting any existing.
   *
   * @param property
   *         the property's expanded name
   * @param values
   *         the values or an empty list
   */
  void set(EName property, List<DublinCoreValue> values);

  /**
   * Add a value to a property.
   * <p/>
   * Please note that it is not allowed to pass {@link #LANGUAGE_ANY} as <code>language</code>.
   *
   * @param property
   *         the property's expanded name
   * @param value
   *         the value
   * @param language
   *         a language code or {@link #LANGUAGE_UNDEFINED}
   */
  void add(EName property, String value, String language);

  /**
   * Add a value without language information to a property. This is like calling {@link #add(EName, String, String)}
   * with <code>language = {@link #LANGUAGE_UNDEFINED}</code>
   *
   * @param property
   *         the property's expanded name
   * @param value
   *         the value
   */
  void add(EName property, String value);

  /**
   * Add a value to a property.
   *
   * @param property
   *         the property's expanded name
   * @param value
   *         the value
   */
  void add(EName property, DublinCoreValue value);

  /**
   * Remove values of a property.
   * <ul>
   * <li> {@link #LANGUAGE_ANY}: remove the whole element
   * <li> {@link #LANGUAGE_UNDEFINED}: remove only values with no language information
   * <li>language code: remove values of that language
   * </ul>
   *
   * @param property
   *         the property's expanded name
   * @param language
   *         a language code, {@link #LANGUAGE_UNDEFINED} or {@link #LANGUAGE_ANY}
   */
  void remove(EName property, String language);

  /**
   * Remove a complete property.
   *
   * @param property
   *         the property's expanded name
   */
  void remove(EName property);

  /** Clear the Dublin Core */
  void clear();

  /**
   * Return all contained properties.
   *
   * @return a set of property names
   */
  Set<EName> getProperties();
}
