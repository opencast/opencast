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

package org.opencastproject.index.service.catalog.adapter;

import org.opencastproject.util.PropertiesUtil;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Test class for {@link CatalogUIAdapterFactory}.
 */
public class CatalogUIAdapterFactoryTest {

  private Dictionary<String, String> configProperties = new Hashtable<>();
  private final CatalogUIAdapterFactory factory = new CatalogUIAdapterFactory();

  @Before
  public void setUpClass() throws Exception {
    // Load the config properties
    InputStream in = getClass().getResourceAsStream("/catalog-adapter/event.properties");
    Properties props = new Properties();
    try {
      props.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    configProperties = PropertiesUtil.toDictionary(props);
  }

  @Test
  public void testRegisteringCommonCatalog() throws Exception {
    ServiceRegistration service = EasyMock.createNiceMock(ServiceRegistration.class);
    BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getServiceReferences(EasyMock.anyObject(Class.class), EasyMock.anyString()))
            .andReturn(new ArrayList<>()).once();
    EasyMock.expect(bundleContext.registerService(EasyMock.anyObject(String[].class), EasyMock.anyObject(),
            EasyMock.anyObject(Dictionary.class))).andReturn(service).once();
    ComponentContext componentContext = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(componentContext.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(componentContext, bundleContext, service);

    factory.activate(componentContext);
    factory.updated("testPid", configProperties);
    factory.deleted("testPid");
  }

}
