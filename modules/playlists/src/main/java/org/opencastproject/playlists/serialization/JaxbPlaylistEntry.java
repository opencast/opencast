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
package org.opencastproject.playlists.serialization;

import org.opencastproject.mediapackage.Publication;
import org.opencastproject.playlists.PlaylistEntry;
import org.opencastproject.playlists.PlaylistEntryType;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * serialization of a {@link PlaylistEntry}. Intended for endpoints.
 *
 * Contains additional fields beyond {@link PlaylistEntry}. The additional fields are for use by e.g. GET
 * endpoints to add additional information about an entry before serializing the entry.
 * */
@XmlType(name = "playlist-entry", namespace = "http://playlist.opencastproject.org")
@XmlRootElement(name = "playlist-entry", namespace = "http://playlist.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class JaxbPlaylistEntry {

  @XmlAttribute()
  private long id;

  @XmlElement(name = "contentId")
  private String contentId;

  @XmlElement(name = "type")
  private PlaylistEntryType type;

  @XmlElementWrapper(name = "publications")
  @XmlElement(name = "publication")
  private List<Publication> publications = new ArrayList<>();

  void beforeMarshal(Marshaller u) {
    if (publications != null && publications.isEmpty()) {
      publications = null;
    }
  }

  /**
   * Default no-arg constructor needed by JAXB
   */
  public JaxbPlaylistEntry() {
  }

  public JaxbPlaylistEntry(PlaylistEntry playlistEntry) {
    this();
    this.id = playlistEntry.getId();
    this.contentId = playlistEntry.getContentId();
    this.type = playlistEntry.getType();
  }

  public PlaylistEntry toPlaylistEntry() {
    return new PlaylistEntry(
        id,
        contentId,
        type
    );
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getContentId() {
    return contentId;
  }

  public PlaylistEntryType getType() {
    return type;
  }

  public void setType(PlaylistEntryType type) {
    this.type = type;
  }

  public List<Publication> getPublications() {
    return publications;
  }

  public void setPublications(List<Publication> publications) {
    this.publications = publications;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    JaxbPlaylistEntry jaxbPlaylistEntry = (JaxbPlaylistEntry) o;

    return new EqualsBuilder()
        .append(id, jaxbPlaylistEntry.id)
        .append(contentId, jaxbPlaylistEntry.contentId)
        .append(type, jaxbPlaylistEntry.type)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(id)
        .append(contentId)
        .append(type)
        .toHashCode();
  }
}
