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

package org.opencastproject.ingestdownloadservice.impl.endpoint;



import org.opencastproject.ingestdownloadservice.api.IngestDownloadService;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;


/**
 * Test class for IngestDownloadServiceRest
 */
public class IngestDownloadServiceRestTest {

  private IngestDownloadServiceEndpoint endpoint;
  private IngestDownloadService service;

  @Before
  public void setUp() throws ServiceRegistryException {
    endpoint = new IngestDownloadServiceEndpoint();
    JobImpl job = new JobImpl();
    service = EasyMock.createMock(IngestDownloadService.class);
    EasyMock.expect(service.ingestDownload(EasyMock.anyObject(), EasyMock.eq("*/*"), EasyMock.eq(""),
            EasyMock.eq(false), EasyMock.eq(false))).andReturn(job).once();
    EasyMock.replay(service);
    endpoint.setIngestDownloadService(service);
  }

  @Test
  public void testEndpoint() throws Exception {
    MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    String mpStr = MediaPackageParser.getAsXml(mediaPackage);
    endpoint.ingestdownload(mpStr, "", "", "", "");
  }

}
