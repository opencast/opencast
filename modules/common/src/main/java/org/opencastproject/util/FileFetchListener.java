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

package org.opencastproject.util;

import java.time.Instant;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface for services that want to be told when a file was fetched from the file server, e.g. to gather
 * statistics.
 * <p>
 * The file server references listeners optionally and calls them while it is serving requests. Serving files must not
 * depend on a listener being present or working, so implementations have to return quickly, must not block and cannot
 * rely on being called for every request.
 */
public interface FileFetchListener {

  /**
   * Called when a file was fetched. It is up to the listener to decide whether it is interested in the file.
   *
   * @param path
   *          path of the file, relative to the file server
   * @param contentType
   *          the mime type of the file
   * @param timestamp
   *          time of the response
   * @param from
   *          start of the fetched byte range, 0 if the request is not a range request
   * @param to
   *          end of the fetched byte range, or null if the request did not specify one
   * @param request
   *          the request of the client that fetched the file. It is only valid during this call.
   */
  void fileFetched(String path, String contentType, Instant timestamp, long from, Long to,
          HttpServletRequest request);
}
