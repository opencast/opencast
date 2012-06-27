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
package org.opencastproject.dictionary.impl;

import org.opencastproject.dictionary.api.DictionaryService;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Loads language packs into the dictionary service, deleting the language pack on completion.
 */
public class DictionaryScanner implements ArtifactInstaller {
  private static final Logger logger = LoggerFactory.getLogger(DictionaryScanner.class);

  /** The dictionary in which we can install languages */
  protected DictionaryService dictionaryService = null;

  /** Sets the dictionary service */
  public void setDictionaryService(DictionaryService dictionaryService) {
    this.dictionaryService = dictionaryService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return "dictionaries".equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".csv");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  @Override
  public void install(File artifact) throws Exception {
    Integer numAllW = 1;
    String language = artifact.getName().split("\\.")[0];
    
    // Make sure we are not importing something that already exists
    if (Arrays.asList(dictionaryService.getLanguages()).contains(language)) {
      logger.debug("Skipping existing dictionary '{}'", language);
      return;
    }

    logger.info("Loading language pack from {}", artifact);

    // read csv file and fill dictionary index
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(artifact)), 1024 * 1024);
    String wordLine;
    while ((wordLine = br.readLine()) != null) {
      if (wordLine.startsWith("#")) {
        if (wordLine.startsWith("#numAllW")) {
          numAllW = Integer.valueOf(wordLine.split(":")[1]);
        }
        continue;
      }
      String[] arr = wordLine.split(",");
      String word = arr[0];
      Integer count = Integer.valueOf(arr[1]);
      Double weight = 1.0 * count / numAllW;
      try {
        dictionaryService.addWord(word, language, count, weight);
      } catch (Exception e) {
        logger.warn("Unable to add word '{}' to the {} dictionary: {}", new String[] { word, language, e.getMessage() });
      }
    }

    logger.info("Finished loading language pack from {}", artifact);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  @Override
  public void uninstall(File artifact) throws Exception {
    // Do nothing
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  @Override
  public void update(File artifact) throws Exception {
    // Do nothing
  }
}
