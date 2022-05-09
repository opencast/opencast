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

import org.opencastproject.mediapackage.identifier.Id;

import java.net.URI;
import java.util.Collection;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface for a media package, which is a data container moving through the system, containing metadata, tracks and
 * attachments.
 */
@XmlJavaTypeAdapter(MediaPackageImpl.Adapter.class)
public interface MediaPackage extends Cloneable {

  /**
   * Returns the media package identifier.
   *
   * @return the identifier
   */
  Id getIdentifier();

  void setIdentifier(Id id);

  void setTitle(String title);

  /**
   * Returns the title for the associated series, if any.
   *
   * @return The series title
   *
   * @deprecated This is not guaranteed to be correct. Use the metadata contained in the Dublin Core catalog instead.
   */
  @Deprecated
  String getSeriesTitle();

  void setSeriesTitle(String seriesTitle);

  /**
   * Returns the title of the episode that this mediapackage represents.
   *
   * @return The episode title
   *
   * @deprecated This is not guaranteed to be correct. Use the metadata contained in the Dublin Core catalog instead.
   */
  @Deprecated
  String getTitle();

  void addCreator(String creator);

  void removeCreator(String creator);

  /**
   * Returns the names of the institutions or people who created this mediapackage
   *
   * @return the creators of this mediapackage
   *
   * @deprecated This is not guaranteed to be correct. Use the metadata contained in the Dublin Core catalog instead.
   */
  @Deprecated
  String[] getCreators();

  void setSeries(String identifier);

  /**
   * Returns the series, if any, to which this mediapackage belongs
   *
   * @return the series
   */
  String getSeries();

  void setLicense(String license);

  /**
   * The license for the content in this mediapackage
   *
   * @return the license
   *
   * @deprecated This is not guaranteed to be correct. Use the metadata contained in the Dublin Core catalog instead.
   */
  @Deprecated
  String getLicense();

  void addContributor(String contributor);

  void removeContributor(String contributor);

  /**
   * Returns the names of the institutions or people who contributed to the content within this mediapackage
   *
   * @return the contributors
   *
   * @deprecated This is not guaranteed to be correct. Use the metadata contained in the Dublin Core catalog instead.
   */
  @Deprecated
  String[] getContributors();

  void setLanguage(String language);

  /**
   * Returns the language written and/or spoken in the media content of this mediapackage
   *
   * @return the language
   *
   * @deprecated This is not guaranteed to be correct. Use the metadata contained in the Dublin Core catalog instead.
   */
  @Deprecated
  String getLanguage();

  void addSubject(String subject);

  void removeSubject(String subject);

  /**
   * The keywords describing the subject(s) or categories describing the content of this mediapackage
   *
   * @return the subjects
   *
   * @deprecated This is not guaranteed to be correct. Use the metadata contained in the Dublin Core catalog instead.
   */
  @Deprecated
  String[] getSubjects();

  void setDate(Date date);

  /**
   * Returns the media package start time.
   *
   * @return the start time
   *
   * @deprecated This is not guaranteed to be correct. Use the metadata contained in the Dublin Core catalog instead.
   */
  @Deprecated
  Date getDate();

  /**
   * Returns the media package duration in milliseconds or <code>null</code> if no duration is available.
   *
   * @return the duration
   *
   * @deprecated This is not guaranteed to be correct. Use the metadata contained in the Dublin Core catalog instead.
   */
  @Deprecated
  Long getDuration();

  /**
   * Sets the duration of the media package in milliseconds. This method will throw an {@link IllegalStateException} if
   * tracks have been added to the mediapackage already. Also note that as soon as the first track is added, the
   * duration will be udpated according to the track's length.
   *
   * @param duration
   *          the duration in milliseconds
   * @throws IllegalStateException
   *           if the mediapackage already contains a track
   */
  void setDuration(Long duration) throws IllegalStateException;

  /**
   * Returns <code>true</code> if the given element is part of the media package.
   *
   * @param element
   *          the element
   * @return <code>true</code> if the element belongs to the media package
   */
  boolean contains(MediaPackageElement element);

  /**
   * Returns an iteration of the media package elements.
   *
   * @return the media package elements
   */
  Iterable<MediaPackageElement> elements();

  /**
   * Returns all the elements.
   *
   * @return the elements
   */
  MediaPackageElement[] getElements();

  /**
   * Returns the element that is identified by the given reference or <code>null</code> if no such element exists.
   *
   * @param reference
   *          the reference
   * @return the element
   */
  MediaPackageElement getElementByReference(MediaPackageReference reference);

  /**
   * Returns the element that is identified by the given identifier or <code>null</code> if no such element exists.
   *
   * @param id
   *          the element identifier
   * @return the element
   */
  MediaPackageElement getElementById(String id);

  /**
   * Returns the elements that are tagged with any of the given tags or an empty array if no such elements are found. If
   * any of the tags in the <code>tags</code> collection start with a '-' character, any elements matching the tag will
   * be excluded from the returned MediaPackageElement[]. If <code>tags</code> is empty or null, all elements are
   * returned.
   *
   * @param tags
   *          the tags
   * @return the elements
   */
  MediaPackageElement[] getElementsByTags(Collection<String> tags);

  /**
   * Returns all elements of this media package with the given flavor.
   *
   * @return the media package elements
   */
  MediaPackageElement[] getElementsByFlavor(MediaPackageElementFlavor flavor);

  /**
   * Returns the track identified by <code>trackId</code> or <code>null</code> if that track doesn't exists.
   *
   * @param trackId
   *          the track identifier
   * @return the tracks
   */
  Track getTrack(String trackId);

  /**
   * Returns the tracks that are part of this media package.
   *
   * @return the tracks
   */
  Track[] getTracks();

  /**
   * Returns the tracks that are tagged with the given tag or an empty array if no such tracks are found.
   *
   * @param tag
   *          the tag
   * @return the tracks
   */
  Track[] getTracksByTag(String tag);

  /**
   * Returns the tracks that are tagged with any of the given tags or an empty array if no such elements are found. If
   * any of the tags in the <code>tags</code> collection start with a '-' character, any elements matching the tag will
   * be excluded from the returned Track[]. If <code>tags</code> is empty or null, all tracks are returned.
   *
   * @param tags
   *          the tags
   * @return the tracks
   */
  Track[] getTracksByTags(Collection<String> tags);

  /**
   * Returns the tracks that are part of this media package and match the given flavor as defined in {@link Track}.
   *
   * @param flavor
   *          the track's flavor
   * @return the tracks with the specified flavor
   */
  Track[] getTracks(MediaPackageElementFlavor flavor);

  /**
   * Returns <code>true</code> if the media package contains media tracks of any kind.
   *
   * @return <code>true</code> if the media package contains tracks
   */
  boolean hasTracks();

  /**
   * Returns the attachment identified by <code>attachmentId</code> or <code>null</code> if that attachment does not
   * exist.
   *
   * @param attachmentId
   *          the attachment identifier
   * @return the attachments
   */
  Attachment getAttachment(String attachmentId);

  /**
   * Returns the attachments that are part of this media package.
   *
   * @return the attachments
   */
  Attachment[] getAttachments();

  /**
   * Returns the attachments that are part of this media package and match the specified flavor.
   *
   * @param flavor
   *          the attachment flavor
   * @return the attachments
   */
  Attachment[] getAttachments(MediaPackageElementFlavor flavor);

  /**
   * Returns the presentations that are part of this media package.
   *
   * @return the attachments
   */
  Publication[] getPublications();

  /**
   * Returns the catalog identified by <code>catalogId</code> or <code>null</code> if that catalog doesn't exists.
   *
   * @param catalogId
   *          the catalog identifier
   * @return the catalogs
   */
  Catalog getCatalog(String catalogId);

  /**
   * Returns the catalogs associated with this media package.
   *
   * @return the catalogs
   */
  Catalog[] getCatalogs();

  /**
   * Returns the catalogs that are tagged with any of the given tags or an empty array if no such elements are found. If
   * any of the tags in the <code>tags</code> collection start with a '-' character, any elements matching the tag will
   * be excluded from the returned Catalog[]. If <code>tags</code> is empty or null, all catalogs are returned.
   *
   * @param tags
   *          the tags
   * @return the catalogs
   */
  Catalog[] getCatalogsByTags(Collection<String> tags);

  /**
   * Returns the catalogs associated with this media package that matches the specified flavor.
   *
   * @param flavor
   *          the catalog type
   * @return the media package catalogs
   */
  Catalog[] getCatalogs(MediaPackageElementFlavor flavor);

  /**
   * Returns the catalogs that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   *
   * @param reference
   *          the reference
   * @return the catalogs with the specified reference
   */
  Catalog[] getCatalogs(MediaPackageReference reference);

  /**
   * Returns the catalogs that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   *
   * @param flavor
   *          the element flavor
   * @param reference
   *          the reference
   * @return the catalogs with the specified reference
   */
  Catalog[] getCatalogs(MediaPackageElementFlavor flavor, MediaPackageReference reference);

  /**
   * Returns media package elements that are neither, attachments, catalogs nor tracks.
   *
   * @return the other media package elements
   */
  MediaPackageElement[] getUnclassifiedElements();

  /**
   * Adds an arbitrary {@link URI} to this media package, utilizing a {@link MediaPackageBuilder} to create a suitable
   * media package element out of the url. If the content cannot be recognized as being either a metadata catalog or
   * multimedia track, it is added as an attachment.
   *
   * @param uri
   *          the element location
   */
  MediaPackageElement add(URI uri);

  /**
   * Adds an arbitrary {@link URI} to this media package, utilizing a {@link MediaPackageBuilder} to create a suitable
   * media package element out of the url. If the content cannot be recognized as being either a metadata catalog or
   * multimedia track, it is added as an attachment.
   *
   * @param uri
   *          the element location
   * @param type
   *          the element type
   * @param flavor
   *          the element flavor
   */
  MediaPackageElement add(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor);

  /**
   * Adds an arbitrary {@link MediaPackageElement} to this media package.
   *
   * @param element
   *          the element
   */
  void add(MediaPackageElement element);

  /**
   * Adds a track to this media package, actually <em>moving</em> the underlying file in the filesystem. Use this method
   * <em>only</em> if you do not need the track in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant performance benefits over copying the track.
   *
   * @param track
   *          the track
   */
  void add(Track track);

  /**
   * Removes the element with the given identifier from the mediapackage and returns it.
   *
   * @param id
   *          the element identifier
   */
  MediaPackageElement removeElementById(String id);

  /**
   * Removes the track from the media package.
   *
   * @param track
   *          the track
   */
  void remove(Track track);

  /**
   * Adds catalog information to this media package.
   *
   * @param catalog
   *          the catalog
   */
  void add(Catalog catalog);

  /**
   * Removes the catalog from the media package.
   *
   * @param catalog
   *          the catalog
   */
  void remove(Catalog catalog);

  /**
   * Adds an attachment to this media package.
   *
   * @param attachment
   *          the attachment
   */
  void add(Attachment attachment);

  /**
   * Removes an arbitrary media package element.
   *
   * @param element
   *          the media package element
   */
  void remove(MediaPackageElement element);

  /**
   * Removes the attachment from the media package.
   *
   * @param attachment
   *          the attachment
   */
  void remove(Attachment attachment);

  /**
   * Adds an element to this media package that represents a derived version of <code>sourceElement</code>. Examples of
   * a derived element could be an encoded version of a track or a converted version of a time text captions file.
   * <p>
   * This method will add <code>derviedElement</code> to the media package and add a reference to the original element
   * <code>sourceElement</code>. Make sure that <code>derivedElement</code> features the right flavor, so that you are
   * later able to look up derived work using {@link #getDerived(MediaPackageElement, MediaPackageElementFlavor)}.
   *
   * @param derivedElement
   *          the derived element
   * @param sourceElement
   *          the source element
   */
  void addDerived(MediaPackageElement derivedElement, MediaPackageElement sourceElement);

  /**
   * Returns those media package elements that are derivates of <code>sourceElement</code> and feature the flavor
   * <code>derivateFlavor</code>. Using this method, you could easily look up e. g. flash-encoded versions of the
   * presenter track or converted versions of a time text captions file.
   *
   * @param sourceElement
   *          the original track, catalog or attachment
   * @param derivateFlavor
   *          the derivate flavor you are looking for
   * @return the derivates
   */
  MediaPackageElement[] getDerived(MediaPackageElement sourceElement, MediaPackageElementFlavor derivateFlavor);

  /**
   * Verifies the media package consistency by checking the media package elements for mimetypes and checksums.
   *
   * @throws MediaPackageException
   *           if an error occurs while checking the media package
   */
  void verify() throws MediaPackageException;

  /**
   * Creates a deep copy of the media package.
   *
   * @return the cloned media package
   */
  Object clone();

}
