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

package org.opencastproject.workflow.handler.archive;

import static org.opencastproject.util.MimeTypeUtil.suffix;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.ResultItem;
import org.opencastproject.archive.api.ResultSet;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.opencast.OpencastQueryBuilder;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation for loading a media package from the episode service.
 */
public class LoadFromArchiveWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(LoadFromArchiveWorkflowOperationHandler.class);

  /** Name of the mediapackage configuration option */
  private static final String OPT_MEDIAPACKAGE = "mediapackage";

  /** Name of the configuration option to control failure in case of no mediapackage */
  private static final String OPT_FAIL_IF_MISSING = "fail-if-missing";

  /** The archive service */
  private Archive<?> archiveService = null;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(OPT_MEDIAPACKAGE, "The identifier of the mediapackage");
    CONFIG_OPTIONS.put(OPT_FAIL_IF_MISSING,
            "Whether to fail the operation if no archived version of the mediapackage is available");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the search service. Implementation
   * assumes that the reference is configured as being static.
   *
   * @param archiveService
   *          an instance of the search service
   */
  protected void setArchiveService(Archive<?> archiveService) {
    this.archiveService = archiveService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackageFromWorkflow = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    // The mediapackage identifier to use
    final Id mediapackageId;

    // The current organization
    final String organization = workflowInstance.getOrganization().getId();

    // Determine whether to use an externally configured mediapackage or the one from the current workflow
    String optMediapackageId = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_MEDIAPACKAGE));
    if (optMediapackageId != null) {
      mediapackageId = IdBuilderFactory.newInstance().newIdBuilder().fromString(optMediapackageId);
      logger.debug("Using the externally configured mediapackage identifier '{}'", mediapackageId);
    } else {
      mediapackageId = workflowInstance.getMediaPackage().getIdentifier();
      logger.debug("Using the workflow's mediapackage identifier '{}'", mediapackageId);
    }

    // Create the URI rewriter
    UriRewriter uriRewriter;
    try {
      uriRewriter = createURIRewriter();
    } catch (ServiceRegistryException e) {
      throw new WorkflowOperationException(e);
    }

    // Load the mediapackage
    try {
      logger.info("Loading media package {} from the archive", mediapackageId);
      Query query = OpencastQueryBuilder.query().organizationId(Option.option(organization))
              .mediaPackageId(mediapackageId.toString());
      // EpisodeQuery query = Query.systemQuery().organization(organization).id(mediapackageId.toString());
      ResultSet result = archiveService.find(query, uriRewriter);

      // If an archived version has been found, return it as the new mediapackage
      for (ResultItem item : result.getItems())
        return createResult(item.getMediaPackage(), Action.CONTINUE);

      // If not, decide what to do depending on the workflow operation configuration
      String optFailIfMissing = StringUtils.trimToNull(currentOperation.getConfiguration(OPT_FAIL_IF_MISSING));
      if (optFailIfMissing == null || Boolean.parseBoolean(optFailIfMissing)) {
        throw new WorkflowOperationException("No archived version of mediapackage " + mediapackageId + " was found");
      }

      logger.info("No archived version of mediapackage {} was found", mediapackageId);
      return createResult(mediaPackageFromWorkflow, Action.CONTINUE);
    } catch (Throwable t) {
      throw new WorkflowOperationException(t);
    }

  }

  /**
   * Creates an instance of the uri rewriter that will create links to the archive service as registered in the service
   * registry.
   *
   * @return the uri rewriter
   * @throws IllegalStateException
   *           if no instance of the archive service is available
   * @throws ServiceRegistryException
   *           if accessing the service registry fails
   */
  private UriRewriter createURIRewriter() throws IllegalStateException, ServiceRegistryException {
    // Prepare the uri rewriter
    final String archiveServiceServerUrl;
    final String archiveServiceMountPoint;
    final String archiveServicePathPrefix;

    // Lookup the least loaded archive service instance
    List<ServiceRegistration> services = serviceRegistry.getServiceRegistrationsByLoad(Archive.JOB_TYPE);
    if (services.size() == 0)
      throw new IllegalStateException("No instance of type " + Archive.JOB_TYPE + " is currently available");
    ServiceRegistration serviceRegistration = services.get(0);
    archiveServiceServerUrl = serviceRegistration.getHost();
    archiveServiceMountPoint = serviceRegistration.getPath();
    archiveServicePathPrefix = "/archive/mediapackage/";

    return new UriRewriter() {
      @Override
      public URI apply(Version version, MediaPackageElement mpe) {
        final String mimeType = option(mpe.getMimeType()).bind(suffix).getOrElse("unknown");
        return uri(archiveServiceServerUrl, archiveServiceMountPoint, archiveServicePathPrefix, mpe.getMediaPackage()
                .getIdentifier(), mpe.getIdentifier(), version, mpe.getElementType().toString().toLowerCase() + "."
                + mimeType);
      }
    };
  }

}
