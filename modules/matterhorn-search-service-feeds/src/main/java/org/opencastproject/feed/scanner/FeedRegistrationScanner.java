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
package org.opencastproject.feed.scanner;

import static org.opencastproject.util.ReadinessIndicator.ARTIFACT;

import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.util.ReadinessIndicator;

import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/**
 * Installs feeds matching "*.properties" in the feeds watch directory.
 */
public class FeedRegistrationScanner implements ArtifactInstaller {
  public static final String FEED_CLASS = "feed.class";
  public static final String FEED_URI = "feed.uri";
  public static final String FEED_SELECTOR = "feed.selector";
  public static final String FEED_ENTRY = "feed.entry";

  private static final Logger logger = LoggerFactory.getLogger(FeedRegistrationScanner.class);

  /** A map to keep track of each feed registration file and feed generator it produces */
  protected Map<File, ServiceRegistration> generators = new HashMap<File, ServiceRegistration>();

  /** The search service to use in each feed generator */
  protected SearchService searchService;

  /** The bundle context for this osgi component */
  protected BundleContext bundleContext;

  /** Sum of profiles files currently installed */
  private int sumInstalledFiles = 0;

  /** Sets the search service */
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * Activates the component
   * 
   * @param cc
   *          the component's context
   */
  protected void activate(ComponentContext cc) {
    this.bundleContext = cc.getBundleContext();
  }

  /**
   * Deactivates the component
   */
  protected void deactivate() {
    this.bundleContext = null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return "feeds".equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".properties");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  @Override
  public void install(File artifact) throws Exception {
    logger.info("Installing a feed from {}", artifact.getAbsolutePath());
    Properties props = new Properties();
    FileInputStream in = null;
    try {
      in = new FileInputStream(artifact);
      props.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
    // Always include the server URL obtained from the bundle context
    props.put("org.opencastproject.server.url", bundleContext.getProperty("org.opencastproject.server.url"));
    Class<?> clazz = getClass().getClassLoader().loadClass(props.getProperty(FEED_CLASS));
    FeedGenerator generator = (FeedGenerator) clazz.newInstance();
    generator.setSearchService(searchService);
    generator.initialize(props);
    ServiceRegistration reg = bundleContext.registerService(FeedGenerator.class.getName(), generator, null);
    generators.put(artifact, reg);
    sumInstalledFiles++;

    // Determine the number of available profiles
    String[] filesInDirectory = artifact.getParentFile().list(new FilenameFilter() {
      public boolean accept(File arg0, String name) {
        return name.endsWith(".properties");
      }
    });

    // Once all profiles have been loaded, announce readiness
    if (filesInDirectory.length == sumInstalledFiles) {
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(ARTIFACT, "feed");
      logger.debug("Indicating readiness of feed");
      bundleContext.registerService(ReadinessIndicator.class.getName(), new ReadinessIndicator(), properties);
      logger.info("All {} feeds installed", filesInDirectory.length);
    } else {
      logger.debug("{} of {} feeds installed", sumInstalledFiles, filesInDirectory.length);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  @Override
  public void uninstall(File artifact) throws Exception {
    ServiceRegistration reg = generators.get(artifact);
    if (reg != null) {
      reg.unregister();
      generators.remove(artifact);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  @Override
  public void update(File artifact) throws Exception {
    uninstall(artifact);
    install(artifact);
  }
}
