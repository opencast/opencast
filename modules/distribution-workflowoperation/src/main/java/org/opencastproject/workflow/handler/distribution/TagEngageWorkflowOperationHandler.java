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

package org.opencastproject.workflow.handler.distribution;

import static org.opencastproject.workflow.handler.distribution.EngagePublicationChannel.CHANNEL_ID;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(
        immediate = true,
        service = WorkflowOperationHandler.class,
        property = {
                "service.description=Tag Engage Workflow Operation Handler",
                "workflow.operation=tag-engage"
        }
)
public class TagEngageWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(TagEngageWorkflowOperationHandler.class);

  private static final String PLUS = "+";
  private static final String MINUS = "-";

  private SearchService searchService = null;

  @Reference
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  @Activate
  protected void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering tag engage workflow operation handler");
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance instance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage currentMediaPackage = instance.getMediaPackage();
    String mediaPackageId = currentMediaPackage.getIdentifier().toString();

    // parse config
    ConfiguredTagsAndFlavors config = getTagsAndFlavors(instance,
            Configuration.many,  // source-tags
            Configuration.many,  // source-flavors
            Configuration.many,  // target-tags
            Configuration.many); //target-flavors

    Set<String> removeTags = new HashSet<>();
    Set<String> addTags = new HashSet<>();
    Set<String> overrideTags = new HashSet<>();

    for (String tag : config.getTargetTags()) {
      if (tag.startsWith(MINUS)) {
        removeTags.add(tag);
      } else if (tag.startsWith(PLUS)) {
        addTags.add(tag);
      } else {
        overrideTags.add(tag);
      }
    }

    SimpleElementSelector mpeSelector = new SimpleElementSelector();
    for (MediaPackageElementFlavor flavor : config.getSrcFlavors()) {
      mpeSelector.addFlavor(flavor);
    }
    for (String tag : config.getSrcTags()) {
      mpeSelector.addTag(tag);
    }

    // get published mp
    SearchResult result = searchService.getByQuery(new SearchQuery().withId(mediaPackageId));
    if (result.size() == 0) {
      throw new WorkflowOperationException(
              "Media package " + mediaPackageId + " can't be updated in Search because it " + "isn't published.");
    } else if (result.size() > 1) {
      throw new WorkflowOperationException("Media package " + mediaPackageId + " can't be updated in Search because "
              + "more than one media package with that id was found.");
    }
    MediaPackage mediaPackageForSearch = result.getItems()[0].getMediaPackage();

    // update tags & flavors in published mp
    Collection<MediaPackageElement> searchElements = mpeSelector.select(mediaPackageForSearch, false);
    boolean changedMediaPackageForSearch = updateTagsAndFlavors(searchElements, config, removeTags, addTags,
            overrideTags);
    if (!changedMediaPackageForSearch) {
      logger.info("No element changed, not publishing anything.");
      return createResult(currentMediaPackage, WorkflowOperationResult.Action.SKIP);
    }

    // update engage publication as well
    boolean changedPublication = false;
    for (Publication publication : currentMediaPackage.getPublications()) {
      if (CHANNEL_ID.equals(publication.getChannel())) {
        List<MediaPackageElement> publicationElements =
                Stream.of(publication.getAttachments(), publication.getCatalogs(), publication.getTracks())
                        .flatMap(Stream::of).collect(Collectors.toList());
        Collection<MediaPackageElement> selectedElements = mpeSelector.select(publicationElements, false);
        changedPublication = updateTagsAndFlavors(selectedElements, config, removeTags, addTags, overrideTags);
      }
    }
    if (!changedPublication) {
      throw new WorkflowOperationException("Publication for " + mediaPackageId + " couldn't be updated.");
    }

    // publish updated mp to search
    Job publishJob;
    try {
      publishJob = searchService.add(mediaPackageForSearch);
      if (!waitForStatus(publishJob).isSuccess()) {
        throw new WorkflowOperationException("Media package " + mediaPackageId + " could not be published.");
      }
    } catch (SearchException | MediaPackageException | UnauthorizedException | ServiceRegistryException e) {
      throw new WorkflowOperationException("Error publishing media package", e);
    }

    return createResult(currentMediaPackage, WorkflowOperationResult.Action.CONTINUE);
  }

  private boolean updateTagsAndFlavors(Collection<MediaPackageElement> elements, ConfiguredTagsAndFlavors config,
          Set<String> removeTags, Set<String> addTags, Set<String> overrideTags) {

    boolean changed = false;
    for (MediaPackageElement element : elements) {

      // update flavor
      if (!config.getTargetFlavors().isEmpty()) {
        MediaPackageElementFlavor currentFlavor = element.getFlavor();
        MediaPackageElementFlavor targetFlavor = config.getTargetFlavors().get(0);

        String newFlavorType = targetFlavor.getType();
        String newFlavorSubtype = targetFlavor.getSubtype();
        if (MediaPackageElementFlavor.WILDCARD.equals(newFlavorType)) {
          newFlavorType = currentFlavor.getType();
        }
        if (MediaPackageElementFlavor.WILDCARD.equals(newFlavorSubtype)) {
          newFlavorSubtype = currentFlavor.getSubtype();
        }
        MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor(
                newFlavorType + MediaPackageElementFlavor.SEPARATOR + newFlavorSubtype);

        if (!newFlavor.equals(currentFlavor)) {
          element.setFlavor(newFlavor);
          changed = true;
        }
      }

      // update tags
      Set<String> currentTags = new HashSet<>(Arrays.asList(element.getTags()));
      if (overrideTags.size() > 0) {
        element.clearTags();
        for (String tag : overrideTags) {
          element.addTag(tag);
        }
      } else {
        for (String tag : removeTags) {
          element.removeTag(tag.substring(MINUS.length()));
        }
        for (String tag : addTags) {
          element.addTag(tag.substring(PLUS.length()));
        }
      }
      Set<String> newTags = new HashSet<>(Arrays.asList(element.getTags()));
      if (!currentTags.equals(newTags)) {
        changed = true;
      }
    }
    return changed;
  }
}
