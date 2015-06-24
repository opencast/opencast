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

import org.junit.Test;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.util.data.Option;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.schema.OcDublinCoreUtil.encodeCreated;
import static org.opencastproject.schema.OcDublinCoreUtil.encodeDate;
import static org.opencastproject.schema.OcDublinCoreUtil.encodeDateAccepted;
import static org.opencastproject.schema.OcDublinCoreUtil.encodeDateCopyrighted;
import static org.opencastproject.schema.OcDublinCoreUtil.encodeDateSubmitted;
import static org.opencastproject.schema.test.TestUtil.randomDc;
import static org.opencastproject.util.ReflectionUtil.run;

public class OcDublinCoreUtilTest {
  @Test
  public void testToCatalog() throws Exception {
    final OcDublinCore source = randomDc();
    final DublinCoreCatalog target = OcDublinCoreUtil.toCatalog(source);
    run(OcDublinCore.class, new OcDublinCore() {
      @Override public Option<String> getAbstract() {
        assertEquals("abstract copy", source.getAbstract().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_ABSTRACT));
        return null;
      }

      @Override public Option<String> getAccessRights() {
        assertEquals("accessRights copy", source.getAccessRights().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_ACCESS_RIGHTS));
        return null;
      }

      @Override public Option<String> getAccrualMethod() {
        assertEquals("accrualMethod copy", source.getAccrualMethod().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_ACCRUAL_METHOD));
        return null;
      }

      @Override public Option<String> getAccrualPeriodicity() {
        assertEquals("accrualPeriodicity copy", source.getAccrualPeriodicity().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_ACCRUAL_PERIODICITY));
        return null;
      }

      @Override public Option<String> getAccrualPolicy() {
        assertEquals("accrualPolicy copy", source.getAccrualPolicy().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_ACCRUAL_POLICY));
        return null;
      }

      @Override public Option<String> getAlternative() {
        assertEquals("alternative copy", source.getAlternative().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_ALTERNATIVE));
        return null;
      }

      @Override public Option<String> getAudience() {
        assertEquals("audience copy", source.getAudience().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_AUDIENCE));
        return null;
      }

      @Override public Option<String> getAvailable() {
        assertEquals("available copy", source.getAvailable().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_AVAILABLE));
        return null;
      }

      @Override public Option<String> getBibliographicCitation() {
        assertEquals("bibliographicCitation copy", source.getBibliographicCitation().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_BIBLIOGRAPHIC_CITATION));
        return null;
      }

      @Override public Option<String> getConformsTo() {
        assertEquals("conformsTo copy", source.getConformsTo().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_CONFORMS_TO));
        return null;
      }

      @Override public Option<String> getContributor() {
        assertEquals("contributor copy", source.getContributor().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_CONTRIBUTOR));
        return null;
      }

      @Override public Option<String> getCoverage() {
        assertEquals("coverage copy", source.getCoverage().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_COVERAGE));
        return null;
      }

      @Override public Date getCreated() {
        assertEquals("created copy", encodeCreated(source.getCreated()), target.getFirstVal(DublinCore.PROPERTY_CREATED));
        return null;
      }

      @Override public Option<String> getCreator() {
        assertEquals("creator copy", source.getCreator().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_CREATOR));
        return null;
      }

      @Override public Option<Date> getDate() {
        assertEquals("date copy", source.getDate().map(encodeDate).getOrElseNull(), target.getFirstVal(DublinCore.PROPERTY_DATE));
        return null;
      }

      @Override public Option<Date> getDateAccepted() {
        assertEquals("dateAccepted copy", source.getDateAccepted().map(encodeDateAccepted).getOrElseNull(), target.getFirstVal(DublinCore.PROPERTY_DATE_ACCEPTED));
        return null;
      }

      @Override public Option<Date> getDateCopyrighted() {
        assertEquals("dateCopyrighted copy", source.getDateCopyrighted().map(encodeDateCopyrighted).getOrElseNull(), target.getFirstVal(DublinCore.PROPERTY_DATE_COPYRIGHTED));
        return null;
      }

      @Override public Option<Date> getDateSubmitted() {
        assertEquals("dateSubmitted copy", source.getDateSubmitted().map(encodeDateSubmitted).getOrElseNull(), target.getFirstVal(DublinCore.PROPERTY_DATE_SUBMITTED));
        return null;
      }

      @Override public Option<String> getDescription() {
        assertEquals("description copy", source.getDescription().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_DESCRIPTION));
        return null;
      }

      @Override public Option<String> getEducationLevel() {
        assertEquals("educationLevel copy", source.getEducationLevel().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_EDUCATION_LEVEL));
        return null;
      }

      @Override public Option<Long> getExtent() {
        assertEquals("extent copy", source.getExtent().getOrElseNull(), (Long) Long.parseLong(target.getFirst(DublinCore.PROPERTY_EXTENT)));
        return null;
      }

      @Override public Option<String> getFormat() {
        assertEquals("format copy", source.getFormat().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_FORMAT));
        return null;
      }

      @Override public Option<String> getHasFormat() {
        assertEquals("hasFormat copy", source.getHasFormat().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_HAS_FORMAT));
        return null;
      }

      @Override public Option<String> getHasPart() {
        assertEquals("hasPart copy", source.getHasPart().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_HAS_PART));
        return null;
      }

      @Override public Option<String> getHasVersion() {
        assertEquals("hasVersion copy", source.getHasVersion().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_HAS_VERSION));
        return null;
      }

      @Override public Option<String> getIdentifier() {
        assertEquals("identifier copy", source.getIdentifier().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_IDENTIFIER));
        return null;
      }

      @Override public Option<String> getInstructionalMethod() {
        assertEquals("instructionalMethod copy", source.getInstructionalMethod().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_INSTRUCTIONAL_METHOD));
        return null;
      }

      @Override public Option<String> getIsFormatOf() {
        assertEquals("isFormatOf copy", source.getIsFormatOf().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_IS_FORMAT_OF));
        return null;
      }

      @Override public Option<String> getIsPartOf() {
        assertEquals("isPartOf copy", source.getIsPartOf().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_IS_PART_OF));
        return null;
      }

      @Override public Option<String> getIsReferencedBy() {
        assertEquals("isReferencedBy copy", source.getIsReferencedBy().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_IS_REFERENCED_BY));
        return null;
      }

      @Override public Option<String> getIsReplacedBy() {
        assertEquals("isReplacedBy copy", source.getIsReplacedBy().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_IS_REPLACED_BY));
        return null;
      }

      @Override public Option<String> getIsRequiredBy() {
        assertEquals("isRequiredBy copy", source.getIsRequiredBy().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_IS_REQUIRED_BY));
        return null;
      }

      @Override public Option<String> getIssued() {
        assertEquals("issued copy", source.getIssued().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_ISSUED));
        return null;
      }

      @Override public Option<String> getIsVersionOf() {
        assertEquals("isVersionOf copy", source.getIsVersionOf().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_IS_VERSION_OF));
        return null;
      }

      @Override public Option<String> getLanguage() {
        assertEquals("language copy", source.getLanguage().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_LANGUAGE));
        return null;
      }

      @Override public Option<String> getLicense() {
        assertEquals("license copy", source.getLicense().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_LICENSE));
        return null;
      }

      @Override public Option<String> getMediator() {
        assertEquals("mediator copy", source.getMediator().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_MEDIATOR));
        return null;
      }

      @Override public Option<String> getMedium() {
        assertEquals("medium copy", source.getMedium().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_MEDIUM));
        return null;
      }

      @Override public Option<String> getModified() {
        assertEquals("modified copy", source.getModified().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_MODIFIED));
        return null;
      }

      @Override public Option<String> getProvenance() {
        assertEquals("provenance copy", source.getProvenance().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_PROVENANCE));
        return null;
      }

      @Override public Option<String> getPublisher() {
        assertEquals("publisher copy", source.getPublisher().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_PUBLISHER));
        return null;
      }

      @Override public Option<String> getReferences() {
        assertEquals("references copy", source.getReferences().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_REFERENCES));
        return null;
      }

      @Override public Option<String> getRelation() {
        assertEquals("relation copy", source.getRelation().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_RELATION));
        return null;
      }

      @Override public Option<String> getReplaces() {
        assertEquals("replaces copy", source.getReplaces().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_REPLACES));
        return null;
      }

      @Override public Option<String> getRequires() {
        assertEquals("requires copy", source.getRequires().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_REQUIRES));
        return null;
      }

      @Override public Option<String> getRights() {
        assertEquals("rights copy", source.getRights().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_RIGHTS));
        return null;
      }

      @Override public Option<String> getRightsHolder() {
        assertEquals("rightsHolder copy", source.getRightsHolder().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER));
        return null;
      }

      @Override public Option<String> getSource() {
        assertEquals("source copy", source.getSource().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_SOURCE));
        return null;
      }

      @Override public Option<String> getSpatial() {
        assertEquals("spatial copy", source.getSpatial().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_SPATIAL));
        return null;
      }

      @Override public Option<String> getSubject() {
        assertEquals("subject copy", source.getSubject().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_SUBJECT));
        return null;
      }

      @Override public Option<String> getTableOfContents() {
        assertEquals("tableOfContents copy", source.getTableOfContents().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_TABLE_OF_CONTENTS));
        return null;
      }

      @Override public Option<String> getTemporal() {
        assertEquals("temporal copy", source.getTemporal().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_TEMPORAL));
        return null;
      }

      @Override public String getTitle() {
        assertEquals("title copy", source.getTitle(), target.getFirst(DublinCore.PROPERTY_TITLE));
        return null;
      }

      @Override public Option<String> getType() {
        assertEquals("type copy", source.getType().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_TYPE));
        return null;
      }

      @Override public Option<String> getValid() {
        assertEquals("valid copy", source.getValid().getOrElseNull(), target.getFirst(DublinCore.PROPERTY_VALID));
        return null;
      }
    });
  }

  @Test
  public void testToJaxb() throws Exception {
    final OcDublinCore source = randomDc();
    final JaxbOcDublinCore target = OcDublinCoreUtil.toJaxb(source);
    run(OcDublinCore.class, new OcDublinCore() {
      @Override public Option<String> getAbstract() {
        assertEquals("abstract copy", source.getAbstract().getOrElseNull(), target.abstrakt);
        return null;
      }

      @Override public Option<String> getAccessRights() {
        assertEquals("accessRights copy", source.getAccessRights().getOrElseNull(), target.accessRights);
        return null;
      }

      @Override public Option<String> getAccrualMethod() {
        assertEquals("accrualMethod copy", source.getAccrualMethod().getOrElseNull(), target.accrualMethod);
        return null;
      }

      @Override public Option<String> getAccrualPeriodicity() {
        assertEquals("accrualPeriodicity copy", source.getAccrualPeriodicity().getOrElseNull(), target.accrualPeriodicity);
        return null;
      }

      @Override public Option<String> getAccrualPolicy() {
        assertEquals("accrualPolicy copy", source.getAccrualPolicy().getOrElseNull(), target.accrualPolicy);
        return null;
      }

      @Override public Option<String> getAlternative() {
        assertEquals("alternative copy", source.getAlternative().getOrElseNull(), target.alternative);
        return null;
      }

      @Override public Option<String> getAudience() {
        assertEquals("audience copy", source.getAudience().getOrElseNull(), target.audience);
        return null;
      }

      @Override public Option<String> getAvailable() {
        assertEquals("available copy", source.getAvailable().getOrElseNull(), target.available);
        return null;
      }

      @Override public Option<String> getBibliographicCitation() {
        assertEquals("bibliographicCitation copy", source.getBibliographicCitation().getOrElseNull(), target.bibliographicCitation);
        return null;
      }

      @Override public Option<String> getConformsTo() {
        assertEquals("conformsTo copy", source.getConformsTo().getOrElseNull(), target.conformsTo);
        return null;
      }

      @Override public Option<String> getContributor() {
        assertEquals("contributor copy", source.getContributor().getOrElseNull(), target.contributor);
        return null;
      }

      @Override public Option<String> getCoverage() {
        assertEquals("coverage copy", source.getCoverage().getOrElseNull(), target.coverage);
        return null;
      }

      @Override public Date getCreated() {
        assertEquals("created copy", source.getCreated(), target.created);
        return null;
      }

      @Override public Option<String> getCreator() {
        assertEquals("creator copy", source.getCreator().getOrElseNull(), target.creator);
        return null;
      }

      @Override public Option<Date> getDate() {
        assertEquals("date copy", source.getDate().getOrElseNull(), target.date);
        return null;
      }

      @Override public Option<Date> getDateAccepted() {
        assertEquals("dateAccepted copy", source.getDateAccepted().getOrElseNull(), target.dateAccepted);
        return null;
      }

      @Override public Option<Date> getDateCopyrighted() {
        assertEquals("dateCopyrighted copy", source.getDateCopyrighted().getOrElseNull(), target.dateCopyrighted);
        return null;
      }

      @Override public Option<Date> getDateSubmitted() {
        assertEquals("dateSubmitted copy", source.getDateSubmitted().getOrElseNull(), target.dateSubmitted);
        return null;
      }

      @Override public Option<String> getDescription() {
        assertEquals("description copy", source.getDescription().getOrElseNull(), target.description);
        return null;
      }

      @Override public Option<String> getEducationLevel() {
        assertEquals("educationLevel copy", source.getEducationLevel().getOrElseNull(), target.educationLevel);
        return null;
      }

      @Override public Option<Long> getExtent() {
        assertEquals("extent copy", source.getExtent().getOrElseNull(), target.extent);
        return null;
      }

      @Override public Option<String> getFormat() {
        assertEquals("format copy", source.getFormat().getOrElseNull(), target.format);
        return null;
      }

      @Override public Option<String> getHasFormat() {
        assertEquals("hasFormat copy", source.getHasFormat().getOrElseNull(), target.hasFormat);
        return null;
      }

      @Override public Option<String> getHasPart() {
        assertEquals("hasPart copy", source.getHasPart().getOrElseNull(), target.hasPart);
        return null;
      }

      @Override public Option<String> getHasVersion() {
        assertEquals("hasVersion copy", source.getHasVersion().getOrElseNull(), target.hasVersion);
        return null;
      }

      @Override public Option<String> getIdentifier() {
        assertEquals("identifier copy", source.getIdentifier().getOrElseNull(), target.identifier);
        return null;
      }

      @Override public Option<String> getInstructionalMethod() {
        assertEquals("instructionalMethod copy", source.getInstructionalMethod().getOrElseNull(), target.instructionalMethod);
        return null;
      }

      @Override public Option<String> getIsFormatOf() {
        assertEquals("isFormatOf copy", source.getIsFormatOf().getOrElseNull(), target.isFormatOf);
        return null;
      }

      @Override public Option<String> getIsPartOf() {
        assertEquals("isPartOf copy", source.getIsPartOf().getOrElseNull(), target.isPartOf);
        return null;
      }

      @Override public Option<String> getIsReferencedBy() {
        assertEquals("isReferencedBy copy", source.getIsReferencedBy().getOrElseNull(), target.isReferencedBy);
        return null;
      }

      @Override public Option<String> getIsReplacedBy() {
        assertEquals("isReplacedBy copy", source.getIsReplacedBy().getOrElseNull(), target.isReplacedBy);
        return null;
      }

      @Override public Option<String> getIsRequiredBy() {
        assertEquals("isRequiredBy copy", source.getIsRequiredBy().getOrElseNull(), target.isRequiredBy);
        return null;
      }

      @Override public Option<String> getIssued() {
        assertEquals("issued copy", source.getIssued().getOrElseNull(), target.issued);
        return null;
      }

      @Override public Option<String> getIsVersionOf() {
        assertEquals("isVersionOf copy", source.getIsVersionOf().getOrElseNull(), target.isVersionOf);
        return null;
      }

      @Override public Option<String> getLanguage() {
        assertEquals("language copy", source.getLanguage().getOrElseNull(), target.language);
        return null;
      }

      @Override public Option<String> getLicense() {
        assertEquals("license copy", source.getLicense().getOrElseNull(), target.license);
        return null;
      }

      @Override public Option<String> getMediator() {
        assertEquals("mediator copy", source.getMediator().getOrElseNull(), target.mediator);
        return null;
      }

      @Override public Option<String> getMedium() {
        assertEquals("medium copy", source.getMedium().getOrElseNull(), target.medium);
        return null;
      }

      @Override public Option<String> getModified() {
        assertEquals("modified copy", source.getModified().getOrElseNull(), target.modified);
        return null;
      }

      @Override public Option<String> getProvenance() {
        assertEquals("provenance copy", source.getProvenance().getOrElseNull(), target.provenance);
        return null;
      }

      @Override public Option<String> getPublisher() {
        assertEquals("publisher copy", source.getPublisher().getOrElseNull(), target.publisher);
        return null;
      }

      @Override public Option<String> getReferences() {
        assertEquals("references copy", source.getReferences().getOrElseNull(), target.references);
        return null;
      }

      @Override public Option<String> getRelation() {
        assertEquals("relation copy", source.getRelation().getOrElseNull(), target.relation);
        return null;
      }

      @Override public Option<String> getReplaces() {
        assertEquals("replaces copy", source.getReplaces().getOrElseNull(), target.replaces);
        return null;
      }

      @Override public Option<String> getRequires() {
        assertEquals("requires copy", source.getRequires().getOrElseNull(), target.requires);
        return null;
      }

      @Override public Option<String> getRights() {
        assertEquals("rights copy", source.getRights().getOrElseNull(), target.rights);
        return null;
      }

      @Override public Option<String> getRightsHolder() {
        assertEquals("rightsHolder copy", source.getRightsHolder().getOrElseNull(), target.rightsHolder);
        return null;
      }

      @Override public Option<String> getSource() {
        assertEquals("source copy", source.getSource().getOrElseNull(), target.source);
        return null;
      }

      @Override public Option<String> getSpatial() {
        assertEquals("spatial copy", source.getSpatial().getOrElseNull(), target.spatial);
        return null;
      }

      @Override public Option<String> getSubject() {
        assertEquals("subject copy", source.getSubject().getOrElseNull(), target.subject);
        return null;
      }

      @Override public Option<String> getTableOfContents() {
        assertEquals("tableOfContents copy", source.getTableOfContents().getOrElseNull(), target.tableOfContents);
        return null;
      }

      @Override public Option<String> getTemporal() {
        assertEquals("temporal copy", source.getTemporal().getOrElseNull(), target.temporal);
        return null;
      }

      @Override public String getTitle() {
        assertEquals("title copy", source.getTitle(), target.title);
        return null;
      }

      @Override public Option<String> getType() {
        assertEquals("type copy", source.getType().getOrElseNull(), target.type);
        return null;
      }

      @Override public Option<String> getValid() {
        assertEquals("valid copy", source.getValid().getOrElseNull(), target.valid);
        return null;
      }
    });
  }
}
