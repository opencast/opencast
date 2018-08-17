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
package org.opencastproject.email.template.impl;

import org.opencastproject.job.api.Incident;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EmailDataTest {

  private WorkflowInstance workflowInstance;
  private MediaPackage mp;
  private final HashMap<String, HashMap<String, String>> catalogs = new HashMap<String, HashMap<String, String>>();
  private WorkflowOperationInstance failedOperation;
  private Incident incident1;
  private Incident incident2;
  private List<Incident> incidents;
  private URI uriMP;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    uriMP = EmailDataTest.class.getResource("/email_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    URI episodeURI = EmailDataTest.class.getResource("/episode_dublincore.xml").toURI();
    URI seriesURI = EmailDataTest.class.getResource("/series_dublincore.xml").toURI();

    DublinCoreCatalog episodeDc = DublinCores.read(episodeURI.toURL().openStream());
    catalogs.put("episode", buildCatalogHash(episodeDc));

    DublinCoreCatalog seriesDc = DublinCores.read(seriesURI.toURL().openStream());
    catalogs.put("series", buildCatalogHash(seriesDc));

    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("wfdef");
    Map<String, String> props = new HashMap<String, String>();
    props.put("emailAddress", "user@domain.com");

    // Create some incidents
    incident1 = EasyMock.createNiceMock(Incident.class);
    List<Tuple<String, String>> details = new LinkedList<Tuple<String, String>>();
    Tuple<String, String> detail = new Tuple<String, String>("detail-type", "error in operation1");
    details.add(detail);
    EasyMock.expect(incident1.getDetails()).andReturn(details);

    incident2 = EasyMock.createNiceMock(Incident.class);
    details = new LinkedList<Tuple<String, String>>();
    detail = new Tuple<String, String>("detail-type", "error in operation2");
    details.add(detail);
    EasyMock.expect(incident2.getDetails()).andReturn(details);

    // Link the incident and the subtree
    incidents = new LinkedList<Incident>();
    incidents.add(incident1);
    incidents.add(incident2);

    workflowInstance = new WorkflowInstanceImpl(def, null, null, null, null, props);
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);

    failedOperation = new WorkflowOperationInstanceImpl("operation1", OperationState.FAILED);

    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("email", OperationState.RUNNING);
    List<WorkflowOperationInstance> operationList = new ArrayList<WorkflowOperationInstance>();
    operationList.add(failedOperation);
    operationList.add(operation);
    workflowInstance.setOperations(operationList);

    EasyMock.replay(incident1, incident2);
  }

  private HashMap<String, String> buildCatalogHash(DublinCoreCatalog dc) {
    HashMap<String, String> catalogHash = new HashMap<String, String>();
    for (EName ename : dc.getProperties()) {
      String name = ename.getLocalName();
      catalogHash.put(name,
              dc.getAsText(ename, DublinCore.LANGUAGE_ANY, EmailTemplateServiceImpl.DEFAULT_DELIMITER_FOR_MULTIPLE));
    }
    return catalogHash;
  }

  @Test
  public void testToMap() throws Exception {
    EmailData emailData = new EmailData("data1", workflowInstance, catalogs, failedOperation, incidents);

    Map<String, Object> map = emailData.toMap();

    Object catalogs = map.get("catalogs");
    Assert.assertNotNull(catalogs);
    Assert.assertTrue(catalogs instanceof HashMap);

    Object catalogHash = ((HashMap) catalogs).get("episode");
    Assert.assertNotNull(catalogHash);
    Assert.assertTrue(catalogHash instanceof HashMap);
    Assert.assertEquals("Test Media Package", ((HashMap) catalogHash).get("title"));

    catalogHash = ((HashMap) catalogs).get("series");
    Assert.assertNotNull(catalogHash);
    Assert.assertTrue(catalogHash instanceof HashMap);
    Assert.assertEquals("20140119997", ((HashMap) catalogHash).get("identifier"));

    Object mp = map.get("mediaPackage");
    Assert.assertNotNull(mp);
    Assert.assertTrue(mp instanceof MediaPackage);
    Assert.assertEquals("Test Media Package", ((MediaPackage) mp).getTitle());

    Object wf = map.get("workflow");
    Assert.assertNotNull(wf);
    Assert.assertTrue(wf instanceof WorkflowInstance);
    Assert.assertEquals(1, ((WorkflowInstance) wf).getId());

    Object wfConf = map.get("workflowConfig");
    Assert.assertNotNull(wfConf);
    Assert.assertTrue(wfConf instanceof Map);
    Assert.assertEquals("user@domain.com", ((Map) wfConf).get("emailAddress"));

    Object op = map.get("failedOperation");
    Assert.assertNotNull(op);
    Assert.assertTrue(op instanceof WorkflowOperationInstance);
    Assert.assertEquals("operation1", ((WorkflowOperationInstance) op).getTemplate());

    Object inc = map.get("incident");
    Assert.assertNotNull(inc);
    Assert.assertTrue(inc instanceof List);
    Assert.assertEquals(2, ((List) inc).size());
    Assert.assertTrue(((List) inc).contains(incident1));
    Assert.assertTrue(((List) inc).contains(incident2));
  }

}
