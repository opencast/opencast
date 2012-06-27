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

package org.opencastproject.oaipmh.harvester;

import org.opencastproject.oaipmh.Granularity;
import org.opencastproject.oaipmh.OaiPmhUtil;
import org.w3c.dom.Document;

/**
 * The "Identify" response.
 * See <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#Identify">4.2 Identify</a> for further
 * information.
 * <p/>
 * todo implement missing element accessors
 */
public class IdentifyResponse extends OaiPmhResponse {

  public IdentifyResponse(Document doc) {
    super(doc);
  }

  public Granularity getGranularity() {
    String v = xpathString("/oai20:OAI-PMH/oai20:Identify/oai20:granularity/text()");
    try {
      return OaiPmhUtil.fromOaiRepresentation(v);
    } catch (IllegalArgumentException e) {
      throw new MalformedResponseException("Identify response does not contain granularity", e);
    }
  }
}
