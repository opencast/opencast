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
package org.opencastproject.solr;

import org.opencastproject.solr.internal.EmbeddedSolrServerWrapper;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.OpencastSolrConfig;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;

import java.io.File;
import java.net.URL;

/**
 * Factory class that will create clients to solr server instances that are either running inside the local virtual
 * machine or remotely.
 */
public final class SolrServerFactory {

  /** Disallow construction of this utility class */
  private SolrServerFactory() {
  }

  /**
   * Constructor. Prepares solr connection.
   *
   * @param solrDir
   *          The directory of the solr instance.
   * @param dataDir
   *          The directory of the solr index data.
   */
  public static SolrServer newEmbeddedInstance(File solrDir, File dataDir) throws SolrServerException {
    try {
      final CoreContainer cc = new CoreContainer(solrDir.getAbsolutePath());
      SolrConfig config = new OpencastSolrConfig(solrDir.getAbsolutePath(), SolrConfig.DEFAULT_CONF_FILE, null);
      IndexSchema schema = new IndexSchema(config, solrDir + "/conf/schema.xml", null);
      SolrCore core0 = new SolrCore(null, dataDir.getAbsolutePath(), config, schema, null);
      cc.register("core0", core0, false);
      return new EmbeddedSolrServerWrapper(cc, "core0");
    } catch (Exception e) {
      throw new SolrServerException(e);
    }
  }

  /**
   * Constructor. Prepares solr connection.
   *
   * @param url
   *          the connection url to the solr server
   */
  public static SolrServer newRemoteInstance(URL url) {
    try {
      CommonsHttpSolrServer server = new CommonsHttpSolrServer(url);
      server.setSoTimeout(1000);
      server.setConnectionTimeout(100);
      server.setDefaultMaxConnectionsPerHost(100);
      server.setMaxTotalConnections(100);
      server.setFollowRedirects(false); // defaults to false
      server.setAllowCompression(true);
      server.setMaxRetries(1); // defaults to 0. > 1 not recommended.
      server.setParser(new XMLResponseParser()); // binary parser is used by default
      return server;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Shuts down a solr server or, in the case of a connection to a remote server, closes the connection.
   *
   * @param solrServer
   *          the solr server instance
   */
  public static void shutdown(SolrServer solrServer) {
    if (solrServer instanceof EmbeddedSolrServerWrapper) {
      ((EmbeddedSolrServerWrapper) solrServer).shutdown();
    } else {
      // TODO: there doesn't appear to be any mechanism to close a remote connection to a solr server.
    }
  }

}
