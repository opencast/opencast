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

package org.opencastproject.oaipmh.server;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.map.LRUMap;
import org.opencastproject.oaipmh.Granularity;
import org.opencastproject.oaipmh.util.XmlGen;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.util.data.Option;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import static org.opencastproject.oaipmh.util.OsgiUtil.checkDictionary;
import static org.opencastproject.oaipmh.util.OsgiUtil.getCfg;
import static org.opencastproject.oaipmh.util.OsgiUtil.getCfgAsInt;
import static org.opencastproject.oaipmh.util.OsgiUtil.getContextProperty;
import static org.opencastproject.util.data.Option.option;

/**
 * The OAI-PMH server. Backed by an OAI-PMH repository.
 */
public final class OaiPmhRepositoryServlet extends HttpServlet implements ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(OaiPmhRepositoryServlet.class);

  private static final String PROP_ADMIN_EMAIL = "org.opencastproject.admin.email";
  private static final String PROP_SERVER_URL = "org.opencastproject.server.url";
  private static final String CFG_REPOSITORY_NAME = "repository-name";
  private static final String CFG_MOUNT_PATH = "mount-path";
  private static final String CFG_RESULT_LIMIT = "result-limit";

  private OaiPmhRepository repository;

  private ComponentContext componentContext;

  /**
   * The alias under which the servlet is currently registered.
   *
   * @see HttpService#registerServlet(String, javax.servlet.Servlet, java.util.Dictionary, org.osgi.service.http.HttpContext)
   */
  private String currentServletAlias;

  private List<MetadataProvider> metadataProviders = Collections.synchronizedList(new ArrayList<MetadataProvider>());

  /**
   * Service dependency. Called by the OSGi container. See the component xml.
   */
  public void setMetadataProvider(MetadataProvider provider) {
    logger.info("Register metadata provider " + provider + " for prefix " + provider.getMetadataFormat().getPrefix());
    metadataProviders.add(provider);
  }

  /**
   * Service dependency. Called by the OSGi container. See the component xml.
   */
  public void unsetMetadataProvider(MetadataProvider provider) {
    logger.info("Unregister metadata provider " + provider + " for prefix " + provider.getMetadataFormat().getPrefix());
    metadataProviders.remove(provider);
  }

  /**
   * OSGi component activation. Called by the container.
   */
  public synchronized void activate(ComponentContext cc) {
    logger.info("Activate");
    this.componentContext = cc;
  }

  /**
   * Called by the ConfigurationAdmin service.
   * This method actually sets up the server.
   */
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    logger.info("Updated");
    checkDictionary(properties, componentContext);
    // collect properties
    final String path = getCfg(properties, CFG_MOUNT_PATH);
    final SearchService searchService = (SearchService) componentContext.locateService("searchService");
    logger.info("Using search service " + searchService);
    final String baseUrl = getContextProperty(componentContext, PROP_SERVER_URL) + path;
    final String repositoryName = getCfg(properties, CFG_REPOSITORY_NAME);
    final String adminEmail = getContextProperty(componentContext, PROP_ADMIN_EMAIL);
    final Integer resultLimit = getCfgAsInt(properties, CFG_RESULT_LIMIT);
    final HttpService httpService = (HttpService) componentContext.locateService("httpService");
    // register servlet
    try {
      // ... and unregister first if necessary
      if (currentServletAlias != null)
        httpService.unregister(currentServletAlias);
      httpService.registerServlet(path, this, null, null);
      currentServletAlias = path;
      logger.info("Registering OAI-PMH server under " + path);
    } catch (Exception e) {
      throw new RuntimeException("Error registering OAI-PMH servlet", e);
    }
    // create repository
    repository = new OaiPmhRepository() {
      private Map resumptionTokens = Collections.synchronizedMap(new LRUMap(100));

      @Override
      public Granularity getRepositoryTimeGranularity() {
        return Granularity.DAY;
      }

      @Override
      public String getBaseUrl() {
        return baseUrl;
      }

      @Override
      public String getRepositoryName() {
        return repositoryName;
      }

      @Override
      public SearchService getSearchService() {
        return searchService;
      }

      @Override
      public String getAdminEmail() {
        return adminEmail;
      }

      @Override
      public String saveQuery(ResumableQuery query) {
        String token = DigestUtils.md5Hex(Double.toString(Math.random()));
        resumptionTokens.put(token, query);
        return token;
      }

      @Override
      public Option<ResumableQuery> getSavedQuery(String resumptionToken) {
        return option(((ResumableQuery) resumptionTokens.get(resumptionToken)));
      }

      @Override
      public int getResultLimit() {
        return resultLimit;
      }

      @Override
      public List<MetadataProvider> getMetadataProviders() {
        return metadataProviders;
      }
    };
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    dispatch(req, res);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    dispatch(req, res);
  }

  private void dispatch(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
    try {
      final Params p = new Params() {
        @Override
        String getParameter(String key) {
          return req.getParameter(key);
        }
      };
      XmlGen oai = repository.selectVerb(p);
      try {
        res.setCharacterEncoding("UTF-8");
        res.setContentType("text/xml;charset=UTF-8");
        oai.generate(res.getOutputStream());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch (Exception e) {
      logger.error("Error handling OAI-PMH request", e);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

}
