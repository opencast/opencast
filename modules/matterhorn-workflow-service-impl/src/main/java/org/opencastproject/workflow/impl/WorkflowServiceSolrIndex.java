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
package org.opencastproject.workflow.impl;

import static org.apache.solr.client.solrj.util.ClientUtils.escapeQueryChars;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.workflow.api.WorkflowService.READ_PERMISSION;
import static org.opencastproject.workflow.api.WorkflowService.WRITE_PERMISSION;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.JobUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowQuery.QueryTerm;
import org.opencastproject.workflow.api.WorkflowQuery.Sort;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowSetImpl;
import org.opencastproject.workflow.api.WorkflowStatistics;
import org.opencastproject.workflow.api.WorkflowStatistics.WorkflowDefinitionReport;
import org.opencastproject.workflow.api.WorkflowStatistics.WorkflowDefinitionReport.OperationReport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides data access to the workflow service through file storage in the workspace, indexed via solr.
 */
public class WorkflowServiceSolrIndex implements WorkflowServiceIndex {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(WorkflowServiceSolrIndex.class);

  /** Configuration key for a remote solr server */
  public static final String CONFIG_SOLR_URL = "org.opencastproject.workflow.solr.url";

  /** Configuration key for an embedded solr configuration and data directory */
  public static final String CONFIG_SOLR_ROOT = "org.opencastproject.workflow.solr.dir";

  /** Connection to the solr server. Solr is used to search for workflows. The workflow data are stored as xml files. */
  protected SolrServer solrServer = null;

  /** The root directory to use for solr config and data files */
  protected String solrRoot = null;

  /** The URL to connect to a remote solr server */
  protected URL solrServerUrl = null;

  /** The key in solr documents representing the workflow's current operation */
  protected static final String OPERATION_KEY = "operation";

  /** The <code>OPERATION_KEY</code> value used when a workflow has no current operation */
  protected static final String NO_OPERATION_KEY = "none";

  /** The key in solr documents representing the workflow definition id */
  protected static final String WORKFLOW_DEFINITION_KEY = "templateid";

  /** The key in solr documents representing the workflow's series identifier */
  protected static final String SERIES_ID_KEY = "seriesid";

  /** The key in solr documents representing the workflow's series title */
  protected static final String SERIES_TITLE_KEY = "seriestitle";

  /** The key in solr documents representing the workflow's ID */
  protected static final String ID_KEY = "id";

  /** The key in solr documents representing the workflow's current state */
  private static final String STATE_KEY = "state";

  /** The key in solr documents representing the workflow as xml */
  private static final String XML_KEY = "xml";

  /** The key in solr documents representing the workflow's contributors */
  private static final String CONTRIBUTOR_KEY = "contributor";

  /** The key in solr documents representing the workflow's mediapackage language */
  private static final String LANGUAGE_KEY = "language";

  /** The key in solr documents representing the workflow's mediapackage license */
  private static final String LICENSE_KEY = "license";

  /** The key in solr documents representing the workflow's mediapackage title */
  private static final String TITLE_KEY = "title";

  /** The key in solr documents representing the workflow's mediapackage identifier */
  private static final String MEDIAPACKAGE_KEY = "mediapackageid";

  /** The key in solr documents representing the workflow's mediapackage creators */
  private static final String CREATOR_KEY = "creator";

  /** The key in solr documents representing the workflow's mediapackage creation date */
  private static final String CREATED_KEY = "created";

  /** The key in solr documents representing the workflow's mediapackage subjects */
  private static final String SUBJECT_KEY = "subject";

  /** The key in solr documents representing the full text index */
  private static final String FULLTEXT_KEY = "fulltext";

  /**
   * The key in solr documents representing the workflow's creator. This, along with the ACLs, are used for determining
   * whether a user can view a workflow or not.
   */
  private static final String WORKFLOW_CREATOR_KEY = "oc_creator";

  /** The key in solr documents representing the organization that owns this workflow instance */
  private static final String ORG_KEY = "oc_org";

  /** The key in solr documents representing the prefix to an access control entry */
  private static final String ACL_KEY_PREFIX = "oc_acl_";

  /** The service registry, managing jobs */
  private ServiceRegistry serviceRegistry = null;

  /** The authorization service */
  private AuthorizationService authorizationService = null;

  /** The organization directory */
  private OrganizationDirectoryService orgDirectory;

  /** The security service */
  private SecurityService securityService = null;

  /** Whether to index workflows synchronously as they are stored */
  protected boolean synchronousIndexing = true;

  /** The thread pool to use in asynchronous indexing */
  protected ExecutorService indexingExecutor;

  /**
   * Callback from the OSGi environment on component registration. The indexing behavior can be set using component
   * context properties. <code>synchronousIndexing=true|false</code> determines whether threads performing workflow
   * updates block on adding the workflow instances to the search index.
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    String solrServerUrlConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_SOLR_URL));
    if (solrServerUrlConfig != null) {
      try {
        solrServerUrl = new URL(solrServerUrlConfig);
      } catch (MalformedURLException e) {
        throw new IllegalStateException("Unable to connect to solr at " + solrServerUrlConfig, e);
      }
    } else if (cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT) != null) {
      solrRoot = cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT);
    } else {
      String storageDir = cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
      if (storageDir == null)
        throw new IllegalStateException("Storage dir must be set (org.opencastproject.storage.dir)");
      solrRoot = PathSupport.concat(storageDir, "workflowindex");
    }
    Object syncIndexingConfig = cc.getProperties().get("synchronousIndexing");
    if (syncIndexingConfig != null && (syncIndexingConfig instanceof Boolean)) {
      this.synchronousIndexing = (Boolean) syncIndexingConfig;
    }
    if (this.synchronousIndexing) {
      logger.debug("Workflows will be added to the search index synchronously");
    } else {
      logger.debug("Workflows will be added to the search index asynchronously");
      indexingExecutor = Executors.newSingleThreadExecutor();
    }
    String systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    activate(systemUserName);
  }

  private long count() throws WorkflowDatabaseException {
    try {
      QueryResponse response = solrServer.query(new SolrQuery("*:*"));
      return response.getResults().getNumFound();
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * Activates the index by configuring solr with the server url that must have been set previously.
   */
  public void activate(String systemUserName) {
    // Set up the solr server
    if (solrServerUrl != null) {
      solrServer = SolrServerFactory.newRemoteInstance(solrServerUrl);
    } else {
      try {
        setupSolr(new File(solrRoot));
      } catch (IOException e) {
        throw new IllegalStateException("Unable to connect to solr at " + solrRoot, e);
      } catch (SolrServerException e) {
        throw new IllegalStateException("Unable to connect to solr at " + solrRoot, e);
      }
    }

    // If the solr is empty, add all of the existing workflows
    long instancesInSolr = 0;
    try {
      instancesInSolr = count();
    } catch (WorkflowDatabaseException e) {
      throw new IllegalStateException(e);
    }

    if (instancesInSolr == 0) {
      logger.info("The workflow index is empty, looking for workflows to index");
      // this may be a new index, so get all of the existing workflows and index them
      List<Job> jobs = null;
      try {
        jobs = serviceRegistry.getJobs(WorkflowService.JOB_TYPE, null);
        Iterator<Job> ji = jobs.iterator();
        while (ji.hasNext()) {
          Job job = ji.next();
          if (!WorkflowServiceImpl.Operation.START_WORKFLOW.toString().equals(job.getOperation())) {
            logger.debug("Removing unrelated job {} of type {}", job.getId(), job.getOperation());
            ji.remove();
          }
        }
      } catch (ServiceRegistryException e) {
        logger.error("Unable to load the workflows jobs: {}", e.getMessage());
        throw new ServiceException(e.getMessage());
      }

      if (jobs.size() > 0) {
        logger.info("Populating the workflow index with {} workflows", jobs.size());
        int errors = 0;
        for (Job job : jobs) {
          if (job.getPayload() == null) {
            logger.warn("Skipping restoring of workflow {}: Payload is empty", job.getId());
            continue;
          }
          WorkflowInstance instance = null;
          boolean erroneousWorkflowJob = false;
          try {
            instance = WorkflowParser.parseWorkflowInstance(job.getPayload());
            Organization organization = orgDirectory.getOrganization(job.getOrganization());
            securityService.setOrganization(organization);
            securityService.setUser(SecurityUtil.createSystemUser(systemUserName, organization));
            index(instance);
          } catch (WorkflowDatabaseException e) {
            logger.warn("Skipping restoring of workflow {}: {}", instance.getId(), e.getMessage());
            erroneousWorkflowJob = true;
            errors++;
          } catch (Throwable e) {
            logger.warn("Skipping restoring of workflow {}: {}", instance.getId(), e.getMessage());
            erroneousWorkflowJob = true;
            errors++;
          }

          // Make sure this job is not being dispatched anymore
          if (erroneousWorkflowJob && JobUtil.isReadyToDispatch(job)) {
            job.setStatus(Job.Status.CANCELED);
            try {
              serviceRegistry.updateJob(job);
              logger.info("Canceled job {} because unable to restore", job);
            } catch (Exception e) {
              logger.error("Error updating erroneous job {}: {}", job.getId(), e.getMessage());
            }
          }
        }

        if (errors > 0)
          logger.warn("Skipped {} erroneous workflows while populating the index", errors);
        logger.info("Finished populating the workflow search index");
      }
    }
  }

  /**
   * Prepares the embedded solr environment.
   *
   * @param solrRoot
   *          the solr root directory
   */
  protected void setupSolr(File solrRoot) throws IOException, SolrServerException {
    logger.debug("Setting up solr search index at {}", solrRoot);
    File solrConfigDir = new File(solrRoot, "conf");

    // Create the config directory
    if (solrConfigDir.exists()) {
      logger.debug("solr search index found at {}", solrConfigDir);
    } else {
      logger.debug("solr config directory doesn't exist.  Creating {}", solrConfigDir);
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
    if (solrIndexDir.isDirectory() && solrIndexDir.list().length == 0) {
      FileUtils.deleteDirectory(solrIndexDir);
    }

    solrServer = SolrServerFactory.newEmbeddedInstance(solrRoot, solrDataDir);
  }

  /**
   * Shuts down the solr index.
   */
  public void deactivate() {
    SolrServerFactory.shutdown(solrServer);
  }

  // TODO: generalize this method
  private void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = null;
    FileOutputStream fos = null;
    try {
      in = WorkflowServiceSolrIndex.class.getResourceAsStream(classpath);
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

  public void index(final WorkflowInstance instance) throws WorkflowDatabaseException {
    if (synchronousIndexing) {
      try {
        SolrInputDocument doc = createDocument(instance);
        synchronized (solrServer) {
          solrServer.add(doc);
          solrServer.commit();
        }
      } catch (Exception e) {
        throw new WorkflowDatabaseException("Unable to index workflow", e);
      }
    } else {
      indexingExecutor.submit(new Runnable() {
        public void run() {
          try {
            SolrInputDocument doc = createDocument(instance);
            synchronized (solrServer) {
              solrServer.add(doc);
              // Use solr's autoCommit feature instead of committing on each document addition.
              // See http://opencast.jira.com/browse/MH-7040 and
              // http://osdir.com/ml/solr-user.lucene.apache.org/2009-09/msg00744.html
              // solrServer.commit();
            }
          } catch (Exception e) {
            WorkflowServiceSolrIndex.logger.warn("Unable to index {}: {}", instance, e);
          }
        }
      });
    }
  }

  /**
   * Adds the workflow instance to the search index.
   *
   * @param instance
   *          the instance
   * @return the solr input document
   * @throws Exception
   */
  protected SolrInputDocument createDocument(WorkflowInstance instance) throws Exception {
    SolrInputDocument doc = new SolrInputDocument();
    doc.addField(ID_KEY, instance.getId());
    doc.addField(WORKFLOW_DEFINITION_KEY, instance.getTemplate());
    doc.addField(STATE_KEY, instance.getState().toString());
    String xml = WorkflowParser.toXml(instance);
    doc.addField(XML_KEY, xml);

    // index the current operation if there is one. If the workflow is finished, there is no current operation, so use a
    // constant
    WorkflowOperationInstance op = instance.getCurrentOperation();
    if (op == null) {
      doc.addField(OPERATION_KEY, NO_OPERATION_KEY);
    } else {
      doc.addField(OPERATION_KEY, op.getTemplate());
    }

    MediaPackage mp = instance.getMediaPackage();
    doc.addField(MEDIAPACKAGE_KEY, mp.getIdentifier().toString());
    if (mp.getSeries() != null) {
      doc.addField(SERIES_ID_KEY, mp.getSeries());
    }
    if (mp.getSeriesTitle() != null) {
      doc.addField(SERIES_TITLE_KEY, mp.getSeriesTitle());
    }
    if (mp.getDate() != null) {
      doc.addField(CREATED_KEY, mp.getDate());
    }
    if (mp.getTitle() != null) {
      doc.addField(TITLE_KEY, mp.getTitle());
    }
    if (mp.getLicense() != null) {
      doc.addField(LICENSE_KEY, mp.getLicense());
    }
    if (mp.getLanguage() != null) {
      doc.addField(LANGUAGE_KEY, mp.getLanguage());
    }
    if (mp.getContributors() != null && mp.getContributors().length > 0) {
      StringBuffer buf = new StringBuffer();
      for (String contributor : mp.getContributors()) {
        if (buf.length() > 0)
          buf.append("; ");
        buf.append(contributor);
      }
      doc.addField(CONTRIBUTOR_KEY, buf.toString());
    }
    if (mp.getCreators() != null && mp.getCreators().length > 0) {
      StringBuffer buf = new StringBuffer();
      for (String creator : mp.getCreators()) {
        if (buf.length() > 0)
          buf.append("; ");
        buf.append(creator);
      }
      doc.addField(CREATOR_KEY, buf.toString());
    }
    if (mp.getSubjects() != null && mp.getSubjects().length > 0) {
      StringBuffer buf = new StringBuffer();
      for (String subject : mp.getSubjects()) {
        if (buf.length() > 0)
          buf.append("; ");
        buf.append(subject);
      }
      doc.addField(SUBJECT_KEY, buf.toString());
    }

    User workflowCreator = instance.getCreator();
    doc.addField(WORKFLOW_CREATOR_KEY, workflowCreator.getUsername());
    doc.addField(ORG_KEY, instance.getOrganization().getId());

    AccessControlList acl;
    try {
      acl = authorizationService.getActiveAcl(mp).getA();
    } catch (Error e) {
      logger.error("No security xacml found on media package {}", mp);
      throw new WorkflowException(e);
    }
    addAuthorization(doc, acl);

    return doc;
  }

  /**
   * Adds authorization fields to the solr document.
   *
   * @param doc
   *          the solr document
   * @param acl
   *          the access control list
   */
  protected void addAuthorization(SolrInputDocument doc, AccessControlList acl) {
    Map<String, List<String>> permissions = new HashMap<String, List<String>>();

    // Define containers for common permissions
    List<String> reads = new ArrayList<String>();
    permissions.put(READ_PERMISSION, reads);
    List<String> writes = new ArrayList<String>();
    permissions.put(WRITE_PERMISSION, writes);

    String adminRole = securityService.getOrganization().getAdminRole();

    // The admin user can read and write
    if (adminRole != null) {
      reads.add(adminRole);
      writes.add(adminRole);
    }

    for (AccessControlEntry entry : acl.getEntries()) {
      if (!entry.isAllow()) {
        logger.warn("Workflow service does not support denial via ACL, ignoring {}", entry);
        continue;
      }
      List<String> actionPermissions = permissions.get(entry.getAction());
      if (actionPermissions == null) {
        actionPermissions = new ArrayList<String>();
        permissions.put(entry.getAction(), actionPermissions);
      }
      actionPermissions.add(entry.getRole());
    }

    // Write the permissions to the solr document
    for (Map.Entry<String, List<String>> entry : permissions.entrySet()) {
      String fieldName = ACL_KEY_PREFIX + entry.getKey();
      doc.setField(fieldName, entry.getValue());
    }

  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.impl.WorkflowServiceIndex#countWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstance.WorkflowState,
   *      java.lang.String)
   */
  @Override
  public long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException {
    StringBuilder query = new StringBuilder();

    // Consider the workflow state
    if (state != null) {
      query.append(STATE_KEY).append(":").append(escapeQueryChars(state.toString()));
    }

    // Consider the current operation
    if (StringUtils.isNotBlank(operation)) {
      if (query.length() > 0)
        query.append(" AND ");
      query.append(OPERATION_KEY).append(":").append(escapeQueryChars(operation));
    }

    // We want all available workflows for this organization
    String orgId = securityService.getOrganization().getId();
    if (query.length() > 0)
      query.append(" AND ");
    query.append(ORG_KEY).append(":").append(escapeQueryChars(orgId));

    appendSolrAuthFragment(query, READ_PERMISSION);

    try {
      QueryResponse response = solrServer.query(new SolrQuery(query.toString()));
      return response.getResults().getNumFound();
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.impl.WorkflowServiceIndex#getStatistics()
   */
  @Override
  public WorkflowStatistics getStatistics() throws WorkflowDatabaseException {

    long total = 0;
    long paused = 0;
    long failed = 0;
    long failing = 0;
    long instantiated = 0;
    long running = 0;
    long stopped = 0;
    long succeeded = 0;

    WorkflowStatistics stats = new WorkflowStatistics();

    // Get all definitions and then query for the numbers and the current operation per definition
    try {
      String orgId = securityService.getOrganization().getId();
      StringBuilder queryString = new StringBuilder().append(ORG_KEY).append(":").append(escapeQueryChars(orgId));
      appendSolrAuthFragment(queryString, WRITE_PERMISSION);
      SolrQuery solrQuery = new SolrQuery(queryString.toString());
      solrQuery.addFacetField(WORKFLOW_DEFINITION_KEY);
      solrQuery.addFacetField(OPERATION_KEY);
      solrQuery.setFacetMinCount(0);
      solrQuery.setFacet(true);
      QueryResponse response = solrServer.query(solrQuery);

      FacetField templateFacet = response.getFacetField(WORKFLOW_DEFINITION_KEY);
      FacetField operationFacet = response.getFacetField(OPERATION_KEY);

      // For every template and every operation
      if (templateFacet != null && templateFacet.getValues() != null) {

        for (Count template : templateFacet.getValues()) {

          WorkflowDefinitionReport templateReport = new WorkflowDefinitionReport();
          templateReport.setId(template.getName());

          long templateTotal = 0;
          long templatePaused = 0;
          long templateFailed = 0;
          long templateFailing = 0;
          long templateInstantiated = 0;
          long templateRunning = 0;
          long templateStopped = 0;
          long templateSucceeded = 0;

          if (operationFacet != null && operationFacet.getValues() != null) {

            for (Count operation : operationFacet.getValues()) {

              OperationReport operationReport = new OperationReport();
              operationReport.setId(operation.getName());

              StringBuilder baseSolrQuery = new StringBuilder().append(ORG_KEY).append(":")
                      .append(escapeQueryChars(orgId));
              appendSolrAuthFragment(baseSolrQuery, WRITE_PERMISSION);
              solrQuery = new SolrQuery(baseSolrQuery.toString());
              solrQuery.addFacetField(STATE_KEY);
              solrQuery.addFacetQuery(STATE_KEY + ":" + WorkflowState.FAILED);
              solrQuery.addFacetQuery(STATE_KEY + ":" + WorkflowState.FAILING);
              solrQuery.addFacetQuery(STATE_KEY + ":" + WorkflowState.INSTANTIATED);
              solrQuery.addFacetQuery(STATE_KEY + ":" + WorkflowState.PAUSED);
              solrQuery.addFacetQuery(STATE_KEY + ":" + WorkflowState.RUNNING);
              solrQuery.addFacetQuery(STATE_KEY + ":" + WorkflowState.STOPPED);
              solrQuery.addFacetQuery(STATE_KEY + ":" + WorkflowState.SUCCEEDED);
              solrQuery.addFilterQuery(WORKFLOW_DEFINITION_KEY + ":" + template.getName());
              solrQuery.addFilterQuery(OPERATION_KEY + ":" + operation.getName());
              solrQuery.setFacetMinCount(0);
              solrQuery.setFacet(true);

              response = solrServer.query(solrQuery);

              // Add the states
              FacetField stateFacet = response.getFacetField(STATE_KEY);
              for (Count stateValue : stateFacet.getValues()) {
                WorkflowState state = WorkflowState.valueOf(stateValue.getName().toUpperCase());
                templateTotal += stateValue.getCount();
                total += stateValue.getCount();
                switch (state) {
                  case FAILED:
                    operationReport.setFailed(stateValue.getCount());
                    templateFailed += stateValue.getCount();
                    failed += stateValue.getCount();
                    break;
                  case FAILING:
                    operationReport.setFailing(stateValue.getCount());
                    templateFailing += stateValue.getCount();
                    failing += stateValue.getCount();
                    break;
                  case INSTANTIATED:
                    operationReport.setInstantiated(stateValue.getCount());
                    templateInstantiated += stateValue.getCount();
                    instantiated += stateValue.getCount();
                    break;
                  case PAUSED:
                    operationReport.setPaused(stateValue.getCount());
                    templatePaused += stateValue.getCount();
                    paused += stateValue.getCount();
                    break;
                  case RUNNING:
                    operationReport.setRunning(stateValue.getCount());
                    templateRunning += stateValue.getCount();
                    running += stateValue.getCount();
                    break;
                  case STOPPED:
                    operationReport.setStopped(stateValue.getCount());
                    templateStopped += stateValue.getCount();
                    stopped += stateValue.getCount();
                    break;
                  case SUCCEEDED:
                    operationReport.setFinished(stateValue.getCount());
                    templateSucceeded += stateValue.getCount();
                    succeeded += stateValue.getCount();
                    break;
                  default:
                    throw new IllegalStateException("State '" + state + "' is not handled");
                }
              }

              templateReport.getOperations().add(operationReport);
            }
          }

          // Update the template statistics
          templateReport.setTotal(templateTotal);
          templateReport.setFailed(templateFailed);
          templateReport.setFailing(templateFailing);
          templateReport.setInstantiated(templateInstantiated);
          templateReport.setPaused(templatePaused);
          templateReport.setRunning(templateRunning);
          templateReport.setStopped(templateStopped);
          templateReport.setFinished(templateSucceeded);

          // Add the definition report to the statistics
          stats.getDefinitions().add(templateReport);

        }

      }
    } catch (SolrServerException e) {
      throw new WorkflowDatabaseException(e);
    }

    stats.setTotal(total);
    stats.setFailed(failed);
    stats.setFailing(failing);
    stats.setInstantiated(instantiated);
    stats.setPaused(paused);
    stats.setRunning(running);
    stats.setStopped(stopped);
    stats.setFinished(succeeded);

    return stats;
  }

  /**
   * Appends query parameters to a solr query
   *
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @param value
   *          the value for this search parameter
   * @param toLowerCase
   *          <code>true</code> to convert the value to lower case prior to comparison
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder append(StringBuilder sb, String key, String value, boolean toLowerCase) {
    if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
      return sb;
    }
    if (sb.length() > 0) {
      sb.append(" AND ");
    }
    sb.append(key);
    sb.append(":");
    if (toLowerCase)
      sb.append(escapeQueryChars(value.toLowerCase()));
    else
      sb.append(escapeQueryChars(value));
    return sb;
  }

  /**
   * Appends query parameters to a solr query in a way that they are found even though they are not treated as a full
   * word in solr.
   *
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @param value
   *          the value for this search parameter
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder appendFuzzy(StringBuilder sb, String key, String value) {
    if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
      return sb;
    }
    if (sb.length() > 0) {
      sb.append(" AND ");
    }
    sb.append("(");
    sb.append(key).append(":").append(escapeQueryChars(value.toLowerCase()));
    sb.append(" OR ");
    sb.append(key).append(":*").append(escapeQueryChars(value.toLowerCase())).append("*");
    sb.append(")");
    return sb;
  }

  /**
   * Appends query parameters to a solr query
   *
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder append(StringBuilder sb, String key, Date startDate, Date endDate) {
    if (StringUtils.isBlank(key) || (startDate == null && endDate == null)) {
      return sb;
    }
    if (sb.length() > 0) {
      sb.append(" AND ");
    }
    if (startDate == null)
      startDate = new Date(0);
    if (endDate == null)
      endDate = new Date(Long.MAX_VALUE);
    sb.append(key);
    sb.append(":");
    sb.append(SolrUtils.serializeDateRange(option(startDate), option(endDate)));
    return sb;
  }

  /**
   * Builds a solr search query from a {@link WorkflowQuery}.
   *
   * @param query
   *          the workflow query
   * @param action
   *          one of {@link org.opencastproject.workflow.api.WorkflowService#READ_PERMISSION},
   *          {@link org.opencastproject.workflow.api.WorkflowService#WRITE_PERMISSION}
   * @param applyPermissions
   *          whether to apply the permissions to the query. Set to false for administrative queries.
   * @return the solr query string
   */
  protected String createQuery(WorkflowQuery query, String action, boolean applyPermissions)
          throws WorkflowDatabaseException {
    String orgId = securityService.getOrganization().getId();
    StringBuilder sb = new StringBuilder().append(ORG_KEY).append(":").append(escapeQueryChars(orgId));
    append(sb, ID_KEY, query.getId(), false);
    append(sb, MEDIAPACKAGE_KEY, query.getMediaPackageId(), false);
    append(sb, SERIES_ID_KEY, query.getSeriesId(), false);
    appendFuzzy(sb, SERIES_TITLE_KEY, query.getSeriesTitle());
    appendFuzzy(sb, FULLTEXT_KEY, query.getText());
    append(sb, WORKFLOW_DEFINITION_KEY, query.getWorkflowDefinitionId(), false);
    append(sb, CREATED_KEY, query.getFromDate(), query.getToDate());
    appendFuzzy(sb, CREATOR_KEY, query.getCreator());
    appendFuzzy(sb, CONTRIBUTOR_KEY, query.getContributor());
    append(sb, LANGUAGE_KEY, query.getLanguage(), true);
    append(sb, LICENSE_KEY, query.getLicense(), true);
    appendFuzzy(sb, TITLE_KEY, query.getTitle());
    appendFuzzy(sb, SUBJECT_KEY, query.getSubject());
    appendMap(sb, OPERATION_KEY, query.getCurrentOperations());
    appendMap(sb, STATE_KEY, query.getStates());

    if (applyPermissions) {
      appendSolrAuthFragment(sb, action);
    }

    // Limit the results to only those workflow instances the current user can read

    logger.debug(sb.toString());

    return sb.toString();
  }

  protected void appendSolrAuthFragment(StringBuilder sb, String action) throws WorkflowDatabaseException {
    User user = securityService.getUser();
    if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole())) {
      sb.append(" AND ").append(ORG_KEY).append(":")
              .append(escapeQueryChars(securityService.getOrganization().getId()));
      Set<Role> roles = user.getRoles();
      if (roles.size() > 0) {
        sb.append(" AND (").append(WORKFLOW_CREATOR_KEY).append(":").append(escapeQueryChars(user.getUsername()));
        for (Role role : roles) {
          sb.append(" OR ");
          sb.append(ACL_KEY_PREFIX).append(action).append(":").append(escapeQueryChars(role.getName()));
        }
        sb.append(")");
      }
    }
  }

  /**
   * Returns the search index' field name that corresponds to the sort field.
   *
   * @param sort
   *          the sort field
   * @return the field name in the search index
   */
  protected String getSortField(Sort sort) {
    switch (sort) {
      case TITLE:
        return TITLE_KEY;
      case CONTRIBUTOR:
        return CONTRIBUTOR_KEY;
      case DATE_CREATED:
        return CREATED_KEY;
      case CREATOR:
        return CREATOR_KEY;
      case LANGUAGE:
        return LANGUAGE_KEY;
      case LICENSE:
        return LICENSE_KEY;
      case MEDIA_PACKAGE_ID:
        return MEDIAPACKAGE_KEY;
      case SERIES_ID:
        return SERIES_ID_KEY;
      case SERIES_TITLE:
        return SERIES_TITLE_KEY;
      case SUBJECT:
        return SUBJECT_KEY;
      case WORKFLOW_DEFINITION_ID:
        return WORKFLOW_DEFINITION_KEY;
      default:
        throw new IllegalArgumentException("No mapping found between sort field and index");
    }
  }

  /**
   * Appends query parameters from a {@link java.util.Map} to a solr query. The map
   *
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @return the appended {@link StringBuilder}
   */
  protected StringBuilder appendMap(StringBuilder sb, String key, List<QueryTerm> queryTerms) {
    if (queryTerms == null || queryTerms.isEmpty()) {
      return sb;
    }
    if (sb.length() > 0) {
      sb.append(" AND ");
    }
    // If we include only negatives inside parentheses, lucene won't return any results. So we need to add "*:*".
    // See
    // http://mail-archives.apache.org/mod_mbox/lucene-solr-user/201011.mbox/%3CAANLkTinTJLo7Y-W2kt+yxAcESf98p8DD7z7mrs4CpNo-@mail.gmail.com%3E
    boolean positiveTerm = false;
    sb.append("(");
    for (int i = 0; i < queryTerms.size(); i++) {
      QueryTerm term = queryTerms.get(i);
      if (i > 0) {
        if (term.isInclude()) {
          sb.append(" OR ");
        } else {
          sb.append(" AND ");
        }
      }
      if (term.isInclude()) {
        positiveTerm = true;
      } else {
        sb.append("-");
      }
      sb.append(key);
      sb.append(":");
      sb.append(escapeQueryChars(term.getValue().toLowerCase()));
    }
    if (!positiveTerm) {
      sb.append(" AND *:*");
    }
    sb.append(")");
    return sb;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.impl.WorkflowServiceIndex#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery,
   *      String, boolean)
   */
  @Override
  public WorkflowSet getWorkflowInstances(WorkflowQuery query, String action, boolean applyPermissions)
          throws WorkflowDatabaseException {
    int count = query.getCount() > 0 ? (int) query.getCount() : 20; // default to 20 items if not specified
    int startPage = query.getStartPage() > 0 ? (int) query.getStartPage() : 0; // default to page zero

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setRows(count);
    solrQuery.setStart(startPage * count);

    String solrQueryString = createQuery(query, action, applyPermissions);
    solrQuery.setQuery(solrQueryString);

    if (query.getSort() != null) {
      ORDER order = query.isSortAscending() ? ORDER.asc : ORDER.desc;
      solrQuery.addSortField(getSortField(query.getSort()) + "_sort", order);
    }

    if (!Sort.DATE_CREATED.equals(query.getSort())) {
      solrQuery.addSortField(getSortField(Sort.DATE_CREATED) + "_sort", ORDER.desc);
    }

    long totalHits;
    long time = System.currentTimeMillis();
    WorkflowSetImpl set = null;
    try {
      QueryResponse response = solrServer.query(solrQuery);
      SolrDocumentList items = response.getResults();
      long searchTime = System.currentTimeMillis() - time;
      totalHits = items.getNumFound();

      set = new WorkflowSetImpl();
      set.setPageSize(count);
      set.setTotalCount(totalHits);
      set.setStartPage(query.getStartPage());
      set.setSearchTime(searchTime);

      // Iterate through the results
      for (SolrDocument doc : items) {
        String xml = (String) doc.get(XML_KEY);
        try {
          set.addItem(WorkflowParser.parseWorkflowInstance(xml));
        } catch (Exception e) {
          throw new IllegalStateException("can not parse workflow xml", e);
        }
      }
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
    long totalTime = System.currentTimeMillis() - time;
    logger.debug("Workflow query took {} ms", totalTime);
    return set;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.impl.WorkflowServiceIndex#remove(long)
   */
  @Override
  public void remove(long id) throws WorkflowDatabaseException, NotFoundException {
    try {
      synchronized (solrServer) {
        solrServer.deleteById(Long.toString(id));
        solrServer.commit();
      }
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.impl.WorkflowServiceIndex#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void update(WorkflowInstance instance) throws WorkflowDatabaseException {
    index(instance);
  }

  /**
   * Clears the index of all workflow instances.
   */
  public void clear() throws WorkflowDatabaseException {
    try {
      synchronized (solrServer) {
        solrServer.deleteByQuery("*:*");
        solrServer.commit();
      }
    } catch (Exception e) {
      throw new WorkflowDatabaseException(e);
    }
  }

  /**
   * Callback for the OSGi environment to register with the <code>ServiceRegistry</code>.
   *
   * @param registry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry registry) {
    this.serviceRegistry = registry;
  }

  /**
   * Callback for setting the organization directory service.
   *
   * @param orgDirectory
   *          the organization directory service
   */
  protected void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  /**
   * Callback for setting the authorization service.
   *
   * @param authorizationService
   *          the authorizationService to set
   */
  protected void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
