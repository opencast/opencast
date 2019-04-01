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

package org.opencastproject.execute.impl

import org.opencastproject.execute.api.ExecuteException
import org.opencastproject.execute.api.ExecuteService
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElement.Type
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.UnsupportedElementException
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

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URI
import java.util.ArrayList
import java.util.Dictionary
import java.util.HashSet
import java.util.NoSuchElementException
import java.util.Scanner
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Implements a service that runs CLI commands with MediaPackage elements as arguments
 */
/**
 * Creates a new instance of the execute service.
 */
class ExecuteServiceImpl : AbstractJobProducer(JOB_TYPE), ExecuteService, ManagedService {

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

    /** The workspace service  */
    protected var workspace: Workspace

    /**
     * List of allowed commands that can be run with an executor. By convention, an empty set doesn't mean any command can
     * be run. An '*' in the service configuration means any command can be executed
     */
    protected val allowedCommands: MutableSet<String> = HashSet()

    /** To allow command-line parameter substitutions configured globally i.e. in config.properties  */
    private var bundleContext: BundleContext? = null

    /** To allow command-line parameter substitutions configured at the service level  */
    private var properties: Dictionary<*, *>? = null

    private var executeJobLoad = 1.0f

    enum class Operation {
        Execute_Element, Execute_Mediapackage
    }

    /**
     * Activates this component with its properties once all of the collaborating services have been set
     *
     * @param cc
     * The component's context, containing the properties used for configuration
     */
    override fun activate(cc: ComponentContext) {
        super.activate(cc)

        properties = cc.properties

        if (properties != null) {
            val commandString = properties!!.get(COMMANDS_ALLOWED_PROPERTY) as String
            if (StringUtils.isNotBlank(commandString)) {
                logger.info("Execute Service permitted commands: {}", commandString)
                for (command in commandString.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    allowedCommands.add(command)
            }
        }

        this.bundleContext = cc.bundleContext
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.execute.api.ExecuteService.execute
     */
    @Throws(ExecuteException::class)
    override fun execute(exec: String, params: String, inElement: MediaPackageElement): Job {
        return execute(exec, params, inElement, null!!, null!!, executeJobLoad)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.execute.api.ExecuteService.execute
     */
    @Throws(ExecuteException::class)
    override fun execute(exec: String, params: String, inElement: MediaPackageElement, load: Float): Job {
        return execute(exec, params, inElement, null!!, null!!, load)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.execute.api.ExecuteService.execute
     */
    @Throws(ExecuteException::class, IllegalArgumentException::class)
    override fun execute(exec: String, params: String, inElement: MediaPackageElement, outFileName: String, expectedType: Type): Job {
        return execute(exec, params, inElement, null!!, null!!, executeJobLoad)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.execute.api.ExecuteService.execute
     * @throws IllegalArgumentException
     * if the input arguments are incorrect
     * @throws ExecuteException
     * if an internal error occurs
     */
    @Throws(ExecuteException::class, IllegalArgumentException::class)
    override fun execute(exec: String, params: String, inElement: MediaPackageElement, outFileName: String, expectedType: Type,
                         load: Float): Job {
        var outFileName = outFileName

        logger.debug("Creating Execute Job for command: {}", exec)

        if (StringUtils.isBlank(exec))
            throw IllegalArgumentException("The command to execute cannot be null")

        if (StringUtils.isBlank(params))
            throw IllegalArgumentException("The command arguments cannot be null")

        if (inElement == null)
            throw IllegalArgumentException("The input MediaPackage element cannot be null")

        outFileName = StringUtils.trimToNull(outFileName)
        if (outFileName == null && expectedType != null || outFileName != null && expectedType == null)
            throw IllegalArgumentException("Expected element type and output filename cannot be null")

        try {
            val paramList = ArrayList<String>(5)
            paramList.add(exec)
            paramList.add(params)
            paramList.add(MediaPackageElementParser.getAsXml(inElement))
            paramList.add(outFileName)
            paramList.add(expectedType?.toString())

            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Execute_Element.toString(), paramList, load)

        } catch (e: ServiceRegistryException) {
            throw ExecuteException(String.format("Unable to create a job of type '%s'", JOB_TYPE), e)
        } catch (e: MediaPackageException) {
            throw ExecuteException("Error serializing an element", e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.execute.api.ExecuteService.execute
     */
    @Throws(ExecuteException::class)
    override fun execute(exec: String, params: String, mp: MediaPackage, outFileName: String, expectedType: Type): Job {
        return execute(exec, params, mp, outFileName, expectedType, 1.0f)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.execute.api.ExecuteService.execute
     */
    @Throws(ExecuteException::class)
    override fun execute(exec: String, params: String, mp: MediaPackage, outFileName: String, expectedType: Type, load: Float): Job {
        var outFileName = outFileName
        if (StringUtils.isBlank(exec))
            throw IllegalArgumentException("The command to execute cannot be null")

        if (StringUtils.isBlank(params))
            throw IllegalArgumentException("The command arguments cannot be null")

        if (mp == null)
            throw IllegalArgumentException("The input MediaPackage cannot be null")

        outFileName = StringUtils.trimToNull(outFileName)
        if (outFileName == null && expectedType != null || outFileName != null && expectedType == null)
            throw IllegalArgumentException("Expected element type and output filename cannot be null")

        try {
            val paramList = ArrayList<String>(5)
            paramList.add(exec)
            paramList.add(params)
            paramList.add(MediaPackageParser.getAsXml(mp))
            paramList.add(outFileName)
            paramList.add(expectedType?.toString())

            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Execute_Mediapackage.toString(), paramList, load)
        } catch (e: ServiceRegistryException) {
            throw ExecuteException(String.format("Unable to create a job of type '%s'", JOB_TYPE), e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @throws ExecuteException
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.process
     */
    @Throws(ExecuteException::class)
    override fun process(job: Job): String {
        val arguments = ArrayList(job.arguments)

        // Check this operation is allowed
        if (!allowedCommands.contains("*") && !allowedCommands.contains(arguments[0]))
            throw ExecuteException("Command '" + arguments[0] + "' is not allowed")

        var outFileName: String? = null
        var strAux: String? = null
        var mp: MediaPackage? = null
        var expectedType: Type? = null
        var element: MediaPackageElement? = null
        var op: Operation? = null

        try {
            op = Operation.valueOf(job.operation)

            val nargs = arguments.size

            if (nargs != 3 && nargs != 5) {
                throw IndexOutOfBoundsException(
                        "Incorrect number of parameters for operation execute_" + op + ": " + arguments.size)
            }
            if (nargs == 5) {
                strAux = arguments.removeAt(4)
                expectedType = if (strAux == null) null else Type.valueOf(strAux)
                outFileName = StringUtils.trimToNull(arguments.removeAt(3))
                if (StringUtils.isNotBlank(outFileName) && expectedType == null || StringUtils.isBlank(outFileName) && expectedType != null) {
                    throw ExecuteException("The output type and filename must be both specified")
                }
                outFileName = if (outFileName == null) null else job.id.toString() + "_" + outFileName
            }

            when (op) {
                ExecuteServiceImpl.Operation.Execute_Mediapackage -> {
                    mp = MediaPackageParser.getFromXml(arguments.removeAt(2))
                    return doProcess(arguments, mp, outFileName, expectedType)
                }
                ExecuteServiceImpl.Operation.Execute_Element -> {
                    element = MediaPackageElementParser.getFromXml(arguments.removeAt(2))
                    return doProcess(arguments, element, outFileName, expectedType)
                }
                else -> throw IllegalStateException("Don't know how to handle operation '" + job.operation + "'")
            }

        } catch (e: MediaPackageException) {
            throw ExecuteException("Error unmarshalling the input mediapackage/element", e)
        } catch (e: IllegalArgumentException) {
            throw ExecuteException("This service can't handle operations of type '$op'", e)
        } catch (e: IndexOutOfBoundsException) {
            throw ExecuteException("The argument list for operation '$op' does not meet expectations", e)
        }

    }

    /**
     * Does the actual processing, given a mediapackage (Execute Once WOH)
     *
     * @param arguments
     * The list containing the program and its arguments
     * @param mp
     * MediaPackage used in the operation
     * @param outFileName
     * The name of the resulting file
     * @param expectedType
     * The expected element type
     * @return A `String` containing the command output
     * @throws ExecuteException
     * if some internal error occurred
     */
    @Throws(ExecuteException::class)
    fun doProcess(arguments: MutableList<String>, mp: MediaPackage, outFileName: String?, expectedType: Type?): String {

        var params = arguments.removeAt(1)

        var outFile: File? = null
        var elementsByFlavor: Array<MediaPackageElement>? = null

        try {
            if (outFileName != null) {
                // FIXME : Find a better way to place the output File
                val firstElement = workspace.get(mp.elements[0].getURI())
                outFile = File(firstElement.parentFile, outFileName)
            }

            // Get the substitution pattern.
            // The following pattern matches, any construct with the form
            // #{name}
            // , where 'name' is the value of a certain property. It is stored in the backreference group 1.
            // Optionally, expressions can take a parameter, like
            // #{name(parameter)}
            // , where 'parameter' is the name of a certain parameter.
            // If specified, 'parameter' is stored in the group 2. Otherwise it's null.
            // Both name and parameter match any character sequence that does not contain {, }, ( or ) .
            val pat = Pattern.compile("#\\{([^\\{\\}\\(\\)]+)(?:\\(([^\\{\\}\\(\\)]+)\\))?\\}")

            // Substitute the appearances of the patterns with the actual absolute paths
            val matcher = pat.matcher(params)
            val sb = StringBuffer()
            while (matcher.find()) {
                // group(1) = property. group(2) = (optional) parameter
                if (matcher.group(1) == "id") {
                    matcher.appendReplacement(sb, mp.identifier.toString())
                } else if (matcher.group(1) == "flavor") {
                    elementsByFlavor = mp.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor(matcher.group(2)))
                    if (elementsByFlavor.size == 0)
                        throw ExecuteException("No elements in the MediaPackage match the flavor '" + matcher.group(2) + "'.")

                    if (elementsByFlavor.size > 1)
                        logger.warn("Found more than one element with flavor '{}'. Using {} by default...", matcher.group(2),
                                elementsByFlavor[0].identifier)

                    val elementFile = workspace.get(elementsByFlavor[0].getURI())
                    matcher.appendReplacement(sb, elementFile.absolutePath)
                } else if (matcher.group(1) == "out") {
                    matcher.appendReplacement(sb, outFile!!.absolutePath)
                } else if (properties!!.get(matcher.group(1)) != null) {
                    matcher.appendReplacement(sb, properties!!.get(matcher.group(1)) as String)
                } else if (bundleContext!!.getProperty(matcher.group(1)) != null) {
                    matcher.appendReplacement(sb, bundleContext!!.getProperty(matcher.group(1)))
                }
            }
            matcher.appendTail(sb)
            params = sb.toString()
        } catch (e: IllegalArgumentException) {
            throw ExecuteException("Tag 'flavor' must specify a valid MediaPackage element flavor.", e)
        } catch (e: NotFoundException) {
            throw ExecuteException(
                    "The element '" + elementsByFlavor!![0].getURI().toString() + "' does not exist in the workspace.", e)
        } catch (e: IOException) {
            throw ExecuteException("Error retrieving MediaPackage element from workspace: '"
                    + elementsByFlavor!![0].getURI().toString() + "'.", e)
        }

        arguments.addAll(splitParameters(params))

        return runCommand(arguments, outFile, expectedType)
    }

    /**
     * Does the actual processing, given a mediapackage element (Execute Many WOH)
     *
     * @param arguments
     * The list containing the program and its arguments
     * @param outFileName
     * The name of the resulting file
     * @param expectedType
     * The expected element type
     * @return A `String` containing the command output
     * @throws ExecuteException
     * if some internal error occurred
     */
    @Throws(ExecuteException::class)
    fun doProcess(arguments: MutableList<String>, element: MediaPackageElement, outFileName: String?, expectedType: Type?): String {

        // arguments(1) contains a list of space-separated arguments for the command
        val params = arguments.removeAt(1)
        arguments.addAll(splitParameters(params))

        var outFile: File? = null

        try {
            // Get the track file from the workspace
            val trackFile = workspace.get(element.getURI())

            // Put the destination file in the same folder as the source file
            if (outFileName != null)
                outFile = File(trackFile.parentFile, outFileName)

            // Substitute the appearances of the patterns with the actual absolute paths
            for (i in 1 until arguments.size) {
                if (arguments[i].contains(INPUT_FILE_PATTERN)) {
                    arguments[i] = arguments[i].replace(INPUT_FILE_PATTERN, trackFile.absolutePath)
                    continue
                }
                if (arguments[i].contains(OUTPUT_FILE_PATTERN)) {
                    if (outFile != null) {
                        arguments[i] = arguments[i].replace(OUTPUT_FILE_PATTERN, outFile.absolutePath)
                        continue
                    } else {
                        logger.error("{} pattern found, but no valid output filename was specified", OUTPUT_FILE_PATTERN)
                        throw ExecuteException(
                                OUTPUT_FILE_PATTERN + " pattern found, but no valid output filename was specified")
                    }
                }
            }

            return runCommand(arguments, outFile, expectedType)

        } catch (e: IOException) {
            logger.error("Error retrieving file from workspace: {}", element.getURI())
            throw ExecuteException("Error retrieving file from workspace: " + element.getURI(), e)
        } catch (e: NotFoundException) {
            logger.error("Element '{}' cannot be found in the workspace.", element.getURI())
            throw ExecuteException("Element " + element.getURI() + " cannot be found in the workspace")
        }

    }

    @Throws(ExecuteException::class)
    private fun runCommand(command: List<String>, outFile: File?, expectedType: Type?): String {

        var p: Process? = null
        var result = 0

        try {
            logger.info("Running command {}", command[0])
            logger.debug("Starting subprocess {} with arguments {}", command[0],
                    StringUtils.join(command.subList(1, command.size), ", "))

            val pb = ProcessBuilder(command)
            pb.redirectErrorStream(true)

            p = pb.start()
            result = p!!.waitFor()

            logger.debug("Command {} finished with result {}", command[0], result)

            if (result == 0) {
                // Read the command output
                if (outFile != null) {
                    if (outFile.isFile) {
                        val newURI = workspace.putInCollection(ExecuteService.COLLECTION, outFile.name, FileInputStream(outFile))
                        if (outFile.delete()) {
                            logger.debug("Deleted the local copy of the encoded file at {}", outFile.absolutePath)
                        } else {
                            logger.warn("Unable to delete the encoding output at {}", outFile.absolutePath)
                        }
                        return MediaPackageElementParser.getAsXml(MediaPackageElementBuilderFactory.newInstance()
                                .newElementBuilder().elementFromURI(newURI, expectedType, null))
                    } else {
                        throw ExecuteException("Expected output file does not exist: " + outFile.absolutePath)
                    }
                }
                return ""
            } else {
                // 'Scanner' reads tokens delimited by an specific character (set).
                // By telling a Scanner to use the 'beginning of the input boundary' character as delimiter, which of course
                // will never find, yields the whole String as the next token.
                var line: String
                try {
                    Scanner(p.inputStream).use { scanner ->
                        scanner.useDelimiter("\\A")
                        line = scanner.next()
                    }
                } catch (e: NoSuchElementException) {
                    line = ""
                }

                throw ExecuteException(String.format("Process %s returned error code %d with this output:\n%s",
                        command[0], result, line.trim { it <= ' ' }))
            }
        } catch (e: InterruptedException) {
            throw ExecuteException("The executor thread has been unexpectedly interrupted", e)
        } catch (e: IOException) {
            // Only log the first argument, the executable, as other arguments may contain sensitive values
            // e.g. MySQL password/user, paths, etc. that should not be shown to caller
            logger.error("Could not start subprocess {}", command[0])
            throw ExecuteException("Could not start subprocess: " + command[0], e)
        } catch (e: UnsupportedElementException) {
            throw ExecuteException("Couldn't create a new MediaPackage element of type " + expectedType!!.toString(), e)
        } catch (e: ConfigurationException) {
            throw ExecuteException("Couldn't instantiate a new MediaPackage element builder", e)
        } catch (e: MediaPackageException) {
            throw ExecuteException("Couldn't serialize a new Mediapackage element of type " + expectedType!!.toString(), e)
        } finally {
            IoSupport.closeQuietly(p)
        }
    }

    /**
     * Returns a list of strings broken on whitespace characters except where those whitespace characters are escaped or
     * quoted.
     *
     * @return list of individual arguments
     */
    private fun splitParameters(input: String): List<String> {

        // This delimiter matches any non-escaped quote
        val quoteDelim = "(?<!\\\\)\""

        // This delimiter matches any number of non-escaped spaces
        val spaceDelim = "((?<!\\\\)\\s)+"

        val parsedInput = ArrayList<String>()
        var quoted = false

        for (token1 in input.split(quoteDelim.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            if (quoted) {
                parsedInput.add(token1)
                quoted = false
            } else {
                for (token2 in token1.split(spaceDelim.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                // This ignores empty tokens if quotes are at the beginning or the end of the string
                    if (!token2.isEmpty())
                        parsedInput.add(token2)
                quoted = true
            }

        return parsedInput
    }

    /**
     * Sets a reference to the workspace service.
     *
     * @param workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    @Throws(org.osgi.service.cm.ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>) {
        executeJobLoad = LoadUtil.getConfiguredLoadValue(properties, EXECUTE_JOB_LOAD_KEY, DEFAULT_EXECUTE_JOB_LOAD,
                serviceRegistry!!)
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ExecuteServiceImpl::class.java)

        /** Bundle property specifying which commands can be run with this executor  */
        val COMMANDS_ALLOWED_PROPERTY = "commands.allowed"

        /** The approximate load placed on the system by running an execute operation  */
        val DEFAULT_EXECUTE_JOB_LOAD = 0.1f

        /** The key to look for in the service configuration file to override the [DEFAULT_EXECUTE_JOB_LOAD]  */
        val EXECUTE_JOB_LOAD_KEY = "job.load.execute"
    }

}
