/*
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


package org.opencastproject.mediapackage;

import org.opencastproject.util.ConfigurationException;

/**
 * Factory to retrieve instances of a media package element builder. Use the static method {@link #newInstance()} to
 * obtain a reference to a <code>MediaPackageElementBuilderFactory</code>. This instance can then be used to create
 * media package elements.
 */
public final class MediaPackageElementBuilderFactory {

  /** The singleton instance of this factory */
  private static final MediaPackageElementBuilderFactory factory = new MediaPackageElementBuilderFactory();

  /** The builder implementation. It is stateless and can be shared. */
  private final MediaPackageElementBuilder builder = new MediaPackageElementBuilderImpl();

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
   *
   * @return the media package element builder
   * @throws ConfigurationException
   *           If the builder cannot be instantiated
   */
  public MediaPackageElementBuilder newElementBuilder() throws ConfigurationException {
    return builder;
  }

}
