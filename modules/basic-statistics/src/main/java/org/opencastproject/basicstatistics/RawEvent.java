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

package org.opencastproject.basicstatistics;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Entity object for low-level events relevant to statistics. Everything starts as a raw event, as this is the only
 * entry point for statistical data.
 */
@Entity(name = "RawEvent")
@Table(name = "oc_basic_statistics_raw_event", indexes = {
    // TODO: indices for faster lookup
    // Lookup by item ID (potentially together with item type)
    // Range queries for timestamp
})
@NamedQueries({
    @NamedQuery(
        name = "RawEvent.findById",
        query = "SELECT p FROM RawEvent p WHERE p.id = :id and p.organization = :organizationId"
    ),
})
public class RawEvent {

  @PrePersist
  public void generateId() {
    if (this.id == null) {
      this.id = UUID.randomUUID().toString();
    }
  }

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "organization", nullable = false, length = 128)
  private String organization;

  /** When the event happened (or started happening) */
  @Column(name = "timestamp", nullable = false)
  private Instant timestamp;

  /** session hash */
  @Column(name = "session", nullable = false)
  private String session;

  /** type of item this applies to, like "video", "series" or "playlist" */
  @Column(name = "item_type", nullable = false)
  private ItemType itemType;

  /** the ID of the item this applies to, e.g. video UUID */
  @Column(name = "item_id", nullable = false)
  private String itemId;

  /** the type of event that happened (e.g. play, pause, seek, …) **/
  @Column(name = "event_type", nullable = false)
  private EventType eventType;

  /** an arbitrary JSON payload that depends on the event type */
  @Column(name = "event_payload", nullable = false, columnDefinition = "TEXT")
  private String eventPayload;

  @Transient
  private static final Gson gson = new Gson();

  public RawEvent() {

  }

  public RawEvent(String id, String organization, Instant timestamp, String session, ItemType itemType, String itemId,
      EventType eventType, String eventPayload) {
    this.id = id;
    this.organization = organization;
    this.timestamp = timestamp;
    this.session = session;
    this.itemType = itemType;
    this.itemId = itemId;
    this.eventType = eventType;
    this.eventPayload = eventPayload;
  }

  public static boolean payloadValidator(EventType eventType, String eventPayload) {
    try {
      switch (eventType) {
        case PAGE_VISIT -> {
          PageVisitParameters payload = gson.fromJson(eventPayload, PageVisitParameters.class);
          if (payload.getUrl() == null) {
            return false;
          }
        }
        case VIDEO_PLAY -> {
          if (eventPayload != null) {
            return false;
          }
        }
        case VIDEO_PAUSE, VIDEO_RESUME -> {
          VideoPauseParameters payload = gson.fromJson(eventPayload, VideoPauseParameters.class);
          if (payload.getAt() == null) {
            return false;
          }
        }
        case VIDEO_SEEK -> {
          VideoSeekParameters payload = gson.fromJson(eventPayload, VideoSeekParameters.class);
          if (payload.getTo() == null) {
            return false;
          }
        }
        case VIDEO_WATCHED -> {
          VideoWatchedParameters payload = gson.fromJson(eventPayload, VideoWatchedParameters.class);
          if (payload.getFrom() == null || payload.getTo() == null) {
            return false;
          }
        }
        case FETCH_FILE -> {
          FetchFileParameters payload = gson.fromJson(eventPayload, FetchFileParameters.class);
          if (payload.getElem() == null || payload.getFrom() == null) {
            return false;
          }
        }
        default -> {
          return false;
        }
      }
    } catch (JsonSyntaxException e) {
      return false;
    }

    return false;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public ItemType getItemType() {
    return itemType;
  }

  public void setItemType(ItemType itemType) {
    this.itemType = itemType;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(EventType eventType) {
    this.eventType = eventType;
  }

  public String getEventPayload() {
    return eventPayload;
  }

  public void setEventPayload(String eventPayload) {
    this.eventPayload = eventPayload;
  }
}
