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
package org.opencastproject.email.template.impl;

import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EmailTemplateScanner implements ArtifactInstaller {

  /** The templates map */
  private final Map<String, String> templates = new ConcurrentHashMap<String, String>();

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
    logger.info("EmailTemplateScanner activated");
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
    InputStream is = new FileInputStream(artifact);
    String template = IOUtils.toString(is);
    templates.put(artifact.getName(), template);
    logger.info("Template {} installed", artifact.getName());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  @Override
  public void uninstall(File artifact) throws Exception {
    for (Iterator<String> iter = templates.values().iterator(); iter.hasNext();) {
      String temp = iter.next();
      if (artifact.getName().equals(temp)) {
        logger.info("Uninstalling template {}", temp);
        iter.remove();
      }
    }
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
