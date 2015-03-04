/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.schema;

import org.junit.Test;
import org.opencastproject.util.data.Option;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.schema.test.TestUtil.randomDc;
import static org.opencastproject.util.ReflectionUtil.run;

public class OcDublinCoreBuilderTest {
  @Test
  public void testCreate() throws Exception {
    final OcDublinCore source = randomDc();
    final OcDublinCoreBuilder target = OcDublinCoreBuilder.create(source);
    run(OcDublinCore.class, new OcDublinCore() {
      @Override public Option<String> getAbstract() {
        assertEquals("abstract copy", source.getAbstract(), target.abstrakt);
        return null;
      }

      @Override public Option<String> getAccessRights() {
        assertEquals("accessRights copy", source.getAccessRights(), target.accessRights);
        return null;
      }

      @Override public Option<String> getAccrualMethod() {
        assertEquals("accrualMethod copy", source.getAccrualMethod(), target.accrualMethod);
        return null;
      }

      @Override public Option<String> getAccrualPeriodicity() {
        assertEquals("accrualPeriodicity copy", source.getAccrualPeriodicity(), target.accrualPeriodicity);
        return null;
      }

      @Override public Option<String> getAccrualPolicy() {
        assertEquals("accrualPolicy copy", source.getAccrualPolicy(), target.accrualPolicy);
        return null;
      }

      @Override public Option<String> getAlternative() {
        assertEquals("alternative copy", source.getAlternative(), target.alternative);
        return null;
      }

      @Override public Option<String> getAudience() {
        assertEquals("audience copy", source.getAudience(), target.audience);
        return null;
      }

      @Override public Option<String> getAvailable() {
        assertEquals("available copy", source.getAvailable(), target.available);
        return null;
      }

      @Override public Option<String> getBibliographicCitation() {
        assertEquals("bibliographicCitation copy", source.getBibliographicCitation(), target.bibliographicCitation);
        return null;
      }

      @Override public Option<String> getConformsTo() {
        assertEquals("conformsTo copy", source.getConformsTo(), target.conformsTo);
        return null;
      }

      @Override public Option<String> getContributor() {
        assertEquals("contributor copy", source.getContributor(), target.contributor);
        return null;
      }

      @Override public Option<String> getCoverage() {
        assertEquals("coverage copy", source.getCoverage(), target.coverage);
        return null;
      }

      @Override public Date getCreated() {
        assertEquals("created copy", source.getCreated(), target.created.getOrElseNull());
        return null;
      }

      @Override public Option<String> getCreator() {
        assertEquals("creator copy", source.getCreator(), target.creator);
        return null;
      }

      @Override public Option<Date> getDate() {
        assertEquals("date copy", source.getDate(), target.date);
        return null;
      }

      @Override public Option<Date> getDateAccepted() {
        assertEquals("dateAccepted copy", source.getDateAccepted(), target.dateAccepted);
        return null;
      }

      @Override public Option<Date> getDateCopyrighted() {
        assertEquals("dateCopyrighted copy", source.getDateCopyrighted(), target.dateCopyrighted);
        return null;
      }

      @Override public Option<Date> getDateSubmitted() {
        assertEquals("dateSubmitted copy", source.getDateSubmitted(), target.dateSubmitted);
        return null;
      }

      @Override public Option<String> getDescription() {
        assertEquals("description copy", source.getDescription(), target.description);
        return null;
      }

      @Override public Option<String> getEducationLevel() {
        assertEquals("educationLevel copy", source.getEducationLevel(), target.educationLevel);
        return null;
      }

      @Override public Option<Long> getExtent() {
        assertEquals("extent copy", source.getExtent(), target.extent);
        return null;
      }

      @Override public Option<String> getFormat() {
        assertEquals("format copy", source.getFormat(), target.format);
        return null;
      }

      @Override public Option<String> getHasFormat() {
        assertEquals("hasFormat copy", source.getHasFormat(), target.hasFormat);
        return null;
      }

      @Override public Option<String> getHasPart() {
        assertEquals("hasPart copy", source.getHasPart(), target.hasPart);
        return null;
      }

      @Override public Option<String> getHasVersion() {
        assertEquals("hasVersion copy", source.getHasVersion(), target.hasVersion);
        return null;
      }

      @Override public Option<String> getIdentifier() {
        assertEquals("identifier copy", source.getIdentifier(), target.identifier);
        return null;
      }

      @Override public Option<String> getInstructionalMethod() {
        assertEquals("instructionalMethod copy", source.getInstructionalMethod(), target.instructionalMethod);
        return null;
      }

      @Override public Option<String> getIsFormatOf() {
        assertEquals("isFormatOf copy", source.getIsFormatOf(), target.isFormatOf);
        return null;
      }

      @Override public Option<String> getIsPartOf() {
        assertEquals("isPartOf copy", source.getIsPartOf(), target.isPartOf);
        return null;
      }

      @Override public Option<String> getIsReferencedBy() {
        assertEquals("isReferencedBy copy", source.getIsReferencedBy(), target.isReferencedBy);
        return null;
      }

      @Override public Option<String> getIsReplacedBy() {
        assertEquals("isReplacedBy copy", source.getIsReplacedBy(), target.isReplacedBy);
        return null;
      }

      @Override public Option<String> getIsRequiredBy() {
        assertEquals("isRequiredBy copy", source.getIsRequiredBy(), target.isRequiredBy);
        return null;
      }

      @Override public Option<String> getIssued() {
        assertEquals("issued copy", source.getIssued(), target.issued);
        return null;
      }

      @Override public Option<String> getIsVersionOf() {
        assertEquals("isVersionOf copy", source.getIsVersionOf(), target.isVersionOf);
        return null;
      }

      @Override public Option<String> getLanguage() {
        assertEquals("language copy", source.getLanguage(), target.language);
        return null;
      }

      @Override public Option<String> getLicense() {
        assertEquals("license copy", source.getLicense(), target.license);
        return null;
      }

      @Override public Option<String> getMediator() {
        assertEquals("mediator copy", source.getMediator(), target.mediator);
        return null;
      }

      @Override public Option<String> getMedium() {
        assertEquals("medium copy", source.getMedium(), target.medium);
        return null;
      }

      @Override public Option<String> getModified() {
        assertEquals("modified copy", source.getModified(), target.modified);
        return null;
      }

      @Override public Option<String> getProvenance() {
        assertEquals("provenance copy", source.getProvenance(), target.provenance);
        return null;
      }

      @Override public Option<String> getPublisher() {
        assertEquals("publisher copy", source.getPublisher(), target.publisher);
        return null;
      }

      @Override public Option<String> getReferences() {
        assertEquals("references copy", source.getReferences(), target.references);
        return null;
      }

      @Override public Option<String> getRelation() {
        assertEquals("relation copy", source.getRelation(), target.relation);
        return null;
      }

      @Override public Option<String> getReplaces() {
        assertEquals("replaces copy", source.getReplaces(), target.replaces);
        return null;
      }

      @Override public Option<String> getRequires() {
        assertEquals("requires copy", source.getRequires(), target.requires);
        return null;
      }

      @Override public Option<String> getRights() {
        assertEquals("rights copy", source.getRights(), target.rights);
        return null;
      }

      @Override public Option<String> getRightsHolder() {
        assertEquals("rightsHolder copy", source.getRightsHolder(), target.rightsHolder);
        return null;
      }

      @Override public Option<String> getSource() {
        assertEquals("source copy", source.getSource(), target.source);
        return null;
      }

      @Override public Option<String> getSpatial() {
        assertEquals("spatial copy", source.getSpatial(), target.spatial);
        return null;
      }

      @Override public Option<String> getSubject() {
        assertEquals("subject copy", source.getSubject(), target.subject);
        return null;
      }

      @Override public Option<String> getTableOfContents() {
        assertEquals("tableOfContents copy", source.getTableOfContents(), target.tableOfContents);
        return null;
      }

      @Override public Option<String> getTemporal() {
        assertEquals("temporal copy", source.getTemporal(), target.temporal);
        return null;
      }

      @Override public String getTitle() {
        assertEquals("title copy", source.getTitle(), target.title.getOrElseNull());
        return null;
      }

      @Override public Option<String> getType() {
        assertEquals("type copy", source.getType(), target.type);
        return null;
      }

      @Override public Option<String> getValid() {
        assertEquals("valid copy", source.getValid(), target.valid);
        return null;
      }
    });
  }
}
