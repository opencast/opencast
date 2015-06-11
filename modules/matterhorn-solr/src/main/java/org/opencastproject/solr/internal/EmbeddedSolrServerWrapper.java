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

package org.opencastproject.solr.internal;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;

/**
 * Wrapper around the embedded solr server class providing the ability to cleanly shut down the server instance.
 */
public class EmbeddedSolrServerWrapper extends EmbeddedSolrServer {

  /** Serial version UID */
  private static final long serialVersionUID = 6563713572181761065L;

  /** Reference to the solr core */
  private CoreContainer coreContainer = null;

  /**
   * Creates a new wrapped instance of the
   *
   * @param coreContainer
   *          the core container
   * @param coreName
   *          name of the core
   */
  public EmbeddedSolrServerWrapper(CoreContainer coreContainer, String coreName) {
    super(coreContainer, coreName);
    this.coreContainer = coreContainer;
  }

  /**
   * Shuts down the embedded solr server by forwarding the shutdown command to the {@link CoreContainer}.
   */
  public void shutdown() {
    if (coreContainer != null)
      coreContainer.shutdown();
  }

}
