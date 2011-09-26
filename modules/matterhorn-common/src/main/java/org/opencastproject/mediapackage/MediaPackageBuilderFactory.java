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

package org.opencastproject.mediapackage;

import org.opencastproject.util.ConfigurationException;

/**
 * Factory to retrieve instances of a media package builder. Use the static method {@link #newInstance()} to obtain a
 * reference to a concrete implementation of a <code>MediaPackageBuilderFactory</code>. This instance can then be used
 * to create or load media packages.
 * <p>
 * The factory can be configured by specifying the concrete implementation class through the system property
 * <code>org.opencastproject.mediapackage.builder</code>.
 * </p>
 */
public final class MediaPackageBuilderFactory {

  /** Class name for the default media package builder */
  private static final String BUILDER_CLASS = "org.opencastproject.mediapackage.MediaPackageBuilderImpl";

  /** Name of the system property */
  public static final String PROPERTY_NAME = "org.opencastproject.mediapackage.builder";

  /** The implementation class name */
  private static String builderClassName = BUILDER_CLASS;

  /** The singleton instance of this factory */
  private static final MediaPackageBuilderFactory factory = new MediaPackageBuilderFactory();

  /**
   * Private method to create a new media package builder factory.
   */
  private MediaPackageBuilderFactory() {
    String className = System.getProperty(PROPERTY_NAME);
    if (className != null) {
      builderClassName = className;
    }
  }

  /**
   * Returns an instance of a MediaPackageBuilderFactory.
   * 
   * @return the media package builder factory
   * @throws ConfigurationException
   *           if the factory cannot be instantiated
   */
  public static MediaPackageBuilderFactory newInstance() throws ConfigurationException {
    return factory;
  }

  /**
   * Factory method that returns an instance of a media package builder.
   * <p>
   * It uses the following ordered lookup procedure to determine which implementation of the {@link MediaPackageBuilder}
   * interface to use:
   * <ul>
   * <li>Implementation specified using the <code>org.opencastproject.mediapackage.builder</code> system property</li>
   * <li>Platform default implementation</li>
   * </ul>
   * 
   * @return the media package builder
   * @throws ConfigurationException
   *           If the builder cannot be instantiated
   */
  public MediaPackageBuilder newMediaPackageBuilder() throws ConfigurationException {
    MediaPackageBuilder builder = null;
    try {
      Class<?> builderClass = Class.forName(builderClassName);
      builder = (MediaPackageBuilder) builderClass.newInstance();
    } catch (ClassNotFoundException e) {
      throw new ConfigurationException("Class not found while creating media package builder: " + e.getMessage(), e);
    } catch (InstantiationException e) {
      throw new ConfigurationException("Instantiation exception while creating media package builder: "
              + e.getMessage(), e);
    } catch (IllegalAccessException e) {
      throw new ConfigurationException("Access exception while creating media package builder: " + e.getMessage(), e);
    }
    return builder;
  }

}
