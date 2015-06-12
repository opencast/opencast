/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package org.opencastproject.mediapackage.identifier;

import org.opencastproject.util.ConfigurationException;

/**
 * This class is used to create instances of an id builder. To specify your own implementation of the id builder, you
 * simply have to provide the class name of the desired implementation by setting the system property
 * <code>opencast.idbuilder</code> accordingly.
 */
public final class IdBuilderFactory {

  /** Class name for the default id builder */
  private static final String BUILDER_CLASS = "org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl";

  /** Name of the system property */
  public static final String PROPERTY_NAME = "opencast.idbuilder";

  /** The implementation class name */
  private static String builderClassName = BUILDER_CLASS;

  /** The singleton instance of this factory */
  private static final IdBuilderFactory factory = new IdBuilderFactory();

  /** The default builder implementation */
  private IdBuilder builder = null;

  /**
   * Private method to create a new id builder factory.
   */
  private IdBuilderFactory() {
    String className = System.getProperty(PROPERTY_NAME);
    if (className != null) {
      builderClassName = className;
    }
  }

  /**
   * Returns an instance of a HandleBuilderFactory.
   *
   * @return the id builder factory
   * @throws ConfigurationException
   *           if the factory cannot be instantiated
   */
  public static IdBuilderFactory newInstance() throws ConfigurationException {
    return factory;
  }

  /**
   * Factory method that returns an instance of an id builder.
   * <p>
   * It uses the following ordered lookup procedure to determine which implementation of the {@link IdBuilder} interface
   * to use:
   * <ul>
   * <li>Implementation specified using the <code>opencast.idbuilder</code> system property</li>
   * <li>Platform default implementation</li>
   * </ul>
   *
   * @return the id builder
   * @throws ConfigurationException
   *           If the builder cannot be instantiated
   */
  public IdBuilder newIdBuilder() throws ConfigurationException {
    if (builder == null) {
      try {
        Class<?> builderClass = Class.forName(builderClassName);
        builder = (IdBuilder) builderClass.newInstance();
      } catch (ClassNotFoundException e) {
        throw new ConfigurationException("Class not found while creating id builder: " + e.getMessage(), e);
      } catch (InstantiationException e) {
        throw new ConfigurationException("Instantiation exception while creating id builder: " + e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new ConfigurationException("Access exception while creating id builder: " + e.getMessage(), e);
      }
    }
    return builder;
  }

}
