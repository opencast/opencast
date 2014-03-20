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

import static org.opencastproject.security.util.SecurityUtil.createSystemUser;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.Xpath;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * This class provides migration index and DB migrations to Matterhorn.
 */
public class IncidentMigrationService {

  private static final Logger logger = LoggerFactory.getLogger(IncidentMigrationService.class);

  /** The security service */
  private SecurityService securityService = null;

  /** The service registry */
  private ServiceRegistry serviceRegistry = null;

  /** The organization directory service */
  private OrganizationDirectoryService organizationDirectoryService = null;

  /**
   * Callback for setting the security service.
   * 
   * @param securityService
   *          the security service to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the service registry.
   * 
   * @param serviceRegistry
   *          the service registry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Callback for setting the organization directory service.
   * 
   * @param organizationDirectoryService
   *          the organization directory service to set
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  public void activate(ComponentContext cc) {
    try {
      List<Job> jobs = serviceRegistry.getJobs(WorkflowService.JOB_TYPE, Status.FAILED);
      for (final Job job : jobs) {
        if (!"START_WORKFLOW".equals(job.getOperation()))
          continue;

        Organization organization;
        try {
          organization = organizationDirectoryService.getOrganization(job.getOrganization());
        } catch (NotFoundException e) {
          logger.warn("Can't create incidents for job {}, organization '{}' not found!", job.getId(),
                  job.getOrganization());
          continue;
        }

        SecurityUtil.runAs(securityService, organization, createSystemUser(cc, organization), new Effect0() {
          @Override
          protected void run() {
            if (serviceRegistry.incident().alreadyRecordedFailureIncident(job.getId()))
              return;

            logger.info("Failed job {} has unrecorded incidents, try to create it from errors", job.getId());

            if (job.getPayload() == null) {
              logger.warn("Can't create incidents for job {}, no payload available!", job.getId());
              return;
            }

            // Create a document from the job payload
            Document doc;
            try {
              DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
              doc = db.parse(new ByteArrayInputStream(job.getPayload().getBytes()));
            } catch (Exception e) {
              logger.warn("Can't create incidents for job {}, unable to parse payload!: {}", job.getId(),
                      ExceptionUtils.getStackTrace(e));
              return;
            }

            for (String error : Xpath.mk(doc).strings("//workflow/errors/error/text()")) {
              serviceRegistry.incident().recordMigrationIncident(job, error);
            }
          }
        });
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to migrate incidents", e);
    }
  }

}
