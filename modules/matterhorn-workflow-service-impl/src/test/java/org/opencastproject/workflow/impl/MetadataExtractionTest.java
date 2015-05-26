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

package org.opencastproject.workflow.impl;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.metadata.api.MediapackageMetadataImpl;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MetadataExtractionTest {
  private WorkflowServiceImpl service;

  // local resources

  // mock services and objects
  private MediaPackageMetadataService metadataService;
  private MediapackageMetadataImpl metadata;

  private static final String TITLE = "title";
  private static final String SERIES = "series";

  @Before
  public void setUp() throws Exception {
    // set up service
    service = new WorkflowServiceImpl();

    // set up mock metadata and metadata service providing it
    metadata = new MediapackageMetadataImpl();
    metadata.setTitle(TITLE);
    metadata.setSeriesTitle(SERIES);
    metadataService = EasyMock.createNiceMock(MediaPackageMetadataService.class);
    EasyMock.expect(metadataService.getMetadata((MediaPackage) EasyMock.anyObject())).andReturn(metadata);
    EasyMock.replay(metadataService);
    service.addMetadataService(metadataService);
  }

  @Test
  public void testMetadata() throws Exception {
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    Assert.assertNull(mp.getTitle());
    // now run the mediapackage through the metadata population process. This works here because the mock always
    // returns metadata.
    service.populateMediaPackageMetadata(mp);
    Assert.assertEquals(TITLE, mp.getTitle());
    Assert.assertEquals(SERIES, mp.getSeriesTitle());
  }
}
