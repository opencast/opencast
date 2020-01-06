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

import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.util.JobUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


/**
 * This WOH duplicates an input event.
 */
public class DuplicateEventWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(DuplicateEventWorkflowOperationHandler.class);
  private static final String PLUS = "+";
  private static final String MINUS = "-";

  /** Name of the configuration option that provides the source flavors we are looking for */
  public static final String SOURCE_FLAVORS_PROPERTY = "source-flavors";

  /** Name of the configuration option that provides the source tags we are looking for */
  public static final String SOURCE_TAGS_PROPERTY = "source-tags";

  /** Name of the configuration option that provides the target tags we should apply */
  public static final String TARGET_TAGS_PROPERTY = "target-tags";

  /** Name of the configuration option that provides the number of events to create */
  public static final String NUMBER_PROPERTY = "number-of-events";

  /** Name of the configuration option that provides the maximum number of events to create */
  public static final String MAX_NUMBER_PROPERTY = "max-number-of-events";

  /** The default maximum number of events to create. Can be overridden. */
  public static final int MAX_NUMBER_DEFAULT = 25;

  /** The namespaces of the asset manager properties to copy. */
  public static final String PROPERTY_NAMESPACES_PROPERTY = "property-namespaces";

  /** The prefix to use for the number which is appended to the original title of the event. */
  public static final String COPY_NUMBER_PREFIX_PROPERTY = "copy-number-prefix";

  /** AssetManager to use for creating new media packages. */
  private AssetManager assetManager;

  /** The workspace to use for retrieving and storing files. */
  protected Workspace workspace;

  /** The distribution service */
  protected DistributionService distributionService;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param assetManager
   *          the asset manager
   */
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param workspace
   *          the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param distributionService
   *          the distributionService to set
   */
  public void setDistributionService(DistributionService distributionService) {
    this.distributionService = distributionService;
  }

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, final JobContext context)
      throws WorkflowOperationException {

    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    final WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    final String configuredSourceFlavors = trimToEmpty(operation.getConfiguration(SOURCE_FLAVORS_PROPERTY));
    final String configuredSourceTags = trimToEmpty(operation.getConfiguration(SOURCE_TAGS_PROPERTY));
    final String configuredTargetTags = trimToEmpty(operation.getConfiguration(TARGET_TAGS_PROPERTY));
    final int numberOfEvents = Integer.parseInt(operation.getConfiguration(NUMBER_PROPERTY));
    final String configuredPropertyNamespaces = trimToEmpty(operation.getConfiguration(PROPERTY_NAMESPACES_PROPERTY));
    int maxNumberOfEvents = MAX_NUMBER_DEFAULT;

    if (operation.getConfiguration(MAX_NUMBER_PROPERTY) != null) {
      maxNumberOfEvents = Integer.parseInt(operation.getConfiguration(MAX_NUMBER_PROPERTY));
    }

    if (numberOfEvents > maxNumberOfEvents) {
      throw new WorkflowOperationException("Number of events to create exceeds the maximum of "
          + maxNumberOfEvents + ". Aborting.");
    }

    logger.info("Creating {} new media packages from media package with id {}.", numberOfEvents,
        mediaPackage.getIdentifier());

    final String[] sourceTags = split(configuredSourceTags, ",");
    final String[] targetTags = split(configuredTargetTags, ",");
    final String[] sourceFlavors = split(configuredSourceFlavors, ",");
    final String[] propertyNamespaces = split(configuredPropertyNamespaces, ",");
    final String copyNumberPrefix = trimToEmpty(operation.getConfiguration(COPY_NUMBER_PREFIX_PROPERTY));

    final SimpleElementSelector elementSelector = new SimpleElementSelector();
    for (String flavor : sourceFlavors) {
      elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }

    final List<String> removeTags = new ArrayList<>();
    final List<String> addTags = new ArrayList<>();
    final List<String> overrideTags = new ArrayList<>();

    for (String tag : targetTags) {
      if (tag.startsWith(MINUS)) {
        removeTags.add(tag);
      } else if (tag.startsWith(PLUS)) {
        addTags.add(tag);
      } else {
        overrideTags.add(tag);
      }
    }

    for (String tag : sourceTags) {
      elementSelector.addTag(tag);
    }

    // Filter elements to copy based on input tags and input flavors
    final Collection<MediaPackageElement> elements = elementSelector.select(mediaPackage, false);
    final Collection<Publication> internalPublications = new HashSet<>();

    for (MediaPackageElement e : mediaPackage.getElements()) {
      if (e instanceof Publication && InternalPublicationChannel.CHANNEL_ID.equals(((Publication) e).getChannel())) {
        internalPublications.add((Publication) e);
        // Remove publications since they are cloned in another way than the other elements
        elements.remove(e);
      }
      if (MediaPackageElements.EPISODE.equals(e.getFlavor())) {
        // Remove episode DC since we will add a new one (with changed title)
        elements.remove(e);
      }
    }

    final MediaPackageElement[] originalEpisodeDc = mediaPackage.getElementsByFlavor(MediaPackageElements.EPISODE);
    if (originalEpisodeDc.length != 1) {
      throw new WorkflowOperationException("Media package " + mediaPackage.getIdentifier() + " has "
          + originalEpisodeDc.length + " episode dublin cores while it is expected to have exactly 1. Aborting.");
    }

    Map<String, String> properties = new HashMap<>();

    for (int i = 0; i < numberOfEvents; i++) {
      final List<URI> temporaryFiles = new ArrayList<>();
      MediaPackage newMp = null;

      try {
        // Clone the media package (without its elements)
        newMp = copyMediaPackage(mediaPackage, i + 1, copyNumberPrefix);

        // Create and add new episode dublin core with changed title
        newMp = copyDublinCore(mediaPackage, originalEpisodeDc[0], newMp, removeTags, addTags, overrideTags,
                temporaryFiles);

        // Clone regular elements
        for (final MediaPackageElement e : elements) {
          final MediaPackageElement element = (MediaPackageElement) e.clone();
          updateTags(element, removeTags, addTags, overrideTags);
          newMp.add(element);
        }

        // Clone internal publications
        for (final Publication originalPub : internalPublications) {
          copyPublication(originalPub, mediaPackage, newMp, removeTags, addTags, overrideTags, temporaryFiles);
        }

        assetManager.takeSnapshot(AssetManager.DEFAULT_OWNER, newMp);

        // Clone properties of media package
        for (String namespace : propertyNamespaces) {
          copyProperties(namespace, mediaPackage, newMp);
        }

        // Store media package ID as workflow property
        properties.put("duplicate_media_package_" + (i + 1) + "_id", newMp.getIdentifier().toString());
      } finally {
        cleanup(temporaryFiles, Optional.ofNullable(newMp));
      }
    }
    return createResult(mediaPackage, properties, Action.CONTINUE, 0);
  }

  private void cleanup(List<URI> temporaryFiles, Optional<MediaPackage> newMp) {
    // Remove temporary files of new media package
    for (URI temporaryFile : temporaryFiles) {
      try {
        workspace.delete(temporaryFile);
      } catch (NotFoundException e) {
        logger.debug("{} could not be found in the workspace and hence, cannot be deleted.", temporaryFile);
      } catch (IOException e) {
        logger.warn("Failed to delete {} from workspace.", temporaryFile);
      }
    }
    newMp.ifPresent(mp -> {
      try {
        workspace.cleanup(mp.getIdentifier());
      } catch (IOException e) {
        logger.warn("Failed to cleanup the workspace for media package {}", mp.getIdentifier());
      }
    });
  }

  private void updateTags(
      MediaPackageElement element,
      List<String> removeTags,
      List<String> addTags,
      List<String> overrideTags) {
    element.setIdentifier(null);

    if (overrideTags.size() > 0) {
      element.clearTags();
      for (String overrideTag : overrideTags) {
        element.addTag(overrideTag);
      }
    } else {
      for (String removeTag : removeTags) {
        element.removeTag(removeTag.substring(MINUS.length()));
      }
      for (String tag : addTags) {
        element.addTag(tag.substring(PLUS.length()));
      }
    }
  }

  private MediaPackage copyMediaPackage(
      MediaPackage source,
      long copyNumber,
      String copyNumberPrefix) throws WorkflowOperationException {
    // We are not using MediaPackage.clone() here, since it does "too much" for us (e.g. copies all the attachments)
    MediaPackage destination;
    try {
      destination = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (MediaPackageException e) {
      logger.error("Failed to create media package " + e.getLocalizedMessage());
      throw new WorkflowOperationException(e);
    }
    logger.info("Created mediapackage {}", destination);
    destination.setDate(source.getDate());
    destination.setSeries(source.getSeries());
    destination.setSeriesTitle(source.getSeriesTitle());
    destination.setDuration(source.getDuration());
    destination.setLanguage(source.getLanguage());
    destination.setLicense(source.getLicense());
    destination.setTitle(source.getTitle() + " (" + copyNumberPrefix + " " + copyNumber + ")");
    return destination;
  }

  private void copyPublication(
      Publication sourcePublication,
      MediaPackage source,
      MediaPackage destination,
      List<String> removeTags,
      List<String> addTags,
      List<String> overrideTags,
      List<URI> temporaryFiles) throws WorkflowOperationException {
    final String newPublicationId = UUID.randomUUID().toString();
    final Publication newPublication = PublicationImpl.publication(newPublicationId,
        InternalPublicationChannel.CHANNEL_ID, null, null);

    // re-distribute elements of publication to internal publication channel
    final Collection<MediaPackageElement> sourcePubElements = new HashSet<>();
    sourcePubElements.addAll(Arrays.asList(sourcePublication.getAttachments()));
    sourcePubElements.addAll(Arrays.asList(sourcePublication.getCatalogs()));
    sourcePubElements.addAll(Arrays.asList(sourcePublication.getTracks()));
    for (final MediaPackageElement e : sourcePubElements) {
      try {
        // We first have to copy the media package element into the workspace
        final MediaPackageElement element = (MediaPackageElement) e.clone();
        try (InputStream inputStream = workspace.read(element.getURI())) {
          final URI tmpUri = workspace.put(destination.getIdentifier().toString(), element.getIdentifier(),
              FilenameUtils.getName(element.getURI().toString()), inputStream);
          temporaryFiles.add(tmpUri);
          element.setIdentifier(null);
          element.setURI(tmpUri);
        }

        // Now we can distribute it to the new media package
        destination.add(element); // Element has to be added before it can be distributed
        final Job job = distributionService.distribute(InternalPublicationChannel.CHANNEL_ID, destination,
            element.getIdentifier());
        final MediaPackageElement distributedElement =
            JobUtil.payloadAsMediaPackageElement(serviceRegistry).apply(job);
        destination.remove(element);

        updateTags(distributedElement, removeTags, addTags, overrideTags);

        PublicationImpl.addElementToPublication(newPublication, distributedElement);
      } catch (Exception exception) {
        throw new WorkflowOperationException(exception);
      }
    }

    // Using an altered copy of the source publication's URI is a bit hacky,
    // but it works without knowing the URI pattern...
    String publicationUri = sourcePublication.getURI().toString();
    publicationUri = publicationUri.replace(source.getIdentifier().toString(), destination.getIdentifier().toString());
    publicationUri = publicationUri.replace(sourcePublication.getIdentifier(), newPublicationId);
    newPublication.setURI(URI.create(publicationUri));
    destination.add(newPublication);
  }

  private MediaPackage copyDublinCore(
      MediaPackage source,
      MediaPackageElement sourceDublinCore,
      MediaPackage destination,
      List<String> removeTags,
      List<String> addTags,
      List<String> overrideTags,
      List<URI> temporaryFiles) throws WorkflowOperationException {
    final DublinCoreCatalog destinationDublinCore = DublinCoreUtil.loadEpisodeDublinCore(workspace, source).get();
    destinationDublinCore.setIdentifier(null);
    destinationDublinCore.setURI(sourceDublinCore.getURI());
    destinationDublinCore.set(DublinCore.PROPERTY_TITLE, destination.getTitle());
    try (InputStream inputStream = IOUtils.toInputStream(destinationDublinCore.toXmlString(), "UTF-8")) {
      final String elementId = UUID.randomUUID().toString();
      final URI newUrl = workspace.put(destination.getIdentifier().compact(), elementId, "dublincore.xml",
          inputStream);
      temporaryFiles.add(newUrl);
      final MediaPackageElement mpe = destination.add(newUrl, MediaPackageElement.Type.Catalog,
          MediaPackageElements.EPISODE);
      mpe.setIdentifier(elementId);
      for (String tag : sourceDublinCore.getTags()) {
        mpe.addTag(tag);
      }
      updateTags(mpe, removeTags, addTags, overrideTags);
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
    }
    return destination;
  }

  private void copyProperties(String namespace, MediaPackage source, MediaPackage destination) {
    final AQueryBuilder q = assetManager.createQuery();
    final AResult properties = q.select(q.propertiesOf(namespace))
        .where(q.mediaPackageId(source.getIdentifier().toString())).run();
    if (properties.getRecords().head().isNone()) {
      logger.info("No properties to copy for media package {}.", source.getIdentifier(), namespace);
      return;
    }
    for (final Property p : properties.getRecords().head().get().getProperties()) {
      final PropertyId newPropId = PropertyId.mk(destination.getIdentifier().toString(), namespace, p.getId()
          .getName());
      assetManager.setProperty(Property.mk(newPropId, p.getValue()));
    }
  }
}
