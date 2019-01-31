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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.Job;
import org.opencastproject.serviceregistry.api.Incidents;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceEnvs;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.workflow.api.WorkflowService;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

/** Tests persistence: storing, merging, retrieving and removing. */
public class OsgiIncidentServiceTest {

  private static final String PROCESSING_HOST = "http://localhost:8080";
  private static final String JOB_TYPE = "inspect";

  private AbstractIncidentService incidentService;
  private PersistenceEnv penv;
  private Map<Long, Job> jobs = new HashMap<>();
  private Incidents incidents;

  /** @throws java.lang.Exception */
  @Before
  public void setUp() throws Exception {
    final EntityManagerFactory emf = PersistenceUtil
            .newTestEntityManagerFactory(AbstractIncidentService.PERSISTENCE_UNIT_NAME);
    penv = PersistenceEnvs.persistenceEnvironment(emf);

    // Mock up a job
    Job job = createNiceMock(Job.class);
    expect(job.getProcessingHost()).andStubReturn(PROCESSING_HOST);
    expect(job.getJobType()).andStubReturn(JOB_TYPE);
    expect(job.getCreator()).andStubReturn("creator");
    expect(job.getOrganization()).andStubReturn("organization");
    replay(job);

    // Mock up a service registry
    final ServiceRegistry serviceRegistry = createNiceMock(ServiceRegistry.class);
    expect(serviceRegistry.getJob(EasyMock.anyLong())).andAnswer(new IAnswer<Job>() {
      @Override
      public Job answer() throws Throwable {
        final Long jobId = (Long) EasyMock.getCurrentArguments()[0];
        return jobs.get(jobId);
      }
    }).anyTimes();
    replay(serviceRegistry);

    // Mock up a workflow service
    final WorkflowService workflowService = createNiceMock(WorkflowService.class);
    replay(workflowService);

    incidentService = new AbstractIncidentService() {
      @Override
      protected ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
      }

      @Override
      protected WorkflowService getWorkflowService() {
        return workflowService;
      }

      @Override
      protected PersistenceEnv getPenv() {
        return PersistenceEnvs.persistenceEnvironment(emf);
      }
    };
    incidents = new Incidents(serviceRegistry, incidentService);
  }

  /** @throws java.lang.Exception */
  @After
  public void tearDown() throws Exception {
    penv.close();
  }

  @Test
  public void testRetrieving() throws Exception {
    // manually create and store a job bypassing the service registry because the JPA implementation of the registry
    // is not very test friendly


    // Mock up a job
    Job job = createNiceMock(Job.class);
    expect(job.getId()).andStubReturn(1L);
    expect(job.getProcessingHost()).andStubReturn("localhost");
    expect(job.getJobType()).andStubReturn("org.opencastproject.service");
    expect(job.getCreator()).andStubReturn("creator");
    expect(job.getOrganization()).andStubReturn("organization");
    replay(job);

    jobs.put(job.getId(), job);
    incidents.record(job, Incident.Severity.FAILURE, 1511);
    // retrieve the job incident
    final List<Incident> incidents = incidentService.getIncidentsOfJob(Collections.singletonList(job.getId()));
    assertEquals(1, incidents.size());
    assertEquals(Incident.Severity.FAILURE, incidents.get(0).getSeverity());
    assertEquals("localhost", incidents.get(0).getProcessingHost());
    assertEquals("org.opencastproject.service", incidents.get(0).getServiceType());
    assertEquals("org.opencastproject.service.1511", incidents.get(0).getCode());
    // todo more tests
  }

  @Test
  public void testGenDbKeys() {
    assertEquals(Arrays.asList("org.opencastproject.composer.1.title.de.DE", "org.opencastproject.composer.1.title.de",
            "org.opencastproject.composer.1.title"),
            OsgiIncidentService.genDbKeys(OsgiIncidentService.localeToList(Locale.GERMANY),
                    "org.opencastproject.composer.1.title"));
    assertEquals(Collections.singletonList("org.opencastproject.composer.1.title"),
            OsgiIncidentService.genDbKeys(Collections.emptyList(), "org.opencastproject.composer.1.title"));
  }
}
