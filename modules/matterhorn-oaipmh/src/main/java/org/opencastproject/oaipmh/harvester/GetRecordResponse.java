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
package org.opencastproject.oaipmh.harvester;

import org.opencastproject.oaipmh.OaiPmhConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * The "GetRecord" response. See <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#GetRecord">4.1
 * GetRecord</a> for further information.
 */
public class GetRecordResponse extends OaiPmhResponse {
  public GetRecordResponse(Document doc) {
    super(doc);
  }

  /**
   * Get the <em>content</em> of the metadata element.
   */
  public Node getMetadataElem() {
    return xpathNode("/oai20:OAI-PMH/oai20:GetRecord/oai20:record/oai20:metadata/*[1]");
  }

  /**
   * Get the records in the current response.
   *
   * <pre>
   *  &lt;record&gt;
   *    &lt;header&gt;...
   *    &lt;/header&gt;
   *    &lt;metadata&gt;...
   *    &lt;/metadata&gt;
   *  &lt;/record&gt;
   * </pre>
   */
  public Node getRecord() {
    return xpathNode("/oai20:OAI-PMH/oai20:GetRecord/oai20:record");
  }

  /**
   * Extract the content, i.e. the first child node, of the metadata node of a record.
   *
   * <pre>
   *  &lt;record&gt;
   *    &lt;header&gt;...
   *    &lt;/header&gt;
   *    &lt;metadata&gt;
   *      &lt;myMd&gt;
   *      &lt;/myMd&gt;
   *    &lt;/metadata&gt;
   *  &lt;/record&gt;
   *
   *  =&gt;
   *
   *  &lt;myMd&gt;
   *  &lt;/myMd&gt;
   * </pre>
   */
  public static Node metadataOfRecord(Node recordNode) {
    return xpathNode(createXPath(), recordNode, "oai20:metadata/*[1]");
  }

  public String getMetadataPrefix() {
    return xpathString("/oai20:OAI-PMH/oai20:request/@metadataPrefix");
  }

  /** Check if the record is marked as deleted. */
  public boolean isDeleted() {
    return "deleted".equals(xpathString("/oai20:OAI-PMH/oai20:GetRecord/oai20:record/oai20:header/@status"));
  }

  public boolean isErrorIdDoesNotExist() {
    return isError(OaiPmhConstants.ERROR_ID_DOES_NOT_EXIST);
  }

  public boolean isErrorBadArgument() {
    return isError(OaiPmhConstants.ERROR_BAD_ARGUMENT);
  }

  public boolean isErrorCannotDisseminateFormat() {
    return isError(OaiPmhConstants.ERROR_CANNOT_DISSEMINATE_FORMAT);
  }
}
