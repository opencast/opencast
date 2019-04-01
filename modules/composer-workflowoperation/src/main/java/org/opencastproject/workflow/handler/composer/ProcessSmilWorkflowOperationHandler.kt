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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workflow.handler.composer

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.EncodingProfile.MediaType
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.smil.api.SmilException
import org.opencastproject.smil.api.SmilResponse
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.PathSupport
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.opencastproject.workflow.handler.composer.ProcessSmilWorkflowOperationHandler.Companion.SEPARATOR
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet

/**
 * The workflow definition for handling "compose" operations
 */
class ProcessSmilWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The composer service  */
    private var composerService: ComposerService? = null
    /** The smil service to parse the smil  */
    private var smilService: SmilService? = null
    /** The local workspace  */
    private var workspace: Workspace? = null

    /**
     * A convenience structure to hold info for each paramgroup in the Smil which will produce one trim/concat/encode job
     */
    private inner class TrackSection internal constructor(private val paramGroupId: String, val flavor: String) {
        /**
         * Set source Tracks for this group, if audio or video is missing in any of the source files, then do not try to
         * edit with the missing media type, because it will fail
         *
         * @param sourceTracks
         */
        var sourceTracks: List<Track>? = null
            set(sourceTracks) {
                var hasVideo = true
                var hasAudio = true
                field = sourceTracks
                for (track in sourceTracks) {
                    if (!track.hasVideo())
                        hasVideo = false
                    if (!track.hasAudio())
                        hasAudio = false
                }
                if (!hasVideo) {
                    mediaType = ComposerService.AUDIO_ONLY
                }
                if (!hasAudio) {
                    mediaType = ComposerService.VIDEO_ONLY
                }
            }
        var smilTrackList: List<String>? = null
        private var mediaType = "" // Has both Audio and Video

        override fun toString(): String {
            return paramGroupId + " " + flavor + " " + this.sourceTracks!!.toString()
        }
    }

    // To return both params from a function that checks all the jobs
    private inner class ResultTally internal constructor(val mediaPackage: MediaPackage, val totalTimeInQueue: Long)

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param composerService
     * the local composer service
     */
    fun setComposerService(composerService: ComposerService) {
        this.composerService = composerService
    }

    /**
     * Callback for the OSGi declarative services configuration.
     *
     * @param smilService
     */
    fun setSmilService(smilService: SmilService) {
        this.smilService = smilService
    }

    /**
     * Callback for declarative services configuration that will introduce us to the local workspace service.
     * Implementation assumes that the reference is configured as being static.
     *
     * @param workspace
     * an instance of the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        try {
            return processSmil(workflowInstance.mediaPackage, workflowInstance.currentOperation)
        } catch (e: Exception) {
            e.printStackTrace()
            throw WorkflowOperationException(e)
        }

    }

    private fun getConfigAsArray(operation: WorkflowOperationInstance, name: String): Array<String>? {
        val sourceOption = StringUtils.trimToNull(operation.getConfiguration(name))
        return sourceOption?.split(SEPARATOR.toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
    }

    private fun collapseConfig(operation: WorkflowOperationInstance, name: String): Array<String>? {
        val targetOption = StringUtils.trimToNull(operation.getConfiguration(name))
        return if (targetOption != null) arrayOf(targetOption.replace(SEPARATOR.toRegex(), ",")) else null
    }

    /**
     * Encode tracks from Smil using profiles stored in properties and updates current MediaPackage. This procedure parses
     * the workflow definitions and decides how many encoding jobs are needed
     *
     * @param src
     * The source media package
     * @param operation
     * the current workflow operation
     * @return the operation result containing the updated media package
     * @throws EncoderException
     * if encoding fails
     * @throws WorkflowOperationException
     * if errors occur during processing
     * @throws IOException
     * if the workspace operations fail
     * @throws NotFoundException
     * if the workspace doesn't contain the requested file
     */
    @Throws(EncoderException::class, IOException::class, NotFoundException::class, MediaPackageException::class, WorkflowOperationException::class)
    private fun processSmil(src: MediaPackage, operation: WorkflowOperationInstance): WorkflowOperationResult {
        val mediaPackage = src.clone() as MediaPackage
        // Check which tags have been configured
        val smilFlavorOption = StringUtils.trimToEmpty(operation.getConfiguration("smil-flavor"))
        val srcFlavors = getConfigAsArray(operation, "source-flavors")
        var targetFlavors = getConfigAsArray(operation, "target-flavors")
        var targetTags = getConfigAsArray(operation, "target-tags")
        var profilesSections = getConfigAsArray(operation, "encoding-profiles")
        val tagWithProfileConfig = StringUtils.trimToNull(operation.getConfiguration("tag-with-profile"))
        val tagWithProfile = tagWithProfileConfig != null && java.lang.Boolean.parseBoolean(tagWithProfileConfig)

        // Make sure there is a smil src
        if (StringUtils.isBlank(smilFlavorOption)) {
            logger.info("No smil flavor has been specified, no src to process") // Must have Smil input
            return createResult(mediaPackage, Action.CONTINUE)
        }

        if (srcFlavors == null) {
            logger.info("No source flavors have been specified, not matching anything")
            return createResult(mediaPackage, Action.CONTINUE) // Should be OK
        }
        // Make sure at least one encoding profile is provided
        if (profilesSections == null) {
            throw WorkflowOperationException("No encoding profile was specified")
        }

        /*
     * Must have smil file, and encoding profile(s) If source-flavors is used, then target-flavors must be used If
     * separators ";" are used in source-flavors, then there must be the equivalent number of matching target-flavors
     * and encoding profiles used, or one for all of them.
     */
        if (srcFlavors.size > 1) { // Different processing for each flavor
            if (targetFlavors != null && srcFlavors.size != targetFlavors.size && targetFlavors.size != 1) {
                val mesg = ("Number of target flavor sections " + targetFlavors + " must either match that of src flavor "
                        + srcFlavors + " or equal 1 ")
                throw WorkflowOperationException(mesg)
            }
            if (srcFlavors.size != profilesSections.size) {
                if (profilesSections.size != 1) {
                    val mesg = ("Number of encoding profile sections " + profilesSections
                            + " must either match that of src flavor " + srcFlavors + " or equal 1 ")
                    throw WorkflowOperationException(mesg)
                } else { // we need to duplicate profileSections for each src selector
                    val array = arrayOfNulls<String>(srcFlavors.size)
                    Arrays.fill(array, 0, srcFlavors.size, profilesSections[0])
                    profilesSections = array
                }
            }
            if (targetTags != null && srcFlavors.size != targetTags.size && targetTags.size != 1) {
                val mesg = ("Number of target Tags sections " + targetTags + " must either match that of src flavor "
                        + srcFlavors + " or equal 1 ")
                throw WorkflowOperationException(mesg)
            }
        } else { // Only one srcFlavor - collapse all sections into one
            targetFlavors = collapseConfig(operation, "target-flavors")
            targetTags = collapseConfig(operation, "target-tags")
            profilesSections = collapseConfig(operation, "encoding-profiles")
            if (profilesSections!!.size != 1)
                throw WorkflowOperationException(
                        "No matching src flavors $srcFlavors for encoding profiles sections $profilesSections")

            logger.debug("Single input flavor: output= " + Arrays.toString(targetFlavors) + " tag: "
                    + Arrays.toString(targetTags) + " profile:" + Arrays.toString(profilesSections))
        }

        val encodingJobs = HashMap<Job, JobInformation>()
        for (i in profilesSections.indices) {
            // Each section is one multiconcatTrim job - set up the jobs
            processSection(encodingJobs, mediaPackage, operation, if (srcFlavors.size > 1) srcFlavors[i] else srcFlavors[0],
                    if (targetFlavors != null) if (targetFlavors.size > 1) targetFlavors[i] else targetFlavors[0] else null,
                    if (targetTags != null) if (targetTags.size > 1) targetTags[i] else targetTags[0] else null,
                    if (profilesSections.size > 0) profilesSections[i] else profilesSections[0], smilFlavorOption,
                    tagWithProfile)
        }

        if (encodingJobs.isEmpty()) {
            logger.info("Failed to process any tracks")
            return createResult(mediaPackage, Action.CONTINUE)
        }

        // Wait for the jobs to return
        if (!waitForStatus(*encodingJobs.keys.toTypedArray()).isSuccess) {
            throw WorkflowOperationException("One of the encoding jobs did not complete successfully")
        }
        val allResults = parseResults(encodingJobs, mediaPackage)
        val result = createResult(allResults.mediaPackage, Action.CONTINUE,
                allResults.totalTimeInQueue)
        logger.debug("ProcessSmil operation completed")
        return result

    }

    /**
     * Process one group encode section with one source Flavor declaration(may be wildcard) , sharing one set of shared
     * optional target tags/flavors and one set of encoding profiles
     *
     * @param encodingJobs
     * @param mediaPackage
     * @param operation
     * @param srcFlavors
     * - used to select which param group/tracks to process
     * @param targetFlavors
     * - the resultant track will be tagged with these flavors
     * @param targetTags
     * - the resultant track will be tagged
     * @param media
     * - if video or audio only
     * @param encodingProfiles
     * - profiles to use, if ant of them does not fit the source tracks, they will be omitted
     * @param smilFlavor
     * - the smil flavor for the input smil
     * @param tagWithProfile - tag target with profile name
     * @throws WorkflowOperationException
     * if flavors/tags/etc are malformed or missing
     * @throws EncoderException
     * if encoding command cannot be constructed
     * @throws MediaPackageException
     * @throws IllegalArgumentException
     * @throws NotFoundException
     * @throws IOException
     */
    @Throws(WorkflowOperationException::class, EncoderException::class, MediaPackageException::class, IllegalArgumentException::class, NotFoundException::class, IOException::class)
    private fun processSection(encodingJobs: MutableMap<Job, JobInformation>, mediaPackage: MediaPackage,
                               operation: WorkflowOperationInstance, srcFlavors: String, targetFlavors: String?, targetTags: String?,
                               encodingProfiles: String, smilFlavor: String, tagWithProfile: Boolean) {
        // Select the source flavors
        val elementSelector = TrackSelector()
        for (flavor in asList(srcFlavors)) {
            try {
                elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Source flavor '$flavor' is malformed")
            }

        }
        val smil = getSmil(mediaPackage, smilFlavor)
        // Check that the matching source tracks exist in the SMIL
        val smilgroups: List<TrackSection>
        try {
            smilgroups = selectTracksFromMP(mediaPackage, smil, srcFlavors)
        } catch (e1: URISyntaxException) {
            logger.info("Smil contains bad URI {}", e1)
            throw WorkflowOperationException("Smil contains bad URI - cannot process", e1)
        }

        if (smilgroups.size == 0 || smilgroups[0].sourceTracks!!.size == 0) {
            logger.info("Smil does not contain any tracks of {} source flavor", srcFlavors)
            return
        }

        // Check Target flavor
        var targetFlavor: MediaPackageElementFlavor? = null
        if (StringUtils.isNotBlank(targetFlavors)) {
            try {
                targetFlavor = MediaPackageElementFlavor.parseFlavor(targetFlavors)
            } catch (e: IllegalArgumentException) {
                throw WorkflowOperationException("Target flavor '$targetFlavors' is malformed")
            }

        }

        val profiles = HashSet<EncodingProfile>()
        val profileNames = HashSet<String>()
        // Find all the encoding profiles
        // Check that the profiles support the media source types
        for (ts in smilgroups)
            for (track in ts.sourceTracks!!) {
                // Check that the profile is supported
                for (profileName in asList(encodingProfiles)) {
                    val profile = composerService!!.getProfile(profileName)
                            ?: throw WorkflowOperationException("Encoding profile '$profileName' was not found")
                    val outputType = profile.outputType
                    // Check if the track supports the output type of the profile MediaType outputType = profile.getOutputType();
                    // Omit if needed
                    if (outputType == MediaType.Audio && !track.hasAudio()) {
                        logger.info("Skipping encoding of '{}' with $profileName, since the track lacks an audio stream",
                                track)
                        continue
                    } else if (outputType == MediaType.Visual && !track.hasVideo()) {
                        logger.info("Skipping encoding of '{}' $profileName, since the track lacks a video stream", track)
                        continue
                    } else if (outputType == MediaType.AudioVisual && !track.hasAudio() && !track.hasVideo()) {
                        logger.info("Skipping encoding of '{}' (audiovisual)" + profileName
                                + ", since it lacks a audio or video stream", track)
                        continue
                    }
                    profiles.add(profile) // Include this profiles for encoding
                    profileNames.add(profileName)
                }
            }
        // Make sure there is at least one profile
        if (profiles.isEmpty())
            throw WorkflowOperationException("No encoding profile was specified")

        val tags = if (targetTags != null) asList(targetTags) else null
        // Encode all tracks found in each param group
        // Start encoding and wait for the result - usually one for presenter, one for presentation
        for (trackGroup in smilgroups) {
            encodingJobs[composerService!!.processSmil(smil, trackGroup.paramGroupId, trackGroup.mediaType,
                    ArrayList(profileNames))] = JobInformation(trackGroup.paramGroupId, trackGroup.sourceTracks,
                    ArrayList(profiles), tags, targetFlavor, tagWithProfile)

            logger.info("Edit and encode {} target flavors: {} tags: {} profile {}", trackGroup, targetFlavor, tags,
                    profileNames)
        }
    }

    /**
     * parse all the encoding jobs to collect all the composed tracks, if any of them fail, just fail the whole thing and
     * try to clean up
     *
     * @param encodingJobs
     * - queued jobs to do the encodings, this is parsed for payload
     * @param mediaPackage
     * - to hold the target tracks
     * @return a structure with time in queue plus a mediaPackage with all the new tracks added if all the encoding jobs
     * passed, if any of them fail, just fail the whole thing and try to clean up
     * @throws IllegalArgumentException
     * @throws NotFoundException
     * @throws IOException
     * @throws MediaPackageException
     */
    @Throws(IllegalArgumentException::class, NotFoundException::class, IOException::class, MediaPackageException::class)
    private fun parseResults(encodingJobs: Map<Job, JobInformation>, mediaPackage: MediaPackage): ResultTally {
        // Process the result
        var totalTimeInQueue: Long = 0
        for ((job, value) in encodingJobs) {
            val tracks = value.tracks
            val track = tracks[0] // Can only reference one track, pick one
            // add this receipt's queue time to the total
            totalTimeInQueue += job.queueTime!!
            // it is allowed for compose jobs to return an empty payload. See the EncodeEngine interface
            var composedTracks: List<Track>? = null
            if (job.payload.length > 0) {
                composedTracks = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Track>
                // Adjust the target tags
                for (composedTrack in composedTracks) {
                    if (value.tags != null) {
                        for (tag in value.tags) {
                            composedTrack.addTag(tag)
                        }
                    }
                    // Adjust the target flavor. Make sure to account for partial updates
                    val targetFlavor = value.flavor
                    if (targetFlavor != null) {
                        var flavorType = targetFlavor.type
                        var flavorSubtype = targetFlavor.subtype
                        if ("*" == flavorType)
                            flavorType = track.flavor.type
                        if ("*" == flavorSubtype)
                            flavorSubtype = track.flavor.subtype
                        composedTrack.flavor = MediaPackageElementFlavor(flavorType!!, flavorSubtype!!)
                        logger.debug("Composed track has flavor '{}'", composedTrack.flavor)
                    }
                    val fileName = composedTrack.getURI().getRawPath()
                    if (value.tagProfile) {
                        // Tag each output with encoding profile name, if configured
                        val eps = value.profiles
                        for (ep in eps) {
                            var suffix = ep.suffix
                            // !! workspace.putInCollection renames the file - need to do the same with suffix
                            suffix = PathSupport.toSafeName(suffix)
                            if (suffix.length > 0 && fileName.endsWith(suffix)) {
                                composedTrack.addTag(ep.identifier)
                                logger.debug("Tagging composed track {} with '{}'", composedTrack.getURI(), ep.identifier)
                                break
                            }
                        }
                    }

                    composedTrack.setURI(workspace!!.moveTo(composedTrack.getURI(), mediaPackage.identifier.toString(),
                            composedTrack.identifier, fileName))
                    synchronized(mediaPackage) {
                        mediaPackage.addDerived(composedTrack, track)
                    }
                }
            }
        }
        return ResultTally(mediaPackage, totalTimeInQueue)
    }

    /**
     * @param trackFlavor
     * @param sourceFlavor
     * @return true if trackFlavor matches sourceFlavor
     */
    private fun trackMatchesFlavor(trackFlavor: MediaPackageElementFlavor, sourceFlavor: MediaPackageElementFlavor): Boolean {
        return ((trackFlavor.type == sourceFlavor.type && trackFlavor.subtype // exact match
                == sourceFlavor.subtype)
                || "*" == sourceFlavor.type && trackFlavor.subtype == sourceFlavor.subtype // same

                // subflavor
                || trackFlavor.type == sourceFlavor.type && "*" == sourceFlavor.subtype) // same
        // flavor
    }

    /**
     * @param mediaPackage
     * - mp obj contains tracks
     * @param smil
     * - smil obj contains description of clips
     * @param srcFlavors
     * - source flavor string (may contain wild cards)
     * @return a structure of smil groups, each with a single flavor and mp tracks for that flavor only
     * @throws WorkflowOperationException
     * @throws URISyntaxException
     */
    @Throws(WorkflowOperationException::class, URISyntaxException::class)
    private fun selectTracksFromMP(mediaPackage: MediaPackage, smil: Smil?, srcFlavors: String): List<TrackSection> {
        val sourceTrackList = ArrayList<TrackSection>()
        val smilFlavors = parseSmil(smil!!)
        val it = smilFlavors.iterator()
        while (it.hasNext()) {
            val ts = it.next()

            for (f in StringUtils.split(srcFlavors, ",")) { // Look for all source Flavors
                val sourceFlavorStr = StringUtils.trimToNull(f) ?: continue
                val sourceFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavorStr)
                val trackFlavor = MediaPackageElementFlavor.parseFlavor(ts.flavor)

                if (trackMatchesFlavor(trackFlavor, sourceFlavor)) {
                    sourceTrackList.add(ts) // This smil group matches src Flavor, add to list
                    var elements: Array<Track>? = null
                    val sourceTracks = ArrayList<Track>()
                    elements = mediaPackage.getTracks(sourceFlavor)
                    for (t in ts.smilTrackList!!) { // Look thru all the tracks referenced by the smil
                        val turi = URI(t)
                        for (e in elements!!)
                            if (e.getURI().equals(turi)) { // find it in the mp
                                sourceTracks.add(e) // add the track from mp containing inspection info
                            }
                    }
                    if (sourceTracks.isEmpty()) {
                        logger.info("ProcessSmil - No tracks in mediapackage matching the URI in the smil- cannot process")
                        throw WorkflowOperationException("Smil has no matching tracks in the mediapackage")
                    }
                    ts.sourceTracks = sourceTracks // Will also if srcTracks are Video/Audio Only
                }
            }
        }
        return sourceTrackList
    }

    /**
     * Get smil from media package
     *
     * @param mp
     * @param smilFlavorOption
     * @return smil
     * @throws WorkflowOperationException
     */
    @Throws(WorkflowOperationException::class)
    private fun getSmil(mp: MediaPackage, smilFlavorOption: String): Smil? {
        val smilFlavor = MediaPackageElementFlavor.parseFlavor(smilFlavorOption)
        val catalogs = mp.getCatalogs(smilFlavor)
        if (catalogs.size == 0) {
            throw WorkflowOperationException("MediaPackage does not contain a SMIL document.")
        }
        var smil: Smil? = null
        try {
            val smilFile = workspace!!.get(catalogs[0].getURI())
            // break up chained method for junit smil service mockup
            val response = smilService!!.fromXml(FileUtils.readFileToString(smilFile, "UTF-8"))
            smil = response.smil
            return smil
        } catch (ex: NotFoundException) {
            throw WorkflowOperationException("MediaPackage does not contain a smil catalog.")
        } catch (ex: IOException) {
            throw WorkflowOperationException("Failed to read smil catalog.", ex)
        } catch (ex: SmilException) {
            throw WorkflowOperationException(ex)
        }

    }

    /**
     * Sort paramGroup by flavor, each one will be a separate job
     *
     * @param smil
     * @return TrackSection
     */
    private fun parseSmil(smil: Smil): Collection<TrackSection> {
        // get all source tracks
        val trackGroups = ArrayList<TrackSection>()
        // Find the track flavors, and find track groups that matches the flavors
        for (paramGroup in smil.head.paramGroups) { // For each group look at elements
            var ts: TrackSection? = null
            val src = ArrayList<String>()
            for (param in paramGroup.params) {
                if (SmilMediaParam.PARAM_NAME_TRACK_FLAVOR.matches(param.name.toRegex())) { // Is a flavor
                    ts = TrackSection(paramGroup.id, param.value)
                    trackGroups.add(ts)
                }
                if (SmilMediaParam.PARAM_NAME_TRACK_SRC.matches(param.name.toRegex())) { // Is a track
                    src.add(param.value)
                }
            }
            if (ts != null)
                ts.smilTrackList = src
        }
        return trackGroups
    }

    /**
     * This class is used to store context information for the jobs.
     */
    private class JobInformation internal constructor(paramgroup: String, val tracks: List<Track>, val profiles: List<EncodingProfile>, tags: List<String>,
                                                      flavor: MediaPackageElementFlavor, val tagProfile: Boolean) {
        val groups: String? = null
        val flavor: MediaPackageElementFlavor? = null
        val tags: List<String>? = null

        init {
            this.groups = paramgroup
            this.tags = tags
            this.flavor = flavor
        }

    }

    companion object {
        internal val SEPARATOR = ";"
        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ProcessSmilWorkflowOperationHandler::class.java)
    }

}
