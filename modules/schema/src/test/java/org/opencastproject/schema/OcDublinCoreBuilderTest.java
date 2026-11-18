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
import static org.opencastproject.schema.test.TestUtil.randomDc;
import static org.opencastproject.util.ReflectionUtil.run;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

public class OcDublinCoreBuilderTest {
  @Test
  public void testCreate() throws Exception {
    final OcDublinCore source = randomDc();
    final OcDublinCoreBuilder target = OcDublinCoreBuilder.create(source);
    run(OcDublinCore.class, new OcDublinCore() {
      @Override public Optional<String> getAbstract() {
        assertEquals(source.getAbstract(), target.abstrakt, "abstract copy");
        return null;
      }

      @Override public Optional<String> getAccessRights() {
        assertEquals(source.getAccessRights(), target.accessRights, "accessRights copy");
        return null;
      }

      @Override public Optional<String> getAccrualMethod() {
        assertEquals(source.getAccrualMethod(), target.accrualMethod, "accrualMethod copy");
        return null;
      }

      @Override public Optional<String> getAccrualPeriodicity() {
        assertEquals(source.getAccrualPeriodicity(), target.accrualPeriodicity, "accrualPeriodicity copy");
        return null;
      }

      @Override public Optional<String> getAccrualPolicy() {
        assertEquals(source.getAccrualPolicy(), target.accrualPolicy, "accrualPolicy copy");
        return null;
      }

      @Override public Optional<String> getAlternative() {
        assertEquals(source.getAlternative(), target.alternative, "alternative copy");
        return null;
      }

      @Override public Optional<String> getAudience() {
        assertEquals(source.getAudience(), target.audience, "audience copy");
        return null;
      }

      @Override public Optional<String> getAvailable() {
        assertEquals(source.getAvailable(), target.available, "available copy");
        return null;
      }

      @Override public Optional<String> getBibliographicCitation() {
        assertEquals(source.getBibliographicCitation(), target.bibliographicCitation, "bibliographicCitation copy");
        return null;
      }

      @Override public Optional<String> getConformsTo() {
        assertEquals(source.getConformsTo(), target.conformsTo, "conformsTo copy");
        return null;
      }

      @Override public Optional<String> getContributor() {
        assertEquals(source.getContributor(), target.contributor, "contributor copy");
        return null;
      }

      @Override public Optional<String> getCoverage() {
        assertEquals(source.getCoverage(), target.coverage, "coverage copy");
        return null;
      }

      @Override public Date getCreated() {
        assertEquals(source.getCreated(), target.created.orElse(null), "created copy");
        return null;
      }

      @Override public Optional<String> getCreator() {
        assertEquals(source.getCreator(), target.creator, "creator copy");
        return null;
      }

      @Override public Optional<Date> getDate() {
        assertEquals(source.getDate(), target.date, "date copy");
        return null;
      }

      @Override public Optional<Date> getDateAccepted() {
        assertEquals(source.getDateAccepted(), target.dateAccepted, "dateAccepted copy");
        return null;
      }

      @Override public Optional<Date> getDateCopyrighted() {
        assertEquals(source.getDateCopyrighted(), target.dateCopyrighted, "dateCopyrighted copy");
        return null;
      }

      @Override public Optional<Date> getDateSubmitted() {
        assertEquals(source.getDateSubmitted(), target.dateSubmitted, "dateSubmitted copy");
        return null;
      }

      @Override public Optional<String> getDescription() {
        assertEquals(source.getDescription(), target.description, "description copy");
        return null;
      }

      @Override public Optional<String> getEducationLevel() {
        assertEquals(source.getEducationLevel(), target.educationLevel, "educationLevel copy");
        return null;
      }

      @Override public Optional<Long> getExtent() {
        assertEquals(source.getExtent(), target.extent, "extent copy");
        return null;
      }

      @Override public Optional<String> getFormat() {
        assertEquals(source.getFormat(), target.format, "format copy");
        return null;
      }

      @Override public Optional<String> getHasFormat() {
        assertEquals(source.getHasFormat(), target.hasFormat, "hasFormat copy");
        return null;
      }

      @Override public Optional<String> getHasPart() {
        assertEquals(source.getHasPart(), target.hasPart, "hasPart copy");
        return null;
      }

      @Override public Optional<String> getHasVersion() {
        assertEquals(source.getHasVersion(), target.hasVersion, "hasVersion copy");
        return null;
      }

      @Override public Optional<String> getIdentifier() {
        assertEquals(source.getIdentifier(), target.identifier, "identifier copy");
        return null;
      }

      @Override public Optional<String> getInstructionalMethod() {
        assertEquals(source.getInstructionalMethod(), target.instructionalMethod, "instructionalMethod copy");
        return null;
      }

      @Override public Optional<String> getIsFormatOf() {
        assertEquals(source.getIsFormatOf(), target.isFormatOf, "isFormatOf copy");
        return null;
      }

      @Override public Optional<String> getIsPartOf() {
        assertEquals(source.getIsPartOf(), target.isPartOf, "isPartOf copy");
        return null;
      }

      @Override public Optional<String> getIsReferencedBy() {
        assertEquals(source.getIsReferencedBy(), target.isReferencedBy, "isReferencedBy copy");
        return null;
      }

      @Override public Optional<String> getIsReplacedBy() {
        assertEquals(source.getIsReplacedBy(), target.isReplacedBy, "isReplacedBy copy");
        return null;
      }

      @Override public Optional<String> getIsRequiredBy() {
        assertEquals(source.getIsRequiredBy(), target.isRequiredBy, "isRequiredBy copy");
        return null;
      }

      @Override public Optional<String> getIssued() {
        assertEquals(source.getIssued(), target.issued, "issued copy");
        return null;
      }

      @Override public Optional<String> getIsVersionOf() {
        assertEquals(source.getIsVersionOf(), target.isVersionOf, "isVersionOf copy");
        return null;
      }

      @Override public Optional<String> getLanguage() {
        assertEquals(source.getLanguage(), target.language, "language copy");
        return null;
      }

      @Override public Optional<String> getLicense() {
        assertEquals(source.getLicense(), target.license, "license copy");
        return null;
      }

      @Override public Optional<String> getMediator() {
        assertEquals(source.getMediator(), target.mediator, "mediator copy");
        return null;
      }

      @Override public Optional<String> getMedium() {
        assertEquals(source.getMedium(), target.medium, "medium copy");
        return null;
      }

      @Override public Optional<String> getModified() {
        assertEquals(source.getModified(), target.modified, "modified copy");
        return null;
      }

      @Override public Optional<String> getProvenance() {
        assertEquals(source.getProvenance(), target.provenance, "provenance copy");
        return null;
      }

      @Override public Optional<String> getPublisher() {
        assertEquals(source.getPublisher(), target.publisher, "publisher copy");
        return null;
      }

      @Override public Optional<String> getReferences() {
        assertEquals(source.getReferences(), target.references, "references copy");
        return null;
      }

      @Override public Optional<String> getRelation() {
        assertEquals(source.getRelation(), target.relation, "relation copy");
        return null;
      }

      @Override public Optional<String> getReplaces() {
        assertEquals(source.getReplaces(), target.replaces, "replaces copy");
        return null;
      }

      @Override public Optional<String> getRequires() {
        assertEquals(source.getRequires(), target.requires, "requires copy");
        return null;
      }

      @Override public Optional<String> getRights() {
        assertEquals(source.getRights(), target.rights, "rights copy");
        return null;
      }

      @Override public Optional<String> getRightsHolder() {
        assertEquals(source.getRightsHolder(), target.rightsHolder, "rightsHolder copy");
        return null;
      }

      @Override public Optional<String> getSource() {
        assertEquals(source.getSource(), target.source, "source copy");
        return null;
      }

      @Override public Optional<String> getSpatial() {
        assertEquals(source.getSpatial(), target.spatial, "spatial copy");
        return null;
      }

      @Override public Optional<String> getSubject() {
        assertEquals(source.getSubject(), target.subject, "subject copy");
        return null;
      }

      @Override public Optional<String> getTableOfContents() {
        assertEquals(source.getTableOfContents(), target.tableOfContents, "tableOfContents copy");
        return null;
      }

      @Override public Optional<String> getTemporal() {
        assertEquals(source.getTemporal(), target.temporal, "temporal copy");
        return null;
      }

      @Override public String getTitle() {
        assertEquals(source.getTitle(), target.title.orElse(null), "title copy");
        return null;
      }

      @Override public Optional<String> getType() {
        assertEquals(source.getType(), target.type, "type copy");
        return null;
      }

      @Override public Optional<String> getValid() {
        assertEquals(source.getValid(), target.valid, "valid copy");
        return null;
      }
    });
  }
}
