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

package org.opencastproject.ingest.scanner;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.opencastproject.security.util.SecurityUtil.getUserAndOrganization;
import static org.opencastproject.util.data.Collections.dict;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

/**
 * The inbox scanner monitors a directory for incoming media packages.
 * <p/>
 * There is one InboxScanner instance per inbox. Each instance is configured by
 * a config file in <code>$FELIX_HOME/load</code> named <code>&lt;inbox-scanned-pid&gt;-&lt;name&gt;.cfg</code>
 * where <code>name</code> can be arbitrarily chosen and has no further meaning.
 * <code>inbox-scanned-pid</code> must confirm to the PID given to the InboxScanner in the declarative service (DS)
 * configuration <code>OSGI-INF/inbox-scanner-service.xml</code>.
 *
 * <h3>Implementation notes</h3>
 * Monitoring leverages Apache FileInstall by implementing {@link ArtifactInstaller}.
 *
 * @see Ingestor
 */
public class InboxScannerService implements ArtifactInstaller, ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(InboxScannerService.class);

  /** The configuration key to use for determining the user to run as for ingest */
  public static final String USER_NAME = "user.name";

  /** The configuration key to use for determining the user's organization */
  public static final String USER_ORG = "user.organization";

  /** The configuration key to use for determining the workflow definition to use for ingest */
  public static final String WORKFLOW_DEFINITION = "workflow.definition";

  /** The configuration key to use for determining the workflow configuration to use for ingest */
  public static final String WORKFLOW_CONFIG = "workflow.config";

  /** The configuration key to use for determining the inbox path */
  public static final String INBOX_PATH = "inbox.path";

  /** The configuration key to use for determining the polling interval in ms. */
  public static final String INBOX_POLL = "inbox.poll";

  private IngestService ingestService;
  private WorkingFileRepository workingFileRepository;
  private SecurityService securityService;
  private UserDirectoryService userDir;
  private OrganizationDirectoryService orgDir;

  private ComponentContext cc;

  private volatile Option<Ingestor> ingestor = none();
  private volatile Option<Configuration> fileInstallCfg = none();

  /** OSGi callback. */
  // synchronized with updated(Dictionary)
  public synchronized void activate(ComponentContext cc) {
    this.cc = cc;
  }

  /** OSGi callback. */
  public void deactivate() {
    fileInstallCfg.foreach(removeFileInstallCfg);
  }

  // synchronized with activate(ComponentContext)
  @Override
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    // build scanner configuration
    final String orgId = getCfg(properties, USER_ORG);
    final String userId = getCfg(properties, USER_NAME);
    final String workflowDefinition = getCfg(properties, WORKFLOW_DEFINITION);
    final Map<String, String> workflowConfig = getCfgAsMap(properties, WORKFLOW_CONFIG);
    final int interval = getCfgAsInt(properties, INBOX_POLL);
    final File inbox = new File(getCfg(properties, INBOX_PATH));
    if (!inbox.isDirectory()) {
      throw new ConfigurationException(INBOX_PATH, "%s does not exists".format(inbox.getAbsolutePath()));
    }
    if (!inbox.canRead()) {
      throw new ConfigurationException(INBOX_PATH, "Cannot read from %s".format(inbox.getAbsolutePath()));
    }
    final int maxthreads = option(cc.getBundleContext().getProperty("org.opencastproject.inbox.threads")).bind(Strings.toInt).getOrElse(1);
    final Option<SecurityContext> secCtx =
            getUserAndOrganization(securityService, orgDir, orgId, userDir, userId)
                    .bind(new Function<Tuple<User, Organization>, Option<SecurityContext>>() {
                      @Override
                      public Option<SecurityContext> apply(Tuple<User, Organization> a) {
                        return some(new SecurityContext(securityService, a.getB(), a.getA()));
                      }
                    });
    // Only setup new inbox if security context could be aquired
    if (secCtx.isSome()) {
      // remove old file install configuration
      fileInstallCfg.foreach(removeFileInstallCfg);
      // set up new file install config
      fileInstallCfg = some(configureFileInstall(cc.getBundleContext(), inbox, interval));
      // create new scanner
      ingestor = some(new Ingestor(ingestService, workingFileRepository, secCtx.get(), workflowDefinition,
              workflowConfig, inbox, maxthreads));
      logger.info("Now watching inbox {}", inbox.getAbsolutePath());
    } else {
      logger.warn("Cannot create security context for user {}, organization {}. "
                          + "Either the organization or the user does not exist", userId, orgId);
    }
  }

  public static final Effect<Configuration> removeFileInstallCfg = new Effect.X<Configuration>() {
    @Override
    protected void xrun(Configuration cfg) throws Exception {
      cfg.delete();
    }
  };

  /**
   * Setup an Apache FileInstall configuration for the inbox folder this scanner is responsible for.
   *
   * see section 104.4.1 Location Binding, paragraph 4, of the OSGi Spec 4.2
   * The correct permissions are needed in order to set configuration data for a bundle other than
   * the calling bundle itself.
   */
  public static Configuration configureFileInstall(BundleContext bc, File inbox, int interval) {
    final ServiceReference caRef = bc.getServiceReference(ConfigurationAdmin.class.getName());
    if (caRef == null) {
      throw new Error("Cannot obtain a reference to the ConfigurationAdmin service");
    }
    final Dictionary<String, String> fileInstallConfig = dict(
            tuple("felix.fileinstall.dir", inbox.getAbsolutePath()),
            tuple("felix.fileinstall.poll", Integer.toString(interval)));

    // update file install config with the new directory
    try {
      final String fileInstallBundleLocation =
              bc.getServiceReferences("org.osgi.service.cm.ManagedServiceFactory", "(service.pid=org.apache.felix.fileinstall)")[0]
                      .getBundle()
                      .getLocation();
      final Configuration conf = ((ConfigurationAdmin) bc.getService(caRef))
              .createFactoryConfiguration("org.apache.felix.fileinstall", fileInstallBundleLocation);
      conf.update(fileInstallConfig);
      return conf;
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  // --

  // FileInstall callback, called on a different thread
  // Attention: This method may be called _before_ the updated(Dictionary) which means that config parameters
  // are not set yet.
  @Override
  public boolean canHandle(final File artifact) {
    return ingestor.fmap(new Function<Ingestor, Boolean>() {
      @Override
      public Boolean apply(Ingestor ingestor) {
        return ingestor.canHandle(artifact);
      }
    }).getOrElse(false);
  }

  @Override
  public void install(final File artifact) throws Exception {
    ingestor.foreach(new Effect<Ingestor>() {
      @Override
      protected void run(Ingestor ingestor) {
        ingestor.ingest(artifact);
      }
    });
  }

  @Override
  public void update(File artifact) throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void uninstall(File artifact) throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  // --

  /** OSGi callback to set the ingest service. */
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /** OSGi callback to set the workspace */
  public void setWorkingFileRepository(WorkingFileRepository workingFileRepository) {
    this.workingFileRepository = workingFileRepository;
  }

  /** OSGi callback to set the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback to set the user directory. */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDir = userDirectoryService;
  }

  /** OSGi callback to set the organization directory server. */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.orgDir = organizationDirectoryService;
  }

  /**
   * Get a mandatory, non-blank value from a dictionary.
   *
   * @throws ConfigurationException
   *         key does not exist or its value is blank
   */
  public static String getCfg(Dictionary d, String key) throws ConfigurationException {
    Object p = d.get(key);
    if (p == null)
      throw new ConfigurationException(key, "does not exist");
    String ps = p.toString();
    if (StringUtils.isBlank(ps))
      throw new ConfigurationException(key, "is blank");
    return ps;
  }

  public static Map<String, String> getCfgAsMap(Dictionary<String, String> d, String key) throws ConfigurationException {
    HashMap<String, String> config = new HashMap<String, String>();
    for (Enumeration<String> e = d.keys(); e.hasMoreElements();) {
      String dKey = (String) e.nextElement();
      if (dKey.startsWith(key))
        config.put(dKey.substring(key.length() + 1), (String) d.get(dKey));
    }
    return config;
  }

  /**
   * Get a mandatory integer from a dictionary.
   *
   * @throws ConfigurationException
   *         key does not exist or is not an integer
   */
  public static int getCfgAsInt(Dictionary d, String key) throws ConfigurationException {
    try {
      return Integer.parseInt(getCfg(d, key));
    } catch (NumberFormatException e) {
      throw new ConfigurationException(key, "not an integer");
    }
  }
}
