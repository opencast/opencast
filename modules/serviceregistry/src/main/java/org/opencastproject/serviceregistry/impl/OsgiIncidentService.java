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

package org.opencastproject.serviceregistry.impl;

import static org.opencastproject.util.IoSupport.loadPropertiesFromUrl;
import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceEnvs;
import org.opencastproject.util.persistence.Queries;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.io.FilenameUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;

public class OsgiIncidentService extends AbstractIncidentService implements BundleListener {
  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(OsgiIncidentService.class);

  public static final String INCIDENT_L10N_DIR = "incident-l10n";

  /** Reference to the receipt service registry */
  private ServiceRegistry serviceRegistry;

  /** Reference to the receipt workflow service */
  private WorkflowService workflowService;

  private EntityManagerFactory emf;
  private PersistenceEnv penv;

  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @Override
  protected WorkflowService getWorkflowService() {
    return workflowService;
  }

  @Override
  protected PersistenceEnv getPenv() {
    return penv;
  }

  /**
   * Sets the service registry
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Sets the workflow service
   *
   * @param workflowService
   *          the workflow service
   */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * OSGi callback on component activation.
   */
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for job incidents");
    penv = PersistenceEnvs.persistenceEnvironment(emf);
    // scan bundles for incident localizations
    cc.getBundleContext().addBundleListener(this);
    for (Bundle b : cc.getBundleContext().getBundles()) {
      storeIncidentTexts(b);
    }
  }

  /**
   * Closes entity manager factory.
   */
  public void deactivate() {
    penv.close();
  }

  /** OSGi DI */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Override
  public void bundleChanged(BundleEvent event) {
    switch (event.getType()) {
      case BundleEvent.INSTALLED:
        storeIncidentTexts(event.getBundle());
        break;
      case BundleEvent.STOPPED:
      case BundleEvent.UNINSTALLED:
        // todo to support deletion of texts the service has to store a mapping
        // from bundle-id -> text-base-keys, e.g. 125 -> "org.opencastproject.composer"
        // In the end deletion is not really necessary, texts may simply be overwritten or just
        // eat up some database space.
        break;
      default:
        // do nothing
    }
  }

  private static final String PROPERTIES_GLOB = "*.properties";

  private void storeIncidentTexts(Bundle bundle) {
    logger.debug("Scanning bundle {}, (ID {}) for incident localizations", bundle.getSymbolicName(),
            bundle.getBundleId());
    final Enumeration<?> l10n = bundle.findEntries(INCIDENT_L10N_DIR, PROPERTIES_GLOB, false);
    while (l10n != null && l10n.hasMoreElements()) {
      final URL resourceUrl = (URL) l10n.nextElement();
      final String resourceFileName = resourceUrl.getPath();
      // e.g. org.opencastproject.composer.properties or org.opencastproject.composer_de.properties
      final String fullResourceName = FilenameUtils.getBaseName(resourceFileName);
      final String[] fullResourceNameParts = fullResourceName.split("_");
      // part 0 contains the key base, e.g. org.opencastproject.composer
      final String keyBase = fullResourceNameParts[0];
      final List<String> locale = mlist(fullResourceNameParts).drop(1).value();
      final Properties texts = loadPropertiesFromUrl(resourceUrl);
      for (String key : texts.stringPropertyNames()) {
        final String text = texts.getProperty(key);
        final String dbKey = mlist(keyBase, key).concat(locale).mkString(".");
        logger.debug("Storing text {}={}", dbKey, text);
        penv.tx(Queries.persistOrUpdate(IncidentTextDto.mk(dbKey, text)));
      }
    }
  }
}
