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
 * Factory to retreive instances of a media package element builder. Use the static method {@link #newInstance()} to
 * obtain a reference to a concrete implementation of a <code>MediaPackageElementBuilderFactory</code>. This instance
 * can then be used to create or load media package elements.
 * <p>
 * The factory can be configured by specifying the concrete implementation class through the system property
 * <code>org.opencastproject.mediapackage.elementbuilder</code>.
 * </p>
 */
public final class MediaPackageElementBuilderFactory {

  /** The implementation class name */
  private static String builderClassName = "org.opencastproject.mediapackage.MediaPackageElementBuilderImpl";

  /** The singleton instance of this factory */
  private static final MediaPackageElementBuilderFactory factory = new MediaPackageElementBuilderFactory();

  /** The default builder implementation */
  private MediaPackageElementBuilder builder = null;

  /**
   * Returns an instance of a MediaPackageElementBuilderFactory.
   *
   * @return the media package element builder factory
   * @throws ConfigurationException
   *           if the factory cannot be instantiated
   */
  public static MediaPackageElementBuilderFactory newInstance() throws ConfigurationException {
    return factory;
  }

  /**
   * Factory method that returns an instance of a media package element builder.
   * <p>
   * It uses the following ordered lookup procedure to determine which implementation of the
   * {@link MediaPackageElementBuilder} interface to use:
   * <ul>
   * <li>Implementation specified using the <code>org.opencastproject.mediapackage.elementbuilder</code> system property
   * </li>
   * <li>Platform default implementation</li>
   * </ul>
   *
   * @return the media package element builder
   * @throws ConfigurationException
   *           If the builder cannot be instantiated
   */
  public MediaPackageElementBuilder newElementBuilder() throws ConfigurationException {
    if (builder == null) {
      try {
        Class<?> builderClass = Class.forName(builderClassName, true,
                MediaPackageElementBuilderFactory.class.getClassLoader());
        builder = (MediaPackageElementBuilder) builderClass.newInstance();
      } catch (ClassNotFoundException e) {
        throw new ConfigurationException("Class not found while creating element builder: " + e.getMessage(), e);
      } catch (InstantiationException e) {
        throw new ConfigurationException("Instantiation exception while creating element builder: " + e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new ConfigurationException("Access exception while creating element builder: " + e.getMessage(), e);
      } catch (Exception e) {
        throw new ConfigurationException("Exception while creating element builder: " + e.getMessage(), e);
      }
    }
    return builder;
  }

}
