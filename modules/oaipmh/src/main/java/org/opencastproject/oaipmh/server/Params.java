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
package org.opencastproject.oaipmh.server;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.util.data.Option.eq;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.oaipmh.OaiPmhConstants;
import org.opencastproject.util.data.Option;

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
    return getVerb().map(eq(OaiPmhConstants.VERB_GET_RECORD)).getOrElse(false);
  }

  boolean isVerbIdentify() {
    return getVerb().map(eq(OaiPmhConstants.VERB_IDENTIFY)).getOrElse(false);
  }

  boolean isVerbListMetadataFormats() {
    return getVerb().map(eq(OaiPmhConstants.VERB_LIST_METADATA_FORMATS)).getOrElse(false);
  }

  boolean isVerbListSets() {
    return getVerb().map(eq(OaiPmhConstants.VERB_LIST_SETS)).getOrElse(false);
  }

  boolean isVerbListIdentifiers() {
    return getVerb().map(eq(OaiPmhConstants.VERB_LIST_IDENTIFIERS)).getOrElse(false);
  }

  boolean isVerbListRecords() {
    return getVerb().map(eq(OaiPmhConstants.VERB_LIST_RECORDS)).getOrElse(false);
  }

  Option<String> getVerb() {
    return option(trimToNull(getParameter("verb")));
  }

  Option<String> getIdentifier() {
    return option(trimToNull(getParameter("identifier")));
  }

  Option<String> getMetadataPrefix() {
    return option(trimToNull(getParameter("metadataPrefix")));
  }

  Option<String> getFrom() {
    return option(trimToNull(getParameter("from")));
  }

  Option<String> getUntil() {
    return option(trimToNull(getParameter("until")));
  }

  Option<String> getSet() {
    return option(trimToNull(getParameter("set")));
  }

  Option<String> getResumptionToken() {
    return option(trimToNull(getParameter("resumptionToken")));
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
