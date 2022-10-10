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

package org.opencastproject.tobira.impl;

import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_DESCRIPTION;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.series.api.Series;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.MimeType;
import org.opencastproject.workspace.api.Workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * A item of the harvesting API, basically as a JSON object. Can be "event", "series",
 * "event-deleted" or "series-deleted". Also contains the modified date, used for sorting.
 */
class Item {
  private static final Logger logger = LoggerFactory.getLogger(Item.class);

  private Date modifiedDate;
  private Jsons.Val obj;

  /** Converts a event into the corresponding JSON representation */
  Item(SearchResultItem event, Workspace workspace) {
    this.modifiedDate = event.getModified();

    if (event.getDeletionDate() != null) {
      this.obj = Jsons.obj(
          Jsons.p("kind", "event-deleted"),
          Jsons.p("id", event.getId()),
          Jsons.p("updated", event.getModified().getTime())
      );
    } else {
      final var mp = event.getMediaPackage();
      final var dccs = getDccsFromMp(mp, workspace);

      // Figure out whether this is a live event
      final var isLive = Arrays.stream(mp.getTracks()).anyMatch(track -> track.isLive());

      // Obtain creators. We first try to obtain it from the DCCs. We collect
      // into `LinkedHashSet` to deduplicate entries.
      final var creators = dccs.stream()
              .flatMap(dcc -> dcc.get(DublinCore.PROPERTY_CREATOR).stream())
              .filter(Objects::nonNull)
              .map(creator -> Jsons.v(creator.getValue()))
              .collect(Collectors.toCollection(LinkedHashSet::new));

      // Get start and end time
      final var period = dccs.stream()
              .map(dcc -> dcc.getFirst(DublinCore.PROPERTY_TEMPORAL))
              .filter(Objects::nonNull)
              .findFirst()
              .flatMap(str -> {
                try {
                  return Optional.of(EncodingSchemeUtils.decodeMandatoryPeriod(str));
                } catch (Exception e) {
                  return Optional.empty();
                }
              });

      // Get title. We require a title and will consult all three sources for it, in decreasing
      // order of trust in that source.
      var title = dccs.stream()
              .map(dcc -> dcc.getFirst(DublinCore.PROPERTY_TITLE))
              .filter(Objects::nonNull)
              .findFirst()
              .orElse(mp.getTitle());
      if (title == null) {
        title = event.getDcTitle();
      }
      if (title == null) {
        // If there is no title to be found, we throw an exception to skip this event.
        throw new RuntimeException("Event has no title");
      }


      this.obj = Jsons.obj(
          Jsons.p("kind", "event"),
          Jsons.p("id", event.getId()),
          Jsons.p("title", title),
          Jsons.p("partOf", event.getDcIsPartOf()),
          Jsons.p("description", event.getDcDescription()),
          Jsons.p("created", event.getDcCreated().getTime()),
          Jsons.p("startTime", period.map(p -> p.getStart().getTime()).orElse(null)),
          Jsons.p("endTime", period.map(p -> p.getEnd().getTime()).orElse(null)),
          Jsons.p("creators", Jsons.arr(new ArrayList<>(creators))),
          Jsons.p("duration", Math.max(0, event.getDcExtent())),
          Jsons.p("thumbnail", findThumbnail(mp)),
          Jsons.p("timelinePreview", findTimelinePreview(mp)),
          Jsons.p("tracks", Jsons.arr(assembleTracks(event, mp))),
          Jsons.p("acl", assembleAcl(event.getAccessControlList())),
          Jsons.p("isLive", isLive),
          Jsons.p("metadata", dccToMetadata(dccs)),
          Jsons.p("updated", event.getModified().getTime())
      );
    }
  }

  private static List<DublinCoreCatalog> getDccsFromMp(MediaPackage mp, Workspace workspace) {
    return Arrays.stream(mp.getElements())
            .filter(mpe -> {
              final var flavor = mpe.getFlavor();
              if (flavor == null) {
                return false;
              }
              final var isForEpisode = Objects.equals(flavor.getSubtype(), "episode");
              final var isCatalog = Objects.equals(mpe.getElementType(), MediaPackageElement.Type.Catalog);
              final var isXml = Objects.equals(mpe.getMimeType(), MimeType.mimeType("text", "xml"));
              return isCatalog && isForEpisode && isXml;
            })
            .map(mpe -> DublinCoreUtil.loadDublinCore(workspace, mpe))
            .collect(Collectors.toCollection(ArrayList::new));
  }

  /** Assembles the object containing all additional metadata. */
  private static Jsons.Obj dccToMetadata(List<DublinCoreCatalog> dccs) {
    /** Metadata fields from dcterms that we already handle elsewhere. Therefore, we don't need to
      * include them here again. */
    final var ignoredDcFields = Set.of(new String[] {
        "created", "creator", "title", "extent", "isPartOf", "description", "identifier",
    });


    final var namespaces = new HashMap<String, ArrayList<Jsons.Prop>>();

    for (final var dcc : (Iterable<DublinCoreCatalog>) dccs::iterator) {
      for (final var e : dcc.getValues().entrySet()) {
        final var key = e.getKey();

        // We special case dcterms here to get a smaller, easier to read JSON. In most cases, this
        // will be the only namespace.
        final var ns = key.getNamespaceURI().equals("http://purl.org/dc/terms/")
            ? "dcterms"
            : key.getNamespaceURI();
        final var fields = namespaces.computeIfAbsent(ns, k -> new ArrayList<>());

        // We skip fields that we already include elsewhere.
        if (ns.equals("dcterms") && ignoredDcFields.contains(key.getLocalName())) {
          continue;
        }

        final var values = e.getValue().stream()
            .map(v -> Jsons.v(v.getValue()))
            .collect(Collectors.toCollection(ArrayList::new));
        final var field = Jsons.p(e.getKey().getLocalName(), Jsons.arr(values));
        fields.add(field);
      }
    }

    final var fields = namespaces.entrySet().stream()
        .map(e -> {
          final var obj = Jsons.obj(e.getValue().toArray(new Jsons.Prop[0]));
          return Jsons.p(e.getKey(), obj);
        })
        .toArray(Jsons.Prop[]::new);

    return Jsons.obj(fields);
  }

  private static Jsons.Obj assembleAcl(AccessControlList acl) {
    final var canReadRoles = new ArrayList<Jsons.Val>();
    final var canWriteRoles = new ArrayList<Jsons.Val>();
    for (final var entry: acl.getEntries()) {
      if (entry.getAction().equals(Permissions.Action.READ.toString())) {
        canReadRoles.add(Jsons.v(entry.getRole()));
      } else if (entry.getAction().equals(Permissions.Action.WRITE.toString())) {
        canWriteRoles.add(Jsons.v(entry.getRole()));
      }
    }
    return Jsons.obj(
        Jsons.p("read", Jsons.arr(canReadRoles)),
        Jsons.p("write", Jsons.arr(canWriteRoles))
    );
  }

  private static List<Jsons.Val> assembleTracks(SearchResultItem event, MediaPackage mp) {
    return Arrays.stream(mp.getTracks())
        .map(track -> {
          var videoStreams = TrackSupport.byType(track.getStreams(), VideoStream.class);
          var resolution = Jsons.NULL;
          if (videoStreams.length > 0) {
            final var stream = videoStreams[0];
            resolution = Jsons.arr(Jsons.v(stream.getFrameWidth()), Jsons.v(stream.getFrameHeight()));

            if (videoStreams.length > 1) {
              logger.warn(
                  "Track of event {} has more than one video stream; we will ignore all but the first",
                  event.getId()
              );
            }
          }

          return Jsons.obj(
              Jsons.p("uri", track.getURI().toString()),
              Jsons.p("mimetype", track.getMimeType().toString()),
              Jsons.p("flavor", track.getFlavor().toString()),
              Jsons.p("resolution", resolution)
          );
        })
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static String findThumbnail(MediaPackage mp) {
    // Find a suitable thumbnail.
    // TODO: This certainly has to be improved in the future.
    return Arrays.stream(mp.getAttachments())
        .filter(a -> a.getFlavor().getSubtype().equals("player+preview"))
        .map(a -> a.getURI().toString())
        .findFirst()
        .orElse(null);
  }

  private static Jsons.Val findTimelinePreview(MediaPackage mp) {
    return Arrays.stream(mp.getAttachments())
        .filter(a -> a.getFlavor().getSubtype().equals("timeline+preview"))
        .map(a -> {
          final var props = a.getProperties();
          final var imageCountX = props.get("imageSizeX");
          final var imageCountY = props.get("imageSizeY");
          final var resolutionX = props.get("resolutionX");
          final var resolutionY = props.get("resolutionY");

          final var anyNull = imageCountX == null
              || imageCountY == null
              || resolutionX == null
              || resolutionY == null;

          if (anyNull) {
            return null;
          }

          return (Jsons.Val) Jsons.obj(
            Jsons.p("url", a.getURI().toString()),
            Jsons.p("imageCountX", imageCountX),
            Jsons.p("imageCountY", imageCountY),
            Jsons.p("resolutionX", resolutionX),
            Jsons.p("resolutionY", resolutionY)
          );
        })
        .filter(o -> o != null)
        .findFirst()
        .orElse(Jsons.NULL);
  }

  /** Converts a series into the corresponding JSON representation */
  Item(Series series) {
    this.modifiedDate = series.getModifiedDate();

    var serializedACL = series.getAccessControl();
    var acl = new AccessControlList();
    if (serializedACL != null) {
      try {
        acl = AccessControlParser.parseAcl(serializedACL);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    if (series.isDeleted()) {
      this.obj = Jsons.obj(
        Jsons.p("kind", "series-deleted"),
        Jsons.p("id", series.getId()),
        Jsons.p("updated", series.getModifiedDate().getTime())
      );
    } else {
      this.obj = Jsons.obj(
        Jsons.p("kind", "series"),
        Jsons.p("id", series.getId()),
        Jsons.p("title", series.getDublinCore().getFirst(PROPERTY_TITLE)),
        Jsons.p("description", series.getDublinCore().getFirst(PROPERTY_DESCRIPTION)),
        Jsons.p("acl", assembleAcl(acl)),
        Jsons.p("updated", series.getModifiedDate().getTime())
      );
    }
  }

  Date getModifiedDate() {
    return this.modifiedDate;
  }

  Jsons.Val getJson() {
    return this.obj;
  }
}
