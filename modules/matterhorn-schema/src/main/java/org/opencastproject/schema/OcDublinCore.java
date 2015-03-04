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

import org.opencastproject.util.data.Option;

import java.util.Date;

/**
 * Opencast Dublin Core schema.
 * <p/>
 * Fields may have at most <em>one</em> value and do <em>not</em> have any language annotation.
 * <p/>
 * For more information about the exposed field see the
 * <a href="http://dublincore.org/documents/dcmi-type-vocabulary/#H2">DublinCore /terms/ namespace</a>.
 */
public interface OcDublinCore {
  Option<String> getAbstract();

  Option<String> getAccessRights();

  Option<String> getAccrualMethod();

  Option<String> getAccrualPeriodicity();

  Option<String> getAccrualPolicy();

  Option<String> getAlternative();

  Option<String> getAudience();

  Option<String> getAvailable();

  Option<String> getBibliographicCitation();

  Option<String> getConformsTo();

  Option<String> getContributor();

  Option<String> getCoverage();

  Date getCreated();

  Option<String> getCreator();

  Option<Date> getDate();

  Option<Date> getDateAccepted();

  Option<Date> getDateCopyrighted();

  Option<Date> getDateSubmitted();

  Option<String> getDescription();

  Option<String> getEducationLevel();

  Option<Long> getExtent();

  Option<String> getFormat();

  Option<String> getHasFormat();

  Option<String> getHasPart();

  Option<String> getHasVersion();

  Option<String> getIdentifier();

  Option<String> getInstructionalMethod();

  Option<String> getIsFormatOf();

  Option<String> getIsPartOf();

  Option<String> getIsReferencedBy();

  Option<String> getIsReplacedBy();

  Option<String> getIsRequiredBy();

  Option<String> getIssued();

  Option<String> getIsVersionOf();

  Option<String> getLanguage();

  Option<String> getLicense();

  Option<String> getMediator();

  Option<String> getMedium();

  Option<String> getModified();

  Option<String> getProvenance();

  Option<String> getPublisher();

  Option<String> getReferences();

  Option<String> getRelation();

  Option<String> getReplaces();

  Option<String> getRequires();

  Option<String> getRights();

  Option<String> getRightsHolder();

  Option<String> getSource();

  Option<String> getSpatial();

  Option<String> getSubject();

  Option<String> getTableOfContents();

  Option<String> getTemporal();

  String getTitle();

  Option<String> getType();

  Option<String> getValid();
}
