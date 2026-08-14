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

package org.opencastproject.tobira.impl;

import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_DESCRIPTION;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.mpeg7.MediaTimePointImpl;
import org.opencastproject.playlists.Playlist;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.series.api.Series;
import org.opencastproject.util.MimeType;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * A item of the harvesting API, basically as a JSON object. Can be "event", "series",
 * "event-deleted" or "series-deleted". Also contains the modified date, used for sorting.
 */
class Item {
  private static final Logger logger = LoggerFactory.getLogger(Item.class);

  private Date modifiedDate;
  private JsonObject obj;

  /** Converts a event into the corresponding JSON representation */
  Item(SearchResult event, AuthorizationService authorizationService, Workspace workspace) {
    this.modifiedDate = event.getModifiedDate();

    if (event.getDeletionDate() != null) {
      this.obj = new JsonObject();
      this.obj.addProperty("kind", "event-deleted");
      this.obj.addProperty("id", event.getId());
      this.obj.addProperty("updated", event.getModifiedDate().getTime());
    } else {
      final var mp = event.getMediaPackage();
      final var dccs = getDccsFromMp(mp, workspace);

      // Figure out whether this is a live event
      final var isLive = Arrays.stream(mp.getTracks()).anyMatch(track -> track.isLive());

      // Obtain creators. We first try to obtain it from the DCCs. We collect
      // into `LinkedHashSet` to deduplicate entries.
      // NB: this used to collect into a LinkedHashSet "to deduplicate entries", but the values were
      // wrapped in objects without equals(), so nothing was ever deduplicated. Keep the previous
      // behaviour here; deduplicating would be a change in what we report, not part of this port.
      final var creators = new JsonArray();
      dccs.stream()
              .flatMap(dcc -> dcc.get(DublinCore.PROPERTY_CREATOR).stream())
              .filter(Objects::nonNull)
              .forEach(creator -> creators.add(creator.getValue()));

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
        // If there is no title to be found, we throw an exception to skip this event.
        throw new RuntimeException("Event has no title");
      }

      final var captions = findCaptions(mp);

      // Get the generated slide text.
      final var slideText = Arrays.stream(mp.getElements())
          .filter(mpe -> mpe.getFlavor().eq("mpeg-7/text"))
          .map(element -> element.getURI())
          .findFirst();

      // Obtain duration from tracks, as that's usually more accurate (stores information from
      // inspect operations). Fall back to `getDcExtent`.
      final var duration = Arrays.stream(mp.getTracks())
          .filter(track -> track.hasVideo() || track.hasAudio())
          .map(Track::getDuration)
          .filter(d -> d != null && d > 0)
          .mapToLong(Long::longValue)
          // Not entirely clear how to combine different track durations. Taking the max is not
          // worse than any other thing that I can think of. And usually all durations are basically
          // the same.
          .max()
          //NB: This is an else case, so we ignore the item(s) in the stream
          .orElseGet(() -> {
            String dcExtent = event.getDublinCore().getFirst(DublinCore.PROPERTY_EXTENT);
            return Math.max(0L, EncodingSchemeUtils.decodeMandatoryDuration(dcExtent));
          });

      this.obj = new JsonObject();
      this.obj.addProperty("kind", "event");
      this.obj.addProperty("id", event.getId());
      this.obj.addProperty("title", title);
      this.obj.addProperty("partOf", event.getDublinCore().getFirst(DublinCore.PROPERTY_IS_PART_OF));
      this.obj.addProperty("description", event.getDublinCore().getFirst(PROPERTY_DESCRIPTION));
      this.obj.addProperty("created", event.getCreatedDate().toEpochMilli());
      this.obj.addProperty("startTime", period.map(p -> p.getStart().getTime()).orElse(null));
      this.obj.addProperty("endTime", period.map(p -> p.getEnd().getTime()).orElse(null));
      this.obj.add("creators", creators);
      this.obj.addProperty("duration", duration);
      this.obj.addProperty("thumbnail", findThumbnail(mp));
      this.obj.add("timelinePreview", findTimelinePreview(mp));
      this.obj.add("tracks", assembleTracks(event, mp));
      this.obj.add("acl", assembleAcl(authorizationService.getAcl(mp, AclScope.Merged).getA().getEntries()));
      this.obj.addProperty("isLive", isLive);
      this.obj.add("metadata", dccToMetadata(dccs, Set.of(new String[] {
          "created", "creator", "title", "extent", "isPartOf", "description", "identifier",
      })));
      this.obj.add("captions", captions);
      this.obj.addProperty("slideText", slideText.map(t -> t.toString()).orElse(null));
      this.obj.add("segments", findSegments(mp));
      this.obj.addProperty("updated", event.getModifiedDate().getTime());
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

  /**
   * Assembles the object containing all additional metadata.
   *
   * The second argument is a list of dcterms metadata fields that is already included elsewhere in
   * the response. They will be ignored here.
   */
  private static JsonObject dccToMetadata(List<DublinCoreCatalog> dccs, Set<String> ignoredDcFields) {
    final var namespaces = new HashMap<String, JsonObject>();

    for (final var dcc : (Iterable<DublinCoreCatalog>) dccs::iterator) {
      for (final var e : dcc.getValues().entrySet()) {
        final var key = e.getKey();

        // We special case dcterms here to get a smaller, easier to read JSON. In most cases, this
        // will be the only namespace.
        final var ns = key.getNamespaceURI().equals("http://purl.org/dc/terms/")
            ? "dcterms"
            : key.getNamespaceURI();

        // We skip fields that we already include elsewhere.
        if (ns.equals("dcterms") && ignoredDcFields.contains(key.getLocalName())) {
          continue;
        }

        final var fields = namespaces.computeIfAbsent(ns, k -> new JsonObject());
        final var values = new JsonArray();
        e.getValue().forEach(v -> values.add(v.getValue()));
        fields.add(e.getKey().getLocalName(), values);
      }
    }

    final var metadata = new JsonObject();
    namespaces.forEach(metadata::add);
    return metadata;
  }

  private static JsonObject assembleAcl(List<AccessControlEntry> acl) {
    // We just transform the ACL into a map with one field per action, and the
    // value being a list of roles, e.g.
    // `{ "read": ["ROLE_USER", "ROLE_FOO"], "write": [...] }`
    final var actionToRoles = new HashMap<String, JsonArray>();
    acl.stream().filter(AccessControlEntry::isAllow).forEach(entry -> {
      final var action = entry.getAction();
      actionToRoles.computeIfAbsent(action, k -> new JsonArray()).add(entry.getRole());
    });

    final var json = new JsonObject();
    actionToRoles.forEach(json::add);
    return json;
  }

  private static JsonArray assembleTracks(SearchResult event, MediaPackage mp) {
    final var tracks = new JsonArray();
    Arrays.stream(mp.getTracks())
        .filter(track -> track.hasAudio() || track.hasVideo())
        .forEach(track -> {
          var videoStreams = TrackSupport.byType(track.getStreams(), VideoStream.class);
          JsonElement resolution = JsonNull.INSTANCE;
          if (videoStreams.length > 0) {
            final var stream = videoStreams[0];
            final var res = new JsonArray();
            res.add(stream.getFrameWidth());
            res.add(stream.getFrameHeight());
            resolution = res;

            if (videoStreams.length > 1) {
              logger.warn(
                  "Track of event {} has more than one video stream; we will ignore all but the first",
                  event.getId()
              );
            }
          }

          final var json = new JsonObject();
          json.addProperty("uri", track.getURI().toString());
          json.addProperty("mimetype", track.getMimeType().toString());
          json.addProperty("flavor", track.getFlavor().toString());
          json.add("resolution", resolution);
          json.addProperty("isMaster", track.isMaster());
          tracks.add(json);
        });
    return tracks;
  }

  private static JsonArray findCaptions(MediaPackage mp) {
    final var captions = new JsonArray();
    Arrays.stream(mp.getElements())
        .filter(element -> {
          final var isVTT = element.getFlavor().toString().startsWith("captions/vtt")
                || element.getMimeType().eq("text", "vtt");
          final var isCorrectType = element.getElementType() == MediaPackageElement.Type.Attachment
                || element.getElementType() == MediaPackageElement.Type.Track;

          return isVTT && isCorrectType;
        })
        .map(track -> {
          final var tags = track.getTags();
          final Function<String, Optional<String>> findTag = (String prefix) -> Arrays.stream(tags)
                .map(tag -> tag.split(":", 2))
                .filter(tagArray -> (tagArray.length == 2 && tagArray[0].equals(prefix)))
                .map(tagArray -> tagArray[1])
                .findFirst();

          // Try to get a language for this subtitle track. We first check the proper tag.
          var lang = findTag.apply("lang");
          if (lang.isEmpty()) {
            // But for compatibility, we also check in the flavor.
            final var subflavor = track.getFlavor().getSubtype();
            if (subflavor.startsWith("vtt+")) {
              final var suffix = subflavor.substring("vtt+".length());
              if (suffix.length() > 0) {
                lang = Optional.of(suffix);
              }
            }
          }

          final var json = new JsonObject();
          json.addProperty("uri", track.getURI().toString());
          json.addProperty("lang", lang.orElse(null));
          json.addProperty("generatorType", findTag.apply("generator-type").orElse(null));
          json.addProperty("generator", findTag.apply("generator").orElse(null));
          json.addProperty("type", findTag.apply("type").orElse(null));
          return json;
        })
        .forEach(captions::add);
    return captions;
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

  private static JsonArray findSegments(MediaPackage mp) {
    final var segments = new JsonArray();
    Arrays.stream(mp.getAttachments())
        .filter(a -> a.getFlavor().getSubtype().equals("segment+preview"))
        .forEach(s -> {
          final var json = new JsonObject();
          json.addProperty("uri", s.getURI().toString());
          json.addProperty("startTime", MediaTimePointImpl.parseTimePoint(
              s.getReference().getProperty("time")
          ).getTimeInMilliseconds());
          segments.add(json);
        });
    return segments;
  }

  private static JsonElement findTimelinePreview(MediaPackage mp) {
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

          final var json = new JsonObject();
          json.addProperty("url", a.getURI().toString());
          json.addProperty("imageCountX", imageCountX);
          json.addProperty("imageCountY", imageCountY);
          json.addProperty("resolutionX", resolutionX);
          json.addProperty("resolutionY", resolutionY);
          return (JsonElement) json;
        })
        .filter(o -> o != null)
        .findFirst()
        .orElse(JsonNull.INSTANCE);
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
      this.obj = new JsonObject();
      this.obj.addProperty("kind", "series-deleted");
      this.obj.addProperty("id", series.getId());
      this.obj.addProperty("updated", series.getModifiedDate().getTime());
    } else {
      // Created date
      var createdDateString = series.getDublinCore().getFirst(PROPERTY_CREATED);
      JsonElement created = JsonNull.INSTANCE;
      var date = EncodingSchemeUtils.decodeDate(createdDateString);
      if (date != null) {
        created = new JsonPrimitive(date.getTime());
      } else {
        logger.warn("Series {} has unparsable created-date: {}", series.getId(), createdDateString);
      }

      var additionalMetadata = dccToMetadata(Arrays.asList(series.getDublinCore()), Set.of(new String[] {
          "created", "title", "description", "identifier",
      }));

      this.obj = new JsonObject();
      this.obj.addProperty("kind", "series");
      this.obj.addProperty("id", series.getId());
      this.obj.addProperty("title", series.getDublinCore().getFirst(PROPERTY_TITLE));
      this.obj.addProperty("description", series.getDublinCore().getFirst(PROPERTY_DESCRIPTION));
      this.obj.add("acl", assembleAcl(acl.getEntries()));
      this.obj.add("metadata", additionalMetadata);
      this.obj.add("created", created);
      this.obj.addProperty("updated", series.getModifiedDate().getTime());
    }
  }

  /** Converts a series into the corresponding JSON representation */
  Item(Playlist playlist) {
    this.modifiedDate = playlist.getUpdated();

    final var acl = assembleAcl(
        playlist.getAccessControlEntries()
            .stream()
            .map(entry -> entry.toAccessControlEntry())
            .collect(Collectors.toList())
    );

    // Assemble entries
    final var entries = new JsonArray();
    playlist.getEntries().forEach(entry -> {
      final var json = new JsonObject();
      json.addProperty("id", entry.getId());
      json.addProperty("contentId", entry.getContentId());
      json.addProperty("type", entry.getType().getCode());
      entries.add(json);
    });

    if (playlist.isDeleted()) {
      this.obj = new JsonObject();
      this.obj.addProperty("kind", "playlist-deleted");
      this.obj.addProperty("id", playlist.getId());
      this.obj.addProperty("updated", playlist.getUpdated().getTime());
    } else {
      this.obj = new JsonObject();
      this.obj.addProperty("kind", "playlist");
      this.obj.addProperty("id", playlist.getId());
      this.obj.addProperty("title", playlist.getTitle());
      this.obj.addProperty("description", playlist.getDescription());
      this.obj.addProperty("creator", playlist.getCreator());
      this.obj.add("entries", entries);
      this.obj.add("acl", acl);
      this.obj.addProperty("updated", this.modifiedDate.getTime());
    }
  }

  Date getModifiedDate() {
    return this.modifiedDate;
  }

  JsonObject getJson() {
    return this.obj;
  }
}
