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

/**
 * Opencast Dublin Core schema.
 * <p>
 * Fields may have at most <em>one</em> value and do <em>not</em> have any language annotation.
 * <p>
 * For more information about the exposed field see the
 * <a href="http://dublincore.org/documents/dcmi-type-vocabulary/#H2">DublinCore /terms/ namespace</a>.
 */
public interface OcDublinCore {
  Optional<String> getAbstract();

  Optional<String> getAccessRights();

  Optional<String> getAccrualMethod();

  Optional<String> getAccrualPeriodicity();

  Optional<String> getAccrualPolicy();

  Optional<String> getAlternative();

  Optional<String> getAudience();

  Optional<String> getAvailable();

  Optional<String> getBibliographicCitation();

  Optional<String> getConformsTo();

  Optional<String> getContributor();

  Optional<String> getCoverage();

  Date getCreated();

  Optional<String> getCreator();

  Optional<Date> getDate();

  Optional<Date> getDateAccepted();

  Optional<Date> getDateCopyrighted();

  Optional<Date> getDateSubmitted();

  Optional<String> getDescription();

  Optional<String> getEducationLevel();

  Optional<Long> getExtent();

  Optional<String> getFormat();

  Optional<String> getHasFormat();

  Optional<String> getHasPart();

  Optional<String> getHasVersion();

  Optional<String> getIdentifier();

  Optional<String> getInstructionalMethod();

  Optional<String> getIsFormatOf();

  Optional<String> getIsPartOf();

  Optional<String> getIsReferencedBy();

  Optional<String> getIsReplacedBy();

  Optional<String> getIsRequiredBy();

  Optional<String> getIssued();

  Optional<String> getIsVersionOf();

  Optional<String> getLanguage();

  Optional<String> getLicense();

  Optional<String> getMediator();

  Optional<String> getMedium();

  Optional<String> getModified();

  Optional<String> getProvenance();

  Optional<String> getPublisher();

  Optional<String> getReferences();

  Optional<String> getRelation();

  Optional<String> getReplaces();

  Optional<String> getRequires();

  Optional<String> getRights();

  Optional<String> getRightsHolder();

  Optional<String> getSource();

  Optional<String> getSpatial();

  Optional<String> getSubject();

  Optional<String> getTableOfContents();

  Optional<String> getTemporal();

  String getTitle();

  Optional<String> getType();

  Optional<String> getValid();
}
