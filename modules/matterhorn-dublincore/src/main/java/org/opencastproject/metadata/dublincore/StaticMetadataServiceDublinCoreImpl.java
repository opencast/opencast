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
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.workspace.api.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_ACCESS_RIGHTS;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_AVAILABLE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CONTRIBUTOR;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATOR;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_DESCRIPTION;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_EXTENT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IS_PART_OF;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LANGUAGE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LICENSE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_PUBLISHER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_REPLACES;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_RIGHTS_HOLDER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SPATIAL;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SUBJECT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TYPE;
import static org.opencastproject.util.data.Collections.head;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Monadics.mlist;
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
    return mlist(list(mp.getCatalogs(DublinCoreCatalog.ANY_DUBLINCORE)))
            .find(flavorPredicate(MediaPackageElements.EPISODE))
            .flatMap(loader)
            .map(new Function<DublinCoreCatalog, StaticMetadata>() {
              @Override
              public StaticMetadata apply(DublinCoreCatalog episode) {
                return newStaticMetadataFromEpisode(episode);
              }
            })
            .getOrElse((StaticMetadata) null);
  }

  private static StaticMetadata newStaticMetadataFromEpisode(DublinCoreCatalog episode) {
    // Ensure that the mandatory properties are present
    final Option<String> id = option(episode.getFirst(PROPERTY_IDENTIFIER));
    final Option<Date> created = option(episode.getFirst(PROPERTY_CREATED)).map(new Function<String, Date>() {
      @Override
      public Date apply(String a) {
        final Date date = EncodingSchemeUtils.decodeDate(a);
        return date != null ? date : Misc.<Date>chuck(new RuntimeException(a + " does not conform to W3C-DTF encoding scheme."));
      }
    });
    final Option<String> language = option(episode.getFirst(PROPERTY_LANGUAGE));
    final Option<Long> extent = head(episode.get(PROPERTY_EXTENT)).map(new Function<DublinCoreValue, Long>() {
      @Override
      public Long apply(DublinCoreValue a) {
        final Long extent = EncodingSchemeUtils.decodeDuration(a);
        return extent != null ? extent : Misc.<Long>chuck(new RuntimeException(a + " does not conform to ISO8601 encoding scheme for durations."));
      }
    });
    final Option<String> type = option(episode.getFirst(PROPERTY_TYPE));

    final Option<String> isPartOf = option(episode.getFirst(PROPERTY_IS_PART_OF));
    final Option<String> replaces = option(episode.getFirst(PROPERTY_REPLACES));
    final Option<Interval> available = head(episode.get(PROPERTY_AVAILABLE)).flatMap(
            new Function<DublinCoreValue, Option<Interval>>() {
              @Override
              public Option<Interval> apply(DublinCoreValue v) {
                final DCMIPeriod p = EncodingSchemeUtils.decodePeriod(v);
                return p != null
                        ? some(Interval.fromValues(p.getStart(), p.getEnd()))
                        : Misc.<Option<Interval>>chuck(new RuntimeException(v + " does not conform to W3C-DTF encoding scheme for periods"));
              }
            });
    final NonEmptyList<MetadataValue<String>> titles = new NonEmptyList<MetadataValue<String>>(
            mlist(episode.get(PROPERTY_TITLE)).map(dc2mvString(PROPERTY_TITLE.getLocalName())).value());
    final List<MetadataValue<String>> subjects =
            mlist(episode.get(PROPERTY_SUBJECT)).map(dc2mvString(PROPERTY_SUBJECT.getLocalName())).value();
    final List<MetadataValue<String>> creators =
            mlist(episode.get(PROPERTY_CREATOR)).map(dc2mvString(PROPERTY_CREATOR.getLocalName())).value();
    final List<MetadataValue<String>> publishers =
            mlist(episode.get(PROPERTY_PUBLISHER)).map(dc2mvString(PROPERTY_PUBLISHER.getLocalName())).value();
    final List<MetadataValue<String>> contributors =
            mlist(episode.get(PROPERTY_CONTRIBUTOR)).map(dc2mvString(PROPERTY_CONTRIBUTOR.getLocalName())).value();
    final List<MetadataValue<String>> description =
            mlist(episode.get(PROPERTY_DESCRIPTION)).map(dc2mvString(PROPERTY_DESCRIPTION.getLocalName())).value();
    final List<MetadataValue<String>> rightsHolders =
            mlist(episode.get(PROPERTY_RIGHTS_HOLDER)).map(dc2mvString(PROPERTY_RIGHTS_HOLDER.getLocalName())).value();
    final List<MetadataValue<String>> spatials =
            mlist(episode.get(PROPERTY_SPATIAL)).map(dc2mvString(PROPERTY_SPATIAL.getLocalName())).value();
    final List<MetadataValue<String>> accessRights =
            mlist(episode.get(PROPERTY_ACCESS_RIGHTS)).map(dc2mvString(PROPERTY_ACCESS_RIGHTS.getLocalName())).value();
    final List<MetadataValue<String>> licenses =
            mlist(episode.get(PROPERTY_LICENSE)).map(dc2mvString(PROPERTY_LICENSE.getLocalName())).value();

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
