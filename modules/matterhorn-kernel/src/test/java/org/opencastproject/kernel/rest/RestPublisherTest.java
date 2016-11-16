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

package org.opencastproject.kernel.rest;

import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class RestPublisherTest extends RestPublisher {

  private ResourceComparator rc = new OsgiCxfEndpointComparator();

  @Test
  public void testResourceComparatorSameClass() {
    Message message = new MessageImpl();

    ClassResourceInfo cri1 = new ClassResourceInfo(this.getClass());
    ClassResourceInfo cri2 = new ClassResourceInfo(this.getClass());

    OperationResourceInfo oper1 = new OperationResourceInfo(this.getClass().getMethods()[0], cri1);
    OperationResourceInfo oper2 = new OperationResourceInfo(this.getClass().getMethods()[0], cri2);

    Assert.assertEquals(0, rc.compare(cri1, cri2, message));
    Assert.assertEquals(0, rc.compare(oper1, oper2, message));
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testResourceComparatorSameNonMatchEqualEndpoint() {
    ServiceReference serviceReference = EasyMock.createNiceMock(ServiceReference.class);
    EasyMock.expect(serviceReference.getProperty(SERVICE_PATH_PROPERTY)).andReturn("/events").anyTimes();
    EasyMock.replay(serviceReference);

    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getServiceReference(EasyMock.anyString())).andReturn(serviceReference).anyTimes();
    EasyMock.replay(bc);

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(cc);

    componentContext = cc;

    Message message = new MessageImpl();
    ExchangeImpl exchange = new ExchangeImpl();
    message.setExchange(exchange);
    message.put(Message.ENDPOINT_ADDRESS, "http://localhost:8080/series");

    ClassResourceInfo cri1 = new ClassResourceInfo(this.getClass());
    ClassResourceInfo cri2 = new ClassResourceInfo(RestPublisher.class.getClass());

    OperationResourceInfo oper1 = new OperationResourceInfo(this.getClass().getMethods()[0], cri1);
    OperationResourceInfo oper2 = new OperationResourceInfo(RestPublisher.class.getClass().getMethods()[0], cri2);

    Assert.assertEquals(0, rc.compare(cri1, cri2, message));
    Assert.assertEquals(0, rc.compare(oper1, oper2, message));
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testResourceComparatorSameNonMatch() {
    ServiceReference serviceReference = EasyMock.createNiceMock(ServiceReference.class);
    EasyMock.expect(serviceReference.getProperty(SERVICE_PATH_PROPERTY)).andReturn("/events").once();
    EasyMock.expect(serviceReference.getProperty(SERVICE_PATH_PROPERTY)).andReturn("/org").once();
    EasyMock.replay(serviceReference);

    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getServiceReference(EasyMock.anyString())).andReturn(serviceReference).anyTimes();
    EasyMock.replay(bc);

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(cc);

    componentContext = cc;

    Message message = new MessageImpl();
    ExchangeImpl exchange = new ExchangeImpl();
    message.setExchange(exchange);
    message.put(Message.ENDPOINT_ADDRESS, "http://localhost:8080/series");

    ClassResourceInfo cri1 = new ClassResourceInfo(this.getClass());
    ClassResourceInfo cri2 = new ClassResourceInfo(RestPublisher.class.getClass());

    OperationResourceInfo oper1 = new OperationResourceInfo(this.getClass().getMethods()[0], cri1);
    OperationResourceInfo oper2 = new OperationResourceInfo(RestPublisher.class.getClass().getMethods()[0], cri2);

    Assert.assertTrue(rc.compare(cri1, cri2, message) < 0);
    Assert.assertTrue(rc.compare(oper1, oper2, message) < 0);
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testResourceComparatorSameMatch() {
    ServiceReference serviceReference = EasyMock.createNiceMock(ServiceReference.class);
    EasyMock.expect(serviceReference.getProperty(SERVICE_PATH_PROPERTY)).andReturn("/events").once();
    EasyMock.expect(serviceReference.getProperty(SERVICE_PATH_PROPERTY)).andReturn("/series").once();
    EasyMock.replay(serviceReference);

    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getServiceReference(EasyMock.anyString())).andReturn(serviceReference).anyTimes();
    EasyMock.replay(bc);

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(cc);

    componentContext = cc;

    Message message = new MessageImpl();
    ExchangeImpl exchange = new ExchangeImpl();
    message.setExchange(exchange);
    message.put(Message.ENDPOINT_ADDRESS, "http://localhost:8080/series");

    ClassResourceInfo cri1 = new ClassResourceInfo(this.getClass());
    ClassResourceInfo cri2 = new ClassResourceInfo(RestPublisher.class.getClass());

    OperationResourceInfo oper1 = new OperationResourceInfo(this.getClass().getMethods()[0], cri1);
    OperationResourceInfo oper2 = new OperationResourceInfo(RestPublisher.class.getClass().getMethods()[0], cri2);

    Assert.assertEquals(1, rc.compare(cri1, cri2, message));
    Assert.assertEquals(1, rc.compare(oper1, oper2, message));
  }

}
