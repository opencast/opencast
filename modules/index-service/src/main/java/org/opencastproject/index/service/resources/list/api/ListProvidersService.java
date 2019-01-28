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

package org.opencastproject.index.service.resources.list.api;

import org.opencastproject.index.service.exception.ListProviderException;

import java.util.List;
import java.util.Map;

/**
 * Service to generate list of key - value from different sources for autocomplete / filetring purpose
 */
public interface ListProvidersService {

  /**
   * Returns the list for the given provider
   *
   * @param providerName
   *          The name of the source
   * @param query
   *          The query for the list
   * @return a list of tuple id - value from the given source
   */
  Map<String, String> getList(String providerName, ResourceListQuery query, boolean inverseValueKey)
          throws ListProviderException;

  /**
   * Defines if keys and values of the given list should be translated in the administrative user interface.
   *
   * @param listName the name of the list
   * @return if the results should be translated
   *
   * @throws ListProviderException
   *              if no list provider found for the given list name
   */
  boolean isTranslatable(String listName) throws ListProviderException;

  /**
   * Defines the key of a default value in the given list.
   *
   * @param listName the name of the list
   * @return the key of the default value
   *
   * @throws ListProviderException
   *              if no list provider found for the given list name
   */
  String getDefault(String listName) throws ListProviderException;

  /**
   * Adds an source to the service
   *
   * @param name
   *          The name of the source
   * @param provider
   *          The list provider to add
   */
  void addProvider(String name, ResourceListProvider provider);

  void addProvider(String name, ResourceListProvider provider, String organizationId);

  /**
   * Removes the given source
   *
   * @param name
   *          The provider to remove
   */
  void removeProvider(String name);

  void removeProvider(String name, String organizationId);

  /**
   * Returns if the given source name is or not available
   *
   * @param name
   *          The source to check
   * @return true if a source with the given name is available in the service
   */
  boolean hasProvider(String name);

  boolean hasProvider(String name, String organizationId);

  /**
   * Returns the resources list providers available
   *
   * @return the list of available providers
   */
  List<String> getAvailableProviders();
}
