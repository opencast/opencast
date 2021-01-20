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

package org.opencastproject.schema;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.util.data.Function;

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

  public static final Function<Date, DublinCoreValue> encodeDate = new Function<Date, DublinCoreValue>() {
    @Override public DublinCoreValue apply(Date a) {
      return encodeDate(a);
    }
  };

  public static DublinCoreValue encodeDateAccepted(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static final Function<Date, DublinCoreValue> encodeDateAccepted = new Function<Date, DublinCoreValue>() {
    @Override public DublinCoreValue apply(Date a) {
      return encodeDateAccepted(a);
    }
  };

  public static DublinCoreValue encodeDateCopyrighted(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static final Function<Date, DublinCoreValue> encodeDateCopyrighted = new Function<Date, DublinCoreValue>() {
    @Override public DublinCoreValue apply(Date a) {
      return encodeDateCopyrighted(a);
    }
  };

  public static DublinCoreValue encodeDateSubmitted(Date a) {
    return EncodingSchemeUtils.encodeDate(a, Precision.Second);
  }

  public static final Function<Date, DublinCoreValue> encodeDateSubmitted = new Function<Date, DublinCoreValue>() {
    @Override public DublinCoreValue apply(Date a) {
      return encodeDateSubmitted(a);
    }
  };

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
    for (String a : source.getAbstract()) target.set(DublinCore.PROPERTY_ABSTRACT, a);
    for (String a : source.getAccessRights()) target.set(DublinCore.PROPERTY_ACCESS_RIGHTS, a);
    for (String a : source.getAccrualMethod()) target.set(DublinCore.PROPERTY_ACCRUAL_METHOD, a);
    for (String a : source.getAccrualPeriodicity()) target.set(DublinCore.PROPERTY_ACCRUAL_PERIODICITY, a);
    for (String a : source.getAccrualPolicy()) target.set(DublinCore.PROPERTY_ACCRUAL_POLICY, a);
    for (String a : source.getAlternative()) target.set(DublinCore.PROPERTY_ALTERNATIVE, a);
    for (String a : source.getAudience()) target.set(DublinCore.PROPERTY_AUDIENCE, a);
    for (String a : source.getAvailable()) target.set(DublinCore.PROPERTY_AVAILABLE, a);
    for (String a : source.getBibliographicCitation()) target.set(DublinCore.PROPERTY_BIBLIOGRAPHIC_CITATION, a);
    for (String a : source.getConformsTo()) target.set(DublinCore.PROPERTY_CONFORMS_TO, a);
    for (String a : source.getContributor()) target.set(DublinCore.PROPERTY_CONTRIBUTOR, a);
    for (String a : source.getCoverage()) target.set(DublinCore.PROPERTY_COVERAGE, a);
    target.set(DublinCore.PROPERTY_CREATED, encodeCreated(source.getCreated()));
    for (String a : source.getCreator()) target.set(DublinCore.PROPERTY_CREATOR, a);
    for (Date a : source.getDate()) target.set(DublinCore.PROPERTY_DATE, encodeDate(a));
    for (Date a : source.getDateAccepted()) target.set(DublinCore.PROPERTY_DATE_ACCEPTED, encodeDateAccepted(a));
    for (Date a : source.getDateCopyrighted())
      target.set(DublinCore.PROPERTY_DATE_COPYRIGHTED, encodeDateCopyrighted(a));
    for (Date a : source.getDateSubmitted()) target.set(DublinCore.PROPERTY_DATE_SUBMITTED, encodeDateSubmitted(a));
    for (String a : source.getDescription()) target.set(DublinCore.PROPERTY_DESCRIPTION, a);
    for (String a : source.getEducationLevel()) target.set(DublinCore.PROPERTY_EDUCATION_LEVEL, a);
    for (Long a : source.getExtent()) target.set(DublinCore.PROPERTY_EXTENT, encodeExtent(a));
    for (String a : source.getFormat()) target.set(DublinCore.PROPERTY_FORMAT, a);
    for (String a : source.getHasFormat()) target.set(DublinCore.PROPERTY_HAS_FORMAT, a);
    for (String a : source.getHasPart()) target.set(DublinCore.PROPERTY_HAS_PART, a);
    for (String a : source.getHasVersion()) target.set(DublinCore.PROPERTY_HAS_VERSION, a);
    for (String a : source.getIdentifier()) target.set(DublinCore.PROPERTY_IDENTIFIER, a);
    for (String a : source.getInstructionalMethod()) target.set(DublinCore.PROPERTY_INSTRUCTIONAL_METHOD, a);
    for (String a : source.getIsFormatOf()) target.set(DublinCore.PROPERTY_IS_FORMAT_OF, a);
    for (String a : source.getIsPartOf()) target.set(DublinCore.PROPERTY_IS_PART_OF, a);
    for (String a : source.getIsReferencedBy()) target.set(DublinCore.PROPERTY_IS_REFERENCED_BY, a);
    for (String a : source.getIsReplacedBy()) target.set(DublinCore.PROPERTY_IS_REPLACED_BY, a);
    for (String a : source.getIsRequiredBy()) target.set(DublinCore.PROPERTY_IS_REQUIRED_BY, a);
    for (String a : source.getIssued()) target.set(DublinCore.PROPERTY_ISSUED, a);
    for (String a : source.getIsVersionOf()) target.set(DublinCore.PROPERTY_IS_VERSION_OF, a);
    for (String a : source.getLanguage()) target.set(DublinCore.PROPERTY_LANGUAGE, a);
    for (String a : source.getLicense()) target.set(DublinCore.PROPERTY_LICENSE, a);
    for (String a : source.getMediator()) target.set(DublinCore.PROPERTY_MEDIATOR, a);
    for (String a : source.getMedium()) target.set(DublinCore.PROPERTY_MEDIUM, a);
    for (String a : source.getModified()) target.set(DublinCore.PROPERTY_MODIFIED, a);
    for (String a : source.getProvenance()) target.set(DublinCore.PROPERTY_PROVENANCE, a);
    for (String a : source.getPublisher()) target.set(DublinCore.PROPERTY_PUBLISHER, a);
    for (String a : source.getReferences()) target.set(DublinCore.PROPERTY_REFERENCES, a);
    for (String a : source.getRelation()) target.set(DublinCore.PROPERTY_RELATION, a);
    for (String a : source.getReplaces()) target.set(DublinCore.PROPERTY_REPLACES, a);
    for (String a : source.getRequires()) target.set(DublinCore.PROPERTY_REQUIRES, a);
    for (String a : source.getRights()) target.set(DublinCore.PROPERTY_RIGHTS, a);
    for (String a : source.getRightsHolder()) target.set(DublinCore.PROPERTY_RIGHTS_HOLDER, a);
    for (String a : source.getSource()) target.set(DublinCore.PROPERTY_SOURCE, a);
    for (String a : source.getSpatial()) target.set(DublinCore.PROPERTY_SPATIAL, a);
    for (String a : source.getSubject()) target.set(DublinCore.PROPERTY_SUBJECT, a);
    for (String a : source.getTableOfContents()) target.set(DublinCore.PROPERTY_TABLE_OF_CONTENTS, a);
    for (String a : source.getTemporal()) target.set(DublinCore.PROPERTY_TEMPORAL, a);
    target.set(DublinCore.PROPERTY_TITLE, source.getTitle());
    for (String a : source.getType()) target.set(DublinCore.PROPERTY_TYPE, a);
    for (String a : source.getValid()) target.set(DublinCore.PROPERTY_VALID, a);
    return target;
  }

  /** Create a JAXB transfer object from an OcDublinCore. */
  public static JaxbOcDublinCore toJaxb(final OcDublinCore source) {
    // completeness assured by unit test
    final JaxbOcDublinCore target = new JaxbOcDublinCore();
    target.abstrakt = source.getAbstract().getOrElseNull();
    target.accessRights = source.getAccessRights().getOrElseNull();
    target.accrualMethod = source.getAccrualMethod().getOrElseNull();
    target.accrualPeriodicity = source.getAccrualPeriodicity().getOrElseNull();
    target.accrualPolicy = source.getAccrualPolicy().getOrElseNull();
    target.alternative = source.getAlternative().getOrElseNull();
    target.audience = source.getAudience().getOrElseNull();
    target.available = source.getAvailable().getOrElseNull();
    target.bibliographicCitation = source.getBibliographicCitation().getOrElseNull();
    target.conformsTo = source.getConformsTo().getOrElseNull();
    target.contributor = source.getContributor().getOrElseNull();
    target.coverage = source.getCoverage().getOrElseNull();
    target.created = source.getCreated();
    target.creator = source.getCreator().getOrElseNull();
    target.date = source.getDate().getOrElseNull();
    target.dateAccepted = source.getDateAccepted().getOrElseNull();
    target.dateCopyrighted = source.getDateCopyrighted().getOrElseNull();
    target.dateSubmitted = source.getDateSubmitted().getOrElseNull();
    target.description = source.getDescription().getOrElseNull();
    target.educationLevel = source.getEducationLevel().getOrElseNull();
    target.extent = source.getExtent().getOrElseNull();
    target.format = source.getFormat().getOrElseNull();
    target.hasFormat = source.getHasFormat().getOrElseNull();
    target.hasPart = source.getHasPart().getOrElseNull();
    target.hasVersion = source.getHasVersion().getOrElseNull();
    target.identifier = source.getIdentifier().getOrElseNull();
    target.instructionalMethod = source.getInstructionalMethod().getOrElseNull();
    target.isFormatOf = source.getIsFormatOf().getOrElseNull();
    target.isPartOf = source.getIsPartOf().getOrElseNull();
    target.isReferencedBy = source.getIsReferencedBy().getOrElseNull();
    target.isReplacedBy = source.getIsReplacedBy().getOrElseNull();
    target.isRequiredBy = source.getIsRequiredBy().getOrElseNull();
    target.issued = source.getIssued().getOrElseNull();
    target.isVersionOf = source.getIsVersionOf().getOrElseNull();
    target.language = source.getLanguage().getOrElseNull();
    target.license = source.getLicense().getOrElseNull();
    target.mediator = source.getMediator().getOrElseNull();
    target.medium = source.getMedium().getOrElseNull();
    target.modified = source.getModified().getOrElseNull();
    target.provenance = source.getProvenance().getOrElseNull();
    target.publisher = source.getPublisher().getOrElseNull();
    target.references = source.getReferences().getOrElseNull();
    target.relation = source.getRelation().getOrElseNull();
    target.replaces = source.getReplaces().getOrElseNull();
    target.requires = source.getRequires().getOrElseNull();
    target.rights = source.getRights().getOrElseNull();
    target.rightsHolder = source.getRightsHolder().getOrElseNull();
    target.source = source.getSource().getOrElseNull();
    target.spatial = source.getSpatial().getOrElseNull();
    target.subject = source.getSubject().getOrElseNull();
    target.tableOfContents = source.getTableOfContents().getOrElseNull();
    target.temporal = source.getTemporal().getOrElseNull();
    target.title = source.getTitle();
    target.type = source.getType().getOrElseNull();
    target.valid = source.getValid().getOrElseNull();
    return target;
  }
}
