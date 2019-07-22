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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utilities to ease dealing with Elasticsearch.
 */
final class ElasticsearchUtils {

  /**
   * Private constructor to make sure this class is used as a utility class.
   */
  private ElasticsearchUtils() {
  }

  /**
   * Creates an elastic search index configuration inside the given directory by loading the relevant configuration
   * files from the bundle.
   * 
   * @param configDirectory
   *          the configuration directory
   * @throws IOException
   *           if creating the configuration fails
   */
  static void createIndexConfigurationAt(File configDirectory) throws IOException {

    // Load the index configuration and move it into place
    String[] files = new String[] { "content-mapping.json", "elasticsearch.yml", "version-mapping.json" };

    for (String file : files) {
      final File fileLocation = new File(configDirectory, file);
      try (InputStream in = ElasticsearchUtils.class.getResourceAsStream("/elasticsearch/" + file)) {
        FileUtils.copyInputStreamToFile(in, fileLocation);
      }
    }

  }

}
