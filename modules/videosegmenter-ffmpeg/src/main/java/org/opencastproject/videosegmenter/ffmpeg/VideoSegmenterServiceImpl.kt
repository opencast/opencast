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

package org.opencastproject.videosegmenter.ffmpeg

import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.metadata.mpeg7.MediaLocator
import org.opencastproject.metadata.mpeg7.MediaLocatorImpl
import org.opencastproject.metadata.mpeg7.MediaRelTimeImpl
import org.opencastproject.metadata.mpeg7.MediaTime
import org.opencastproject.metadata.mpeg7.MediaTimePoint
import org.opencastproject.metadata.mpeg7.MediaTimePointImpl
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService
import org.opencastproject.metadata.mpeg7.Segment
import org.opencastproject.metadata.mpeg7.Video
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.videosegmenter.api.VideoSegmenterException
import org.opencastproject.videosegmenter.api.VideoSegmenterService
import org.opencastproject.workspace.api.Workspace

import com.google.common.io.LineReader

import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URL
import java.util.Arrays
import java.util.Collections
import java.util.Dictionary
import java.util.LinkedList
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Media analysis plugin that takes a video stream and extracts video segments
 * by trying to detect slide and/or scene changes.
 *
 * This plugin runs
 *
 * <pre>
 * ffmpeg -nostats -i in.mp4 -filter:v 'select=gt(scene\,0.04),showinfo' -f null - 2&gt;&amp;1 | grep Parsed_showinfo_1
</pre> *
 */
class VideoSegmenterServiceImpl : AbstractJobProducer(JOB_TYPE), VideoSegmenterService, ManagedService {

    /** Path to the executable  */
    protected var binary: String

    /** The load introduced on the system by creating a caption job  */
    private var segmenterJobLoad = DEFAULT_SEGMENTER_JOB_LOAD

    /** Number of pixels that may change between two frames without considering them different  */
    var changesThreshold = DEFAULT_CHANGES_THRESHOLD

    /** The number of seconds that need to resemble until a scene is considered "stable"  */
    var stabilityThreshold = DEFAULT_STABILITY_THRESHOLD

    /** The minimum segment length in seconds for creation of segments from ffmpeg output  */
    protected var stabilityThresholdPrefilter = 1

    /** The number of segments that should be generated  */
    var prefNumber = DEFAULT_PREF_NUMBER

    /** The number of cycles after which the optimization of the number of segments is forced to end  */
    var maxCycles = DEFAULT_MAX_CYCLES

    /** The tolerance with which the optimization of the number of segments is considered successful  */
    var maxError = DEFAULT_MAX_ERROR

    /** The absolute maximum for the number of segments whose compliance will be enforced after the optimization */
    protected var absoluteMax = DEFAULT_ABSOLUTE_MAX

    /** The absolute minimum for the number of segments whose compliance will be enforced after the optimization */
    var absoluteMin = DEFAULT_ABSOLUTE_MIN

    /** The boolean that defines whether segment numbers are interpreted as absolute or relative to track duration  */
    protected var durationDependent = DEFAULT_DURATION_DEPENDENT

    /** Reference to the receipt service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getServiceRegistry
     */
    /**
     * Sets the receipt service
     *
     * @param serviceRegistry
     * the service registry
     */
    protected override var serviceRegistry: ServiceRegistry? = null
        set

    /** The mpeg-7 service  */
    var mpeg7CatalogService: Mpeg7CatalogService? = null

    /** The workspace to use when retrieving remote media files  */
    protected var workspace: Workspace? = null

    /** The security service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getSecurityService
     */
    /**
     * Callback for setting the security service.
     *
     * @param securityService
     * the securityService to set
     */
    override var securityService: SecurityService? = null
        set

    /** The user directory service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getUserDirectoryService
     */
    /**
     * Callback for setting the user directory service.
     *
     * @param userDirectoryService
     * the userDirectoryService to set
     */
    override var userDirectoryService: UserDirectoryService? = null
        set

    /** The organization directory service  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getOrganizationDirectoryService
     */
    /**
     * Sets a reference to the organization directory service.
     *
     * @param organizationDirectory
     * the organization directory
     */
    override var organizationDirectoryService: OrganizationDirectoryService? = null
        set

    /** List of available operations on jobs  */
    private enum class Operation {
        Segment
    }

    /**
     * Creates a new instance of the video segmenter service.
     */
    init {
        this.binary = FFMPEG_BINARY_DEFAULT
    }

    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        /* Configure segmenter */
        val path = cc.bundleContext.getProperty(FFMPEG_BINARY_CONFIG)
        this.binary = path ?: FFMPEG_BINARY_DEFAULT
        logger.debug("Configuration {}: {}", FFMPEG_BINARY_CONFIG, FFMPEG_BINARY_DEFAULT)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.service.cm.ManagedService.updated
     */
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null) {
            return
        }
        logger.debug("Configuring the videosegmenter")

        // Stability threshold
        if (properties.get(OPT_STABILITY_THRESHOLD) != null) {
            val threshold = properties.get(OPT_STABILITY_THRESHOLD) as String
            try {
                stabilityThreshold = Integer.parseInt(threshold)
                logger.info("Stability threshold set to {} consecutive frames", stabilityThreshold)
            } catch (e: Exception) {
                logger.warn("Found illegal value '{}' for videosegmenter's stability threshold", threshold)
            }

        }

        // Changes threshold
        if (properties.get(OPT_CHANGES_THRESHOLD) != null) {
            val threshold = properties.get(OPT_CHANGES_THRESHOLD) as String
            try {
                changesThreshold = java.lang.Float.parseFloat(threshold)
                logger.info("Changes threshold set to {}", changesThreshold)
            } catch (e: Exception) {
                logger.warn("Found illegal value '{}' for videosegmenter's changes threshold", threshold)
            }

        }

        // Preferred Number of Segments
        if (properties.get(OPT_PREF_NUMBER) != null) {
            val number = properties.get(OPT_PREF_NUMBER) as String
            try {
                prefNumber = Integer.parseInt(number)
                logger.info("Preferred number of segments set to {}", prefNumber)
            } catch (e: Exception) {
                logger.warn("Found illegal value '{}' for videosegmenter's preferred number of segments", number)
            }

        }

        // Maximum number of cycles
        if (properties.get(OPT_MAX_CYCLES) != null) {
            val number = properties.get(OPT_MAX_CYCLES) as String
            try {
                maxCycles = Integer.parseInt(number)
                logger.info("Maximum number of cycles set to {}", maxCycles)
            } catch (e: Exception) {
                logger.warn("Found illegal value '{}' for videosegmenter's maximum number of cycles", number)
            }

        }

        // Absolute maximum number of segments
        if (properties.get(OPT_ABSOLUTE_MAX) != null) {
            val number = properties.get(OPT_ABSOLUTE_MAX) as String
            try {
                absoluteMax = Integer.parseInt(number)
                logger.info("Absolute maximum number of segments set to {}", absoluteMax)
            } catch (e: Exception) {
                logger.warn("Found illegal value '{}' for videosegmenter's absolute maximum number of segments", number)
            }

        }

        // Absolute minimum number of segments
        if (properties.get(OPT_ABSOLUTE_MIN) != null) {
            val number = properties.get(OPT_ABSOLUTE_MIN) as String
            try {
                absoluteMin = Integer.parseInt(number)
                logger.info("Absolute minimum number of segments set to {}", absoluteMin)
            } catch (e: Exception) {
                logger.warn("Found illegal value '{}' for videosegmenter's absolute minimum number of segments", number)
            }

        }

        // Dependency on video duration
        if (properties.get(OPT_DURATION_DEPENDENT) != null) {
            val value = properties.get(OPT_DURATION_DEPENDENT) as String
            try {
                durationDependent = java.lang.Boolean.parseBoolean(value)
                logger.info("Dependency on video duration is set to {}", durationDependent)
            } catch (e: Exception) {
                logger.warn("Found illegal value '{}' for videosegmenter's dependency on video duration", value)
            }

        }

        segmenterJobLoad = LoadUtil.getConfiguredLoadValue(properties, SEGMENTER_JOB_LOAD_KEY, DEFAULT_SEGMENTER_JOB_LOAD, serviceRegistry!!)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.videosegmenter.api.VideoSegmenterService.segment
     */
    @Throws(VideoSegmenterException::class, MediaPackageException::class)
    override fun segment(track: Track): Job {
        try {
            return serviceRegistry!!.createJob(JOB_TYPE,
                    Operation.Segment.toString(),
                    Arrays.asList<T>(MediaPackageElementParser.getAsXml(track)), segmenterJobLoad)
        } catch (e: ServiceRegistryException) {
            throw VideoSegmenterException("Unable to create a job", e)
        }

    }

    /**
     * Starts segmentation on the video track identified by
     * `mediapackageId` and `elementId` and returns a
     * receipt containing the final result in the form of anMpeg7Catalog.
     *
     * @param track
     * the element to analyze
     * @return a receipt containing the resulting mpeg-7 catalog
     * @throws VideoSegmenterException
     */
    @Throws(VideoSegmenterException::class, MediaPackageException::class)
    protected fun segment(job: Job, track: Track): Catalog {

        // Make sure the element can be analyzed using this analysis
        // implementation
        if (!track.hasVideo()) {
            logger.warn("Element {} is not a video track", track)
            throw VideoSegmenterException("Element is not a video track")
        }

        try {
            var mpeg7: Mpeg7Catalog

            var mediaFile: File? = null
            var mediaUrl: URL? = null
            try {
                mediaFile = workspace!!.get(track.getURI())
                mediaUrl = mediaFile!!.toURI().toURL()
            } catch (e: NotFoundException) {
                throw VideoSegmenterException(
                        "Error finding the video file in the workspace", e)
            } catch (e: IOException) {
                throw VideoSegmenterException(
                        "Error reading the video file in the workspace", e)
            }

            if (track.duration == null)
                throw MediaPackageException("Track " + track
                        + " does not have a duration")
            logger.info("Track {} loaded, duration is {} s", mediaUrl,
                    track.duration!! / 1000)

            val contentTime = MediaRelTimeImpl(0,
                    track.duration!!)
            val contentLocator = MediaLocatorImpl(track.getURI())

            var videoContent: Video

            logger.debug("changesThreshold: {}, stabilityThreshold: {}", changesThreshold, stabilityThreshold)
            logger.debug("prefNumber: {}, maxCycles: {}", prefNumber, maxCycles)

            var endOptimization = false
            var cycleCount = 0
            var segments: LinkedList<Segment>?
            val optimizationList = LinkedList<OptimizationStep>()
            val unusedResultsList = LinkedList<OptimizationStep>()
            var stepBest = OptimizationStep()

            // local copy of changesThreshold, that can safely be changed over optimization iterations
            var changesThresholdLocal = changesThreshold

            // local copies of prefNumber, absoluteMin and absoluteMax, to make a dependency on track length possible
            var prefNumberLocal = prefNumber
            var absoluteMaxLocal = absoluteMax
            var absoluteMinLocal = absoluteMin

            // if the number of segments should depend on the duration of the track, calculate new values for prefNumber,
            // absoluteMax and absoluteMin with the duration of the track
            if (durationDependent) {
                val trackDurationInHours = track.duration!! / 3600000.0
                prefNumberLocal = Math.round(trackDurationInHours * prefNumberLocal).toInt()
                absoluteMaxLocal = Math.round(trackDurationInHours * absoluteMax).toInt()
                absoluteMinLocal = Math.round(trackDurationInHours * absoluteMin).toInt()

                //make sure prefNumberLocal will never be 0 or negative
                if (prefNumberLocal <= 0) {
                    prefNumberLocal = 1
                }

                logger.info("Numbers of segments are set to be relative to track duration. Therefore for {} the preferred " + "number of segments is {}", mediaUrl, prefNumberLocal)
            }

            logger.info("Starting video segmentation of {}", mediaUrl)


            // optimization loop to get a segmentation with a number of segments close
            // to the desired number of segments
            while (!endOptimization) {

                mpeg7 = mpeg7CatalogService!!.newInstance()
                videoContent = mpeg7.addVideoContent("videosegment",
                        contentTime, contentLocator)


                // run the segmentation with FFmpeg
                segments = runSegmentationFFmpeg(track, videoContent, mediaFile, changesThresholdLocal)


                // calculate errors for "normal" and filtered segmentation
                // and compare them to find better optimization.
                // "normal"
                val currentStep = OptimizationStep(stabilityThreshold,
                        changesThresholdLocal, segments.size, prefNumberLocal, mpeg7, segments)
                // filtered
                val segmentsNew = LinkedList<Segment>()
                val currentStepFiltered = OptimizationStep(
                        stabilityThreshold, changesThresholdLocal, 0,
                        prefNumberLocal, filterSegmentation(segments, track, segmentsNew, stabilityThreshold * 1000), segments)
                currentStepFiltered.setSegmentNumAndRecalcErrors(segmentsNew.size)

                logger.info("Segmentation yields {} segments after filtering", segmentsNew.size)

                val currentStepBest: OptimizationStep

                // save better optimization in optimizationList
                //
                // the unfiltered segmentation is better if
                // - the error is smaller than the error of the filtered segmentation
                // OR - the filtered number of segments is smaller than the preferred number
                //    - and the unfiltered number of segments is bigger than a value that should roughly estimate how many
                //          segments with the length of the stability threshold could maximally be in a video
                //          (this is to make sure that if there are e.g. 1000 segments and the filtering would yield
                //           smaller and smaller results, the stability threshold won't be optimized in the wrong direction)
                //    - and the filtered segmentation is not already better than the maximum error
                if (currentStep.errorAbs <= currentStepFiltered.errorAbs || (segmentsNew.size < prefNumberLocal
                                && currentStep.segmentNum > track.duration!! / 1000.0f / (stabilityThreshold / 2)
                                && currentStepFiltered.errorAbs > maxError)) {

                    optimizationList.add(currentStep)
                    Collections.sort(optimizationList)
                    currentStepBest = currentStep
                    unusedResultsList.add(currentStepFiltered)
                } else {
                    optimizationList.add(currentStepFiltered)
                    Collections.sort(optimizationList)
                    currentStepBest = currentStepFiltered
                }

                cycleCount++

                logger.debug("errorAbs = {}, error = {}", currentStep.errorAbs, currentStep.error)
                logger.debug("changesThreshold = {}", changesThresholdLocal)
                logger.debug("cycleCount = {}", cycleCount)

                // end optimization if maximum number of cycles is reached or if the segmentation is good enough
                if (cycleCount >= maxCycles || currentStepBest.errorAbs <= maxError) {
                    endOptimization = true
                    if (optimizationList.size > 0) {
                        if (optimizationList.first.errorAbs <= optimizationList.last.errorAbs && optimizationList.first.error >= 0) {
                            stepBest = optimizationList.first
                        } else {
                            stepBest = optimizationList.last
                        }
                    }

                    // just to be sure, check if one of the unused results was better
                    for (currentUnusedStep in unusedResultsList) {
                        if (currentUnusedStep.errorAbs < stepBest.errorAbs) {
                            stepBest = unusedResultsList.first
                        }
                    }


                    // continue optimization, calculate new changes threshold for next iteration of optimization
                } else {
                    val first = optimizationList.first
                    val last = optimizationList.last
                    // if this was the first iteration or there are only positive or negative errors,
                    // estimate a new changesThreshold based on the one yielding the smallest error
                    if (optimizationList.size == 1 || first.error < 0 || last.error > 0) {
                        if (currentStepBest.error >= 0) {
                            // if the error is smaller or equal to 1, increase changes threshold weighted with the error
                            if (currentStepBest.error <= 1) {
                                changesThresholdLocal += changesThresholdLocal * currentStepBest.error
                            } else {
                                // if there are more than 2000 segments in the first iteration, set changes threshold to 0.2
                                // to faster reach reasonable segment numbers
                                if (cycleCount <= 1 && currentStep.segmentNum > 2000) {
                                    changesThresholdLocal = 0.2f
                                    // if the error is bigger than one, double the changes threshold, because multiplying
                                    // with a large error can yield a much too high changes threshold
                                } else {
                                    changesThresholdLocal *= 2f
                                }
                            }
                        } else {
                            changesThresholdLocal /= 2f
                        }

                        logger.debug("onesided optimization yields new changesThreshold = {}", changesThresholdLocal)
                        // if there are already iterations with positive and negative errors, choose a changesThreshold between those
                    } else {
                        // for simplicity a linear relationship between the changesThreshold
                        // and the number of generated segments is assumed and based on that
                        // the expected correct changesThreshold is calculated

                        // the new changesThreshold is calculated by averaging the the mean and the mean weighted with errors
                        // because this seemed to yield better results in several cases

                        val x = (first.segmentNum - prefNumberLocal) / (first.segmentNum - last.segmentNum).toFloat()
                        val newX = (x + 0.5f) * 0.5f
                        changesThresholdLocal = first.changesThreshold * (1 - newX) + last.changesThreshold * newX
                        logger.debug("doublesided optimization yields new changesThreshold = {}", changesThresholdLocal)
                    }
                }
            }


            // after optimization of the changes threshold, the minimum duration for a segment
            // (stability threshold) is optimized if the result is still not good enough
            val threshLow = stabilityThreshold * 1000
            var threshHigh = threshLow + threshLow / 2

            var tmpSegments: LinkedList<Segment>
            var smallestError = java.lang.Float.MAX_VALUE
            var bestI = threshLow
            segments = stepBest.segments

            // if the error is negative (which means there are already too few segments) or if the error
            // is smaller than the maximum error, the stability threshold will not be optimized
            if (stepBest.error <= maxError) {
                threshHigh = stabilityThreshold * 1000
            }
            run {
                var i = threshLow
                while (i <= threshHigh) {
                    tmpSegments = LinkedList()
                    filterSegmentation(segments, track, tmpSegments, i)
                    val newError = OptimizationStep.calculateErrorAbs(tmpSegments.size, prefNumberLocal)
                    if (newError < smallestError) {
                        smallestError = newError
                        bestI = i
                    }
                    i = i + 1000
                }
            }
            tmpSegments = LinkedList()
            mpeg7 = filterSegmentation(segments, track, tmpSegments, bestI)

            // for debugging: output of final segmentation after optimization
            logger.debug("result segments:")
            for (i in tmpSegments.indices) {
                val tmpLog2 = IntArray(7)
                tmpLog2[0] = tmpSegments[i].mediaTime.mediaTimePoint.hour
                tmpLog2[1] = tmpSegments[i].mediaTime.mediaTimePoint.minutes
                tmpLog2[2] = tmpSegments[i].mediaTime.mediaTimePoint.seconds
                tmpLog2[3] = tmpSegments[i].mediaTime.mediaDuration.hours
                tmpLog2[4] = tmpSegments[i].mediaTime.mediaDuration.minutes
                tmpLog2[5] = tmpSegments[i].mediaTime.mediaDuration.seconds
                val tmpLog1 = arrayOf<Any>(tmpLog2[0], tmpLog2[1], tmpLog2[2], tmpLog2[3], tmpLog2[4], tmpLog2[5], tmpLog2[6])
                tmpLog1[6] = tmpSegments[i].identifier
                logger.debug("s:{}:{}:{}, d:{}:{}:{}, {}", *tmpLog1)
            }

            logger.info("Optimized Segmentation yields (after {} iteration" + (if (cycleCount == 1) "" else "s") + ") {} segments",
                    cycleCount, tmpSegments.size)

            // if no reasonable segmentation could be found, instead return a uniform segmentation
            if (tmpSegments.size < absoluteMinLocal || tmpSegments.size > absoluteMaxLocal) {
                mpeg7 = uniformSegmentation(track, tmpSegments, prefNumberLocal)
                logger.info("Since no reasonable segmentation could be found, a uniform segmentation was created")
            }


            val mpeg7Catalog = MediaPackageElementBuilderFactory
                    .newInstance().newElementBuilder()
                    .newElement(Catalog.TYPE, MediaPackageElements.SEGMENTS) as Catalog
            val uri: URI
            try {
                uri = workspace!!.putInCollection(COLLECTION_ID, job.id.toString() + ".xml", mpeg7CatalogService!!.serialize(mpeg7))
            } catch (e: IOException) {
                throw VideoSegmenterException(
                        "Unable to put the mpeg7 catalog into the workspace", e)
            }

            mpeg7Catalog.setURI(uri)

            logger.info("Finished video segmentation of {}", mediaUrl)
            return mpeg7Catalog
        } catch (e: Exception) {
            logger.warn("Error segmenting $track", e)
            if (e is VideoSegmenterException) {
                throw e
            } else {
                throw VideoSegmenterException(e)
            }
        }

    }

    /**
     * Does the actual segmentation with an FFmpeg call, adds the segments to the given videoContent of a catalog and
     * returns a list with the resulting segments
     *
     * @param track the element to analyze
     * @param videoContent the videoContent of the Mpeg7Catalog that the segments should be added to
     * @param mediaFile the file of the track to analyze
     * @param changesThreshold the changesThreshold that is used as option for the FFmpeg call
     * @return a list of the resulting segments
     * @throws IOException
     * @throws VideoSegmenterException
     */
    @Throws(IOException::class, VideoSegmenterException::class)
    protected fun runSegmentationFFmpeg(track: Track, videoContent: Video, mediaFile: File,
                                        changesThreshold: Float): LinkedList<Segment> {

        val command = arrayOf(binary, "-nostats", "-i", mediaFile.absolutePath, "-filter:v", "select=gt(scene\\,$changesThreshold),showinfo", "-f", "null", "-")

        logger.info("Detecting video segments using command: {}", *command)

        val pbuilder = ProcessBuilder(*command)
        val segmentsStrings = LinkedList<String>()
        val process = pbuilder.start()
        val reader = BufferedReader(
                InputStreamReader(process.errorStream))
        try {
            val lr = LineReader(reader)
            var line: String? = lr.readLine()
            while (null != line) {
                if (line.startsWith("[Parsed_showinfo")) {
                    segmentsStrings.add(line)
                }
                line = lr.readLine()
            }
        } catch (e: IOException) {
            logger.error("Error executing ffmpeg: {}", e.message)
        } finally {
            reader.close()
        }

        // [Parsed_showinfo_1 @ 0x157fb40] n:0 pts:12 pts_time:12 pos:227495
        // fmt:rgb24 sar:0/1 s:320x240 i:P iskey:1 type:I checksum:8DF39EA9
        // plane_checksum:[8DF39EA9]

        var segmentcount = 1
        val segments = LinkedList<Segment>()

        if (segmentsStrings.size == 0) {
            val s = videoContent.temporalDecomposition
                    .createSegment("segment-$segmentcount")
            s.mediaTime = MediaRelTimeImpl(0, track.duration!!)
            segments.add(s)
        } else {
            var starttime: Long = 0
            var endtime: Long = 0
            val pattern = Pattern.compile("pts_time\\:\\d+(\\.\\d+)?")
            for (seginfo in segmentsStrings) {
                val matcher = pattern.matcher(seginfo)
                var time = ""
                while (matcher.find()) {
                    time = matcher.group().substring(9)
                }
                if ("" == time) {
                    // continue if the showinfo does not contain any time information. This may happen since the FFmpeg showinfo
                    // filter is used for multiple purposes.
                    continue
                }
                try {
                    endtime = Math.round(java.lang.Float.parseFloat(time) * 1000).toLong()
                } catch (e: NumberFormatException) {
                    logger.error("Unable to parse FFmpeg output, likely FFmpeg version mismatch!", e)
                    throw VideoSegmenterException(e)
                }

                val segmentLength = endtime - starttime
                if (1000 * stabilityThresholdPrefilter < segmentLength) {
                    val segment = videoContent.temporalDecomposition
                            .createSegment("segment-$segmentcount")
                    segment.mediaTime = MediaRelTimeImpl(starttime,
                            endtime - starttime)
                    logger.debug("Created segment {} at start time {} with duration {}", segmentcount, starttime, endtime)
                    segments.add(segment)
                    segmentcount++
                    starttime = endtime
                }
            }
            // Add last segment
            val s = videoContent.temporalDecomposition
                    .createSegment("segment-$segmentcount")
            s.mediaTime = MediaRelTimeImpl(starttime, track.duration!! - starttime)
            logger.debug("Created segment {} at start time {} with duration {}", segmentcount, starttime,
                    track.duration!! - endtime)
            segments.add(s)
        }

        logger.info("Segmentation of {} yields {} segments",
                mediaFile.toURI().toURL(), segments.size)

        return segments
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.process
     */
    @Throws(Exception::class)
    override fun process(job: Job): String {
        var op: Operation? = null
        val operation = job.operation
        val arguments = job.arguments
        try {
            op = Operation.valueOf(operation)
            when (op) {
                VideoSegmenterServiceImpl.Operation.Segment -> {
                    val track = MediaPackageElementParser
                            .getFromXml(arguments[0]) as Track
                    val catalog = segment(job, track)
                    return MediaPackageElementParser.getAsXml(catalog)
                }
                else -> throw IllegalStateException(
                        "Don't know how to handle operation '" + operation
                                + "'")
            }
        } catch (e: IllegalArgumentException) {
            throw ServiceRegistryException(
                    "This service can't handle operations of type '$op'",
                    e)
        } catch (e: IndexOutOfBoundsException) {
            throw ServiceRegistryException(
                    "This argument list for operation '" + op
                            + "' does not meet expectations", e)
        } catch (e: Exception) {
            throw ServiceRegistryException("Error handling operation '"
                    + op + "'", e)
        }

    }

    /**
     * Merges small subsequent segments (with high difference) into a bigger one
     *
     * @param segments list of segments to be filtered
     * @param track the track that is segmented
     * @param segmentsNew will be set to list of new segments (pass null if not required)
     * @return Mpeg7Catalog that can later be saved in a Catalog as endresult
     */
    protected fun filterSegmentation(
            segments: LinkedList<Segment>, track: Track, segmentsNew: LinkedList<Segment>): Mpeg7Catalog {
        val mergeThresh = stabilityThreshold * 1000
        return filterSegmentation(segments, track, segmentsNew, mergeThresh)
    }


    /**
     * Merges small subsequent segments (with high difference) into a bigger one
     *
     * @param segments list of segments to be filtered
     * @param track the track that is segmented
     * @param segmentsNew will be set to list of new segments (pass null if not required)
     * @param mergeThresh minimum duration for a segment in milliseconds
     * @return Mpeg7Catalog that can later be saved in a Catalog as endresult
     */
    fun filterSegmentation(
            segments: LinkedList<Segment>?, track: Track, segmentsNew: LinkedList<Segment>?, mergeThresh: Int): Mpeg7Catalog {
        var segmentsNew = segmentsNew
        if (segmentsNew == null) {
            segmentsNew = LinkedList()
        }
        var merging = false
        val contentTime = MediaRelTimeImpl(0, track.duration!!)
        val contentLocator = MediaLocatorImpl(track.getURI())
        val mpeg7 = mpeg7CatalogService!!.newInstance()
        val videoContent = mpeg7.addVideoContent("videosegment", contentTime, contentLocator)

        var segmentcount = 1

        var currentSegStart: MediaTimePoint = MediaTimePointImpl()

        for (o in segments!!) {

            // if the current segment is shorter than merge treshold start merging
            if (o.mediaTime.mediaDuration.durationInMilliseconds <= mergeThresh) {
                // start merging and save beginning of new segment that will be generated
                if (!merging) {
                    currentSegStart = o.mediaTime.mediaTimePoint
                    merging = true
                }

                // current segment is longer than merge threshold
            } else {
                val currentSegDuration = o.mediaTime.mediaDuration.durationInMilliseconds
                val currentSegEnd = o.mediaTime.mediaTimePoint.timeInMilliseconds + currentSegDuration

                if (merging) {
                    val newDuration = o.mediaTime.mediaTimePoint.timeInMilliseconds - currentSegStart.timeInMilliseconds

                    // if new segment would be long enough
                    // save new segment that merges all previously skipped short segments
                    if (newDuration >= mergeThresh) {
                        val s = videoContent.temporalDecomposition
                                .createSegment("segment-" + segmentcount++)
                        s.mediaTime = MediaRelTimeImpl(currentSegStart.timeInMilliseconds, newDuration)
                        segmentsNew.add(s)

                        // copy the following long segment to new list
                        val s2 = videoContent.temporalDecomposition
                                .createSegment("segment-" + segmentcount++)
                        s2.mediaTime = o.mediaTime
                        segmentsNew.add(s2)

                        // if too short split new segment in middle and merge halves to
                        // previous and following segments
                    } else {
                        val followingStartOld = o.mediaTime.mediaTimePoint.timeInMilliseconds
                        val newSplit = (currentSegStart.timeInMilliseconds + followingStartOld) / 2
                        val followingEnd = followingStartOld + o.mediaTime.mediaDuration.durationInMilliseconds
                        val followingDuration = followingEnd - newSplit

                        // if at beginning, don't split, just merge to first large segment
                        if (segmentsNew.isEmpty()) {
                            val s = videoContent.temporalDecomposition
                                    .createSegment("segment-" + segmentcount++)
                            s.mediaTime = MediaRelTimeImpl(0, followingEnd)
                            segmentsNew.add(s)
                        } else {

                            val previousStart = segmentsNew.last.mediaTime.mediaTimePoint.timeInMilliseconds

                            // adjust end time of previous segment to split time
                            segmentsNew.last.mediaTime = MediaRelTimeImpl(previousStart, newSplit - previousStart)

                            // create new segment starting at split time
                            val s = videoContent.temporalDecomposition
                                    .createSegment("segment-" + segmentcount++)
                            s.mediaTime = MediaRelTimeImpl(newSplit, followingDuration)
                            segmentsNew.add(s)
                        }
                    }
                    merging = false

                    // copy segments that are long enough to new list (with corrected number)
                } else {
                    val s = videoContent.temporalDecomposition
                            .createSegment("segment-" + segmentcount++)
                    s.mediaTime = o.mediaTime
                    segmentsNew.add(s)
                }
            }
        }

        // if there is an unfinished merging process after going through all segments
        if (merging && !segmentsNew.isEmpty()) {

            var newDuration = track.duration!! - currentSegStart.timeInMilliseconds
            // if merged segment is long enough, create new segment
            if (newDuration >= mergeThresh) {

                val s = videoContent.temporalDecomposition
                        .createSegment("segment-$segmentcount")
                s.mediaTime = MediaRelTimeImpl(currentSegStart.timeInMilliseconds, newDuration)
                segmentsNew.add(s)

                // if not long enough, merge with previous segment
            } else {
                newDuration = track.duration!! - segmentsNew.last.mediaTime.mediaTimePoint
                        .timeInMilliseconds
                segmentsNew.last.mediaTime = MediaRelTimeImpl(segmentsNew.last.mediaTime
                        .mediaTimePoint.timeInMilliseconds, newDuration)

            }
        }

        // if there is no segment in the list (to merge with), create new
        // segment spanning the whole video
        if (segmentsNew.isEmpty()) {
            val s = videoContent.temporalDecomposition
                    .createSegment("segment-$segmentcount")
            s.mediaTime = MediaRelTimeImpl(0, track.duration!!)
            segmentsNew.add(s)
        }

        return mpeg7
    }

    /**
     * Creates a uniform segmentation for a given track, with prefNumber as the number of segments
     * which will all have the same length
     *
     * @param track the track that is segmented
     * @param segmentsNew will be set to list of new segments (pass null if not required)
     * @param prefNumber number of generated segments
     * @return Mpeg7Catalog that can later be saved in a Catalog as endresult
     */
    protected fun uniformSegmentation(track: Track, segmentsNew: LinkedList<Segment>?, prefNumber: Int): Mpeg7Catalog {
        var segmentsNew = segmentsNew
        if (segmentsNew == null) {
            segmentsNew = LinkedList()
        }
        val contentTime = MediaRelTimeImpl(0, track.duration!!)
        val contentLocator = MediaLocatorImpl(track.getURI())
        val mpeg7 = mpeg7CatalogService!!.newInstance()
        val videoContent = mpeg7.addVideoContent("videosegment", contentTime, contentLocator)

        val segmentDuration = track.duration!! / prefNumber
        var currentSegStart: Long = 0

        // create "prefNumber"-many segments that all have the same length
        for (i in 1 until prefNumber) {
            val s = videoContent.temporalDecomposition
                    .createSegment("segment-$i")
            s.mediaTime = MediaRelTimeImpl(currentSegStart, segmentDuration)
            segmentsNew.add(s)

            currentSegStart += segmentDuration
        }

        // add last segment separately to make sure the last segment ends exactly at the end of the track
        val s = videoContent.temporalDecomposition
                .createSegment("segment-$prefNumber")
        s.mediaTime = MediaRelTimeImpl(currentSegStart, track.duration!! - currentSegStart)
        segmentsNew.add(s)

        return mpeg7
    }

    /**
     * Sets the workspace
     *
     * @param workspace
     * an instance of the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /**
     * Sets the mpeg7CatalogService
     *
     * @param mpeg7CatalogService
     * an instance of the mpeg7 catalog service
     */
    fun setMpeg7CatalogService(
            mpeg7CatalogService: Mpeg7CatalogService) {
        this.mpeg7CatalogService = mpeg7CatalogService
    }

    companion object {

        /** Resulting collection in the working file repository  */
        val COLLECTION_ID = "videosegments"

        val FFMPEG_BINARY_CONFIG = "org.opencastproject.composer.ffmpeg.path"
        val FFMPEG_BINARY_DEFAULT = "ffmpeg"

        /** Name of the constant used to retrieve the stability threshold  */
        val OPT_STABILITY_THRESHOLD = "stabilitythreshold"

        /** The number of seconds that need to resemble until a scene is considered "stable"  */
        val DEFAULT_STABILITY_THRESHOLD = 60

        /** Name of the constant used to retrieve the changes threshold  */
        val OPT_CHANGES_THRESHOLD = "changesthreshold"

        /** Default value for the number of pixels that may change between two frames without considering them different  */
        val DEFAULT_CHANGES_THRESHOLD = 0.025f // 2.5% change

        /** Name of the constant used to retrieve the preferred number of segments  */
        val OPT_PREF_NUMBER = "prefNumber"

        /** Default value for the preferred number of segments  */
        val DEFAULT_PREF_NUMBER = 30

        /** Name of the constant used to retrieve the maximum number of cycles  */
        val OPT_MAX_CYCLES = "maxCycles"

        /** Default value for the maximum number of cycles  */
        val DEFAULT_MAX_CYCLES = 3

        /** Name of the constant used to retrieve the maximum tolerance for result  */
        val OPT_MAX_ERROR = "maxError"

        /** Default value for the maximum tolerance for result  */
        val DEFAULT_MAX_ERROR = 0.25f

        /** Name of the constant used to retrieve the absolute maximum number of segments  */
        val OPT_ABSOLUTE_MAX = "absoluteMax"

        /** Default value for the absolute maximum number of segments  */
        val DEFAULT_ABSOLUTE_MAX = 150

        /** Name of the constant used to retrieve the absolute minimum number of segments  */
        val OPT_ABSOLUTE_MIN = "absoluteMin"

        /** Default value for the absolute minimum number of segments  */
        val DEFAULT_ABSOLUTE_MIN = 3

        /** Name of the constant used to retrieve the option whether segments numbers depend on track duration  */
        val OPT_DURATION_DEPENDENT = "durationDependent"

        /** Default value for the option whether segments numbers depend on track duration  */
        val DEFAULT_DURATION_DEPENDENT = false

        /** The load introduced on the system by a segmentation job  */
        val DEFAULT_SEGMENTER_JOB_LOAD = 0.3f

        /** The key to look for in the service configuration file to override the DEFAULT_CAPTION_JOB_LOAD  */
        val SEGMENTER_JOB_LOAD_KEY = "job.load.videosegmenter"

        /** The logging facility  */
        protected val logger = LoggerFactory
                .getLogger(VideoSegmenterServiceImpl::class.java)
    }

}
