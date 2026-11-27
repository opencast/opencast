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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencastproject.schema.OcDublinCoreUtil.encodeCreated;
import static org.opencastproject.schema.test.TestUtil.randomDc;
import static org.opencastproject.util.ReflectionUtil.run;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

public class OcDublinCoreUtilTest {
  @Test
  public void testToCatalog() throws Exception {
    final OcDublinCore source = randomDc();
    final DublinCoreCatalog target = OcDublinCoreUtil.toCatalog(source);
    run(OcDublinCore.class, new OcDublinCore() {
      @Override public Optional<String> getAbstract() {
        assertEquals(source.getAbstract().orElse(null), target.getFirst(DublinCore.PROPERTY_ABSTRACT),
            "abstract copy");
        return null;
      }

      @Override public Optional<String> getAccessRights() {
        assertEquals(source.getAccessRights().orElse(null), target.getFirst(DublinCore.PROPERTY_ACCESS_RIGHTS),
            "accessRights copy");
        return null;
      }

      @Override public Optional<String> getAccrualMethod() {
        assertEquals(source.getAccrualMethod().orElse(null), target.getFirst(DublinCore.PROPERTY_ACCRUAL_METHOD),
            "accrualMethod copy");
        return null;
      }

      @Override public Optional<String> getAccrualPeriodicity() {
        assertEquals(source.getAccrualPeriodicity().orElse(null),
            target.getFirst(DublinCore.PROPERTY_ACCRUAL_PERIODICITY),
            "accrualPeriodicity copy");
        return null;
      }

      @Override public Optional<String> getAccrualPolicy() {
        assertEquals(source.getAccrualPolicy().orElse(null), target.getFirst(DublinCore.PROPERTY_ACCRUAL_POLICY),
            "accrualPolicy copy");
        return null;
      }

      @Override public Optional<String> getAlternative() {
        assertEquals(source.getAlternative().orElse(null), target.getFirst(DublinCore.PROPERTY_ALTERNATIVE),
            "alternative copy");
        return null;
      }

      @Override public Optional<String> getAudience() {
        assertEquals(source.getAudience().orElse(null), target.getFirst(DublinCore.PROPERTY_AUDIENCE),
            "audience copy");
        return null;
      }

      @Override public Optional<String> getAvailable() {
        assertEquals(source.getAvailable().orElse(null), target.getFirst(DublinCore.PROPERTY_AVAILABLE),
            "available copy");
        return null;
      }

      @Override public Optional<String> getBibliographicCitation() {
        assertEquals(source.getBibliographicCitation().orElse(null),
            target.getFirst(DublinCore.PROPERTY_BIBLIOGRAPHIC_CITATION),
            "bibliographicCitation copy");
        return null;
      }

      @Override public Optional<String> getConformsTo() {
        assertEquals(source.getConformsTo().orElse(null), target.getFirst(DublinCore.PROPERTY_CONFORMS_TO),
            "conformsTo copy");
        return null;
      }

      @Override public Optional<String> getContributor() {
        assertEquals(source.getContributor().orElse(null), target.getFirst(DublinCore.PROPERTY_CONTRIBUTOR),
            "contributor copy");
        return null;
      }

      @Override public Optional<String> getCoverage() {
        assertEquals(source.getCoverage().orElse(null), target.getFirst(DublinCore.PROPERTY_COVERAGE),
            "coverage copy");
        return null;
      }

      @Override public Date getCreated() {
        assertEquals(encodeCreated(source.getCreated()), target.getFirstVal(DublinCore.PROPERTY_CREATED),
            "created copy");
        return null;
      }

      @Override public Optional<String> getCreator() {
        assertEquals(source.getCreator().orElse(null), target.getFirst(DublinCore.PROPERTY_CREATOR),
            "creator copy");
        return null;
      }

      @Override public Optional<Date> getDate() {
        assertEquals(source.getDate().map(OcDublinCoreUtil::encodeDate).orElse(null),
            target.getFirstVal(DublinCore.PROPERTY_DATE),
            "date copy");
        return null;
      }

      @Override public Optional<Date> getDateAccepted() {
        assertEquals(source.getDateAccepted().map(OcDublinCoreUtil::encodeDateAccepted).orElse(null),
            target.getFirstVal(DublinCore.PROPERTY_DATE_ACCEPTED),
            "dateAccepted copy");
        return null;
      }

      @Override public Optional<Date> getDateCopyrighted() {
        assertEquals(source.getDateCopyrighted().map(OcDublinCoreUtil::encodeDateCopyrighted).orElse(null),
            target.getFirstVal(DublinCore.PROPERTY_DATE_COPYRIGHTED),
            "dateCopyrighted copy");
        return null;
      }

      @Override public Optional<Date> getDateSubmitted() {
        assertEquals(source.getDateSubmitted().map(OcDublinCoreUtil::encodeDateSubmitted).orElse(null),
            target.getFirstVal(DublinCore.PROPERTY_DATE_SUBMITTED),
            "dateSubmitted copy");
        return null;
      }

      @Override public Optional<String> getDescription() {
        assertEquals(source.getDescription().orElse(null), target.getFirst(DublinCore.PROPERTY_DESCRIPTION),
            "description copy");
        return null;
      }

      @Override public Optional<String> getEducationLevel() {
        assertEquals(source.getEducationLevel().orElse(null), target.getFirst(DublinCore.PROPERTY_EDUCATION_LEVEL),
            "educationLevel copy");
        return null;
      }

      @Override public Optional<Long> getExtent() {
        assertEquals(source.getExtent().orElse(null),
            (Long) Long.parseLong(target.getFirst(DublinCore.PROPERTY_EXTENT)),
            "extent copy");
        return null;
      }

      @Override public Optional<String> getFormat() {
        assertEquals(source.getFormat().orElse(null), target.getFirst(DublinCore.PROPERTY_FORMAT),
            "format copy");
        return null;
      }

      @Override public Optional<String> getHasFormat() {
        assertEquals(source.getHasFormat().orElse(null), target.getFirst(DublinCore.PROPERTY_HAS_FORMAT),
            "hasFormat copy");
        return null;
      }

      @Override public Optional<String> getHasPart() {
        assertEquals(source.getHasPart().orElse(null), target.getFirst(DublinCore.PROPERTY_HAS_PART), "hasPart copy");
        return null;
      }

      @Override public Optional<String> getHasVersion() {
        assertEquals(source.getHasVersion().orElse(null), target.getFirst(DublinCore.PROPERTY_HAS_VERSION),
            "hasVersion copy");
        return null;
      }

      @Override public Optional<String> getIdentifier() {
        assertEquals(source.getIdentifier().orElse(null), target.getFirst(DublinCore.PROPERTY_IDENTIFIER),
            "identifier copy");
        return null;
      }

      @Override public Optional<String> getInstructionalMethod() {
        assertEquals(source.getInstructionalMethod().orElse(null),
            target.getFirst(DublinCore.PROPERTY_INSTRUCTIONAL_METHOD),
            "instructionalMethod copy");
        return null;
      }

      @Override public Optional<String> getIsFormatOf() {
        assertEquals(source.getIsFormatOf().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_FORMAT_OF),
            "isFormatOf copy");
        return null;
      }

      @Override public Optional<String> getIsPartOf() {
        assertEquals(source.getIsPartOf().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_PART_OF),
            "isPartOf copy");
        return null;
      }

      @Override public Optional<String> getIsReferencedBy() {
        assertEquals(source.getIsReferencedBy().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_REFERENCED_BY),
            "isReferencedBy copy");
        return null;
      }

      @Override public Optional<String> getIsReplacedBy() {
        assertEquals(source.getIsReplacedBy().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_REPLACED_BY),
            "isReplacedBy copy");
        return null;
      }

      @Override public Optional<String> getIsRequiredBy() {
        assertEquals(source.getIsRequiredBy().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_REQUIRED_BY),
            "isRequiredBy copy");
        return null;
      }

      @Override public Optional<String> getIssued() {
        assertEquals(source.getIssued().orElse(null), target.getFirst(DublinCore.PROPERTY_ISSUED),
            "issued copy");
        return null;
      }

      @Override public Optional<String> getIsVersionOf() {
        assertEquals(source.getIsVersionOf().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_VERSION_OF),
            "isVersionOf copy");
        return null;
      }

      @Override public Optional<String> getLanguage() {
        assertEquals(source.getLanguage().orElse(null), target.getFirst(DublinCore.PROPERTY_LANGUAGE),
            "language copy");
        return null;
      }

      @Override public Optional<String> getLicense() {
        assertEquals(source.getLicense().orElse(null), target.getFirst(DublinCore.PROPERTY_LICENSE), "license copy");
        return null;
      }

      @Override public Optional<String> getMediator() {
        assertEquals(source.getMediator().orElse(null), target.getFirst(DublinCore.PROPERTY_MEDIATOR), "mediator copy");
        return null;
      }

      @Override public Optional<String> getMedium() {
        assertEquals(source.getMedium().orElse(null), target.getFirst(DublinCore.PROPERTY_MEDIUM), "medium copy");
        return null;
      }

      @Override public Optional<String> getModified() {
        assertEquals(source.getModified().orElse(null), target.getFirst(DublinCore.PROPERTY_MODIFIED), "modified copy");
        return null;
      }

      @Override public Optional<String> getProvenance() {
        assertEquals(source.getProvenance().orElse(null), target.getFirst(DublinCore.PROPERTY_PROVENANCE),
            "provenance copy");
        return null;
      }

      @Override public Optional<String> getPublisher() {
        assertEquals(source.getPublisher().orElse(null), target.getFirst(DublinCore.PROPERTY_PUBLISHER),
            "publisher copy");
        return null;
      }

      @Override public Optional<String> getReferences() {
        assertEquals(source.getReferences().orElse(null), target.getFirst(DublinCore.PROPERTY_REFERENCES),
            "references copy");
        return null;
      }

      @Override public Optional<String> getRelation() {
        assertEquals(source.getRelation().orElse(null), target.getFirst(DublinCore.PROPERTY_RELATION), "relation copy");
        return null;
      }

      @Override public Optional<String> getReplaces() {
        assertEquals(source.getReplaces().orElse(null), target.getFirst(DublinCore.PROPERTY_REPLACES), "replaces copy");
        return null;
      }

      @Override public Optional<String> getRequires() {
        assertEquals(source.getRequires().orElse(null), target.getFirst(DublinCore.PROPERTY_REQUIRES), "requires copy");
        return null;
      }

      @Override public Optional<String> getRights() {
        assertEquals(source.getRights().orElse(null), target.getFirst(DublinCore.PROPERTY_RIGHTS), "rights copy");
        return null;
      }

      @Override public Optional<String> getRightsHolder() {
        assertEquals(source.getRightsHolder().orElse(null), target.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER),
            "rightsHolder copy");
        return null;
      }

      @Override public Optional<String> getSource() {
        assertEquals(source.getSource().orElse(null), target.getFirst(DublinCore.PROPERTY_SOURCE), "source copy");
        return null;
      }

      @Override public Optional<String> getSpatial() {
        assertEquals(source.getSpatial().orElse(null), target.getFirst(DublinCore.PROPERTY_SPATIAL), "spatial copy");
        return null;
      }

      @Override public Optional<String> getSubject() {
        assertEquals(source.getSubject().orElse(null), target.getFirst(DublinCore.PROPERTY_SUBJECT), "subject copy");
        return null;
      }

      @Override public Optional<String> getTableOfContents() {
        assertEquals(source.getTableOfContents().orElse(null), target.getFirst(DublinCore.PROPERTY_TABLE_OF_CONTENTS),
            "tableOfContents copy");
        return null;
      }

      @Override public Optional<String> getTemporal() {
        assertEquals(source.getTemporal().orElse(null), target.getFirst(DublinCore.PROPERTY_TEMPORAL), "temporal copy");
        return null;
      }

      @Override public String getTitle() {
        assertEquals(source.getTitle(), target.getFirst(DublinCore.PROPERTY_TITLE), "title copy");
        return null;
      }

      @Override public Optional<String> getType() {
        assertEquals(source.getType().orElse(null), target.getFirst(DublinCore.PROPERTY_TYPE), "type copy");
        return null;
      }

      @Override public Optional<String> getValid() {
        assertEquals(source.getValid().orElse(null), target.getFirst(DublinCore.PROPERTY_VALID), "valid copy");
        return null;
      }
    });
  }

  @Test
  public void testToJaxb() throws Exception {
    final OcDublinCore source = randomDc();
    final JaxbOcDublinCore target = OcDublinCoreUtil.toJaxb(source);
    run(OcDublinCore.class, new OcDublinCore() {
      @Override public Optional<String> getAbstract() {
        assertEquals(source.getAbstract().orElse(null), target.abstrakt, "abstract copy");
        return null;
      }

      @Override public Optional<String> getAccessRights() {
        assertEquals(source.getAccessRights().orElse(null), target.accessRights, "accessRights copy");
        return null;
      }

      @Override public Optional<String> getAccrualMethod() {
        assertEquals(source.getAccrualMethod().orElse(null), target.accrualMethod, "accrualMethod copy");
        return null;
      }

      @Override public Optional<String> getAccrualPeriodicity() {
        assertEquals(source.getAccrualPeriodicity().orElse(null), target.accrualPeriodicity, "accrualPeriodicity copy");
        return null;
      }

      @Override public Optional<String> getAccrualPolicy() {
        assertEquals(source.getAccrualPolicy().orElse(null), target.accrualPolicy, "accrualPolicy copy");
        return null;
      }

      @Override public Optional<String> getAlternative() {
        assertEquals(source.getAlternative().orElse(null), target.alternative, "alternative copy");
        return null;
      }

      @Override public Optional<String> getAudience() {
        assertEquals(source.getAudience().orElse(null), target.audience, "audience copy");
        return null;
      }

      @Override public Optional<String> getAvailable() {
        assertEquals(source.getAvailable().orElse(null), target.available, "available copy");
        return null;
      }

      @Override public Optional<String> getBibliographicCitation() {
        assertEquals(source.getBibliographicCitation().orElse(null), target.bibliographicCitation,
            "bibliographicCitation copy");
        return null;
      }

      @Override public Optional<String> getConformsTo() {
        assertEquals(source.getConformsTo().orElse(null), target.conformsTo, "conformsTo copy");
        return null;
      }

      @Override public Optional<String> getContributor() {
        assertEquals(source.getContributor().orElse(null), target.contributor, "contributor copy");
        return null;
      }

      @Override public Optional<String> getCoverage() {
        assertEquals(source.getCoverage().orElse(null), target.coverage, "coverage copy");
        return null;
      }

      @Override public Date getCreated() {
        assertEquals(source.getCreated(), target.created, "created copy");
        return null;
      }

      @Override public Optional<String> getCreator() {
        assertEquals(source.getCreator().orElse(null), target.creator, "creator copy");
        return null;
      }

      @Override public Optional<Date> getDate() {
        assertEquals(source.getDate().orElse(null), target.date, "date copy");
        return null;
      }

      @Override public Optional<Date> getDateAccepted() {
        assertEquals(source.getDateAccepted().orElse(null), target.dateAccepted, "dateAccepted copy");
        return null;
      }

      @Override public Optional<Date> getDateCopyrighted() {
        assertEquals(source.getDateCopyrighted().orElse(null), target.dateCopyrighted, "dateCopyrighted copy");
        return null;
      }

      @Override public Optional<Date> getDateSubmitted() {
        assertEquals(source.getDateSubmitted().orElse(null), target.dateSubmitted, "dateSubmitted copy");
        return null;
      }

      @Override public Optional<String> getDescription() {
        assertEquals(source.getDescription().orElse(null), target.description, "description copy");
        return null;
      }

      @Override public Optional<String> getEducationLevel() {
        assertEquals(source.getEducationLevel().orElse(null), target.educationLevel, "educationLevel copy");
        return null;
      }

      @Override public Optional<Long> getExtent() {
        assertEquals(source.getExtent().orElse(null), target.extent, "extent copy");
        return null;
      }

      @Override public Optional<String> getFormat() {
        assertEquals(source.getFormat().orElse(null), target.format, "format copy");
        return null;
      }

      @Override public Optional<String> getHasFormat() {
        assertEquals(source.getHasFormat().orElse(null), target.hasFormat, "hasFormat copy");
        return null;
      }

      @Override public Optional<String> getHasPart() {
        assertEquals(source.getHasPart().orElse(null), target.hasPart, "hasPart copy");
        return null;
      }

      @Override public Optional<String> getHasVersion() {
        assertEquals(source.getHasVersion().orElse(null), target.hasVersion, "hasVersion copy");
        return null;
      }

      @Override public Optional<String> getIdentifier() {
        assertEquals(source.getIdentifier().orElse(null), target.identifier, "identifier copy");
        return null;
      }

      @Override public Optional<String> getInstructionalMethod() {
        assertEquals(source.getInstructionalMethod().orElse(null), target.instructionalMethod,
            "instructionalMethod copy");
        return null;
      }

      @Override public Optional<String> getIsFormatOf() {
        assertEquals(source.getIsFormatOf().orElse(null), target.isFormatOf, "isFormatOf copy");
        return null;
      }

      @Override public Optional<String> getIsPartOf() {
        assertEquals(source.getIsPartOf().orElse(null), target.isPartOf, "isPartOf copy");
        return null;
      }

      @Override public Optional<String> getIsReferencedBy() {
        assertEquals(source.getIsReferencedBy().orElse(null), target.isReferencedBy, "isReferencedBy copy");
        return null;
      }

      @Override public Optional<String> getIsReplacedBy() {
        assertEquals(source.getIsReplacedBy().orElse(null), target.isReplacedBy, "isReplacedBy copy");
        return null;
      }

      @Override public Optional<String> getIsRequiredBy() {
        assertEquals(source.getIsRequiredBy().orElse(null), target.isRequiredBy, "isRequiredBy copy");
        return null;
      }

      @Override public Optional<String> getIssued() {
        assertEquals(source.getIssued().orElse(null), target.issued, "issued copy");
        return null;
      }

      @Override public Optional<String> getIsVersionOf() {
        assertEquals(source.getIsVersionOf().orElse(null), target.isVersionOf, "isVersionOf copy");
        return null;
      }

      @Override public Optional<String> getLanguage() {
        assertEquals(source.getLanguage().orElse(null), target.language, "language copy");
        return null;
      }

      @Override public Optional<String> getLicense() {
        assertEquals(source.getLicense().orElse(null), target.license, "license copy");
        return null;
      }

      @Override public Optional<String> getMediator() {
        assertEquals(source.getMediator().orElse(null), target.mediator, "mediator copy");
        return null;
      }

      @Override public Optional<String> getMedium() {
        assertEquals(source.getMedium().orElse(null), target.medium, "medium copy");
        return null;
      }

      @Override public Optional<String> getModified() {
        assertEquals(source.getModified().orElse(null), target.modified, "modified copy");
        return null;
      }

      @Override public Optional<String> getProvenance() {
        assertEquals(source.getProvenance().orElse(null), target.provenance, "provenance copy");
        return null;
      }

      @Override public Optional<String> getPublisher() {
        assertEquals(source.getPublisher().orElse(null), target.publisher, "publisher copy");
        return null;
      }

      @Override public Optional<String> getReferences() {
        assertEquals(source.getReferences().orElse(null), target.references, "references copy");
        return null;
      }

      @Override public Optional<String> getRelation() {
        assertEquals(source.getRelation().orElse(null), target.relation, "relation copy");
        return null;
      }

      @Override public Optional<String> getReplaces() {
        assertEquals(source.getReplaces().orElse(null), target.replaces, "replaces copy");
        return null;
      }

      @Override public Optional<String> getRequires() {
        assertEquals(source.getRequires().orElse(null), target.requires, "requires copy");
        return null;
      }

      @Override public Optional<String> getRights() {
        assertEquals(source.getRights().orElse(null), target.rights, "rights copy");
        return null;
      }

      @Override public Optional<String> getRightsHolder() {
        assertEquals(source.getRightsHolder().orElse(null), target.rightsHolder, "rightsHolder copy");
        return null;
      }

      @Override public Optional<String> getSource() {
        assertEquals(source.getSource().orElse(null), target.source, "source copy");
        return null;
      }

      @Override public Optional<String> getSpatial() {
        assertEquals(source.getSpatial().orElse(null), target.spatial, "spatial copy");
        return null;
      }

      @Override public Optional<String> getSubject() {
        assertEquals(source.getSubject().orElse(null), target.subject, "subject copy");
        return null;
      }

      @Override public Optional<String> getTableOfContents() {
        assertEquals(source.getTableOfContents().orElse(null), target.tableOfContents, "tableOfContents copy");
        return null;
      }

      @Override public Optional<String> getTemporal() {
        assertEquals(source.getTemporal().orElse(null), target.temporal, "temporal copy");
        return null;
      }

      @Override public String getTitle() {
        assertEquals(source.getTitle(), target.title, "title copy");
        return null;
      }

      @Override public Optional<String> getType() {
        assertEquals(source.getType().orElse(null), target.type, "type copy");
        return null;
      }

      @Override public Optional<String> getValid() {
        assertEquals(source.getValid().orElse(null), target.valid, "valid copy");
        return null;
      }
    });
  }
}
