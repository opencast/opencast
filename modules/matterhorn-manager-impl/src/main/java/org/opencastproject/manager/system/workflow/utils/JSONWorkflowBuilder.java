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


package org.opencastproject.manager.system.workflow.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.opencastproject.manager.core.MetadataDocumentHandler;
import org.osgi.framework.BundleContext;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

/**
 * This class handles JSON objects for workflow's.
 *
 * @author Leonid Oldenburger
 */
public class JSONWorkflowBuilder {

  /**
   * The bundle context
   */
  private BundleContext bundleContext;

  /**
   * The Document Handler
   */
  private MetadataDocumentHandler handleDocument = new MetadataDocumentHandler();

  /**
   * Constructor
   *
   * @param bundleContext
   */
  public JSONWorkflowBuilder(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  /**
   * Creates HashMap with workflow's data.
   *
   * @return string writer object
   * @throws ParserConfigurationException
   * @throws IOException
   * @throws SAXEception
   */
  public StringWriter createHashMapWorkflowDataFromXML() throws ParserConfigurationException, IOException, SAXException {

        StringWriter w = new StringWriter();

    File folder = new File("etc/workflows/");

    File[] filesInFolder = folder.listFiles();

        if (filesInFolder != null) {
            for (final File fileEntry : filesInFolder) {
                if (!fileEntry.isDirectory()) {

                    Document doc = handleDocument.getDocumentBuilder().parse(fileEntry);
                    doc.getDocumentElement().normalize();
                    String id = doc.getDocumentElement().getElementsByTagName("id").item(0).getTextContent();

                 w.append("{\"id\":\"" + id + "\", \"name\":\"" + fileEntry.getName() + "\"}, ");
                }
            }
        }

    return w;
  }
}
