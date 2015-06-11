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

package org.opencastproject.metadata.dublincore;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.api.CatalogService;
import org.opencastproject.metadata.api.MediaPackageMetadata;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.metadata.api.MediapackageMetadataImpl;
import org.opencastproject.workspace.api.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses {@link DublinCoreCatalog}s from serialized DC representations.
 */
public class DublinCoreCatalogService implements CatalogService<DublinCoreCatalog>, MediaPackageMetadataService {

  private static final Logger logger = LoggerFactory.getLogger(DublinCoreCatalogService.class);

  protected int priority = 0;

  protected Workspace workspace = null;

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void activate(@SuppressWarnings("unchecked") Map properties) {
    logger.debug("activate()");
    if (properties != null) {
      String priorityString = (String) properties.get(PRIORITY_KEY);
      if (priorityString != null) {
        try {
          priority = Integer.parseInt(priorityString);
        } catch (NumberFormatException e) {
          logger.warn("Unable to set priority to {}", priorityString);
          throw e;
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.api.CatalogService#serialize(org.opencastproject.metadata.api.MetadataCatalog)
   */
  @Override
  public InputStream serialize(DublinCoreCatalog catalog) throws IOException {
    try {
      Transformer tf = TransformerFactory.newInstance().newTransformer();
      DOMSource xmlSource = new DOMSource(catalog.toXml());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      tf.transform(xmlSource, new StreamResult(out));
      return new ByteArrayInputStream(out.toByteArray());
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.api.MetadataService#getMetadata(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackageMetadata getMetadata(MediaPackage mp) {
    MediapackageMetadataImpl metadata = new MediapackageMetadataImpl();

    Catalog[] dcs = mp.getCatalogs(DublinCoreCatalog.ANY_DUBLINCORE);
    for (Catalog catalog : dcs) {
      DublinCoreCatalog dc;
      InputStream in = null;
      try {
        File f = workspace.get(catalog.getURI());
        in = new FileInputStream(f);
        dc = load(in);
      } catch (Exception e) {
        logger.warn("Unable to load metadata from catalog '{}'", catalog);
        continue;
      } finally {
        IOUtils.closeQuietly(in);
      }
      if (MediaPackageElements.EPISODE.equals(catalog.getFlavor())) {
        // Title
        metadata.setTitle(dc.getFirst(DublinCore.PROPERTY_TITLE));

        // Created date
        if (dc.hasValue(DublinCore.PROPERTY_CREATED))
          metadata.setDate(EncodingSchemeUtils.decodeDate(dc.get(DublinCore.PROPERTY_CREATED).get(0)));

        // Series id
        if (dc.hasValue(DublinCore.PROPERTY_IS_PART_OF))
          metadata.setSeriesIdentifier(dc.get(DublinCore.PROPERTY_IS_PART_OF).get(0).getValue());

        // Creator
        if (dc.hasValue(DublinCore.PROPERTY_CREATOR)) {
          List<String> creators = new ArrayList<String>();
          for (DublinCoreValue creator : dc.get(DublinCore.PROPERTY_CREATOR)) {
            creators.add(creator.getValue());
          }
          metadata.setCreators(creators.toArray(new String[creators.size()]));
        }

        // Contributor
        if (dc.hasValue(DublinCore.PROPERTY_CONTRIBUTOR)) {
          List<String> contributors = new ArrayList<String>();
          for (DublinCoreValue contributor : dc.get(DublinCore.PROPERTY_CONTRIBUTOR)) {
            contributors.add(contributor.getValue());
          }
          metadata.setContributors(contributors.toArray(new String[contributors.size()]));
        }

        // Subject
        if (dc.hasValue(DublinCore.PROPERTY_SUBJECT)) {
          List<String> subjects = new ArrayList<String>();
          for (DublinCoreValue subject : dc.get(DublinCore.PROPERTY_SUBJECT)) {
            subjects.add(subject.getValue());
          }
          metadata.setSubjects(subjects.toArray(new String[subjects.size()]));
        }

        // License
        metadata.setLicense(dc.getFirst(DublinCore.PROPERTY_LICENSE));

        // Language
        metadata.setLanguage(dc.getFirst(DublinCore.PROPERTY_LANGUAGE));
      } else if (MediaPackageElements.SERIES.equals(catalog.getFlavor())) {
        // Series Title and Identifier
        metadata.setSeriesTitle(dc.getFirst(DublinCore.PROPERTY_TITLE));
        metadata.setSeriesIdentifier(dc.getFirst(DublinCore.PROPERTY_IDENTIFIER));
      } else if (catalog.getFlavor().getSubtype().startsWith(MediaPackageElements.OAIPMH.getSubtype())) {
        // ignoring OAI-PMH dublincore flavors
      } else {
        logger.warn("Unexpected dublin core catalog flavor found, ignoring '" + catalog.getFlavor() + "'");
      }
    }
    return metadata;
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.api.CatalogService#load(java.io.InputStream)
   */
  @Override
  public DublinCoreCatalog load(InputStream in) throws IOException {
    if (in == null)
      throw new IllegalArgumentException("Stream must not be null");
    return DublinCores.read(in);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.api.CatalogService#accepts(org.opencastproject.mediapackage.Catalog)
   */
  @Override
  public boolean accepts(Catalog catalog) {
    if (catalog == null)
      throw new IllegalArgumentException("Catalog must not be null");
    MediaPackageElementFlavor flavor = catalog.getFlavor();
    return flavor != null && (flavor.equals(DublinCoreCatalog.ANY_DUBLINCORE));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.api.CatalogService#newInstance()
   */
  @Override
  public DublinCoreCatalog newInstance() {
    return DublinCores.mkOpencast();
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.api.MetadataService#getPriority()
   */
  @Override
  public int getPriority() {
    return priority;
  }

}
