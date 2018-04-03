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
package org.opencastproject.workflow.handler.crop;

import org.opencastproject.crop.api.CropService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The workflow definition will run recordings to crop them from 4:3 to 16:9.
 */
public class CropWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

    /** The logging facility */
    private static final Logger logger = LoggerFactory.getLogger(CropWorkflowOperationHandler.class);

    /** Name of the configuration key that specifies the flavor of the track to analyze */
    private static final String PROP_ANALYSIS_TRACK_FLAVOR = "source-flavor";

    /** Name of the configuration key that specifies the flavor of the track to analyze */
    private static final String PROP_TARGET_TAGS = "target-tags";

    /** The configuration options for this handler */
    private static final SortedMap<String, String> CONFIG_OPTIONS;

    /** Id builder used to create ids for cropped tracks */
    private final IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();


    static {
        CONFIG_OPTIONS = new TreeMap<>();
        CONFIG_OPTIONS.put(PROP_ANALYSIS_TRACK_FLAVOR, "The flavor of the track to analyze. If multiple tracks match this flavor, the first will be used.");
        CONFIG_OPTIONS.put(PROP_TARGET_TAGS, "The tags to apply to the resulting mpeg-7 catalog");
    }

    /** The composer service */
    private CropService cropService = null;

    /** The local workspace */
    private Workspace workspace = null;

    /**
     * {@inheritDoc}
     *
     * @see WorkflowOperationHandler#getConfigurationOptions()
     */
    @Override
    public SortedMap<String, String> getConfigurationOptions() { return CONFIG_OPTIONS; }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance, JobContext)
     */
    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext jobContext) throws WorkflowOperationException {
        logger.debug("Running cropping on workflow {}", workflowInstance.getId());

        final String targetTrackId = idBuilder.createNew().toString();
        WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
        MediaPackage mediaPackage = workflowInstance.getMediaPackage();

        String trackFlavor = StringUtils.trimToNull(operation.getConfiguration(PROP_ANALYSIS_TRACK_FLAVOR));
        List<String> targetTags = asList(operation.getConfiguration(PROP_TARGET_TAGS));
        List<Track> candidates = new ArrayList<>();
        if (trackFlavor != null) {
            candidates.addAll(Arrays.asList(mediaPackage.getTracks(MediaPackageElementFlavor.parseFlavor(trackFlavor))));
        } else {
            candidates.addAll(Arrays.asList(mediaPackage.getTracks(MediaPackageElements.PRESENTATION_SOURCE)));
        }

        Iterator<Track> ti = candidates.iterator();
        while (ti.hasNext()) {
            Track t = ti.next();
            if (!t.hasVideo()) {
                ti.remove();
            }
        }

        if (candidates.size() == 0) {
            logger.info("No matching tracks available for cropping in workflow {}", workflowInstance);
            return createResult(WorkflowOperationResult.Action.CONTINUE);
        }

        if (candidates.size() > 1) {
            logger.info("Found mor than one track to crop, choosing the fist one ({})", candidates.get(0));
        }




        Track track = candidates.get(0);


        Track croppedTrack = null;
        Job job = null;
        try {
            job = cropService.crop(track);
            if (!waitForStatus(job).isSuccess()) {
                throw new WorkflowOperationException("Video cropping of " + track + " failed");
            }

            croppedTrack = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
            mediaPackage.add(croppedTrack);
            croppedTrack.setIdentifier(targetTrackId);
            croppedTrack.setURI(workspace.moveTo(croppedTrack.getURI(), mediaPackage.getIdentifier().toString(),
                    croppedTrack.getIdentifier(),croppedTrack.getIdentifier() + "corpped"));
            croppedTrack.setMimeType(track.getMimeType());

            // Add target tags
            for (String tag : targetTags) {
                croppedTrack.addTag(tag);
            }
        } catch (Exception e) {
            throw new WorkflowOperationException(e);
        }


        logger.info("Video cropping completed");
        return createResult(mediaPackage, WorkflowOperationResult.Action.CONTINUE, job.getQueueTime());
    }

    /**
     * Callback for declarative services configuration that will introduce us to the crop service.
     * Implementation assumes that the reference is configured as being static.
     *
     * @param cropService
     *          the crop service
     */
    protected void setCropService(CropService cropService) { this.cropService = cropService; }


    /**
     * Callback for declarative services configuration that will introduce us to the local workspace service.
     * Implementation assumes that the reference is configured as being static.
     *
     * @param workspace
     *          an instance of the workspace
     */
    protected void setWorkspace(Workspace workspace) { this.workspace = workspace; }
}
