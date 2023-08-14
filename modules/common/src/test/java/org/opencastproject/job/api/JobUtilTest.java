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
package org.opencastproject.job.api;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.JobUtil.getNonFinished;
import static org.opencastproject.util.JobUtil.getPayload;
import static org.opencastproject.util.JobUtil.isReadyToDispatch;
import static org.opencastproject.util.JobUtil.payloadAsMediaPackageElement;
import static org.opencastproject.util.JobUtil.sumQueueTime;
import static org.opencastproject.util.JobUtil.update;
import static org.opencastproject.util.JobUtil.waitForJob;
import static org.opencastproject.util.JobUtil.waitForJobSuccess;
import static org.opencastproject.util.data.Collections.list;

import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier.Result;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.JobUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.Opt;

import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class JobUtilTest {

  private ServiceRegistry serviceRegistry;

  @Before
  public void setUp() throws Exception {
    serviceRegistry = createNiceMock(ServiceRegistry.class);
    JobImpl job = new JobImpl(20);
    job.setPayload("a payload");
    Job finishedJob1 = new JobImpl(1);
    finishedJob1.setStatus(Status.FINISHED);
    Job finishedJob2 = new JobImpl(2);
    finishedJob2.setStatus(Status.FINISHED);
    Job finishedJob3 = new JobImpl(3);
    finishedJob3.setStatus(Status.FINISHED);
    expect(serviceRegistry.getJob(1)).andReturn(finishedJob1).anyTimes();
    expect(serviceRegistry.getJob(2)).andReturn(finishedJob2).anyTimes();
    expect(serviceRegistry.getJob(3)).andReturn(finishedJob3).anyTimes();
    expect(serviceRegistry.getJob(23)).andThrow(new NotFoundException()).anyTimes();
    expect(serviceRegistry.getJob(20)).andReturn(job).anyTimes();
    replay(serviceRegistry);
  }

  @Test
  public void testWaitForJob() {
    Job job1 = new JobImpl(1);
    job1.setStatus(Status.FINISHED);
    Job job2 = new JobImpl(1);
    job2.setStatus(Status.RUNNING);
    Job job3 = new JobImpl(1);
    job3.setStatus(Status.FINISHED);

    Result result = waitForJob(serviceRegistry, job1);
    assertTrue(result.isSuccess());

    result = waitForJob(job2, serviceRegistry, job3);
    assertTrue(result.isSuccess());

    result = waitForJob(serviceRegistry, Option.some(2000L), job1);
    assertTrue(result.isSuccess());

    result = waitForJob(serviceRegistry, Option.<Long> none(), job1);
    assertTrue(result.isSuccess());

    result = waitForJob(job2, serviceRegistry, Option.some(2000L), job3);
    assertTrue(result.isSuccess());

    result = waitForJob(job2, serviceRegistry, Option.some(2000L), job3);
    assertTrue(result.isSuccess());
  }

  @Test
  public void testWaitForJobs() {
    Job job1 = new JobImpl(1);
    job1.setStatus(Status.FINISHED);
    Job job2 = new JobImpl(2);
    job2.setStatus(Status.RUNNING);
    Job job3 = new JobImpl(3);
    job3.setStatus(Status.FINISHED);

    Result result = JobUtil.waitForJobs(job2, serviceRegistry, 0L, 0L, new Job[] { job1, job3 });
    assertTrue(result.isSuccess());
  }

  @Test
  public void testGetPayload() throws NotFoundException, ServiceRegistryException {
    Opt<String> payload = getPayload(serviceRegistry, new JobImpl(23));
    assertTrue(payload.isNone());

    payload = getPayload(serviceRegistry, new JobImpl(20));
    assertTrue(payload.isSome());
  }

  @Test
  public void testUpdate() throws NotFoundException, ServiceRegistryException {
    Opt<Job> update = update(serviceRegistry, new JobImpl(23));
    assertTrue(update.isNone());

    update = update(serviceRegistry, new JobImpl(20));
    assertTrue(update.isSome());
  }

  @Test
  public void testIsReadyToDispatch() {
    JobImpl job = new JobImpl(20);
    job.setStatus(Status.RUNNING);
    boolean readyToDispatch = isReadyToDispatch(job);
    assertTrue(readyToDispatch);

    job.setStatus(Status.FAILED);
    readyToDispatch = isReadyToDispatch(job);
    assertFalse(readyToDispatch);
  }

  @Test
  public void testWaitForJobSuccess() {
    JobImpl job = new JobImpl(20);
    job.setStatus(Status.FAILED);

    Function<Job, Boolean> waitForJobSuccess = waitForJobSuccess(job, serviceRegistry, Option.<Long> none());
    Boolean isSuccess = waitForJobSuccess.apply(job);
    assertFalse(isSuccess);

    job.setStatus(Status.FINISHED);
    isSuccess = waitForJobSuccess.apply(job);
    assertTrue(isSuccess);
  }

  @Test
  public void testPayloadAsMediaPackageElement() throws Exception {
    MediaPackageElement element = new MediaPackageElementBuilderImpl().newElement(Type.Track,
            MediaPackageElements.PRESENTATION_SOURCE);

    JobImpl job = new JobImpl(20);
    job.setStatus(Status.FINISHED);
    job.setPayload(MediaPackageElementParser.getAsXml(element));

    Function<Job, MediaPackageElement> payloadAsMediaPackageElement = payloadAsMediaPackageElement(job,
            serviceRegistry);
    assertEquals(element, payloadAsMediaPackageElement.apply(job));
  }

  @Test
  public void testJobFromHttpResponse() throws Exception {
    BasicHttpResponse response = new BasicHttpResponse(
            new BasicStatusLine(new HttpVersion(1, 1), HttpStatus.SC_NO_CONTENT, "No message"));
    Option<Job> job = JobUtil.jobFromHttpResponse.apply(response);
    assertFalse(job.isSome());

    JaxbJob jaxbJob = new JaxbJob(new JobImpl(32));
    response.setEntity(new StringEntity(JobParser.toXml(jaxbJob), StandardCharsets.UTF_8));

    job = JobUtil.jobFromHttpResponse.apply(response);
    assertTrue(job.isSome());
    assertEquals(jaxbJob.toJob(), job.get());
  }

  @Test
  public void testSumQueueTime() {
    long queueTime1 = 314;
    long queueTime2 = 14342L;
    long queueTime3 = 1521L;

    Job job1 = new JobImpl(1);
    job1.setQueueTime(queueTime1);
    Job job2 = new JobImpl(1);
    job2.setQueueTime(queueTime2);
    Job job3 = new JobImpl(1);
    job3.setQueueTime(queueTime3);

    long sumQueueTime = sumQueueTime(list(job1, job2, job3));
    assertEquals(queueTime1 + queueTime2 + queueTime3, sumQueueTime);
  }

  @Test
  public void testGetNonFinished() {
    Job job1 = new JobImpl(1);
    job1.setStatus(Status.FINISHED);
    Job job2 = new JobImpl(1);
    job2.setStatus(Status.RUNNING);
    Job job3 = new JobImpl(1);
    job3.setStatus(Status.WAITING);

    List<Job> nonFinished = getNonFinished(list(job1, job2, job3));
    assertEquals(2, nonFinished.size());
    assertFalse(nonFinished.contains(job1));
  }

}
