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

package org.opencastproject.workflow.handler.composer;

import static org.opencastproject.mediapackage.Attachment.TYPE;
import static org.opencastproject.mediapackage.MediaPackageElements.MEDIAPACKAGE_COVER_FLAVOR;
import static org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_ANY;
import static org.opencastproject.workflow.api.WorkflowOperationResult.Action.CONTINUE;
import static org.opencastproject.workflow.api.WorkflowOperationResult.Action.SKIP;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.SortedMap;
import java.util.TreeMap;

public class CoverArtWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(CoverArtWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  /** The series service */
  protected SeriesService seriesService = null;

  protected String coverArtUrlTemplate = null;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("target-tags",
            "Apply these (comma separated) tags to any mediapackage elements produced as a result of distribution");
  }

  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
    this.coverArtUrlTemplate = cc.getBundleContext().getProperty("cover.art.url.template");
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    String seriesId = mediaPackage.getSeries();
    if (seriesId == null) {
      return createResult(Action.SKIP);
    }

    DublinCoreCatalog catalog = null;
    try {
      catalog = seriesService.getSeries(seriesId);
    } catch (SeriesException e) {
      logger.warn("Unable to load series {}", seriesId);
      return createResult(SKIP);
    } catch (NotFoundException e) {
      return createResult(SKIP);
    } catch (UnauthorizedException e) {
      return createResult(SKIP);
    }

    // TODO: Read this from iTunes U catalog
    String category = catalog.getAsText(new EName(DublinCores.OC_PROPERTY_NS_URI, "category"), LANGUAGE_ANY, null);
    if (category == null) {
      return createResult(SKIP);
    }

    URI coverArtUri;
    try {
      coverArtUri = new URI(MessageFormat.format(coverArtUrlTemplate, category));
    } catch (URISyntaxException e) {
      logger.warn("Unable to create a valid URL for category {} using template {}", category, coverArtUrlTemplate);
      return createResult(SKIP);
    }
    Attachment attachment = (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromURI(coverArtUri, TYPE, MEDIAPACKAGE_COVER_FLAVOR);

    for (String tag : asList(workflowInstance.getCurrentOperation().getConfiguration("target-tags"))) {
      attachment.addTag(tag);
    }

    mediaPackage.add(attachment);
    return createResult(mediaPackage, CONTINUE);
  }

  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }
}
