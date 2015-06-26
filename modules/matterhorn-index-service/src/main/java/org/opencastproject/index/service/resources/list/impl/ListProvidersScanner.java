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

package org.opencastproject.index.service.resources.list.impl;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;

import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ListProvidersScanner implements ArtifactInstaller {
  /** The directory name that has the properties file defining the list providers **/
  public static final String LIST_PROVIDERS_DIRECTORY = "listproviders";
  /** The key to look for in the properties file to name the list provider. **/
  public static final String LIST_NAME_KEY = "list.name";
  /** The key to attach this list to a particular org, if not present then all orgs can get list **/
  public static final String LIST_ORG_KEY = "list.org";
  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(ListProvidersScanner.class);
 /** The Map to go from file locations of properties files to list names. **/
  private Map<String, String> fileToListNames = new HashMap<String, String>();
  /** The list providers service to add the list provider to. **/
  private ListProvidersService listProvidersService;

  void activate(BundleContext ctx) {
    logger.info("Activated");
  }

  void deactivate(BundleContext ctx) {
    logger.info("Deactivated");
  }

  public void setListProvidersService(ListProvidersService listProvidersService) {
    this.listProvidersService = listProvidersService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return LIST_PROVIDERS_DIRECTORY.equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".properties");
  }

  /**
   * Inner class used to represent a new list.
   */
  private class SingleResourceListProviderImpl implements ResourceListProvider {
    private String listName;
    private String orgId = "";
    private Map<String, Object> list;

    /**
     * Default constructor.
     *
     * @param listName
     *          The name of the new list to add.
     * @param list
     *          The list of properties to expose.
     */
    public SingleResourceListProviderImpl(String listName, Map<String, Object> list) {
      this.listName = listName;
      this.list = list;
    }

    public String getListName() {
      return listName;
    }

    @Override
    public String[] getListNames() {
      String[] listNames = { listName };
      return listNames;
    }

    public void setOrg(String orgName) {
      this.orgId = orgName;
    }

    @Override
    public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
            throws ListProviderException {
      logger.info("Getting list " + listName + " query " + query + " org " + organization);
      if (this.listName.equals(listName) && "".equals(this.orgId)) {
        return Collections.unmodifiableMap(list);
      }
      else if (this.listName.equals(listName) && organization != null && organization.getId().equals(this.orgId)) {
        return Collections.unmodifiableMap(list);
      } else {
        return null;
      }
    }

  }

  /**
   * Add a list provider based upon a configuration file.
   *
   * @param artifact
   *          The File representing the configuration file for the list.
   */
  public void addResourceListProvider(File artifact) throws IOException {
    logger.debug("Adding {}", artifact.getAbsolutePath());
    // Format name
    FileInputStream in = null;
    Properties properties = new Properties();
    try {
      in = new FileInputStream(artifact);
      properties.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    String listName = "";
    if (properties.getProperty(LIST_NAME_KEY) != null) {
      listName = properties.getProperty(LIST_NAME_KEY).toString();
    }

    String orgId = "";
    logger.debug("Found list with name '{}'", listName);
    if (!"".equals(listName)) {
      HashMap<String, Object> list = new HashMap<String, Object>();
      Enumeration<Object> keys = properties.keys();
      while (keys.hasMoreElements()) {
        Object key = keys.nextElement();
        String keyString = key.toString();
        if (!keyString.equalsIgnoreCase(LIST_NAME_KEY) && !keyString.equalsIgnoreCase(LIST_ORG_KEY)) {
          String value = properties.get(key).toString();
          list.put(keyString, value);
          logger.debug("Found key:{} value:{}", keyString, value);
        } else if (keyString.equalsIgnoreCase(LIST_ORG_KEY)) {
          orgId = properties.get(key).toString();
          logger.debug("Found org:{}", orgId);
        }
        else {
          logger.debug("Skipping key:{}", keyString);
        }
      }

      SingleResourceListProviderImpl singleResourceListProviderImpl = new SingleResourceListProviderImpl(listName, list);
      if (orgId != null && !"".equals(orgId)) {
        singleResourceListProviderImpl.setOrg(orgId);
      }
      listProvidersService.addProvider(singleResourceListProviderImpl.getListName(), singleResourceListProviderImpl);
      fileToListNames.put(artifact.getAbsolutePath(), singleResourceListProviderImpl.getListName());
    } else {
      logger.error("Unable to add {} as a list provider because the {} entry was empty. Please add it to get this list provider to work.", new Object[] {artifact.getAbsolutePath(), LIST_NAME_KEY, listName});
    }
  }

  /**
   * Remove a list provider based upon the location of its configuration file.
   *
   * @param artifact
   *          The File representing the configuration file for the list.
   */
  public void removeResourceListProvider(File artifact) {
    String listName = fileToListNames.remove(artifact.getAbsoluteFile());
    if (listName != null) {
      listProvidersService.removeProvider(listName);
    }
  }

  @Override
  public void install(File artifact) throws Exception {
    logger.info("Installing list provider {}", artifact.getAbsolutePath());
    addResourceListProvider(artifact);
  }

  @Override
  public void update(File artifact) throws Exception {
    logger.info("Updating list provider {}", artifact.getAbsolutePath());
    removeResourceListProvider(artifact);
    addResourceListProvider(artifact);
  }

  @Override
  public void uninstall(File artifact) throws Exception {
    logger.info("Removing list provider {}", artifact.getAbsolutePath());
    removeResourceListProvider(artifact);
  }
}
