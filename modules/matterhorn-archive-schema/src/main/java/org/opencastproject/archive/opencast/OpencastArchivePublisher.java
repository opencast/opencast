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
package org.opencastproject.archive.opencast;

import static org.opencastproject.util.data.Collections.cons;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.functions.Booleans.ne;
import static org.opencastproject.util.osgi.SimpleServicePublisher.ServiceReg.reg;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.base.jmx.ElementStoreBean;
import org.opencastproject.archive.base.persistence.AbstractArchiveDb;
import org.opencastproject.archive.base.persistence.ArchiveDb;
import org.opencastproject.archive.base.storage.ElementStore;
import org.opencastproject.archive.opencast.solr.SolrIndexManager;
import org.opencastproject.archive.opencast.solr.SolrRequester;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.VCell;
import org.opencastproject.util.jmx.JmxUtil;
import org.opencastproject.util.osgi.SimpleServicePublisher;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import javax.management.ObjectInstance;
import javax.persistence.spi.PersistenceProvider;

public class OpencastArchivePublisher extends SimpleServicePublisher {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(OpencastArchivePublisher.class);

  /** Configuration key for a remote solr server */
  public static final String CONFIG_SOLR_URL = "org.opencastproject.episode.solr.url";

  /** Configuration key for an embedded solr configuration and data directory */
  public static final String CONFIG_SOLR_ROOT = "org.opencastproject.episode.solr.dir";

  /** File system element store JMX type */
  private static final String JMX_ELEMENT_STORE_TYPE = "ElementStore";

  /** The JMX bean object instance */
  private ObjectInstance registeredMXBean;

  private VCell<List<StaticMetadataService>> metadataSvcs = VCell
          .<List<StaticMetadataService>> cell(new ArrayList<StaticMetadataService>());
  private Mpeg7CatalogService mpeg7CatalogService;
  private SeriesService seriesService;
  private Workspace workspace;
  private SecurityService securityService;
  private AuthorizationService authorizationService;
  private OrganizationDirectoryService orgDirectory;
  private ServiceRegistry serviceRegistry;
  private WorkflowService workflowService;
  private ElementStore elementStore;
  private OpencastArchive archive;
  private Map<String, ?> persistenceProperties;
  private PersistenceProvider persistenceProvider;
  private MessageSender messageSender;
  private MessageReceiver messageReceiver;

  public synchronized void setHttpMediaPackageElementProvider(
          HttpMediaPackageElementProvider httpMediaPackageElementProvider) {
    // Populate the search index if it is empty
    // bad approach but archive and its rest endpoint are in a cyclic dependency
    logger.info("Received HttpMediaPackageElementProvider");
    archive.populateIndex(httpMediaPackageElementProvider.getUriRewriter());
  }

  public void unsetHttpMediaPackageElementProvider(HttpMediaPackageElementProvider ignore) {
  }

  public void setStaticMetadataService(StaticMetadataService metadataSvc) {
    metadataSvcs.set(cons(metadataSvc, metadataSvcs.get()));
  }

  public void unsetStaticMetadataService(StaticMetadataService metadataSvc) {
    metadataSvcs.set(mlist(metadataSvcs.get()).filter(ne(metadataSvc)).value());
  }

  public void setMpeg7CatalogService(Mpeg7CatalogService mpeg7CatalogService) {
    this.mpeg7CatalogService = mpeg7CatalogService;
  }

  public void setPersistenceProperties(Map<String, ?> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  public void setElementStore(ElementStore elementStore) {
    this.elementStore = elementStore;
  }

  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  @Override
  public ServiceReg registerService(Dictionary properties, final ComponentContext cc) throws ConfigurationException {
    final String solrServerUrlConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_SOLR_URL));
    final SolrServer solrServer = new Function0<SolrServer>() {
      @Override
      public SolrServer apply() {
        if (solrServerUrlConfig != null) {
          try {
            URL solrServerUrl = new URL(solrServerUrlConfig);
            return setupSolr(solrServerUrl);
          } catch (Exception e) {
            throw connectError(solrServerUrlConfig, e);
          }
        } else if (cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT) != null) {
          String solrRoot = cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT);
          try {
            return setupSolr(new File(solrRoot));
          } catch (Exception e) {
            throw connectError(solrServerUrlConfig, e);
          }
        } else {
          String storageDir = cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
          if (storageDir == null)
            throw new IllegalStateException("Storage dir must be set (org.opencastproject.storage.dir)");
          String solrRoot = PathSupport.concat(storageDir, "archiveindex");
          try {
            return setupSolr(new File(solrRoot));
          } catch (Exception e) {
            throw connectError(solrServerUrlConfig, e);
          }
        }
      }

      IllegalStateException connectError(String target, Exception e) {
        return new IllegalStateException("Unable to connect to solr at " + target, e);
      }
    }.apply();
    final SolrRequester solrRequester = new SolrRequester(solrServer);
    final SolrIndexManager solrIndex = new SolrIndexManager(solrServer, workspace, metadataSvcs, seriesService,
            mpeg7CatalogService, securityService);
    final String systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    final PersistenceEnv penv = PersistenceUtil.newPersistenceEnvironment(persistenceProvider,
            "org.opencastproject.archive.base.persistence", persistenceProperties);
    final ArchiveDb persistence = new AbstractArchiveDb() {
      @Override
      protected PersistenceEnv getPenv() {
        return penv;
      }

      @Override
      protected SecurityService getSecurityService() {
        return securityService;
      }
    };
    archive = new OpencastArchive(solrIndex, solrRequester, securityService, authorizationService, orgDirectory,
            serviceRegistry, workflowService, workspace, persistence, elementStore, systemUserName, messageSender,
            messageReceiver);
    archive.activate();

    // the JMX file system element store bean
    final ElementStoreBean elementStoreBean = new ElementStoreBean(elementStore);
    registeredMXBean = JmxUtil.registerMXBean(elementStoreBean, JMX_ELEMENT_STORE_TYPE);
    final Effect0 shutdown = new Effect0() {
      @Override
      protected void run() {
        SolrServerFactory.shutdown(solrServer);
        JmxUtil.unregisterMXBean(registeredMXBean);
        penv.close();
      }
    };
    return reg(shutdown, registerService(cc, archive, Archive.class, "Archive"),
            registerService(cc, archive, OpencastArchive.class, "Opencast Archive"));
  }

  @Override
  public boolean needConfig() {
    return false;
  }

  /**
   * Prepares the embedded solr environment.
   *
   * @param solrRoot
   *          the solr root directory
   */
  public static SolrServer setupSolr(File solrRoot) throws IOException, SolrServerException {
    logger.info("Setting up solr search index at {}", solrRoot);
    File solrConfigDir = new File(solrRoot, "conf");

    // Create the config directory
    if (solrConfigDir.exists()) {
      logger.info("solr search index found at {}", solrConfigDir);
    } else {
      logger.info("solr config directory doesn't exist.  Creating {}", solrConfigDir);
      FileUtils.forceMkdir(solrConfigDir);
    }

    // Make sure there is a configuration in place
    copyClasspathResourceToFile("/solr/conf/protwords.txt", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/schema.xml", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/scripts.conf", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/solrconfig.xml", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/stopwords.txt", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/synonyms.txt", solrConfigDir);

    // Test for the existence of a data directory
    File solrDataDir = new File(solrRoot, "data");
    if (!solrDataDir.exists()) {
      FileUtils.forceMkdir(solrDataDir);
    }

    // Test for the existence of the index. Note that an empty index directory will prevent solr from
    // completing normal setup.
    File solrIndexDir = new File(solrDataDir, "index");
    if (solrIndexDir.exists() && solrIndexDir.list().length == 0) {
      FileUtils.deleteDirectory(solrIndexDir);
    }

    return SolrServerFactory.newEmbeddedInstance(solrRoot, solrDataDir);
  }

  /**
   * Prepares the embedded solr environment.
   *
   * @param url
   *          the url of the remote solr server
   */
  public static SolrServer setupSolr(URL url) throws IOException, SolrServerException {
    logger.info("Connecting to solr search index at {}", url);
    return SolrServerFactory.newRemoteInstance(url);
  }

  // TODO: generalize this method
  public static void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = null;
    FileOutputStream fos = null;
    try {
      in = OpencastArchivePublisher.class.getResourceAsStream(classpath);
      File file = new File(dir, FilenameUtils.getName(classpath));
      logger.debug("copying " + classpath + " to " + file);
      fos = new FileOutputStream(file);
      IOUtils.copy(in, fos);
    } catch (IOException e) {
      throw new RuntimeException("Error copying solr classpath resource to the filesystem", e);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(fos);
    }
  }

  @Override
  public void activate(ComponentContext cc) throws ConfigurationException {
    super.activate(cc);
  }

  @Override
  public void deactivate() {
    super.deactivate();
  }

}
