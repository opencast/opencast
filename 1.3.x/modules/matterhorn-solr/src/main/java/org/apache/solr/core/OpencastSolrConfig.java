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
package org.apache.solr.core;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Provides a static reference to a Config object modeling the main configuration data for a a Solr instance.
 *
 */
public class OpencastSolrConfig extends SolrConfig {

  /**
   * Overrides the {@link SolrConfig} constructor in order to pass an OSGi-safe {@link SolrResourceLoader}.
   * 
   * @param instanceDir
   * @param name
   * @param is
   * @throws ParserConfigurationException
   * @throws IOException
   * @throws SAXException
   */
  public OpencastSolrConfig(String instanceDir, String name, InputStream is) throws ParserConfigurationException,
          IOException, SAXException {
    super(new SolrResourceLoader(instanceDir, OpencastSolrConfig.class.getClassLoader()), name, is);
  }
}
