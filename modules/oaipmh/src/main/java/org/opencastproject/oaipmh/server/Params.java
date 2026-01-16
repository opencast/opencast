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
package org.opencastproject.oaipmh.server;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import org.opencastproject.oaipmh.OaiPmhConstants;

import java.util.Optional;

/**
 * Helper to encapsulate HTTP parameter handling.
 */
public abstract class Params {
  /**
   * Implement this method to supply the needed parameters.
   *
   * @return the parameter or null or a blank string to indicate a missing parameter
   */
  abstract String getParameter(String key);

  /**
   * Return the complete repository URL. This is everything before the query parameters.
   * Examples: http://localhost:8080/oaipmh or http://localhost:8080/oaipmh/cq5
   */
  abstract String getRepositoryUrl();

  boolean isVerbGetRecord() {
    return getVerb().map(s -> OaiPmhConstants.VERB_GET_RECORD.equals(s)).orElse(false);
  }

  boolean isVerbIdentify() {
    return getVerb().map(s -> OaiPmhConstants.VERB_IDENTIFY.equals(s)).orElse(false);
  }

  boolean isVerbListMetadataFormats() {
    return getVerb().map(s -> OaiPmhConstants.VERB_LIST_METADATA_FORMATS.equals(s)).orElse(false);
  }

  boolean isVerbListSets() {
    return getVerb().map(s -> OaiPmhConstants.VERB_LIST_SETS.equals(s)).orElse(false);
  }

  boolean isVerbListIdentifiers() {
    return getVerb().map(s -> OaiPmhConstants.VERB_LIST_IDENTIFIERS.equals(s)).orElse(false);
  }

  boolean isVerbListRecords() {
    return getVerb().map(s -> OaiPmhConstants.VERB_LIST_RECORDS.equals(s)).orElse(false);
  }

  Optional<String> getVerb() {
    return Optional.ofNullable(trimToNull(getParameter("verb")));
  }

  Optional<String> getIdentifier() {
    return Optional.ofNullable(trimToNull(getParameter("identifier")));
  }

  Optional<String> getMetadataPrefix() {
    return Optional.ofNullable(trimToNull(getParameter("metadataPrefix")));
  }

  Optional<String> getFrom() {
    return Optional.ofNullable(trimToNull(getParameter("from")));
  }

  Optional<String> getUntil() {
    return Optional.ofNullable(trimToNull(getParameter("until")));
  }

  Optional<String> getSet() {
    return Optional.ofNullable(trimToNull(getParameter("set")));
  }

  Optional<String> getResumptionToken() {
    return Optional.ofNullable(trimToNull(getParameter("resumptionToken")));
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Params");
    sb.append("{verb=");
    sb.append(getVerb());
    sb.append("}");
    return sb.toString();
  }
}
