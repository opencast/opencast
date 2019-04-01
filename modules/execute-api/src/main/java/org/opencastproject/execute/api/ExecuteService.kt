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

package org.opencastproject.execute.api

import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement

/**
 * API for a service that runs CLI commands with MediaPackage elements as arguments
 */

interface ExecuteService {

    /**
     * Execute the operation specified by `exec` with the argument string indicated by `args`.
     *
     * @param exec
     * The command to be executed by this method
     * @param args
     * A string containing the argument list for this command. The special argument {@value #INPUT_FILE_PATTERN}
     * will be substituted by the location of the resource represented by the `inElement` parameter
     * @param inElement
     * An [org.opencastproject.mediapackage.MediaPackageElement] object to be used as an input to the
     * command
     * @return A [org.opencastproject.job.api.Job] representing the execution of the command. After a successful
     * execution, the {link @Job} payload will contain a serialized mediapackage element.
     * @throws ExecuteException
     * if this method fails to create the `Job` correctly
     */
    @Throws(ExecuteException::class)
    fun execute(exec: String, args: String, inElement: MediaPackageElement): Job

    /**
     * Execute the operation specified by `exec` with the argument string indicated by `args`.
     *
     * @param exec
     * The command to be executed by this method
     * @param args
     * A string containing the argument list for this command. The special argument {@value #INPUT_FILE_PATTERN}
     * will be substituted by the location of the resource represented by the `inElement` parameter
     * @param inElement
     * An [org.opencastproject.mediapackage.MediaPackageElement] object to be used as an input to the
     * command
     * @param load
     * A floating point estimate of the load imposed on the node by executing the command
     * @return A [org.opencastproject.job.api.Job] representing the execution of the command. After a successful
     * execution, the {link @Job} payload will contain a serialized mediapackage element.
     * @throws ExecuteException
     * if this method fails to create the `Job` correctly
     */
    @Throws(ExecuteException::class)
    fun execute(exec: String, args: String, inElement: MediaPackageElement, load: Float): Job

    /**
     * Execute the operation specified by `exec` with the argument string indicated by `args`. This method may
     * create an output file with the name `outFileName` and a
     * [org.opencastproject.mediapackage.MediaPackageElement] will be created with the
     * [org.opencastproject.mediapackage.MediaPackageElement.Type] indicated by the argument `type`.
     *
     * @param exec
     * The command to be executed by this method
     * @param args
     * A string containing the argument list for this command. The special argument {@value #INPUT_FILE_PATTERN}
     * will be substituted by the location of the resource represented by the `inElement` parameter, and
     * {@value #OUTPUT_FILE_PATTERN} by the file name indicated in `outFileName`.
     * @param inElement
     * An [org.opencastproject.mediapackage.MediaPackageElement] object to be used as an input to the
     * command
     * @param outFileName
     * The name of the file the command may possibly create.
     * @param type
     * The [org.opencastproject.mediapackage.MediaPackageElement.Type] of the `MediaPackageElement`
     * created by this command.
     * @return A [org.opencastproject.job.api.Job] representing the execution of the command. After a successful
     * execution, the {link @Job} payload will contain a serialized mediapackage element.
     * @throws ExecuteException
     * if this method fails to create the `Job` correctly
     */
    @Throws(ExecuteException::class)
    fun execute(exec: String, args: String, inElement: MediaPackageElement, outFileName: String, type: MediaPackageElement.Type): Job

    /**
     * Execute the operation specified by `exec` with the argument string indicated by `args`. This method may
     * create an output file with the name `outFileName` and a
     * [org.opencastproject.mediapackage.MediaPackageElement] will be created with the
     * [org.opencastproject.mediapackage.MediaPackageElement.Type] indicated by the argument `type`.
     *
     * @param exec
     * The command to be executed by this method
     * @param args
     * A string containing the argument list for this command. The special argument {@value #INPUT_FILE_PATTERN}
     * will be substituted by the location of the resource represented by the `inElement` parameter, and
     * {@value #OUTPUT_FILE_PATTERN} by the file name indicated in `outFileName`.
     * @param inElement
     * An [org.opencastproject.mediapackage.MediaPackageElement] object to be used as an input to the
     * command
     * @param outFileName
     * The name of the file the command may possibly create.
     * @param type
     * The [org.opencastproject.mediapackage.MediaPackageElement.Type] of the `MediaPackageElement`
     * created by this command.
     * @param load
     * A floating point estimate of the load imposed on the node by executing the command
     * @return A [org.opencastproject.job.api.Job] representing the execution of the command. After a successful
     * execution, the {link @Job} payload will contain a serialized mediapackage element.
     * @throws ExecuteException
     * if this method fails to create the `Job` correctly
     */
    @Throws(ExecuteException::class)
    fun execute(exec: String, args: String, inElement: MediaPackageElement, outFileName: String,
                type: MediaPackageElement.Type, load: Float): Job

    /**
     * Execute the operation specified by `exec` with the argument string indicated by `args`. This method
     * accepts a [org.opencastproject.mediapackage.MediaPackage] as an argument, and elements within that
     * MediaPackage may be referenced in the argument list by using certain placeholders.
     *
     * @param exec
     * The command to be executed by this method
     * @param args
     * A string containing the argument list for this command. Some special placeholders are allowed, that will
     * be converted into the actual locations of elements in the supplied MediaPackage
     * @param mp
     * The [org.opencastproject.mediapackage.MediaPackage] containing `MediaPackageElements` that
     * will be used as inputs of the command
     * @param outFileName
     * The name of the file the command may possibly create.
     * @param type
     * The [org.opencastproject.mediapackage.MediaPackageElement.Type] of the `MediaPackageElement`
     * created by this command.
     * @return A [org.opencastproject.job.api.Job] representing the execution of the command. After a successful
     * execution, the {link @Job} payload will contain a serialized mediapackage element.
     * @throws ExecuteException
     * if this method fails to create the `Job` correctly
     */
    @Throws(ExecuteException::class)
    fun execute(exec: String, args: String, mp: MediaPackage, outFileName: String, type: MediaPackageElement.Type): Job

    /**
     * Execute the operation specified by `exec` with the argument string indicated by `args`. This method
     * accepts a [org.opencastproject.mediapackage.MediaPackage] as an argument, and elements within that
     * MediaPackage may be referenced in the argument list by using certain placeholders.
     *
     * @param exec
     * The command to be executed by this method
     * @param args
     * A string containing the argument list for this command. Some special placeholders are allowed, that will
     * be converted into the actual locations of elements in the supplied MediaPackage
     * @param mp
     * The [org.opencastproject.mediapackage.MediaPackage] containing `MediaPackageElements` that
     * will be used as inputs of the command
     * @param outFileName
     * The name of the file the command may possibly create.
     * @param type
     * The [org.opencastproject.mediapackage.MediaPackageElement.Type] of the `MediaPackageElement`
     * created by this command.
     * @param load
     * A floating point estimate of the load imposed on the node by executing the command
     * @return A [org.opencastproject.job.api.Job] representing the execution of the command. After a successful
     * execution, the {link @Job} payload will contain a serialized mediapackage element.
     * @throws ExecuteException
     * if this method fails to create the `Job` correctly
     */
    @Throws(ExecuteException::class)
    fun execute(exec: String, args: String, mp: MediaPackage, outFileName: String, type: MediaPackageElement.Type, load: Float): Job

    companion object {

        /** Receipt type  */
        val JOB_TYPE = "org.opencastproject.execute"

        /** Placeholder to be replaced by the actual track location in the command line  */
        val INPUT_FILE_PATTERN = "#{in}"

        /** Placeholder to be replaced by the actual track location in the command line  */
        val OUTPUT_FILE_PATTERN = "#{out}"

        /** The subdirectory of the REST endpoint for this service  */
        val ENDPOINT_NAME = "execute"

        /** Name of the form parameter in the REST endpoints that contains the name of the command  */
        val EXEC_FORM_PARAM = "exec"

        /** Name of the form parameter in the REST endpoints that contains the command arguments  */
        val PARAMS_FORM_PARAM = "params"

        /** Name of the form parameter in the REST endpoints that contains the load estimate  */
        val LOAD_FORM_PARAM = "load"

        /** Name of the form parameter in the REST endpoints that contains the serialized input element  */
        val INPUT_ELEM_FORM_PARAM = "inputElement"

        /** Name of the form parameter in the REST endpoints that contains the serialized input element  */
        val INPUT_MP_FORM_PARAM = "inputMediaPackage"

        /** Name of the form parameter in the REST endpoints that contains the name of the file generated by the command  */
        val OUTPUT_NAME_FORM_PARAMETER = "outputFilename"

        /** Name of the form parameter in the REST endpoints that contains the element type of the generated file  */
        val TYPE_FORM_PARAMETER = "expectedType"

        /** The collection for the executor files  */
        val COLLECTION = "executor"
    }

}
