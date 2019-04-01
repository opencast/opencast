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
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.PathSupport
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

/**
 * The workflow definition for handling multiple concurrent outputs in one ffmpeg operation. This allows encoding and
 * tagging to be done in one operation
 */
class MultiEncodeWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The composer service  */
    private var composerService: ComposerService? = null

    /** The local workspace  */
    private var workspace: Workspace? = null

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
        logger.debug("Running Multiencode workflow operation on workflow {}", workflowInstance.id)

        try {
            return multiencode(workflowInstance.mediaPackage, workflowInstance.currentOperation)
        } catch (e: Exception) {
            throw WorkflowOperationException(e)
        }

    }

    protected inner class ElementProfileTagFlavor internal constructor(profiles: String) {
        val selector: AbstractMediaPackageElementSelector<Track> = TrackSelector()
        internal var targetFlavor: String? = null
        internal var targetTags: String? = null
        private val encodingProfiles = ArrayList<String>() // redundant storage
        private val encodingProfileList = ArrayList<EncodingProfile>()

        val profiles: List<String>
            get() = this.encodingProfiles

        init {
            val profilelist = asList(profiles)
            for (profile in profilelist) {
                val encodingprofile = composerService!!.getProfile(profile)
                if (encodingprofile != null)
                    encodingProfiles.add(encodingprofile.identifier)
                encodingProfileList.add(encodingprofile)
            }
        }

        internal fun addSourceFlavor(flavor: String) {
            this.selector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor))
        }

        internal fun addSourceTag(tag: String) {
            this.selector.addTag(tag)
        }
    }

/*
   * Figures out the logic of all the source tags, flavors and profiles and sorts out the source tracks and
   * corresponding encoding profiles.
   *
   * Source Tracks are selected by (Flavor AND Tag) if they are both provided
   *
   * There can be multiple sources and flavors to create more than one source tracks. In the workflow, A semi-colon ";"
   * is used to separate the independent operations.
   *
   * The independent operations can be either all share the same set of properties or all have different sets of
   * properties. For example, There are two sets of source flavors: * "presenter/* ; presentation/*", one source tag,
   * eg: "preview", and two sets of encoding profiles, eg: "mp4,flv ; mp4,hdtv" then there are two concurrent
   * operations: the first one is all "presenter" tracks tagged "preview" will be encoded with "mp4" and "flv". The
   * second one is all "presentation" tracks tagged "preview" encoded with "mp4" and "hdtv"
   *
   */
  @Throws(WorkflowOperationException::class)
private fun getSrcSelector(sourceFlavors:Array<String>?, sourceTags:Array<String>?,
targetFlavors:Array<String>?, targetTags:Array<String>?, profiles:Array<String>?):List<ElementProfileTagFlavor> {
var n = 0
val elementSelectors = ArrayList<ElementProfileTagFlavor>()
if (sourceTags == null && sourceFlavors == null)
throw WorkflowOperationException("No source tags or Flavor")
if (profiles == null)
throw WorkflowOperationException("Missing profiles")
if (sourceTags != null)
{ // If source tags are used to select tracks
 // If use source and target tags, there should be the same number of them or all map into one target
      if (targetTags != null && (targetTags!!.size != 1 && sourceTags!!.size != targetTags!!.size))
throw WorkflowOperationException(("number of source tags " + sourceTags!!.size
+ " does not match number of target tags " + targetTags!!.size + " (must be the same or one target)"))
 // There should be the same number of source tags or profile groups or all use same group of profiles
      if (profiles!!.size != 1 && sourceTags!!.size != profiles!!.size)
{
throw WorkflowOperationException(
("number of source tags segments " + sourceTags!!.size + " does not match number of profiles segments "
+ profiles!!.size + " (must be the same or one profile)"))
}
 // If use source tags and source flavors, there should be the same number of them or one
      if ((sourceFlavors != null && (sourceTags!!.size != 1 && sourceFlavors!!.size != 1)
&& sourceFlavors!!.size != sourceTags!!.size))
{
throw WorkflowOperationException(("number of source tags segments " + sourceTags!!.size
+ " does not match number of source Flavor segments " + sourceFlavors!!.size
+ " (must be the same or one)"))
}
n = sourceTags!!.size // at least this many tracks
}
if (sourceFlavors != null)
{ // If flavors are used to select tracks
 // If use source and target flavors, there should be the same number of them or all map into one target
      if (targetFlavors != null && (targetFlavors!!.size != 1 && sourceFlavors!!.size != targetFlavors!!.size))
{
throw WorkflowOperationException(
("number of source flavors " + sourceFlavors!!.size + " segment does not match number of target flavors"
+ targetFlavors!!.size + " (must be the same or one target flavor)"))
}
 // If use target tags, there should be the same number of source flavors and target tags or all map into one
      // target tag
      if (targetTags != null && targetTags!!.size != 1 && sourceFlavors!!.size != targetTags!!.size)
{
throw WorkflowOperationException(
("number of source flavors " + sourceFlavors!!.size + " segment does not match number of target Tags"
+ targetTags!!.size + " (must be the same or one target)"))
}
 // Number of profile groups should match number of source flavors
      if ((profiles!!.size != 1 && sourceFlavors!!.size != profiles!!.size))
{
throw WorkflowOperationException(("number of source flavors segments " + sourceFlavors!!.size
+ " does not match number of profiles segments " + profiles!!.size
+ " (must be the same or one profile)"))
}
if (sourceFlavors!!.size > n)
n = sourceFlavors!!.size // at least this many tracks
}
var numProfiles = 0
 // One for each source flavor
    for (i in 0 until n)
{
elementSelectors.add(ElementProfileTagFlavor(profiles!![numProfiles]))
if (profiles!!.size > 1)
numProfiles++ // All source use the same set of profiles or its own
}
 // If uses tags to select, but sets target flavor, they must match
    if (sourceTags != null && sourceFlavors != null)
{
if (sourceTags!!.size != sourceFlavors!!.size && sourceFlavors!!.size != 1 && sourceTags!!.size != 1)
{
throw WorkflowOperationException(
("number of source flavors " + sourceTags!!.size + " does not match number of source tags "
+ sourceFlavors!!.size + " (must be the same or one set of tags or flavors)"))
}
}
populateFlavorsAndTags(elementSelectors, sourceFlavors, targetFlavors, sourceTags, targetTags)
return elementSelectors
}

@Throws(WorkflowOperationException::class)
private fun populateFlavorsAndTags(elementSelectors:List<ElementProfileTagFlavor>,
sourceFlavors:Array<String>?, targetFlavors:Array<String>?, sourceTags:Array<String>?, targetTags:Array<String>?):List<ElementProfileTagFlavor> {
var sf = 0
var tf = 0
var st = 0
var tt = 0
for (ep in elementSelectors)
{
try
{
if (sourceTags != null)
{
for (tag in asList(sourceTags!![st]))
{
ep.addSourceTag(tag)
}
if (sourceTags!!.size != 1)
st++
}
if (targetTags != null)
{
ep.targetTags = targetTags!![tt]
if (targetTags!!.size != 1)
tt++
}
if (sourceFlavors != null)
{
for (flavor in asList(sourceFlavors!![sf]))
{
ep.addSourceFlavor(flavor)
}
if (sourceFlavors!!.size != 1)
sf++
}
if (targetFlavors != null)
{
for (flavor in asList(targetFlavors!![tf]))
{
ep.targetFlavor = flavor
}
if (targetFlavors!!.size != 1)
tf++
}
}
catch (e:IllegalArgumentException) {
throw WorkflowOperationException("Set Tags or Flavor " + e.message)
}

}
return elementSelectors
}

private fun getConfigAsArray(operation:WorkflowOperationInstance, name:String):Array<String>? {
val sourceOption = StringUtils.trimToNull(operation.getConfiguration(name))
return StringUtils.split(sourceOption, SEPARATOR)
}

 /*
   * Encode multiple tracks in a mediaPackage concurrently with different encoding profiles for each track. The encoding
   * profiles are specified by names in a list and are the names used to tag each corresponding output. Each source
   * track will start one operation on one worker. concurrency is achieved by running on different workers
   *
   * @param src The source media package
   *
   * @param operation the current workflow operation
   *
   * @return the operation result containing the updated media package
   *
   * @throws EncoderException if encoding fails
   *
   * @throws WorkflowOperationException if errors occur during processing
   *
   * @throws IOException if the workspace operations fail
   *
   * @throws NotFoundException if the workspace doesn't contain the requested file
   */
  @Throws(EncoderException::class, IOException::class, NotFoundException::class, MediaPackageException::class, WorkflowOperationException::class)
private fun multiencode(src:MediaPackage, operation:WorkflowOperationInstance):WorkflowOperationResult {
val mediaPackage = src.clone() as MediaPackage
 // Check which tags have been configured
    val sourceTags = getConfigAsArray(operation, "source-tags")
val sourceFlavors = getConfigAsArray(operation, "source-flavors")
val targetTags = getConfigAsArray(operation, "target-tags")
val targetFlavors = getConfigAsArray(operation, "target-flavors")
val tagWithProfileConfig = StringUtils.trimToNull(operation.getConfiguration("tag-with-profile"))
val tagWithProfile = BooleanUtils.toBoolean(tagWithProfileConfig)

 // Make sure either one of tags or flavors are provided
    if (sourceFlavors == null && sourceTags == null)
{
logger.info("No source tags or flavors have been specified, not matching anything")
return createResult(mediaPackage, Action.CONTINUE)
}
val profiles = getConfigAsArray(operation, "encoding-profiles")
if (profiles == null)
throw WorkflowOperationException("Missing encoding profiles")

 // Sort out the combinatorics of all the tags and flavors
    val selectors = getSrcSelector(sourceFlavors, sourceTags, targetFlavors, targetTags,
profiles)

var totalTimeInQueue:Long = 0
val encodingJobs = HashMap<Job, JobInformation>()
 // Find the encoding profiles - should only be one per flavor or tag
    for (eptf in selectors)
{
 // Look for elements matching the tag and flavor
      val elements = eptf.selector.select(mediaPackage, true)
for (sourceTrack in elements)
{
logger.info("Encoding track {} using encoding profile '{}'", sourceTrack, eptf.profiles.get(0).toString())
 // Start encoding and wait for the result
        encodingJobs.put(composerService!!.multiEncode(sourceTrack, eptf.profiles),
JobInformation(sourceTrack, eptf, tagWithProfile))
}
}

if (encodingJobs.isEmpty())
{
logger.info("No matching tracks found")
return createResult(mediaPackage, Action.CONTINUE)
}

 // Wait for the jobs to return
    if (!waitForStatus(*encodingJobs.keys.toTypedArray<Job>()).isSuccess)
{
throw WorkflowOperationException("One of the encoding jobs did not complete successfully")
}

 // Process the result
    for (entry in encodingJobs.entries)
{
val job = entry.key
val sourceTrack = entry.value.track // source
val info = entry.value.info // tags and flavors
 // add this receipt's queue time to the total
      totalTimeInQueue += job.queueTime!!
 // it is allowed for compose jobs to return an empty payload. See the EncodeEngine interface
      if (job.payload.length > 0)
{
val composedTracks = MediaPackageElementParser.getArrayFromXml(job.payload) as List<Track>
if (composedTracks.size != info!!.profiles.size)
{
logger.info("Encoded {} tracks, with {} profiles", composedTracks.size, info!!.profiles.size)
throw WorkflowOperationException("Number of output tracks does not match number of encoding profiles")
}
for (composedTrack in composedTracks)
{
if (info!!.targetFlavor != null)
{ // Has Flavors
 // set it to the matching flavor in the order listed
            composedTrack.flavor = newFlavor(sourceTrack, info!!.targetFlavor)
logger.debug("Composed track has flavor '{}'", composedTrack.flavor)
}
if (info!!.targetTags != null)
{ // Has Tags
for (tag in asList(info!!.targetTags))
{
logger.trace("Tagging composed track with '{}'", tag)
composedTrack.addTag(tag)
}
}
if (entry.value.tagWithProfile)
{
 // Tag each output with encoding profile name if configured
            val rawfileName = composedTrack.getURI().getRawPath()
val eps = entry.value.profileList
for (ep in eps)
{
var suffix = ep.getSuffix()
 // !! workspace.putInCollection renames the file - need to do the same with suffix
              suffix = PathSupport.toSafeName(suffix)
if (suffix.length > 0 && rawfileName.endsWith(suffix))
{
composedTrack.addTag(ep.getIdentifier())
logger.debug("Tagging composed track {} with '{}'", composedTrack.getURI(), ep.getIdentifier())
break
}
}
}
 // store new tracks to mediaPackage
          val fileName = getFileNameFromElements(sourceTrack, composedTrack)
composedTrack.setURI(workspace!!.moveTo(composedTrack.getURI(), mediaPackage.identifier.toString(),
composedTrack.identifier, fileName))
mediaPackage.addDerived(composedTrack, sourceTrack!!)
}
}
else
{
logger.warn("No output from MultiEncode operation")
}
}
val result = createResult(mediaPackage, Action.CONTINUE, totalTimeInQueue)
logger.debug("MultiEncode operation completed")
return result
}

@Throws(WorkflowOperationException::class)
private fun newFlavor(track:Track, flavor:String?):MediaPackageElementFlavor? {
if (StringUtils.isNotBlank(flavor))
{
try
{
val targetFlavor = MediaPackageElementFlavor.parseFlavor(flavor)
var flavorType = targetFlavor.type
var flavorSubtype = targetFlavor.subtype
 // Adjust the target flavor. Make sure to account for partial updates
        if ("*" == flavorType)
flavorType = track.flavor.type
if ("*" == flavorSubtype)
flavorSubtype = track.flavor.subtype
return (MediaPackageElementFlavor(flavorType!!, flavorSubtype!!))
}
catch (e:IllegalArgumentException) {
throw WorkflowOperationException("Target flavor '" + flavor + "' is malformed")
}

}
return null
}

/**
 * This class is used to store context information for the jobs.
 */
  private class JobInformation internal constructor(track:Track, info:ElementProfileTagFlavor,  val tagWithProfile:Boolean) {

/**
 * Returns the track.
 *
 * @return the track
 */
     val track:Track? = null
 val info:ElementProfileTagFlavor? = null

 val profileList:List<EncodingProfile>
get() {
return info!!.encodingProfileList
}

init{
this.track = track
this.info = info
}
}

companion object {

/** The logging facility  */
  private val logger = LoggerFactory.getLogger(MultiEncodeWorkflowOperationHandler::class.java!!)

/** seperator for independent clauses  */
  internal val SEPARATOR = ";"
}

}
