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

import static org.junit.Assert.assertEquals;

import org.opencastproject.util.ConfigurationException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

/**
 * Test class for {@link CatalogUIAdapterConfiguration}.
 */
public class CatalogUIAdapterConfigurationTest {

  private final Properties configProperties = new Properties();

  @Before
  public void setUpClass() throws Exception {
    // Reset the config properties
    configProperties.clear();

    // Load the config properties
    InputStream in = getClass().getResourceAsStream("/catalog-adapter/event.properties");
    try {
      configProperties.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Test
  public void testGetXmlRootElementName() {
    CatalogUIAdapterConfiguration configuration = CatalogUIAdapterConfiguration.loadFromDictionary(configProperties);
    assertEquals("custom-root-element-name", configuration.getCatalogXmlRootElementName());
  }

  @Test(expected = ConfigurationException.class)
  public void testMissingXmlRootElementName() {
    configProperties.remove(CatalogUIAdapterConfiguration.KEY_XML_ROOT_ELEMENT_NAME);
    CatalogUIAdapterConfiguration.loadFromDictionary(configProperties);
  }

  @Test
  public void testGetXmlRootNamespace() {
    CatalogUIAdapterConfiguration configuration = CatalogUIAdapterConfiguration.loadFromDictionary(configProperties);
    assertEquals("http://myorg.com/metadata/catalog", configuration.getCatalogXmlRootNamespace());
  }

  @Test(expected = ConfigurationException.class)
  public void testMissingXmlRootNamespace() {
    configProperties.remove(CatalogUIAdapterConfiguration.KEY_XML_ROOT_ELEMENT_NS_URI);
    CatalogUIAdapterConfiguration.loadFromDictionary(configProperties);
  }

  @Test(expected = ConfigurationException.class)
  public void testMissingXmlRootNamespaceBinding() {
    configProperties.remove("xml.namespaceBinding.root.URI");
    configProperties.remove("xml.namespaceBinding.root.prefix");
    CatalogUIAdapterConfiguration.loadFromDictionary(configProperties);
  }

}
