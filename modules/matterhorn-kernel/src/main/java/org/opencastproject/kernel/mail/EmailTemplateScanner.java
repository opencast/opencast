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
package org.opencastproject.kernel.mail;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EmailTemplateScanner implements ArtifactInstaller {

  /** OSGi bundle context */
  private BundleContext bundleCtx = null;

  /** Sum of template files currently installed */
  // private int sumInstalledFiles = 0;

  /** The templates map */
  private Map<String, String> templates = new HashMap<String, String>();

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(EmailTemplateScanner.class);

  /**
   * Returns the list of templates.
   * 
   * @return the email templates
   */
  public Map<String, String> getTemplates() {
    return templates;
  }

  /**
   * OSGi callback on component activation.
   * 
   * @param ctx
   *          the bundle context
   */
  void activate(BundleContext ctx) {
    logger.debug("EmailTemplateScanner activated");
    this.bundleCtx = ctx;
  }

  /**
   * Returns the email template for the given file name or <code>null</code> if no such one.
   * 
   * @param fileName
   *          the template file name
   * @return the email template text
   */
  public String getTemplate(String fileName) {
    return templates.get(fileName);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return "email".equals(artifact.getParentFile().getName());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  @Override
  public void install(File artifact) throws Exception {
    logger.info("EmailTemplateScanner install called");
    /*
     * logger.info("Registering email template from {}", artifact.getName()); FileReader in = null; BufferedReader
     * reader = null; StringBuilder stringBuilder = new StringBuilder(); try { in = new FileReader(artifact); reader =
     * new BufferedReader(in); String line = null; String ls = System.getProperty("line.separator");
     * 
     * while ((line = reader.readLine()) != null) { stringBuilder.append(line); stringBuilder.append(ls); }
     * templates.put(artifact.getName(), stringBuilder.toString()); sumInstalledFiles++; } catch (Exception e) {
     * logger.error("Email template could not be read from {}: {}", artifact, e.getMessage()); } finally {
     * IOUtils.closeQuietly(reader); IOUtils.closeQuietly(in); }
     * 
     * // Determine the number of available templates String[] filesInDirectory = artifact.getParentFile().list(new
     * FilenameFilter() { public boolean accept(File arg0, String name) { return true; } });
     * 
     * // Once all templates have been loaded, announce readiness if (filesInDirectory.length == sumInstalledFiles) {
     * Dictionary<String, String> properties = new Hashtable<String, String>(); properties.put(ARTIFACT,
     * "emailtemplates"); logger.debug("Indicating readiness of email templates");
     * bundleCtx.registerService(ReadinessIndicator.class.getName(), new ReadinessIndicator(), properties);
     * logger.info("All {} email templates installed", filesInDirectory.length); } else {
     * logger.debug("{} of {} email templates installed", sumInstalledFiles, filesInDirectory.length); }
     */
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  @Override
  public void uninstall(File artifact) throws Exception {
    logger.info("EmailTemplateScanner install called");
    /*
     * for (Iterator<String> iter = templates.values().iterator(); iter.hasNext();) { String temp = iter.next(); if
     * (artifact.getName().equals(temp)) { logger.info("Uninstalling template {}", temp); iter.remove(); } }
     */
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  @Override
  public void update(File artifact) throws Exception {
    uninstall(artifact);
    install(artifact);
  }

}
