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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.job.api.Job.FailureReason.NONE;
import static org.opencastproject.job.api.Job.Status.DISPATCHING;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class JobImplTest {

  private final List<String> arguments = Arrays.asList("arg-1", "arg-2");
  private final Date created = new Date(1455008400000L); // Tue Feb 09 2016 09:00:00
  private final Date started = new Date(1455008700000L); // Tue Feb 09 2016 09:05:00
  private final Date completed = new Date(1455009315000L); // Tue Feb 09 2016 09:15:15
  private final URI uri;

  private final List<Long> blockedJobs = Arrays.asList(5L, 6L);

  private JobImpl job;

  public JobImplTest() throws Exception {
    uri =  new URI("http://test");
  }

  @Before
  public void setUp() throws Exception {
    job = new JobImpl(3L, "test", "test_org", 0L, "simple", "do", arguments, DISPATCHING, "localhost", "remotehost",
            created, started, completed, 100L, 200L, "result", 3L, 1L, true, uri, 1.5F);
  }

  @Test
  public void testGetId() throws Exception {
    assertEquals(3L, job.getId());
  }

  @Test
  public void testGetCreator() throws Exception {
    assertEquals("test", job.getCreator());
  }

  @Test
  public void testGetOrganization() throws Exception {
    assertEquals("test_org", job.getOrganization());
  }

  @Test
  public void testGetVersion() throws Exception {
    assertEquals(0L, job.getVersion());
  }

  @Test
  public void testGetJobType() throws Exception {
    assertEquals("simple", job.getJobType());
  }

  @Test
  public void testGetOperation() throws Exception {
    assertEquals("do", job.getOperation());
  }

  @Test
  public void testGetArguments() throws Exception {
    assertEquals(arguments, job.getArguments());
  }

  @Test
  public void testGetStatus() throws Exception {
    assertEquals(DISPATCHING, job.getStatus());
  }

  @Test
  public void testGetFailureReason() throws Exception {
    assertEquals(NONE, job.getFailureReason());
  }

  @Test
  public void testGetCreatedHost() throws Exception {
    assertEquals("localhost", job.getCreatedHost());
  }

  @Test
  public void testGetProcessingHost() throws Exception {
    assertEquals("remotehost", job.getProcessingHost());
  }

  @Test
  public void testGetDateCreated() throws Exception {
    assertEquals(created, job.getDateCreated());
  }

  @Test
  public void testGetDateStarted() throws Exception {
    assertEquals(started, job.getDateStarted());
  }

  @Test
  public void testGetDateCompleted() throws Exception {
    assertEquals(completed, job.getDateCompleted());
  }

  @Test
  public void testGetQueueTime() throws Exception {
    assertEquals((Long) 100L, job.getQueueTime());
  }

  @Test
  public void testGetRunTime() throws Exception {
    assertEquals((Long) 200L, job.getRunTime());
  }

  @Test
  public void testGetPayload() throws Exception {
    assertEquals("result", job.getPayload());
  }

  @Test
  public void testGetParentJobId() throws Exception {
    assertEquals((Long) 3L, job.getParentJobId());
  }

  @Test
  public void testGetRootJobId() throws Exception {
    assertEquals((Long) 1L, job.getRootJobId());
  }

  @Test
  public void testIsDispatchable() throws Exception {
    assertTrue(job.isDispatchable());
  }

  @Test
  public void testGetUri() throws Exception {
    assertEquals(uri, job.getUri());
  }

  @Test
  public void testGetSignature() throws Exception {
    assertEquals(2076214452, job.getSignature());
  }

  @Test
  public void testGetJobLoad() throws Exception {
    assertEquals((Float) 1.5F, job.getJobLoad());
  }

  @Test
  public void testEquals() throws Exception {
    Job equalJob = new JobImpl(3L, "test", "test_org", 0L, "simple", "do", arguments, DISPATCHING, "localhost", "remotehost",
            created, started, completed, 100L, 200L, "result", 3L, 1L, true, uri, 1.5F);

    assertEquals(job, equalJob);
  }

  @Test
  public void testToString() throws Exception {
    Job newJob = new JobImpl(3L, "test", "test_org", 0L, "simple", "do", arguments, DISPATCHING, "localhost",
            "remotehost", created, started, completed, 100L, 200L, "result", 3L, 1L, true, uri, 1.5F);
    String jobString = "Job {id:3, operation:do, status:DISPATCHING}";
    assertEquals(newJob.toString(), jobString);
  }
}
