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

import java.net.URI;
import java.net.URL;

/**
 * Describes the repository metadata format as defined in
 * <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#ListMetadataFormats">ListMetadataFormats</a>.
 */
public interface MetadataFormat {
  /**
   * The metadata prefix.
   */
  String getPrefix();

  /**
   * The location of the associated XML schema.
   */
  URL getSchema();

  /**
   * The XML namespace URI of this metadata format.
   */
  URI getNamespace();
}
