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
package org.opencastproject.metadata.dublincore;

import org.apache.commons.io.IOUtils;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.api.MetadataValue;
import org.opencastproject.metadata.api.StaticMetadata;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.api.util.Interval;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.NonEmptyList;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Predicate;
import org.opencastproject.workspace.api.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.opencastproject.util.data.Collections.find;
import static org.opencastproject.util.data.Collections.head;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

/**
 * This service provides {@link org.opencastproject.metadata.api.StaticMetadata} for a given mediapackage,
 * based on a contained dublin core catalog describing the episode.
 */
public class StaticMetadataServiceDublinCoreImpl implements StaticMetadataService {

  private static final Logger logger = LoggerFactory.getLogger(StaticMetadataServiceDublinCoreImpl.class);

  // Catalog loader function
  private Function<Catalog, Option<DublinCoreCatalog>> loader = new Function<Catalog, Option<DublinCoreCatalog>>() {
    @Override
    public Option<DublinCoreCatalog> apply(Catalog catalog) {
      return load(catalog);
    }
  };

  protected int priority = 0;

  protected Workspace workspace = null;

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void activate(@SuppressWarnings("rawtypes") Map properties) {
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
   * @see org.opencastproject.metadata.api.MetadataService#getMetadata(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public StaticMetadata getMetadata(final MediaPackage mp) {
    List<Catalog> dcs = Arrays.asList(mp.getCatalogs(DublinCoreCatalog.ANY_DUBLINCORE));
    return find(dcs, flavorPredicate(MediaPackageElements.EPISODE))
            .flatMap(loader)
            .map(new Function<DublinCoreCatalog, StaticMetadata>() {
              @Override
              public StaticMetadata apply(DublinCoreCatalog episode) {
                return newStaticMetadataFromEpisode(episode);
              }
            }).getOrElse((StaticMetadata) null);
  }

  private static StaticMetadata newStaticMetadataFromEpisode(DublinCoreCatalog episode) {
    // Ensure that the mandatory properties are present
    final Option<String> id = option(episode.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    final Option<Date> created = option(episode.getFirst(DublinCore.PROPERTY_CREATED)).map(new Function<String, Date>() {
      @Override
      public Date apply(String a) {
        return EncodingSchemeUtils.decodeDate(a);
      }
    });
    final Option<String> language = option(episode.getFirst(DublinCore.PROPERTY_LANGUAGE));
    final Option<Long> extent = head(episode.get(DublinCore.PROPERTY_EXTENT)).map(new Function<DublinCoreValue, Long>() {
      @Override
      public Long apply(DublinCoreValue a) {
        return EncodingSchemeUtils.decodeDuration(a);
      }
    });
    final Option<String> type = option(episode.getFirst(DublinCore.PROPERTY_TYPE));

    final Option<String> isPartOf = option(episode.getFirst(DublinCore.PROPERTY_IS_PART_OF));
    final Option<String> replaces = option(episode.getFirst(DublinCore.PROPERTY_REPLACES));
    final Option<Interval> available = head(episode.get(DublinCore.PROPERTY_AVAILABLE)).flatMap(
            new Function<DublinCoreValue, Option<Interval>>() {
              @Override
              public Option<Interval> apply(DublinCoreValue v) {
                return option(EncodingSchemeUtils.decodePeriod(v)).map(new Function<DCMIPeriod, Interval>() {
                  @Override
                  public Interval apply(DCMIPeriod p) {
                    return Interval.fromValues(p.getStart(), p.getEnd());
                  }
                });
              }
            });
    final NonEmptyList<MetadataValue<String>> titles = new NonEmptyList<MetadataValue<String>>(map(
            episode.get(DublinCore.PROPERTY_TITLE), dc2mvString(DublinCore.PROPERTY_TITLE.getLocalName())));
    final List<MetadataValue<String>> subjects = (List<MetadataValue<String>>) map(
            episode.get(DublinCore.PROPERTY_SUBJECT), dc2mvString(DublinCore.PROPERTY_SUBJECT.getLocalName()));
    final List<MetadataValue<String>> creators = (List<MetadataValue<String>>) map(
            episode.get(DublinCore.PROPERTY_CREATOR), dc2mvString(DublinCore.PROPERTY_CREATOR.getLocalName()));
    final List<MetadataValue<String>> publishers = (List<MetadataValue<String>>) map(
            episode.get(DublinCore.PROPERTY_PUBLISHER), dc2mvString(DublinCore.PROPERTY_PUBLISHER.getLocalName()));
    final List<MetadataValue<String>> contributors = (List<MetadataValue<String>>) map(
            episode.get(DublinCore.PROPERTY_CONTRIBUTOR), dc2mvString(DublinCore.PROPERTY_CONTRIBUTOR.getLocalName()));
    final List<MetadataValue<String>> description = (List<MetadataValue<String>>) map(
            episode.get(DublinCore.PROPERTY_DESCRIPTION), dc2mvString(DublinCore.PROPERTY_DESCRIPTION.getLocalName()));
    final List<MetadataValue<String>> rightsHolders = (List<MetadataValue<String>>) map(
            episode.get(DublinCore.PROPERTY_RIGHTS_HOLDER),
            dc2mvString(DublinCore.PROPERTY_RIGHTS_HOLDER.getLocalName()));
    final List<MetadataValue<String>> spatials = (List<MetadataValue<String>>) map(
            episode.get(DublinCore.PROPERTY_SPATIAL), dc2mvString(DublinCore.PROPERTY_SPATIAL.getLocalName()));
    final List<MetadataValue<String>> accessRights = (List<MetadataValue<String>>) map(
            episode.get(DublinCore.PROPERTY_ACCESS_RIGHTS),
            dc2mvString(DublinCore.PROPERTY_ACCESS_RIGHTS.getLocalName()));
    final List<MetadataValue<String>> licenses = (List<MetadataValue<String>>) map(
            episode.get(DublinCore.PROPERTY_LICENSE), dc2mvString(DublinCore.PROPERTY_LICENSE.getLocalName()));

    return new StaticMetadata() {
      @Override
      public Option<String> getId() {
        return id;
      }

      @Override
      public Option<Date> getCreated() {
        return created;
      }

      @Override
      public Option<Long> getExtent() {
        return extent;
      }

      @Override
      public Option<String> getLanguage() {
        return language;
      }

      @Override
      public Option<String> getIsPartOf() {
        return isPartOf;
      }

      @Override
      public Option<String> getReplaces() {
        return replaces;
      }

      @Override
      public Option<String> getType() {
        return type;
      }

      @Override
      public Option<Interval> getAvailable() {
        return available;
      }

      @Override
      public NonEmptyList<MetadataValue<String>> getTitles() {
        return titles;
      }

      @Override
      public List<MetadataValue<String>> getSubjects() {
        return subjects;
      }

      @Override
      public List<MetadataValue<String>> getCreators() {
        return creators;
      }

      @Override
      public List<MetadataValue<String>> getPublishers() {
        return publishers;
      }

      @Override
      public List<MetadataValue<String>> getContributors() {
        return contributors;
      }

      @Override
      public List<MetadataValue<String>> getDescription() {
        return description;
      }

      @Override
      public List<MetadataValue<String>> getRightsHolders() {
        return rightsHolders;
      }

      @Override
      public List<MetadataValue<String>> getSpatials() {
        return spatials;
      }

      @Override
      public List<MetadataValue<String>> getAccessRights() {
        return accessRights;
      }

      @Override
      public List<MetadataValue<String>> getLicenses() {
        return licenses;
      }
    };
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

  /**
   * Return a function that creates a MetadataValue[String] from a DublinCoreValue setting its name to <code>name</code>.
   */
  private static Function<DublinCoreValue, MetadataValue<String>> dc2mvString(final String name) {
    return new Function<DublinCoreValue, MetadataValue<String>>() {
      @Override
      public MetadataValue<String> apply(DublinCoreValue dcv) {
        return new MetadataValue<String>(dcv.getValue(), name, dcv.getLanguage());
      }
    };
  }

  private static Predicate<Catalog> flavorPredicate(final MediaPackageElementFlavor flavor) {
    return new Predicate<Catalog>() {
      @Override
      public Boolean apply(Catalog catalog) {
        return flavor.equals(catalog.getFlavor());
      }
    };
  }

  private Option<DublinCoreCatalog> load(Catalog catalog) {
    InputStream in = null;
    try {
      File f = workspace.get(catalog.getURI());
      in = new FileInputStream(f);
      return some((DublinCoreCatalog) new DublinCoreCatalogImpl(in));
    } catch (Exception e) {
      logger.warn("Unable to load metadata from catalog '{}'", catalog);
      return Option.none();
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
