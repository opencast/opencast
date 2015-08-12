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


package org.opencastproject.oaipmh.harvester;

import org.joda.time.DateTime;
import org.opencastproject.oaipmh.util.PersistenceEnv;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.util.Date;
import java.util.Dictionary;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.opencastproject.oaipmh.harvester.LastHarvested.cleanup;
import static org.opencastproject.oaipmh.harvester.LastHarvested.getLastHarvestDate;
import static org.opencastproject.oaipmh.harvester.LastHarvested.update;
import static org.opencastproject.oaipmh.util.ConcurrencyUtil.shutdownAndAwaitTermination;
import static org.opencastproject.oaipmh.util.OsgiUtil.checkDictionary;
import static org.opencastproject.oaipmh.util.OsgiUtil.getCfg;
import static org.opencastproject.oaipmh.util.OsgiUtil.getCfgAsInt;
import static org.opencastproject.oaipmh.util.PersistenceUtil.newEntityManagerFactory;
import static org.opencastproject.oaipmh.util.PersistenceUtil.newPersistenceEnvironment;

/**
 * The harvester queries OAI-PMH repositories for a certain metadata prefix and passes
 * the retrieved records to the configured {@link RecordHandler} for the actual processing.
 * <p/>
 * todo
 * <h3>Currently not supported</h3>
 * <ul>
 * <li>Recovery from network errors while processing a resumable request. Currently
 * the request sequence terminates and processing goes on with the next configured repository.
 * <li>Selective harvesting by time is not yet implemented. The harvester always requests the whole repository.</li>
 * </ul>
 */
public class OaiPmhHarvester implements ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(OaiPmhHarvester.class);

  // config keys

  private static final String CFG_USER_ORGANIZATION = "user.organization";
  private static final String CFG_USER_NAME = "user.name";
  private static final String CFG_PERIOD = "period";
  private static final String CFG_INITIAL_DELEY = "initial-delay";
  private static final String CFG_URLS = "urls";

  // service reference names - make sure they match the names used in the component.xml

  private static final String REF_ORG_SERVICE = "orgDirectory";
  private static final String REF_SECURITY_SERVICE = "securityService";
  private static final String REF_USER_SERVICE = "userDirectory";
  private static final String REF_RECORD_HANDLER = "recordHandler";

  private ComponentContext componentContext;

  private ScheduledExecutorService scheduler;

  private PersistenceEnv penv;

  /**
   * @see #activate(ComponentContext)
   */
  @Override
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    logger.info("Updated");
    try {
      checkDictionary(properties, componentContext);
      // locate all services
      final RecordHandler recordhandler = (RecordHandler) componentContext.locateService(REF_RECORD_HANDLER);
      // collect all config params
      final int period = getCfgAsInt(properties, CFG_PERIOD);
      final int initialDelay = getCfgAsInt(properties, CFG_INITIAL_DELEY);
      final String urlsRaw = getCfg(properties, CFG_URLS);
      final String[] urls = urlsRaw.split("\\s*,\\s*");
      // shutdown currently running tasks
      if (scheduler != null)
        scheduler.shutdown();
      scheduler = Executors.newSingleThreadScheduledExecutor();
      logger.info("Schedule harvesting " + urlsRaw + " at " + initialDelay + ", " + period + " (minutes)");
      final Function0<Void> secConf = createSecurityConfigurator(properties, componentContext);
      // get persistence provider
      penv = newPersistenceEnvironment(newEntityManagerFactory(componentContext, "org.opencastproject.oaipmh.harvester"));
      // create a new worker
      Worker worker = new Worker(urls, recordhandler, secConf, penv);
      scheduler.scheduleAtFixedRate(worker, initialDelay, period, TimeUnit.MINUTES);
    } catch (ConfigurationException e) {
      logger.info("Configuration not complete since at least property " + e.getProperty() + " is missing or malformed. "
          + "Please provide a clean configuration to enable harvesting.");
    }
  }

  /**
   * Return a function that configures the security service with a {@link User} and {@link Organization}.
   */
  private static Function0<Void> createSecurityConfigurator(Dictionary properties, ComponentContext cc) throws ConfigurationException {
    // get services
    final OrganizationDirectoryService organizationDirectoryService =
        (OrganizationDirectoryService) cc.locateService(REF_ORG_SERVICE);
    final SecurityService securityService = (SecurityService) cc.locateService(REF_SECURITY_SERVICE);
    final UserDirectoryService userDirectoryService =
        (UserDirectoryService) cc.locateService(REF_USER_SERVICE);
    // get the organization
    String organizationName = getCfg(properties, CFG_USER_ORGANIZATION);
    final Organization organization;
    try {
      organization = organizationDirectoryService.getOrganization(organizationName);
    } catch (NotFoundException e) {
      throw new ConfigurationException(CFG_USER_ORGANIZATION, "Organization '" + organizationName + "' does not exist");
    }

    // get the user
    final User user;
    final Organization originalOrg = securityService.getOrganization();
    try {
      String userName = getCfg(properties, CFG_USER_NAME);
      securityService.setOrganization(organization);
      user = userDirectoryService.loadUser(userName);
    } finally {
      securityService.setOrganization(originalOrg);
    }
    return new Function0<Void>() {
      @Override
      public Void apply() {
        securityService.setOrganization(organization);
        securityService.setUser(user);
        return null;
      }
    };
  }

  /**
   * OSGi component activation. Called by the container. Declare in the component xml.
   * Called before {@link #updated(java.util.Dictionary)} but needs to be synchronized with it.
   */
  public synchronized void activate(ComponentContext cc) {
    logger.info("Activate");
    this.componentContext = cc;
  }

  /**
   * OSGi component deactivation. Called by the container. Declare in the component xml.
   */
  public synchronized void deactivate() {
    logger.info("Deactivate");
    if (scheduler != null)
      shutdownAndAwaitTermination(scheduler, 60, new Function0<Void>() {
        @Override
        public Void apply() {
          logger.error("Scheduler does not terminate");
          return null;
        }
      });
    if (penv != null)
      penv.close();
  }

  static class Worker implements Runnable {

    private final String[] urls;
    private final RecordHandler handler;
    private final Function0<Void> securityConfigurator;
    private final PersistenceEnv penv;

    /**
     * @param urls the urls, i.e. the repositories, to harvest
     * @param securityConfigurator a function to configure the security service in order to access the search service
     */
    Worker(String[] urls,
           RecordHandler handler,
           Function0<Void> securityConfigurator,
           PersistenceEnv penv) {
      this.urls = urls;
      this.handler = handler;
      this.securityConfigurator = securityConfigurator;
      this.penv = penv;
    }

    @Override
    public void run() {
      // configure security settings for this thread
      securityConfigurator.apply();
      for (String url : urls) {
        try {
          DateTime now = new DateTime();
          harvest(url, getLastHarvestDate(penv, url));
          // save the time of the last harvest but with a security delta of 1 minutes
          update(penv, new LastHarvested(url, now.minusMinutes(1).toDate()));
        } catch (Exception e) {
          logger.error("An error occured while harvesting " + url + ". Skipping this repository for now...", e);
        }
      }
      cleanup(penv, urls);
    }

    private void harvest(String url, Option<Date> from) throws Exception {
      logger.info("Harvesting " + url + " from " + from + " on thread " + Thread.currentThread());
      OaiPmhRepositoryClient repositoryClient = OaiPmhRepositoryClient.newHarvester(url);
      ListRecordsResponse response =
          repositoryClient.listRecords(handler.getMetadataPrefix(), from, Option.<Date>none(), Option.<String>none());
      if (!response.isError()) {
        for (Node recordNode : ListRecordsResponse.getAllRecords(response, repositoryClient)) {
          handler.handle(recordNode);
        }
      } else if (response.isErrorNoRecordsMatch()) {
        logger.info("Repository returned no records.");
      } else {
        logger.error("Repository returned error code: " + response.getErrorCode().getOrElse("?"));
      }
    }
  }

}
