/*
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

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.opencastproject.oaipmh.server.OaiPmhServerInfoUtil.ORG_CFG_OAIPMH_SERVER_HOSTURL;

import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.oaipmh.server.OaiPmhServerInfo;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.serviceregistry.api.UndispatchableJobException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Collections;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OaiPmhPublicationServiceImplTest {

  public static final String OAI_PMH_SERVER_URL = "http://myorg.tld";
  public static final String OAI_PMH_SERVER_MOUNT_POINT = "/oaipmh";


  private MediaPackage mp = null;
  private MediaPackage mp2 = null;
  private List<String> validOaiPmhRepositories = null;
  private OaiPmhPublicationServiceImpl service = null;
  private ServiceRegistryInMemoryImpl serviceRegistry = null;

  @Before
  public void setUp() throws Exception {
    mp = MediaPackageSupport.loadFromClassPath("/mediapackage.xml");
    mp2 = MediaPackageSupport.loadFromClassPath("/mediapackage2.xml");
    validOaiPmhRepositories = Collections.list("default");

    OaiPmhServerInfo oaiPmhServerInfo = EasyMock.createNiceMock(OaiPmhServerInfo.class);
    EasyMock.expect(oaiPmhServerInfo.hasRepo(anyString()))
            .andAnswer(() -> validOaiPmhRepositories.contains((String)EasyMock.getCurrentArguments()[0])).anyTimes();
    EasyMock.expect(oaiPmhServerInfo.getMountPoint()).andReturn(OAI_PMH_SERVER_MOUNT_POINT).anyTimes();

    DefaultOrganization org = new DefaultOrganization() {
      @Override
      public Map<String, String> getProperties() {
        HashMap<String, String> props = new HashMap<>();
        props.putAll(DEFAULT_PROPERTIES);
        props.put(ORG_CFG_OAIPMH_SERVER_HOSTURL, OAI_PMH_SERVER_URL);
        return props;
      }
    };
    HashSet<JaxbRole> roles = new HashSet<JaxbRole>();
    roles.add(new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, org, ""));
    User user = new JaxbUser("admin", "test", org, roles);
    OrganizationDirectoryService orgDirectory = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirectory.getOrganization((String) EasyMock.anyObject())).andReturn(org).anyTimes();

    UserDirectoryService userDirectory = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectory.loadUser("admin")).andReturn(user).anyTimes();

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectory, orgDirectory,
            EasyMock.createNiceMock(IncidentService.class));

    // Finish setting up the mocks
    EasyMock.replay(oaiPmhServerInfo, orgDirectory, userDirectory, securityService);

    service = new OaiPmhPublicationServiceImpl();
    service.setOaiPmhServerInfo(oaiPmhServerInfo);
    service.setSecurityService(securityService);
    service.setServiceRegistry(serviceRegistry);

    // mock streaming/download distribution jobs dispatching
    AbstractJobProducer distributionJobProducerMock = new AbstractJobProducer("distribute") {
      @Override
      protected ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
      }

      @Override
      protected SecurityService getSecurityService() {
        return securityService;
      }

      @Override
      protected UserDirectoryService getUserDirectoryService() {
        return userDirectory;
      }

      @Override
      protected OrganizationDirectoryService getOrganizationDirectoryService() {
        return orgDirectory;
      }

      @Override
      protected String process(Job job) throws Exception {
        return job.getPayload();
      }
      @Override
      public boolean isReadyToAccept(Job job) throws ServiceRegistryException, UndispatchableJobException {
        return true;
      }
    };
    serviceRegistry.registerService(distributionJobProducerMock);
  }

  @Test
  public void testPublish() throws MediaPackageException, PublicationException, ServiceRegistryException {
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    Job dummyJob = EasyMock.createMock(Job.class);
    Capture<List<String>> jobArgsCapture = EasyMock.newCapture();
    EasyMock.expect(serviceRegistry.createJob(
            eq(OaiPmhPublicationService.JOB_TYPE),
            eq(OaiPmhPublicationServiceImpl.Operation.Publish.toString()),
            capture(jobArgsCapture))).andReturn(dummyJob).once();
    EasyMock.replay(serviceRegistry);
    service.setServiceRegistry(serviceRegistry);

    Job j = service.publish(mp, "default",
            Collections.set("catalog-1", "catalog-2", "track-1"), Collections.set("track-1"),true);

    Assert.assertSame(dummyJob, j);
    List<String> jobArgs = jobArgsCapture.getValue();
    // test job arguments
    Assert.assertEquals(5, jobArgs.size());
    Assert.assertTrue(jobArgs.get(0).contains("<mediapackage "));
    Assert.assertEquals("default", jobArgs.get(1));
    String downloadDistributionIdsArg = jobArgs.get(2);
    Assert.assertNotNull(downloadDistributionIdsArg);
    Assert.assertTrue(downloadDistributionIdsArg.contains("catalog-1"));
    Assert.assertTrue(downloadDistributionIdsArg.contains("catalog-2"));
    Assert.assertTrue(downloadDistributionIdsArg.contains("track-1"));
    String streamingDistributionIdsArg = jobArgs.get(3);
    Assert.assertNotNull(streamingDistributionIdsArg);
    Assert.assertTrue(!streamingDistributionIdsArg.contains("catalog-1"));
    Assert.assertTrue(!streamingDistributionIdsArg.contains("catalog-2"));
    Assert.assertTrue(streamingDistributionIdsArg.contains("track-1"));
    Assert.assertTrue(BooleanUtils.toBoolean(jobArgs.get(4)));
  }

  @Test
  public void testRetract() throws NotFoundException, PublicationException, ServiceRegistryException {
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    Job dummyJob = EasyMock.createMock(Job.class);
    Capture<List<String>> jobArgsCapture = EasyMock.newCapture();
    EasyMock.expect(serviceRegistry.createJob(
            eq(OaiPmhPublicationService.JOB_TYPE),
            eq(OaiPmhPublicationServiceImpl.Operation.Retract.toString()),
            capture(jobArgsCapture))).andReturn(dummyJob).once();
    EasyMock.replay(serviceRegistry);
    service.setServiceRegistry(serviceRegistry);

    Job j = service.retract(mp2, "default");

    Assert.assertSame(dummyJob, j);
    // test job arguments
    List<String> jobArgs = jobArgsCapture.getValue();
    Assert.assertEquals(2, jobArgs.size());
    Assert.assertTrue(jobArgs.get(0).contains("<mediapackage "));
    Assert.assertEquals("default", jobArgs.get(1));
  }

  @Test
  public void testUpdateMetadata() throws PublicationException, ServiceRegistryException, MediaPackageException {
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    Job dummyJob = EasyMock.createMock(Job.class);
    Capture<List<String>> jobArgsCapture = EasyMock.newCapture();
    EasyMock.expect(serviceRegistry.createJob(
            eq(OaiPmhPublicationService.JOB_TYPE),
            eq(OaiPmhPublicationServiceImpl.Operation.UpdateMetadata.toString()),
            capture(jobArgsCapture))).andReturn(dummyJob).once();
    EasyMock.replay(serviceRegistry);
    service.setServiceRegistry(serviceRegistry);

    Set<String> flavorsSet = Collections.set("dublincore/*", "security/*");
    Set<String> tagsSet = Collections.set("archive", "other");
    Job j = service.updateMetadata(mp, "default", flavorsSet, tagsSet, true);

    Assert.assertSame(dummyJob, j);
    // test job arguments
    List<String> jobArgs = jobArgsCapture.getValue();
    Assert.assertEquals(5, jobArgs.size());
    Assert.assertTrue(jobArgs.get(0).contains("<mediapackage "));
    Assert.assertEquals("default", jobArgs.get(1));
    Assert.assertEquals(StringUtils.join(flavorsSet, OaiPmhPublicationServiceImpl.SEPARATOR), jobArgs.get(2));
    Assert.assertEquals(StringUtils.join(tagsSet, OaiPmhPublicationServiceImpl.SEPARATOR), jobArgs.get(3));
    Assert.assertTrue(Boolean.valueOf(jobArgs.get(4)));
  }
}
