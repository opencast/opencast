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

import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.series.api.Series;
import org.opencastproject.util.Jsons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
  Item(SearchResultItem event) {
    this.modifiedDate = event.getModified();

    if (event.getDeletionDate() != null) {
      this.obj = Jsons.obj(
          Jsons.p("kind", "event-deleted"),
          Jsons.p("id", event.getId()),
          Jsons.p("updated", event.getModified().getTime())
      );
    } else {
      // Find a suitable thumbnail.
      // TODO: This certainly has to be improved in the future.
      final var thumbnail = Arrays.stream(event.getMediaPackage().getAttachments())
          .filter(a -> a.getFlavor().getSubtype().equals("player+preview"))
          .map(a -> a.getURI().toString())
          .findFirst()
          .orElse(null);

      // Obtain JSON array of tracks.
      final List<Jsons.Val> tracks = Arrays.stream(event.getMediaPackage().getTracks())
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

      // Assemble ACL
      final var canReadRoles = new ArrayList<Jsons.Val>();
      final var canWriteRoles = new ArrayList<Jsons.Val>();
      for (final var entry: event.getAccessControlList().getEntries()) {
        if (entry.getAction().equals(Permissions.Action.READ.toString())) {
          canReadRoles.add(Jsons.v(entry.getRole()));
        } else if (entry.getAction().equals(Permissions.Action.WRITE.toString())) {
          canWriteRoles.add(Jsons.v(entry.getRole()));
        }
      }
      final var acl = Jsons.obj(
          Jsons.p("read", Jsons.arr(canReadRoles)),
          Jsons.p("write", Jsons.arr(canWriteRoles))
      );

      final List<Jsons.Val> creators = Arrays.stream(mp.getCreators())
          .map(creator -> Jsons.v(creator))
          .collect(Collectors.toCollection(ArrayList::new));

      this.obj = Jsons.obj(
          Jsons.p("kind", "event"),
          Jsons.p("id", event.getId()),
          Jsons.p("title", event.getDcTitle()),
          Jsons.p("partOf", event.getDcIsPartOf()),
          Jsons.p("description", event.getDcDescription()),
          Jsons.p("created", event.getDcCreated().getTime()),
          Jsons.p("creators", Jsons.arr(creators)),
          Jsons.p("duration", Math.max(0, event.getDcExtent())),
          Jsons.p("thumbnail", thumbnail),
          Jsons.p("tracks", Jsons.arr(tracks)),
          Jsons.p("acl", acl),
          Jsons.p("updated", event.getModified().getTime())
      );
    }
  }

  /** Converts a series into the corresponding JSON representation */
  Item(Series series) {
    this.modifiedDate = series.getModifiedDate();

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
