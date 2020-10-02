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

package org.opencastproject.execute.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;

/**
 * API for a service that runs CLI commands with MediaPackage elements as arguments
 */

public interface ExecuteService {

  /** Receipt type */
  String JOB_TYPE = "org.opencastproject.execute";

  /** Placeholder to be replaced by the actual track location in the command line */
  String INPUT_FILE_PATTERN = "#{in}";

  /** Placeholder to be replaced by the actual track location in the command line */
  String OUTPUT_FILE_PATTERN = "#{out}";

  /** The subdirectory of the REST endpoint for this service */
  String ENDPOINT_NAME = "execute";

  /** Name of the form parameter in the REST endpoints that contains the name of the command */
  String EXEC_FORM_PARAM = "exec";

  /** Name of the form parameter in the REST endpoints that contains the command arguments */
  String PARAMS_FORM_PARAM = "params";

  /** Name of the form parameter in the REST endpoints that contains the load estimate */
  String LOAD_FORM_PARAM = "load";

  /** Name of the form parameter in the REST endpoints that contains the serialized input element */
  String INPUT_ELEM_FORM_PARAM = "inputElement";

  /** Name of the form parameter in the REST endpoints that contains the serialized input element */
  String INPUT_MP_FORM_PARAM = "inputMediaPackage";

  /** Name of the form parameter in the REST endpoints that contains the name of the file generated by the command */
  String OUTPUT_NAME_FORM_PARAMETER = "outputFilename";

  /** Name of the form parameter in the REST endpoints that contains the element type of the generated file */
  String TYPE_FORM_PARAMETER = "expectedType";

  /** The collection for the executor files */
  String COLLECTION = "executor";

  /**
   * Execute the operation specified by {@code exec} with the argument string indicated by {@code args}. This method may
   * create an output file with the name {@code outFileName} and a
   * {@link org.opencastproject.mediapackage.MediaPackageElement} will be created with the
   * {@link org.opencastproject.mediapackage.MediaPackageElement.Type} indicated by the argument {@code type}.
   * 
   * @param exec
   *          The command to be executed by this method
   * @param args
   *          A string containing the argument list for this command. The special argument {@value #INPUT_FILE_PATTERN}
   *          will be substituted by the location of the resource represented by the {@code inElement} parameter, and
   *          {@value #OUTPUT_FILE_PATTERN} by the file name indicated in {@code outFileName}.
   * @param inElement
   *          An {@link org.opencastproject.mediapackage.MediaPackageElement} object to be used as an input to the
   *          command
   * @param outFileName
   *          The name of the file the command may possibly create.
   * @param type
   *          The {@link org.opencastproject.mediapackage.MediaPackageElement.Type} of the {@code MediaPackageElement}
   *          created by this command.
   * @param load
   *          A floating point estimate of the load imposed on the node by executing the command
   * @return A {@link org.opencastproject.job.api.Job} representing the execution of the command. After a successful
   *         execution, the {link @Job} payload will contain a serialized mediapackage element.
   * @throws ExecuteException
   *           if this method fails to create the {@code Job} correctly
   */
  Job execute(String exec, String args, MediaPackageElement inElement, String outFileName,
          MediaPackageElement.Type type, float load) throws ExecuteException;

  /**
   * Execute the operation specified by {@code exec} with the argument string indicated by {@code args}. This method
   * accepts a {@link org.opencastproject.mediapackage.MediaPackage} as an argument, and elements within that
   * MediaPackage may be referenced in the argument list by using certain placeholders.
   * 
   * @param exec
   *          The command to be executed by this method
   * @param args
   *          A string containing the argument list for this command. Some special placeholders are allowed, that will
   *          be converted into the actual locations of elements in the supplied MediaPackage
   * @param mp
   *          The {@link org.opencastproject.mediapackage.MediaPackage} containing {@code MediaPackageElements} that
   *          will be used as inputs of the command
   * @param outFileName
   *          The name of the file the command may possibly create.
   * @param type
   *          The {@link org.opencastproject.mediapackage.MediaPackageElement.Type} of the {@code MediaPackageElement}
   *          created by this command.
   * @param load
   *          A floating point estimate of the load imposed on the node by executing the command
   * @return A {@link org.opencastproject.job.api.Job} representing the execution of the command. After a successful
   *         execution, the {link @Job} payload will contain a serialized mediapackage element.
   * @throws ExecuteException
   *           if this method fails to create the {@code Job} correctly
   */
  Job execute(String exec, String args, MediaPackage mp, String outFileName, MediaPackageElement.Type type, float load)
          throws ExecuteException;

}
