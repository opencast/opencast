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

import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.JobDispatcher;
import org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.JobProducerHearbeat;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

public class ServiceRegistryJpaImplTest {
  private Query query = null;
  private EntityTransaction tx = null;
  private EntityManager em = null;
  private EntityManagerFactory emf = null;
  private Map<String, Object> persistenceProperties = null;
  private PersistenceProvider persistenceProvider = null;
  private BundleContext bundleContext = null;
  private ComponentContext cc = null;
  private ServiceRegistryJpaImpl serviceRegistryJpaImpl = null;
  private HostRegistration hostRegistration = null;

  @Before
  public void setUp() throws InvalidSyntaxException {
    // Setup mock objects
    setUpQuery();
    setUpEntityTransaction();
    setUpEntityManager();
    setUpEntityManagerFactory();
    setUpPersistenceProperties();
    setUpPersistenceProvider();
    // Setup context settings
    setupBundleContext();
    setupComponentContext();
    // Setup test object.
    setUpServiceRegistryJpaImpl();
  }

  public void setUpQuery() {
    query = EasyMock.createNiceMock(Query.class);
    EasyMock.expect(query.getSingleResult()).andReturn(new HostRegistration("http://localhost:8080", 9, true, false));
    EasyMock.expect(query.getResultList()).andReturn(new ArrayList<Object>()).anyTimes();
    EasyMock.replay(query);
  }

  public void setUpEntityTransaction() {
    tx = EasyMock.createNiceMock(EntityTransaction.class);
  }

  public void setUpEntityManager() {
    em = EasyMock.createNiceMock(EntityManager.class);
    EasyMock.expect(em.getTransaction()).andReturn(tx);
    EasyMock.expect(em.createNamedQuery("HostRegistration.byHostName")).andReturn(query);
    EasyMock.expect(em.createNamedQuery("ServiceRegistration.statistics")).andReturn(query);
    EasyMock.expect(em.createNamedQuery("ServiceRegistration.getAll")).andReturn(query);
    EasyMock.replay(em);
  }

  public void setUpEntityManagerFactory() {
    emf = EasyMock.createMock(EntityManagerFactory.class);
    EasyMock.expect(emf.createEntityManager()).andReturn(em).anyTimes();
    EasyMock.replay(emf);
  }

  @SuppressWarnings("unchecked")
  public void setUpPersistenceProperties() {
    persistenceProperties = EasyMock.createMock(Map.class);
  }

  public void setUpPersistenceProvider() {
    persistenceProvider = EasyMock.createMock(PersistenceProvider.class);
    EasyMock.expect(
            persistenceProvider
                    .createEntityManagerFactory("org.opencastproject.serviceregistry", persistenceProperties))
            .andReturn(emf);
    EasyMock.replay(persistenceProvider);
  }

  public void setUpServiceRegistryJpaImpl() {
    serviceRegistryJpaImpl = new ServiceRegistryJpaImpl();
    serviceRegistryJpaImpl.setPersistenceProperties(persistenceProperties);
    serviceRegistryJpaImpl.setPersistenceProvider(persistenceProvider);
  }

  private void setupBundleContext() throws InvalidSyntaxException {
    bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.server.url")).andReturn("");
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.jobs.url")).andReturn("");
    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_MAXLOAD)).andReturn("");
    EasyMock.expect(bundleContext.createFilter((String) EasyMock.anyObject())).andReturn(
            EasyMock.createNiceMock(Filter.class));
    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_DISPATCHINTERVAL)).andReturn("0");
  }

  private void setupComponentContext() {
    cc = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(cc);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void nullContextActivatesOkay() throws ServiceRegistryException {
    serviceRegistryJpaImpl.scheduledExecutor = EasyMock.createMock(ScheduledExecutorService.class);

    EasyMock.expect(
            serviceRegistryJpaImpl.scheduledExecutor.scheduleWithFixedDelay((JobDispatcher) EasyMock.anyObject(),
                    EasyMock.eq(ServiceRegistryJpaImpl.DEFAULT_DISPATCH_PERIOD),
                    EasyMock.eq(ServiceRegistryJpaImpl.DEFAULT_DISPATCH_PERIOD), EasyMock.eq(TimeUnit.MILLISECONDS)))
            .andReturn(EasyMock.createNiceMock(ScheduledFuture.class));

    EasyMock.expect(
            serviceRegistryJpaImpl.scheduledExecutor.scheduleWithFixedDelay((JobProducerHearbeat) EasyMock.anyObject(),
                    EasyMock.eq(ServiceRegistryJpaImpl.DEFAULT_HEART_BEAT),
                    EasyMock.eq(ServiceRegistryJpaImpl.DEFAULT_HEART_BEAT), EasyMock.eq(TimeUnit.MINUTES))).andReturn(
            EasyMock.createNiceMock(ScheduledFuture.class));
    EasyMock.replay(serviceRegistryJpaImpl.scheduledExecutor);

    serviceRegistryJpaImpl.activate(null);
  }

  @Test
  public void heartBeatDisabledWithZeroInterval() {
    String input = "0";

    serviceRegistryJpaImpl.scheduledExecutor = EasyMock.createMock(ScheduledExecutorService.class);
    EasyMock.replay(serviceRegistryJpaImpl.scheduledExecutor);

    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_HEARTBEATINTERVAL)).andReturn(input);
    EasyMock.replay(bundleContext);

    serviceRegistryJpaImpl.activate(cc);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void heartBeatDefaultValueSetProperly() {
    String input = "";
    long expected = ServiceRegistryJpaImpl.DEFAULT_HEART_BEAT;

    serviceRegistryJpaImpl.scheduledExecutor = EasyMock.createMock(ScheduledExecutorService.class);
    EasyMock.expect(
            serviceRegistryJpaImpl.scheduledExecutor.scheduleWithFixedDelay((JobProducerHearbeat) EasyMock.anyObject(),
                    EasyMock.eq(expected), EasyMock.eq(expected), EasyMock.eq(TimeUnit.MINUTES))).andReturn(
            EasyMock.createNiceMock(ScheduledFuture.class));
    EasyMock.replay(serviceRegistryJpaImpl.scheduledExecutor);

    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_HEARTBEATINTERVAL)).andReturn(input);
    EasyMock.replay(bundleContext);

    serviceRegistryJpaImpl.activate(cc);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void heartBeatNegativeValueSetToDefault() {
    String input = "-20";
    long expected = ServiceRegistryJpaImpl.DEFAULT_HEART_BEAT;

    serviceRegistryJpaImpl.scheduledExecutor = EasyMock.createMock(ScheduledExecutorService.class);
    EasyMock.expect(
            serviceRegistryJpaImpl.scheduledExecutor.scheduleWithFixedDelay((JobProducerHearbeat) EasyMock.anyObject(),
                    EasyMock.eq(expected), EasyMock.eq(expected), EasyMock.eq(TimeUnit.MINUTES))).andReturn(
            EasyMock.createNiceMock(ScheduledFuture.class));
    EasyMock.replay(serviceRegistryJpaImpl.scheduledExecutor);

    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_HEARTBEATINTERVAL)).andReturn(input);
    EasyMock.replay(bundleContext);

    serviceRegistryJpaImpl.activate(cc);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void heartBeatNominalValueSetProperly() {
    String input = "10";
    long expected = 10;

    serviceRegistryJpaImpl.scheduledExecutor = EasyMock.createMock(ScheduledExecutorService.class);
    EasyMock.expect(
            serviceRegistryJpaImpl.scheduledExecutor.scheduleWithFixedDelay((JobProducerHearbeat) EasyMock.anyObject(),
                    EasyMock.eq(expected), EasyMock.eq(expected), EasyMock.eq(TimeUnit.MINUTES))).andReturn(
            EasyMock.createNiceMock(ScheduledFuture.class));
    EasyMock.replay(serviceRegistryJpaImpl.scheduledExecutor);

    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_HEARTBEATINTERVAL)).andReturn(input);
    EasyMock.replay(bundleContext);

    serviceRegistryJpaImpl.activate(cc);
  }
}
