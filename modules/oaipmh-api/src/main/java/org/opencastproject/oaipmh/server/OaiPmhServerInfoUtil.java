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
package org.opencastproject.oaipmh.server;

import static org.opencastproject.util.data.Option.option;

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.data.Option;

public final class OaiPmhServerInfoUtil {
  private OaiPmhServerInfoUtil() {
  }

  /** Organization config key. */
  public static final String ORG_CFG_OAIPMH_SERVER_HOSTURL = "org.opencastproject.oaipmh.server.hosturl";

  /** Get the OAI-PMH server URL of the current organization. */
  public static Option<String> oaiPmhServerUrlOfCurrentOrganization(SecurityService secSvc) {
    return option(secSvc.getOrganization().getProperties().get(ORG_CFG_OAIPMH_SERVER_HOSTURL));
  }
}
