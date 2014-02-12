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
package org.opencastproject.gstreamer.service.impl;

import org.opencastproject.gstreamer.service.api.GStreamerLaunchException;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.easymock.EasyMock;
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


/**
 * Test class for GStreamer Launch
 */
public class GStreamerServiceImplTest {
  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(GStreamerServiceImplTest.class.getName());

  /** True if the environment provides the tools needed for the test suite */
  private static boolean gstreamerInstalled = true;

  private GStreamerServiceImpl gstreamerServiceImpl;

  private static Workspace workspace;

  private ServiceRegistry serviceRegistry;

  private MediaPackage mediaPackage;

  private URI badGStreamerURI;
  private URI movieFileURI;
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
  public void setUp() throws NotFoundException, IOException, URISyntaxException, GStreamerLaunchException,
          MediaPackageException, ServiceRegistryException {
    if (!gstreamerInstalled) {
      return;
    }
    badGStreamerURI = GStreamerServiceImplTest.class.getResource("/with.bad.gstreamer.properties").toURI();
    movieFileURI = GStreamerServiceImplTest.class.getResource("/av.mov").toURI();
    withGStreamerURI = GStreamerServiceImplTest.class.getResource("/with.gstreamer.properties").toURI();
    withoutGStreamerURI = GStreamerServiceImplTest.class.getResource("/without.gstreamer.properties").toURI();
    randomXMLURI = GStreamerServiceImplTest.class.getResource("/compose_encode_mediapackage.xml").toURI();

    Attachment binaryFile = AttachmentImpl.fromURI(movieFileURI);
    Attachment withoutGStreamerFile = AttachmentImpl.fromURI(withoutGStreamerURI);
    Attachment withGStreamer = AttachmentImpl.fromURI(withGStreamerURI);
    Attachment randomXML = AttachmentImpl.fromURI(randomXMLURI);

    Attachment[] attachments = { binaryFile, withoutGStreamerFile, randomXML, withGStreamer };

    Track movieTrack = TrackImpl.fromURI(movieFileURI);
    movieTrack.setIdentifier("track-1");
    Track[] tracks = { movieTrack };

    mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.clone()).andReturn(mediaPackage).anyTimes();
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

    Job job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getStatus()).andReturn(Status.FINISHED).anyTimes();
    EasyMock.expect(job.getQueueTime()).andReturn(10L).anyTimes();
    EasyMock.replay(job);

    serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job).anyTimes();
    EasyMock.replay(serviceRegistry);
  }

  @Test
  public void gstreamerBinaryLocationSetBeforeActivation() {
    if (!gstreamerInstalled) {
      return;
    }
    gstreamerServiceImpl = new GStreamerServiceImpl();
    Assert.assertEquals(GStreamerServiceImpl.DEFAULT_GSTREAMER_LOCATION, gstreamerServiceImpl.getGStreamerLocation());
  }

  @Test
  public void defaultGstreamerBinaryLocationSetAfterActivation() {
    if (!gstreamerInstalled) {
      return;
    }
    gstreamerServiceImpl = new GStreamerServiceImpl();
    gstreamerServiceImpl.activate(null);
    Assert.assertEquals(GStreamerServiceImpl.DEFAULT_GSTREAMER_LOCATION, gstreamerServiceImpl.getGStreamerLocation());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void nullGstreamerBinaryLocationDoesntBreak() {
    if (!gstreamerInstalled) {
      return;
    }
    ServiceRegistration mockServiceRegistration = EasyMock.createMock(ServiceRegistration.class);
    Properties properties = new Properties();

    BundleContext mockBundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(mockBundleContext.registerService((String) EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary) EasyMock.anyObject())).andReturn(mockServiceRegistration);
    EasyMock.expect(mockBundleContext.getProperty(GStreamerServiceImpl.CONFIG_GSTREAMER_LOCATION_KEY)).andReturn(null);
    EasyMock.replay(mockBundleContext);

    ComponentContext mockComponentContext = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(mockComponentContext.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(mockComponentContext.getBundleContext()).andReturn(mockBundleContext).anyTimes();
    EasyMock.replay(mockComponentContext);

    gstreamerServiceImpl = new GStreamerServiceImpl();
    gstreamerServiceImpl.activate(mockComponentContext);
    Assert.assertEquals(GStreamerServiceImpl.DEFAULT_GSTREAMER_LOCATION, gstreamerServiceImpl.getGStreamerLocation());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void emptyStringGstreamerBinaryLocationDoesntBreak() {
    if (!gstreamerInstalled) {
      return;
    }
    ServiceRegistration mockServiceRegistration = EasyMock.createMock(ServiceRegistration.class);
    Properties properties = new Properties();

    BundleContext mockBundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(mockBundleContext.registerService((String) EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary) EasyMock.anyObject())).andReturn(mockServiceRegistration);
    EasyMock.expect(mockBundleContext.getProperty(GStreamerServiceImpl.CONFIG_GSTREAMER_LOCATION_KEY)).andReturn("");
    EasyMock.replay(mockBundleContext);

    ComponentContext mockComponentContext = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(mockComponentContext.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(mockComponentContext.getBundleContext()).andReturn(mockBundleContext).anyTimes();
    EasyMock.replay(mockComponentContext);

    gstreamerServiceImpl = new GStreamerServiceImpl();
    gstreamerServiceImpl.activate(mockComponentContext);
    Assert.assertEquals(GStreamerServiceImpl.DEFAULT_GSTREAMER_LOCATION, gstreamerServiceImpl.getGStreamerLocation());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void gstreamerBinaryLocationCanBeSetInConfigProperties() {
    if (!gstreamerInstalled) {
      return;
    }
    String location = "/to/nowhere/gstreamer";
    ServiceRegistration mockServiceRegistration = EasyMock.createMock(ServiceRegistration.class);
    Properties properties = new Properties();

    BundleContext mockBundleContext = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(mockBundleContext.registerService((String) EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary) EasyMock.anyObject())).andReturn(mockServiceRegistration);
    EasyMock.expect(mockBundleContext.getProperty(GStreamerServiceImpl.CONFIG_GSTREAMER_LOCATION_KEY)).andReturn(location);
    EasyMock.replay(mockBundleContext);

    ComponentContext mockComponentContext = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(mockComponentContext.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(mockComponentContext.getBundleContext()).andReturn(mockBundleContext).anyTimes();
    EasyMock.replay(mockComponentContext);

    gstreamerServiceImpl = new GStreamerServiceImpl();
    gstreamerServiceImpl.activate(mockComponentContext);
    Assert.assertEquals(location, gstreamerServiceImpl.getGStreamerLocation());
  }

  @Test
  public void substitutionOfTrackGStreamerLineInProperties() throws NotFoundException, IOException {
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
    output = GStreamerServiceImpl.replaceTemplateValues(input, replacements);
    Assert.assertTrue("Input did not match output when nothing should has changed due to no substitutions. Output:"
            + output, input.equals(output));

    // test substitution of one, but nothing to replace.
    input = "filesrc location=target/test-classes/av.mov !  decodebin ! fakesink";
    replacements = new HashMap<String, String>();
    replacements.put("track-1", movieFileURI.toString());
    output = GStreamerServiceImpl.replaceTemplateValues(input, replacements);
    Assert.assertTrue(
            "Input did not match output when nothing should has changed with nothing in tempate but one substitution. Output:"
                    + output, input.equals(output));

    // test substitution of two, but nothing to replace.
    input = "filesrc location=target/test-classes/av.mov !  decodebin ! fakesink";
    replacements = new HashMap<String, String>();
    replacements.put("track-1", movieFileURI.toString());
    replacements.put("track-2", badGStreamerURI.toString());
    output = GStreamerServiceImpl.replaceTemplateValues(input, replacements);
    Assert.assertTrue(
            "Input did not match output when nothing should has changed with nothing in tempate but two substitutions. Output:"
                    + output, input.equals(output));

    // test substitution list two, but only one to replace.
    input = "filesrc location=${.vars[\"track-1\"]} !  decodebin ! fakesink";
    expected = "filesrc location=" + movieFileURI.toString() + " !  decodebin ! fakesink";
    replacements = new HashMap<String, String>();
    replacements.put("track-1", movieFileURI.toString());
    replacements.put("track-2", badGStreamerURI.toString());
    output = GStreamerServiceImpl.replaceTemplateValues(input, replacements);
    Assert.assertTrue(
            "Input did not match output when nothing should has changed with nothing in tempate but two substitutions. Output:"
                    + output, output.equals(expected));

    // test substitution list two, but only the second one to replace.
    input = "filesrc location=${.vars[\"track-2\"]} !  decodebin ! fakesink";
    expected = "filesrc location=" + badGStreamerURI.toString() + " !  decodebin ! fakesink";
    replacements = new HashMap<String, String>();
    replacements.put("track-1", movieFileURI.toString());
    replacements.put("track-2", badGStreamerURI.toString());
    output = GStreamerServiceImpl.replaceTemplateValues(input, replacements);
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
    output = GStreamerServiceImpl.replaceTemplateValues(input, replacements);
    Assert.assertTrue(
            "Input did not match output when nothing should has changed with nothing in tempate but two substitutions. Output:"
                    + output, output.equals(expected));
  }
}
