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


import static org.opencastproject.util.RequireUtil.notNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity object for storing playlists in persistence storage. Playlists contain an ordered list of entries,
 * which represent Opencast entities (event, series and the like). Playlists also contain some metadata and their own
 * access control list.
 */
@Entity(name = "Playlist")
@Table(name = "oc_playlist")
@NamedQueries({
    @NamedQuery(
        name = "Playlist.findById",
        query = "SELECT p FROM Playlist p WHERE p.id = :id and p.organization = :organizationId"
    ),
})
public class Playlist {

  /**
   * Workaround for generating a UUID for the id if we don't already have one.
   *
   * We cannot use EclipseLinks @UuidGenerator annotation for this, because it breaks our JUnit tests.
   * We also cannot use Hibernates @GenericGenerator for this, since we lack the appropriate library (and possibly
   * hibernate version).
   */
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

  @OneToMany(
      fetch = FetchType.LAZY,
      cascade = { CascadeType.PERSIST, CascadeType.REMOVE },
      mappedBy = "playlist",
      orphanRemoval = true
  )
  @OrderColumn(name = "position_entries")
  private List<PlaylistEntry> entries = new ArrayList<PlaylistEntry>();

  @Column(name = "title")
  private String title;

  @Column(name = "description")
  private String description;

  @Column(name = "creator")
  private String creator;

  @Column(name = "updated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date updated;

  @Column(name = "deletion_date")
  @Temporal(TemporalType.TIMESTAMP)
  protected Date deletionDate = null;

  @OneToMany(
      fetch = FetchType.LAZY,
      cascade = { CascadeType.PERSIST, CascadeType.REMOVE },
      mappedBy = "playlist",
      orphanRemoval = true
  )
  @OrderColumn(name = "position_access_control_entries")
  private List<PlaylistAccessControlEntry> accessControlEntries = new ArrayList<>();

  /**
   * Default constructor
   */
  public Playlist() {

  }

  public Playlist(String id, String organization, List<PlaylistEntry> entries, String title, String description,
      String creator, Date updated, List<PlaylistAccessControlEntry> accessControlEntries) {
    this.id = id;
    this.organization = organization;
    this.entries = entries;
    this.title = title;
    this.description = description;
    this.creator = creator;
    this.updated = updated;
    this.accessControlEntries = accessControlEntries;
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

  public List<PlaylistEntry> getEntries() {
    return entries;
  }

  public void setEntries(List<PlaylistEntry> entries) {
    for (var entry : entries) {
      entry.setPlaylist(this);
    }
    this.entries = entries;
  }

  public boolean addEntry(PlaylistEntry entry) {
    notNull(entry, "entry");
    entry.setPlaylist(this);
    return entries.add(entry);
  }

  public boolean removeEntry(PlaylistEntry entry) {
    notNull(entry, "entry");
    entry.setPlaylist(null);
    return entries.remove(entry);
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public Date getUpdated() {
    return updated;
  }

  public void setUpdated(Date updated) {
    this.updated = updated;
  }

  public Date getDeletionDate() {
    return deletionDate;
  }

  public void setDeletionDate(Date deletionDate) {
    this.deletionDate = deletionDate;
  }

  public boolean isDeleted() {
    return this.deletionDate != null;
  }

  public List<PlaylistAccessControlEntry> getAccessControlEntries() {
    return accessControlEntries;
  }

  public void setAccessControlEntries(List<PlaylistAccessControlEntry> accessControlEntries) {
    for (var accessControlEntry : accessControlEntries) {
      accessControlEntry.setPlaylist(this);
    }
    this.accessControlEntries = accessControlEntries;
  }


}
