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

package org.opencastproject.mediapackage.identifier;

import org.opencastproject.util.ConfigurationException;

/**
 * This class is used to create instances of a handle builder. To specify your own implementation of the handle builder,
 * you simply have to provide the class name of the desired implementation by setting the system property
 * <code>opencast.handlebuilder</code> accordingly.
 */
public final class HandleBuilderFactory {

  /** Class name for the default handle builder */
  private static final String BUILDER_CLASS = "org.opencastproject.mediapackage.identifier.HandleBuilderImpl";

  /** Name of the system property */
  public static final String PROPERTY_NAME = "opencast.handlebuilder";

  /** The implementation class name */
  private static String builderClassName = BUILDER_CLASS;

  /** The singleton instance of this factory */
  private static final HandleBuilderFactory factory = new HandleBuilderFactory();

  /** The default builder implementation */
  private HandleBuilder builder = null;

  /**
   * Private method to create a new handle builder factory.
   */
  private HandleBuilderFactory() {
    String className = System.getProperty(PROPERTY_NAME);
    if (className != null) {
      builderClassName = className;
    }
  }

  /**
   * Returns an instance of a HandleBuilderFactory.
   *
   * @return the handle builder factory
   * @throws ConfigurationException
   *           if the factory cannot be instantiated
   */
  public static HandleBuilderFactory newInstance() throws ConfigurationException {
    return factory;
  }

  /**
   * Factory method that returns an instance of a handle builder.
   * <p>
   * It uses the following ordered lookup procedure to determine which implementation of the {@link HandleBuilder}
   * interface to use:
   * <ul>
   * <li>Implementation specified using the <code>opencast.handlebuilder</code> system property</li>
   * <li>Platform default implementation</li>
   * </ul>
   *
   * @return the handle builder
   * @throws ConfigurationException
   *           If the builder cannot be instantiated
   */
  public HandleBuilder newHandleBuilder() throws ConfigurationException {
    if (builder == null) {
      try {
        Class<?> builderClass = Class.forName(builderClassName);
        builder = (HandleBuilder) builderClass.newInstance();
      } catch (ClassNotFoundException e) {
        throw new ConfigurationException("Class not found while creating handle builder: " + e.getMessage(), e);
      } catch (InstantiationException e) {
        throw new ConfigurationException("Instantiation exception while creating handle builder: " + e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new ConfigurationException("Access exception while creating handle builder: " + e.getMessage(), e);
      }
    }
    return builder;
  }

}
