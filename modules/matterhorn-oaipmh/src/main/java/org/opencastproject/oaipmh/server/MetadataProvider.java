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

import org.opencastproject.search.api.SearchResultItem;
import org.w3c.dom.Element;

/**
 * A metadata provider provides XML serialized metadata for a certain OAI-PMH metadata prefix.
 * For further information about metadata prefixes see the section
 * <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#MetadataNamespaces">3.4 metadataPrefix and Metadata Schema</a>
 * of the OAI-PMH specification.
 */
public interface MetadataProvider {
  /**
   * Return the metadata format handled by this provider.
   */
  MetadataFormat getMetadataFormat();

  /**
   * Transform a search result item into a piece of XML metadata.
   */
  Element createMetadata(OaiPmhRepository repository, SearchResultItem item);
}
