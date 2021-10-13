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

import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.api.CatalogService;
import org.opencastproject.metadata.api.MediaPackageMetadata;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.metadata.api.MediapackageMetadataImpl;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Parses {@link DublinCoreCatalog}s from serialized DC representations.
 */
@Component(
  property = {
    "service.description=Dublin Core Catalog Service",
    "priority=1"
  },
  immediate = true,
  service = { CatalogService.class, MediaPackageMetadataService.class, DublinCoreCatalogService.class }
)
public class DublinCoreCatalogService implements CatalogService<DublinCoreCatalog>, MediaPackageMetadataService {

  private static final Logger logger = LoggerFactory.getLogger(DublinCoreCatalogService.class);

  protected int priority = 0;

  protected Workspace workspace = null;

  @Reference(name = "workspace")
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Activate
  public void activate(Map<String, ?> properties) {
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

  public InputStream serialize(DublinCoreCatalog catalog) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    catalog.toXml(out, true);
    return new ByteArrayInputStream(out.toByteArray());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.api.MetadataService#getMetadata(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackageMetadata getMetadata(MediaPackage mp) {
    MediapackageMetadataImpl metadata = new MediapackageMetadataImpl();
    for (Catalog catalog : Stream.$(mp.getCatalogs(DublinCoreCatalog.ANY_DUBLINCORE)).sort(COMPARE_BY_FLAVOR)) {
      DublinCoreCatalog dc = DublinCoreUtil.loadDublinCore(workspace, catalog);
      if (MediaPackageElements.EPISODE.equals(catalog.getFlavor())) {
        // Title
        metadata.setTitle(dc.getFirst(DublinCore.PROPERTY_TITLE));

        // use started date as created date (see MH-12250)
        if (dc.hasValue(DublinCore.PROPERTY_TEMPORAL) && dc.getFirst(PROPERTY_TEMPORAL) != null) {
          DCMIPeriod period = EncodingSchemeUtils
            .decodeMandatoryPeriod(dc.getFirst(PROPERTY_TEMPORAL));
          metadata.setDate(period.getStart());
        } else {
          // ...and only if started date is not available the created date
          if (dc.hasValue(DublinCore.PROPERTY_CREATED))
            metadata.setDate(EncodingSchemeUtils.decodeDate(dc.get(DublinCore.PROPERTY_CREATED).get(0)));
        }
        // Series id
        if (dc.hasValue(DublinCore.PROPERTY_IS_PART_OF))
          metadata.setSeriesIdentifier(dc.get(DublinCore.PROPERTY_IS_PART_OF).get(0).getValue());

        // Creator
        if (dc.hasValue(DublinCore.PROPERTY_CREATOR)) {
          List<String> creators = new ArrayList<String>();
          for (DublinCoreValue creator : dc.get(DublinCore.PROPERTY_CREATOR)) {
            creators.add(creator.getValue());
          }
          metadata.setCreators(creators.toArray(new String[0]));
        }

        // Contributor
        if (dc.hasValue(DublinCore.PROPERTY_CONTRIBUTOR)) {
          List<String> contributors = new ArrayList<>();
          for (DublinCoreValue contributor : dc.get(DublinCore.PROPERTY_CONTRIBUTOR)) {
            contributors.add(contributor.getValue());
          }
          metadata.setContributors(contributors.toArray(new String[0]));
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
      } else {
        logger.debug("Excluding unknown catalog flavor '{}' from the top level metadata of mediapackage '{}'",
                catalog.getFlavor(), mp.getIdentifier());
      }
    }
    return metadata;
  }

  public static final Comparator<Catalog> COMPARE_BY_FLAVOR = new Comparator<Catalog>() {
    @Override
    public int compare(Catalog c1, Catalog c2) {
      if (MediaPackageElements.EPISODE.equals(c1.getFlavor()))
        return 1;
      return -1;
    }
  };

  public DublinCoreCatalog load(InputStream in) throws IOException {
    if (in == null)
      throw new IllegalArgumentException("Stream must not be null");
    return DublinCores.read(in);
  }

  public DublinCoreCatalog newInstance() {
    return DublinCores.mkOpencastEpisode().getCatalog();
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
