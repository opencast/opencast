/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.composer.impl.qtembedder;

import org.opencastproject.composer.impl.AbstractCmdlineEmbedderEngine;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

/**
 * QT embedder engine that is capable of creating soft subtitles in QT files. Uses <code>qtsbtlembedder</code>.
 *
 */
public class QTSbtlEmbedderEngine extends AbstractCmdlineEmbedderEngine {

  /** Default location of the qtsbtlembedder binary (resembling the installer) */
  public static final String QTEMBEDDER_BINARY_DEFAULT = "qtsbtlembedder";

  /** Parameter name for retrieving qt embedder path */
  private static final String CONFIG_QTEMBEDDER_PATH = "org.opencastproject.composer.qtembedder.path";

  /** Command line template for executing job */
  // WARNING use 0.3 with multiple subtitle embedding capability
  private static final String CMD_TEMPLATE = "#{-fonth param.fonth} #{-trackh param.trackh} #{-offset param.offset} #{-opt param.optimization} #{-out out.media.path} #{in.media.path} #<#{in.captions.path}@#{param.lang}>";

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(QTSbtlEmbedderEngine.class);

  /**
   * Create new QT embedder engine with default binary.
   */
  public QTSbtlEmbedderEngine() {
    super(QTEMBEDDER_BINARY_DEFAULT);
    setCmdTemplate(CMD_TEMPLATE);
  }

  /**
   * Activates component. Retrieve binary location if it was set.
   *
   * @param context
   *          component context
   */
  public void activate(ComponentContext context) {
    String path = (String) context.getBundleContext().getProperty(CONFIG_QTEMBEDDER_PATH);
    if (path == null) {
      logger.debug("DEFAULT " + CONFIG_QTEMBEDDER_PATH + ": " + QTEMBEDDER_BINARY_DEFAULT);
    } else {
      setBinary(path);
      logger.debug("QTSbtlEmbedderEngine config binary: {}", path);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.impl.AbstractCmdlineEmbedderEngine#handleEmbedderOutput(java.lang.String,
   *      java.io.File[])
   */
  @Override
  protected void handleEmbedderOutput(String output, File... sourceFiles) {
    if (output.startsWith("Info:")) {
      logger.info(output);
    } else if (output.startsWith("Warning:")) {
      logger.warn(output);
    } else if (output.startsWith("Error:")) {
      logger.error(output);
    }
  }

  /**
   * {@inheritDoc} Accepts ISO 639-1 language code and returns ISO 639-2 language code. If language cannot be
   * determined, returns <code>und</code>.
   *
   * @see org.opencastproject.composer.impl.AbstractCmdlineEmbedderEngine#normalizeLanguage(java.lang.String)
   */
  @Override
  protected String normalizeLanguage(String language) {

    if (language == null) {
      logger.warn("Language code attribute is null. Language set to: {}", "und");
      return "und";
    }

    // truncating if necessary
    if (language.length() > 2) {
      logger.warn("Language code {} too long, truncating...", language);
      language = language.substring(0, 2);
    }

    // set to lower case
    language = language.toLowerCase();

    // constructing locale
    Locale loc = new Locale(language);
    try {
      return loc.getISO3Language();
    } catch (MissingResourceException e) {
      logger.warn("Could not determine language. Language set to: {}", "und");
      return "und";
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.composer.impl.AbstractCmdlineEmbedderEngine#getOutputFile(java.util.Map)
   */
  @Override
  protected File getOutputFile(Map<String, String> properties) {
    // check if output file property has been set
    if (properties.containsKey("out.media.path")) {
      return new File(properties.get("out.media.path"));
    }
    return new File(properties.get("in.media.path"));
  }
}
