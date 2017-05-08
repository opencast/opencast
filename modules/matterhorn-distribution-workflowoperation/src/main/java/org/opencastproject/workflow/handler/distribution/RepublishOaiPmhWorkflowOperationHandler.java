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

import static com.entwinemedia.fn.fns.Strings.trimToNone;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.opencastproject.util.EqualsUtil.ne;
import static org.opencastproject.util.data.Collections.smap;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException;
import org.opencastproject.oaipmh.persistence.QueryBuilder;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.util.JobUtil;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import com.entwinemedia.fn.data.Opt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/** Workflow operation for handling "republish" operations to OAI-PMH repositories. */
public final class RepublishOaiPmhWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(RepublishOaiPmhWorkflowOperationHandler.class);

  private OaiPmhDatabase oaiPmhDb = null;
  private DownloadDistributionService distSvc = null;

  /** The configuration options */
  private static final String OPT_SOURCE_FLAVORS = "source-flavors";
  private static final String OPT_SOURCE_TAGS = "source-tags";
  private static final String OPT_MERGE = "merge";
  private static final String OPT_REPOSITORY = "repository";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS = smap(
          tuple(OPT_SOURCE_FLAVORS, "Republish any media package elements with one of these flavors"),
          tuple(OPT_SOURCE_TAGS, "Republish only media package elements that are tagged with one of these tags"),
          tuple(OPT_MERGE, "Merge with existing published data"),
          tuple(OPT_REPOSITORY, "The OAI-PMH repository to update"));

  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance wi, JobContext context) throws WorkflowOperationException {
    final MediaPackage mp = wi.getMediaPackage();
    // The flavors of the elements that are to be published
    final Set<MediaPackageElementFlavor> flavors = new HashSet<>();
    // Check which flavors have been configured
    final List<String> configuredFlavors = getOptConfig(wi, OPT_SOURCE_FLAVORS).bind(trimToNone).map(asList.toFn())
            .getOr(Collections.<String> nil());
    for (String flavor : configuredFlavors) {
      flavors.add(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    // Get the configured tags
    final List<String> tags = asList(getOptConfig(wi, OPT_SOURCE_TAGS).getOr(""));
    // Merge or replace?
    boolean merge = Boolean.parseBoolean(getConfig(wi, OPT_MERGE));
    // repository
    final String repository = getConfig(wi, OPT_REPOSITORY);
    //
    final MediaPackage filteredMp;
    final MediaPackage publishedMp;
    final SearchResult result = oaiPmhDb.search(QueryBuilder.queryRepo(repository).mediaPackageId(mp).build());
    if (result.size() == 1) {
      // apply tags and flavors to the current media package
      try {
        filteredMp = filterMediaPackage(mp, flavors, tags);
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException("Error filtering media package", e);
      }
    } else if (result.size() == 0) {
      logger.info(
              format("Skipping update of media package %s since it is not currently published to %s", mp, repository));
      return createResult(mp, Action.SKIP);
    } else {
      final String msg = format("More than one media package with id %s found", mp);
      logger.warn(msg);
      throw new WorkflowOperationException(msg);
    }
    // re-distribute elements to download
    final List<MediaPackageElement> distributedElements = mlist(filteredMp.getElements())
            .filter(MediaPackageSupport.Filters.isNotPublication).map(new Function.X<MediaPackageElement, Job>() {
              @Override
              public Job xapply(MediaPackageElement mpe) throws Exception {
                try {
                  // todo this -> OaiPmhPublicationService.PUBLICATION_CHANNEL_PREFIX + repository
                  // is pretty ugly. In fact republication should be subject to the OaiPmhPublicationService
                  // and _not_ the workflow operation handler.
                  return distSvc.distribute(OaiPmhPublicationService.PUBLICATION_CHANNEL_PREFIX + repository,
                          filteredMp, mpe.getIdentifier());
                } catch (Exception e) {
                  throw new WorkflowOperationException(e);
                }
              }
            }).map(JobUtil.payloadAsMediaPackageElement(serviceRegistry)).value();
    // update elements (URLs)
    for (MediaPackageElement e : filteredMp.getElements()) {
      if (MediaPackageElement.Type.Publication.equals(e.getElementType()))
        continue;
      filteredMp.remove(e);
    }
    for (MediaPackageElement e : distributedElements) {
      filteredMp.add(e);
    }
    if (merge) {
      publishedMp = merge(filteredMp, result.getItems().get(0).getMediaPackage());
    } else {
      publishedMp = filteredMp;
    }
    // Does the media package have a title and track?
    if (!isPublishable(publishedMp)) {
      throw new WorkflowOperationException("Media package does not meet criteria for publication");
    }
    // Publish the media package to the search index
    try {
      logger.info(format("Updating metadata of media package %s in %s", publishedMp, repository));
      oaiPmhDb.store(publishedMp, repository);
      logger.info("Completed update operation on {}", mp.getIdentifier());
      return createResult(mp, Action.CONTINUE);
    } catch (OaiPmhDatabaseException e) {
      throw new WorkflowOperationException(format("Media package %s could not be updated", publishedMp));
    }
  }

  /**
   * Creates a clone of the mediapackage and removes those elements that do not match the flavor and tags filter
   * criteria.
   *
   * @param mediaPackage
   *          the media package
   * @param flavors
   *          the flavors
   * @param tags
   *          the tags
   * @return the filtered media package
   */
  private MediaPackage filterMediaPackage(MediaPackage mediaPackage, Set<MediaPackageElementFlavor> flavors,
          List<String> tags) throws MediaPackageException {
    MediaPackage filteredMediaPackage = (MediaPackage) mediaPackage.clone();

    // The list of elements to keep
    List<MediaPackageElement> keep = new ArrayList<>();

    // Filter by flavor
    if (flavors.size() > 0) {
      logger.debug("Filtering elements based on flavors");
      for (MediaPackageElementFlavor flavor : flavors) {
        keep.addAll(Arrays.asList(mediaPackage.getElementsByFlavor(flavor)));
      }
    }

    // Keep those elements that have been identified in the tags
    if (tags.size() > 0) {
      logger.debug("Filtering elements based on tags");
      if (keep.size() > 0) {
        keep.retainAll(Arrays.asList(mediaPackage.getElementsByTags(tags)));
      } else {
        keep.addAll(Arrays.asList(mediaPackage.getElementsByTags(tags)));
      }
    }

    // Keep publications
    for (Publication p : filteredMediaPackage.getPublications())
      keep.add(p);

    // Fix references and flavors
    for (MediaPackageElement element : filteredMediaPackage.getElements()) {

      if (!keep.contains(element)) {
        logger.info("Removing {} '{}' from media package '{}'",
                new String[] { element.getElementType().toString().toLowerCase(), element.getIdentifier(),
                        filteredMediaPackage.getIdentifier().toString() });
        filteredMediaPackage.remove(element);
        continue;
      }

      // Is the element referencing anything?
      MediaPackageReference reference = element.getReference();
      if (reference != null) {
        Map<String, String> referenceProperties = reference.getProperties();
        MediaPackageElement referencedElement = filteredMediaPackage.getElementByReference(reference);

        // if we are distributing the referenced element, everything is fine. Otherwise...
        if (referencedElement != null && !keep.contains(referencedElement)) {

          // Follow the references until we find a flavor
          MediaPackageElement parent = null;
          while ((parent = mediaPackage.getElementByReference(reference)) != null) {
            if (parent.getFlavor() != null && element.getFlavor() == null) {
              element.setFlavor(parent.getFlavor());
            }
            if (parent.getReference() == null)
              break;
            reference = parent.getReference();
          }

          // Done. Let's cut the path but keep references to the mediapackage itself
          if (reference != null && reference.getType().equals(MediaPackageReference.TYPE_MEDIAPACKAGE))
            element.setReference(reference);
          else if (reference != null && (referenceProperties == null || referenceProperties.size() == 0))
            element.clearReference();
          else {
            // Ok, there is more to that reference than just pointing at an element. Let's keep the original,
            // you never know.
            referencedElement.setURI(null);
            referencedElement.setChecksum(null);
          }
        }
      }
    }

    return filteredMediaPackage;
  }

  /**
   * Merges the updated mediapackage with the one that is currently published in a way where the updated elements
   * replace existing ones in the published mediapackage based on their flavor.
   * <p>
   * If <code>publishedMp</code> is <code>null</code>, this method returns the updated mediapackage without any
   * modifications.
   *
   * @param updatedMp
   *          the updated media package
   * @param publishedMp
   *          the mediapackage that is currently published
   * @return the merged mediapackage
   */
  public static MediaPackage merge(MediaPackage updatedMp, MediaPackage publishedMp) {
    if (publishedMp == null)
      return updatedMp;

    final MediaPackage mergedMp = MediaPackageSupport.copy(publishedMp);

    // Merge the elements
    for (final MediaPackageElement updatedElement : updatedMp.elements()) {
      for (final MediaPackageElementFlavor flavor : Opt.nul(updatedElement.getFlavor())) {
        for (final MediaPackageElement outdated : mergedMp.getElementsByFlavor(flavor)) {
          mergedMp.remove(outdated);
        }
        logger.info(format("Update %s of type %s", updatedElement.getIdentifier(), updatedElement.getElementType()));
        mergedMp.add(updatedElement);
      }
    }

    // Remove publications
    for (final Publication p : mergedMp.getPublications())
      mergedMp.remove(p);

    // Add updated publications
    for (final Publication updatedPublication : updatedMp.getPublications())
      mergedMp.add(updatedPublication);

    // Merge media package fields
    if (updatedMp.getDate() != null && ne(updatedMp.getDate(), mergedMp.getDate()))
      mergedMp.setDate(updatedMp.getDate());
    if (updatedMp.getDuration() != null && ne(updatedMp.getDuration(), mergedMp.getDuration()))
      mergedMp.setDuration(updatedMp.getDuration());
    if (isNotBlank(updatedMp.getLicense()) && ne(updatedMp.getLicense(), mergedMp.getLicense()))
      mergedMp.setLicense(updatedMp.getLicense());
    if (isNotBlank(updatedMp.getSeries()) && ne(updatedMp.getSeries(), mergedMp.getSeries()))
      mergedMp.setSeries(updatedMp.getSeries());
    if (isNotBlank(updatedMp.getSeriesTitle()) && ne(updatedMp.getSeriesTitle(), mergedMp.getSeriesTitle()))
      mergedMp.setSeriesTitle(updatedMp.getSeriesTitle());
    if (isNotBlank(updatedMp.getTitle()) && ne(updatedMp.getTitle(), mergedMp.getTitle()))
      mergedMp.setTitle(updatedMp.getTitle());
    if (updatedMp.getSubjects().length > 0 && ne(updatedMp.getSubjects(), mergedMp.getSubjects())) {
      for (String subject : mergedMp.getSubjects()) {
        mergedMp.removeSubject(subject);
      }
      for (String subject : updatedMp.getSubjects()) {
        mergedMp.addSubject(subject);
      }
    }
    if (updatedMp.getContributors().length > 0 && ne(updatedMp.getContributors(), mergedMp.getContributors())) {
      for (String contributor : mergedMp.getContributors()) {
        mergedMp.removeContributor(contributor);
      }
      for (String contributor : updatedMp.getContributors()) {
        mergedMp.addContributor(contributor);
      }
    }
    if (updatedMp.getCreators().length > 0 && ne(updatedMp.getCreators(), mergedMp.getCreators())) {
      for (String creator : mergedMp.getCreators()) {
        mergedMp.removeCreator(creator);
      }
      for (String creator : updatedMp.getCreators()) {
        mergedMp.addCreator(creator);
      }
    }
    return mergedMp;
  }

  /**
   * Media package must have a title and contain tracks in order to be published.
   *
   * @param mp
   *          the media package
   * @return <code>true</code> if the media package can be published
   */
  private boolean isPublishable(MediaPackage mp) {
    return !isBlank(mp.getTitle()) && mp.hasTracks();
  }

  /** OSGi DI. */
  public void setOaiPmhDatabase(OaiPmhDatabase oaiPmhDb) {
    this.oaiPmhDb = oaiPmhDb;
  }

  /** OSGi DI. */
  public void setDistributionService(DownloadDistributionService distSvc) {
    this.distSvc = distSvc;
  }
}
