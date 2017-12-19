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

package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EmbedderEngine;

import java.io.File;
import java.util.Map;

/**
 * Abstract class for command line embedder engines.
 *
 */
public abstract class AbstractCmdlineEmbedderEngine implements EmbedderEngine {

  /** the encoder binary */
  private String binary = null;

  /** the command line options */
  private String cmdTemplate = "";

  /**
   * Creates embedder engine with given binary.
   *
   * @param binary
   *          path to the binary of specific embedder
   */
  public AbstractCmdlineEmbedderEngine(String binary) {
    if (binary == null) {
      throw new IllegalArgumentException("Binary is null.");
    }

    this.binary = binary;
  }

  /**
   * Set binary for embedder engine.
   *
   * @param binary
   */
  protected void setBinary(String binary) {
    if (binary == null) {
      throw new IllegalArgumentException("Binary is null.");
    }
    this.binary = binary;
  }

  /**
   * Gets the binary for the embedder engine.
   *
   * @return the binary for the embedder engine
   */
  protected String getBinary() {
    return this.binary;
  }

  /**
   * Set template command for embedder engine. Variables are specified in one of two ways: #{&lt;switch&gt; &lt;key&gt;}
   * or #{&lt;key&gt;}. For array parameters (those parameters that can be set multiple times) the following form is
   * used: #&lt; one_or_more_variables &gt;
   *
   * @param cmdTemplate
   *          template for given command line embedder engine
   */
  protected void setCmdTemplate(String cmdTemplate) {
    this.cmdTemplate = cmdTemplate;
  }

  /**
   * Get the template command for embedder engine. Variables are specified in one of two ways: #{&lt;switch&gt; &lt;key&gt;}
   * or #{&lt;key&gt;}. For array parameters (those parameters that can be set multiple times) the following form is
   * used: #&lt; one_or_more_variables &gt;
   *
   * @return the template for given command line embedder engine
   */
  protected String getCmdTemplate() {
    return this.cmdTemplate;
  }

  /**
   * Function that normalizes language attribute to valid language code for specific embedder engine. Should be able to
   * process any input, even <code>null</code> string and substitute it for reasonable code or return null. In this case
   * exception will be thrown.
   *
   * @param language
   * @return
   */
  protected abstract String normalizeLanguage(String language);

  /**
   * Method to which embedder output is directed.
   *
   * @param output
   *          embedder output
   * @param sourceFiles
   *          source files used in operation
   */
  protected abstract void handleEmbedderOutput(String output, File... sourceFiles);

  /**
   * Returns file resulting from embedding.
   *
   * @param properties
   *          properties used for initiating embedding job
   * @return resulting file
   */
  protected abstract File getOutputFile(Map<String, String> properties);
}
