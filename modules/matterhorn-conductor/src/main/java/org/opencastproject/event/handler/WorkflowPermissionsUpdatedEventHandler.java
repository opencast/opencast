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
package org.opencastproject.event.handler;

import static org.opencastproject.event.EventAdminConstants.ID;
import static org.opencastproject.event.EventAdminConstants.PAYLOAD;
import static org.opencastproject.event.EventAdminConstants.SERIES_ACL_TOPIC;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/** Responds to series events by re-distributing metadata and security policy files to workflows. */
public class WorkflowPermissionsUpdatedEventHandler {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(WorkflowPermissionsUpdatedEventHandler.class);

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The series service */
  protected SeriesService seriesService = null;

  /** The workflow service */
  protected WorkflowService workflowService = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The authorization service */
  protected AuthorizationService authorizationService = null;

  /** The organization directory */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** Dublin core catalog service */
  protected DublinCoreCatalogService dublinCoreService = null;

  /** The workspace */
  protected Workspace workspace = null;

  /** The system account to use for running asynchronous events */
  protected String systemAccount = null;

  /**
   * OSGI callback for component activation.
   *
   * @param bundleContext
   *          the OSGI bundle context
   */
  protected void activate(BundleContext bundleContext) {
    this.systemAccount = bundleContext.getProperty("org.opencastproject.security.digest.user");
  }

  /**
   * @param serviceRegistry
   *          the serviceRegistry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * @param workspace
   *          the workspace to set
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * @param seriesService
   *          the series service to set
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * @param dublinCoreService
   *          the dublin core service to set
   */
  public void setDublinCoreCatalogService(DublinCoreCatalogService dublinCoreService) {
    this.dublinCoreService = dublinCoreService;
  }

  /**
   * @param workflowService
   *          the workflow service to set
   */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param authorizationService
   *          the authorizationService to set
   */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * @param organizationDirectoryService
   *          the organizationDirectoryService to set
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  public void handleEvent(final Event event) {
    // A series or its ACL has been updated. Find any mediapackages with that series, and update them.
    logger.debug("Handling {}", event);
    String seriesId = (String) event.getProperty(ID);

    // We must be an administrative user to make this query
    try {
      DefaultOrganization defaultOrg = new DefaultOrganization();
      securityService.setOrganization(defaultOrg);
      securityService.setUser(SecurityUtil.createSystemUser(systemAccount, defaultOrg));

      // Note: getWorkflowInstances will only return a given number of results (default 20)
      WorkflowQuery q = new WorkflowQuery().withSeriesId(seriesId);
      WorkflowSet result = workflowService.getWorkflowInstancesForAdministrativeRead(q);
      Integer offset = 0;

      while (result.size() > 0) {
        for (WorkflowInstance instance : result.getItems()) {
          if (!isActive(instance))
            continue;

          Organization org = instance.getOrganization();
          securityService.setOrganization(org);

          MediaPackage mp = instance.getMediaPackage();

          // Update the series XACML file
          if (SERIES_ACL_TOPIC.equals(event.getTopic())) {
            // Build a new XACML file for this mediapackage
            AccessControlList acl = AccessControlParser.parseAcl((String) event.getProperty(PAYLOAD));
            authorizationService.setAcl(mp, AclScope.Series, acl);
          }

          // Update the series dublin core
          DublinCoreCatalog seriesDublinCore = seriesService.getSeries(seriesId);
          mp.setSeriesTitle(seriesDublinCore.getFirst(DublinCore.PROPERTY_TITLE));

          // Update the series dublin core
          Catalog[] seriesCatalogs = mp.getCatalogs(MediaPackageElements.SERIES);
          if (seriesCatalogs.length == 1) {
            Catalog c = seriesCatalogs[0];
            String filename = FilenameUtils.getName(c.getURI().toString());
            URI uri = workspace.put(mp.getIdentifier().toString(), c.getIdentifier(), filename,
                    dublinCoreService.serialize(seriesDublinCore));
            c.setURI(uri);
            // setting the URI to a new source so the checksum will most like be invalid
            c.setChecksum(null);
          }

          // Update the search index with the modified mediapackage
          workflowService.update(instance);
        }
        offset++;
        q = q.withStartPage(offset);
        result = workflowService.getWorkflowInstancesForAdministrativeRead(q);
      }
    } catch (WorkflowException e) {
      logger.warn(e.getMessage());
    } catch (UnauthorizedException e) {
      logger.warn(e.getMessage());
    } catch (NotFoundException e) {
      logger.warn(e.getMessage());
    } catch (IOException e) {
      logger.warn(e.getMessage());
    } catch (AccessControlParsingException e) {
      logger.warn(e.getMessage());
    } catch (SeriesException e) {
      logger.warn(e.getMessage());
    } finally {
      securityService.setOrganization(null);
      securityService.setUser(null);
    }
  }

  private boolean isActive(WorkflowInstance workflowInstance) {
    if (WorkflowState.INSTANTIATED.equals(workflowInstance.getState())
            || WorkflowState.RUNNING.equals(workflowInstance.getState())
            || WorkflowState.PAUSED.equals(workflowInstance.getState())) {
      return true;
    }
    return false;
  }

}
