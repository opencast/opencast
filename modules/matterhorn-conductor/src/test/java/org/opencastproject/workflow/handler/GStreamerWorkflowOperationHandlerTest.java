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

package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.JaxbJobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.easymock.classextension.EasyMock;
import org.gstreamer.Gst;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Properties;

public class GStreamerWorkflowOperationHandlerTest {

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(GStreamerWorkflowOperationHandlerTest.class.getName());

  /** True if the environment provides the tools needed for the test suite */
  private static boolean gstreamerInstalled = true;

  /** The temp directory */
  private final File tmpDir = new File(System.getProperty("java.io.tmpdir"));

  private static Workspace workspace;

  private GStreamerWorkflowOperationHandler gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();

  private MediaPackage mediaPackage;

  private URI badGStreamerURI;
  private URI movieFileURI;
  private URI complicatedFileURI;
  private URI withoutGStreamerURI;
  private URI withGStreamerURI;
  private URI randomXMLURI;
  
  
  /** Make sure that gstreamer is installed and available. **/
  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping gstreamer related tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }

  @Before
  public void setUp() throws NotFoundException, IOException, URISyntaxException {
     if (!gstreamerInstalled) {
       return;
    }
    badGStreamerURI = GStreamerWorkflowOperationHandlerTest.class.getResource("/with.bad.gstreamer.properties").toURI();
    movieFileURI = GStreamerWorkflowOperationHandlerTest.class.getResource("/av.mov").toURI();
    complicatedFileURI = GStreamerWorkflowOperationHandlerTest.class.getResource(
            "/with.complicated.gstreamer.properties").toURI();
    withGStreamerURI = GStreamerWorkflowOperationHandlerTest.class.getResource("/with.gstreamer.properties").toURI();
    withoutGStreamerURI = GStreamerWorkflowOperationHandlerTest.class.getResource("/without.gstreamer.properties")
            .toURI();
    randomXMLURI = GStreamerWorkflowOperationHandlerTest.class.getResource("/compose_encode_mediapackage.xml").toURI();

    Attachment binaryFile = AttachmentImpl.fromURI(movieFileURI);
    Attachment withoutGStreamerFile = AttachmentImpl.fromURI(withoutGStreamerURI);
    Attachment withGStreamer = AttachmentImpl.fromURI(withGStreamerURI);
    Attachment randomXML = AttachmentImpl.fromURI(randomXMLURI);

    Attachment[] attachments = { binaryFile, withoutGStreamerFile, randomXML, withGStreamer };

    Track movieTrack = TrackImpl.fromURI(movieFileURI);
    movieTrack.setIdentifier("track-1");
    Track[] tracks = { movieTrack };

    mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getAttachments()).andReturn(attachments);
    EasyMock.expect(mediaPackage.getTracks()).andReturn(tracks).anyTimes();
    EasyMock.replay(mediaPackage);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(movieFileURI)).andReturn(new File(movieFileURI));
    EasyMock.expect(workspace.get(withoutGStreamerURI)).andReturn(new File(withoutGStreamerURI));
    EasyMock.expect(workspace.get(withGStreamerURI)).andReturn(new File(withGStreamerURI));
    EasyMock.expect(workspace.get(randomXMLURI)).andReturn(new File(randomXMLURI));
    EasyMock.expect(workspace.get(movieFileURI)).andReturn(new File(movieFileURI)).anyTimes();
    EasyMock.expect(workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (InputStream) EasyMock.anyObject())).andReturn(movieFileURI);
    EasyMock.replay(workspace);

  }

  /**
   * Test it correctly processes attachments
   * 
   * @throws Exception
   */

  @Test
  public void testAttachmentProcessing() throws Exception {
    if (!gstreamerInstalled) {
       return; 
    }
    gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();
    gstreamerWorkflowOperationHandler.setWorkspace(workspace);

    WorkflowInstance workflowInstance = new WorkflowInstanceImpl();
    JaxbJobContext jobContext = new JaxbJobContext();
    workflowInstance.setMediaPackage(mediaPackage);
    gstreamerWorkflowOperationHandler.resume(workflowInstance, jobContext, null);
  }

  @Test
  public void noGStreamerLineInProperties() throws NotFoundException, IOException {
     if (!gstreamerInstalled) {
       return;
    }
    Attachment binaryFile = AttachmentImpl.fromURI(movieFileURI);
    Attachment withoutGStreamerFile = AttachmentImpl.fromURI(withoutGStreamerURI);
    Attachment randomXML = AttachmentImpl.fromURI(randomXMLURI);

    Attachment[] attachments = { binaryFile, withoutGStreamerFile, randomXML };

    mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getAttachments()).andReturn(attachments);
    EasyMock.replay(mediaPackage);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(movieFileURI)).andReturn(new File(movieFileURI));
    EasyMock.expect(workspace.get(withoutGStreamerURI)).andReturn(new File(withoutGStreamerURI));
    EasyMock.expect(workspace.get(randomXMLURI)).andReturn(new File(randomXMLURI));
    EasyMock.replay(workspace);

    gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();
    gstreamerWorkflowOperationHandler.setWorkspace(workspace);

    WorkflowInstance workflowInstance = new WorkflowInstanceImpl();
    JaxbJobContext jobContext = new JaxbJobContext();
    workflowInstance.setMediaPackage(mediaPackage);
    try {
      gstreamerWorkflowOperationHandler.resume(workflowInstance, jobContext, null);
      Assert.fail();
    } catch (WorkflowOperationException e) {

    }
  }

  @Test
  public void badGStreamerLineInProperties() throws NotFoundException, IOException {
     if (!gstreamerInstalled) {
       return;
    }
    Attachment badGStreamerFile = AttachmentImpl.fromURI(badGStreamerURI);
    Attachment[] attachments = { badGStreamerFile };
    Track movieTrack = TrackImpl.fromURI(movieFileURI);
    movieTrack.setIdentifier("track-1");
    Track[] tracks = { movieTrack };
    mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getAttachments()).andReturn(attachments);
    EasyMock.expect(mediaPackage.getTracks()).andReturn(tracks).anyTimes();
    EasyMock.replay(mediaPackage);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(badGStreamerURI)).andReturn(new File(badGStreamerURI));
    EasyMock.expect(workspace.get(movieFileURI)).andReturn(new File(movieFileURI)).anyTimes();
    EasyMock.replay(workspace);

    gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();
    gstreamerWorkflowOperationHandler.setWorkspace(workspace);

    WorkflowInstance workflowInstance = new WorkflowInstanceImpl();
    JaxbJobContext jobContext = new JaxbJobContext();
    workflowInstance.setMediaPackage(mediaPackage);
    try {
      gstreamerWorkflowOperationHandler.resume(workflowInstance, jobContext, null);
      Assert.fail();
    } catch (WorkflowOperationException e) {

    }
  } 

  @Test
  public void noTracksAvailable() throws NotFoundException, IOException {
     if (!gstreamerInstalled) {
       return;
    }
    Attachment withGStreamer = AttachmentImpl.fromURI(withGStreamerURI);
    Attachment[] attachments = { withGStreamer };

    mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getAttachments()).andReturn(attachments);
    EasyMock.replay(mediaPackage);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(withGStreamerURI)).andReturn(new File(withGStreamerURI));
    EasyMock.replay(workspace);

    gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();
    gstreamerWorkflowOperationHandler.setWorkspace(workspace);

    WorkflowInstance workflowInstance = new WorkflowInstanceImpl();
    JaxbJobContext jobContext = new JaxbJobContext();
    workflowInstance.setMediaPackage(mediaPackage);
    try {
      gstreamerWorkflowOperationHandler.resume(workflowInstance, jobContext, null);
      Assert.fail();
    } catch (WorkflowOperationException e) {

    }
  }

  @Test
  public void complicatedGStreamerLineInProperties() throws NotFoundException, IOException, WorkflowOperationException {
    if (!gstreamerInstalled) {
       return;
    }
    Attachment complicatedGStreamerFile = AttachmentImpl.fromURI(complicatedFileURI);
    Attachment[] attachments = { complicatedGStreamerFile };
    Track movieTrack = TrackImpl.fromURI(movieFileURI);
    movieTrack.setIdentifier("track-1");
    Track[] tracks = { movieTrack };
    mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getAttachments()).andReturn(attachments);
    EasyMock.expect(mediaPackage.getTracks()).andReturn(tracks).anyTimes();
    EasyMock.replay(mediaPackage);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(complicatedFileURI)).andReturn(new File(complicatedFileURI));
    EasyMock.expect(workspace.get(movieFileURI)).andReturn(new File(movieFileURI)).anyTimes();
    EasyMock.replay(workspace);

    gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();
    gstreamerWorkflowOperationHandler.setWorkspace(workspace);

    WorkflowInstance workflowInstance = new WorkflowInstanceImpl();
    JaxbJobContext jobContext = new JaxbJobContext();
    workflowInstance.setMediaPackage(mediaPackage);

    gstreamerWorkflowOperationHandler.resume(workflowInstance, jobContext, null);

  }

  @Test
  public void substitutionOfTrackGStreamerLineInProperties() throws NotFoundException, IOException,
          WorkflowOperationException {
    if (!gstreamerInstalled) {
       return;
    }
    HashMap<String, String> replacements;
    String input;
    String output;
    String expected;
    // test no substitutions
    input = "filesrc location=target/test-classes/av.mov !  decodebin ! fakesink";
    replacements = new HashMap<String, String>();
    output = GStreamerWorkflowOperationHandler.replaceTemplateValues(input, replacements);
    Assert.assertTrue("Input did not match output when nothing should has changed due to no substitutions. Output:"
            + output, input.equals(output));

    // test substitution of one, but nothing to replace.
    input = "filesrc location=target/test-classes/av.mov !  decodebin ! fakesink";
    replacements = new HashMap<String, String>();
    replacements.put("track-1", movieFileURI.toString());
    output = GStreamerWorkflowOperationHandler.replaceTemplateValues(input, replacements);
    Assert.assertTrue(
            "Input did not match output when nothing should has changed with nothing in tempate but one substitution. Output:"
                    + output, input.equals(output));

    // test substitution of two, but nothing to replace.
    input = "filesrc location=target/test-classes/av.mov !  decodebin ! fakesink";
    replacements = new HashMap<String, String>();
    replacements.put("track-1", movieFileURI.toString());
    replacements.put("track-2", badGStreamerURI.toString());
    output = GStreamerWorkflowOperationHandler.replaceTemplateValues(input, replacements);
    Assert.assertTrue(
            "Input did not match output when nothing should has changed with nothing in tempate but two substitutions. Output:"
                    + output, input.equals(output));

    // test substitution list two, but only one to replace.
    input = "filesrc location=${.vars[\"track-1\"]} !  decodebin ! fakesink";
    expected = "filesrc location=" + movieFileURI.toString() + " !  decodebin ! fakesink";
    replacements = new HashMap<String, String>();
    replacements.put("track-1", movieFileURI.toString());
    replacements.put("track-2", badGStreamerURI.toString());
    output = GStreamerWorkflowOperationHandler.replaceTemplateValues(input, replacements);
    Assert.assertTrue(
            "Input did not match output when nothing should has changed with nothing in tempate but two substitutions. Output:"
                    + output, output.equals(expected));

    // test substitution list two, but only the second one to replace.
    input = "filesrc location=${.vars[\"track-2\"]} !  decodebin ! fakesink";
    expected = "filesrc location=" + badGStreamerURI.toString() + " !  decodebin ! fakesink";
    replacements = new HashMap<String, String>();
    replacements.put("track-1", movieFileURI.toString());
    replacements.put("track-2", badGStreamerURI.toString());
    output = GStreamerWorkflowOperationHandler.replaceTemplateValues(input, replacements);
    Assert.assertTrue(
            "Input did not match output when nothing should has changed with nothing in tempate but two substitutions. Output:"
                    + output, output.equals(expected));

    // test substitution list two, but with two to replace.
    input = "filesrc location=${.vars[\"track-1\"]} !  decodebin ! filesink location=${.vars[\"track-2\"]}";
    expected = "filesrc location=" + movieFileURI.toString() + " !  decodebin ! filesink location="
            + badGStreamerURI.toString();
    replacements = new HashMap<String, String>();
    replacements.put("track-1", movieFileURI.toString());
    replacements.put("track-2", badGStreamerURI.toString());
    output = GStreamerWorkflowOperationHandler.replaceTemplateValues(input, replacements);
    Assert.assertTrue(
            "Input did not match output when nothing should has changed with nothing in tempate but two substitutions. Output:"
                    + output, output.equals(expected));
  }
  
  @Test
  public void gstreamerBinaryLocationSetBeforeActivation() {
    gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();
    Assert.assertEquals(GStreamerWorkflowOperationHandler.DEFAULT_GSTREAMER_LOCATION, gstreamerWorkflowOperationHandler.getGStreamerLocation());
  }
  
  @Test
  public void defaultGstreamerBinaryLocationSetAfterActivation() {
    gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();
    gstreamerWorkflowOperationHandler.activate(null);
    Assert.assertEquals(GStreamerWorkflowOperationHandler.DEFAULT_GSTREAMER_LOCATION, gstreamerWorkflowOperationHandler.getGStreamerLocation());
  }
  
  @Test
  public void nullGstreamerBinaryLocationDoesntBreak() {
    ServiceRegistration mockServiceRegistration = EasyMock.createMock(ServiceRegistration.class);
    Properties properties = new Properties();
    
    BundleContext mockBundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(mockBundleContext.registerService((String) EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary) EasyMock.anyObject())).andReturn(mockServiceRegistration);
    EasyMock.expect(mockBundleContext.getProperty(GStreamerWorkflowOperationHandler.CONFIG_GSTREAMER_LOCATION_KEY)).andReturn(null);
    EasyMock.replay(mockBundleContext);
    
    ComponentContext mockComponentContext = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(mockComponentContext.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(mockComponentContext.getBundleContext()).andReturn(mockBundleContext).anyTimes();
    EasyMock.replay(mockComponentContext);
    
    gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();
    gstreamerWorkflowOperationHandler.activate(mockComponentContext);
    Assert.assertEquals(GStreamerWorkflowOperationHandler.DEFAULT_GSTREAMER_LOCATION, gstreamerWorkflowOperationHandler.getGStreamerLocation());
  }
  
  @Test
  public void emptyStringGstreamerBinaryLocationDoesntBreak() {
    ServiceRegistration mockServiceRegistration = EasyMock.createMock(ServiceRegistration.class);
    Properties properties = new Properties();
    
    BundleContext mockBundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(mockBundleContext.registerService((String) EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary) EasyMock.anyObject())).andReturn(mockServiceRegistration);
    EasyMock.expect(mockBundleContext.getProperty(GStreamerWorkflowOperationHandler.CONFIG_GSTREAMER_LOCATION_KEY)).andReturn("");
    EasyMock.replay(mockBundleContext);
    
    ComponentContext mockComponentContext = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(mockComponentContext.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(mockComponentContext.getBundleContext()).andReturn(mockBundleContext).anyTimes();
    EasyMock.replay(mockComponentContext);
    
    gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();
    gstreamerWorkflowOperationHandler.activate(mockComponentContext);
    Assert.assertEquals(GStreamerWorkflowOperationHandler.DEFAULT_GSTREAMER_LOCATION, gstreamerWorkflowOperationHandler.getGStreamerLocation());
  }
  
  @Test
  public void gstreamerBinaryLocationCanBeSetInConfigProperties() {
    String location = "/to/nowhere/gstreamer";
    ServiceRegistration mockServiceRegistration = EasyMock.createMock(ServiceRegistration.class);
    Properties properties = new Properties();
    
    BundleContext mockBundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(mockBundleContext.registerService((String) EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary) EasyMock.anyObject())).andReturn(mockServiceRegistration);
    EasyMock.expect(mockBundleContext.getProperty(GStreamerWorkflowOperationHandler.CONFIG_GSTREAMER_LOCATION_KEY)).andReturn(location);
    EasyMock.replay(mockBundleContext);
    
    ComponentContext mockComponentContext = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(mockComponentContext.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(mockComponentContext.getBundleContext()).andReturn(mockBundleContext).anyTimes();
    EasyMock.replay(mockComponentContext);
    
    gstreamerWorkflowOperationHandler = new GStreamerWorkflowOperationHandler();
    gstreamerWorkflowOperationHandler.activate(mockComponentContext);
    Assert.assertEquals(location, gstreamerWorkflowOperationHandler.getGStreamerLocation());
  }
}
