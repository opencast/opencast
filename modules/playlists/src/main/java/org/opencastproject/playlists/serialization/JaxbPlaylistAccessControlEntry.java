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

import org.opencastproject.playlists.PlaylistAccessControlEntry;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/** 1:1 serialization of a {@link PlaylistAccessControlEntry}. Intended for endpoints. */
@XmlType(name = "playlist-access-control-entry", namespace = "http://playlist.opencastproject.org")
@XmlRootElement(name = "playlist-access-control-entry", namespace = "http://playlist.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class JaxbPlaylistAccessControlEntry {
  @XmlTransient
  private long id;

  @XmlElement(name = "allow")
  private boolean allow;

  @XmlElement(name = "role")
  private String role;

  @XmlElement(name = "action")
  private String action;

  public JaxbPlaylistAccessControlEntry() {
  }

  public JaxbPlaylistAccessControlEntry(PlaylistAccessControlEntry playlistAccessControlEntry) {
    this();
    this.id = playlistAccessControlEntry.getId();
    this.allow = playlistAccessControlEntry.isAllow();
    this.role = playlistAccessControlEntry.getRole();
    this.action = playlistAccessControlEntry.getAction();
  }

  public PlaylistAccessControlEntry toPlaylistAccessControlEntry() {
    return new PlaylistAccessControlEntry(
        id,
        allow,
        role,
        action
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    JaxbPlaylistAccessControlEntry jaxbPlaylistAccessControlEntry = (JaxbPlaylistAccessControlEntry) o;

    return new EqualsBuilder()
        .append(id, jaxbPlaylistAccessControlEntry.id)
        .append(allow, jaxbPlaylistAccessControlEntry.allow)
        .append(role, jaxbPlaylistAccessControlEntry.role)
        .append(action, jaxbPlaylistAccessControlEntry.action)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(id)
        .append(allow)
        .append(role)
        .append(action)
        .toHashCode();
  }
}
