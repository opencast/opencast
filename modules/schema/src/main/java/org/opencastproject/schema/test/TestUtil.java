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

package org.opencastproject.schema.test;

import org.opencastproject.schema.OcDublinCore;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public final class TestUtil {
  private TestUtil() {
  }

  public static OcDublinCore randomDc() {
    return new OcDublinCore() {
      private final Optional<String> abstrakt = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> accessRights = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> accrualMethod = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> accrualPeriodicity = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> accrualPolicy = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> alternative = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> audience = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> available = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> bibliographicCitation = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> conformsTo = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> contributor = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> coverage = Optional.of(UUID.randomUUID().toString());
      private final Date created = new Date();
      private final Optional<String> creator = Optional.of(UUID.randomUUID().toString());
      private final Optional<Date> date = Optional.of(new Date());
      private final Optional<Date> dateAccepted = Optional.of(new Date());
      private final Optional<Date> dateCopyrighted = Optional.of(new Date());
      private final Optional<Date> dateSubmitted = Optional.of(new Date());
      private final Optional<String> description = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> educationLevel = Optional.of(UUID.randomUUID().toString());
      private final Optional<Long> extent = Optional.of(1L);
      private final Optional<String> format = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> hasFormat = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> hasPart = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> hasVersion = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> identifier = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> instructionalMethod = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> isFormatOf = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> isPartOf = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> isReferencedBy = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> isReplacedBy = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> isRequiredBy = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> issued = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> isVersionOf = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> language = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> license = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> mediator = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> medium = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> modified = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> provenance = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> publisher = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> references = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> relation = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> replaces = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> requires = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> rights = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> rightsHolder = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> source = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> spatial = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> subject = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> tableOfContents = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> temporal = Optional.of(UUID.randomUUID().toString());
      private final String title = UUID.randomUUID().toString();
      private final Optional<String> type = Optional.of(UUID.randomUUID().toString());
      private final Optional<String> valid = Optional.of(UUID.randomUUID().toString());

      @Override public Optional<String> getAbstract() {
        return abstrakt;
      }

      @Override public Optional<String> getAccessRights() {
        return accessRights;
      }

      @Override public Optional<String> getAccrualMethod() {
        return accrualMethod;
      }

      @Override public Optional<String> getAccrualPeriodicity() {
        return accrualPeriodicity;
      }

      @Override public Optional<String> getAccrualPolicy() {
        return accrualPolicy;
      }

      @Override public Optional<String> getAlternative() {
        return alternative;
      }

      @Override public Optional<String> getAudience() {
        return audience;
      }

      @Override public Optional<String> getAvailable() {
        return available;
      }

      @Override public Optional<String> getBibliographicCitation() {
        return bibliographicCitation;
      }

      @Override public Optional<String> getConformsTo() {
        return conformsTo;
      }

      @Override public Optional<String> getContributor() {
        return contributor;
      }

      @Override public Optional<String> getCoverage() {
        return coverage;
      }

      @Override public Date getCreated() {
        return created;
      }

      @Override public Optional<String> getCreator() {
        return creator;
      }

      @Override public Optional<Date> getDate() {
        return date;
      }

      @Override public Optional<Date> getDateAccepted() {
        return dateAccepted;
      }

      @Override public Optional<Date> getDateCopyrighted() {
        return dateCopyrighted;
      }

      @Override public Optional<Date> getDateSubmitted() {
        return dateSubmitted;
      }

      @Override public Optional<String> getDescription() {
        return description;
      }

      @Override public Optional<String> getEducationLevel() {
        return educationLevel;
      }

      @Override public Optional<Long> getExtent() {
        return extent;
      }

      @Override public Optional<String> getFormat() {
        return format;
      }

      @Override public Optional<String> getHasFormat() {
        return hasFormat;
      }

      @Override public Optional<String> getHasPart() {
        return hasPart;
      }

      @Override public Optional<String> getHasVersion() {
        return hasVersion;
      }

      @Override public Optional<String> getIdentifier() {
        return identifier;
      }

      @Override public Optional<String> getInstructionalMethod() {
        return instructionalMethod;
      }

      @Override public Optional<String> getIsFormatOf() {
        return isFormatOf;
      }

      @Override public Optional<String> getIsPartOf() {
        return isPartOf;
      }

      @Override public Optional<String> getIsReferencedBy() {
        return isReferencedBy;
      }

      @Override public Optional<String> getIsReplacedBy() {
        return isReplacedBy;
      }

      @Override public Optional<String> getIsRequiredBy() {
        return isRequiredBy;
      }

      @Override public Optional<String> getIssued() {
        return issued;
      }

      @Override public Optional<String> getIsVersionOf() {
        return isVersionOf;
      }

      @Override public Optional<String> getLanguage() {
        return language;
      }

      @Override public Optional<String> getLicense() {
        return license;
      }

      @Override public Optional<String> getMediator() {
        return mediator;
      }

      @Override public Optional<String> getMedium() {
        return medium;
      }

      @Override public Optional<String> getModified() {
        return modified;
      }

      @Override public Optional<String> getProvenance() {
        return provenance;
      }

      @Override public Optional<String> getPublisher() {
        return publisher;
      }

      @Override public Optional<String> getReferences() {
        return references;
      }

      @Override public Optional<String> getRelation() {
        return relation;
      }

      @Override public Optional<String> getReplaces() {
        return replaces;
      }

      @Override public Optional<String> getRequires() {
        return requires;
      }

      @Override public Optional<String> getRights() {
        return rights;
      }

      @Override public Optional<String> getRightsHolder() {
        return rightsHolder;
      }

      @Override public Optional<String> getSource() {
        return source;
      }

      @Override public Optional<String> getSpatial() {
        return spatial;
      }

      @Override public Optional<String> getSubject() {
        return subject;
      }

      @Override public Optional<String> getTableOfContents() {
        return tableOfContents;
      }

      @Override public Optional<String> getTemporal() {
        return temporal;
      }

      @Override public String getTitle() {
        return title;
      }

      @Override public Optional<String> getType() {
        return type;
      }

      @Override public Optional<String> getValid() {
        return valid;
      }
    };
  }
}
