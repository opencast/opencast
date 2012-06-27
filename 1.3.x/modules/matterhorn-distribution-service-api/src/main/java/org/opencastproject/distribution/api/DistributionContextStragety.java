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
package org.opencastproject.distribution.api;


/**
 * DistributionContextStrategy implementations provide mappings between mediapackages and distribution channel specific
 * keys (such as keys for obtaining iTunesU tabs or youTube playlists).
 */
public interface DistributionContextStragety {
  /**
   * Gets an identifier for a mediapackage. Implementations may return a series identifier, a creator name, or any other
   * kind of metadata obtained from the mediapackage.
   * 
   * @param mediaPackageId
   *          The mediapackage id
   * @return The context ID
   */
  String getContextId(String mediaPackageId);

  /**
   * Gets the name for a context within a distribution channel. This method is called once to create a new tab or
   * playlist.
   * 
   * @param mediaPackageId
   *          The mediapackage id
   * @return The name of the context (playlist, tab, etc)
   */
  String getContextName(String mediaPackageId);

}
