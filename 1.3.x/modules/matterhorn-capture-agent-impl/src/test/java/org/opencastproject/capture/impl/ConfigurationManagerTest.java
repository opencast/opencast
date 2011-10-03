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
package org.opencastproject.capture.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.opencastproject.capture.CaptureParameters;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Test the functionality of the ConfigurationManager
 */
public class ConfigurationManagerTest {

  /** the singleton object to test with */
  private ConfigurationManager configManager;

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationManagerTest.class);
  
  @Before
  public void setUp() throws ConfigurationException, IOException, URISyntaxException {
    configManager = new ConfigurationManager();
    Assert.assertNotNull(configManager);
    configManager.activate(null);

    // Checks on the basic operations before updated() has been called
    Assert.assertNull(configManager.getItem("nothing"));
    Assert.assertNull(configManager.getItem(null));
    configManager.setItem("anything", "nothing");
    Assert.assertEquals("nothing", configManager.getItem("anything"));
    configManager.setItem(null, "this should work, but not put anything in the props");
    Assert.assertEquals(1, configManager.getAllProperties().size());

    Properties p = new Properties();
    InputStream is = getClass().getResourceAsStream("/config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    p.load(is);
    IOUtils.closeQuietly(is);
    p.put("org.opencastproject.storage.dir",
            new File("./target", "configman-test").getAbsolutePath());

    configManager.updated(p);
    configManager.updated(null);
  }

  @After
  public void tearDown() {
    configManager.deactivate();
    configManager = null;
  }

  @Test
  public void testMerge() {
    // Setup the basic properties
    configManager.setItem("test", "foo");
    configManager.setItem("unchanged", "bar");
    Assert.assertEquals("foo", configManager.getItem("test"));
    Assert.assertEquals("bar", configManager.getItem("unchanged"));

    // Setup the additions
    Properties p = new Properties();
    p.setProperty("test", "value");

    // Do some idiot checks to make sure that trying to merge a null properties object does nothing
    Properties defaults = configManager.getAllProperties();
    configManager.merge(null, true);
    Assert.assertEquals(defaults, configManager.getAllProperties());
    configManager.merge(null, false);
    Assert.assertEquals(defaults, configManager.getAllProperties());

    // Now test a basic merge
    Properties t = configManager.merge(p, false);
    Assert.assertEquals("value", t.getProperty("test"));
    Assert.assertEquals("bar", t.getProperty("unchanged"));
    t = null;

    // Now overwrite the system settings
    t = configManager.merge(p, true);
    Assert.assertEquals("value", t.getProperty("test"));
    Assert.assertEquals("bar", t.getProperty("unchanged"));
    Assert.assertEquals("value", configManager.getItem("test"));
    Assert.assertEquals("bar", configManager.getItem("unchanged"));
  }

  @Test
  public void testGetAllProperties() {
    Properties properties;

    configManager.setItem("a", "1");
    configManager.setItem("b", "2");
    configManager.setItem("c", "3");

    properties = configManager.getAllProperties();
    Assert.assertEquals("1", properties.get("a"));
    Assert.assertEquals("2", properties.get("b"));
    Assert.assertEquals("3", properties.get("c"));
  }

  @Test
  public void testUpdate() throws IOException, ConfigurationException {
    Properties sourceProps = new Properties();
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    sourceProps.load(is);
    IOUtils.closeQuietly(is);

    configManager.setItem("org.opencastproject.storage.dir", new File("./target",
            "configman-test").getAbsolutePath());
    configManager.setItem("org.opencastproject.server.url", "http://localhost:8080");
    configManager.updated(sourceProps);

    Properties configProps = configManager.getAllProperties();
    for (Object key : sourceProps.stringPropertyNames()) {
      if (!configProps.containsKey(key)) {
        Assert.fail();
      }
    }
  }

  @Test
  public void testCapabilities() throws IOException, ConfigurationException, URISyntaxException {
    Properties sourceProps = new Properties();
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    sourceProps.load(is);
    IOUtils.closeQuietly(is);

    configManager.setItem("org.opencastproject.storage.dir", new File("./target",
            "configman-test").getAbsolutePath());
    configManager.setItem("org.opencastproject.server.url", "http://localhost:8080");
    configManager.setItem("M2_REPO", getClass().getClassLoader().getResource("m2_repo").toURI().getPath());
    configManager.updated(sourceProps);

    Properties caps = configManager.getCapabilities();
    Assert.assertNotNull(caps);
    assertCaps(caps, "MOCK_SCREEN", "M2_REPO", "org/opencastproject/samples/screen/1.0/screen-1.0.mpg",
            "screen_out.mpg", "presentation/source");
    assertCaps(caps, "MOCK_PRESENTER", "M2_REPO", "org/opencastproject/samples/camera/1.0/camera-1.0.mpg",
            "camera_out.mpg", "presentation/source");
    assertCaps(caps, "MOCK_MICROPHONE", "M2_REPO", "org/opencastproject/samples/audio/1.0/audio-1.0.mp3",
            "audio_out.mp3", "presentation/source");
  }

  private void assertCaps(Properties caps, String name, String baseVar, String relPath, String dest, String flavour) {
    String devBase = CaptureParameters.CAPTURE_DEVICE_PREFIX + name;
    String devSource = devBase + CaptureParameters.CAPTURE_DEVICE_SOURCE;
    Assert.assertEquals("${" + baseVar + "}" + "/" + relPath, configManager.getUninterpretedItem(devSource));
    Assert.assertTrue(new File(configManager.getItem(devSource)).exists());
    Assert.assertEquals(configManager.getVariable(baseVar) + "/" + relPath, caps.get(devSource));
    Assert.assertEquals(dest, caps.get(devBase + CaptureParameters.CAPTURE_DEVICE_DEST));
    Assert.assertEquals(flavour, caps.get(devBase + CaptureParameters.CAPTURE_DEVICE_FLAVOR));
  }

  @Test
  public void testBrokenCapabilities() throws IOException, ConfigurationException {
    Properties sourceProps = new Properties();
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    sourceProps.load(is);
    IOUtils.closeQuietly(is);

    sourceProps.remove("capture.device.MOCK_PRESENTER.src");
    sourceProps.remove("capture.device.MOCK_PRESENTER.outputfile");
    configManager.setItem("capture.device.MOCK_PRESENTER.src", null);
    configManager.setItem("capture.device.MOCK_PRESENTER.outputfile", null);
    configManager.setItem("org.opencastproject.storage.dir", new File("./target",
            "configman-test").getAbsolutePath());
    configManager.setItem("org.opencastproject.server.url", "http://localhost:8080");
    configManager.updated(sourceProps);

    Assert.assertNull(configManager.getCapabilities());
  }

  @Test
  @Ignore
  public void testFileWrite() throws IOException, ConfigurationException {
    XProperties sourceProps = new XProperties();
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    sourceProps.load(is);
    IOUtils.closeQuietly(is);

    // Add in two missing props
    sourceProps.put("anything", "nothing");
    sourceProps.put("org.opencastproject.storage.dir", "${java.io.tmpdir}/configman-test");
    sourceProps.put("M2_REPO", getClass().getClassLoader().getResource("m2_repo").getFile());
    sourceProps.put("org.opencastproject.server.url", "http://localhost:8080");
    File tempfile = File.createTempFile("temp", "file",
            new File(sourceProps.getProperty("org.opencastproject.storage.dir")));

    configManager.updated(sourceProps);
    configManager.writeConfigFileToDisk();

    FileOutputStream out = new FileOutputStream(tempfile);
    sourceProps.store(out, "");
    is = new FileInputStream(tempfile);
    sourceProps.load(is);

    XProperties testProps = new XProperties();
    InputStream testInput = new FileInputStream(configManager.getItem(CaptureParameters.CAPTURE_CONFIG_CACHE_URL));
    testProps.load(testInput);
    IOUtils.closeQuietly(testInput);

    if (!testProps.equals(sourceProps)) {
      for (Object e : sourceProps.keySet()) {
        String key = (String) e;
        if (testProps.getProperty(key) != null && !testProps.getProperty(key).equals(sourceProps.getProperty(key))) {
          System.out.println("testProps differs: " + key + " => " + sourceProps.getProperty(key) + " != "
                  + testProps.getProperty(key));
        } else if (testProps.getProperty(key) == null) {
          System.out.println("testProps missing: " + key);
        }
      }

      for (Object e : testProps.keySet()) {
        String key = (String) e;
        if (sourceProps.getProperty(key) == null) {
          System.out.println("sourceProps missing: " + key);
        }
      }

      Assert.fail();
    }
  }

  @Test
  public void configurationManagerNotifiesListenersCorrectly() throws ConfigurationException, InterruptedException {
    ConfigurationManager configurationManager = new ConfigurationManager();
    // Setup a listener to be registered before the configuration manager update.
    ConfigurationManagerListener registersBefore = createMock(ConfigurationManagerListener.class);
    registersBefore.refresh();
    replay(registersBefore);
    // Setup a listener to be registered after the configuration manager update.
    ConfigurationManagerListener registersAfter = createMock(ConfigurationManagerListener.class);
    registersAfter.refresh();
    replay(registersAfter);
    configurationManager.registerListener(registersBefore);
    configurationManager.updated(new XProperties());
    Thread.sleep(100);
    // A listener registered before an update should be refreshed as soon as the ConfigurationManager is updated.
    verify(registersBefore);
    configurationManager.registerListener(registersAfter);
    // A listener registered after the update should be refreshed as soon as it is registered.
    Thread.sleep(100);
    verify(registersAfter);
  }
  
  @Test
  public void testPropertiesAreStripped() {
    configManager = new ConfigurationManager();
    Properties properties = new Properties();
    String blanks = "blanks";
    String leadingAndTrailing = " spaces ";
    String withoutLeadingAndTrailing = "spaces";
    properties.put(blanks, "                   ");
    properties.put(withoutLeadingAndTrailing, leadingAndTrailing);
    try {
      configManager.updated(properties);
    } catch (ConfigurationException e) {
      logger.error(e.getMessage());
      Assert.fail("ConfigurationException cccured before the test could complete. ");
    }
    
    Properties returnedProperties = configManager.getAllProperties();
    Assert.assertTrue("Configuration Manager doesn't trim blank entries properly. ",
            returnedProperties.getProperty(blanks).equalsIgnoreCase(""));
    Assert.assertTrue("Configuration Manager doesn't trim ntries with spaces properly. ", returnedProperties
            .getProperty(withoutLeadingAndTrailing).equalsIgnoreCase(withoutLeadingAndTrailing));
  }
  
}
