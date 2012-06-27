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

package org.opencastproject.oaipmh.server;

import org.apache.commons.lang.StringUtils;
import org.opencastproject.oaipmh.OaiPmhConstants;
import org.opencastproject.util.data.Option;

import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.opencastproject.util.data.Option.eq;
import static org.opencastproject.util.data.Option.wrap;

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
    return wrap(trimToNull(getParameter("verb")));
  }

  Option<String> getIdentifier() {
    return wrap(trimToNull(getParameter("identifier")));
  }

  Option<String> getMetadataPrefix() {
    return wrap(trimToNull(getParameter("metadataPrefix")));
  }

  Option<String> getFrom() {
    return wrap(trimToNull(getParameter("from")));
  }

  Option<String> getUntil() {
    return wrap(trimToNull(getParameter("until")));
  }

  Option<String> getSet() {
    return wrap(StringUtils.trimToNull(getParameter("set")));
  }

  Option<String> getResumptionToken() {
    return wrap(trimToNull(getParameter("resumptionToken")));
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
