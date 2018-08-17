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


package org.opencastproject.oaipmh.harvester;

import static org.junit.Assert.assertEquals;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;

public class OaiPmhResponseTest {

  public void testMetadataFromRecord() throws Exception {
    ListRecordsResponse response = new ListRecordsResponse(loadDoc("list-records-response.xml"));
    assertEquals(1, response.getRecords().getLength());
    assertEquals("mediapackage", ListRecordsResponse.metadataOfRecord(response.getRecords().item(0)).getNodeName());
  }

  public Document loadDoc(String name) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder().parse(this.getClass().getResourceAsStream(name));
  }
}
