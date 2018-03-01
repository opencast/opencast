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
package org.opencastproject.assetmanager.impl;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import junitparams.JUnitParamsRunner;

@RunWith(JUnitParamsRunner.class)
public class TieredStorageAssetManagerJobProducerTest extends AbstractTieredStorageAssetManagerTest<AbstractAssetManagerWithTieredStorage> {

  private TieredStorageAssetManagerJobProducer tsamjp = null;
  private ServiceRegistry sr = null;

  @Before
  public void before() throws Exception {
    setUp(mkTieredStorageAM());
    am.addRemoteAssetStore(remoteAssetStore1);

    tsamjp = new TieredStorageAssetManagerJobProducer();
    tsamjp.setOrganizationDirectoryService(null);
    tsamjp.setUserDirectoryService(null);
    tsamjp.setSecurityService(null);

    sr = EasyMock.createMock(ServiceRegistry.class);

    tsamjp.setAssetManager(am);
    tsamjp.setServiceRegistry(sr);
  }

  @Test
  public void testById() throws ServiceRegistryException {
    String[] mp = createAndAddMediaPackagesSimple(1, 2, 2, Opt.<String>none());

    createIdExpectation(mp[0]);
    EasyMock.replay(sr);

    tsamjp.moveById(mp[0], REMOTE_STORE_1_ID);

    EasyMock.verify(sr);
  }

  @Test
  public void testInternalById() throws ServiceRegistryException {
    String[] mp = createAndAddMediaPackagesSimple(2, 2, 2, Opt.<String>none());
    //Because this is the internal check, this is an id-and-version check since it's the terminal phase (ie, post process())
    createIdAndVersionExpectation(mp[1], 0, 2);
    EasyMock.replay(sr);

    Assert.assertEquals("Both versions should move",
            "2", tsamjp.internalMoveById(mp[1], REMOTE_STORE_1_ID));
    EasyMock.verify(sr);
  }

  @Test
  public void testProcessById() throws ServiceRegistryException {
    String[] mp = createAndAddMediaPackagesSimple(2, 2, 2, Opt.<String>none());
    //This is checking the logic of process(), so we need to have terminal phase jobs
    List<Job> jobs = createIdAndVersionExpectation(mp[1], 0, 2);
    EasyMock.replay(sr);

    for (Job j : jobs) {
      Assert.assertEquals("OK", tsamjp.process(j));
    }
  }

  @Test
  public void testByIdAndVersion() throws ServiceRegistryException {
    String[] mp = createAndAddMediaPackagesSimple(1, 2, 2, Opt.<String>none());
    //This is a terminal query, so we need terminal expectations
    createIdAndVersionExpectation(mp[0], 1, 2);
    EasyMock.replay(sr);

    tsamjp.moveByIdAndVersion(VersionImpl.mk(1L), mp[0], REMOTE_STORE_1_ID);

    EasyMock.verify(sr);
  }

  @Test
  public void testInternalByIdAndVersion() throws NotFoundException, ServiceRegistryException {
    String[] mp = createAndAddMediaPackagesSimple(2, 2, 2, Opt.<String>none());
    //This is a terminal query, so we need terminal expectations
    createIdAndVersionExpectation(mp[1], 1, 1);
    EasyMock.replay(sr);

    Assert.assertEquals("Only one version should move",
            "OK", tsamjp.internalMoveByIdAndVersion(new VersionImpl(1L), mp[1], REMOTE_STORE_1_ID));
    EasyMock.verify(sr);
  }

  @Test
  public void testProcessByIdAndVersion() throws NotFoundException, ServiceRegistryException {
    String[] mp = createAndAddMediaPackagesSimple(2, 2, 2, Opt.<String>none());
    //This is a terminal query, so we need terminal expectations;
    List<Job> jobs = createIdAndVersionExpectation(mp[1], 1, 1);
    EasyMock.replay(sr);

    for (Job j : jobs) {
      Assert.assertEquals("OK", tsamjp.process(j));
    }
  }

  @Test(expected = NotFoundException.class)
  public void testNotFoundInternalByIdAndVersion() throws NotFoundException {
    EasyMock.replay(sr);
    Assert.assertEquals("Only one version should move",
            "1", tsamjp.internalMoveByIdAndVersion(new VersionImpl(5L), "fake", REMOTE_STORE_1_ID));
  }

  @Test
  public void testByDate() throws ServiceRegistryException {
    String[] mp = createAndAddMediaPackagesSimple(1, 2, 2, Opt.<String>none());
    Date start = new Date(-10000L);
    Date end = new Date(new Date().getTime() + 10000L);
    //Non terminal query, non terminal expectations
    createDateExpectation(start, end);
    EasyMock.replay(sr);

    tsamjp.moveByDate(start, end, REMOTE_STORE_1_ID);

    EasyMock.verify(sr);
  }

  @Test
  public void testInternalByDate() throws NotFoundException, ServiceRegistryException {
    Date start = new Date();
    Date before = new Date();
    String[] mp = createAndAddMediaPackagesSimple(2, 2, 2, Opt.<String>none());
    Date after = new Date();
    //Non terminal query, but internal test so we create terminal expectations
    createIdAndVersionExpectation(mp[0], 0, 2);
    createIdAndVersionExpectation(mp[1], 0, 2);
    EasyMock.replay(sr);

    Assert.assertEquals("No versions exist between the start and before test values",
            "0", tsamjp.internalMoveByDate(start, before, REMOTE_STORE_1_ID));
    Assert.assertEquals("All four versions should move",
            "4", tsamjp.internalMoveByDate(before, after, REMOTE_STORE_1_ID));
    Assert.assertEquals("No versions exist after the end date",
            "0", tsamjp.internalMoveByDate(after, new Date(), REMOTE_STORE_1_ID));
    EasyMock.verify(sr);
  }

  @Test
  public void testProcessByDate() throws NotFoundException, ServiceRegistryException {
    Date start = new Date();
    String[] mps = createAndAddMediaPackagesSimple(2, 2, 2, Opt.<String>none());
    Date after = new Date();
    //Non terminal query, but internal test so we create terminal expectations
    List<Job> jobs = createDateTriggerJob(mps, 2);
    EasyMock.replay(sr);

    for (Job j : jobs) {
      Assert.assertEquals("OK", tsamjp.process(j));
    }
    EasyMock.verify(sr);
  }

  @Test
  public void testByDateAndId() throws ServiceRegistryException {
    String[] mp = createAndAddMediaPackagesSimple(1, 2, 2, Opt.<String>none());
    Date start = new Date(-10000L);
    Date end = new Date(new Date().getTime() + 10000L);
    //Non terminal query, non terminal expectations
    createIdAndDateExpectation(mp[0], start, end);
    EasyMock.replay(sr);

    tsamjp.moveByIdAndDate(mp[0], start, end, REMOTE_STORE_1_ID);

    EasyMock.verify(sr);
  }

  @Test
  public void testInternalByIdAndDate() throws NotFoundException, ServiceRegistryException {
    Date start = new Date();
    Date before = new Date();
    String[] mp = createAndAddMediaPackagesSimple(2, 2, 2, Opt.<String>none());
    Date after = new Date();
    //Non terminal query, but internal test so we create terminal expectations
    createIdAndVersionExpectation(mp[1], 0, 2);
    EasyMock.replay(sr);

    Assert.assertEquals("No versions exist between the start and before test values",
            "0", tsamjp.internalMoveByIdAndDate("fake", start, before, REMOTE_STORE_1_ID));
    Assert.assertEquals("Both versions of " + mp[1] + " should move",
            "2", tsamjp.internalMoveByIdAndDate(mp[1], before, after, REMOTE_STORE_1_ID));
    Assert.assertEquals("No versions of " + mp[1] + " should move",
            "0", tsamjp.internalMoveByIdAndDate(mp[1], after, new Date(), REMOTE_STORE_1_ID));
    EasyMock.verify(sr);
  }

  @Test
  public void testProcessByIdAndDate() throws NotFoundException, ServiceRegistryException {
    String[] mp = createAndAddMediaPackagesSimple(2, 2, 2, Opt.<String>none());
    //Non terminal query, but internal test so we create terminal expectations
    List<Job> jobs = createDateTriggerJob(mp, 2);
    EasyMock.replay(sr);

    for (Job j : jobs) {
      Assert.assertEquals("OK", tsamjp.process(j));
    }
    EasyMock.verify(sr);
  }

  private Job createIdExpectation(String mpId) throws ServiceRegistryException {
    List<String> args = new LinkedList<String>();
    args.add(REMOTE_STORE_1_ID);
    args.add(mpId);
    createExpectation(TieredStorageAssetManagerJobProducer.Operation.MoveById.toString(), args, TieredStorageAssetManagerJobProducer.NONTERMINAL_JOB_LOAD);
    return createTriggerJob(args, TieredStorageAssetManagerJobProducer.NONTERMINAL_JOB_LOAD);
  }

  private List<Job> createIdAndVersionExpectation(String mpId, int start, int end) throws ServiceRegistryException {
    List<Job> jobs = new LinkedList<Job>();
    for (int i = start; i < end; i++) {
      List<String> args = new LinkedList<String>();
      args.add(REMOTE_STORE_1_ID);
      args.add(mpId);
      args.add(VersionImpl.mk(i).toString());
      jobs.add(createTriggerJob(args, TieredStorageAssetManagerJobProducer.JOB_LOAD));
      createExpectation(TieredStorageAssetManagerJobProducer.Operation.MoveByIdAndVersion.toString(), args, TieredStorageAssetManagerJobProducer.JOB_LOAD);
    }
    return jobs;
  }

  private void createDateExpectation(Date start, Date end) throws ServiceRegistryException {
    List<String> args = new LinkedList<String>();
    args.add(REMOTE_STORE_1_ID);
    args.add(Long.toString(start.getTime()));
    args.add(Long.toString(end.getTime()));
    createExpectation(TieredStorageAssetManagerJobProducer.Operation.MoveByDate.toString(), args, TieredStorageAssetManagerJobProducer.NONTERMINAL_JOB_LOAD);
  }

  private void createIdAndDateExpectation(String mpId, Date start, Date end) throws ServiceRegistryException {
    List<String> args = new LinkedList<String>();
    args.add(REMOTE_STORE_1_ID);
    args.add(mpId);
    args.add(Long.toString(start.getTime()));
    args.add(Long.toString(end.getTime()));
    createExpectation(TieredStorageAssetManagerJobProducer.Operation.MoveByIdAndDate.toString(), args, TieredStorageAssetManagerJobProducer.NONTERMINAL_JOB_LOAD);
  }

  /**
   * Creates an EasyMock expectation of a call to ServiceRegistry.createJob
   * @param operation
   *   The operation to expect
   * @param args
   *   The arguments for the job
   * @param jobload
   *   The job's load
   * @throws ServiceRegistryException
   */
  private void createExpectation(String operation, List<String> args, float jobload) throws ServiceRegistryException {
    EasyMock.expect(
            sr.createJob(
                    EasyMock.eq(TieredStorageAssetManagerJobProducer.JOB_TYPE),
                    EasyMock.eq(operation),
                    EasyMock.eq(args),
                    EasyMock.isNull(String.class),
                    EasyMock.eq(true),
                    EasyMock.eq(jobload)
            )
        ).andReturn(new JobImpl()).once();
  }

  /**
   * Creates a {@link Job} which can be used to test the process method in {@link TieredStorageAssetManagerJobProducer}
   * @param args
   *   The job's args value
   * @param jobload
   *   The job's load value
   * @return
   *   The job, appropriately configured
   */
  private Job createTriggerJob(List<String> args, float jobload) {
    Job triggerJob = new JobImpl();
    triggerJob.setJobType(TieredStorageAssetManagerJobProducer.JOB_TYPE);
    //Note, all of the methods which use these returned jobs are expecting terminal jobs!
    triggerJob.setOperation(TieredStorageAssetManagerJobProducer.Operation.MoveByIdAndVersion.toString());
    triggerJob.setArguments(args);
    triggerJob.setPayload(null);
    triggerJob.setDispatchable(true);
    triggerJob.setJobLoad(jobload);
    return triggerJob;
  }

  /**
   * Creates a {@link Job} which can be used to test the process method in {@link TieredStorageAssetManagerJobProducer}
   * @param mps
   *   The mediapackage ids
   * @param numVersions
   *   The number of jobs to create (equal to the number of versions present)
   * @return
   *   The job, appropriately configured
   */
  private List<Job> createDateTriggerJob(String[] mps, int numVersions) {
    List<Job> jobs = new LinkedList<Job>();
    for (String mp : mps) {
      for (int i = 0; i < numVersions; i++) {
        List args = new LinkedList<String>();
        args.add(REMOTE_STORE_1_ID);
        args.add(mp);
        args.add(VersionImpl.mk(i).toString());
        jobs.add(createTriggerJob(args, TieredStorageAssetManagerJobProducer.JOB_LOAD));
      }
    }
    return jobs;
  }
}
