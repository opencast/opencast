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


package org.opencastproject.mediapackage;

import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;

import java.net.URI;
import java.util.Collection;

/**
 * All classes that will be part of a media package must implement this interface.
 */
public interface MediaPackageElement extends ManifestContributor, Comparable<MediaPackageElement>, Cloneable {

  /**
   * The element type todo is the type definitely needed or can the flavor take its responsibilities?
   */
  enum Type {
    Manifest, Timeline, Track, Catalog, Attachment, Publication, Other
  }

  /**
   * Returns the element identifier.
   *
   * @return the element identifier, may be null
   */
  String getIdentifier();

  /**
   * Sets the element identifier.
   *
   * @param id
   *          the new element identifier
   */
  void setIdentifier(String id);

  /**
   * Returns the element's manifest type.
   *
   * @return the manifest type
   */
  Type getElementType();

  /**
   * Returns a human readable name for this media package element. If no name was provided, the filename is returned
   * instead.
   *
   * @return the element name
   */
  String getElementDescription();

  /**
   * Sets the element description of this media package element.
   *
   * @param description
   *          the new element description
   */
  void setElementDescription(String description);

  /**
   * Tags the media package element with the given tag.
   *
   * @param tag
   *          the tag
   */
  void addTag(String tag);

  /**
   * Removes the tag from the media package element.
   *
   * @param tag
   *          the tag
   */
  void removeTag(String tag);

  /**
   * Returns <code>true</code> if the media package element contains the given tag.
   *
   * @param tag
   *          the tag
   * @return <code>true</code> if the element is tagged
   */
  boolean containsTag(String tag);

  /**
   * Returns <code>true</code> if the media package element contains at least one of the given tags. If there are no
   * tags contained in the set, then the element is considered to match as well.
   *
   * @param tags
   *          the set of tag
   * @return <code>true</code> if the element is tagged accordingly
   */
  boolean containsTag(Collection<String> tags);

  /**
   * Returns the tags for this media package element or an empty array if there are no tags.
   *
   * @return the tags
   */
  String[] getTags();

  /** Removes all tags associated with this element */
  void clearTags();

  /**
   * Returns the media package if the element has been added, <code>null</code> otherwise.
   *
   * @return the media package
   */
  MediaPackage getMediaPackage();

  /**
   * Returns a reference to another entitiy, both inside or outside the media package.
   *
   * @return the reference
   */
  MediaPackageReference getReference();

  /**
   * Sets the element reference.
   *
   * @param reference
   *          the reference
   */
  void setReference(MediaPackageReference reference);

  /**
   * Returns a reference to the element location.
   *
   * @return the element location
   */
  URI getURI();

  /**
   * Sets the elements location.
   *
   * @param uri
   *          the element location
   */
  void setURI(URI uri);

  /**
   * Returns the file's checksum.
   *
   * @return the checksum
   */
  Checksum getChecksum();

  /**
   * Sets the new checksum on this media package element.
   *
   * @param checksum
   *          the checksum
   */
  void setChecksum(Checksum checksum);

  /**
   * Returns the element's mimetype as found in the ISO Mime Type Registrations.
   * <p/>
   * For example, in case of motion jpeg slides, this method will return the mime type for <code>video/mj2</code>.
   *
   * @return the mime type
   */
  MimeType getMimeType();

  /**
   * Sets the mime type on this media package element.
   *
   * @param mimeType
   *          the new mime type
   */
  void setMimeType(MimeType mimeType);

  /**
   * Returns the element's type as defined for the specific media package element.
   * <p/>
   * For example, in case of a video track, the type could be <code>video/x-presentation</code>.
   *
   * @return the element flavor
   */
  MediaPackageElementFlavor getFlavor();

  /**
   * Sets the flavor on this media package element.
   *
   * @param flavor
   *          the new flavor
   */
  void setFlavor(MediaPackageElementFlavor flavor);

  /**
   * Returns the number of bytes that are occupied by this media package element.
   *
   * @return the size
   */
  long getSize();

  /**
   * Sets the file size in bytes
   *
   * @param size
   */
  void setSize(long size);

  /**
   * Verifies the integrity of the media package element.
   *
   * @throws MediaPackageException
   *           if the media package element is in an incosistant state
   */
  void verify() throws MediaPackageException;

  /**
   * Adds a reference to the media package <code>mediaPackage</code>.
   * <p/>
   * Note that an element can only refer to one object. Therefore, any existing reference will be replaced.
   *
   * @param mediaPackage
   *          the media package to refere to
   */
  void referTo(MediaPackage mediaPackage);

  /**
   * Adds a reference to the media package element <code>element</code>.
   * <p/>
   * Note that an element can only refere to one object. Therefore, any existing reference will be replaced. Also note
   * that if this element is part of a media package, a consistency check will be made making sure the refered element
   * is also part of the same media package. If not, a {@link MediaPackageException} will be thrown.
   *
   * @param element
   *          the element to refere to
   */
  void referTo(MediaPackageElement element);

  /**
   * Adds an arbitrary reference.
   * <p/>
   * Note that an element can only have one reference. Therefore, any existing reference will be replaced. Also note
   * that if this element is part of a media package, a consistency check will be made making sure the refered element
   * is also part of the same media package. If not, a {@link MediaPackageException} will be thrown.
   *
   * @param reference
   *          the reference
   */
  void referTo(MediaPackageReference reference);

  /**
   * Removes any reference.
   */
  void clearReference();

  /**
   * Create a deep copy of this object.
   *
   * @return The copy
   */
  Object clone();

}
