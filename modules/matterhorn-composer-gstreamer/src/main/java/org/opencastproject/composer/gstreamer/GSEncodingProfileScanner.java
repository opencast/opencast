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
package org.opencastproject.composer.gstreamer;

import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.util.ConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class GSEncodingProfileScanner implements ArtifactInstaller {

  /** Prefix for encoding profile property keys **/
  private static final String PROP_PREFIX = "profile.";

  /* Property names */
  private static final String PROP_NAME = ".name";
  private static final String PROP_APPLICABLE = ".input";
  private static final String PROP_OUTPUT = ".output";
  private static final String PROP_SUFFIX = ".suffix";
  private static final String PROP_MIMETYPE = ".mimetype";

  /** The profiles map */
  private Map<String, EncodingProfile> profiles = new HashMap<String, EncodingProfile>();

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(GSEncodingProfileScanner.class);

  /*
   * (non-Javadoc)
   *
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return "gsencoding".equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".properties");
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  @Override
  public void install(File artifact) throws Exception {
    logger.info("Registering encoding profiles from {}", artifact);
    try {
      Map<String, EncodingProfile> profileMap = loadFromProperties(artifact);
      for (Entry<String, EncodingProfile> entry : profileMap.entrySet()) {
        logger.info("Installed profile {}", entry.getValue().getIdentifier());
        profiles.put(entry.getKey(), entry.getValue());
      }
    } catch (Exception e) {
      logger.error("Encoding profiles could not be read from " + artifact, e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  @Override
  public void update(File artifact) throws Exception {
    uninstall(artifact);
    install(artifact);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  @Override
  public void uninstall(File artifact) throws Exception {
    for (Iterator<EncodingProfile> iter = profiles.values().iterator(); iter.hasNext();) {
      EncodingProfile profile = iter.next();
      if (artifact.equals(profile.getSource())) {
        logger.info("Uninstalling profile {}", profile.getIdentifier());
        iter.remove();
      }
    }
  }

  /**
   * Returns the encoding profile for the given identifier or <code>null</code> if no such profile has been configured.
   *
   * @param id
   *          the profile identifier
   * @return the profile
   */
  public EncodingProfile getProfile(String id) {
    return profiles.get(id);
  }

  /**
   * Returns Map containing profiles.
   *
   * @return the profile definitions
   */
  public Map<String, EncodingProfile> getProfiles() {
    return new HashMap<String, EncodingProfile>(profiles);
  }

  /**
   * Reads the profiles from the given set of properties.
   *
   * @param properties
   *          the properties
   * @return the profiles found in the properties
   */
  private Map<String, EncodingProfile> loadFromProperties(File artifact) throws IOException {
    // Format name
    FileInputStream in = null;
    Properties properties = new Properties();
    try {
      in = new FileInputStream(artifact);
      properties.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // Find list of formats in properties
    List<String> profileNames = new ArrayList<String>();
    for (Object fullKey : properties.keySet()) {
      String key = fullKey.toString();
      if (key.startsWith(PROP_PREFIX) && key.endsWith(PROP_NAME)) {
        int separatorLocation = fullKey.toString().lastIndexOf('.');
        key = key.substring(PROP_PREFIX.length(), separatorLocation);
        if (!profileNames.contains(key)) {
          profileNames.add(key);
        } else {
          throw new ConfigurationException("Found duplicate definition for encoding profile '" + key + "'");
        }
      }
    }

    // Load the formats
    Map<String, EncodingProfile> profiles = new HashMap<String, EncodingProfile>();
    for (String profileId : profileNames) {
      logger.debug("Enabling media format " + profileId);
      EncodingProfile profile = loadProfile(profileId, properties, artifact);
      profiles.put(profileId, profile);
    }

    return profiles;
  }

  /**
   * Reads the profile from the given properties
   *
   * @param profile
   * @param properties
   * @param artifact
   * @return
   * @throws RuntimeException
   */
  private EncodingProfile loadProfile(String profile, Properties properties, File artifact) throws RuntimeException {
    String identifier = profile;
    List<String> defaultProperties = new ArrayList<String>(10);

    Object name = getDefaultProperty(profile, PROP_NAME, properties, defaultProperties);
    if (name == null || "".equals(name.toString().trim()))
      throw new ConfigurationException("Distribution profile '" + profile
              + "' is missing a name. (Check web.xml profiles.)");

    EncodingProfileImpl df = new EncodingProfileImpl(identifier, name.toString().trim(), artifact);

    // Output Type
    Object type = getDefaultProperty(profile, PROP_OUTPUT, properties, defaultProperties);
    if (type == null || "".equals(type.toString().trim()))
      throw new ConfigurationException("Type of profile '" + profile + "' is missing");
    try {
      df.setOutputType(MediaType.parseString(type.toString().trim()));
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException("Type '" + type + "' of profile '" + profile + "' is unknwon");
    }

    // Suffix
    Object suffixObj = getDefaultProperty(profile, PROP_SUFFIX, properties, defaultProperties);
    if (suffixObj == null || "".equals(suffixObj.toString().trim()))
      throw new ConfigurationException("Suffix of profile '" + profile + "' is missing");
    df.setSuffix(suffixObj.toString());

    // Mimetype
    Object mimeTypeObj = getDefaultProperty(profile, PROP_MIMETYPE, properties, defaultProperties);
    if (mimeTypeObj == null || "".equals(mimeTypeObj.toString().trim()))
      throw new ConfigurationException("Mime type of profile '" + profile + "' is missing");
    df.setMimeType(mimeTypeObj.toString());

    // Applicable to the following track categories
    Object applicableObj = getDefaultProperty(profile, PROP_APPLICABLE, properties, defaultProperties);
    if (applicableObj == null || "".equals(applicableObj.toString().trim()))
      throw new ConfigurationException("Input type of profile '" + profile + "' is missing");
    df.setApplicableType(MediaType.parseString(applicableObj.toString().trim()));

    // Look for extensions
    String extensionKey = PROP_PREFIX + profile + ".";
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      if (key.startsWith(extensionKey) && !defaultProperties.contains(key)) {
        String k = key.substring(extensionKey.length());
        String v = entry.getValue().toString();
        df.addExtension(k, v);
      }
    }

    return df;
  }

  /**
   * Returns the default property and registers the property key in the list.
   *
   * @param profile
   *          the profile identifier
   * @param keySuffix
   *          the key suffix, like ".name"
   * @param properties
   *          the properties
   * @param list
   *          the list of default property keys
   * @return the property value or <code>null</code>
   */
  private static String getDefaultProperty(String profile, String keySuffix, Properties properties, List<String> list) {
    StringBuffer buf = new StringBuffer(PROP_PREFIX);
    buf.append(profile);
    buf.append(keySuffix);
    String key = buf.toString();
    list.add(key);
    return properties.getProperty(key);
  }
}
