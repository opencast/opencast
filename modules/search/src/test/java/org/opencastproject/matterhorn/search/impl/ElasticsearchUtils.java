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


package org.opencastproject.matterhorn.search.impl;

import org.opencastproject.util.PathSupport;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Utilities to ease dealing with Elasticsearch.
 */
public final class ElasticsearchUtils {

  /**
   * Private constructor to make sure this class is used as a utility class.
   */
  private ElasticsearchUtils() {
  }

  /**
   * Creates an elastic search index configuration inside the given directory by loading the relevant configuration
   * files from the bundle. The final location will be <code>configDirectory/etc/index</code>.
   * 
   * @param configDirectory
   *          the configuration directory
   * @param index
   *          the index name
   * @throws IOException
   *           if creating the configuration fails
   */
  public static void createIndexConfigurationAt(File configDirectory, String index) throws IOException {

    // Load the index configuration and move it into place
    String[] files = new String[] { "content-mapping.json", "elasticsearch.yml", "version-mapping.json" };

    for (String file : files) {
      String bundleLocation = PathSupport.concat(new String[] { "/elasticsearch", index, file });
      File fileLocation = new File(configDirectory, file);
      FileUtils.copyInputStreamToFile(ElasticsearchUtils.class.getResourceAsStream(bundleLocation), fileLocation);
    }

  }

}
