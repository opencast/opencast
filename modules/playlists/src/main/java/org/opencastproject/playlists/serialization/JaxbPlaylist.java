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

import org.opencastproject.playlists.Playlist;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/** 1:1 serialization of a {@link Playlist}. Intended for endpoints. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "playlist", namespace = "http://playlist.opencastproject.org")
@XmlRootElement(name = "playlist", namespace = "http://playlist.opencastproject.org")
public class JaxbPlaylist {

  static class DateAdapter extends XmlAdapter<Long, Date> {
    /**
     * {@inheritDoc}
     *
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
     */
    @Override
    public Long marshal(Date v) throws Exception {
      return v == null ? null : v.getTime();
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
     */
    @Override
    public Date unmarshal(Long v) throws Exception {
      return v == null ? null : new Date(v);
    }
  }

  @XmlAttribute()
  private String id;

  @XmlElement(name = "organization")
  private String organization;

  private List<JaxbPlaylistEntry> entries;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "description")
  private String description;

  @XmlElement(name = "creator")
  private String creator;

  @XmlJavaTypeAdapter(JaxbPlaylist.DateAdapter.class)
  @XmlElement
  private Date updated;

  private List<JaxbPlaylistAccessControlEntry> accessControlEntries ;

  void beforeMarshal(Marshaller u) {
    // Explicitly set empty lists to `null`. This is to avoid having an empty list wrongly show up in a JSON
    // serialization with the value of an empty string
    if (entries != null && entries.isEmpty()) {
      entries = null;
    }
    if (accessControlEntries != null && accessControlEntries.isEmpty()) {
      accessControlEntries = null;
    }
  }

  /**
   * Default no-arg constructor needed by JAXB
   */
  public JaxbPlaylist() {
  }

  public JaxbPlaylist(Playlist playlist) {
    this();
    this.id = playlist.getId();
    this.organization = playlist.getOrganization();
    this.entries = playlist.getEntries()
        .stream()
        .map(JaxbPlaylistEntry::new)
        .collect(Collectors.toList());
    this.title = playlist.getTitle();
    this.description = playlist.getDescription();
    this.creator = playlist.getCreator();
    this.updated = playlist.getUpdated();
    this.accessControlEntries = playlist.getAccessControlEntries()
        .stream()
        .map(JaxbPlaylistAccessControlEntry::new)
        .collect(Collectors.toList());
  }

  public Playlist toPlaylist() {
    return new Playlist(
        id,
        organization,
        Optional.ofNullable(entries).orElseGet(Collections::emptyList)
        .stream().map(JaxbPlaylistEntry::toPlaylistEntry).collect(Collectors.toList()),
        title,
        description,
        creator,
        updated,
        Optional.ofNullable(accessControlEntries).orElseGet(Collections::emptyList)
            .stream().map(JaxbPlaylistAccessControlEntry::toPlaylistAccessControlEntry).collect(Collectors.toList())

    );
  }

  public List<JaxbPlaylistEntry> getEntries() {
    return entries;
  }

  public void setEntries(List<JaxbPlaylistEntry> entries) {
    this.entries = entries;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    JaxbPlaylist jaxbPlaylist = (JaxbPlaylist) o;

    return new EqualsBuilder()
        .append(id, jaxbPlaylist.id)
        .append(organization, jaxbPlaylist.organization)
        .append(entries, jaxbPlaylist.entries)
        .append(title, jaxbPlaylist.title)
        .append(description, jaxbPlaylist.description)
        .append(creator, jaxbPlaylist.creator)
        .append(updated, jaxbPlaylist.updated)
        .append(accessControlEntries, jaxbPlaylist.accessControlEntries)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(id)
        .append(organization)
        .append(entries)
        .append(title)
        .append(description)
        .append(creator)
        .append(updated)
        .append(accessControlEntries)
        .toHashCode();
  }
}
