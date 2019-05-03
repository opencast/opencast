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

package org.opencastproject.serviceregistry.impl;

import static org.junit.Assert.assertEquals;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.jmx.JmxUtil;
import org.opencastproject.util.persistence.PersistenceUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.Executors;

import javax.management.ObjectInstance;
import javax.persistence.EntityManagerFactory;

public class ServiceRegistryJpaImplTest {
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryJpaImplTest.class);
  private Job undispatchableJob1 = null;
  private Job undispatchableJob2 = null;
  private EntityManagerFactory emf = null;
  private BundleContext bundleContext = null;
  private ComponentContext cc = null;
  private ServiceRegistryJpaImpl serviceRegistryJpaImpl = null;

  private static final String TEST_SERVICE = "ingest";
  private static final String TEST_SERVICE_2 = "compose";
  private static final String TEST_SERVICE_3 = "org.opencastproject.workflow";
  private static final String TEST_SERVICE_FAIRNESS = "fairness";
  private static final String TEST_OPERATION = "ingest";
  private static final String TEST_PATH = "/ingest";
  private static final String TEST_PATH_2 = "/compose";
  private static final String TEST_PATH_3 = "/workflow";
  private static final String TEST_HOST = "http://localhost:8080";
  private static final String TEST_HOST_OTHER = "http://otherhost:8080";
  private static final String TEST_HOST_THIRD = "http://thirdhost:8080";

  @Before
  public void setUp() throws Exception {
    // Setup JPA context
    setUpEntityManagerFactory();
    // Setup context settings
    setupBundleContext();
    setupComponentContext();
    // Setup test object.
    setUpServiceRegistryJpaImpl();
  }

  @After
  public void tearDown() throws ServiceRegistryException {
    for (ObjectInstance mbean : serviceRegistryJpaImpl.jmxBeans) {
      JmxUtil.unregisterMXBean(mbean);
    }
    for (ServiceRegistration service : serviceRegistryJpaImpl.getServiceRegistrations()) {
      serviceRegistryJpaImpl.unRegisterService(service.getServiceType(), service.getHost());
    }
    serviceRegistryJpaImpl.deactivate();
  }

  public void setUpUndispatchableJobs() throws ServiceRegistryException {
    undispatchableJob1 = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_OPERATION, null, null, false,
            null);
    undispatchableJob2 = serviceRegistryJpaImpl.createJob(TEST_HOST_OTHER, TEST_SERVICE, TEST_OPERATION, null, null,
            false, null);
    undispatchableJob1.setDateStarted(new Date());
    undispatchableJob1.setStatus(Status.RUNNING);
    undispatchableJob2.setDateStarted(new Date());
    undispatchableJob2.setStatus(Status.RUNNING);
    undispatchableJob1 = serviceRegistryJpaImpl.updateJob(undispatchableJob1);
    undispatchableJob2 = serviceRegistryJpaImpl.updateJob(undispatchableJob2);

  }

  public void setUpEntityManagerFactory() {
    emf = PersistenceUtil.newTestEntityManagerFactory("org.opencastproject.common");
  }

  public void setUpServiceRegistryJpaImpl()
          throws PropertyVetoException, NotFoundException, TrustedHttpClientException {
    serviceRegistryJpaImpl = new ServiceRegistryJpaImpl();
    serviceRegistryJpaImpl.setEntityManagerFactory(emf);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject())).andReturn(organization)
            .anyTimes();

    EasyMock.replay(organizationDirectoryService);
    serviceRegistryJpaImpl.setOrganizationDirectoryService(organizationDirectoryService);

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);
    User anonymous = new JaxbUser("anonymous", "test", jaxbOrganization,
            new JaxbRole(jaxbOrganization.getAnonymousRole(), jaxbOrganization));
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);
    serviceRegistryJpaImpl.setSecurityService(securityService);

    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyString())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);
    serviceRegistryJpaImpl.setUserDirectoryService(userDirectoryService);

    final Capture<HttpUriRequest> request = EasyMock.newCapture();
    final BasicHttpResponse successResponse = new BasicHttpResponse(
            new BasicStatusLine(new HttpVersion(1, 1), HttpStatus.SC_NO_CONTENT, "No message"));
    final BasicHttpResponse unavailableResponse = new BasicHttpResponse(
            new BasicStatusLine(new HttpVersion(1, 1), HttpStatus.SC_SERVICE_UNAVAILABLE, "No message"));
    TrustedHttpClient trustedHttpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    EasyMock.expect(trustedHttpClient.execute(EasyMock.capture(request))).andAnswer(new IAnswer<HttpResponse>() {
      @Override
      public HttpResponse answer() throws Throwable {
        if (!request.hasCaptured())
          return unavailableResponse;

        if (request.getValue().getURI().toString().contains(TEST_PATH))
          return unavailableResponse;

        if (request.getValue().getURI().toString().contains(TEST_PATH_3))
          return unavailableResponse;

        return successResponse;
      }
    }).anyTimes();
    EasyMock.replay(trustedHttpClient);
    serviceRegistryJpaImpl.setTrustedHttpClient(trustedHttpClient);
  }

  private void registerTestHostAndService() throws ServiceRegistryException {
    // register the hosts, service must be activated at this point
    serviceRegistryJpaImpl.registerHost(TEST_HOST, "127.0.0.1", 1024, 1, 1);
    serviceRegistryJpaImpl.registerHost(TEST_HOST_OTHER, "127.0.0.1", 1024, 1, 2);
    serviceRegistryJpaImpl.registerHost(TEST_HOST_THIRD, "127.0.0.1", 1024, 1, 4);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE, TEST_HOST, TEST_PATH);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE, TEST_HOST_OTHER, TEST_PATH);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE_2, TEST_HOST, TEST_PATH_2);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE_FAIRNESS, TEST_HOST, TEST_PATH_2);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE_FAIRNESS, TEST_HOST_OTHER, TEST_PATH_2);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE_FAIRNESS, TEST_HOST_THIRD, TEST_PATH_2);
  }

  private void setupBundleContext() throws InvalidSyntaxException {
    bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(OpencastConstants.SERVER_URL_PROPERTY)).andReturn("");
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.jobs.url")).andReturn("");
    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_MAXLOAD)).andReturn("");
    EasyMock.expect(bundleContext.createFilter((String) EasyMock.anyObject()))
            .andReturn(EasyMock.createNiceMock(Filter.class));
    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_DISPATCHINTERVAL)).andReturn("0");
  }

  private void setupComponentContext() {
    cc = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(cc);
  }

  @Test
  public void nullContextActivatesOkay() throws ServiceRegistryException {
    serviceRegistryJpaImpl.activate(null);
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteJobInvalidJobId() throws Exception {
    serviceRegistryJpaImpl.activate(null);
    serviceRegistryJpaImpl.removeJobs(Collections.singletonList(1L));
  }

  @Test
  public void testCancelUndispatchablesOrphanedByActivatingNode() throws Exception {
    serviceRegistryJpaImpl.activate(null);
    registerTestHostAndService();
    setUpUndispatchableJobs();
    // verify the current running status
    undispatchableJob1 = serviceRegistryJpaImpl.getJob(undispatchableJob1.getId());
    assertEquals(Status.RUNNING, undispatchableJob1.getStatus());
    undispatchableJob2 = serviceRegistryJpaImpl.getJob(undispatchableJob2.getId());
    assertEquals(Status.RUNNING, undispatchableJob2.getStatus());

    // remove the activate beans, so this can be reactivated
    for (ObjectInstance mbean : serviceRegistryJpaImpl.jmxBeans) {
      JmxUtil.unregisterMXBean(mbean);
    }

    // reactivate and expect local undispatchable job to be canceled, but not the remote job
    serviceRegistryJpaImpl.activate(null);
    logger.info("Undispatachable job 1 " + undispatchableJob1.getId());
    undispatchableJob1 = serviceRegistryJpaImpl.getJob(undispatchableJob1.getId());
    assertEquals(Status.CANCELED, undispatchableJob1.getStatus());
    logger.info("Undispatachable job 1 " + undispatchableJob2.getId());
    undispatchableJob2 = serviceRegistryJpaImpl.getJob(undispatchableJob2.getId());
    assertEquals(Status.RUNNING, undispatchableJob2.getStatus());
  }

  @Test
  public void testHostAddedToPriorityList() throws Exception {
    if (serviceRegistryJpaImpl.scheduledExecutor != null)
      serviceRegistryJpaImpl.scheduledExecutor.shutdown();
    serviceRegistryJpaImpl.scheduledExecutor = Executors.newScheduledThreadPool(1);
    serviceRegistryJpaImpl.activate(null);
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put("dispatchinterval", "1000");
    serviceRegistryJpaImpl.updated(properties);
    registerTestHostAndService();
    Job testJob = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_OPERATION, null, null, true, null);
    JobBarrier barrier = new JobBarrier(null, serviceRegistryJpaImpl, testJob);
    try {
      barrier.waitForJobs(2000);
      Assert.fail();
    } catch (Exception e) {
      Assert.assertEquals(1, serviceRegistryJpaImpl.dispatchPriorityList.size());
    }
  }

  @Test
  public void testHostAddedToPriorityListExceptWorkflowType() throws Exception {
    if (serviceRegistryJpaImpl.scheduledExecutor != null)
      serviceRegistryJpaImpl.scheduledExecutor.shutdown();
    serviceRegistryJpaImpl.scheduledExecutor = Executors.newScheduledThreadPool(1);
    serviceRegistryJpaImpl.activate(null);
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put("dispatchinterval", "1000");
    serviceRegistryJpaImpl.updated(properties);
    registerTestHostAndService();
    serviceRegistryJpaImpl.registerService(TEST_SERVICE_3, TEST_HOST, TEST_PATH_3);
    Job testJob = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_3, TEST_OPERATION, null, null, true, null);
    JobBarrier barrier = new JobBarrier(null, serviceRegistryJpaImpl, testJob);
    try {
      barrier.waitForJobs(2000);
      Assert.fail();
    } catch (Exception e) {
      Assert.assertEquals(0, serviceRegistryJpaImpl.dispatchPriorityList.size());
    }
  }

  @Test
  public void testHostsBeingRemovedFromPriorityList() throws Exception {
    if (serviceRegistryJpaImpl.scheduledExecutor != null)
      serviceRegistryJpaImpl.scheduledExecutor.shutdown();
    serviceRegistryJpaImpl.scheduledExecutor = Executors.newScheduledThreadPool(1);
    serviceRegistryJpaImpl.activate(null);
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put("dispatchinterval", "1000");
    serviceRegistryJpaImpl.updated(properties);
    registerTestHostAndService();
    serviceRegistryJpaImpl.dispatchPriorityList.put(0L, TEST_HOST);
    Job testJob = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_2, TEST_OPERATION, null, null, true, null);
    JobBarrier barrier = new JobBarrier(null, serviceRegistryJpaImpl, testJob);
    try {
      barrier.waitForJobs(2000);
      Assert.fail();
    } catch (Exception e) {
      Assert.assertEquals(0, serviceRegistryJpaImpl.dispatchPriorityList.size());
    }
  }

  @Test
  public void testIgnoreHostsInPriorityList() throws Exception {
    if (serviceRegistryJpaImpl.scheduledExecutor != null)
      serviceRegistryJpaImpl.scheduledExecutor.shutdown();
    serviceRegistryJpaImpl.scheduledExecutor = Executors.newScheduledThreadPool(1);
    serviceRegistryJpaImpl.activate(null);
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put("dispatchinterval", "1000");
    serviceRegistryJpaImpl.updated(properties);
    registerTestHostAndService();
    Job testJob = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_2, TEST_OPERATION, null, null, true, null);
    Job testJob2 = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_OPERATION, null, null, true, null);
    serviceRegistryJpaImpl.dispatchPriorityList.put(testJob2.getId(), TEST_HOST);
    JobBarrier barrier = new JobBarrier(null, serviceRegistryJpaImpl, testJob, testJob2);
    try {
      barrier.waitForJobs(2000);
      Assert.fail();
    } catch (Exception e) {
      Assert.assertTrue(StringUtils.isBlank(serviceRegistryJpaImpl.getJob(testJob.getId()).getProcessingHost()));
      Assert.assertTrue(StringUtils.isNotBlank(serviceRegistryJpaImpl.getJob(testJob2.getId()).getProcessingHost()));
      Assert.assertEquals(1, serviceRegistryJpaImpl.dispatchPriorityList.size());
      String blockingHost = serviceRegistryJpaImpl.dispatchPriorityList.get(testJob2.getId());
      Assert.assertEquals(TEST_HOST, blockingHost);
    }
  }

  private void assertHostloads(Job j, Float a, Float b, Float c) throws Exception {
    Thread.sleep(1100); //1100 is 100ms more than the minimum job dispatch interval.  Setting this lower causes race conditions.
    Job k = serviceRegistryJpaImpl.getJob(j.getId());
    k.setStatus(Status.RUNNING);
    serviceRegistryJpaImpl.updateJob(k);
    SystemLoad hostloads = serviceRegistryJpaImpl.getHostLoads(emf.createEntityManager());
    Assert.assertTrue(String.format("Host load is incorrect, should be %f, is %f", a, hostloads.get(TEST_HOST).getCurrentLoad()),
            hostloads.get(TEST_HOST).getCurrentLoad() - a >= 0.0f);
    Assert.assertTrue(String.format("Host load is incorrect, should be %f, is %f", a, hostloads.get(TEST_HOST).getCurrentLoad()),
            hostloads.get(TEST_HOST).getCurrentLoad() - a < 0.1f);
    Assert.assertTrue(String.format("Host load is incorrect, should be %f, is %f", b, hostloads.get(TEST_HOST_OTHER).getCurrentLoad()),
            hostloads.get(TEST_HOST_OTHER).getCurrentLoad() - b >= 0.0f);
    Assert.assertTrue(String.format("Host load is incorrect, should be %f, is %f", b, hostloads.get(TEST_HOST_OTHER).getCurrentLoad()),
            hostloads.get(TEST_HOST_OTHER).getCurrentLoad() - b < 0.1f);
    Assert.assertTrue(String.format("Host load is incorrect, should be %f, is %f", c, hostloads.get(TEST_HOST_THIRD).getCurrentLoad()),
            hostloads.get(TEST_HOST_THIRD).getCurrentLoad() - c >= 0.0f);
    Assert.assertTrue(String.format("Host load is incorrect, should be %f, is %f", c, hostloads.get(TEST_HOST_THIRD).getCurrentLoad()),
            hostloads.get(TEST_HOST_THIRD).getCurrentLoad() - c < 0.1f);
  }

  @Test
  public void testJobDispatchingFairness() throws Exception {
    if (serviceRegistryJpaImpl.scheduledExecutor != null)
      serviceRegistryJpaImpl.scheduledExecutor.shutdown();
    serviceRegistryJpaImpl.scheduledExecutor = Executors.newScheduledThreadPool(1);
    serviceRegistryJpaImpl.activate(null);
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put("dispatchinterval", "1000");
    serviceRegistryJpaImpl.updated(properties);
    registerTestHostAndService();

    Job j = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_FAIRNESS, TEST_OPERATION, null, null, true, null, 1.0f);
    assertHostloads(j, 0.0f, 0.0f, 1.0f);
    j = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_FAIRNESS, TEST_OPERATION, null, null, true, null, 1.0f);
    assertHostloads(j, 0.0f, 1.0f, 1.0f);
    j = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_FAIRNESS, TEST_OPERATION, null, null, true, null, 1.0f);
    assertHostloads(j, 1.0f, 1.0f, 1.0f);
    j = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_FAIRNESS, TEST_OPERATION, null, null, true, null, 1.0f);
    assertHostloads(j,1.0f, 1.0f, 2.0f);
    j = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_FAIRNESS, TEST_OPERATION, null, null, true, null, 1.0f);
    assertHostloads(j, 1.0f, 1.0f, 3.0f);
    j = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_FAIRNESS, TEST_OPERATION, null, null, true, null, 1.0f);
    assertHostloads(j, 1.0f, 2.0f, 3.0f);
    j = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_FAIRNESS, TEST_OPERATION, null, null, true, null, 1.0f);
    assertHostloads(j,1.0f, 2.0f, 4.0f);
    serviceRegistryJpaImpl.deactivate();
  }


  @Test
  public void testDispatchingJobsHigherMaxLoad() throws Exception {
    if (serviceRegistryJpaImpl.scheduledExecutor != null)
      serviceRegistryJpaImpl.scheduledExecutor.shutdown();
    serviceRegistryJpaImpl.scheduledExecutor = Executors.newScheduledThreadPool(1);
    serviceRegistryJpaImpl.activate(null);
    Hashtable<String, String> properties = new Hashtable<>();
    properties.put("dispatchinterval", "1000");
    serviceRegistryJpaImpl.updated(properties);
    registerTestHostAndService();
    Job testJob = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_OPERATION, null, null, true, null,
            10.0f);
    JobBarrier barrier = new JobBarrier(null, serviceRegistryJpaImpl, testJob);
    try {
      barrier.waitForJobs(2000);
      //We should never successfully complete the job, so if we get here then something is wrong
      Assert.fail();
    } catch (Exception e) {
      testJob = serviceRegistryJpaImpl.getJob(testJob.getId());
      //Some explanation here: If the load exceeds the global maximum node load (ie, jobLoad > all individual node max
      // loads), then we dispatch to the biggest, even if it's not going to normally accept the job.  That node may still
      // reject the job, but that's AbstractJobProducer's job, not the service registry's
      Assert.assertEquals(TEST_HOST_OTHER, testJob.getProcessingHost());
    }
  }

  @Test
  public void testUpdateJobFailed() throws Exception {
    serviceRegistryJpaImpl.activate(null);
    registerTestHostAndService();
    Job job = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, null, null, true, null, 1.0f);
    job.setStatus(Job.Status.FAILED);
    Job updatedJob = serviceRegistryJpaImpl.updateJob(job);
    Assert.assertNotNull(updatedJob.getDateCompleted());
  }

  @Test
  public void testUpdateJobFinished() throws Exception {
    serviceRegistryJpaImpl.activate(null);
    registerTestHostAndService();
    Job job = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, null, null, true, null, 1.0f);
    job.setStatus(Job.Status.FINISHED);
    Job updatedJob = serviceRegistryJpaImpl.updateJob(job);
    Assert.assertNotNull(updatedJob.getDateCompleted());
    Assert.assertNotNull(updatedJob.getRunTime());
  }

  @Test
  public void testCompletedRuntimeDoNotChange() throws Exception {
    serviceRegistryJpaImpl.activate(null);
    registerTestHostAndService();
    Job job = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, null, null, true, null, 1.0f);
    job.setStatus(Job.Status.FINISHED);
    Job updatedJob = serviceRegistryJpaImpl.updateJob(job);
    Date dateCompleted = updatedJob.getDateCompleted();
    Long runTime = updatedJob.getRunTime();
    updatedJob = serviceRegistryJpaImpl.updateJob(updatedJob);
    Assert.assertEquals(dateCompleted, updatedJob.getDateCompleted());
    Assert.assertEquals(runTime, updatedJob.getRunTime());
  }

}
