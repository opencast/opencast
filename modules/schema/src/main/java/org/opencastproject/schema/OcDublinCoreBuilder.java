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

import java.util.Date;
import java.util.Optional;

/** Mutable builder for {@link org.opencastproject.schema.OcDublinCore}. */
public class OcDublinCoreBuilder implements OcDublinCore {
  protected Optional<String> abstrakt = Optional.empty();
  protected Optional<String> accessRights = Optional.empty();
  protected Optional<String> accrualMethod = Optional.empty();
  protected Optional<String> accrualPeriodicity = Optional.empty();
  protected Optional<String> accrualPolicy = Optional.empty();
  protected Optional<String> alternative = Optional.empty();
  protected Optional<String> audience = Optional.empty();
  protected Optional<String> available = Optional.empty();
  protected Optional<String> bibliographicCitation = Optional.empty();
  protected Optional<String> conformsTo = Optional.empty();
  protected Optional<String> contributor = Optional.empty();
  protected Optional<String> coverage = Optional.empty();
  protected Optional<Date> created = Optional.empty();
  protected Optional<String> creator = Optional.empty();
  protected Optional<Date> date = Optional.empty();
  protected Optional<Date> dateAccepted = Optional.empty();
  protected Optional<Date> dateCopyrighted = Optional.empty();
  protected Optional<Date> dateSubmitted = Optional.empty();
  protected Optional<String> description = Optional.empty();
  protected Optional<String> educationLevel = Optional.empty();
  protected Optional<Long> extent = Optional.empty();
  protected Optional<String> format = Optional.empty();
  protected Optional<String> hasFormat = Optional.empty();
  protected Optional<String> hasPart = Optional.empty();
  protected Optional<String> hasVersion = Optional.empty();
  protected Optional<String> identifier = Optional.empty();
  protected Optional<String> instructionalMethod = Optional.empty();
  protected Optional<String> isFormatOf = Optional.empty();
  protected Optional<String> isPartOf = Optional.empty();
  protected Optional<String> isReferencedBy = Optional.empty();
  protected Optional<String> isReplacedBy = Optional.empty();
  protected Optional<String> isRequiredBy = Optional.empty();
  protected Optional<String> issued = Optional.empty();
  protected Optional<String> isVersionOf = Optional.empty();
  protected Optional<String> language = Optional.empty();
  protected Optional<String> license = Optional.empty();
  protected Optional<String> mediator = Optional.empty();
  protected Optional<String> medium = Optional.empty();
  protected Optional<String> modified = Optional.empty();
  protected Optional<String> provenance = Optional.empty();
  protected Optional<String> publisher = Optional.empty();
  protected Optional<String> references = Optional.empty();
  protected Optional<String> relation = Optional.empty();
  protected Optional<String> replaces = Optional.empty();
  protected Optional<String> requires = Optional.empty();
  protected Optional<String> rights = Optional.empty();
  protected Optional<String> rightsHolder = Optional.empty();
  protected Optional<String> source = Optional.empty();
  protected Optional<String> spatial = Optional.empty();
  protected Optional<String> subject = Optional.empty();
  protected Optional<String> tableOfContents = Optional.empty();
  protected Optional<String> temporal = Optional.empty();
  protected Optional<String> title = Optional.empty();
  protected Optional<String> type = Optional.empty();
  protected Optional<String> valid = Optional.empty();

  public static OcDublinCoreBuilder create(final OcDublinCore source) {
    // traditional copy here: completeness is checked in the corresponding unit test by means of the transfer idiom
    final OcDublinCoreBuilder target = new OcDublinCoreBuilder();
    target.abstrakt = source.getAbstract();
    target.accessRights = source.getAccessRights();
    target.accrualMethod = source.getAccrualMethod();
    target.accrualPeriodicity = source.getAccrualPeriodicity();
    target.accrualPolicy = source.getAccrualPolicy();
    target.alternative = source.getAlternative();
    target.audience = source.getAudience();
    target.available = source.getAvailable();
    target.bibliographicCitation = source.getBibliographicCitation();
    target.conformsTo = source.getConformsTo();
    target.contributor = source.getContributor();
    target.coverage = source.getCoverage();
    target.created = Optional.of(source.getCreated());
    target.creator = source.getCreator();
    target.date = source.getDate();
    target.dateAccepted = source.getDateAccepted();
    target.dateCopyrighted = source.getDateCopyrighted();
    target.dateSubmitted = source.getDateSubmitted();
    target.description = source.getDescription();
    target.educationLevel = source.getEducationLevel();
    target.extent = source.getExtent();
    target.format = source.getFormat();
    target.hasFormat = source.getHasFormat();
    target.hasPart = source.getHasPart();
    target.hasVersion = source.getHasVersion();
    target.identifier = source.getIdentifier();
    target.instructionalMethod = source.getInstructionalMethod();
    target.isFormatOf = source.getIsFormatOf();
    target.isPartOf = source.getIsPartOf();
    target.isReferencedBy = source.getIsReferencedBy();
    target.isReplacedBy = source.getIsReplacedBy();
    target.isRequiredBy = source.getIsRequiredBy();
    target.issued = source.getIssued();
    target.isVersionOf = source.getIsVersionOf();
    target.language = source.getLanguage();
    target.license = source.getLicense();
    target.mediator = source.getMediator();
    target.medium = source.getMedium();
    target.modified = source.getModified();
    target.provenance = source.getProvenance();
    target.publisher = source.getPublisher();
    target.references = source.getReferences();
    target.relation = source.getRelation();
    target.replaces = source.getReplaces();
    target.requires = source.getRequires();
    target.rights = source.getRights();
    target.rightsHolder = source.getRightsHolder();
    target.source = source.getSource();
    target.spatial = source.getSpatial();
    target.subject = source.getSubject();
    target.tableOfContents = source.getTableOfContents();
    target.temporal = source.getTemporal();
    target.title = Optional.of(source.getTitle());
    target.type = source.getType();
    target.valid = source.getValid();
    return target;
  }

  public OcDublinCoreBuilder setAbstract(Optional<String> a) {
    this.abstrakt = a;
    return this;
  }

  public OcDublinCoreBuilder setAccessRights(Optional<String> a) {
    this.accessRights = a;
    return this;
  }

  public OcDublinCoreBuilder setAccrualMethod(Optional<String> a) {
    this.accrualMethod = a;
    return this;
  }

  public OcDublinCoreBuilder setAccrualPeriodicity(Optional<String> a) {
    this.accrualPeriodicity = a;
    return this;
  }

  public OcDublinCoreBuilder setAccrualPolicy(Optional<String> a) {
    this.accrualPolicy = a;
    return this;
  }

  public OcDublinCoreBuilder setAlternative(Optional<String> a) {
    this.alternative = a;
    return this;
  }

  public OcDublinCoreBuilder setAudience(Optional<String> a) {
    this.audience = a;
    return this;
  }

  public OcDublinCoreBuilder setAvailable(Optional<String> a) {
    this.available = a;
    return this;
  }

  public OcDublinCoreBuilder setBibliographicCitation(Optional<String> a) {
    this.bibliographicCitation = a;
    return this;
  }

  public OcDublinCoreBuilder setConformsTo(Optional<String> a) {
    this.conformsTo = a;
    return this;
  }

  public OcDublinCoreBuilder setContributor(Optional<String> a) {
    this.contributor = a;
    return this;
  }

  public OcDublinCoreBuilder setCoverage(Optional<String> a) {
    this.coverage = a;
    return this;
  }

  public OcDublinCoreBuilder setCreated(Optional<Date> a) {
    this.created = a;
    return this;
  }

  public OcDublinCoreBuilder setCreator(Optional<String> a) {
    this.creator = a;
    return this;
  }

  public OcDublinCoreBuilder setDate(Optional<Date> a) {
    this.date = a;
    return this;
  }

  public OcDublinCoreBuilder setDateAccepted(Optional<Date> a) {
    this.dateAccepted = a;
    return this;
  }

  public OcDublinCoreBuilder setDateCopyrighted(Optional<Date> a) {
    this.dateCopyrighted = a;
    return this;
  }

  public OcDublinCoreBuilder setDateSubmitted(Optional<Date> a) {
    this.dateSubmitted = a;
    return this;
  }

  public OcDublinCoreBuilder setDescription(Optional<String> a) {
    this.description = a;
    return this;
  }

  public OcDublinCoreBuilder setEducationLevel(Optional<String> a) {
    this.educationLevel = a;
    return this;
  }

  public OcDublinCoreBuilder setExtent(Optional<Long> a) {
    this.extent = a;
    return this;
  }

  public OcDublinCoreBuilder setFormat(Optional<String> a) {
    this.format = a;
    return this;
  }

  public OcDublinCoreBuilder setHasFormat(Optional<String> a) {
    this.hasFormat = a;
    return this;
  }

  public OcDublinCoreBuilder setHasPart(Optional<String> a) {
    this.hasPart = a;
    return this;
  }

  public OcDublinCoreBuilder setHasVersion(Optional<String> a) {
    this.hasVersion = a;
    return this;
  }

  public OcDublinCoreBuilder setIdentifier(Optional<String> a) {
    this.identifier = a;
    return this;
  }

  public OcDublinCoreBuilder setInstructionalMethod(Optional<String> a) {
    this.instructionalMethod = a;
    return this;
  }

  public OcDublinCoreBuilder setIsFormatOf(Optional<String> a) {
    this.isFormatOf = a;
    return this;
  }

  public OcDublinCoreBuilder setIsPartOf(Optional<String> a) {
    this.isPartOf = a;
    return this;
  }

  public OcDublinCoreBuilder setIsReferencedBy(Optional<String> a) {
    this.isReferencedBy = a;
    return this;
  }

  public OcDublinCoreBuilder setIsReplacedBy(Optional<String> a) {
    this.isReplacedBy = a;
    return this;
  }

  public OcDublinCoreBuilder setIsRequiredBy(Optional<String> a) {
    this.isRequiredBy = a;
    return this;
  }

  public OcDublinCoreBuilder setIssued(Optional<String> a) {
    this.issued = a;
    return this;
  }

  public OcDublinCoreBuilder setIsVersionOf(Optional<String> a) {
    this.isVersionOf = a;
    return this;
  }

  public OcDublinCoreBuilder setLanguage(Optional<String> a) {
    this.language = a;
    return this;
  }

  public OcDublinCoreBuilder setLicense(Optional<String> a) {
    this.license = a;
    return this;
  }

  public OcDublinCoreBuilder setMediator(Optional<String> a) {
    this.mediator = a;
    return this;
  }

  public OcDublinCoreBuilder setMedium(Optional<String> a) {
    this.medium = a;
    return this;
  }

  public OcDublinCoreBuilder setModified(Optional<String> a) {
    this.modified = a;
    return this;
  }

  public OcDublinCoreBuilder setProvenance(Optional<String> a) {
    this.provenance = a;
    return this;
  }

  public OcDublinCoreBuilder setPublisher(Optional<String> a) {
    this.publisher = a;
    return this;
  }

  public OcDublinCoreBuilder setReferences(Optional<String> a) {
    this.references = a;
    return this;
  }

  public OcDublinCoreBuilder setRelation(Optional<String> a) {
    this.relation = a;
    return this;
  }

  public OcDublinCoreBuilder setReplaces(Optional<String> a) {
    this.replaces = a;
    return this;
  }

  public OcDublinCoreBuilder setRequires(Optional<String> a) {
    this.requires = a;
    return this;
  }

  public OcDublinCoreBuilder setRights(Optional<String> a) {
    this.rights = a;
    return this;
  }

  public OcDublinCoreBuilder setRightsHolder(Optional<String> a) {
    this.rightsHolder = a;
    return this;
  }

  public OcDublinCoreBuilder setSource(Optional<String> a) {
    this.source = a;
    return this;
  }

  public OcDublinCoreBuilder setSpatial(Optional<String> a) {
    this.spatial = a;
    return this;
  }

  public OcDublinCoreBuilder setSubject(Optional<String> a) {
    this.subject = a;
    return this;
  }

  public OcDublinCoreBuilder setTableOfContents(Optional<String> a) {
    this.tableOfContents = a;
    return this;
  }

  public OcDublinCoreBuilder setTemporal(Optional<String> a) {
    this.temporal = a;
    return this;
  }

  public OcDublinCoreBuilder setTitle(Optional<String> a) {
    this.title = a;
    return this;
  }

  public OcDublinCoreBuilder setType(Optional<String> a) {
    this.type = a;
    return this;
  }

  public OcDublinCoreBuilder setValid(Optional<String> a) {
    this.valid = a;
    return this;
  }

  public OcDublinCoreBuilder setAbstract(String a) {
    this.abstrakt = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setAccessRights(String a) {
    this.accessRights = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setAccrualMethod(String a) {
    this.accrualMethod = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setAccrualPeriodicity(String a) {
    this.accrualPeriodicity = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setAccrualPolicy(String a) {
    this.accrualPolicy = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setAlternative(String a) {
    this.alternative = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setAudience(String a) {
    this.audience = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setAvailable(String a) {
    this.available = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setBibliographicCitation(String a) {
    this.bibliographicCitation = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setConformsTo(String a) {
    this.conformsTo = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setContributor(String a) {
    this.contributor = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setCoverage(String a) {
    this.coverage = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setCreated(Date a) {
    this.created = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setCreator(String a) {
    this.creator = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setDate(Date a) {
    this.date = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setDateAccepted(Date a) {
    this.dateAccepted = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setDateCopyrighted(Date a) {
    this.dateCopyrighted = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setDateSubmitted(Date a) {
    this.dateSubmitted = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setDescription(String a) {
    this.description = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setEducationLevel(String a) {
    this.educationLevel = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setExtent(Long a) {
    this.extent = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setFormat(String a) {
    this.format = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setHasFormat(String a) {
    this.hasFormat = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setHasPart(String a) {
    this.hasPart = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setHasVersion(String a) {
    this.hasVersion = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setIdentifier(String a) {
    this.identifier = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setInstructionalMethod(String a) {
    this.instructionalMethod = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setIsFormatOf(String a) {
    this.isFormatOf = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setIsPartOf(String a) {
    this.isPartOf = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setIsReferencedBy(String a) {
    this.isReferencedBy = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setIsReplacedBy(String a) {
    this.isReplacedBy = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setIsRequiredBy(String a) {
    this.isRequiredBy = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setIssued(String a) {
    this.issued = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setIsVersionOf(String a) {
    this.isVersionOf = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setLanguage(String a) {
    this.language = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setLicense(String a) {
    this.license = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setMediator(String a) {
    this.mediator = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setMedium(String a) {
    this.medium = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setModified(String a) {
    this.modified = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setProvenance(String a) {
    this.provenance = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setPublisher(String a) {
    this.publisher = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setReferences(String a) {
    this.references = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setRelation(String a) {
    this.relation = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setReplaces(String a) {
    this.replaces = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setRequires(String a) {
    this.requires = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setRights(String a) {
    this.rights = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setRightsHolder(String a) {
    this.rightsHolder = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setSource(String a) {
    this.source = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setSpatial(String a) {
    this.spatial = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setSubject(String a) {
    this.subject = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setTableOfContents(String a) {
    this.tableOfContents = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setTemporal(String a) {
    this.temporal = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setTitle(String a) {
    this.title = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setType(String a) {
    this.type = Optional.of(a);
    return this;
  }

  public OcDublinCoreBuilder setValid(String a) {
    this.valid = Optional.of(a);
    return this;
  }

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
    return created.orElseThrow(() -> new Error("created not set"));
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
    return title.orElseThrow(() -> new Error("title not set"));
  }

  @Override public Optional<String> getType() {
    return type;
  }

  @Override public Optional<String> getValid() {
    return valid;
  }
}
