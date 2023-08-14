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

import org.opencastproject.oaipmh.OaiPmhConstants;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.util.data.Option;

import org.w3c.dom.Element;

/**
 * Metadata provider for the mandatory <code>oai_dc</code> metadata prefix.
 */
public class OaiDcMetadataProvider implements MetadataProvider {
  @Override
  public MetadataFormat getMetadataFormat() {
    return OaiPmhConstants.OAI_DC_METADATA_FORMAT;
  }

  @Override
  public Element createMetadata(OaiPmhRepository repository, final SearchResultItem item, final Option<String> set) {
    return new OaiXmlGen(repository) {
      @Override
      public Element create() {
        return dc(item, set);
      }
    }.create();
  }
}
