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
package org.opencastproject.index.service.resources.list.api;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.security.api.Organization;

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
   * @param organization
   *          The organization context
   * @return a list of tuple id - value from the given source
   */
  Map<String, Object> getList(String providerName, ResourceListQuery query, Organization organization)
          throws ListProviderException;

  /**
   * Adds an source to the service
   * 
   * @param name
   *          The name of the source
   * @param provider
   *          The list provider to add
   */
  void addProvider(String name, ResourceListProvider provider);

  /**
   * Removes the given source
   * 
   * @param name
   *          The provider to remove
   */
  void removeProvider(String name);

  /**
   * Returns if the given source name is or not available
   * 
   * @param name
   *          The source to check
   * @return true if a source with the given name is available in the service
   */
  boolean hasProvider(String name);

  /**
   * Returns the resources list providers available
   * 
   * @return the list of available providers
   */
  List<String> getAvailableProviders();
}
