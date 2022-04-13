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

package org.opencastproject.oaipmh.server.remote;

import static org.opencastproject.util.HttpUtil.get;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.oaipmh.server.OaiPmhServerInfo;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.UrlSupport;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component(
    immediate = true,
    service = OaiPmhServerInfo.class,
    property = {
        "service.description=OAI-PMH server info remote proxy"
    }
)
public class OaiPmhServerInfoRemoteImpl extends RemoteBase implements OaiPmhServerInfo {
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhServerInfoRemoteImpl.class);

  public static final String SERVICE_TYPE = "org.opencastproject.oaipmhinfo";

  public OaiPmhServerInfoRemoteImpl() {
    super(SERVICE_TYPE);
  }

  @Override public boolean hasRepo(String id) {
    for (HttpResponse r : option(getResponse(get(UrlSupport.concat("hasrepo", id)), HttpStatus.SC_OK))) {
      try {
        return Boolean.parseBoolean(EntityUtils.toString(r.getEntity()));
      } catch (IOException e) {
        logger.error("Cannot contact remote service", e);
        chuck(e);
      }
    }
    return false;
  }

  @Override public String getMountPoint() {
    for (HttpResponse r : option(getResponse(get("mountpoint"), HttpStatus.SC_OK))) {
      try {
        return EntityUtils.toString(r.getEntity());
      } catch (IOException e) {
        logger.error("Cannot contact remote service", e);
        return chuck(e);
      }
    }
    throw new RuntimeException("Cannot contact remote service");
  }

  @Reference
  @Override
  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    super.setTrustedHttpClient(trustedHttpClient);
  }

  @Reference
  @Override
  public void setRemoteServiceManager(ServiceRegistry serviceRegistry) {
    super.setRemoteServiceManager(serviceRegistry);
  }

}
