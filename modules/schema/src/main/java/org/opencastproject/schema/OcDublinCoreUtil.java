/*
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

package org.opencastproject.schema;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;

import java.util.Date;

/** Constructor, converter and encoder functions for {@link org.opencastproject.schema.OcDublinCore}. */
public final class OcDublinCoreUtil {
  private OcDublinCoreUtil() {
  }

  public static DublinCoreValue encodeCreated(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static DublinCoreValue encodeDate(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static DublinCoreValue encodeDateAccepted(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static DublinCoreValue encodeDateCopyrighted(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static DublinCoreValue encodeDateSubmitted(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static DublinCoreValue encodeExtent(Long a) {
    return DublinCoreValue.mk(a.toString());
  }

  /**
   * Create a generic DublinCoreCatalog from an OcDublinCore.
   * Fields are encoded according to the Opencast rules. This class provides functions for each DublinCore
   * property that needs special encoding.
   */
  public static DublinCoreCatalog toCatalog(final OcDublinCore source) {
    // completeness assured by unit test
    final DublinCoreCatalog target = DublinCores.mkOpencastEpisode().getCatalog();
    if (source.getAbstract().isPresent()) target.set(DublinCore.PROPERTY_ABSTRACT, source.getAbstract().get());
    if (source.getAccessRights().isPresent()) target.set(DublinCore.PROPERTY_ACCESS_RIGHTS, source.getAccessRights().get());
    if (source.getAccrualMethod().isPresent()) target.set(DublinCore.PROPERTY_ACCRUAL_METHOD, source.getAccrualMethod().get());
    if (source.getAccrualPeriodicity().isPresent()) target.set(DublinCore.PROPERTY_ACCRUAL_PERIODICITY, source.getAccrualPeriodicity().get());
    if (source.getAccrualPolicy().isPresent()) target.set(DublinCore.PROPERTY_ACCRUAL_POLICY, source.getAccrualPolicy().get());
    if (source.getAlternative().isPresent()) target.set(DublinCore.PROPERTY_ALTERNATIVE, source.getAlternative().get());
    if (source.getAudience().isPresent()) target.set(DublinCore.PROPERTY_AUDIENCE, source.getAudience().get());
    if (source.getAvailable().isPresent()) target.set(DublinCore.PROPERTY_AVAILABLE, source.getAvailable().get());
    if (source.getBibliographicCitation().isPresent()) target.set(DublinCore.PROPERTY_BIBLIOGRAPHIC_CITATION, source.getBibliographicCitation().get());
    if (source.getConformsTo().isPresent()) target.set(DublinCore.PROPERTY_CONFORMS_TO, source.getConformsTo().get());
    if (source.getContributor().isPresent()) target.set(DublinCore.PROPERTY_CONTRIBUTOR, source.getContributor().get());
    if (source.getCoverage().isPresent()) target.set(DublinCore.PROPERTY_COVERAGE, source.getCoverage().get());
    target.set(DublinCore.PROPERTY_CREATED, encodeCreated(source.getCreated()));
    if (source.getCreator().isPresent()) target.set(DublinCore.PROPERTY_CREATOR, source.getCreator().get());
    if (source.getDate().isPresent()) target.set(DublinCore.PROPERTY_DATE, encodeDate(source.getDate().get()));
    if (source.getDateAccepted().isPresent()) target.set(DublinCore.PROPERTY_DATE_ACCEPTED, encodeDateAccepted(source.getDateAccepted().get()));
    if (source.getDateCopyrighted().isPresent())
      target.set(DublinCore.PROPERTY_DATE_COPYRIGHTED, encodeDateCopyrighted(source.getDateCopyrighted().get()));
    if (source.getDateSubmitted().isPresent()) target.set(DublinCore.PROPERTY_DATE_SUBMITTED, encodeDateSubmitted(source.getDateSubmitted().get()));
    if (source.getDescription().isPresent()) target.set(DublinCore.PROPERTY_DESCRIPTION, source.getDescription().get());
    if (source.getEducationLevel().isPresent()) target.set(DublinCore.PROPERTY_EDUCATION_LEVEL, source.getEducationLevel().get());
    if (source.getExtent().isPresent()) target.set(DublinCore.PROPERTY_EXTENT, encodeExtent(source.getExtent().get()));
    if (source.getFormat().isPresent()) target.set(DublinCore.PROPERTY_FORMAT, source.getFormat().get());
    if (source.getHasFormat().isPresent()) target.set(DublinCore.PROPERTY_HAS_FORMAT, source.getHasFormat().get());
    if (source.getHasPart().isPresent()) target.set(DublinCore.PROPERTY_HAS_PART, source.getHasPart().get());
    if (source.getHasVersion().isPresent()) target.set(DublinCore.PROPERTY_HAS_VERSION, source.getHasVersion().get());
    if (source.getIdentifier().isPresent()) target.set(DublinCore.PROPERTY_IDENTIFIER, source.getIdentifier().get());
    if (source.getInstructionalMethod().isPresent()) target.set(DublinCore.PROPERTY_INSTRUCTIONAL_METHOD, source.getInstructionalMethod().get());
    if (source.getIsFormatOf().isPresent()) target.set(DublinCore.PROPERTY_IS_FORMAT_OF, source.getIsFormatOf().get());
    if (source.getIsPartOf().isPresent()) target.set(DublinCore.PROPERTY_IS_PART_OF, source.getIsPartOf().get());
    if (source.getIsReferencedBy().isPresent()) target.set(DublinCore.PROPERTY_IS_REFERENCED_BY, source.getIsReferencedBy().get());
    if (source.getIsReplacedBy().isPresent()) target.set(DublinCore.PROPERTY_IS_REPLACED_BY, source.getIsReplacedBy().get());
    if (source.getIsRequiredBy().isPresent()) target.set(DublinCore.PROPERTY_IS_REQUIRED_BY, source.getIsRequiredBy().get());
    if (source.getIssued().isPresent()) target.set(DublinCore.PROPERTY_ISSUED, source.getIssued().get());
    if (source.getIsVersionOf().isPresent()) target.set(DublinCore.PROPERTY_IS_VERSION_OF, source.getIsVersionOf().get());
    if (source.getLanguage().isPresent()) target.set(DublinCore.PROPERTY_LANGUAGE, source.getLanguage().get());
    if (source.getLicense().isPresent()) target.set(DublinCore.PROPERTY_LICENSE, source.getLicense().get());
    if (source.getMediator().isPresent()) target.set(DublinCore.PROPERTY_MEDIATOR, source.getMediator().get());
    if (source.getMedium().isPresent()) target.set(DublinCore.PROPERTY_MEDIUM, source.getMedium().get());
    if (source.getModified().isPresent()) target.set(DublinCore.PROPERTY_MODIFIED, source.getModified().get());
    if (source.getProvenance().isPresent()) target.set(DublinCore.PROPERTY_PROVENANCE, source.getProvenance().get());
    if (source.getPublisher().isPresent()) target.set(DublinCore.PROPERTY_PUBLISHER, source.getPublisher().get());
    if (source.getReferences().isPresent()) target.set(DublinCore.PROPERTY_REFERENCES, source.getReferences().get());
    if (source.getRelation().isPresent()) target.set(DublinCore.PROPERTY_RELATION, source.getRelation().get());
    if (source.getReplaces().isPresent()) target.set(DublinCore.PROPERTY_REPLACES, source.getReplaces().get());
    if (source.getRequires().isPresent()) target.set(DublinCore.PROPERTY_REQUIRES, source.getRequires().get());
    if (source.getRights().isPresent()) target.set(DublinCore.PROPERTY_RIGHTS, source.getRights().get());
    if (source.getRightsHolder().isPresent()) target.set(DublinCore.PROPERTY_RIGHTS_HOLDER, source.getRightsHolder().get());
    if (source.getSource().isPresent()) target.set(DublinCore.PROPERTY_SOURCE, source.getSource().get());
    if (source.getSpatial().isPresent()) target.set(DublinCore.PROPERTY_SPATIAL, source.getSpatial().get());
    if (source.getSubject().isPresent()) target.set(DublinCore.PROPERTY_SUBJECT, source.getSubject().get());
    if (source.getTableOfContents().isPresent()) target.set(DublinCore.PROPERTY_TABLE_OF_CONTENTS, source.getTableOfContents().get());
    if (source.getTemporal().isPresent()) target.set(DublinCore.PROPERTY_TEMPORAL, source.getTemporal().get());
    target.set(DublinCore.PROPERTY_TITLE, source.getTitle());
    if (source.getType().isPresent()) target.set(DublinCore.PROPERTY_TYPE, source.getType().get());
    if (source.getValid().isPresent()) target.set(DublinCore.PROPERTY_VALID, source.getValid().get());
    return target;
  }

  /** Create a JAXB transfer object from an OcDublinCore. */
  public static JaxbOcDublinCore toJaxb(final OcDublinCore source) {
    // completeness assured by unit test
    final JaxbOcDublinCore target = new JaxbOcDublinCore();
    target.abstrakt = source.getAbstract().orElse(null);
    target.accessRights = source.getAccessRights().orElse(null);
    target.accrualMethod = source.getAccrualMethod().orElse(null);
    target.accrualPeriodicity = source.getAccrualPeriodicity().orElse(null);
    target.accrualPolicy = source.getAccrualPolicy().orElse(null);
    target.alternative = source.getAlternative().orElse(null);
    target.audience = source.getAudience().orElse(null);
    target.available = source.getAvailable().orElse(null);
    target.bibliographicCitation = source.getBibliographicCitation().orElse(null);
    target.conformsTo = source.getConformsTo().orElse(null);
    target.contributor = source.getContributor().orElse(null);
    target.coverage = source.getCoverage().orElse(null);
    target.created = source.getCreated();
    target.creator = source.getCreator().orElse(null);
    target.date = source.getDate().orElse(null);
    target.dateAccepted = source.getDateAccepted().orElse(null);
    target.dateCopyrighted = source.getDateCopyrighted().orElse(null);
    target.dateSubmitted = source.getDateSubmitted().orElse(null);
    target.description = source.getDescription().orElse(null);
    target.educationLevel = source.getEducationLevel().orElse(null);
    target.extent = source.getExtent().orElse(null);
    target.format = source.getFormat().orElse(null);
    target.hasFormat = source.getHasFormat().orElse(null);
    target.hasPart = source.getHasPart().orElse(null);
    target.hasVersion = source.getHasVersion().orElse(null);
    target.identifier = source.getIdentifier().orElse(null);
    target.instructionalMethod = source.getInstructionalMethod().orElse(null);
    target.isFormatOf = source.getIsFormatOf().orElse(null);
    target.isPartOf = source.getIsPartOf().orElse(null);
    target.isReferencedBy = source.getIsReferencedBy().orElse(null);
    target.isReplacedBy = source.getIsReplacedBy().orElse(null);
    target.isRequiredBy = source.getIsRequiredBy().orElse(null);
    target.issued = source.getIssued().orElse(null);
    target.isVersionOf = source.getIsVersionOf().orElse(null);
    target.language = source.getLanguage().orElse(null);
    target.license = source.getLicense().orElse(null);
    target.mediator = source.getMediator().orElse(null);
    target.medium = source.getMedium().orElse(null);
    target.modified = source.getModified().orElse(null);
    target.provenance = source.getProvenance().orElse(null);
    target.publisher = source.getPublisher().orElse(null);
    target.references = source.getReferences().orElse(null);
    target.relation = source.getRelation().orElse(null);
    target.replaces = source.getReplaces().orElse(null);
    target.requires = source.getRequires().orElse(null);
    target.rights = source.getRights().orElse(null);
    target.rightsHolder = source.getRightsHolder().orElse(null);
    target.source = source.getSource().orElse(null);
    target.spatial = source.getSpatial().orElse(null);
    target.subject = source.getSubject().orElse(null);
    target.tableOfContents = source.getTableOfContents().orElse(null);
    target.temporal = source.getTemporal().orElse(null);
    target.title = source.getTitle();
    target.type = source.getType().orElse(null);
    target.valid = source.getValid().orElse(null);
    return target;
  }
}
