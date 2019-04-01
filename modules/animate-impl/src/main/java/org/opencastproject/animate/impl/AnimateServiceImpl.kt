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

package org.opencastproject.animate.impl

import org.opencastproject.animate.api.AnimateService
import org.opencastproject.animate.api.AnimateServiceException
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.ConfigurationException
import org.opencastproject.util.IoSupport
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.workspace.api.Workspace

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Dictionary

/** Create video animations using Synfig  */
/** Creates a new animate service instance.  */
class AnimateServiceImpl : AbstractJobProducer(JOB_TYPE), AnimateService, ManagedService {

    /** Path to the synfig binary  */
    private var synfigBinary = SYNFIG_BINARY_DEFAULT

    /** The load introduced on the system by creating an inspect job  */
    private var jobLoad = JOB_LOAD_DEFAULT

    private var workspace: Workspace? = null
    protected override var serviceRegistry: ServiceRegistry? = null
        set
    override var securityService: SecurityService? = null
        set
    override var userDirectoryService: UserDirectoryService? = null
        set
    override var organizationDirectoryService: OrganizationDirectoryService? = null
        set

    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        logger.debug("Activated animate service")
    }

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>?) {
        if (properties == null)
            return
        logger.debug("Start updating animate service")

        synfigBinary = StringUtils.defaultIfBlank(properties.get(SYNFIG_BINARY_CONFIG) as String, SYNFIG_BINARY_DEFAULT)
        logger.debug("Set synfig binary path to {}", synfigBinary)

        jobLoad = LoadUtil.getConfiguredLoadValue(properties, JOB_LOAD_CONFIG, JOB_LOAD_DEFAULT, serviceRegistry!!)
        logger.debug("Set animate job load to {}", jobLoad)

        logger.debug("Finished updating animate service")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.process
     */
    @Throws(Exception::class)
    public override fun process(job: Job): String {
        logger.debug("Started processing job {}", job.id)
        if (OPERATION != job.operation) {
            throw ServiceRegistryException(String.format("This service can't handle operations of type '%s'",
                    job.operation))
        }

        val arguments = job.arguments
        val animation = URI(arguments[0])
        val gson = Gson()
        val metadata = gson.fromJson<Map<String, String>>(arguments[1], stringMapType)
        val options = gson.fromJson<List<String>>(arguments[2], stringListType)

        // filter animation and get new, custom input file
        val input = customAnimation(job, animation, metadata)

        // prepare output file
        val output = File(workspace!!.rootDirectory(), String.format("animate/%d/%s.%s", job.id,
                FilenameUtils.getBaseName(animation.path), "mkv"))
        FileUtils.forceMkdirParent(output)

        // create animation process.
        val command = ArrayList<String>()
        command.add(synfigBinary)
        command.add("-i")
        command.add(input.absolutePath)
        command.add("-o")
        command.add(output.absolutePath)
        command.addAll(options)
        logger.info("Executing animation command: {}", command)

        var process: Process? = null
        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()

            // print synfig (+ffmpeg) output
            BufferedReader(InputStreamReader(process!!.inputStream)).use { `in` ->
                var line: String
                while ((line = `in`.readLine()) != null) {
                    logger.debug("Synfig: {}", line)
                }
            }

            // wait until the task is finished
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw AnimateServiceException(String.format("Synfig exited abnormally with status %d (command: %s)",
                        exitCode, command))
            }
            if (!output.isFile) {
                throw AnimateServiceException("Synfig produced no output")
            }
            logger.info("Animation generated successfully: {}", output)
        } catch (e: Exception) {
            // Ensure temporary data are removed
            FileUtils.deleteQuietly(output.parentFile)
            logger.debug("Removed output directory of failed animation process: {}", output.parentFile)
            throw AnimateServiceException(e)
        } finally {
            IoSupport.closeQuietly(process)
            FileUtils.deleteQuietly(input)
        }

        val uri = workspace!!.putInCollection("animate-" + job.id, output.name,
                FileInputStream(output))
        FileUtils.deleteQuietly(File(workspace!!.rootDirectory(), String.format("animate/%d", job.id)))

        return uri.toString()
    }


    @Throws(IOException::class, NotFoundException::class)
    private fun customAnimation(job: Job, input: URI, metadata: Map<String, String>?): File {
        logger.debug("Start customizing the animation")
        val output = File(workspace!!.rootDirectory(), String.format("animate/%d/%s.%s", job.id,
                FilenameUtils.getBaseName(input.path), FilenameUtils.getExtension(input.path)))
        FileUtils.forceMkdirParent(output)
        var animation: String
        try {
            animation = FileUtils.readFileToString(File(input), "UTF-8")
        } catch (e: IOException) {
            // Maybe no local file?
            logger.debug("Falling back to workspace to read {}", input)
            workspace!!.read(input).use { `in` -> animation = IOUtils.toString(`in`, "UTF-8") }
        }

        // replace all metadata
        for ((key, value1) in metadata!!) {
            val value = StringEscapeUtils.escapeXml11(value1)
            animation = animation.replace("\\{\\{$key\\}\\}".toRegex(), value)
        }

        // write new animation file
        FileUtils.write(output, animation, "utf-8")

        return output
    }


    @Throws(AnimateServiceException::class)
    override fun animate(animation: URI, metadata: Map<String, String>, arguments: List<String>): Job {
        val gson = Gson()
        val jobArguments = Arrays.asList(animation.toString(), gson.toJson(metadata), gson.toJson(arguments))
        try {
            logger.debug("Create animate service job")
            return serviceRegistry!!.createJob(JOB_TYPE, OPERATION, jobArguments, jobLoad)
        } catch (e: ServiceRegistryException) {
            throw AnimateServiceException(e)
        }

    }

    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    companion object {

        /** Configuration key for setting a custom synfig path  */
        private val SYNFIG_BINARY_CONFIG = "synfig.path"

        /** Default path to the synfig binary  */
        val SYNFIG_BINARY_DEFAULT = "synfig"

        /** Configuration key for this operation's job load  */
        private val JOB_LOAD_CONFIG = "job.load.animate"

        /** The load introduced on the system by creating an inspect job  */
        private val JOB_LOAD_DEFAULT = 0.8f

        private val logger = LoggerFactory.getLogger(AnimateServiceImpl::class.java)

        /** List of available operations on jobs  */
        private val OPERATION = "animate"

        private val stringMapType = object : TypeToken<Map<String, String>>() {

        }.type
        private val stringListType = object : TypeToken<List<String>>() {

        }.type
    }
}
