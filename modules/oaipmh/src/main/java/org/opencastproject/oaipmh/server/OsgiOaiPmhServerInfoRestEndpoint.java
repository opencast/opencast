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
package org.opencastproject.oaipmh.server;

import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.Path;

@Path("/oaipmhinfo")
@RestService(
        name = "oaipmhserverinfo",
        title = "OAI-PMH server info service",
        abstractText = "This service "
                + "provides system gateway to the OAI-PMH server.",
        notes = {})
@Component(
    immediate = true,
    service = OsgiOaiPmhServerInfoRestEndpoint.class,
    property = {
        "service.description=OAI-PMH server REST endpoint",
        "opencast.service.type=org.opencastproject.oaipmhinfo",
        "opencast.service.path=/oaipmhinfo",
        "opencast.service.jobproducer=false"
    }
)
public class OsgiOaiPmhServerInfoRestEndpoint extends AbstractOaiPmhServerInfoRestEndpoint {
  private OaiPmhServerInfo oaiPmhServerInfo;

  @Override public OaiPmhServerInfo getOaiPmhServerInfo() {
    return oaiPmhServerInfo;
  }

  /** OSGi DI. */
  @Reference
  public void setOaiPmhServerInfo(OaiPmhServerInfo oaiPmhServerInfo) {
    this.oaiPmhServerInfo = oaiPmhServerInfo;
  }
}
