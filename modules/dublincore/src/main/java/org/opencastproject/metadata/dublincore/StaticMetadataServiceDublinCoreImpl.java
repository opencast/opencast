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

package org.opencastproject.metadata.dublincore;

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
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TYPE;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.metadata.api.MetadataValue;
import org.opencastproject.metadata.api.StaticMetadata;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.api.util.Interval;
import org.opencastproject.util.data.NonEmptyList;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This service provides {@link org.opencastproject.metadata.api.StaticMetadata} for a given mediapackage,
 * based on a contained dublin core catalog describing the episode.
 */
@Component(
    immediate = true,
    service = StaticMetadataService.class,
    property = {
        "service.description=Static Metadata Service, dublin core based",
        "metadata.source=dublincore",
        "priority=1"
    }
)
public class StaticMetadataServiceDublinCoreImpl implements StaticMetadataService {

  private static final Logger logger = LoggerFactory.getLogger(StaticMetadataServiceDublinCoreImpl.class);

  protected int priority = 0;

  protected Workspace workspace = null;

  protected MediaPackageSerializer serializer = null;

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference(
      cardinality = ReferenceCardinality.OPTIONAL,
      policy = ReferencePolicy.DYNAMIC,
      target = "(service.pid=org.opencastproject.mediapackage.ChainingMediaPackageSerializer)",
      unbind = "unsetMediaPackageSerializer"
  )
  public void setMediaPackageSerializer(MediaPackageSerializer serializer) {
    this.serializer = serializer;
  }

  public void unsetMediaPackageSerializer(MediaPackageSerializer serializer) {
    if (this.serializer == serializer) {
      this.serializer = null;
    }
  }

  @Activate
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
    Catalog[] catalogs = mp.getCatalogs(MediaPackageElements.EPISODE);
    if (catalogs.length > 0) {
      return newStaticMetadataFromEpisode(DublinCoreUtil.loadDublinCore(workspace, catalogs[0]));
    }
    return null;
  }

  private static StaticMetadata newStaticMetadataFromEpisode(DublinCoreCatalog episode) {
    // Ensure that the mandatory properties are present
    final Optional<String> id = Optional.ofNullable(episode.getFirst(PROPERTY_IDENTIFIER));
    final Optional<Date> created = Optional.ofNullable(episode.getFirst(PROPERTY_CREATED))
        .map(a -> {
          Date date = EncodingSchemeUtils.decodeDate(a);
          if (date == null) {
            throw new RuntimeException(a + " does not conform to W3C-DTF encoding scheme.");
          }
          return date;
        });
    final Optional temporalOpt = Optional.ofNullable(episode.getFirstVal(PROPERTY_TEMPORAL))
        .map(dc2temporalValueOption());
    final Optional<Date> start;
    if (episode.getFirst(PROPERTY_TEMPORAL) != null) {
      DCMIPeriod period = EncodingSchemeUtils
                  .decodeMandatoryPeriod(episode.getFirst(PROPERTY_TEMPORAL));
      start = Optional.ofNullable(period.getStart());
    } else {
      start = created;
    }
    final Optional<String> language = Optional.ofNullable(episode.getFirst(PROPERTY_LANGUAGE));
    final Optional<Long> extent = episode.get(PROPERTY_EXTENT).stream().findFirst()
        .map(a -> {
          Long duration = EncodingSchemeUtils.decodeDuration(a);
          if (duration == null) {
            throw new RuntimeException(a + " does not conform to ISO8601 encoding scheme for durations.");
          }
          return duration;
        });
    final Optional<String> type = Optional.ofNullable(episode.getFirst(PROPERTY_TYPE));

    final Optional<String> isPartOf = Optional.ofNullable(episode.getFirst(PROPERTY_IS_PART_OF));
    final Optional<String> replaces = Optional.ofNullable(episode.getFirst(PROPERTY_REPLACES));
    final Optional<Interval> available = episode.get(PROPERTY_AVAILABLE).stream().findFirst()
        .flatMap(v -> {
          DCMIPeriod p = EncodingSchemeUtils.decodePeriod(v);
          if (p == null) {
            throw new RuntimeException(v + " does not conform to W3C-DTF encoding scheme for periods");
          }
          return Optional.of(Interval.fromValues(p.getStart(), p.getEnd()));
        });
    final NonEmptyList<MetadataValue<String>> titles = new NonEmptyList<>(
        episode.get(PROPERTY_TITLE).stream()
            .map(dc2mvString(PROPERTY_TITLE.getLocalName()))
            .collect(Collectors.toList())
    );
    final List<MetadataValue<String>> subjects = episode.get(PROPERTY_SUBJECT).stream()
            .map(dc2mvString(PROPERTY_SUBJECT.getLocalName()))
            .collect(Collectors.toList());
    final List<MetadataValue<String>> creators = episode.get(PROPERTY_CREATOR).stream()
            .map(dc2mvString(PROPERTY_CREATOR.getLocalName()))
            .collect(Collectors.toList());
    final List<MetadataValue<String>> publishers = episode.get(PROPERTY_PUBLISHER).stream()
            .map(dc2mvString(PROPERTY_PUBLISHER.getLocalName()))
            .collect(Collectors.toList());
    final List<MetadataValue<String>> contributors = episode.get(PROPERTY_CONTRIBUTOR).stream()
            .map(dc2mvString(PROPERTY_CONTRIBUTOR.getLocalName()))
            .collect(Collectors.toList());
    final List<MetadataValue<String>> description = episode.get(PROPERTY_DESCRIPTION).stream()
            .map(dc2mvString(PROPERTY_DESCRIPTION.getLocalName()))
            .collect(Collectors.toList());
    final List<MetadataValue<String>> rightsHolders = episode.get(PROPERTY_RIGHTS_HOLDER).stream()
            .map(dc2mvString(PROPERTY_RIGHTS_HOLDER.getLocalName()))
            .collect(Collectors.toList());
    final List<MetadataValue<String>> spatials = episode.get(PROPERTY_SPATIAL).stream()
            .map(dc2mvString(PROPERTY_SPATIAL.getLocalName()))
            .collect(Collectors.toList());
    final List<MetadataValue<String>> accessRights = episode.get(PROPERTY_ACCESS_RIGHTS).stream()
            .map(dc2mvString(PROPERTY_ACCESS_RIGHTS.getLocalName()))
            .collect(Collectors.toList());
    final List<MetadataValue<String>> licenses = episode.get(PROPERTY_LICENSE).stream()
            .map(dc2mvString(PROPERTY_LICENSE.getLocalName()))
            .collect(Collectors.toList());

    return new StaticMetadata() {
      @Override
      public Optional<String> getId() {
        return id;
      }

      @Override
      public Optional<Date[]> getTemporalPeriod() {
        if (temporalOpt.isPresent()) {
          if (temporalOpt.get() instanceof DCMIPeriod) {
            DCMIPeriod p = (DCMIPeriod) temporalOpt.get();
            return Optional.ofNullable(new Date[] { p.getStart(), p.getEnd() });
          }
        }
        return Optional.empty();
      }

      @Override
      public Optional<Date> getTemporalInstant() {
        if (temporalOpt.isPresent()) {
          if (temporalOpt.get() instanceof Date) {
            return temporalOpt;
          }
        }
        return Optional.empty();
      }

      @Override
      public Optional<Long> getTemporalDuration() {
        if (temporalOpt.isPresent()) {
          if (temporalOpt.get() instanceof Long) {
            return temporalOpt;
          }
        }
        return Optional.empty();
      }

      @Override
      public Optional<Long> getExtent() {
        return extent;
      }

      @Override
      public Optional<String> getLanguage() {
        return language;
      }

      @Override
      public Optional<String> getIsPartOf() {
        return isPartOf;
      }

      @Override
      public Optional<String> getReplaces() {
        return replaces;
      }

      @Override
      public Optional<String> getType() {
        return type;
      }

      @Override
      public Optional<Interval> getAvailable() {
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
   * Return a function that creates a Option with the value of temporal from a DublinCoreValue.
   */
  private static java.util.function.Function<DublinCoreValue, Object> dc2temporalValueOption() {
    return dcv -> {
      Temporal temporal = EncodingSchemeUtils.decodeTemporal(dcv);
      if (temporal == null) {
        throw new RuntimeException(dcv
            + " does not conform to ISO8601 encoding scheme for temporal.");
      }
      return temporal.fold(new Temporal.Match<Object>() {
        @Override
        public Object period(DCMIPeriod period) {
          return period;
        }

        @Override
        public Object instant(Date instant) {
          return instant;
        }

        @Override
        public Object duration(long duration) {
          return duration;
        }
      });
    };
  }

  /**
   * Return a function that creates a MetadataValue[String] from a DublinCoreValue setting its name to
   * <code>name</code>.
   */
  private static Function<DublinCoreValue, MetadataValue<String>> dc2mvString(final String name) {
    return dcv -> new MetadataValue<>(dcv.getValue(), name, dcv.getLanguage());
  }

  private Optional<DublinCoreCatalog> load(Catalog catalog) {
    InputStream in = null;
    try {
      URI uri = catalog.getURI();
      if (serializer != null) {
        uri = serializer.decodeURI(uri);
      }
      in = workspace.read(uri);
      return Optional.of((DublinCoreCatalog) DublinCores.read(in));
    } catch (Exception e) {
      logger.warn("Unable to load metadata from catalog '{}'", catalog);
      return Optional.empty();
    } finally {
      IOUtils.closeQuietly(in);
    }
  }
}
