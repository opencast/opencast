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

package org.opencastproject.index.service.catalog.adapter.events;

import org.opencastproject.index.service.catalog.adapter.ConfigurableDCCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.DublinCoreMetadataUtil;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.util.IoSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Managed service implementation of a AbstractEventsCatalogUIAdapter
 */
public class ConfigurableEventDCCatalogUIAdapter extends ConfigurableDCCatalogUIAdapter
        implements EventCatalogUIAdapter {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurableEventDCCatalogUIAdapter.class);

  private Workspace workspace;

  @Override
  public DublinCoreMetadataCollection getFields(MediaPackage mediapackage) {
    List<DublinCoreCatalog> dcCatalogs = Arrays.stream(mediapackage.getCatalogs(flavor))
            .map(catalog -> DublinCoreUtil.loadDublinCore(getWorkspace(), catalog))
            .collect(Collectors.toList());

    return getFieldsFromCatalogs(dcCatalogs);
  }

  @Override
  public Catalog storeFields(MediaPackage mediaPackage, DublinCoreMetadataCollection abstractMetadata) {
    final Catalog catalog;
    final DublinCoreCatalog dc;
    final String filename;

    Catalog[] catalogs = mediaPackage.getCatalogs(flavor);
    if (catalogs.length == 0) {
      catalog = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .newElement(org.opencastproject.mediapackage.MediaPackageElement.Type.Catalog, getFlavor());
      catalog.setIdentifier(UUID.randomUUID().toString());
      mediaPackage.add(catalog);

      dc = DublinCores.mkSimple();
      dc.addBindings(config.getXmlNamespaceContext());
      dc.setRootTag(new EName(config.getCatalogXmlRootNamespace(), config.getCatalogXmlRootElementName()));
      filename = "dublincore.xml";
    } else {
      catalog = catalogs[0];
      dc = DublinCoreUtil.loadDublinCore(getWorkspace(), catalog);
      dc.addBindings(config.getXmlNamespaceContext());
      filename = FilenameUtils.getName(catalog.getURI().toString());
    }

    DublinCoreMetadataUtil.updateDublincoreCatalog(dc, abstractMetadata);

    URI uri;
    InputStream inputStream = null;
    try {
      inputStream = IOUtils.toInputStream(dc.toXmlString(), "UTF-8");
      uri = getWorkspace().put(mediaPackage.getIdentifier().toString(), catalog.getIdentifier(), filename, inputStream);
      catalog.setURI(uri);
      // setting the URI to a new source so the checksum will most like be invalid
      catalog.setChecksum(null);
    } catch (IOException e) {
      logger.error("Unable to store catalog {} metadata to workspace", catalog, e);
    } finally {
      IoSupport.closeQuietly(inputStream);
    }
    return catalog;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  protected Workspace getWorkspace() {
    return workspace;
  }
}
