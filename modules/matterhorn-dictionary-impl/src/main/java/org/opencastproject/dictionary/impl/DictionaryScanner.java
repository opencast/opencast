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

import static org.opencastproject.util.ReadinessIndicator.ARTIFACT;

import org.opencastproject.dictionary.api.DictionaryService;
import org.opencastproject.util.ReadinessIndicator;

import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Loads language packs into the dictionary service, deleting the language pack on completion.
 */
public class DictionaryScanner implements ArtifactInstaller {
  private static final Logger logger = LoggerFactory.getLogger(DictionaryScanner.class);

  /** The dictionary in which we can install languages */
  protected DictionaryService dictionaryService = null;

  /** OSGi bundle context */
  private BundleContext bundleCtx = null;

  /** Sum of profiles files currently installed */
  private int sumInstalledFiles = 0;

  /**
   * OSGi callback on component activation.
   *
   * @param ctx
   *          the bundle context
   */
  void activate(BundleContext ctx) {
    this.bundleCtx = ctx;
  }

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
      sumInstalledFiles++;
    } else {
      logger.info("Loading language pack from {}", artifact);
      // read csv file and fill dictionary index
      BufferedReader br = null;
      try {
        br = new BufferedReader(new InputStreamReader(new FileInputStream(artifact), "utf-8"), 1024 * 1024);
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
            logger.warn("Unable to add word '{}' to the {} dictionary: {}",
                    new String[] { word, language, e.getMessage() });
          }
        }
        sumInstalledFiles++;
      } catch (Exception e) {
        logger.error("Error installing dictionary from {}", artifact.getAbsolutePath());
        return;
      } finally {
        IOUtils.closeQuietly(br);
      }
      logger.info("Finished loading language pack from {}", artifact);
    }

    // Determine the number of available profiles
    String[] filesInDirectory = artifact.getParentFile().list(new FilenameFilter() {
      public boolean accept(File arg0, String name) {
        return name.endsWith(".csv");
      }
    });

    // Once all profiles have been loaded, announce readiness
    if (filesInDirectory.length == sumInstalledFiles) {
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(ARTIFACT, "dictionary");
      logger.debug("Indicating readiness of dictionnaries");
      bundleCtx.registerService(ReadinessIndicator.class.getName(), new ReadinessIndicator(), properties);
      logger.info("All {} dictionaries installed", filesInDirectory.length);
    } else {
      logger.info("{} of {} dictionaries installed", sumInstalledFiles, filesInDirectory.length);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  @Override
  public void uninstall(File artifact) throws Exception {
    String language = artifact.getName().split("\\.")[0];
    dictionaryService.clear(language);
    sumInstalledFiles--;
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
