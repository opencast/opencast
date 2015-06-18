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


package org.opencastproject.oaipmh.matterhorn;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.oaipmh.harvester.ListRecordsResponse;
import org.opencastproject.oaipmh.harvester.RecordHandler;
import org.opencastproject.search.api.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * Handle records from the "matterhorn" metadata prefix
 * that contain whole media packages and feed them to the search service.
 */
public class MatterhornRecordHandler implements RecordHandler {
  private static final Logger logger = LoggerFactory.getLogger(MatterhornRecordHandler.class);

  private final MediaPackageBuilder mediaPackageBuilder;

  private SearchService searchService;

  public MatterhornRecordHandler() {
    mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
  }

  /**
   * Set the search service. To be called by the OSGi container.
   */
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  @Override
  public String getMetadataPrefix() {
    return "matterhorn";
  }

  @Override
  public void handle(Node record) {
    Node mediaPackageNode = ListRecordsResponse.metadataOfRecord(record);
    final MediaPackage mediaPackage;
    try {
      mediaPackage = mediaPackageBuilder.loadFromXml(mediaPackageNode);
    } catch (MediaPackageException e) {
      throw new RuntimeException(e);
    }
    logger.info("Harvested mediapackage " + mediaPackage.getIdentifier().toString());
    try {
      searchService.add(mediaPackage);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
