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

package org.opencastproject.inspection.impl.api.util;

import org.opencastproject.inspection.impl.api.MediaAnalyzer;
import org.opencastproject.inspection.impl.api.MediaAnalyzerException;
import org.opencastproject.inspection.impl.api.MediaContainerMetadata;
import org.opencastproject.util.ProcessExcecutorException;
import org.opencastproject.util.ProcessExecutor;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Support class for {@link org.opencastproject.inspection.impl.api.MediaAnalyzer} implementations that use an external
 * program for analysis.
 */
public abstract class CmdlineMediaAnalyzerSupport implements MediaAnalyzer {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CmdlineMediaAnalyzerSupport.class);

  /** Path to the executable */
  protected String binary;

  /** The extracted metadata */
  protected MediaContainerMetadata metadata = new MediaContainerMetadata();

  /** The commandline */
  protected String commandline = null;

  /**
   * 
   * 
   * @param binary
   */
  protected CmdlineMediaAnalyzerSupport(String binary) {
    this.binary = binary;
  }

  /**
   * Returns the binary used to provide media inspection functionality.
   * 
   * @return the binary
   */
  protected String getBinary() {
    return binary;
  }

  public void setBinary(String binary) {
    this.binary = binary;
  }

  public MediaContainerMetadata analyze(File media) throws MediaAnalyzerException {

    if (binary == null)
      throw new IllegalStateException("Binary is not set");

    String[] cmdOptions = getAnalysisOptions(media);
    commandline = binary + " " + StringUtils.join(cmdOptions, " ");

    // Analyze
    ProcessExecutor<MediaAnalyzerException> mediaAnalyzer = null;
    mediaAnalyzer = new ProcessExecutor<MediaAnalyzerException>(binary, cmdOptions) {
      @Override
      protected boolean onStdout(String line) {
        onAnalysis(line);
        return true;
      }

      @Override
      protected boolean onStderr(String line) {
        onAnalysis(line);
        return true;
      }

      @Override
      protected void onProcessFinished(int exitCode) throws MediaAnalyzerException {
        onFinished(exitCode);
      }
    };

    try {
      mediaAnalyzer.execute();
    } catch (ProcessExcecutorException e) {
      throw new MediaAnalyzerException("Error while running media analyzer " + binary, e);
    }

    postProcess();

    MediaContainerMetadata m = metadata;
    metadata = new MediaContainerMetadata();
    commandline = null;

    return m;
  }

  /**
   * Override this method to do any post processing on the gathered metadata. The default implementation does nothing.
   */
  protected void postProcess() {
  }

  /**
   * Returns the command line options that need to be passed to the media analyzer.
   * 
   * @param media
   *          the input media
   * @return the options for the call to the analyzer
   */
  protected abstract String[] getAnalysisOptions(File media);

  /**
   * This method will be called for every single line of output that the analysis tool returns. Like this, subclasses
   * are able to process the output returned by the tool and build up the resulting metadata.
   * 
   * @param line
   *          the line of output
   */
  protected abstract void onAnalysis(String line);

  /**
   * Returns the options that are need to check for required versions of a certain tool.
   * 
   * @return the options for version checks
   */
  protected String getVersionCheckOptions() {
    return null;
  }

  /**
   * This method will be called once the process returned. This implementation will check for exit codes different from
   * <code>-1</code>, <code>0</code> and <code>255</code> and throw an exception.
   * 
   * @param exitCode
   *          the processe's exit code
   * @throws MediaAnalyzerException
   *           if the exit code is different from -1, 0 or 255.
   */
  protected void onFinished(int exitCode) throws MediaAnalyzerException {
    // Windows binary will return -1 when queried for options
    if (exitCode != -1 && exitCode != 0 && exitCode != 255) {
      logger.error("Error code " + exitCode + " occured while executing '" + commandline + "'");
      throw new MediaAnalyzerException("Cmdline tool " + binary + " exited with exit code " + exitCode);
    }
  }

}
