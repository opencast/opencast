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
package org.opencastproject.serviceregistry.impl.jpa;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.db.DBTestEnv.newDBSession;
import static org.opencastproject.db.Queries.namedQuery;
import static org.opencastproject.job.api.Job.Status.DISPATCHING;

import org.opencastproject.db.DBSession;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.job.jpa.JpaJob;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.security.impl.jpa.JpaUser;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ServiceRegistrationJpaImplTest {

  private DBSession db;

  private JpaOrganization org;
  private JpaUser user;

  @Before
  public void setUp() throws Exception {
    db = newDBSession("org.opencastproject.common");
    setUpOrganizationAndUsers();
  }

  private void setUpOrganizationAndUsers() {
    org = new JpaOrganization("test-org", "Test Organization", "http://testorg.edu", 80, "TEST_ORG_ADMIN",
            "TEST_ORG_ANON", new HashMap<String, String>());
    user = new JpaUser("producer1", "pw-producer1", org, "test", true, new HashSet<JpaRole>());

    org = db.execTx(namedQuery.persistOrUpdate(org));
    user = db.execTx(namedQuery.persistOrUpdate(user));
  }

  @After
  public void cleanUp() {
    db.close();
  }

  private JpaJob createJob(Date dateCreated, ServiceRegistrationJpaImpl serviceRegistry) {
    JpaJob job = new JpaJob(user, org, serviceRegistry, "NOP", null, null, true, 1.0F);
    job.setProcessorServiceRegistration(serviceRegistry);
    job.setQueueTime(500L);
    job.setRunTime(1000L);
    job.setDateCreated(dateCreated);
    return job;
  }

  @Test
  public void testQueryStatistics() throws Exception {
    HostRegistrationJpaImpl host = new HostRegistrationJpaImpl(
        "http://localhost:8081", "http://localhost:8081", "Admin", 1024L, 1, 1, true, false);
    ServiceRegistrationJpaImpl serviceReg = new ServiceRegistrationJpaImpl(host, "NOP", "/nop", false);

    Date now = new Date();

    host = db.execTx(namedQuery.persistOrUpdate(host));
    serviceReg = db.execTx(namedQuery.persistOrUpdate(serviceReg));

    JpaJob job = db.execTx(namedQuery.persistOrUpdate(createJob(now, serviceReg)));
    JpaJob jobYesterday = db.execTx(namedQuery.persistOrUpdate(createJob(DateUtils.addDays(now, -1), serviceReg)));

    /* find the job created at 'now' should reveal exactly one job */
    List<Object> statistic = db.execTx(
        namedQuery.findAll(
            "ServiceRegistration.statistics",
            Object.class,
            Pair.of("minDateCreated", now),
            Pair.of("maxDateCreated", now)
        )
    );

    Object[] stats = (Object[]) statistic.get(0);
    assertEquals(1, statistic.size());
    assertEquals(5, stats.length);
    assertEquals(serviceReg.getId().longValue(), ((Number) stats[0]).longValue());
    assertEquals(job.getStatus().ordinal(), ((Number) stats[1]).intValue());
    assertEquals(1, ((Number) stats[2]).intValue());
    assertEquals(500L, ((Number) stats[3]).longValue());
    assertEquals(1000L, ((Number) stats[4]).longValue());

    /* There are no jobs in the specific time interval */
    statistic = db.execTx(
        namedQuery.findAll(
            "ServiceRegistration.statistics",
            Object.class,
            Pair.of("minDateCreated", DateUtils.addDays(now, -3)),
            Pair.of("maxDateCreated", DateUtils.addDays(now, -2))
        )
    );

    assertEquals(0, statistic.size());
  }

  @Test
  public void testToString() throws Exception {
    Job newJob = new JobImpl(3L, "test", "test_org", 0L, "simple", "do", null, DISPATCHING, "localhost",
            "remotehost", null, null, null, 100L, 200L, "result", 3L, 1L, true, null, 1.5F);
    JpaJob jpaJob = JpaJob.from(newJob);
    String jobString = "Job {id:3, operation:do, status:DISPATCHING}";
    assertEquals(jpaJob.toString(), jobString);
  }
}
