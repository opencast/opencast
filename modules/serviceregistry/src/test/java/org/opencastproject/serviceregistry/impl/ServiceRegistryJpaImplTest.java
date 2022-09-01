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
import static org.junit.Assert.assertThrows;

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
import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.JobProducerHeartbeat;
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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectInstance;
import javax.persistence.EntityManagerFactory;

public class ServiceRegistryJpaImplTest {
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryJpaImplTest.class);
  private Job undispatchableJob1 = null;
  private Job undispatchableJob2 = null;
  private static EntityManagerFactory emf = null;
  private static BundleContext bundleContext = null;
  private static ComponentContext cc = null;
  private static ServiceRegistryJpaImpl serviceRegistryJpaImpl = null;
  private static JobDispatcher jobDispatcher = null;


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

  private static final long JOB_BARRIER_TIMEOUT = 100L; //in ms
  private static final long DISPATCH_START_DELAY = 10L; //in ms

  @Rule public TestWatcher watcher = new TestWatcher() {
      protected void starting(Description description) {
      logger.info("Test '{}' is about to start ...", description.getMethodName());
    }
  };

  @BeforeClass
  public static void setUpOnce() throws Exception {
    // Setup JPA context
    setUpEntityManagerFactory();
    // Setup context settings
    setupBundleContext();
    setupComponentContext();
    // Setup test object.
    setUpServiceRegistryJpaImpl();
  }

  @Before
  public void cleanBeforeEach() throws ServiceRegistryException, NotFoundException, ConfigurationException {
    logger.debug("start clean before each");
    // remove the activate beans, so this can be reactivated
    for (ObjectInstance mbean : serviceRegistryJpaImpl.jmxBeans) {
      JmxUtil.unregisterMXBean(mbean);
    }
    // reset the scheduledExecutor
    if (serviceRegistryJpaImpl.scheduledExecutor != null)
      serviceRegistryJpaImpl.scheduledExecutor.shutdown();
    if (!serviceRegistryJpaImpl.getActiveJobs().isEmpty()) {
      List<Long> jobIds = new ArrayList();
      for (Job job : serviceRegistryJpaImpl.getActiveJobs()) {
        jobIds.add(job.getId());
      }
      logger.trace("about to remove {} jobs", jobIds.size());
      try {
        serviceRegistryJpaImpl.removeJobs(jobIds);
      } catch (Exception e) {
        logger.debug("Ignoring exception {}", e.getMessage());
      }
    }
    serviceRegistryJpaImpl.activate(null);
    unregisterTestHostAndServices();
    registerTestHostAndService();

    // Stop current scheduled executors so dispatch can be launched in a controlled way in each test
    if (serviceRegistryJpaImpl.scheduledExecutor != null) {
      serviceRegistryJpaImpl.scheduledExecutor.shutdown();
    }
    logger.debug("end clean before each");
  }

  @AfterClass
  public static void tearDownAfterAll() throws ServiceRegistryException {
    for (ObjectInstance mbean : serviceRegistryJpaImpl.jmxBeans) {
      JmxUtil.unregisterMXBean(mbean);
    }
    logger.debug("About to deactivate after all tests");
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

  public static void setUpEntityManagerFactory() {
    emf = PersistenceUtil.newTestEntityManagerFactory("org.opencastproject.common");
  }

  public static void setUpServiceRegistryJpaImpl()
          throws PropertyVetoException, NotFoundException, TrustedHttpClientException {
    serviceRegistryJpaImpl = new ServiceRegistryJpaImpl();
    serviceRegistryJpaImpl.setEntityManagerFactory(emf);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject())).andReturn(organization)
            .anyTimes();

    EasyMock.replay(organizationDirectoryService);

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

    // Setup the job dispatcher
    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    Dictionary<String, Object> jdProps = new Hashtable<>();
    jdProps.put(JobDispatcher.OPT_DISPATCHINTERVAL, "0");
    EasyMock.expect(cc.getProperties()).andReturn(jdProps).anyTimes();
    EasyMock.replay(cc);
    jobDispatcher = new JobDispatcher();
    jobDispatcher.setEntityManagerFactory(emf);
    jobDispatcher.setServiceRegistry(serviceRegistryJpaImpl);
    jobDispatcher.setSecurityService(securityService);
    jobDispatcher.setOrganizationDirectoryService(organizationDirectoryService);
    jobDispatcher.setUserDirectoryService(userDirectoryService);
    jobDispatcher.setTrustedHttpClient(trustedHttpClient);
    try {
      jobDispatcher.activate(cc);
    } catch (ConfigurationException e) {
      Assert.fail("Job dispatcher activation failed" + e);
    }
  }

  private void unregisterTestHostAndServices() {
    try {
      serviceRegistryJpaImpl.unregisterHost(TEST_HOST);
      serviceRegistryJpaImpl.unregisterHost(TEST_HOST_OTHER);
      serviceRegistryJpaImpl.unregisterHost(TEST_HOST_THIRD);
      serviceRegistryJpaImpl.unRegisterService(TEST_SERVICE, TEST_HOST);
      serviceRegistryJpaImpl.unRegisterService(TEST_SERVICE, TEST_HOST_OTHER);
      serviceRegistryJpaImpl.unRegisterService(TEST_SERVICE_2, TEST_HOST);
      serviceRegistryJpaImpl.unRegisterService(TEST_SERVICE_FAIRNESS, TEST_HOST);
      serviceRegistryJpaImpl.unRegisterService(TEST_SERVICE_FAIRNESS, TEST_HOST_OTHER);
      serviceRegistryJpaImpl.unRegisterService(TEST_SERVICE_FAIRNESS, TEST_HOST_THIRD);
    } catch (Exception e) {
      logger.info("Ignoring exception {}", e.getMessage());
    }
  }

  private void registerTestHostAndService() throws ServiceRegistryException {
    // register the hosts, service must be activated at this point
    serviceRegistryJpaImpl.registerHost(TEST_HOST, "127.0.0.1", "test", 1024, 1, 1);
    serviceRegistryJpaImpl.registerHost(TEST_HOST_OTHER, "127.0.0.1", "test_other", 1024, 1, 2);
    serviceRegistryJpaImpl.registerHost(TEST_HOST_THIRD, "127.0.0.1", "test_third", 1024, 1, 4);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE, TEST_HOST, TEST_PATH);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE, TEST_HOST_OTHER, TEST_PATH);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE_2, TEST_HOST, TEST_PATH_2);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE_FAIRNESS, TEST_HOST, TEST_PATH_2);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE_FAIRNESS, TEST_HOST_OTHER, TEST_PATH_2);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE_FAIRNESS, TEST_HOST_THIRD, TEST_PATH_2);
  }

  private static void setupBundleContext() throws InvalidSyntaxException {
    bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(OpencastConstants.SERVER_URL_PROPERTY)).andReturn("");
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.jobs.url")).andReturn("");
    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_MAXLOAD)).andReturn("");
    EasyMock.expect(bundleContext.createFilter((String) EasyMock.anyObject()))
            .andReturn(EasyMock.createNiceMock(Filter.class));
  }

  private static void setupComponentContext() {
    cc = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(cc);
  }

  // Avoid a junit race condition with the dispatch loop by only running dispatch in a
  // controlled way during the test
  private void launchDispatcherOnce(boolean withProducerHeartBeat) {
    // Stop current scheduled executors so dispatch can be launched in a controlled way in each test
    if (serviceRegistryJpaImpl.scheduledExecutor != null) {
      serviceRegistryJpaImpl.scheduledExecutor.shutdown();
    }
    serviceRegistryJpaImpl.scheduledExecutor = Executors.newScheduledThreadPool(1);
    jobDispatcher.scheduledExecutor.schedule(jobDispatcher.getJobDispatcherRunnable(), DISPATCH_START_DELAY, TimeUnit.MILLISECONDS);

    if (withProducerHeartBeat) {
      JobProducerHeartbeat jph = serviceRegistryJpaImpl.new JobProducerHeartbeat();
      serviceRegistryJpaImpl.scheduledExecutor.schedule(jph, DISPATCH_START_DELAY, TimeUnit.MILLISECONDS);
    }
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteJobInvalidJobId() throws Exception {
    serviceRegistryJpaImpl.removeJobs(Collections.singletonList(1L));
  }

  @Test
  public void testCancelUndispatchablesOrphanedByActivatingNode() throws Exception {
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
    assertEquals(Status.CANCELLED, undispatchableJob1.getStatus());
    logger.info("Undispatachable job 1 " + undispatchableJob2.getId());
    undispatchableJob2 = serviceRegistryJpaImpl.getJob(undispatchableJob2.getId());
    assertEquals(Status.RUNNING, undispatchableJob2.getStatus());
  }

  @Test
  public void testHostAddedToPriorityList() throws Exception {
    Job testJob = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_OPERATION, null, null, true, null);
    JobBarrier barrier = new JobBarrier(null, serviceRegistryJpaImpl, testJob);
    launchDispatcherOnce(false);
    try {
      barrier.waitForJobs(JOB_BARRIER_TIMEOUT);
      Assert.fail("Did not receive a timeout exception");
    } catch (Exception e) {
      Assert.assertEquals(1, jobDispatcher.dispatchPriorityList.size());
    }
  }

  @Test
  public void testHostAddedToPriorityListExceptWorkflowType() throws Exception {
    serviceRegistryJpaImpl.registerService(TEST_SERVICE_3, TEST_HOST, TEST_PATH_3);
    Job testJob = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_3, TEST_OPERATION, null, null, true, null);
    JobBarrier barrier = new JobBarrier(null, serviceRegistryJpaImpl, testJob);
    launchDispatcherOnce(false);
    try {
      barrier.waitForJobs(JOB_BARRIER_TIMEOUT);
      Assert.fail("Did not receive a timeout exception");
    } catch (Exception e) {
      Assert.assertEquals(0, jobDispatcher.dispatchPriorityList.size());
    } finally {
      // extra clean up
      serviceRegistryJpaImpl.unRegisterService(TEST_SERVICE_3, TEST_HOST);
    }
  }

  @Test
  public void testHostsBeingRemovedFromPriorityList() throws Exception {
    jobDispatcher.dispatchPriorityList.put(0L, TEST_HOST);
    Job testJob = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_2, TEST_OPERATION, null, null, true, null);
    JobBarrier barrier = new JobBarrier(null, serviceRegistryJpaImpl, testJob);
    launchDispatcherOnce(false);
    try {
      barrier.waitForJobs(JOB_BARRIER_TIMEOUT);
      Assert.fail("Did not receive a timeout exception");
    } catch (Exception e) {
      Assert.assertEquals(0, jobDispatcher.dispatchPriorityList.size());
    } finally {
      logger.debug("end testHostsBeingRemovedFromPriorityList");
    }
  }

  @Test
  public void testIgnoreHostsInPriorityList() throws Exception {
    Job testJob = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_2, TEST_OPERATION, null, null, true, null);
    Job testJob2 = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_OPERATION, null, null, true, null);
    jobDispatcher.dispatchPriorityList.put(testJob2.getId(), TEST_HOST);
    JobBarrier barrier = new JobBarrier(null, serviceRegistryJpaImpl, testJob, testJob2);
    launchDispatcherOnce(false);
    try {
      barrier.waitForJobs(JOB_BARRIER_TIMEOUT);
      Assert.fail("Did not receive a timeout exception");
    } catch (Exception e) {
      logger.debug("job1: '{}'", serviceRegistryJpaImpl.getJob(testJob.getId()));
      logger.debug("job2: '{}'", serviceRegistryJpaImpl.getJob(testJob2.getId()));
      for (Long jobId :jobDispatcher.dispatchPriorityList.keySet()) {
        logger.debug("job in priority queue: {}, {}", jobId, jobDispatcher.dispatchPriorityList.get(jobId));
      }
      // Mock http client always returns 503 for this path so it won't be dispatched anyway
      testJob = serviceRegistryJpaImpl.getJob(testJob.getId());
      Assert.assertTrue("First job should not have a processing host", StringUtils.isBlank(testJob.getProcessingHost()));
      Assert.assertEquals("First job is queued", Job.Status.QUEUED, testJob.getStatus());

      // Mock http client always returns 204 for this path, but it should not be dispatched
      // because the host is in the dispatchPriorityList
      testJob2 = serviceRegistryJpaImpl.getJob(testJob2.getId());
      Assert.assertTrue("Second job should not have a processing host", StringUtils.isBlank(testJob2.getProcessingHost()));
      Assert.assertEquals("Second job is queued", Job.Status.QUEUED, testJob2.getStatus());

      Assert.assertEquals(1, jobDispatcher.dispatchPriorityList.size());
      String blockingHost = jobDispatcher.dispatchPriorityList.get(testJob2.getId());
      Assert.assertEquals(TEST_HOST, blockingHost);
    } finally {
      logger.debug("end testIgnoreHostsInPriorityList");
    }
  }

  private void assertHostloads(Job j, Float a, Float b, Float c) throws Exception {
    // launch the  dispatcher and wait a little longer for dispatch to complete before getting job
    launchDispatcherOnce(false);
    Thread.sleep(JOB_BARRIER_TIMEOUT);
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
  }


  @Test
  public void testDispatchingJobsHigherMaxLoad() throws Exception {
    logger.debug("KHD start of testDispatchingJobsHigherMaxLoad");
    Job testJob = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_FAIRNESS, TEST_OPERATION, null, null, true, null,
            10.0f);
    JobBarrier barrier = new JobBarrier(null, serviceRegistryJpaImpl, testJob);
    launchDispatcherOnce(false);
    assertThrows(IllegalStateException.class, () -> barrier.waitForJobs(JOB_BARRIER_TIMEOUT));
  }

  @Test
  public void testUpdateJobFailed() throws Exception {
    Job job = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, null, null, true, null, 1.0f);
    job.setStatus(Job.Status.FAILED);
    Job updatedJob = serviceRegistryJpaImpl.updateJob(job);
    Assert.assertNotNull(updatedJob.getDateCompleted());
  }

  @Test
  public void testUpdateJobFinished() throws Exception {
    Job job = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, null, null, true, null, 1.0f);
    job.setStatus(Job.Status.FINISHED);
    Job updatedJob = serviceRegistryJpaImpl.updateJob(job);
    Assert.assertNotNull(updatedJob.getDateCompleted());
    Assert.assertNotNull(updatedJob.getRunTime());
  }

  @Test
  public void testErrorState() throws Exception {
    // set max attempts to 1
    Dictionary<String, String> properties = new Hashtable<>();
    properties.put(ServiceRegistryJpaImpl.MAX_ATTEMPTS_CONFIG_KEY, "1");
    serviceRegistryJpaImpl.updated(properties);

    serviceRegistryJpaImpl.sanitize(TEST_SERVICE, TEST_HOST);

    // first job pushes service into warning state
    Job job = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, new ArrayList(
            Arrays.asList("a", "b")), null, true, null, 1.0f);
    job.setStatus(Job.Status.FAILED);
    job.setProcessingHost(TEST_HOST);
    serviceRegistryJpaImpl.updateJob(job);
    ServiceRegistration service = serviceRegistryJpaImpl.getServiceRegistration(TEST_SERVICE, TEST_HOST);
    Assert.assertEquals(ServiceState.WARNING, service.getServiceState());

    // second job takes service to error state
    Job job2 = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, new ArrayList(
            Arrays.asList("a", "b", "c")), null, true, null, 1.0f);
    job2.setStatus(Job.Status.FAILED);
    job2.setProcessingHost(TEST_HOST);
    serviceRegistryJpaImpl.updateJob(job2);
    service = serviceRegistryJpaImpl.getServiceRegistration(TEST_SERVICE, TEST_HOST);
    Assert.assertEquals(ServiceState.ERROR, service.getServiceState());
  }

  @Test
  public void testDisablingErrorState() throws Exception {
    // disable error states
    Dictionary<String, String> properties = new Hashtable<>();
    properties.put(ServiceRegistryJpaImpl.MAX_ATTEMPTS_CONFIG_KEY, "-1");
    serviceRegistryJpaImpl.updated(properties);

    serviceRegistryJpaImpl.sanitize(TEST_SERVICE, TEST_HOST);

    // warning state still works
    Job job = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, new ArrayList(
            Arrays.asList("a", "b")), null, true, null, 1.0f);
    job.setStatus(Job.Status.FAILED);
    job.setProcessingHost(TEST_HOST);
    serviceRegistryJpaImpl.updateJob(job);
    ServiceRegistration service = serviceRegistryJpaImpl.getServiceRegistration(TEST_SERVICE, TEST_HOST);
    Assert.assertEquals(ServiceState.WARNING, service.getServiceState());

    // error state isn't entered
    Job job2 = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, new ArrayList(
            Arrays.asList("a", "b", "c")), null, true, null, 1.0f);
    job2.setStatus(Job.Status.FAILED);
    job2.setProcessingHost(TEST_HOST);
    serviceRegistryJpaImpl.updateJob(job2);
    service = serviceRegistryJpaImpl.getServiceRegistration(TEST_SERVICE, TEST_HOST);
    Assert.assertEquals(ServiceState.WARNING, service.getServiceState());
  }

  @Test
  public void testDisablingErrorStateForService() throws Exception {
    // disable error states for one service
    Dictionary<String, String> properties = new Hashtable<>();
    properties.put(ServiceRegistryJpaImpl.MAX_ATTEMPTS_CONFIG_KEY, "1");
    properties.put(ServiceRegistryJpaImpl.NO_ERROR_STATE_SERVICE_TYPES_CONFIG_KEY, TEST_SERVICE_2 + ", " + TEST_SERVICE_3);
    serviceRegistryJpaImpl.updated(properties);

    serviceRegistryJpaImpl.sanitize(TEST_SERVICE, TEST_HOST);
    serviceRegistryJpaImpl.sanitize(TEST_SERVICE_2, TEST_HOST);

    // error states still work for other services
    Job job = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, new ArrayList(
            Arrays.asList("a", "b")), null, true, null, 1.0f);
    job.setStatus(Job.Status.FAILED);
    job.setProcessingHost(TEST_HOST);
    serviceRegistryJpaImpl.updateJob(job);
    ServiceRegistration service = serviceRegistryJpaImpl.getServiceRegistration(TEST_SERVICE, TEST_HOST);
    Assert.assertEquals(ServiceState.WARNING, service.getServiceState());

    Job job2 = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_PATH, new ArrayList(
            Arrays.asList("a", "b", "c")), null, true, null, 1.0f);
    job2.setStatus(Job.Status.FAILED);
    job2.setProcessingHost(TEST_HOST);
    serviceRegistryJpaImpl.updateJob(job2);
    service = serviceRegistryJpaImpl.getServiceRegistration(TEST_SERVICE, TEST_HOST);
    Assert.assertEquals(ServiceState.ERROR, service.getServiceState());

    // but not for the configured service
    Job job3 = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_2, TEST_PATH, new ArrayList(
            Arrays.asList("a", "b")), null, true, null, 1.0f);
    job3.setStatus(Job.Status.FAILED);
    job3.setProcessingHost(TEST_HOST);
    serviceRegistryJpaImpl.updateJob(job3);
    ServiceRegistration service2 = serviceRegistryJpaImpl.getServiceRegistration(TEST_SERVICE_2, TEST_HOST);
    Assert.assertEquals(ServiceState.WARNING, service2.getServiceState());

    Job job4 = serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE_2, TEST_PATH, new ArrayList(
            Arrays.asList("a", "b", "c")), null, true, null, 1.0f);
    job4.setStatus(Job.Status.FAILED);
    job4.setProcessingHost(TEST_HOST);
    serviceRegistryJpaImpl.updateJob(job4);
    service2 = serviceRegistryJpaImpl.getServiceRegistration(TEST_SERVICE_2, TEST_HOST);
    Assert.assertEquals(ServiceState.WARNING, service2.getServiceState());
  }

  @Test
  public void testCompletedRuntimeDoNotChange() throws Exception {
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
