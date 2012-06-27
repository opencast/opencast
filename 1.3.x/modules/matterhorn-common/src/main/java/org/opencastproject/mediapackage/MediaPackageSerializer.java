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
package org.opencastproject.mediapackage;

import java.net.URI;
import java.net.URISyntaxException;

public interface MediaPackageSerializer {

  /**
   * This method is called every time a url is being written to a media package manifest. By implementing this method,
   * serializers are able to store package elements in directories relative to some common root folder, thereby making
   * it movable.
   * 
   * @param uri
   *          the url to encode
   * @return the encoded path
   */
  String encodeURI(URI uri);

  /**
   * This method is called every time a url is being read from a media package manifest. By implementing this method,
   * serializers are able to redirect urls to local caches which might make sense when it comes to dealing with huge
   * media files.
   * 
   * @param path
   *          the original path from the manifest
   * @return the resolved url
   * @throws URISyntaxException
   *           if the path cannot be converted into a url
   */
  URI resolvePath(String path) throws URISyntaxException;

}
