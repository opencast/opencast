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

package org.opencastproject.speechtotext.impl.engine;

import org.opencastproject.speechtotext.api.SpeechToTextEngine;
import org.opencastproject.speechtotext.api.SpeechToTextEngineException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.IoSupport;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

/** Vosk implementation of the Speech-to-text engine interface. */
@Component(
    immediate = true,
    service = {
        SpeechToTextEngine.class,
        ManagedService.class
    },
    property = {
        "service.description=Vosk implementation of the SpeechToTextEngine interface",
        "service.pid=org.opencastproject.speechtotext.impl.engine.VoskEngine"
    }
)
public class VoskEngine implements SpeechToTextEngine, ManagedService {

  private static final Logger log = LoggerFactory.getLogger(VoskEngine.class);

  /** Name of the engine. */
  private static final String engineName = "Vosk";

  /** Config key for setting the path to the vosk. */
  private static final String VOSK_EXECUTABLE_PATH_CONFIG_KEY = "vosk.root.path";

  /** Default path to vosk. */
  public static final String VOSK_EXECUTABLE_DEFAULT_PATH = "vosk-cli";

  /** Currently used path of the vosk installation. */
  private String voskExecutable = VOSK_EXECUTABLE_DEFAULT_PATH;


  @Override
  public String getEngineName() {
    return engineName;
  }

  @Activate
  public void activate(ComponentContext cc) {
    log.debug("Activating Vosk as viable speech-to-text engine...");
  }

  /**
   * OSGI callback when the configuration is updated. This method is only here to prevent the
   * configuration admin service from calling the service deactivate and activate methods
   * for a config update. It does not have to do anything as the updates are handled by updated().
   */
  @Modified
  public void modified(Map<String, Object> config) throws ConfigurationException {
    log.debug("Modified vosk engine service");
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null) {
      return;
    }
    log.debug("Start updating Vosk configuration.");
    voskExecutable = StringUtils.defaultIfBlank(
            (String) properties.get(VOSK_EXECUTABLE_PATH_CONFIG_KEY), VOSK_EXECUTABLE_DEFAULT_PATH);
    log.debug("Set vosk path to {}", voskExecutable);

    log.debug("Finished updating Vosk configuration");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.speechtotext.api.SpeechToTextEngine#generateSubtitlesFile(URI, File, String)
   */
  @Override
  public File generateSubtitlesFile(URI mediaFile, File preparedOutputFile, String language)
          throws SpeechToTextEngineException {

    final List<String> command = new ArrayList<>();
    command.add(voskExecutable);
    command.add("-i");
    command.add(mediaFile.toString());
    command.add("-o");
    command.add(preparedOutputFile.getAbsolutePath());
    command.add("-l");
    command.add(language);
    log.info("Executing Vosk's transcription command: {}", command);

    Process process = null;
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      process = processBuilder.start();

      // wait until the task is finished
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new SpeechToTextEngineException(
                String.format("Vosk exited abnormally with status %d (command: %s)", exitCode, command));
      }
      if (!preparedOutputFile.isFile()) {
        throw new SpeechToTextEngineException("Vosk produced no output");
      }
      log.info("Subtitles file generated successfully: {}", preparedOutputFile);
    } catch (Exception e) {
      log.debug("Transcription failed closing Vosk transcription process for: {}", mediaFile);
      throw new SpeechToTextEngineException(e);
    } finally {
      IoSupport.closeQuietly(process);
    }

    return preparedOutputFile; // now containing subtitles data
  }

}
