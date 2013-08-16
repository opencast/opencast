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
package org.opencastproject.migration;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

/**
 * This class provides migration index and DB migrations to Matterhorn.
 */
public class WorkflowMigrationService {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowMigrationService.class);

  /** The service registry */
  private ServiceRegistry serviceRegistry = null;

  /** working file repository URL */
  protected String wfrUrl;

  /** The download URL */
  protected String downloadUrl;

  /**
   * Callback for setting the service registry
   * 
   * @param serviceRegistry
   *          the service registry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Migrates Matterhorn 1.3 workflow index to a 1.4 index and DB
   */
  public void activate(ComponentContext cc) throws IOException {
    downloadUrl = cc.getBundleContext().getProperty("org.opencastproject.download.url");
    wfrUrl = UrlSupport.concat(cc.getBundleContext().getProperty("org.opencastproject.server.url"), "files",
            "mediapackage");

    logger.info("Start migration 1.3 workflow DB to 1.4 workflow DB");
    List<Job> jobs = null;
    try {
      jobs = serviceRegistry.getJobs(WorkflowService.JOB_TYPE, null);
    } catch (ServiceRegistryException e) {
      logger.error("Unable to load the workflows jobs: {}", e.getMessage());
      throw new ServiceException(e.getMessage());
    }

    if (jobs.size() > 0) {
      logger.info("Found {} total workflow service items to migrate", jobs.size());
      int totalMigrated = 0;
      int failed = 0;
      for (Job job : jobs) {
        if (job.getPayload() == null) {
          totalMigrated++;
          logger.info("Skiped migrating null payload job '{}' ({}/{})",
                  new Object[] { job.getId(), totalMigrated, jobs.size() });
          continue;
        }
        WorkflowInstance instance = null;
        try {
          WorkflowInstance instance14 = WorkflowParser.parseWorkflowInstance(job.getPayload());
          if (instance14.getMediaPackage() != null && instance14.getTitle() != null) {
            totalMigrated++;
            logger.info("Skiped migrating already migrated job '{}' ({}/{})", new Object[] { job.getId(),
                    totalMigrated, jobs.size() });
            continue;
          }
          instance = parseOldWorkflowInstance(job.getPayload(), job.getOrganization());
          job.setPayload(WorkflowParser.toXml(instance));
          serviceRegistry.updateJob(job);
          totalMigrated++;
          logger.info("Successfully migrated {} ({}/{})", new Object[] { instance.getId(), totalMigrated, jobs.size() });
        } catch (WorkflowParsingException e) {
          logger.error("Unable to parse workflow job {}: {}", instance.getId(), e.getMessage());
          failed++;
        } catch (NotFoundException e) {
          throw new IllegalStateException("Previous loaded job can not be found: " + job.getId());
        } catch (ServiceRegistryException e) {
          logger.error("Unable to update workflow job {}: {}", job.getId(), e.getMessage());
          failed++;
        }
      }
      logger.info(
              "Finished migration of 1.3 workflow DB to 1.4 workflow DB. {} entries migrated. {} items couldn't be migrated. Check logs for errors",
              new Object[] { totalMigrated, failed });
    } else {
      logger.info("Finished migration of 1.3 workflow DB to 1.4 workflow DB. Nothing found to migrate");
    }
  }

  /**
   * Parse an old 1.3 workflow instance
   * 
   * @param workflowInstance
   *          the 1.3 workflow instance XML
   * @param organization
   *          the fallback organization
   * @return the 1.4 workflow instance
   */
  @SuppressWarnings("unchecked")
  protected WorkflowInstance parseOldWorkflowInstance(String workflowInstance, String organization) throws IOException,
          WorkflowParsingException {
    ByteArrayOutputStream baos = null;
    ByteArrayInputStream bais = null;
    InputStream manifest = null;
    try {
      manifest = IOUtils.toInputStream(workflowInstance, "UTF-8");
      Document domMP = new SAXBuilder().build(manifest);

      Namespace wfNs = Namespace.getNamespace("http://workflow.opencastproject.org");
      Namespace secNs = Namespace.getNamespace("http://org.opencastproject.security");
      Namespace mpNs = Namespace.getNamespace("http://mediapackage.opencastproject.org");

      Iterator<Element> it = domMP.getDescendants(new ElementFilter());
      while (it.hasNext()) {
        Element elem = it.next();
        elem.setNamespace(wfNs);
      }
      it = domMP.getDescendants(new ElementFilter("organization"));
      while (it.hasNext()) {
        Element orgElem = it.next();
        setNamespaceToAllChildren(orgElem, secNs);
        orgElem.setNamespace(secNs);
      }
      it = domMP.getDescendants(new ElementFilter("creator"));
      while (it.hasNext()) {
        Element creatorElem = it.next();
        setNamespaceToAllChildren(creatorElem, secNs);
        creatorElem.setNamespace(secNs);
      }
      it = domMP.getDescendants(new ElementFilter("mediapackage"));
      while (it.hasNext()) {
        Element mpElem = it.next();
        setNamespaceToAllChildren(mpElem, mpNs);
        mpElem.setNamespace(mpNs);
      }

      baos = new ByteArrayOutputStream();
      new XMLOutputter().output(domMP, baos);
      bais = new ByteArrayInputStream(baos.toByteArray());
      WorkflowInstanceImpl instance = WorkflowParser.parseWorkflowInstance(IOUtils.toString(bais, "UTF-8"));
      if (instance.getOrganization() == null)
        instance.setOrganization(new JaxbOrganization(organization));
      migrateSecurityPolicy(instance);
      return instance;
    } catch (JDOMException e) {
      throw new WorkflowParsingException("Error unmarshalling workflow", e);
    } finally {
      IOUtils.closeQuietly(bais);
      IOUtils.closeQuietly(baos);
      IOUtils.closeQuietly(manifest);
    }
  }

  private void migrateSecurityPolicy(WorkflowInstanceImpl instance) {
    MediaPackage mp = instance.getMediaPackage();
    Attachment[] xacmlAttachments = mp.getAttachments(MediaPackageElements.XACML_POLICY);
    for (Attachment a : xacmlAttachments) {
      String uriString = a.getURI().toString();
      if (StringUtils.isNotBlank(downloadUrl) && uriString.startsWith(wfrUrl)) {
        String path = uriString.substring(wfrUrl.length());
        URI newUri = URI.create(UrlSupport.concat(downloadUrl, "engage-player", path));
        a.setURI(newUri);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void setNamespaceToAllChildren(Element elem, Namespace namespace) {
    Iterator<Element> children = elem.getDescendants(new ElementFilter());
    while (children.hasNext()) {
      Element child = children.next();
      child.setNamespace(namespace);
    }
  }

}
