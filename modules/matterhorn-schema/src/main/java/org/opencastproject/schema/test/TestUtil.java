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
package org.opencastproject.schema.test;

import org.opencastproject.schema.OcDublinCore;
import org.opencastproject.util.data.Option;

import java.util.Date;
import java.util.UUID;

import static org.opencastproject.util.data.Option.some;

public final class TestUtil {
  private TestUtil() {
  }

  public static OcDublinCore randomDc() {
    return new OcDublinCore() {
      private final Option<String> abstrakt = some(UUID.randomUUID().toString());
      private final Option<String> accessRights = some(UUID.randomUUID().toString());
      private final Option<String> accrualMethod = some(UUID.randomUUID().toString());
      private final Option<String> accrualPeriodicity = some(UUID.randomUUID().toString());
      private final Option<String> accrualPolicy = some(UUID.randomUUID().toString());
      private final Option<String> alternative = some(UUID.randomUUID().toString());
      private final Option<String> audience = some(UUID.randomUUID().toString());
      private final Option<String> available = some(UUID.randomUUID().toString());
      private final Option<String> bibliographicCitation = some(UUID.randomUUID().toString());
      private final Option<String> conformsTo = some(UUID.randomUUID().toString());
      private final Option<String> contributor = some(UUID.randomUUID().toString());
      private final Option<String> coverage = some(UUID.randomUUID().toString());
      private final Date created = new Date();
      private final Option<String> creator = some(UUID.randomUUID().toString());
      private final Option<Date> date = some(new Date());
      private final Option<Date> dateAccepted = some(new Date());
      private final Option<Date> dateCopyrighted = some(new Date());
      private final Option<Date> dateSubmitted = some(new Date());
      private final Option<String> description = some(UUID.randomUUID().toString());
      private final Option<String> educationLevel = some(UUID.randomUUID().toString());
      private final Option<Long> extent = some(1L);
      private final Option<String> format = some(UUID.randomUUID().toString());
      private final Option<String> hasFormat = some(UUID.randomUUID().toString());
      private final Option<String> hasPart = some(UUID.randomUUID().toString());
      private final Option<String> hasVersion = some(UUID.randomUUID().toString());
      private final Option<String> identifier = some(UUID.randomUUID().toString());
      private final Option<String> instructionalMethod = some(UUID.randomUUID().toString());
      private final Option<String> isFormatOf = some(UUID.randomUUID().toString());
      private final Option<String> isPartOf = some(UUID.randomUUID().toString());
      private final Option<String> isReferencedBy = some(UUID.randomUUID().toString());
      private final Option<String> isReplacedBy = some(UUID.randomUUID().toString());
      private final Option<String> isRequiredBy = some(UUID.randomUUID().toString());
      private final Option<String> issued = some(UUID.randomUUID().toString());
      private final Option<String> isVersionOf = some(UUID.randomUUID().toString());
      private final Option<String> language = some(UUID.randomUUID().toString());
      private final Option<String> license = some(UUID.randomUUID().toString());
      private final Option<String> mediator = some(UUID.randomUUID().toString());
      private final Option<String> medium = some(UUID.randomUUID().toString());
      private final Option<String> modified = some(UUID.randomUUID().toString());
      private final Option<String> provenance = some(UUID.randomUUID().toString());
      private final Option<String> publisher = some(UUID.randomUUID().toString());
      private final Option<String> references = some(UUID.randomUUID().toString());
      private final Option<String> relation = some(UUID.randomUUID().toString());
      private final Option<String> replaces = some(UUID.randomUUID().toString());
      private final Option<String> requires = some(UUID.randomUUID().toString());
      private final Option<String> rights = some(UUID.randomUUID().toString());
      private final Option<String> rightsHolder = some(UUID.randomUUID().toString());
      private final Option<String> source = some(UUID.randomUUID().toString());
      private final Option<String> spatial = some(UUID.randomUUID().toString());
      private final Option<String> subject = some(UUID.randomUUID().toString());
      private final Option<String> tableOfContents = some(UUID.randomUUID().toString());
      private final Option<String> temporal = some(UUID.randomUUID().toString());
      private final String title = UUID.randomUUID().toString();
      private final Option<String> type = some(UUID.randomUUID().toString());
      private final Option<String> valid = some(UUID.randomUUID().toString());

      @Override public Option<String> getAbstract() {
        return abstrakt;
      }

      @Override public Option<String> getAccessRights() {
        return accessRights;
      }

      @Override public Option<String> getAccrualMethod() {
        return accrualMethod;
      }

      @Override public Option<String> getAccrualPeriodicity() {
        return accrualPeriodicity;
      }

      @Override public Option<String> getAccrualPolicy() {
        return accrualPolicy;
      }

      @Override public Option<String> getAlternative() {
        return alternative;
      }

      @Override public Option<String> getAudience() {
        return audience;
      }

      @Override public Option<String> getAvailable() {
        return available;
      }

      @Override public Option<String> getBibliographicCitation() {
        return bibliographicCitation;
      }

      @Override public Option<String> getConformsTo() {
        return conformsTo;
      }

      @Override public Option<String> getContributor() {
        return contributor;
      }

      @Override public Option<String> getCoverage() {
        return coverage;
      }

      @Override public Date getCreated() {
        return created;
      }

      @Override public Option<String> getCreator() {
        return creator;
      }

      @Override public Option<Date> getDate() {
        return date;
      }

      @Override public Option<Date> getDateAccepted() {
        return dateAccepted;
      }

      @Override public Option<Date> getDateCopyrighted() {
        return dateCopyrighted;
      }

      @Override public Option<Date> getDateSubmitted() {
        return dateSubmitted;
      }

      @Override public Option<String> getDescription() {
        return description;
      }

      @Override public Option<String> getEducationLevel() {
        return educationLevel;
      }

      @Override public Option<Long> getExtent() {
        return extent;
      }

      @Override public Option<String> getFormat() {
        return format;
      }

      @Override public Option<String> getHasFormat() {
        return hasFormat;
      }

      @Override public Option<String> getHasPart() {
        return hasPart;
      }

      @Override public Option<String> getHasVersion() {
        return hasVersion;
      }

      @Override public Option<String> getIdentifier() {
        return identifier;
      }

      @Override public Option<String> getInstructionalMethod() {
        return instructionalMethod;
      }

      @Override public Option<String> getIsFormatOf() {
        return isFormatOf;
      }

      @Override public Option<String> getIsPartOf() {
        return isPartOf;
      }

      @Override public Option<String> getIsReferencedBy() {
        return isReferencedBy;
      }

      @Override public Option<String> getIsReplacedBy() {
        return isReplacedBy;
      }

      @Override public Option<String> getIsRequiredBy() {
        return isRequiredBy;
      }

      @Override public Option<String> getIssued() {
        return issued;
      }

      @Override public Option<String> getIsVersionOf() {
        return isVersionOf;
      }

      @Override public Option<String> getLanguage() {
        return language;
      }

      @Override public Option<String> getLicense() {
        return license;
      }

      @Override public Option<String> getMediator() {
        return mediator;
      }

      @Override public Option<String> getMedium() {
        return medium;
      }

      @Override public Option<String> getModified() {
        return modified;
      }

      @Override public Option<String> getProvenance() {
        return provenance;
      }

      @Override public Option<String> getPublisher() {
        return publisher;
      }

      @Override public Option<String> getReferences() {
        return references;
      }

      @Override public Option<String> getRelation() {
        return relation;
      }

      @Override public Option<String> getReplaces() {
        return replaces;
      }

      @Override public Option<String> getRequires() {
        return requires;
      }

      @Override public Option<String> getRights() {
        return rights;
      }

      @Override public Option<String> getRightsHolder() {
        return rightsHolder;
      }

      @Override public Option<String> getSource() {
        return source;
      }

      @Override public Option<String> getSpatial() {
        return spatial;
      }

      @Override public Option<String> getSubject() {
        return subject;
      }

      @Override public Option<String> getTableOfContents() {
        return tableOfContents;
      }

      @Override public Option<String> getTemporal() {
        return temporal;
      }

      @Override public String getTitle() {
        return title;
      }

      @Override public Option<String> getType() {
        return type;
      }

      @Override public Option<String> getValid() {
        return valid;
      }
    };
  }
}
