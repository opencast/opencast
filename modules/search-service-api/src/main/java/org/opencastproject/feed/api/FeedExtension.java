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


package org.opencastproject.feed.api;

/**
 * Modules are external namespaces than can be baked into a feed. A good example of such a module is the dublin core
 * extension.
 * <p>
 * Note that this interface is heavily inspired and backed by the excellent rss/atom feed library <code>Rome</code>
 * (http://https://rome.dev.java.net).
 */
public interface FeedExtension {

  /**
   * Returns the URI of the module.
   *
   * @return URI of the module.
   */
  String getUri();

}
