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

import org.w3c.dom.Node;

/**
 * Pluggable component to handle OAI-PMH records harvested by the {@link OaiPmhHarvester}.
 */
public interface RecordHandler {

  /**
   * Return the OAI-PMH metadata prefix this handler deals with.
   * See <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#MetadataNamespaces">this section</a>
   * of the OAI-PMH specification for more details.
   */
  String getMetadataPrefix();

  /**
   * Handle an OAI-PMH record.
   * See section <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#Record">2.5 Record</a> of the
   * OAI-PMH specification.
   */
  void handle(Node record);
}
