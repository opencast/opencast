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

package org.opencastproject.message.broker.api.archive;

import org.opencastproject.archive.api.Version;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;

import java.io.Serializable;
import java.util.Date;

/**
 * {@link Serializable} class that represents all of the possible messages sent through an Archive queue.
 */
public class ArchiveItem implements Serializable {

  private static final long serialVersionUID = 1838486668816386129L;

  public static final String ARCHIVE_QUEUE_PREFIX = "ARCHIVE.";

  public static final String ARCHIVE_QUEUE = ARCHIVE_QUEUE_PREFIX + "QUEUE";

  private final String mediapackageId;
  private final String mediapackage;
  private final String acl;
  private final long version;
  private final Date date;
  private final Type type;

  public enum Type {
    Update, Delete
  };

  /**
   * @param mediapackage
   *          The mediapackage to update.
   * @param acl
   *          The access control list of the mediapackage to update.
   * @param version
   *          The version of the mediapackage.
   * @param date
   *          The modification date.
   * @return Builds a {@link ArchiveItem} for updating a mediapackage.
   */
  public static ArchiveItem update(MediaPackage mediapackage, AccessControlList acl, Version version, Date date) {
    return new ArchiveItem(mediapackage, acl, version, date);
  }

  /**
   * @param mediapackageId
   *          The unique id of the mediapackage to delete.
   * @param date
   *          The modification date.
   * @return Builds {@link ArchiveItem} for deleting a mediapackage from the archive.
   */
  public static ArchiveItem delete(String mediapackageId, Date date) {
    return new ArchiveItem(mediapackageId, date);
  }

  /**
   * Constructor to build an Update {@link ArchiveItem}
   *
   * @param mediapackage
   *          The mediapackage to populate
   * @param acl
   *          The access control list for the mediapackage.
   * @param version
   *          The version of the mediapackage.
   * @param date
   *          The modification date.
   */
  public ArchiveItem(MediaPackage mediapackage, AccessControlList acl, Version version, Date date) {
    this.mediapackageId = null;
    this.mediapackage = MediaPackageParser.getAsXml(mediapackage);
    this.acl = AccessControlParser.toJsonSilent(acl);
    this.version = version.value();
    this.date = date;
    this.type = Type.Update;
  }

  /**
   * Constructor to build a delete mediapackage {@link ArchiveItem}.
   *
   * @param mediapackageId
   *          The id of the mediapackage to delete.
   * @param date
   *          The modification date.
   */
  public ArchiveItem(String mediapackageId, Date date) {
    this.mediapackageId = mediapackageId;
    this.mediapackage = null;
    this.acl = null;
    this.version = -1;
    this.date = date;
    this.type = Type.Delete;
  }

  public String getMediapackageId() {
    return mediapackageId;
  }

  public MediaPackage getMediapackage() {
    try {
      return MediaPackageParser.getFromXml(mediapackage);
    } catch (MediaPackageException e) {
      throw new IllegalStateException(e);
    }
  }

  public AccessControlList getAcl() {
    try {
      return acl == null ? null : AccessControlParser.parseAcl(acl);
    } catch (Exception e) {
      throw new IllegalStateException();
    }
  }

  public Version getVersion() {
    return Version.version(version);
  }

  public Date getDate() {
    return date;
  }

  public Type getType() {
    return type;
  }

}
