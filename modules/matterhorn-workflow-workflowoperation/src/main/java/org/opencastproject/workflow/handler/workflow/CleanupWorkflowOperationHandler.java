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

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.BooleanUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    String flavors = currentOperation.getConfiguration(PRESERVE_FLAVOR_PROPERTY);
    final List<MediaPackageElementFlavor> flavorsToPreserve = new ArrayList<MediaPackageElementFlavor>();

    boolean deleteExternal = BooleanUtils.toBoolean(currentOperation.getConfiguration(DELETE_EXTERNAL));

    // If the configuration does not specify flavors, remove them all
    for (String flavor : asList(flavors)) {
      flavorsToPreserve.add(MediaPackageElementFlavor.parseFlavor(flavor));
    }

    String baseUrl = workspace.getBaseUri().toString();

    // Find all external working file repository base Urls
    List<String> externalWfrBaseUrls = new ArrayList<String>();
    if (deleteExternal) {
      try {
        for (ServiceRegistration reg : serviceRegistry
                .getServiceRegistrationsByType(WorkingFileRepository.SERVICE_TYPE)) {
          if (baseUrl.startsWith(reg.getHost()))
            continue;
          externalWfrBaseUrls.add(UrlSupport.concat(reg.getHost(), reg.getPath()));
        }
      } catch (ServiceRegistryException e) {
        logger.error("Unable to load WFR services from service registry: {}", e.getMessage());
        throw new WorkflowOperationException(e);
      }
    }

    // Some URIs are shared by multiple elements. If one of these elements should be deleted but another should not, we
    // must keep the file.
    Set<URI> urisToDelete = new HashSet<URI>();
    Set<URI> urisToKeep = new HashSet<URI>();
    for (MediaPackageElement element : mediaPackage.getElements()) {
      if (element.getURI() == null)
        continue;

      String elementUri = element.getURI().toString();
      if (!elementUri.startsWith(baseUrl)) {
        if (deleteExternal) {

          String wfrBaseUrl = null;
          for (String url : externalWfrBaseUrls) {
            if (element.getURI().toString().startsWith(url)) {
              wfrBaseUrl = url;
              break;
            }
          }
          if (wfrBaseUrl == null)
            continue;

          HttpDelete delete;
          if (elementUri.startsWith(UrlSupport.concat(wfrBaseUrl, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX))) {
            String wfrDeleteUrl = elementUri.substring(0, elementUri.lastIndexOf("/"));
            delete = new HttpDelete(wfrDeleteUrl);
          } else if (elementUri.startsWith(UrlSupport.concat(wfrBaseUrl, WorkingFileRepository.COLLECTION_PATH_PREFIX))) {
            delete = new HttpDelete(elementUri);
          } else {
            logger.info("Unable to handle URI {}", elementUri);
            continue;
          }

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
          } catch (TrustedHttpClientException e) {
            logger.warn("Unable to execute DELETE request on external URI {}", delete.getURI());
            throw new WorkflowOperationException(e);
          } finally {
            client.close(response);
          }
        }
        continue;
      }

      // remove the element if it doesn't match the flavors to preserve
      boolean remove = true;
      for (MediaPackageElementFlavor flavor : flavorsToPreserve) {
        if (flavor.matches(element.getFlavor())) {
          remove = false;
          break;
        }
      }
      if (remove) {
        urisToDelete.add(element.getURI());
        mediaPackage.remove(element);
      } else {
        urisToKeep.add(element.getURI());
      }
    }

    // Remove all of the files to keep from the one to delete
    urisToDelete.removeAll(urisToKeep);

    // Now remove the files to delete
    for (URI uri : urisToDelete) {
      try {
        workspace.delete(uri);
      } catch (Exception e) {
        logger.warn("Unable to delete {}", uri);
      }
    }
    return createResult(mediaPackage, Action.CONTINUE);
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
