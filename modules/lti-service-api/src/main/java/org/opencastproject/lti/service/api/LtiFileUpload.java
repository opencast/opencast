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
package org.opencastproject.lti.service.api;

import java.io.InputStream;

/**
 * A tuple consisting of a file name and the file data
 */
public final class LtiFileUpload {
  private final InputStream stream;
  private final String sourceName;

  /**
   * Construct a file upload object
   * @param stream The file data
   * @param sourceName The file (or source) name
   */
  public LtiFileUpload(final InputStream stream, final String sourceName) {
    this.stream = stream;
    this.sourceName = sourceName;
  }

  /**
   * Get the file data
   * @return The file data
   */
  public InputStream getStream() {
    return stream;
  }

  /**
   * Get the source (file) name
   * @return The source (file) name
   */
  public String getSourceName() {
    return sourceName;
  }
}
