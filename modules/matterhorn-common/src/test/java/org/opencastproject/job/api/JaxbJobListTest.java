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
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import com.entwinemedia.fn.Stream;

import org.junit.Test;

public class JaxbJobListTest {

  @Test
  public void testNewFromList() throws Exception {
    Job job1 = createNiceMock(Job.class);
    replay(job1);

    JaxbJobList jobList = new JaxbJobList(Stream.$(job1).toList());

    assertEquals(1, jobList.getJobs().size());
  }


  @Test
  public void testAddJob() throws Exception {
    JaxbJob job1 = createNiceMock(JaxbJob.class);
    replay(job1);

    JaxbJobList jobList = new JaxbJobList();
    jobList.add(job1);

    assertEquals(1, jobList.getJobs().size());
  }

}
