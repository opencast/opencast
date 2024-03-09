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
package org.opencastproject.assetmanager.impl.endpoint;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.impl.AssetManagerJobProducer;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.Path;

/** OSGi bound implementation. */
@Path("/assets")
@Component(
    immediate = true,
    service = OsgiAssetManagerRestEndpoint.class,
    property = {
        "service.description=AssetManager REST Endpoint",
        "opencast.service.type=org.opencastproject.assetmanager",
        "opencast.service.path=/assets",
        "opencast.service.jobproducer=true"
    }
)
public class OsgiAssetManagerRestEndpoint extends AbstractTieredStorageAssetManagerRestEndpoint {
  private AssetManager assetManager;

  @Override public AssetManager getAssetManager() {
    return assetManager;
  }

  /** OSGi DI */
  @Reference
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  @Reference
  @Override
  public void setJobProducer(AssetManagerJobProducer assetManagerJobProducer) {
    super.setJobProducer(assetManagerJobProducer);
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

}
