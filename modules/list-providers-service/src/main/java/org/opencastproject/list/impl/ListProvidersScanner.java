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

package org.opencastproject.list.impl;

import static org.opencastproject.list.impl.ListProvidersServiceImpl.ALL_ORGANIZATIONS;
import static org.opencastproject.list.impl.ListProvidersServiceImpl.ResourceTuple;

import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class ListProvidersScanner implements ArtifactInstaller {
  /** The directory name that has the properties file defining the list providers **/
  public static final String LIST_PROVIDERS_DIRECTORY = "listproviders";
  /** The key to look for in the properties file to name the list provider. **/
  public static final String LIST_NAME_KEY = "list.name";
  /** The key to look for in the properties file defining if the list's values should be translated */
  public static final String LIST_TRANSLATABLE_KEY = "list.translatable";
  /** The key to attach this list to a particular org, if not present then all orgs can get list **/
  public static final String LIST_ORG_KEY = "list.org";
  /** The key to define a default value **/
  public static final String LIST_DEFAULT_KEY = "list.default";


  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(ListProvidersScanner.class);
  /** The Map to go from file locations of properties files to list names. **/
  private Map<String, ListProvidersServiceImpl.ResourceTuple> fileToListNames = new HashMap<>();
  /** The list providers service to add the list provider to. **/
  private ListProvidersService listProvidersService;

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
    return LIST_PROVIDERS_DIRECTORY.equals(artifact.getParentFile().getName())
            && artifact.getName().endsWith(".properties");
  }

  /**
   * Inner class used to represent a new list.
   */
  private static class SingleResourceListProviderImpl implements ResourceListProvider {
    private String listName;
    private String orgId;
    private Map<String, String> list;
    private boolean translatable;
    private String defaultKey;

    SingleResourceListProviderImpl(String listName, Map<String, String> list, String organizationId,
            boolean translatable) {
      this.listName = listName;
      this.list = list;
      this.orgId = organizationId;
      this.translatable = translatable;
    }

    public String getListName() {
      return listName;
    }

    @Override
    public String[] getListNames() {
      return new String[] { listName };
    }

    @Override
    public Map<String, String> getList(String listName, ResourceListQuery query)
            throws ListProviderNotFoundException {
      logger.debug("Getting list {} with query {} for org {}", listName, query, orgId);
      if (this.listName.equals(listName)) return Collections.unmodifiableMap(list);
      else throw new ListProviderNotFoundException("Unable to read list data from file");
    }

    @Override
    public boolean isTranslatable(String listName) {
      return translatable;
    }

    public void setDefault(String defaultKey) {
      this.defaultKey = defaultKey;
    }

    @Override
    public String getDefault() {
      return defaultKey;
    }
  }

  /**
   * Adds a resource list provider to the provider service
   *
   * @param path the path to the property file
   * @param properties the properties from the property file
   */
  private void addResourceListProvider(String path, Properties properties) {
    String listName = properties.getProperty(LIST_NAME_KEY);
    if (StringUtils.isBlank(listName)) {
      logger.error("Unable to add {} as a list provider because the {} entry was empty. "
              + "Please add it to get this list provider to work.", path, LIST_NAME_KEY);
      return;
    }
    String defaultKey = properties.getProperty(LIST_DEFAULT_KEY);
    boolean translatable = BooleanUtils.toBoolean(properties.getProperty(LIST_TRANSLATABLE_KEY));
    String orgId = Objects.toString(properties.getProperty(LIST_ORG_KEY), ALL_ORGANIZATIONS);

    logger.info("Adding {} for {}", listName, orgId);

    HashMap<String, String> list = new HashMap<>();
    for (Map.Entry<Object, Object> entry: properties.entrySet()) {
      switch (entry.getKey().toString().toLowerCase()) {
        case LIST_NAME_KEY:
        case LIST_TRANSLATABLE_KEY:
        case LIST_DEFAULT_KEY:
        case LIST_ORG_KEY:
          logger.debug("Skipping key: {}", entry.getKey());
          break;
        default:
          list.put(entry.getKey().toString(), entry.getValue().toString());
          logger.debug("Found entry: {}", entry);
      }
    }

    SingleResourceListProviderImpl listProvider = new SingleResourceListProviderImpl(
            listName,
            list,
            orgId,
            translatable);
    if (StringUtils.isNotBlank(defaultKey)) {
      listProvider.setDefault(defaultKey);
    }
    fileToListNames.put(path, new ResourceTuple(listName, orgId));
    listProvidersService.addProvider(listName, listProvider, orgId);
  }

  private static Properties readProperties(File file) throws IOException {
    Properties properties = new Properties();
    InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
    properties.load(reader);
    return properties;
  }

  /**
   * Remove a list provider based upon the location of its configuration file.
   *
   * @param artifact
   *          The File representing the configuration file for the list.
   */
  private void removeResourceListProvider(File artifact) {
    ResourceTuple resource = fileToListNames.remove(artifact.getAbsolutePath());
    if (resource != null) {
      logger.info("Removing {} for {}", resource.getResourceName(), resource.getOrganizationId());
      listProvidersService.removeProvider(resource.getResourceName(), resource.getOrganizationId());
    }
  }

  @Override
  public void install(File artifact) {
    String filePath = artifact.getAbsolutePath();
    logger.info("Installing list provider {}", filePath);

    Properties properties;
    try {
      properties = readProperties(artifact);
    } catch (IOException e) {
      logger.error("Unable to read file " + filePath);
      return;
    }
    addResourceListProvider(filePath, properties);
  }

  @Override
  public void update(File artifact) {
    String filePath = artifact.getAbsolutePath();
    logger.info("Updating list provider {}", filePath);
    // get the previous affiliation
    ResourceTuple resource = fileToListNames.get(filePath);

    Properties properties;
    try {
      properties = readProperties(artifact);
    } catch (IOException e) {
      logger.error("Unable to read file " + filePath);
      return;
    }
    String orgId = Objects.toString(properties.getProperty(LIST_ORG_KEY), ALL_ORGANIZATIONS);
    if (resource != null && !orgId.equals(resource.getOrganizationId())) { // the org ID was changed
      removeResourceListProvider(artifact);
    }
    addResourceListProvider(filePath, properties);
  }

  @Override
  public void uninstall(File artifact) {
    logger.info("Removing list provider {}", artifact.getAbsolutePath());
    removeResourceListProvider(artifact);
  }
}
