/**
 * Copyright 2009, 2010 The Regents of the University of California Licensed
 * under the Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.engage.theodul.manager.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class StaticResourceClassloaderTest {

  final static String OVERRIDE_DIR_NAME = "theodul.override.test";
  final static String RESOURCE_FILE_NAME = "test.resource";
  final static String GENUINE_RESOURCE_NAME = "other.resource";
  final static String BUNDLE_PATH = "ui/";
  final static String BUNDLE_URL_PREFIX = "http://localhost/";
  final static String OVERRIDDEN_BUNDLE_RESOURCE = BUNDLE_PATH + RESOURCE_FILE_NAME;
  final static String OVERRIDEN_RESOURCE_URL = BUNDLE_URL_PREFIX + BUNDLE_PATH + RESOURCE_FILE_NAME;
  final static String GENUINE_BUNDLE_RESOURCE = BUNDLE_PATH + GENUINE_RESOURCE_NAME;
  final static String GENUINE_RESOURCE_URL = BUNDLE_URL_PREFIX + BUNDLE_PATH + GENUINE_RESOURCE_NAME;

  static File overrideDir;
  static File overrideResource;

  @BeforeClass
  public static void setUp() throws Exception {
    // create override directory and resource
    try {
      String tempPath = System.getProperty("java.io.tmpdir");
      overrideResource = new File(tempPath + File.separator + OVERRIDE_DIR_NAME + File.separator + RESOURCE_FILE_NAME);
      overrideDir = overrideResource.getParentFile();
      overrideDir.mkdir();
      System.out.println("Creating " + overrideResource.getAbsolutePath());
      if (overrideResource.createNewFile()) {
        // write something to ensure file existence
        PrintWriter writer = new PrintWriter(overrideResource);
        writer.print("Override Test Resource");
        writer.flush();
        writer.close();
      } else {
        throw new IOException("Failed to create " + overrideResource.getAbsolutePath());
      }
    } catch (IOException ex) {
      throw new RuntimeException("Failed to properly set up override direcory. ", ex);
    }
  }

  @Test
  public void testBundleResourceLoading() throws Exception {
    StaticResourceClassloader klas = new StaticResourceClassloader(new MockBundle(), overrideDir, BUNDLE_PATH);
    URL result = klas.getResource(GENUINE_BUNDLE_RESOURCE);
    Assert.assertNotNull(result);
    Assert.assertEquals(new URL(GENUINE_RESOURCE_URL), result);
  }

  @Test
  public void testFielesystemResourceOverriding() throws Exception {
    StaticResourceClassloader klas = new StaticResourceClassloader(new MockBundle(), overrideDir, BUNDLE_PATH);
    URL result = klas.getResource(OVERRIDDEN_BUNDLE_RESOURCE);
    Assert.assertNotNull(result);
    Assert.assertEquals(overrideResource.toURI().toURL(), result);
  }

  @Test
  public void testMaliciousPath() {
    // test if absolut paths are not working
    StaticResourceClassloader klas = new StaticResourceClassloader(new MockBundle(), overrideDir, BUNDLE_PATH);
    URL result = klas.getResource("/etc/hostname");
    Assert.assertNull(result);

    // test if relative ascending paths are not working
    result = klas.getResource("../../etc/hostname");
    Assert.assertNull(result);
  }
  
  @AfterClass
  public static void cleanUp() throws Exception {
    overrideResource.delete();
    overrideDir.delete();
  }

  class MockBundle implements Bundle {

    @Override
    public String getSymbolicName() {
      return MockBundle.class.getSimpleName();
    }

    @Override
    public URL getResource(String path) {
      try {
        if (path.endsWith(GENUINE_BUNDLE_RESOURCE)) {
          return new URL(GENUINE_RESOURCE_URL);
        } else if (path.endsWith(OVERRIDDEN_BUNDLE_RESOURCE)) {
          return new URL(OVERRIDEN_RESOURCE_URL);
        }
      } catch (MalformedURLException ex) {
        throw new RuntimeException("Could not instantiate URL object.", ex);
      }
      return null;
    }

//<editor-fold defaultstate="collapsed" desc="Unused methods from Bundle interface.">
    @Override
    public int getState() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void start(int i) throws BundleException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void start() throws BundleException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void stop(int i) throws BundleException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void stop() throws BundleException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update(InputStream in) throws BundleException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update() throws BundleException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void uninstall() throws BundleException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Dictionary getHeaders() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getBundleId() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLocation() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ServiceReference[] getRegisteredServices() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ServiceReference[] getServicesInUse() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasPermission(Object o) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Dictionary getHeaders(String string) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Class loadClass(String string) throws ClassNotFoundException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Enumeration getResources(String string) throws IOException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Enumeration getEntryPaths(String string) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public URL getEntry(String string) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastModified() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Enumeration findEntries(String string, String string1, boolean bln) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BundleContext getBundleContext() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map getSignerCertificates(int i) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Version getVersion() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

//</editor-fold>
  }
}
