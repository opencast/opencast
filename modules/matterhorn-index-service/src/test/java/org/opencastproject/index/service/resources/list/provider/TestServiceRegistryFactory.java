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
package org.opencastproject.index.service.resources.list.provider;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.List;

@Ignore
public final class TestServiceRegistryFactory {
  private static List<ServiceStatistics> testData;
  private static ServiceRegistry serviceRegistry;

  private TestServiceRegistryFactory() {
  }

  static {
    testData = new ArrayList<ServiceStatistics>();
    testData.add(new TestServiceStatistics());
    serviceRegistry = createMock(ServiceRegistry.class);
  }

  public static ServiceRegistry getStub() {
    EasyMock.reset(serviceRegistry);
    return getVerifiableInstance();
  }

  public static ServiceRegistry getVerifiableInstance() {
    try {
      expect(serviceRegistry.getServiceStatistics()).andReturn(testData);
      replay(serviceRegistry);
    } catch (ServiceRegistryException e) {
      throw new RuntimeException("oops, could not sertup test service registry");
    }
    return serviceRegistry;
  }

  public static void verify() {
    org.easymock.EasyMock.verify(serviceRegistry);
  }

  public static void reset() {
    EasyMock.reset(serviceRegistry);
  }
}
