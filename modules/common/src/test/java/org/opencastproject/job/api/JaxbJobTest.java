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
import static org.opencastproject.job.api.Job.Status.DISPATCHING;

import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class JaxbJobTest {

  @Test
  public void testMappingOfAllFields() throws Exception {

    final List<String> arguments = Arrays.asList("arg-1", "arg-2");
    final Date now = new Date();
    final URI uri = new URI("http://test");
    final List<Long> blockedJobs = Arrays.asList(5L, 6L);

    final JaxbJob jaxb = new JaxbJob(
            new JobImpl(3L, "test", "test_org", 0L, "simple", "do", arguments, DISPATCHING, "localhost", "remotehost",
                    now, now, now, 100L, 200L, "result", 1L, 3L, true, uri, 1.5F));
    final Job job = jaxb.toJob();

    assertEquals(3L, job.getId());
    assertEquals("test", job.getCreator());
    assertEquals("test_org", job.getOrganization());
    assertEquals(0L, job.getVersion());
    assertEquals("simple", job.getJobType());
    assertEquals("do", job.getOperation());
    assertEquals(arguments, job.getArguments());
    assertEquals(DISPATCHING, job.getStatus());
    assertEquals("localhost", job.getCreatedHost());
    assertEquals("remotehost", job.getProcessingHost());
    assertEquals(now, job.getDateStarted());
    assertEquals(now, job.getDateCreated());
    assertEquals(now, job.getDateCompleted());
    assertEquals((Long) 100L, job.getQueueTime());
    assertEquals((Long) 200L, job.getRunTime());
    assertEquals("result", job.getPayload());
    assertEquals((Long) 1L, job.getParentJobId());
    assertEquals((Long) 3L, job.getRootJobId());
    assertTrue(job.isDispatchable());
    assertEquals(uri, job.getUri());
    assertEquals((Float) 1.5F, job.getJobLoad());
  }

}
