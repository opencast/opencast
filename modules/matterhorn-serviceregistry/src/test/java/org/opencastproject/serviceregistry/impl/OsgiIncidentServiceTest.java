/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.serviceregistry.impl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.opencastproject.fn.juc.Immutables;
import org.opencastproject.fn.juc.Mutables;
import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.Job;
import org.opencastproject.serviceregistry.api.Incidents;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceEnvs;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.util.persistence.Queries;
import org.opencastproject.workflow.api.WorkflowService;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
  private Map<Long, JobJpaImpl> jobs = Mutables.map();
  private Incidents incidents;

  /** @throws java.lang.Exception */
  @Before
  public void setUp() throws Exception {
    final EntityManagerFactory emf = PersistenceUtil
            .newTestEntityManagerFactory(AbstractIncidentService.PERSISTENCE_UNIT_NAME);
    penv = PersistenceEnvs.persistenceEnvironment(emf);

    // Mock up a job
    JobJpaImpl job = new JobJpaImpl();
    job.setProcessingHost(PROCESSING_HOST);
    job.setJobType(JOB_TYPE);
    job.setCreator("creator");
    job.setOrganization("organization");

    // Mock up a service registry
    final ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andAnswer(new IAnswer<Job>() {
      @Override
      public Job answer() throws Throwable {
        final Long jobId = (Long) EasyMock.getCurrentArguments()[0];
        return jobs.get(jobId);
      }
    }).anyTimes();
    EasyMock.replay(serviceRegistry);

    // Mock up a workflow service
    final WorkflowService workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.replay(workflowService);

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
    final JobJpaImpl job = new JobJpaImpl();
    job.setCreator("creator");
    job.setOrganization("organization");
    job.setProcessingHost("localhost");
    job.setJobType("org.opencastproject.service");
    final JobJpaImpl pjob = penv.tx(Queries.persist(job));
    jobs.put(pjob.getId(), pjob);
    assertThat(pjob.getId(), is(not(0L)));
    incidents.record(pjob, Incident.Severity.FAILURE, 1511);
    // retrieve the job incident
    final List<Incident> incidents = incidentService.getIncidentsOfJob(Immutables.list(pjob.getId()));
    assertEquals(1, incidents.size());
    assertEquals(Incident.Severity.FAILURE, incidents.get(0).getSeverity());
    assertEquals("localhost", incidents.get(0).getProcessingHost());
    assertEquals("org.opencastproject.service", incidents.get(0).getServiceType());
    assertEquals("org.opencastproject.service.1511", incidents.get(0).getCode());
    // todo more tests
  }

  @Test
  public void testGenDbKeys() {
    assertEquals(Immutables.list("org.opencastproject.composer.1.title.de.DE",
            "org.opencastproject.composer.1.title.de", "org.opencastproject.composer.1.title"),
            OsgiIncidentService.genDbKeys(OsgiIncidentService.localeToList(Locale.GERMANY),
                    "org.opencastproject.composer.1.title"));
    assertEquals(Immutables.list("org.opencastproject.composer.1.title"),
            OsgiIncidentService.genDbKeys(Immutables.<String> nil(), "org.opencastproject.composer.1.title"));
  }
}
