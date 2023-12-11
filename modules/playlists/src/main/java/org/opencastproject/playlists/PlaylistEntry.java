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
package org.opencastproject.playlists;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A playlist entry belongs to a playlist and cannot exist without it. It holds a reference to an event, as well as
 * additional metadata.
 */
@Entity(name = "PlaylistEntry")
@Table(name = "oc_playlist_entry")
public class PlaylistEntry {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  @Column(name = "event_id")
  private String eventId;

  private PlaylistEntryType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  /**
   * Default constructor
   */
  public PlaylistEntry() {

  }

  public PlaylistEntry(String eventId, PlaylistEntryType type) {
    this.eventId = eventId;
    this.type = type;
  }

  public PlaylistEntry(long id, String eventId, PlaylistEntryType type) {
    this.id = id;
    this.eventId = eventId;
    this.type = type;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public PlaylistEntryType getType() {
    return type;
  }

  public void setType(PlaylistEntryType type) {
    this.type = type;
  }

  public Playlist getPlaylist() {
    return playlist;
  }

  public void setPlaylist(Playlist playlist) {
    this.playlist = playlist;
  }
}
