/*
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

package org.opencastproject.kernel.security;

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.userdirectory.api.UserReferenceProvider;
import org.opencastproject.util.OsgiUtil;

import org.apache.commons.io.FilenameUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.config.SecurityNamespaceHandler;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;

/**
 * Registers a security filter, which delegates to the spring filter chain appropriate for the current request's
 * organization. Organizational security configurations may be added to the security watch directory, and should be
 * named &lt;organization_id&gt;.xml.
 */
@Component(
    immediate = true,
    service = ArtifactInstaller.class,
    property = {
        "service.description=Security Configuration Scanner"
    }
)
public class SpringSecurityConfigurationArtifactInstaller implements ArtifactInstaller {
  protected static final Logger logger = LoggerFactory.getLogger(SpringSecurityConfigurationArtifactInstaller.class);

  /** This component's bundle context */
  protected BundleContext bundleContext = null;

  /** The security filter */
  protected SecurityFilter securityFilter = null;
  /** The security service reference for Spring beans */
  protected SecurityService securityService = null;
  /** The user directory reference for Spring beans */
  protected UserDirectoryService userDirectory = null;
  /** The user detail service reference for Spring beans */
  protected UserDetailsService userDetailsService = null;
  /** The user reference provider service reference for Spring beans */
  protected UserReferenceProvider userReferenceProvider = null;
  /** LTI 1.1. configuration services */
  protected OAuthConsumerDetailsService oAuthConsumerDetailsService = null;
  protected LtiLaunchAuthenticationHandler ltiLaunchAuthenticationHandler = null;
  /** LDAP authorities populators, keyed by organization id and, inside each organization, keyed by pid/instanceId */
  protected Map<String, Map<String, LdapAuthoritiesPopulator>> ldapAuthoritiesPopulators =
          new HashMap<String, Map<String, LdapAuthoritiesPopulator>>();
  /** List of expected LDAP instances for each organization */
  private Map<String, Set<String>> expectedLdapInstances = new HashMap<String, Set<String>>();
  /**
   * List of security/ORG_ID.xml files that were waiting for LDAP authorities populator instances, keyed by organization
   * ID
   */
  private Map<String, File> pendingArtifactsToInstall = new HashMap<String, File>();

  private static final String LDAP_AUTH_POPULATOR_BEAN_NAME_PREFIX = "ldapAuthoritiesPopulator_";

  /** Spring application contexts */
  protected Map<String, GenericApplicationContext> appContexts = null;

  /**
   * Configuration listing all expected LDAP authorities populator instances/pid for each organization.
   * Example:
   * ldap.instances.mh_default_org=ldap1,ldap2
   * ldap.instances.dce=ldap3
   * ldap.instances.manchester=ldap4
   */
  private static final String LDAP_INSTANCES_PREFIX = "ldap.instances.";

  /** OSGi DI. */
  @Reference
  public void setSecurityFilter(SecurityFilter securityFilter) {
    logger.info("Set SecurityFilter");
    this.securityFilter = securityFilter;
  }

  @Reference
  void setSecurityService(SecurityService securityService) {
    logger.info("Set SecurityService");
    this.securityService = securityService;
  }

  @Reference
  void setUserDirectory(UserDirectoryService userDirectory) {
    logger.info("Set UserDirectoryService");
    this.userDirectory = userDirectory;
  }

  @Reference
  public void setUserDetailsService(UserDetailsService userDetailsService) {
    logger.info("Set UserDetailsService");
    this.userDetailsService = userDetailsService;
  }

  @Reference
  public void setUserReferenceProvider(UserReferenceProvider userReferenceProvider) {
    logger.info("Set UserReferenceProvider");
    this.userReferenceProvider = userReferenceProvider;
  }

  @Reference
  void setOAuthConsumerDetailsService(OAuthConsumerDetailsService oAuthConsumerDetailsService) {
    logger.info("Set setOAuthConsumerDetailsService");
    this.oAuthConsumerDetailsService = oAuthConsumerDetailsService;
  }

  @Reference
  void setLtiLaunchAuthenticationHandler(LtiLaunchAuthenticationHandler ltiLaunchAuthenticationHandler) {
    logger.info("Set LtiLaunchAuthenticationHandler");
    this.ltiLaunchAuthenticationHandler = ltiLaunchAuthenticationHandler;
  }

  /* These are set as LdapAuthoritiesPopulator service properties when they are registered */
  private static final String INSTANCE_ID_SERVICE_PROPERTY_KEY = "instanceId";
  private static final String ORGANIZATION_ID_SERVICE_PROPERTY_KEY = "orgId";

  @Reference(
      cardinality = ReferenceCardinality.MULTIPLE, // 0..n
          policy = ReferencePolicy.DYNAMIC, unbind = "unbindLdapAuthoritiesPopulator"
  )
  synchronized void bindLdapAuthoritiesPopulator(LdapAuthoritiesPopulator service, Map<String, Object> properties)
          throws Exception {
    String orgId = (String) properties.get(ORGANIZATION_ID_SERVICE_PROPERTY_KEY);
    String instanceId = (String) properties.get(INSTANCE_ID_SERVICE_PROPERTY_KEY);
    if (ldapAuthoritiesPopulators.get(orgId) == null) {
      ldapAuthoritiesPopulators.put(orgId, new HashMap<String, LdapAuthoritiesPopulator>());
    }
    ldapAuthoritiesPopulators.get(orgId).put(instanceId, service);

    // If all expected LDAP instances ready, process pending artifact if there is one
    if (allExpectedAlreadyRegistered(orgId) && pendingArtifactsToInstall.containsKey(orgId)) {
      createSecurityContext(pendingArtifactsToInstall.get(orgId));
      // Remove from pending list
      pendingArtifactsToInstall.remove(orgId);
    }
  }

  synchronized void unbindLdapAuthoritiesPopulator(LdapAuthoritiesPopulator service, Map<String, Object> properties) {
    String orgId = (String) properties.get(ORGANIZATION_ID_SERVICE_PROPERTY_KEY);
    String instanceId = (String) properties.get(INSTANCE_ID_SERVICE_PROPERTY_KEY);
    if (ldapAuthoritiesPopulators.get(orgId) != null) {
      ldapAuthoritiesPopulators.get(orgId).remove(instanceId);
    }
  }

  /**
   * OSGI activation callback
   */
  @Activate
  protected void activate(ComponentContext cc) {
    this.bundleContext = cc.getBundleContext();
    this.appContexts = new HashMap<>();

    Collections.list(cc.getProperties().keys()).stream().filter(s -> s.startsWith(LDAP_INSTANCES_PREFIX)).forEach(s -> {
      String orgId = s.substring(LDAP_INSTANCES_PREFIX.length());
      String instanceList = OsgiUtil.getComponentContextProperty(cc, s);
      expectedLdapInstances.put(orgId, new HashSet(Arrays.asList(instanceList.split(","))));
    });
    expectedLdapInstances.forEach((orgId, instances) -> logger.info(orgId + " = " + instances));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return "security".equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".xml");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  @Override
  public synchronized void install(File artifact) throws Exception {
    logger.trace("Install " + artifact.getAbsolutePath());

    // Are there any expected LDAP authorities populators and have all already registered?
    String orgId = FilenameUtils.getBaseName(artifact.getName());
    if (allExpectedAlreadyRegistered(orgId)) {
      createSecurityContext(artifact);
    } else {
      logger.warn("Not all LDAP authorities populators registered so artifact installation is pending.");
      pendingArtifactsToInstall.put(orgId, artifact);
    }
  }

  /**
   * Returns true if all expected LDAP authorities populators have already registered.
   *
   * @param orgId
   *          The organization ID
   * @return true if all registered
   */
  private boolean allExpectedAlreadyRegistered(String orgId) {
    Set<String> expected = expectedLdapInstances.get(orgId) != null
            ? expectedLdapInstances.get(orgId)
            : new HashSet<String>();
    Set<String> registered = ldapAuthoritiesPopulators.get(orgId) != null
            ? ldapAuthoritiesPopulators.get(orgId).keySet()
            : new HashSet<String>();

    return expected.size() == 0 || registered.containsAll(expected);
  }

  private void createSecurityContext(File artifact) throws Exception {
    // If we already have a registration for this ID, take it out of the security filter and close it
    String orgId = FilenameUtils.getBaseName(artifact.getName());
    logger.info("Creating security context for organization {}", orgId);

    GenericApplicationContext orgAppContext = appContexts.get(orgId);
    if (orgAppContext != null) {
      securityFilter.removeFilter(orgId);
      orgAppContext.close();
    }

    orgAppContext = new GenericApplicationContext();
    // this did NOT work orgAppContext.setClassLoader(SecurityNamespaceHandler.class.getClassLoader());
    // When loading the beans, setting the spring application context class
    // loader to this bundle's and explicitly importing the spring classes in the pom fixed
    // the ClassNotFoundExceptions. TODO Is this the best way? Is there more to add?
    orgAppContext.setClassLoader(this.getClass().getClassLoader());

    // Manually add the OSGI dependencies
    orgAppContext.getBeanFactory().registerSingleton("securityService", this.securityService);
    orgAppContext.getBeanFactory().registerSingleton("userDirectoryService", this.userDirectory);
    orgAppContext.getBeanFactory().registerSingleton("userDetailsService", this.userDetailsService);
    orgAppContext.getBeanFactory().registerSingleton("userReferenceProvider", this.userReferenceProvider);
    orgAppContext.getBeanFactory().registerSingleton("oAuthConsumerDetailsService", this.oAuthConsumerDetailsService);
    orgAppContext.getBeanFactory().registerSingleton("ltiLaunchAuthenticationHandler",
            this.ltiLaunchAuthenticationHandler);
    if (ldapAuthoritiesPopulators.get(orgId) != null) {
      for (Map.Entry<String, LdapAuthoritiesPopulator> entry : ldapAuthoritiesPopulators.get(orgId).entrySet()) {
        logger.trace("Registering bean {}", LDAP_AUTH_POPULATOR_BEAN_NAME_PREFIX + entry.getKey());
        orgAppContext.getBeanFactory().registerSingleton(LDAP_AUTH_POPULATOR_BEAN_NAME_PREFIX + entry.getKey(),
                entry.getValue());
      }
    }

    XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(orgAppContext);
    // So that it finds META-INF/spring.handlers
    xmlBeanDefinitionReader.setNamespaceHandlerResolver(
            new DefaultNamespaceHandlerResolver(SecurityNamespaceHandler.class.getClassLoader()));
    FileSystemResource beanFile = new FileSystemResource(artifact);
    xmlBeanDefinitionReader.loadBeanDefinitions(beanFile);

    logger.info("registered {} items in {} for {} from file {}", orgAppContext.getBeanDefinitionCount(),
            orgAppContext.getBeanDefinitionNames(), orgId, artifact.getAbsolutePath());

    // Refresh the spring application context
    try {
      orgAppContext.refresh();
    } catch (Exception e) {
      logger.error("Unable to refresh spring security configuration file {}: {}", artifact, e);
      orgAppContext.close();
      return;
    }

    // Keep track of the app context so we can close it later
    appContexts.put(orgId, orgAppContext);

    // Add the filter chain for this org to the security filter
    securityFilter.addFilter(orgId, (Filter) orgAppContext.getBean("springSecurityFilterChain"));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  @Override
  public void uninstall(File artifact) throws Exception {
    String orgId = FilenameUtils.getBaseName(artifact.getName());
    GenericApplicationContext appContext = appContexts.get(orgId);
    if (appContext != null) {
      securityFilter.removeFilter(orgId);
      appContexts.remove(orgId);
      appContext.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  @Override
  public void update(File artifact) throws Exception {
    install(artifact);
  }

}
