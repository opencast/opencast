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


package org.opencastproject.composer.impl

import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.VideoClip
import org.opencastproject.mediapackage.identifier.IdBuilder
import org.opencastproject.mediapackage.identifier.IdBuilderFactory
import org.opencastproject.util.IoSupport
import org.opencastproject.util.data.Collections
import org.opencastproject.util.data.Tuple

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.codehaus.plexus.util.cli.CommandLineUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList
import java.util.Objects
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern

import javax.activation.MimetypesFileTypeMap

/**
 * Abstract base class for encoder engines.
 */
class EncoderEngine
/**
 * Creates a new abstract encoder engine with or without support for multiple job submission.
 */
internal constructor(binary: String) : AutoCloseable {
    /** the encoder binary  */
    private val binary = "ffmpeg"
    /** Set of processes to clean up  */
    private val processes = HashSet<Process>()

    private val outputPattern = Pattern.compile("Output .* to '(.*)':")

    init {
        this.binary = binary
    }

    /**
     * {@inheritDoc}
     *
     * @see EncoderEngine.encode
     */
    @Throws(EncoderException::class)
    internal fun encode(mediaSource: File, format: EncodingProfile, properties: Map<String, String>?): File {
        val output = process(Collections.map(Tuple.tuple("video", mediaSource)), format, properties)
        if (output.size != 1) {
            throw EncoderException(String.format("Encode expects one output file (%s found)", output.size))
        }
        return output[0]
    }

    /**
     * Extract several images from a video file.
     *
     * @param mediaSource
     * File to extract images from
     * @param format
     * Encoding profile to use for extraction
     * @param properties
     * @param times
     * Times at which to extract the images
     * @return  List of image files
     * @throws EncoderException Something went wrong during image extraction
     */
    @Throws(EncoderException::class)
    internal fun extract(mediaSource: File, format: EncodingProfile, properties: Map<String, String>?, vararg times: Double): List<File> {

        val extractedImages = LinkedList<File>()
        try {
            // Extract one image if no times are specified
            if (times.size == 0) {
                extractedImages.add(encode(mediaSource, format, properties))
            }
            for (time in times) {
                val params = HashMap<String, String>()
                if (properties != null) {
                    params.putAll(properties)
                }

                val ffmpegFormat = DecimalFormatSymbols()
                ffmpegFormat.decimalSeparator = '.'
                val df = DecimalFormat("0.00000", ffmpegFormat)
                params["time"] = df.format(time)

                extractedImages.add(encode(mediaSource, format, params))
            }
        } catch (e: Exception) {
            cleanup(extractedImages)
            if (e is EncoderException) {
                throw e
            } else {
                throw EncoderException("Image extraction failed", e)
            }
        }

        return extractedImages
    }

    /**
     * Executes the command line encoder with the given set of files and properties and using the provided encoding
     * profile.
     *
     * @param source
     * the source files for encoding
     * @param profile
     * the profile identifier
     * @param properties
     * the encoding properties to be interpreted by the actual encoder implementation
     * @return the processed file
     * @throws EncoderException
     * if processing fails
     */
    @Throws(EncoderException::class)
    internal fun process(source: Map<String, File>, profile: EncodingProfile, properties: Map<String, String>?): List<File> {
        // Fist, update the parameters
        val params = HashMap<String, String>()
        if (properties != null)
            params.putAll(properties)
        // build command
        if (source.isEmpty()) {
            throw IllegalArgumentException("At least one track must be specified.")
        }
        // Set encoding parameters
        for ((key, value) in source) {
            val input = FilenameUtils.normalize(value.absolutePath)
            val pre = "in.$key"
            params["$pre.path"] = input
            params["$pre.name"] = FilenameUtils.getBaseName(input)
            params["$pre.suffix"] = FilenameUtils.getExtension(input)
            params["$pre.filename"] = FilenameUtils.getName(input)
            params["$pre.mimetype"] = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(input)
        }
        val parentFile = (source as java.util.Map<String, File>).getOrDefault("video", source["audio"])

        val outDir = parentFile.absoluteFile.parent
        val outFileName = (FilenameUtils.getBaseName(parentFile.name)
                + "_" + UUID.randomUUID().toString())
        params["out.dir"] = outDir
        params["out.name"] = outFileName
        if (profile.suffix != null) {
            val outSuffix = processParameters(profile.suffix, params)
            params["out.suffix"] = outSuffix
        }

        for (tag in profile.tags) {
            val suffix = processParameters(profile.getSuffix(tag), params)
            params["out.suffix.$tag"] = suffix
        }

        // create encoder process.
        val command = buildCommand(profile, params)
        logger.info("Executing encoding command: {}", command)

        val outFiles = ArrayList<File>()
        var `in`: BufferedReader? = null
        var encoderProcess: Process? = null
        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(REDIRECT_ERROR_STREAM)
            encoderProcess = processBuilder.start()
            processes.add(encoderProcess)

            // tell encoder listeners about output
            `in` = BufferedReader(InputStreamReader(encoderProcess!!.inputStream))
            var line: String
            while ((line = `in`.readLine()) != null) {
                handleEncoderOutput(outFiles, line)
            }

            // wait until the task is finished
            val exitCode = encoderProcess.waitFor()
            if (exitCode != 0) {
                throw EncoderException("Encoder exited abnormally with status $exitCode")
            }

            logger.info("Tracks {} successfully encoded using profile '{}'", source, profile.identifier)
            return outFiles
        } catch (e: Exception) {
            logger.warn("Error while encoding {}  using profile '{}'",
                    source, profile.identifier, e)

            // Ensure temporary data are removed
            for (outFile in outFiles) {
                if (FileUtils.deleteQuietly(outFile)) {
                    logger.debug("Removed output file of failed encoding process: {}", outFile)
                }
            }
            throw EncoderException(e)
        } finally {
            IoSupport.closeQuietly(`in`)
            IoSupport.closeQuietly(encoderProcess)
        }
    }

    /*
   * Runs the raw command string thru the encoder. The string commandopts is ffmpeg specific, it just needs the binary.
   * The calling function is responsible in doing all the appropriate substitutions using the encoding profiles,
   * creating the directory for storage, etc. Encoding profiles and input names are included here for logging and
   * returns
   *
   * @param commandopts - tokenized ffmpeg command
   *
   * @param inputs - input files in the command, used for reporting
   *
   * @param profiles - encoding profiles, used for reporting
   *
   * @return encoded - media as a result of running the command
   *
   * @throws EncoderException if it fails
   */

    @Throws(EncoderException::class)
    protected fun process(commandopts: List<String>): List<File> {
        logger.trace("Process raw command -  {}", commandopts)
        // create encoder process. using working dir of the
        // current java process
        var encoderProcess: Process? = null
        var `in`: BufferedReader? = null
        val outFiles = ArrayList<File>()
        try {
            val command = ArrayList<String>()
            command.add(binary)
            command.addAll(commandopts)
            logger.info("Executing encoding command: {}", StringUtils.join(command, " "))

            val pbuilder = ProcessBuilder(command)
            pbuilder.redirectErrorStream(REDIRECT_ERROR_STREAM)
            encoderProcess = pbuilder.start()
            // tell encoder listeners about output
            `in` = BufferedReader(InputStreamReader(encoderProcess!!.inputStream))
            var line: String
            while ((line = `in`.readLine()) != null) {
                handleEncoderOutput(outFiles, line) // get names of output files
            }
            // wait until the task is finished
            encoderProcess.waitFor()
            val exitCode = encoderProcess.exitValue()
            if (exitCode != 0) {
                throw EncoderException("Encoder exited abnormally with status $exitCode")
            }
            logger.info("Video track successfully encoded '{}'",
                    *arrayOf<Any>(StringUtils.join(commandopts, " ")))
            return outFiles // return output as a list of files
        } catch (e: Exception) {
            logger.warn("Error while encoding video tracks using '{}': {}",
                    *arrayOf<Any>(StringUtils.join(commandopts, " "), e.message))
            // Ensure temporary data are removed
            for (outFile in outFiles) {
                if (FileUtils.deleteQuietly(outFile)) {
                    logger.debug("Removed output file of failed encoding process: {}", outFile)
                }
            }
            throw EncoderException(e)
        } finally {
            IoSupport.closeQuietly(`in`)
            IoSupport.closeQuietly(encoderProcess)
        }
    }

    /**
     * Deletes all valid files found in a list
     *
     * @param outputFiles
     * list containing files
     */
    private fun cleanup(outputFiles: List<File>) {
        for (file in outputFiles) {
            if (file != null && file.isFile) {
                val path = file.absolutePath
                if (file.delete()) {
                    logger.info("Deleted file {}", path)
                } else {
                    logger.warn("Could not delete file {}", path)
                }
            }
        }
    }

    /**
     * Creates the command that is sent to the commandline encoder.
     *
     * @return the commandline
     * @throws EncoderException
     * in case of any error
     */
    @Throws(EncoderException::class)
    private fun buildCommand(profile: EncodingProfile, argumentReplacements: Map<String, String>): List<String> {
        val command = ArrayList<String>()
        command.add(binary)
        command.add("-nostats")

        var commandline = profile.getExtension(CMD_SUFFIX)

        // Handle command line extensions before parsing:
        // Example:
        //   ffmpeg.command = #{concatCmd} -c copy out.mp4
        //   ffmpeg.command.concatCmd = -i ...
        for (key in argumentReplacements.keys) {
            if (key.startsWith("$CMD_SUFFIX.")) {
                val shortKey = key.substring(CMD_SUFFIX.length + 1)
                commandline = commandline.replace("#{$shortKey}", argumentReplacements[key])
            }
        }

        val arguments: Array<String>
        try {
            arguments = CommandLineUtils.translateCommandline(commandline)
        } catch (e: Exception) {
            throw EncoderException("Could not parse encoding profile command line", e)
        }

        for (arg in arguments) {
            val result = processParameters(arg, argumentReplacements)
            if (StringUtils.isNotBlank(result)) {
                command.add(result)
            }
        }
        return command
    }

    /**
     * {@inheritDoc}
     *
     * @see EncoderEngine.trim
     */
    @Throws(EncoderException::class)
    internal fun trim(mediaSource: File, format: EncodingProfile, start: Long, duration: Long, properties: MutableMap<String, String>?): File {
        var properties = properties
        if (properties == null)
            properties = HashMap()
        val startD = start.toDouble() / 1000
        val durationD = duration.toDouble() / 1000
        val ffmpegFormat = DecimalFormatSymbols()
        ffmpegFormat.decimalSeparator = '.'
        val df = DecimalFormat("00.00000", ffmpegFormat)
        properties[PROP_TRIMMING_START_TIME] = df.format(startD)
        properties[PROP_TRIMMING_DURATION] = df.format(durationD)
        return encode(mediaSource, format, properties)
    }

    /**
     * Processes the command options by replacing the templates with their actual values.
     *
     * @return the commandline
     */
    private fun processParameters(cmd: String, args: Map<String, String>): String {
        var cmd = cmd
        for ((key, value) in args) {
            cmd = cmd.replace("#{$key}", value)
        }

        // Also replace spaces
        cmd = cmd.replace("#{space}", " ")

        /* Remove unused commandline parts */
        return cmd.replace("#\\{.*?\\}".toRegex(), "")
    }

    override fun close() {
        for (process in processes) {
            if (process.isAlive) {
                logger.debug("Destroying encoding process {}", process)
                process.destroy()
            }
        }
    }

    /**
     * Handles the encoder output by analyzing it first and then firing it off to the registered listeners.
     *
     * @param message
     * the message returned by the encoder
     */
    private fun handleEncoderOutput(output: MutableList<File>, message: String) {
        var message = message
        message = message.trim { it <= ' ' }
        if ("" == message)
            return

        // Others go to trace logging
        if (StringUtils.startsWithAny(message.toLowerCase(),
                        "ffmpeg version", "configuration", "lib", "size=", "frame=", "built with")) {
            logger.trace(message)

            // Handle output files
        } else if (StringUtils.startsWith(message, "Output #")) {
            logger.debug(message)
            val matcher = outputPattern.matcher(message)
            if (matcher.find()) {
                val outputPath = matcher.group(1)
                if (!StringUtils.equals("NUL", outputPath)
                        && !StringUtils.equals("/dev/null", outputPath)
                        && !StringUtils.startsWith("pipe:", outputPath)) {
                    val outputFile = File(outputPath)
                    logger.info("Identified output file {}", outputFile)
                    output.add(outputFile)
                }
            }

            // Some to debug
        } else if (StringUtils.startsWithAny(message.toLowerCase(),
                        "artist", "compatible_brands", "copyright", "creation_time", "description", "duration",
                        "encoder", "handler_name", "input #", "last message repeated", "major_brand", "metadata", "minor_version",
                        "output #", "program", "side data:", "stream #", "stream mapping", "title", "video:", "[libx264 @ ")) {
            logger.debug(message)

            // And the rest is likely to deserve at least info
        } else {
            logger.info(message)
        }
    }

    /**
     * Rewrite multiple profiles to ffmpeg complex filter filtergraph chains - inputs are passed in as options, eq: [0aa]
     * and [0vv] Any filters in the encoding profiles are moved into a clause in the complex filter chain for each output
     */
    protected inner class OutputAggregate
    /**
     * Translate the profiles to work with complex filter clauses in ffmpeg, it splits one output into multiple, one for
     * each encoding profile
     *
     * @param profiles
     * - list of encoding profiles
     * @param params
     * - values for substitution
     * @param vInputPad
     * - name of video pad as input, eg: [0v] null if no video
     * @param aInputPad
     * - name of audio pad as input, eg [0a], null if no audio
     * @throws EncoderException
     * - if it fails
     */
    @Throws(EncoderException::class)
    constructor(private val pf: List<EncodingProfile>, params: MutableMap<String, String>, vInputPad: String?,
                aInputPad: String?) {
        private val outputs = ArrayList<String>()
        private val outputFiles = ArrayList<String>()
        private val vpads: ArrayList<String> // output pads for each segment
        private val apads: ArrayList<String>
        private val vfilter: ArrayList<String> // filters for each output format
        private val afilter: ArrayList<String>
        private val vInputPad = ""
        private val aInputPad = ""
        /**
         *
         * @return filter split clause for ffmpeg
         */
        var vsplit: String? = ""
            private set
        var asplit: String? = ""
            private set

        val outFiles: List<String>
            get() = outputFiles

        /**
         *
         * @return output pads - the "-map xyz" clauses
         */
        val output: List<String>
            get() = outputs

        val videoFilter: String?
            get() = if (vfilter.isEmpty()) null else StringUtils.join(vfilter, ";")

        val audioFilter: String?
            get() = if (afilter.isEmpty()) null else StringUtils.join(afilter, ";")

        /*
     * set the audio filter if there are any in the profiles or identity
     */
        private fun setAudioFilters() {
            if (pf.size == 1) {
                if (afilter[0] != null)
                    afilter[0] = aInputPad + afilter[0] + apads[0] // Use audio filter on input directly
            } else
                for (i in pf.indices) {
                    if (afilter[i] != null) {
                        afilter[i] = "[oa0" + i + "]" + afilter[i] + apads[i] // Use audio filter on apad
                        asplit += "[oa0$i]"
                    } else {
                        asplit += apads[i] // straight to output
                    }
                }
            afilter.removeAll(Arrays.asList((null as String?)!!))
        }

        /*
     * set the video filter if there are any in the profiles
     */
        private fun setVideoFilters() {
            if (pf.size == 1) {
                if (vfilter[0] != null)
                    vfilter[0] = vInputPad + vfilter[0] + vpads[0] // send to filter first
            } else
                for (i in pf.indices) {
                    if (vfilter[i] != null) {
                        vfilter[i] = "[ov0" + i + "]" + vfilter[i] + vpads[i] // send to filter first
                        vsplit += "[ov0$i]"
                    } else {
                        vsplit += vpads[i]// straight to output
                    }
                }

            vfilter.removeAll(Arrays.asList((null as String?)!!))
        }

        /**
         * If this is a raw mapping not used with complex filter, strip the square brackets if there are any
         *
         * @param pad
         * - such as 0:a, [0:v], [1:1],[0:12],[main],[overlay]
         * @return adjusted pad
         */
        fun adjustForNoComplexFilter(pad: String?): String {
            val outpad = Pattern.compile("\\[(\\d+:[av\\d{1,2}])\\]")
            try {
                val matcher = outpad.matcher(pad!!) // throws exception if pad is null
                if (matcher.matches()) {
                    return matcher.group(1)
                }
            } catch (e: Exception) {
            }

            return pad
        }

        /**
         * Replace all the templates with real values for each profile
         *
         * @param cmd
         * from profile
         * @param params
         * from input
         * @return command
         */
        protected fun processParameters(cmd: String, params: Map<String, String>): String {
            var r = cmd
            for ((key, value) in params) {
                r = r.replace("#{$key}", value)
            }
            return r
        }

        init {
            if (vInputPad == null && aInputPad == null)
                throw EncoderException("At least one of video or audio input must be specified")
            val idbuilder = IdBuilderFactory.newInstance().newIdBuilder()
            val size = pf.size
            // Init
            vfilter = ArrayList(java.util.Collections.nCopies<String>(size, null))
            afilter = ArrayList(java.util.Collections.nCopies<String>(size, null))
            // name of output pads to map to files
            apads = ArrayList(java.util.Collections.nCopies<String>(size, null))
            vpads = ArrayList(java.util.Collections.nCopies<String>(size, null))

            vsplit = if (size > 1) vInputPad + "split=" + size else null // number of splits
            asplit = if (size > 1) aInputPad + "asplit=" + size else null
            this.vInputPad = vInputPad
            this.aInputPad = aInputPad

            var indx = 0 // profiles
            for (profile in pf) {
                var cmd: String? = ""
                val outSuffix: String
                // generate random name as we only have one base name
                val outFileName = params["out.name.base"] + "_" + idbuilder.createNew().toString()
                params["out.name"] = outFileName // Output file name for this profile
                try {
                    outSuffix = processParameters(profile.suffix, params)
                    params["out.suffix"] = outSuffix // Add profile suffix
                } catch (e: Exception) {
                    throw EncoderException("Missing Encoding Profiles")
                }

                // substitute the output file name
                var ffmpgCmd: String? = profile.getExtension(CMD_SUFFIX)
                        ?: throw EncoderException("Missing Encoding Profile " + profile.identifier + " ffmpeg command") // Get ffmpeg command from profile
                for ((key, value) in params) { // replace output filenames
                    ffmpgCmd = ffmpgCmd!!.replace("#{$key}", value)
                }
                ffmpgCmd = ffmpgCmd!!.replace("#{space}", " ")
                val arguments: Array<String>
                try {
                    arguments = CommandLineUtils.translateCommandline(ffmpgCmd)
                } catch (e: Exception) {
                    throw EncoderException("Could not parse encoding profile command line", e)
                }

                val cmdToken = Arrays.asList(*arguments)
                // Find and remove input and filters from ffmpeg command from the profile
                var i = 0
                while (i < cmdToken.size) {
                    val opt = cmdToken[i]
                    if (opt.startsWith("-vf") || opt.startsWith("-filter:v")) { // video filters
                        vfilter[indx] = cmdToken[i + 1].replace("\"", "") // store without quotes
                        i++
                    } else if (opt.startsWith("-filter_complex") || opt.startsWith("-lavfi")) { // safer to quit now than to
                        // baffle users with strange errors later
                        i++
                        logger.error("Command does not support complex filters - only simple -af or -vf filters are supported")
                        throw EncoderException(
                                "Cannot parse complex filters in" + profile.identifier + " for this operation")
                    } else if (opt.startsWith("-af") || opt.startsWith("-filter:a")) { // audio filter
                        afilter[indx] = cmdToken[i + 1].replace("\"", "") // store without quotes
                        i++
                    } else if ("-i" == opt) {
                        i++ // inputs are now mapped, remove from command
                    } else if (opt.startsWith("-c:") || opt.startsWith("-codec:") || opt.contains("-vcodec")
                            || opt.contains("-acodec")) { // cannot copy codec in complex filter
                        val str = cmdToken[i + 1]
                        if (str.contains("copy"))
                        // c
                            i++
                        else
                            cmd = "$cmd $opt"
                    } else { // keep the rest
                        cmd = "$cmd $opt"
                    }
                    i++
                }
                /* Remove unused commandline parts */
                cmd = cmd!!.replace("#\\{.*?\\}".toRegex(), "")
                // Find the output map based on splits and filters
                if (size == 1) { // no split
                    if (afilter[indx] == null)
                        apads[indx] = adjustForNoComplexFilter(aInputPad)
                    else
                        apads[indx] = "[oa$indx]"
                    if (vfilter[indx] == null)
                        vpads[indx] = adjustForNoComplexFilter(vInputPad) // No split, no filter - straight from input
                    else
                        vpads[indx] = "[ov$indx]"

                } else { // split
                    vpads[indx] = "[ov$indx]" // name the output pads from split -> input to final format
                    apads[indx] = "[oa$indx]" // name the output audio pads
                }
                cmd = StringUtils.trimToNull(cmd) // remove all leading/trailing white spaces
                if (cmd != null) {
                    outputFiles.add(cmdToken[cmdToken.size - 1])
                    if (vInputPad != null) {
                        outputs.add("-map " + vpads[indx])
                    }
                    if (aInputPad != null) {
                        outputs.add("-map " + apads[indx]) // map video and audio input
                    }
                    outputs.add(cmd) // profiles appended in order, they are numbered 0,1,2,3...
                    indx++ // indx for this profile
                }
            }
            setVideoFilters()
            setAudioFilters()
        }
    }

    /**
     * Create the trim part of the complex filter and return the clauses for the complex filter. The transition is fade to
     * black then fade from black. The outputs are mapped to [ov] and [oa]
     *
     * @param clips
     * - video segments as indices into the media files by time
     * @param transitionDuration
     * - length of transition in MS between each segment
     * @param hasVideo
     * - has video, from inspection
     * @param hasAudio
     * - has audio
     * @return complex filter clauses to do editing for ffmpeg
     * @throws Exception
     * - if it fails
     */
    @Throws(Exception::class)
    private fun makeEdits(clips: List<VideoClip>?, transitionDuration: Int, hasVideo: Boolean?,
                          hasAudio: Boolean?): MutableList<String> {
        val vfade = (transitionDuration / 1000).toDouble() // video and audio have the same transition duration
        val ffmpegFormat = DecimalFormatSymbols()
        ffmpegFormat.decimalSeparator = '.'
        val f = DecimalFormat("0.00", ffmpegFormat)
        val vpads = ArrayList<String>()
        val apads = ArrayList<String>()
        val clauses = ArrayList<String>() // The clauses are ordered
        var n = 0
        if (clips != null)
            n = clips.size
        var outmap = "o"
        if (n > 1) { // Create the input pads if we have multiple segments
            for (i in 0 until n) {
                vpads.add("[v$i]") // post filter
                apads.add("[a$i]")
            }
            outmap = ""
            // Create the trims
            for (i in 0 until n) { // Each clip
                // get clip and add fades to each clip
                val vclip = clips!![i]
                val fileindx = vclip.src // get source file by index
                val inpt = vclip.start // get in points
                val duration = vclip.duration
                val vend = Math.max(duration - vfade, 0.0)
                val aend = Math.max(duration - vfade, 0.0)
                if (hasVideo!!) {
                    val vvclip: String
                    vvclip = ("[" + fileindx + ":v]trim=" + f.format(inpt) + ":duration=" + f.format(duration)
                            + ",setpts=PTS-STARTPTS"
                            + (if (vfade > 0)
                        ",fade=t=in:st=0:d=" + vfade + ",fade=t=out:st=" + f.format(vend) + ":d=" + vfade
                    else
                        "")
                            + "[" + outmap + "v" + i + "]")
                    clauses.add(vvclip)
                }
                if (hasAudio!!) {
                    val aclip: String
                    aclip = ("[" + fileindx + ":a]atrim=" + f.format(inpt) + ":duration=" + f.format(duration)
                            + ",asetpts=PTS-STARTPTS"
                            + (if (vfade > 0)
                        ",afade=t=in:st=0:d=" + vfade + ",afade=t=out:st=" + f.format(aend) + ":d=" + +vfade
                    else
                        "")
                            + "[" + outmap + "a" + i + "]")
                    clauses.add(aclip)
                }
            }
            // use unsafe because different files may have different SAR/framerate
            if (hasVideo!!)
                clauses.add(StringUtils.join(vpads, "") + "concat=n=" + n + ":unsafe=1[ov]") // concat video clips
            if (hasAudio!!)
                clauses.add(StringUtils.join(apads, "") + "concat=n=" + n + ":v=0:a=1[oa]") // concat audio clips in stream 0,
        } else if (n == 1) { // single segment
            val vclip = clips!![0]
            val fileindx = vclip.src // get source file by index
            val inpt = vclip.start // get in points
            val duration = vclip.duration
            val vend = Math.max(duration - vfade, 0.0)
            val aend = Math.max(duration - vfade, 0.0)

            if (hasVideo!!) {
                val vvclip: String

                vvclip = ("[" + fileindx + ":v]trim=" + f.format(inpt) + ":duration=" + f.format(duration)
                        + ",setpts=PTS-STARTPTS,"
                        + (if (vfade > 0) "fade=t=in:st=0:d=" + vfade + ",fade=t=out:st=" + f.format(vend) + ":d=" + vfade else "")
                        + "[ov]")

                clauses.add(vvclip)
            }
            if (hasAudio!!) {
                val aclip: String
                aclip = ("[" + fileindx + ":a]atrim=" + f.format(inpt) + ":duration=" + f.format(duration)
                        + ",asetpts=PTS-STARTPTS,"
                        + (if (vfade > 0) "afade=t=in:st=0:d=" + vfade + ",afade=t=out:st=" + f.format(aend) + ":d=" else "")
                        + vfade + "[oa]")

                clauses.add(aclip)
            }
        }
        return clauses // if no edits, there are no clauses
    }

    private fun getParamsFromFile(parentFile: File): MutableMap<String, String> {
        val params = HashMap<String, String>()
        val videoInput = FilenameUtils.normalize(parentFile.absolutePath)
        params["in.video.path"] = videoInput
        params["in.video.name"] = FilenameUtils.getBaseName(videoInput)
        params["in.name"] = FilenameUtils.getBaseName(videoInput) // One of the names
        params["in.video.suffix"] = FilenameUtils.getExtension(videoInput)
        params["in.video.filename"] = FilenameUtils.getName(videoInput)
        params["in.video.mimetype"] = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(videoInput)
        val outDir = parentFile.absoluteFile.parent // Use first file dir
        params["out.dir"] = outDir
        val outFileName = FilenameUtils.getBaseName(parentFile.name)
        params["out.name.base"] = outFileName // Base file name used
        params["out.name"] = outFileName // file name used - may be replaced
        return params
    }

    @Throws(EncoderException::class, IllegalArgumentException::class)
    @JvmOverloads
    fun multiTrimConcat(inputs: List<File>?, edits: List<Long>?, profiles: List<EncodingProfile>?,
                        transitionDuration: Int, hasVideo: Boolean = true, hasAudio: Boolean = true): List<File> {
        if (inputs == null || inputs.size < 1) {
            throw IllegalArgumentException("At least one track must be specified.")
        }
        if (edits == null && inputs.size > 1) {
            throw IllegalArgumentException("If there is no editing, only one track can be specified.")
        }
        var clips: MutableList<VideoClip>? = null
        if (edits != null) {
            clips = ArrayList(edits.size / 3)
            var adjust = 0
            // When the first clip starts at 0, and there is a fade, lip sync can be off,
            // this adjustment will mitigate the problem
            var i = 0
            while (i < edits.size) {
                if (edits[i + 1] < transitionDuration)
                // If taken from the beginning of video
                    adjust = transitionDuration / 2000 // add half the fade duration in seconds
                else
                    adjust = 0
                clips.add(VideoClip(edits[i].toInt(), edits[i + 1] as Double / 1000 + adjust,
                        edits[i + 2] as Double / 1000))
                i += 3
            }
            try {
                clips = sortSegments(clips, (transitionDuration / 1000).toDouble()) // remove bad edit points
            } catch (e: Exception) {
                logger.error("Illegal edits, cannot sort segment", e)
                throw EncoderException("Cannot understand the edit points", e)
            }

        }
        // Set encoding parameters
        var params: MutableMap<String, String>? = null
        if (inputs.size > 0) { // Shared parameters - the rest are profile specific
            params = getParamsFromFile(inputs[0])
        }
        if (profiles == null || profiles.size == 0) {
            logger.error("Missing encoding profiles")
            throw EncoderException("Missing encoding profile(s)")
        }
        try {
            val command = ArrayList<String>()
            val clauses = makeEdits(clips, transitionDuration, hasVideo, hasAudio) // map inputs into [ov]
            // and [oa]
            // Entry point for multiencode here, if edits is empty, then use raw channels instead of output from edits
            val videoOut = if (clips == null) "[0:v]" else "[ov]"
            val audioOut = if (clips == null) "[0:a]" else "[oa]"
            val outmaps = OutputAggregate(profiles, params, if (hasVideo) videoOut else null,
                    if (hasAudio) audioOut else null) // map outputs from ov and oa
            if (hasAudio) {
                clauses.add(outmaps.asplit)
                clauses.add(outmaps.audioFilter)
            }
            if (hasVideo) {
                clauses.add(outmaps.vsplit)
                clauses.add(outmaps.videoFilter)
            }
            clauses.removeIf(Predicate<String> { Objects.isNull(it) }) // remove all empty filters
            command.add("-y") // overwrite old files
            command.add("-nostats") // no progress report
            for (o in inputs) {
                command.add("-i") // Add inputfile in the order of entry
                command.add(o.canonicalPath)
            }
            command.add("-filter_complex")
            command.add(StringUtils.join(clauses, ";"))
            for (outpad in outmaps.output) {
                command.addAll(Arrays.asList(*outpad.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
            }
            return process(command) // Run the ffmpeg command
        } catch (e: Exception) {
            logger.error("MultiTrimConcat failed to run command {} ", e.message)
            throw EncoderException("Cannot encode the inputs", e)
        }

    }

    companion object {

        /** The ffmpeg commandline suffix  */
        internal val CMD_SUFFIX = "ffmpeg.command"
        /** The trimming start time property name  */
        internal val PROP_TRIMMING_START_TIME = "trim.start"
        /** The trimming duration property name  */
        internal val PROP_TRIMMING_DURATION = "trim.duration"
        /** If true STDERR and STDOUT of the spawned process will be mixed so that both can be read via STDIN  */
        private val REDIRECT_ERROR_STREAM = true

        /** the logging facility provided by log4j  */
        private val logger = LoggerFactory.getLogger(EncoderEngine::class.java.name)

        /**
         * Clean up the edit points, make sure the gap between consecutive segments are larger than the transition Otherwise
         * it can be very slow to run and output will be ugly because the fades will extend the clip
         *
         * @param edits
         * - clips to be stitched together
         * @param gap
         * = transitionDuration / 1000; default gap size - same as fade
         * @return a list of sanitized video clips
         */
        private fun sortSegments(edits: List<VideoClip>, gap: Double): MutableList<VideoClip> {
            val ll = LinkedList<VideoClip>()
            var it = edits.iterator()
            var clip: VideoClip
            var nextclip: VideoClip
            var lastSrc = -1
            while (it.hasNext()) { // Skip sort if there are multiple sources
                clip = it.next()
                if (lastSrc < 0) {
                    lastSrc = clip.src
                } else if (lastSrc != clip.src) {
                    return edits
                }
            }
            java.util.Collections.sort(edits) // Sort clips if all clips are from the same src
            val clips = ArrayList<VideoClip>()
            it = edits.iterator()
            while (it.hasNext()) { // Check for legal durations
                clip = it.next()
                if (clip.duration > gap) { // Keep segments at least as long as transition fade
                    ll.add(clip)
                }
            }
            clip = ll.pop() // initialize
            // Clean up segments so that the cut out is at least as long as the transition gap (default is fade out-fade in)
            while (!ll.isEmpty()) { // Check that 2 consecutive segments from same src are at least GAP secs apart
                if (ll.peek() != null) {
                    nextclip = ll.pop() // check next consecutive segment
                    if (nextclip.src == clip.src && nextclip.start - clip.end < gap) { // collapse two
                        // segments into one
                        clip.end = nextclip.end // by using inpt of seg 1 and outpoint of seg 2
                    } else {
                        clips.add(clip) // keep last segment
                        clip = nextclip // check next segment
                    }
                }
            }
            clips.add(clip) // add last segment
            return clips
        }
    }

}
/**
 * Concatenate segments of one or more input tracks specified by trim points into the track the edits are passed in as
 * double so that it is generic. The tracks are assumed to have the same resolution.
 *
 * @param inputs
 * - input tracks as a list of files
 * @param edits
 * - edits are a flat list of triplets, each triplet represent one clip: index (int) into input tracks, trim in point(long)
 * in milliseconds and trim out point (long) in milliseconds for each segment
 * @param profiles
 * - encoding profiles for the target
 * @param transitionDuration
 * in ms, transition time between each edited segment
 * @throws EncoderException
 * - if it fails
 */
