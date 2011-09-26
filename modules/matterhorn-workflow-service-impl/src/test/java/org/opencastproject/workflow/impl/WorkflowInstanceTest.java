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
package org.opencastproject.workflow.impl;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowParser;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class WorkflowInstanceTest {
  @Test
  public void testWorkflowWithoutOperations() throws Exception {
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    workflow.setMediaPackage(mp);
    Assert.assertEquals(mp.getIdentifier(), workflow.getMediaPackage().getIdentifier());
  }

  @Test
  public void testWorkflowFields() throws Exception {
    // Workflows should obtain information from the definition used in the constructor
    WorkflowDefinitionImpl def = new WorkflowDefinitionImpl();
    def.setId("123");
    def.setPublished(true);

    Map<String, String> props = new HashMap<String, String>();
    props.put("key1", "value1");
    WorkflowInstance instance = new WorkflowInstanceImpl(def, null, null, null, props);
    Assert.assertEquals(def.getId(), instance.getTemplate());
    Assert.assertEquals("value1", instance.getConfiguration("key1"));
    def.setTitle("a title");
    instance = new WorkflowInstanceImpl(def, null, null, null, null);
    Assert.assertEquals(def.getTitle(), instance.getTitle());
  }

  @Test
  public void testMediaPackageSerializationInWorkflowInstance() throws Exception {
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    MediaPackage src = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    Track track = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromURI(new URI("http://sample"), Track.TYPE, MediaPackageElements.PRESENTER_SOURCE);
    src.add(track);
    MediaPackage deserialized = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(MediaPackageParser.getAsXml(src));
    workflow.setMediaPackage(deserialized);
    Assert.assertEquals(1, workflow.getMediaPackage().getTracks().length);
  }

  @Test
  public void testMediaPackageDeserialization() throws Exception {
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    String xml = "<ns2:mediapackage xmlns:ns2=\"http://mediapackage.opencastproject.org\" start=\"2007-12-05T13:40:00\" duration=\"1004400000\"><media><track id=\"track-1\" type=\"presenter/source\"><mimetype>audio/mp3</mimetype><url>http://localhost:8080/workflow/samples/audio.mp3</url><checksum type=\"md5\">950f9fa49caa8f1c5bbc36892f6fd062</checksum><duration>10472</duration><audio><channels>2</channels><bitdepth>0</bitdepth><bitrate>128004.0</bitrate><samplingrate>44100</samplingrate></audio></track><track id=\"track-2\" type=\"presenter/source\"><mimetype>video/quicktime</mimetype><url>http://localhost:8080/workflow/samples/camera.mpg</url><checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum><duration>14546</duration><video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" /><encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution><scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video></track></media><metadata><catalog id=\"catalog-1\" type=\"dublincore/episode\"><mimetype>text/xml</mimetype><url>http://localhost:8080/workflow/samples/dc-1.xml</url><checksum type=\"md5\">20e466615251074e127a1627fd0dae3e</checksum></catalog></metadata></ns2:mediapackage>";
    MediaPackage src = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(xml);
    workflow.setMediaPackage(src);
    Assert.assertEquals(2, workflow.getMediaPackage().getTracks().length);
  }

  @Test
  public void testWorkflowDefinitionDeserialization() throws Exception {
    InputStream in = getClass().getResourceAsStream("/workflow-definition-1.xml");
    WorkflowDefinition def = WorkflowParser.parseWorkflowDefinition(in);
    IOUtils.closeQuietly(in);
    Assert.assertEquals("The First Workflow Definition", def.getTitle());
    Assert.assertEquals(2, def.getOperations().size());
    Assert.assertEquals("definition-1", def.getId());
    Assert.assertEquals("Unit testing workflow", def.getDescription());
    Assert.assertTrue(def.isPublished());
  }

  @Test
  public void testFlavorMarshalling() throws Exception {
    URI uri = new URI("http://testing");
    Track track = TrackImpl.fromURI(uri);
    track.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.add(track);

    WorkflowInstance workflow = new WorkflowInstanceImpl();
    workflow.setMediaPackage(mp);

    // Marshall the workflow to xml
    String xml = WorkflowParser.toXml(workflow);

    // Get it back from xml
    WorkflowInstance instance2 = WorkflowParser.parseWorkflowInstance(xml);
    Assert.assertEquals(workflow.getMediaPackage().getTracks()[0].getFlavor(),
            instance2.getMediaPackage().getTracks()[0].getFlavor());

    // now without namespaces
    String noNamespaceXml = "<workflow><parent/><mediapackage><media><track type=\"presentation/source\" id=\"track-1\"><url>http://testing</url></track></media></mediapackage></workflow>";
    WorkflowInstance instance3 = WorkflowParser.parseWorkflowInstance(noNamespaceXml);
    Assert.assertEquals(workflow.getMediaPackage().getTracks()[0].getFlavor(),
            instance3.getMediaPackage().getTracks()[0].getFlavor());
  }
}
