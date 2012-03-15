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
package org.opencastproject.security.api;

import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ADMIN;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ANONYMOUS;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ID;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_NAME;
import static org.opencastproject.util.UrlSupport.DEFAULT_BASE_URL;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The default organization.
 */
public class DefaultOrganization extends Organization {

  /** The default organization properties */
  public static final Map<String, String> DEFAULT_PROPERTIES;

  static {
    Map<String, String> map = new HashMap<String, String>();
    map.put("logo_large", "/admin/img/mh_logos/MatterhornLogo_large.png");
    map.put("logo_small", "/admin/img/mh_logos/OpencastLogo.png");
    DEFAULT_PROPERTIES = Collections.unmodifiableMap(map);
  }

  /**
   * No-arg constructor needed by JAXB
   */
  public DefaultOrganization() {
    super(DEFAULT_ORGANIZATION_ID, DEFAULT_ORGANIZATION_NAME, DEFAULT_BASE_URL, DEFAULT_ORGANIZATION_ADMIN,
            DEFAULT_ORGANIZATION_ANONYMOUS, DEFAULT_PROPERTIES);
  }

  /**
   * Creates a default organization for the given hostname and port number.
   * 
   * @param host
   *          the server name
   * @param port
   *          the server port
   */
  public DefaultOrganization(String host, int port) {
    super(DEFAULT_ORGANIZATION_ID, DEFAULT_ORGANIZATION_NAME, host, port, DEFAULT_ORGANIZATION_ADMIN,
            DEFAULT_ORGANIZATION_ANONYMOUS, DEFAULT_PROPERTIES);
  }

}
