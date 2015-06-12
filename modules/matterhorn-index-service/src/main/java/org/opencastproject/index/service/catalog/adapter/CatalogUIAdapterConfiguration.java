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

import static org.apache.commons.lang.StringUtils.isBlank;
import static java.lang.String.format;

import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.XmlNamespaceContext;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * This class parses a catalog UI adapter configuration and provides convenience access methods.
 */
public final class CatalogUIAdapterConfiguration {

  /* Configuration keys */
  public static final String KEY_XML_ROOT_ELEMENT_NAME = "xml.rootElement.name";
  public static final String KEY_XML_ROOT_ELEMENT_NS_URI = "xml.rootElement.namespace.URI";
  public static final String XML_BINDING_KEY_PREFIX = "xml.namespaceBinding.";
  public static final String XML_BINDING_URI_SUFFIX = ".URI";
  public static final String XML_BINDING_PREFIX_SUFFIX = ".prefix";

  /** The raw configuration properties */
  @SuppressWarnings("rawtypes")
  private final Dictionary configProperties;

  /** The xml namespace binding context */
  private XmlNamespaceContext xmlNSContext;

  /**
   * Load configuration from a dictionary.
   *
   * @param properties
   *          The configuration properties
   * @return The parsed configuration as {@link CatalogUIAdapterConfiguration} instance
   * @throws ConfigurationException
   *           If the configuration has any errors
   */
  public static CatalogUIAdapterConfiguration loadFromDictionary(@SuppressWarnings("rawtypes") Dictionary properties)
          throws ConfigurationException {
    return new CatalogUIAdapterConfiguration(properties);
  }

  private CatalogUIAdapterConfiguration(@SuppressWarnings("rawtypes") Dictionary properties)
          throws ConfigurationException {
    this.configProperties = properties;
    loadXmlNSContext();
    validate();
  }

  /**
   * Validates the configuration and throws a {@link ConfigurationException} if there is any error.
   *
   * @throws ConfigurationException
   *           if the configuration is not valid
   */
  private void validate() throws ConfigurationException {
    if (configProperties.get(KEY_XML_ROOT_ELEMENT_NAME) == null)
      throw new ConfigurationException(format("Value for configuration key '%s' is missing", KEY_XML_ROOT_ELEMENT_NAME));

    if (configProperties.get(KEY_XML_ROOT_ELEMENT_NS_URI) == null)
      throw new ConfigurationException(format("Value for configuration key '%s' is missing",
              KEY_XML_ROOT_ELEMENT_NS_URI));

    if (xmlNSContext.getPrefix(getCatalogXmlRootNamespace()) == null)
      throw new ConfigurationException(format("Binding for XML namespace URI '%s' is missing",
              getCatalogXmlRootNamespace()));
  }

  /**
   * Load the XML namespace bindings from the configuration and build the XML namespace context.
   */
  private void loadXmlNSContext() {
    @SuppressWarnings("rawtypes")
    final Enumeration keys = configProperties.keys();
    final Map<String, String> prefixToUri = new HashMap<String, String>();
    while (keys.hasMoreElements()) {
      final String key = (String) keys.nextElement();
      if (key.startsWith(XML_BINDING_KEY_PREFIX)) {
        // First, we need to get the name of the binding
        final String nsBindingName = getXmlBindingNameFromConfigKey(key);
        // Once we have the name, we're able to retrieve the URI as well as the prefix
        final String nsUri = (String) configProperties.get(XML_BINDING_KEY_PREFIX + nsBindingName
                + XML_BINDING_URI_SUFFIX);
        final String nsPrefix = (String) configProperties.get(XML_BINDING_KEY_PREFIX + nsBindingName
                + XML_BINDING_PREFIX_SUFFIX);
        // Check if URI and the prefix have valid values
        if (isBlank(nsUri))
          throw new ConfigurationException(format("No URI for namespace binding '%s' found", nsBindingName));
        if (nsPrefix == null)
          throw new ConfigurationException(format("No prefix for namespace binding '%s' found", nsBindingName));
        // Add prefix & URI to the intermediate map
        prefixToUri.put(nsPrefix, nsUri);
      }
    }

    xmlNSContext = new XmlNamespaceContext(prefixToUri);
  }

  /**
   * Get the name of an XML namespace binding by one of its configuration keys.
   *
   * @param key
   *          the key name
   * @return the XML namespace binding name
   */
  private static String getXmlBindingNameFromConfigKey(final String key) {
    if (isBlank(key) || !key.startsWith(XML_BINDING_KEY_PREFIX))
      throw new IllegalArgumentException(format("The given key '%s' is not part of a XML binding definition", key));
    final String keyWithoutPrefix = key.substring(XML_BINDING_KEY_PREFIX.length());
    return keyWithoutPrefix.substring(0, keyWithoutPrefix.indexOf("."));
  }

  /**
   * Return the value of the configuration property {@link CatalogUIAdapterConfiguration#KEY_XML_ROOT_ELEMENT_NAME}
   */
  public String getCatalogXmlRootElementName() {
    return (String) configProperties.get(KEY_XML_ROOT_ELEMENT_NAME);
  }

  /**
   * Return the value of the configuration property {@link CatalogUIAdapterConfiguration#KEY_XML_ROOT_ELEMENT_NS_URI}
   */
  public String getCatalogXmlRootNamespace() {
    return (String) configProperties.get(KEY_XML_ROOT_ELEMENT_NS_URI);
  }

  /**
   * Returns the XML namespace context that could be built out of the configuration.
   */
  public XmlNamespaceContext getXmlNamespaceContext() {
    return xmlNSContext;
  }

}
