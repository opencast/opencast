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
package org.opencastproject.assetmanager.impl.endpoint;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.opencastproject.assetmanager.impl.AbstractAssetManager.getFileNameFromUrn;
import static org.opencastproject.util.MimeTypeUtil.Fns.suffix;
import static org.opencastproject.util.OsgiUtil.getComponentContextProperty;
import static org.opencastproject.util.OsgiUtil.getContextProperty;
import static org.opencastproject.util.UrlSupport.uri;

import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.impl.AbstractAssetManager;
import org.opencastproject.assetmanager.impl.HttpAssetProvider;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Implementation of an {@link HttpAssetProvider} for the {@link OsgiAssetManagerRestEndpoint}.
 * <p>
 * The implementation of the interface needs to be decoupled from the endpoint to avoid
 * a circular dependency between the AssetManager and the endpoint. See HIWEST-492 for details.
 * <p>
 * Because of its tight coupling to the OSGi runtime, the implementation goes without an abstract base class.
 */
public class OsgiEndpointHttpAssetProvider implements HttpAssetProvider {
  private static final Logger logger = LoggerFactory.getLogger(OsgiEndpointHttpAssetProvider.class);

  private String serverUrl;
  private String mountPoint;

  private OrganizationDirectoryService orgDir;

  /** Calculate the server url based on the current organization. */
  private String calcServerUrl(String organizationId) {
    Organization organization = null;
    try {
      organization = orgDir.getOrganization(organizationId);
    } catch (NotFoundException e) {
      logger.warn("No organization found! Using default server url ({})", serverUrl);
      return serverUrl.trim();
    }

    // Get asset manager URL. Default to admin node URL or to server URL
    String orgServerUrl = organization.getProperties().get(MatterhornConstants.ASSET_MANAGER_URL_ORG_PROPERTY);
    if (StringUtils.isBlank(orgServerUrl)) {
      orgServerUrl = organization.getProperties().get(MatterhornConstants.ADMIN_URL_ORG_PROPERTY);
      logger.debug("No asset manager URL for organization '{}'. Falling back to admin node url ({})",
                   organization, orgServerUrl);
    }
    if (StringUtils.isBlank(orgServerUrl)) {
      logger.debug("No admin node URL for organization '{}' set. Falling back to default server url ({})",
                   organization, serverUrl);
      orgServerUrl = serverUrl;
    }
    return orgServerUrl.trim();
  }

  @Override public Snapshot prepareForDelivery(final Snapshot snapshot) {
    return AbstractAssetManager.rewriteUris(snapshot, new Fn<MediaPackageElement, URI>() {
      @Override public URI apply(MediaPackageElement mpe) {
        return createUriFor(mpe, snapshot);
      }
    });
  }

  private URI createUriFor(MediaPackageElement mpe, Snapshot snapshot) {
    String baseName = getBaseName(getFileNameFromUrn(mpe).getOr(mpe.getElementType().toString()));

    // the returned uri must match the path of the {@link #getAsset} method
    return uri(calcServerUrl(snapshot.getOrganizationId().toString()),
               mountPoint,
               "assets",
               mpe.getMediaPackage().getIdentifier().toString(),
               mpe.getIdentifier(),
               snapshot.getVersion().toString(),
               baseName + "." + mimeTypeToSuffix(Opt.nul(mpe.getMimeType())));
  }

  /** Get a file name suffix for the given MIME type. */
  private static String mimeTypeToSuffix(Opt<MimeType> t) {
    return t.bind(suffix).getOr("unknown");
  }

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    serverUrl = getContextProperty(cc, "org.opencastproject.server.url");
    mountPoint = getComponentContextProperty(cc, "assetmanager.service.path");
  }

  /** OSGi DI */
  public void setOrgDir(OrganizationDirectoryService orgDir) {
    this.orgDir = orgDir;
  }
}
