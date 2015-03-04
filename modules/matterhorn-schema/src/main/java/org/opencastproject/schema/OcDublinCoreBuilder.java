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

import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;

import java.util.Date;

import static org.opencastproject.util.data.Option.error;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/** Mutable builder for {@link org.opencastproject.schema.OcDublinCore}. */
public class OcDublinCoreBuilder implements OcDublinCore {
  protected Option<String> abstrakt = none();
  protected Option<String> accessRights = none();
  protected Option<String> accrualMethod = none();
  protected Option<String> accrualPeriodicity = none();
  protected Option<String> accrualPolicy = none();
  protected Option<String> alternative = none();
  protected Option<String> audience = none();
  protected Option<String> available = none();
  protected Option<String> bibliographicCitation = none();
  protected Option<String> conformsTo = none();
  protected Option<String> contributor = none();
  protected Option<String> coverage = none();
  protected Option<Date> created = none();
  protected Option<String> creator = none();
  protected Option<Date> date = none();
  protected Option<Date> dateAccepted = none();
  protected Option<Date> dateCopyrighted = none();
  protected Option<Date> dateSubmitted = none();
  protected Option<String> description = none();
  protected Option<String> educationLevel = none();
  protected Option<Long> extent = none();
  protected Option<String> format = none();
  protected Option<String> hasFormat = none();
  protected Option<String> hasPart = none();
  protected Option<String> hasVersion = none();
  protected Option<String> identifier = none();
  protected Option<String> instructionalMethod = none();
  protected Option<String> isFormatOf = none();
  protected Option<String> isPartOf = none();
  protected Option<String> isReferencedBy = none();
  protected Option<String> isReplacedBy = none();
  protected Option<String> isRequiredBy = none();
  protected Option<String> issued = none();
  protected Option<String> isVersionOf = none();
  protected Option<String> language = none();
  protected Option<String> license = none();
  protected Option<String> mediator = none();
  protected Option<String> medium = none();
  protected Option<String> modified = none();
  protected Option<String> provenance = none();
  protected Option<String> publisher = none();
  protected Option<String> references = none();
  protected Option<String> relation = none();
  protected Option<String> replaces = none();
  protected Option<String> requires = none();
  protected Option<String> rights = none();
  protected Option<String> rightsHolder = none();
  protected Option<String> source = none();
  protected Option<String> spatial = none();
  protected Option<String> subject = none();
  protected Option<String> tableOfContents = none();
  protected Option<String> temporal = none();
  protected Option<String> title = none();
  protected Option<String> type = none();
  protected Option<String> valid = none();

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
    target.created = some(source.getCreated());
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
    target.title = some(source.getTitle());
    target.type = source.getType();
    target.valid = source.getValid();
    return target;
  }

  public OcDublinCoreBuilder setAbstract(Option<String> a) {
    this.abstrakt = a;
    return this;
  }

  public OcDublinCoreBuilder setAccessRights(Option<String> a) {
    this.accessRights = a;
    return this;
  }

  public OcDublinCoreBuilder setAccrualMethod(Option<String> a) {
    this.accrualMethod = a;
    return this;
  }

  public OcDublinCoreBuilder setAccrualPeriodicity(Option<String> a) {
    this.accrualPeriodicity = a;
    return this;
  }

  public OcDublinCoreBuilder setAccrualPolicy(Option<String> a) {
    this.accrualPolicy = a;
    return this;
  }

  public OcDublinCoreBuilder setAlternative(Option<String> a) {
    this.alternative = a;
    return this;
  }

  public OcDublinCoreBuilder setAudience(Option<String> a) {
    this.audience = a;
    return this;
  }

  public OcDublinCoreBuilder setAvailable(Option<String> a) {
    this.available = a;
    return this;
  }

  public OcDublinCoreBuilder setBibliographicCitation(Option<String> a) {
    this.bibliographicCitation = a;
    return this;
  }

  public OcDublinCoreBuilder setConformsTo(Option<String> a) {
    this.conformsTo = a;
    return this;
  }

  public OcDublinCoreBuilder setContributor(Option<String> a) {
    this.contributor = a;
    return this;
  }

  public OcDublinCoreBuilder setCoverage(Option<String> a) {
    this.coverage = a;
    return this;
  }

  public OcDublinCoreBuilder setCreated(Option<Date> a) {
    this.created = a;
    return this;
  }

  public OcDublinCoreBuilder setCreator(Option<String> a) {
    this.creator = a;
    return this;
  }

  public OcDublinCoreBuilder setDate(Option<Date> a) {
    this.date = a;
    return this;
  }

  public OcDublinCoreBuilder setDateAccepted(Option<Date> a) {
    this.dateAccepted = a;
    return this;
  }

  public OcDublinCoreBuilder setDateCopyrighted(Option<Date> a) {
    this.dateCopyrighted = a;
    return this;
  }

  public OcDublinCoreBuilder setDateSubmitted(Option<Date> a) {
    this.dateSubmitted = a;
    return this;
  }

  public OcDublinCoreBuilder setDescription(Option<String> a) {
    this.description = a;
    return this;
  }

  public OcDublinCoreBuilder setEducationLevel(Option<String> a) {
    this.educationLevel = a;
    return this;
  }

  public OcDublinCoreBuilder setExtent(Option<Long> a) {
    this.extent = a;
    return this;
  }

  public OcDublinCoreBuilder setFormat(Option<String> a) {
    this.format = a;
    return this;
  }

  public OcDublinCoreBuilder setHasFormat(Option<String> a) {
    this.hasFormat = a;
    return this;
  }

  public OcDublinCoreBuilder setHasPart(Option<String> a) {
    this.hasPart = a;
    return this;
  }

  public OcDublinCoreBuilder setHasVersion(Option<String> a) {
    this.hasVersion = a;
    return this;
  }

  public OcDublinCoreBuilder setIdentifier(Option<String> a) {
    this.identifier = a;
    return this;
  }

  public OcDublinCoreBuilder setInstructionalMethod(Option<String> a) {
    this.instructionalMethod = a;
    return this;
  }

  public OcDublinCoreBuilder setIsFormatOf(Option<String> a) {
    this.isFormatOf = a;
    return this;
  }

  public OcDublinCoreBuilder setIsPartOf(Option<String> a) {
    this.isPartOf = a;
    return this;
  }

  public OcDublinCoreBuilder setIsReferencedBy(Option<String> a) {
    this.isReferencedBy = a;
    return this;
  }

  public OcDublinCoreBuilder setIsReplacedBy(Option<String> a) {
    this.isReplacedBy = a;
    return this;
  }

  public OcDublinCoreBuilder setIsRequiredBy(Option<String> a) {
    this.isRequiredBy = a;
    return this;
  }

  public OcDublinCoreBuilder setIssued(Option<String> a) {
    this.issued = a;
    return this;
  }

  public OcDublinCoreBuilder setIsVersionOf(Option<String> a) {
    this.isVersionOf = a;
    return this;
  }

  public OcDublinCoreBuilder setLanguage(Option<String> a) {
    this.language = a;
    return this;
  }

  public OcDublinCoreBuilder setLicense(Option<String> a) {
    this.license = a;
    return this;
  }

  public OcDublinCoreBuilder setMediator(Option<String> a) {
    this.mediator = a;
    return this;
  }

  public OcDublinCoreBuilder setMedium(Option<String> a) {
    this.medium = a;
    return this;
  }

  public OcDublinCoreBuilder setModified(Option<String> a) {
    this.modified = a;
    return this;
  }

  public OcDublinCoreBuilder setProvenance(Option<String> a) {
    this.provenance = a;
    return this;
  }

  public OcDublinCoreBuilder setPublisher(Option<String> a) {
    this.publisher = a;
    return this;
  }

  public OcDublinCoreBuilder setReferences(Option<String> a) {
    this.references = a;
    return this;
  }

  public OcDublinCoreBuilder setRelation(Option<String> a) {
    this.relation = a;
    return this;
  }

  public OcDublinCoreBuilder setReplaces(Option<String> a) {
    this.replaces = a;
    return this;
  }

  public OcDublinCoreBuilder setRequires(Option<String> a) {
    this.requires = a;
    return this;
  }

  public OcDublinCoreBuilder setRights(Option<String> a) {
    this.rights = a;
    return this;
  }

  public OcDublinCoreBuilder setRightsHolder(Option<String> a) {
    this.rightsHolder = a;
    return this;
  }

  public OcDublinCoreBuilder setSource(Option<String> a) {
    this.source = a;
    return this;
  }

  public OcDublinCoreBuilder setSpatial(Option<String> a) {
    this.spatial = a;
    return this;
  }

  public OcDublinCoreBuilder setSubject(Option<String> a) {
    this.subject = a;
    return this;
  }

  public OcDublinCoreBuilder setTableOfContents(Option<String> a) {
    this.tableOfContents = a;
    return this;
  }

  public OcDublinCoreBuilder setTemporal(Option<String> a) {
    this.temporal = a;
    return this;
  }

  public OcDublinCoreBuilder setTitle(Option<String> a) {
    this.title = a;
    return this;
  }

  public OcDublinCoreBuilder setType(Option<String> a) {
    this.type = a;
    return this;
  }

  public OcDublinCoreBuilder setValid(Option<String> a) {
    this.valid = a;
    return this;
  }

  public OcDublinCoreBuilder setAbstract(String a) {
    this.abstrakt = some(a);
    return this;
  }

  public OcDublinCoreBuilder setAccessRights(String a) {
    this.accessRights = some(a);
    return this;
  }

  public OcDublinCoreBuilder setAccrualMethod(String a) {
    this.accrualMethod = some(a);
    return this;
  }

  public OcDublinCoreBuilder setAccrualPeriodicity(String a) {
    this.accrualPeriodicity = some(a);
    return this;
  }

  public OcDublinCoreBuilder setAccrualPolicy(String a) {
    this.accrualPolicy = some(a);
    return this;
  }

  public OcDublinCoreBuilder setAlternative(String a) {
    this.alternative = some(a);
    return this;
  }

  public OcDublinCoreBuilder setAudience(String a) {
    this.audience = some(a);
    return this;
  }

  public OcDublinCoreBuilder setAvailable(String a) {
    this.available = some(a);
    return this;
  }

  public OcDublinCoreBuilder setBibliographicCitation(String a) {
    this.bibliographicCitation = some(a);
    return this;
  }

  public OcDublinCoreBuilder setConformsTo(String a) {
    this.conformsTo = some(a);
    return this;
  }

  public OcDublinCoreBuilder setContributor(String a) {
    this.contributor = some(a);
    return this;
  }

  public OcDublinCoreBuilder setCoverage(String a) {
    this.coverage = some(a);
    return this;
  }

  public OcDublinCoreBuilder setCreated(Date a) {
    this.created = some(a);
    return this;
  }

  public OcDublinCoreBuilder setCreator(String a) {
    this.creator = some(a);
    return this;
  }

  public OcDublinCoreBuilder setDate(Date a) {
    this.date = some(a);
    return this;
  }

  public OcDublinCoreBuilder setDateAccepted(Date a) {
    this.dateAccepted = some(a);
    return this;
  }

  public OcDublinCoreBuilder setDateCopyrighted(Date a) {
    this.dateCopyrighted = some(a);
    return this;
  }

  public OcDublinCoreBuilder setDateSubmitted(Date a) {
    this.dateSubmitted = some(a);
    return this;
  }

  public OcDublinCoreBuilder setDescription(String a) {
    this.description = some(a);
    return this;
  }

  public OcDublinCoreBuilder setEducationLevel(String a) {
    this.educationLevel = some(a);
    return this;
  }

  public OcDublinCoreBuilder setExtent(Long a) {
    this.extent = some(a);
    return this;
  }

  public OcDublinCoreBuilder setFormat(String a) {
    this.format = some(a);
    return this;
  }

  public OcDublinCoreBuilder setHasFormat(String a) {
    this.hasFormat = some(a);
    return this;
  }

  public OcDublinCoreBuilder setHasPart(String a) {
    this.hasPart = some(a);
    return this;
  }

  public OcDublinCoreBuilder setHasVersion(String a) {
    this.hasVersion = some(a);
    return this;
  }

  public OcDublinCoreBuilder setIdentifier(String a) {
    this.identifier = some(a);
    return this;
  }

  public OcDublinCoreBuilder setInstructionalMethod(String a) {
    this.instructionalMethod = some(a);
    return this;
  }

  public OcDublinCoreBuilder setIsFormatOf(String a) {
    this.isFormatOf = some(a);
    return this;
  }

  public OcDublinCoreBuilder setIsPartOf(String a) {
    this.isPartOf = some(a);
    return this;
  }

  public OcDublinCoreBuilder setIsReferencedBy(String a) {
    this.isReferencedBy = some(a);
    return this;
  }

  public OcDublinCoreBuilder setIsReplacedBy(String a) {
    this.isReplacedBy = some(a);
    return this;
  }

  public OcDublinCoreBuilder setIsRequiredBy(String a) {
    this.isRequiredBy = some(a);
    return this;
  }

  public OcDublinCoreBuilder setIssued(String a) {
    this.issued = some(a);
    return this;
  }

  public OcDublinCoreBuilder setIsVersionOf(String a) {
    this.isVersionOf = some(a);
    return this;
  }

  public OcDublinCoreBuilder setLanguage(String a) {
    this.language = some(a);
    return this;
  }

  public OcDublinCoreBuilder setLicense(String a) {
    this.license = some(a);
    return this;
  }

  public OcDublinCoreBuilder setMediator(String a) {
    this.mediator = some(a);
    return this;
  }

  public OcDublinCoreBuilder setMedium(String a) {
    this.medium = some(a);
    return this;
  }

  public OcDublinCoreBuilder setModified(String a) {
    this.modified = some(a);
    return this;
  }

  public OcDublinCoreBuilder setProvenance(String a) {
    this.provenance = some(a);
    return this;
  }

  public OcDublinCoreBuilder setPublisher(String a) {
    this.publisher = some(a);
    return this;
  }

  public OcDublinCoreBuilder setReferences(String a) {
    this.references = some(a);
    return this;
  }

  public OcDublinCoreBuilder setRelation(String a) {
    this.relation = some(a);
    return this;
  }

  public OcDublinCoreBuilder setReplaces(String a) {
    this.replaces = some(a);
    return this;
  }

  public OcDublinCoreBuilder setRequires(String a) {
    this.requires = some(a);
    return this;
  }

  public OcDublinCoreBuilder setRights(String a) {
    this.rights = some(a);
    return this;
  }

  public OcDublinCoreBuilder setRightsHolder(String a) {
    this.rightsHolder = some(a);
    return this;
  }

  public OcDublinCoreBuilder setSource(String a) {
    this.source = some(a);
    return this;
  }

  public OcDublinCoreBuilder setSpatial(String a) {
    this.spatial = some(a);
    return this;
  }

  public OcDublinCoreBuilder setSubject(String a) {
    this.subject = some(a);
    return this;
  }

  public OcDublinCoreBuilder setTableOfContents(String a) {
    this.tableOfContents = some(a);
    return this;
  }

  public OcDublinCoreBuilder setTemporal(String a) {
    this.temporal = some(a);
    return this;
  }

  public OcDublinCoreBuilder setTitle(String a) {
    this.title = some(a);
    return this;
  }

  public OcDublinCoreBuilder setType(String a) {
    this.type = some(a);
    return this;
  }

  public OcDublinCoreBuilder setValid(String a) {
    this.valid = some(a);
    return this;
  }

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

  private static final Function0<Date> createdNotSet = error("created not set");

  @Override public Date getCreated() {
    return created.getOrElse(createdNotSet);
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

  private static final Function0<String> titleNotSet = error("title not set");

  @Override public String getTitle() {
    return title.getOrElse(titleNotSet);
  }

  @Override public Option<String> getType() {
    return type;
  }

  @Override public Option<String> getValid() {
    return valid;
  }
}
