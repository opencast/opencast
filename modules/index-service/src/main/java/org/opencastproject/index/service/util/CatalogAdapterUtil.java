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
package org.opencastproject.index.service.util;

import org.opencastproject.util.IoSupport;

import org.osgi.service.component.ComponentException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class CatalogAdapterUtil {

  private CatalogAdapterUtil() {

  }

  /**
   * Get the catalog properties from the given file
   * 
   * @param sourceClass
   *          Source from where the the method is called
   * @param sourceFile
   *          the path to the source file
   * @return the catalog {@link Properties}
   */
  public static Properties getCatalogProperties(Class<?> sourceClass, String sourceFile) {
    Properties episodeCatalogProperties = new Properties();
    InputStream in = null;
    try {
      in = sourceClass.getResourceAsStream(sourceFile);
      episodeCatalogProperties.load(in);
    } catch (IOException e) {
      throw new ComponentException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }
    return episodeCatalogProperties;
  }

}
