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

package org.opencastproject.workflow.handler.workflow;

import static org.opencastproject.mediapackage.MediaPackageElement.Type.Publication;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Removes all files in the working file repository for mediapackage elements that don't match one of the
 * "preserve-flavors" configuration value.
 */
public class CleanupWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(CleanupWorkflowOperationHandler.class);

  /** The element flavors to maintain in the original mediapackage. All others will be removed */
  public static final String PRESERVE_FLAVOR_PROPERTY = "preserve-flavors";

  /** Deleting external URI's config key */
  public static final String DELETE_EXTERNAL = "delete-external";

  /** Time to wait in seconds before removing files */
  public static final String DELAY = "delay";

  /** The configuration properties */
  protected SortedMap<String, String> configurationOptions = null;

  /**
   * The workspace to use in retrieving and storing files.
   */
  protected Workspace workspace;

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** The default no-arg constructor builds the configuration options set */
  public CleanupWorkflowOperationHandler() {
    configurationOptions = new TreeMap<String, String>();
    configurationOptions.put(PRESERVE_FLAVOR_PROPERTY,
            "The configuration key that specifies the flavors to preserve.  If not specified, this operation will not"
                    + "remove any files.");
    configurationOptions.put(DELETE_EXTERNAL,
            "Whether to try to delete external working file repository URIs. Default is false.");
    configurationOptions.put(DELAY,
            "Time to wait in seconds before removing files. Default is 1s.");
  }

  /**
   * Sets the workspace to use.
   *
   * @param workspace
   *          the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the trusted http client
   *
   * @param client
   *          the trusted http client
   */
  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  /**
   * Deletes JobArguments for every finished Job of the WorkfloInstance
   *
   * @param workflowInstance
   */
  public void cleanUpJobArgument(WorkflowInstance workflowInstance) {
    List<WorkflowOperationInstance> operationInstances = workflowInstance.getOperations();
    for (WorkflowOperationInstance operationInstance : operationInstances) {
      logger.debug("Delete JobArguments for Job id from Workflowinstance" + operationInstance.getId());

      // delete job Arguments
      Long operationInstanceId = null;
      try {
        operationInstanceId = operationInstance.getId();
        // instanceId can be null if the operation never run
        if (operationInstanceId != null) {
          Job operationInstanceJob = (serviceRegistry.getJob(operationInstanceId));
          List<String> list = new ArrayList<>();
          operationInstanceJob.setArguments(list);
          serviceRegistry.updateJob(operationInstanceJob);

          List<Job> jobs = serviceRegistry.getChildJobs(operationInstanceId);
          for (Job job : jobs) {
            if (job.getStatus() == Job.Status.FINISHED) {
              logger.debug("Deleting Arguments:  " + job.getArguments());
              job.setArguments(list);
              serviceRegistry.updateJob(job);
            }
          }
        }
      } catch (ServiceRegistryException | NotFoundException ex) {
        logger.error("Deleting JobArguments failed for Job {}: {} ", operationInstanceId, ex);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    cleanUpJobArgument(workflowInstance);

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    String flavors = currentOperation.getConfiguration(PRESERVE_FLAVOR_PROPERTY);
    final List<MediaPackageElementFlavor> flavorsToPreserve = new ArrayList<MediaPackageElementFlavor>();

    boolean deleteExternal = BooleanUtils.toBoolean(currentOperation.getConfiguration(DELETE_EXTERNAL));

    String delayStr = currentOperation.getConfiguration(DELAY);
    int delay = 1;

    if (delayStr != null) {
      try {
        delay = Integer.parseInt(delayStr);
      } catch (NumberFormatException e) {
        logger.warn("Invalid value '{}' for delay in workflow operation configuration (should be integer)", delayStr);
      }
    }

    if (delay > 0) {
      try {
        logger.debug("Sleeping {}s before removing workflow files", delay);
        Thread.sleep(delay * 1000);
      } catch (InterruptedException e) {
        // ignore
      }
    }

    // If the configuration does not specify flavors, remove them all
    for (String flavor : asList(flavors))
      flavorsToPreserve.add(MediaPackageElementFlavor.parseFlavor(flavor));

    List<MediaPackageElement> elementsToRemove = new ArrayList<>();
    for (MediaPackageElement element : mediaPackage.getElements()) {
      if (element.getURI() == null)
        continue;


      if (!isPreserved(element, flavorsToPreserve))
        elementsToRemove.add(element);
    }

    List<String> externalBaseUrls = null;
    if (deleteExternal) {
      externalBaseUrls = getAllWorkingFileRepositoryUrls();
      externalBaseUrls.remove(workspace.getBaseUri().toString());
    }
    for (MediaPackageElement elementToRemove : elementsToRemove) {
      if (deleteExternal) {
        // cleanup external working file repositories
        for (String repository : externalBaseUrls) {
          logger.debug("Removing {} from repository {}", elementToRemove.getURI(), repository);
          try {
            removeElementFromRepository(elementToRemove, repository);
          } catch (TrustedHttpClientException ex) {
            logger.debug("Removing media package element {} from repository {} failed: {}",
                    elementToRemove.getURI(), repository, ex.getMessage());
          }
        }
      }
      // cleanup workspace and also the internal working file repository
      logger.debug("Removing {} from the workspace", elementToRemove.getURI());
      try {
        mediaPackage.remove(elementToRemove);
        workspace.delete(elementToRemove.getURI());
      } catch (NotFoundException ex) {
        logger.debug("Workspace doesn't contain element with Id '{}' from media package '{}': {}",
                elementToRemove.getIdentifier(), mediaPackage.getIdentifier().compact(), ex.getMessage());
      } catch (IOException ex) {
        logger.warn("Unable to remove element with Id '{}' from the media package '{}': {}",
                elementToRemove.getIdentifier(), mediaPackage.getIdentifier().compact(), ex.getMessage());
      }
    }
    return createResult(mediaPackage, Action.CONTINUE);
  }

  /**
   * Returns if elements flavor matches one of the preserved flavors or the element is a publication.
   * Publications cannot be deleted but need to be retracted and will hence always be preserved. Note that publications
   * should also never directly correspond to files in the workspace or the working file repository.
   *
   * @param element Media package element to test
   * @param flavorsToPreserve Flavors to preserve
   * @return true, if elements flavor matches one of the preserved flavors, false otherwise
   */
  private boolean isPreserved(MediaPackageElement element, List<MediaPackageElementFlavor> flavorsToPreserve) {
    if (Publication == element.getElementType())
      return true;

    for (MediaPackageElementFlavor flavor : flavorsToPreserve) {
      if (flavor.matches(element.getFlavor())) {
        return true;
      }
    }
    return false;
  }

  private List<String> getAllWorkingFileRepositoryUrls() {
    List<String> wfrBaseUrls = new ArrayList<String>();
    try {
      for (ServiceRegistration reg : serviceRegistry.getServiceRegistrationsByType(WorkingFileRepository.SERVICE_TYPE))
        wfrBaseUrls.add(UrlSupport.concat(reg.getHost(), reg.getPath()));
    } catch (ServiceRegistryException e) {
      logger.warn("Unable to load services of type {} from service registry: {}",
              WorkingFileRepository.SERVICE_TYPE, e.getMessage());
    }
    return wfrBaseUrls;
  }

  private void removeElementFromRepository(MediaPackageElement elementToRemove, String repositoryBaseUrl)
          throws TrustedHttpClientException {
    if (elementToRemove == null || elementToRemove.getURI() == null || StringUtils.isBlank(repositoryBaseUrl)) {
      return;
    }

    String elementUri = elementToRemove.getURI().toString();
    String deleteUri;
    if (StringUtils.containsIgnoreCase(elementUri, UrlSupport.concat(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
              elementToRemove.getMediaPackage().getIdentifier().compact(), elementToRemove.getIdentifier()))) {
      deleteUri = UrlSupport.concat(repositoryBaseUrl, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
              elementToRemove.getMediaPackage().getIdentifier().compact(), elementToRemove.getIdentifier());
    } else if (StringUtils.containsIgnoreCase(elementUri, WorkingFileRepository.COLLECTION_PATH_PREFIX)) {
      deleteUri = UrlSupport.concat(repositoryBaseUrl, WorkingFileRepository.COLLECTION_PATH_PREFIX,
          StringUtils.substringAfter(elementToRemove.getURI().getPath(), WorkingFileRepository.COLLECTION_PATH_PREFIX));
    } else {
      // the element isn't from working file repository, skip
      logger.info("Unable to handle URI {} for deletion from repository {}", elementUri, repositoryBaseUrl);
      return;
    }
    HttpDelete delete = new HttpDelete(deleteUri);
    HttpResponse response = null;
    try {
      response = client.execute(delete);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_NO_CONTENT || statusCode == HttpStatus.SC_OK) {
        logger.info("Sucessfully deleted external URI {}", delete.getURI());
      } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
        logger.info("External URI {} has already been deleted", delete.getURI());
      } else {
        logger.info("Unable to delete external URI {}, status code '{}' returned", delete.getURI(), statusCode);
      }
    } finally {
      client.close(response);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return configurationOptions;
  }
}
