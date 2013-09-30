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
package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Downloads all external URI's to the working file repository and optionally deletes external working file repository
 * resources
 */
public class IngestDownloadWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(IngestDownloadWorkflowOperationHandler.class);

  /** Deleting external working file repository URI's after download config key */
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
  public IngestDownloadWorkflowOperationHandler() {
    configurationOptions = new TreeMap<String, String>();
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

    boolean deleteExternal = BooleanUtils.toBoolean(currentOperation.getConfiguration(DELETE_EXTERNAL));

    String baseUrl = workspace.getBaseUri().toString();

    List<URI> externalUris = new ArrayList<URI>();
    for (MediaPackageElement element : mediaPackage.getElements()) {
      if (element.getURI() == null)
        continue;

      URI originalElementUri = element.getURI();
      if (originalElementUri.toString().startsWith(baseUrl)) {
        logger.info("Skipping downloading already existing element {}", originalElementUri);
        continue;
      }

      // Download the external URI
      File file;
      try {
        file = workspace.get(element.getURI());
      } catch (Exception e) {
        logger.warn("Unable to download the external element {}", element.getURI());
        throw new WorkflowOperationException("Unable to download the external element " + element.getURI(), e);
      }

      // Put to working file repository and rewrite URI on element
      InputStream in = null;
      try {
        in = new FileInputStream(file);
        URI uri = workspace.put(mediaPackage.getIdentifier().compact(), element.getIdentifier(),
                FilenameUtils.getName(element.getURI().getPath()), in);
        element.setURI(uri);
      } catch (Exception e) {
        logger.warn("Unable to store downloaded element '{}': {}", element.getURI(), e.getMessage());
        throw new WorkflowOperationException("Unable to store downloaded element " + element.getURI(), e);
      } finally {
        IOUtils.closeQuietly(in);
        try {
          workspace.delete(originalElementUri);
        } catch (Exception e) {
          logger.warn("Unable to delete ingest-downloaded element {}: {}", element.getURI(), e);
        }
      }

      logger.info("Downloaded the external element {}", originalElementUri);

      // Store origianl URI for deletion
      externalUris.add(originalElementUri);
    }

    if (!deleteExternal || externalUris.size() == 0)
      return createResult(mediaPackage, Action.CONTINUE);

    // Find all external working file repository base Urls
    logger.debug("Assembling list of external working file repositories");
    List<String> externalWfrBaseUrls = new ArrayList<String>();
    try {
      for (ServiceRegistration reg : serviceRegistry.getServiceRegistrationsByType(WorkingFileRepository.SERVICE_TYPE)) {
        if (baseUrl.startsWith(reg.getHost())) {
          logger.trace("Skpping local working file repository");
          continue;
        }
        externalWfrBaseUrls.add(UrlSupport.concat(reg.getHost(), reg.getPath()));
      }
      logger.debug("{} external working file repositories found", externalWfrBaseUrls.size());
    } catch (ServiceRegistryException e) {
      logger.error("Unable to load WFR services from service registry: {}", e.getMessage());
      throw new WorkflowOperationException(e);
    }

    for (URI uri : externalUris) {

      String elementUri = uri.toString();

      // Delete external working file repository URI's
      String wfrBaseUrl = null;
      for (String url : externalWfrBaseUrls) {
        if (elementUri.startsWith(url)) {
          wfrBaseUrl = url;
          break;
        }
      }

      if (wfrBaseUrl == null) {
        logger.info("Unable to delete external URI {}, no working file repository found", elementUri);
        continue;
      }

      HttpDelete delete;
      if (elementUri.startsWith(UrlSupport.concat(wfrBaseUrl, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX))) {
        String wfrDeleteUrl = elementUri.substring(0, elementUri.lastIndexOf("/"));
        delete = new HttpDelete(wfrDeleteUrl);
      } else if (elementUri.startsWith(UrlSupport.concat(wfrBaseUrl, WorkingFileRepository.COLLECTION_PATH_PREFIX))) {
        delete = new HttpDelete(elementUri);
      } else {
        logger.info("Unable to handle working file repository URI {}", elementUri);
        continue;
      }

      try {
        HttpResponse response = client.execute(delete);
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
