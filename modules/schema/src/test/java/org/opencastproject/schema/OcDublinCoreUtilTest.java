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

import static org.junit.Assert.assertEquals;
import static org.opencastproject.schema.OcDublinCoreUtil.encodeCreated;
import static org.opencastproject.schema.test.TestUtil.randomDc;
import static org.opencastproject.util.ReflectionUtil.run;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;

import org.junit.Test;

import java.util.Date;
import java.util.Optional;

public class OcDublinCoreUtilTest {
  @Test
  public void testToCatalog() throws Exception {
    final OcDublinCore source = randomDc();
    final DublinCoreCatalog target = OcDublinCoreUtil.toCatalog(source);
    run(OcDublinCore.class, new OcDublinCore() {
      @Override public Optional<String> getAbstract() {
        assertEquals("abstract copy", source.getAbstract().orElse(null), target.getFirst(DublinCore.PROPERTY_ABSTRACT));
        return null;
      }

      @Override public Optional<String> getAccessRights() {
        assertEquals("accessRights copy", source.getAccessRights().orElse(null), target.getFirst(DublinCore.PROPERTY_ACCESS_RIGHTS));
        return null;
      }

      @Override public Optional<String> getAccrualMethod() {
        assertEquals("accrualMethod copy", source.getAccrualMethod().orElse(null), target.getFirst(DublinCore.PROPERTY_ACCRUAL_METHOD));
        return null;
      }

      @Override public Optional<String> getAccrualPeriodicity() {
        assertEquals("accrualPeriodicity copy", source.getAccrualPeriodicity().orElse(null), target.getFirst(DublinCore.PROPERTY_ACCRUAL_PERIODICITY));
        return null;
      }

      @Override public Optional<String> getAccrualPolicy() {
        assertEquals("accrualPolicy copy", source.getAccrualPolicy().orElse(null), target.getFirst(DublinCore.PROPERTY_ACCRUAL_POLICY));
        return null;
      }

      @Override public Optional<String> getAlternative() {
        assertEquals("alternative copy", source.getAlternative().orElse(null), target.getFirst(DublinCore.PROPERTY_ALTERNATIVE));
        return null;
      }

      @Override public Optional<String> getAudience() {
        assertEquals("audience copy", source.getAudience().orElse(null), target.getFirst(DublinCore.PROPERTY_AUDIENCE));
        return null;
      }

      @Override public Optional<String> getAvailable() {
        assertEquals("available copy", source.getAvailable().orElse(null), target.getFirst(DublinCore.PROPERTY_AVAILABLE));
        return null;
      }

      @Override public Optional<String> getBibliographicCitation() {
        assertEquals("bibliographicCitation copy", source.getBibliographicCitation().orElse(null), target.getFirst(DublinCore.PROPERTY_BIBLIOGRAPHIC_CITATION));
        return null;
      }

      @Override public Optional<String> getConformsTo() {
        assertEquals("conformsTo copy", source.getConformsTo().orElse(null), target.getFirst(DublinCore.PROPERTY_CONFORMS_TO));
        return null;
      }

      @Override public Optional<String> getContributor() {
        assertEquals("contributor copy", source.getContributor().orElse(null), target.getFirst(DublinCore.PROPERTY_CONTRIBUTOR));
        return null;
      }

      @Override public Optional<String> getCoverage() {
        assertEquals("coverage copy", source.getCoverage().orElse(null), target.getFirst(DublinCore.PROPERTY_COVERAGE));
        return null;
      }

      @Override public Date getCreated() {
        assertEquals("created copy", encodeCreated(source.getCreated()), target.getFirstVal(DublinCore.PROPERTY_CREATED));
        return null;
      }

      @Override public Optional<String> getCreator() {
        assertEquals("creator copy", source.getCreator().orElse(null), target.getFirst(DublinCore.PROPERTY_CREATOR));
        return null;
      }

      @Override public Optional<Date> getDate() {
        assertEquals("date copy", source.getDate().map(OcDublinCoreUtil::encodeDate).orElse(null), target.getFirstVal(DublinCore.PROPERTY_DATE));
        return null;
      }

      @Override public Optional<Date> getDateAccepted() {
        assertEquals("dateAccepted copy", source.getDateAccepted().map(OcDublinCoreUtil::encodeDateAccepted).orElse(null), target.getFirstVal(DublinCore.PROPERTY_DATE_ACCEPTED));
        return null;
      }

      @Override public Optional<Date> getDateCopyrighted() {
        assertEquals("dateCopyrighted copy", source.getDateCopyrighted().map(OcDublinCoreUtil::encodeDateCopyrighted).orElse(null), target.getFirstVal(DublinCore.PROPERTY_DATE_COPYRIGHTED));
        return null;
      }

      @Override public Optional<Date> getDateSubmitted() {
        assertEquals("dateSubmitted copy", source.getDateSubmitted().map(OcDublinCoreUtil::encodeDateSubmitted).orElse(null), target.getFirstVal(DublinCore.PROPERTY_DATE_SUBMITTED));
        return null;
      }

      @Override public Optional<String> getDescription() {
        assertEquals("description copy", source.getDescription().orElse(null), target.getFirst(DublinCore.PROPERTY_DESCRIPTION));
        return null;
      }

      @Override public Optional<String> getEducationLevel() {
        assertEquals("educationLevel copy", source.getEducationLevel().orElse(null), target.getFirst(DublinCore.PROPERTY_EDUCATION_LEVEL));
        return null;
      }

      @Override public Optional<Long> getExtent() {
        assertEquals("extent copy", source.getExtent().orElse(null), (Long) Long.parseLong(target.getFirst(DublinCore.PROPERTY_EXTENT)));
        return null;
      }

      @Override public Optional<String> getFormat() {
        assertEquals("format copy", source.getFormat().orElse(null), target.getFirst(DublinCore.PROPERTY_FORMAT));
        return null;
      }

      @Override public Optional<String> getHasFormat() {
        assertEquals("hasFormat copy", source.getHasFormat().orElse(null), target.getFirst(DublinCore.PROPERTY_HAS_FORMAT));
        return null;
      }

      @Override public Optional<String> getHasPart() {
        assertEquals("hasPart copy", source.getHasPart().orElse(null), target.getFirst(DublinCore.PROPERTY_HAS_PART));
        return null;
      }

      @Override public Optional<String> getHasVersion() {
        assertEquals("hasVersion copy", source.getHasVersion().orElse(null), target.getFirst(DublinCore.PROPERTY_HAS_VERSION));
        return null;
      }

      @Override public Optional<String> getIdentifier() {
        assertEquals("identifier copy", source.getIdentifier().orElse(null), target.getFirst(DublinCore.PROPERTY_IDENTIFIER));
        return null;
      }

      @Override public Optional<String> getInstructionalMethod() {
        assertEquals("instructionalMethod copy", source.getInstructionalMethod().orElse(null), target.getFirst(DublinCore.PROPERTY_INSTRUCTIONAL_METHOD));
        return null;
      }

      @Override public Optional<String> getIsFormatOf() {
        assertEquals("isFormatOf copy", source.getIsFormatOf().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_FORMAT_OF));
        return null;
      }

      @Override public Optional<String> getIsPartOf() {
        assertEquals("isPartOf copy", source.getIsPartOf().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_PART_OF));
        return null;
      }

      @Override public Optional<String> getIsReferencedBy() {
        assertEquals("isReferencedBy copy", source.getIsReferencedBy().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_REFERENCED_BY));
        return null;
      }

      @Override public Optional<String> getIsReplacedBy() {
        assertEquals("isReplacedBy copy", source.getIsReplacedBy().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_REPLACED_BY));
        return null;
      }

      @Override public Optional<String> getIsRequiredBy() {
        assertEquals("isRequiredBy copy", source.getIsRequiredBy().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_REQUIRED_BY));
        return null;
      }

      @Override public Optional<String> getIssued() {
        assertEquals("issued copy", source.getIssued().orElse(null), target.getFirst(DublinCore.PROPERTY_ISSUED));
        return null;
      }

      @Override public Optional<String> getIsVersionOf() {
        assertEquals("isVersionOf copy", source.getIsVersionOf().orElse(null), target.getFirst(DublinCore.PROPERTY_IS_VERSION_OF));
        return null;
      }

      @Override public Optional<String> getLanguage() {
        assertEquals("language copy", source.getLanguage().orElse(null), target.getFirst(DublinCore.PROPERTY_LANGUAGE));
        return null;
      }

      @Override public Optional<String> getLicense() {
        assertEquals("license copy", source.getLicense().orElse(null), target.getFirst(DublinCore.PROPERTY_LICENSE));
        return null;
      }

      @Override public Optional<String> getMediator() {
        assertEquals("mediator copy", source.getMediator().orElse(null), target.getFirst(DublinCore.PROPERTY_MEDIATOR));
        return null;
      }

      @Override public Optional<String> getMedium() {
        assertEquals("medium copy", source.getMedium().orElse(null), target.getFirst(DublinCore.PROPERTY_MEDIUM));
        return null;
      }

      @Override public Optional<String> getModified() {
        assertEquals("modified copy", source.getModified().orElse(null), target.getFirst(DublinCore.PROPERTY_MODIFIED));
        return null;
      }

      @Override public Optional<String> getProvenance() {
        assertEquals("provenance copy", source.getProvenance().orElse(null), target.getFirst(DublinCore.PROPERTY_PROVENANCE));
        return null;
      }

      @Override public Optional<String> getPublisher() {
        assertEquals("publisher copy", source.getPublisher().orElse(null), target.getFirst(DublinCore.PROPERTY_PUBLISHER));
        return null;
      }

      @Override public Optional<String> getReferences() {
        assertEquals("references copy", source.getReferences().orElse(null), target.getFirst(DublinCore.PROPERTY_REFERENCES));
        return null;
      }

      @Override public Optional<String> getRelation() {
        assertEquals("relation copy", source.getRelation().orElse(null), target.getFirst(DublinCore.PROPERTY_RELATION));
        return null;
      }

      @Override public Optional<String> getReplaces() {
        assertEquals("replaces copy", source.getReplaces().orElse(null), target.getFirst(DublinCore.PROPERTY_REPLACES));
        return null;
      }

      @Override public Optional<String> getRequires() {
        assertEquals("requires copy", source.getRequires().orElse(null), target.getFirst(DublinCore.PROPERTY_REQUIRES));
        return null;
      }

      @Override public Optional<String> getRights() {
        assertEquals("rights copy", source.getRights().orElse(null), target.getFirst(DublinCore.PROPERTY_RIGHTS));
        return null;
      }

      @Override public Optional<String> getRightsHolder() {
        assertEquals("rightsHolder copy", source.getRightsHolder().orElse(null), target.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER));
        return null;
      }

      @Override public Optional<String> getSource() {
        assertEquals("source copy", source.getSource().orElse(null), target.getFirst(DublinCore.PROPERTY_SOURCE));
        return null;
      }

      @Override public Optional<String> getSpatial() {
        assertEquals("spatial copy", source.getSpatial().orElse(null), target.getFirst(DublinCore.PROPERTY_SPATIAL));
        return null;
      }

      @Override public Optional<String> getSubject() {
        assertEquals("subject copy", source.getSubject().orElse(null), target.getFirst(DublinCore.PROPERTY_SUBJECT));
        return null;
      }

      @Override public Optional<String> getTableOfContents() {
        assertEquals("tableOfContents copy", source.getTableOfContents().orElse(null), target.getFirst(DublinCore.PROPERTY_TABLE_OF_CONTENTS));
        return null;
      }

      @Override public Optional<String> getTemporal() {
        assertEquals("temporal copy", source.getTemporal().orElse(null), target.getFirst(DublinCore.PROPERTY_TEMPORAL));
        return null;
      }

      @Override public String getTitle() {
        assertEquals("title copy", source.getTitle(), target.getFirst(DublinCore.PROPERTY_TITLE));
        return null;
      }

      @Override public Optional<String> getType() {
        assertEquals("type copy", source.getType().orElse(null), target.getFirst(DublinCore.PROPERTY_TYPE));
        return null;
      }

      @Override public Optional<String> getValid() {
        assertEquals("valid copy", source.getValid().orElse(null), target.getFirst(DublinCore.PROPERTY_VALID));
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
        assertEquals("abstract copy", source.getAbstract().orElse(null), target.abstrakt);
        return null;
      }

      @Override public Optional<String> getAccessRights() {
        assertEquals("accessRights copy", source.getAccessRights().orElse(null), target.accessRights);
        return null;
      }

      @Override public Optional<String> getAccrualMethod() {
        assertEquals("accrualMethod copy", source.getAccrualMethod().orElse(null), target.accrualMethod);
        return null;
      }

      @Override public Optional<String> getAccrualPeriodicity() {
        assertEquals("accrualPeriodicity copy", source.getAccrualPeriodicity().orElse(null), target.accrualPeriodicity);
        return null;
      }

      @Override public Optional<String> getAccrualPolicy() {
        assertEquals("accrualPolicy copy", source.getAccrualPolicy().orElse(null), target.accrualPolicy);
        return null;
      }

      @Override public Optional<String> getAlternative() {
        assertEquals("alternative copy", source.getAlternative().orElse(null), target.alternative);
        return null;
      }

      @Override public Optional<String> getAudience() {
        assertEquals("audience copy", source.getAudience().orElse(null), target.audience);
        return null;
      }

      @Override public Optional<String> getAvailable() {
        assertEquals("available copy", source.getAvailable().orElse(null), target.available);
        return null;
      }

      @Override public Optional<String> getBibliographicCitation() {
        assertEquals("bibliographicCitation copy", source.getBibliographicCitation().orElse(null), target.bibliographicCitation);
        return null;
      }

      @Override public Optional<String> getConformsTo() {
        assertEquals("conformsTo copy", source.getConformsTo().orElse(null), target.conformsTo);
        return null;
      }

      @Override public Optional<String> getContributor() {
        assertEquals("contributor copy", source.getContributor().orElse(null), target.contributor);
        return null;
      }

      @Override public Optional<String> getCoverage() {
        assertEquals("coverage copy", source.getCoverage().orElse(null), target.coverage);
        return null;
      }

      @Override public Date getCreated() {
        assertEquals("created copy", source.getCreated(), target.created);
        return null;
      }

      @Override public Optional<String> getCreator() {
        assertEquals("creator copy", source.getCreator().orElse(null), target.creator);
        return null;
      }

      @Override public Optional<Date> getDate() {
        assertEquals("date copy", source.getDate().orElse(null), target.date);
        return null;
      }

      @Override public Optional<Date> getDateAccepted() {
        assertEquals("dateAccepted copy", source.getDateAccepted().orElse(null), target.dateAccepted);
        return null;
      }

      @Override public Optional<Date> getDateCopyrighted() {
        assertEquals("dateCopyrighted copy", source.getDateCopyrighted().orElse(null), target.dateCopyrighted);
        return null;
      }

      @Override public Optional<Date> getDateSubmitted() {
        assertEquals("dateSubmitted copy", source.getDateSubmitted().orElse(null), target.dateSubmitted);
        return null;
      }

      @Override public Optional<String> getDescription() {
        assertEquals("description copy", source.getDescription().orElse(null), target.description);
        return null;
      }

      @Override public Optional<String> getEducationLevel() {
        assertEquals("educationLevel copy", source.getEducationLevel().orElse(null), target.educationLevel);
        return null;
      }

      @Override public Optional<Long> getExtent() {
        assertEquals("extent copy", source.getExtent().orElse(null), target.extent);
        return null;
      }

      @Override public Optional<String> getFormat() {
        assertEquals("format copy", source.getFormat().orElse(null), target.format);
        return null;
      }

      @Override public Optional<String> getHasFormat() {
        assertEquals("hasFormat copy", source.getHasFormat().orElse(null), target.hasFormat);
        return null;
      }

      @Override public Optional<String> getHasPart() {
        assertEquals("hasPart copy", source.getHasPart().orElse(null), target.hasPart);
        return null;
      }

      @Override public Optional<String> getHasVersion() {
        assertEquals("hasVersion copy", source.getHasVersion().orElse(null), target.hasVersion);
        return null;
      }

      @Override public Optional<String> getIdentifier() {
        assertEquals("identifier copy", source.getIdentifier().orElse(null), target.identifier);
        return null;
      }

      @Override public Optional<String> getInstructionalMethod() {
        assertEquals("instructionalMethod copy", source.getInstructionalMethod().orElse(null), target.instructionalMethod);
        return null;
      }

      @Override public Optional<String> getIsFormatOf() {
        assertEquals("isFormatOf copy", source.getIsFormatOf().orElse(null), target.isFormatOf);
        return null;
      }

      @Override public Optional<String> getIsPartOf() {
        assertEquals("isPartOf copy", source.getIsPartOf().orElse(null), target.isPartOf);
        return null;
      }

      @Override public Optional<String> getIsReferencedBy() {
        assertEquals("isReferencedBy copy", source.getIsReferencedBy().orElse(null), target.isReferencedBy);
        return null;
      }

      @Override public Optional<String> getIsReplacedBy() {
        assertEquals("isReplacedBy copy", source.getIsReplacedBy().orElse(null), target.isReplacedBy);
        return null;
      }

      @Override public Optional<String> getIsRequiredBy() {
        assertEquals("isRequiredBy copy", source.getIsRequiredBy().orElse(null), target.isRequiredBy);
        return null;
      }

      @Override public Optional<String> getIssued() {
        assertEquals("issued copy", source.getIssued().orElse(null), target.issued);
        return null;
      }

      @Override public Optional<String> getIsVersionOf() {
        assertEquals("isVersionOf copy", source.getIsVersionOf().orElse(null), target.isVersionOf);
        return null;
      }

      @Override public Optional<String> getLanguage() {
        assertEquals("language copy", source.getLanguage().orElse(null), target.language);
        return null;
      }

      @Override public Optional<String> getLicense() {
        assertEquals("license copy", source.getLicense().orElse(null), target.license);
        return null;
      }

      @Override public Optional<String> getMediator() {
        assertEquals("mediator copy", source.getMediator().orElse(null), target.mediator);
        return null;
      }

      @Override public Optional<String> getMedium() {
        assertEquals("medium copy", source.getMedium().orElse(null), target.medium);
        return null;
      }

      @Override public Optional<String> getModified() {
        assertEquals("modified copy", source.getModified().orElse(null), target.modified);
        return null;
      }

      @Override public Optional<String> getProvenance() {
        assertEquals("provenance copy", source.getProvenance().orElse(null), target.provenance);
        return null;
      }

      @Override public Optional<String> getPublisher() {
        assertEquals("publisher copy", source.getPublisher().orElse(null), target.publisher);
        return null;
      }

      @Override public Optional<String> getReferences() {
        assertEquals("references copy", source.getReferences().orElse(null), target.references);
        return null;
      }

      @Override public Optional<String> getRelation() {
        assertEquals("relation copy", source.getRelation().orElse(null), target.relation);
        return null;
      }

      @Override public Optional<String> getReplaces() {
        assertEquals("replaces copy", source.getReplaces().orElse(null), target.replaces);
        return null;
      }

      @Override public Optional<String> getRequires() {
        assertEquals("requires copy", source.getRequires().orElse(null), target.requires);
        return null;
      }

      @Override public Optional<String> getRights() {
        assertEquals("rights copy", source.getRights().orElse(null), target.rights);
        return null;
      }

      @Override public Optional<String> getRightsHolder() {
        assertEquals("rightsHolder copy", source.getRightsHolder().orElse(null), target.rightsHolder);
        return null;
      }

      @Override public Optional<String> getSource() {
        assertEquals("source copy", source.getSource().orElse(null), target.source);
        return null;
      }

      @Override public Optional<String> getSpatial() {
        assertEquals("spatial copy", source.getSpatial().orElse(null), target.spatial);
        return null;
      }

      @Override public Optional<String> getSubject() {
        assertEquals("subject copy", source.getSubject().orElse(null), target.subject);
        return null;
      }

      @Override public Optional<String> getTableOfContents() {
        assertEquals("tableOfContents copy", source.getTableOfContents().orElse(null), target.tableOfContents);
        return null;
      }

      @Override public Optional<String> getTemporal() {
        assertEquals("temporal copy", source.getTemporal().orElse(null), target.temporal);
        return null;
      }

      @Override public String getTitle() {
        assertEquals("title copy", source.getTitle(), target.title);
        return null;
      }

      @Override public Optional<String> getType() {
        assertEquals("type copy", source.getType().orElse(null), target.type);
        return null;
      }

      @Override public Optional<String> getValid() {
        assertEquals("valid copy", source.getValid().orElse(null), target.valid);
        return null;
      }
    });
  }
}
