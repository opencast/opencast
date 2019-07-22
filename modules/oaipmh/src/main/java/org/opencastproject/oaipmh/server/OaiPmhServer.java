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
package org.opencastproject.oaipmh.server;

import static java.lang.String.format;
import static org.opencastproject.oaipmh.util.OsgiUtil.checkDictionary;
import static org.opencastproject.oaipmh.util.OsgiUtil.getCfg;
import static org.opencastproject.oaipmh.util.OsgiUtil.getContextProperty;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Strings.trimToNil;

import org.opencastproject.oaipmh.util.XmlGen;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** The OAI-PMH server. Backed by an arbitrary amount of OAI-PMH repositories. */
public final class OaiPmhServer extends HttpServlet implements OaiPmhServerInfo, ManagedService {

  private static final long serialVersionUID = -7536526468920288612L;

  private static final Logger logger = LoggerFactory.getLogger(OaiPmhServer.class);

  private static final String CFG_DEFAULT_REPOSITORY = "default-repository";
  private static final String CFG_OAIPMH_MOUNTPOINT = "org.opencastproject.oaipmh.mountpoint";
  private static final String CFG_DEFAULT_OAIPMH_MOUNTPOINT = "/oaipmh";

  private SecurityService securityService;

  private final Map<String, OaiPmhRepository> repositories = map();

  private ComponentContext componentContext;

  private String defaultRepo;

  /**
   * The alias under which the servlet is currently registered.
   */
  private String mountPoint;

  private ServiceRegistration<?> serviceRegistration;

  /** OSGi DI. */
  public void setRepository(final OaiPmhRepository r) {
    synchronized (repositories) {
      final String rId = r.getRepositoryId();
      if (repositories.containsKey(rId)) {
        logger.error(format("A repository with id %s has already been registered", rId));
      } else {
        // lazy creation since 'baseUrl' is not available at this time
        repositories.put(rId, r);
        logger.info("Registered repository " + rId);
      }
    }
  }

  /** OSGi DI. */
  public void unsetRepository(OaiPmhRepository r) {
    synchronized (repositories) {
      repositories.remove(r.getRepositoryId());
      logger.info("Unregistered repository " + r.getRepositoryId());
    }
  }

  /** OSGi DI. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi component activation. */
  public synchronized void activate(ComponentContext cc) {
    logger.info("Activate");
    this.componentContext = cc;
    // get mount point
    try {
        mountPoint = UrlSupport.concat("/", StringUtils.trimToNull(getContextProperty(componentContext, CFG_OAIPMH_MOUNTPOINT)));
    } catch (RuntimeException e) {
        mountPoint = CFG_DEFAULT_OAIPMH_MOUNTPOINT;
    }

  }

  /** Called by the ConfigurationAdmin service. This method actually sets up the server. */
  @Override
  public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    // Because the OAI-PMH server implementation is technically not a REST service implemented
    // using JAX-RS annotations the Opencast mechanisms for registering REST endpoints do not work.
    // The server has to register itself with the OSGi HTTP service.
    logger.info("Updated");
    checkDictionary(properties, componentContext);
    defaultRepo = getCfg(properties, CFG_DEFAULT_REPOSITORY);
    // register servlet
    try {
      // ... and unregister first if necessary
      tryUnregisterServlet();
      logger.info("Registering OAI-PMH server under " + mountPoint);
      logger.info("Default repository is " + defaultRepo);

      serviceRegistration = OsgiUtil.registerServlet(componentContext.getBundleContext(), this, mountPoint);
    } catch (Exception e) {
      logger.error("Error registering OAI-PMH servlet", e);
      throw new RuntimeException("Error registering OAI-PMH servlet", e);
    }
    logger.info(format("There are %d repositories registered yet. Watch out for later registration messages.",
            repositories.values().size()));
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
      for (String serverUrl : OaiPmhServerInfoUtil.oaiPmhServerUrlOfCurrentOrganization(securityService)) {
        for (String repoId : repositoryId(req, mountPoint)) {
          if (runRepo(repoId, serverUrl, req, res)) {
            return;
          } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
          }
        }
        // no repository id in path, try default repo
        if (runRepo(defaultRepo, serverUrl, req, res)) {
          return;
        }
      }
      res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    } catch (Exception e) {
      logger.error("Error handling OAI-PMH request", e);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Run repo <code>repoId</code>.
   *
   * @return false if the repo does not exist, true otherwise
   */
  private boolean runRepo(String repoId, String serverUrl, HttpServletRequest req, HttpServletResponse res)
          throws Exception {
    for (OaiPmhRepository repo : getRepoById(repoId)) {
      final String repoUrl = UrlSupport.concat(serverUrl, mountPoint, repoId);
      runRepo(repo, repoUrl, req, res);
      return true;
    }
    return false;
  }

  private void runRepo(OaiPmhRepository repo, final String repoUrl, final HttpServletRequest req,
          HttpServletResponse res) throws Exception {
    final Params p = new Params() {
      @Override
      String getParameter(String key) {
        return req.getParameter(key);
      }

      @Override
      String getRepositoryUrl() {
        return repoUrl;
      }
    };
    final XmlGen oai = repo.selectVerb(p);
    res.setCharacterEncoding("UTF-8");
    res.setContentType("text/xml;charset=UTF-8");
    oai.generate(res.getOutputStream());
  }

  @Override
  public void destroy() {
    super.destroy();
    tryUnregisterServlet();
  }

  private void tryUnregisterServlet() {
    if (serviceRegistration != null) {
      serviceRegistration.unregister();
    }
  }

  /**
   * Retrieve the repository id from the requested path.
   *
   * @param req
   *          the HTTP request
   * @param mountPoint
   *          the base path of the OAI-PMH server, e.g. /oaipmh
   */
  public static Option<String> repositoryId(HttpServletRequest req, String mountPoint) {
    return mlist(StringUtils.removeStart(UrlSupport.removeDoubleSeparator(req.getRequestURI()), mountPoint).split("/"))
            .bind(trimToNil).headOpt();
  }

  /** Get a repository by id. */
  private Option<OaiPmhRepository> getRepoById(String id) {
    synchronized (repositories) {
      if (hasRepo(id)) {
        return some(repositories.get(id));
      } else {
        logger.warn("No OAI-PMH repository has been registered with id " + id);
        return none();
      }
    }
  }

  @Override
  public boolean hasRepo(String id) {
    synchronized (repositories) {
      return repositories.containsKey(id);
    }
  }

  @Override
  public String getMountPoint() {
    return mountPoint;
  }
}
