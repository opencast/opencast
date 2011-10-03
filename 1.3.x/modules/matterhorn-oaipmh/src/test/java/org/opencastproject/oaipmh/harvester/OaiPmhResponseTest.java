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

package org.opencastproject.oaipmh.harvester;

import org.junit.Test;
import org.opencastproject.oaipmh.Granularity;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;

public class OaiPmhResponseTest {

  @Test
  public void testExtractGranularity() throws Exception {
    IdentifyResponse response = new IdentifyResponse(loadDoc("identify-response.xml"));
    assertEquals(Granularity.DAY, response.getGranularity());
  }

  @Test
  public void testExtractMetadataPrefix() throws Exception {
    ListRecordsResponse response = new ListRecordsResponse(loadDoc("list-records-response.xml"));
    assertEquals("matterhorn", response.getMetadataPrefix());
  }

  public Document loadDoc(String name) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder().parse(this.getClass().getResourceAsStream(name));
  }
}
