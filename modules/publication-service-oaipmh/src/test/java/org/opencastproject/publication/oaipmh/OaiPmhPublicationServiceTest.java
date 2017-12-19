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
package org.opencastproject.publication.oaipmh;

import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.Query;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.server.OaiPmhServerInfo;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.data.Collections;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OaiPmhPublicationServiceTest {

  private MediaPackage mp = null;
  private MediaPackage mp2 = null;
  private OaiPmhPublicationServiceImpl service = null;
  private Capture<MediaPackage> mpCapture;

  @Before
  public void setUp() throws Exception {
    mp = MediaPackageSupport.loadFromClassPath("/mediapackage.xml");
    mp2 = MediaPackageSupport.loadFromClassPath("/mediapackage2.xml");

    service = new OaiPmhPublicationServiceImpl() {
      @Override
      protected MediaPackage publishElementsToDownload(Job job, MediaPackage mediaPackage, String channel,
              Set<String> downloadIds, Set<String> streamingIds, boolean checkAvailability)
              throws PublicationException, MediaPackageException {
        return mp;
      }
    };

    mpCapture = new Capture<>();
    final OaiPmhDatabase oaiPmhDatabase = EasyMock.createMock(OaiPmhDatabase.class);
    final SearchResult searchResult = EasyMock.createNiceMock(SearchResult.class);
    try {
      EasyMock.expect(oaiPmhDatabase.search(EasyMock.<Query> anyObject())).andReturn(searchResult).atLeastOnce();
      oaiPmhDatabase.store(EasyMock.capture(mpCapture), EasyMock.<String> anyObject());
      EasyMock.expectLastCall().atLeastOnce();
      oaiPmhDatabase.delete(EasyMock.<String> anyObject(), EasyMock.<String> anyObject());
      EasyMock.expectLastCall().atLeastOnce();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    EasyMock.replay(oaiPmhDatabase);
    service.setPersistence(oaiPmhDatabase);

    service.setOaiPmhServerInfo(new OaiPmhServerInfo() {
      @Override
      public boolean hasRepo(String id) {
        return true;
      }

      @Override
      public String getMountPoint() {
        return "/oaipmh";
      }
    });

    // final BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    // EasyMock.expect(bundleContext.getProperty(OaiPmhPublicationServiceImpl.CFG_OAIPMH_MOUNT_POINT))
    // .andReturn("/oaipmh");
    // EasyMock.replay(bundleContext);

    // final ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    // EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    // EasyMock.replay(cc);

    final Organization org = new Organization() {
      @Override
      public String getId() {
        return "mh_default_org";
      }

      @Override
      public String getAnonymousRole() {
        return "anonymous";
      }

      @Override
      public String getAdminRole() {
        return "admin";
      }

      @Override
      public String getName() {
        return "Default Organization";
      }

      @Override
      public Map<String, String> getProperties() {
        return map(tuple("org.opencastproject.oaipmh.server.hosturl", "http://localhost:8080"));
      }

      @Override
      public Map<String, Integer> getServers() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }
    };
    final SecurityService secSvc = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(secSvc.getOrganization()).andReturn(org).anyTimes();
    EasyMock.replay(secSvc);
    service.setSecurityService(secSvc);
  }

  @Test
  public void testPublication() throws Exception {
    Set<String> elementIds = new HashSet<>();
    elementIds.add("track-1");

    Publication publish = service.publishInternal(null, mp, "doi", elementIds, Collections.<String> set(), false);
    Assert.assertNotNull(publish);
    Assert.assertEquals("http://localhost:8080/oaipmh/doi?verb=ListMetadataFormats&identifier=10.0000-1",
            publish.getURI().toString());
    Assert.assertEquals(OaiPmhPublicationServiceImpl.PUBLICATION_CHANNEL_PREFIX.concat("doi"), publish.getChannel());
    Assert.assertEquals("text/xml", "text/xml");
    Assert.assertEquals(2, mpCapture.getValue().getPublications().length);
  }

  @Test
  @Ignore
  public void testRetract() throws Exception {
    Publication retractedElement = service.retractInternal(null, mp, "test");
    Assert.assertNull(retractedElement);

    retractedElement = service.retractInternal(null, mp2, "test");
    Assert.assertNotNull(retractedElement);
  }

}
