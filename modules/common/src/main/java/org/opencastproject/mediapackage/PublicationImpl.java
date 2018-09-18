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

import org.opencastproject.util.MimeType;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "publication", namespace = "http://mediapackage.opencastproject.org")
@XmlRootElement(name = "publication", namespace = "http://mediapackage.opencastproject.org")
public class PublicationImpl extends AbstractMediaPackageElement implements Publication {
  /** Serial version UID */
  private static final long serialVersionUID = 11151970L;

  @XmlAttribute(name = "channel", required = true)
  private String channel;

  @XmlElementWrapper(name = "media")
  @XmlElement(name = "track")
  private final List<Track> tracks = new ArrayList<Track>();

  @XmlElementWrapper(name = "attachments")
  @XmlElement(name = "attachment")
  private final List<Attachment> attachments = new ArrayList<Attachment>();

  @XmlElementWrapper(name = "metadata")
  @XmlElement(name = "catalog")
  private final List<Catalog> catalogs = new ArrayList<Catalog>();

  /** JAXB constructor */
  public PublicationImpl() {
    this.elementType = Type.Publication;
  }

  public PublicationImpl(String id, String channel, URI uri, MimeType mimeType) {
    this();
    setURI(uri);
    setIdentifier(id);
    setMimeType(mimeType);
    this.channel = channel;
  }

  public static Publication publication(String id, String channel, URI uri, MimeType mimeType) {
    return new PublicationImpl(id, channel, uri, mimeType);
  }

  @Override
  public String getChannel() {
    return channel;
  }

  @Override
  public Track[] getTracks() {
    return tracks.toArray(new Track[tracks.size()]);
  }

  @Override
  public void addTrack(Track track) {
    // Check (uniqueness of) track identifier
    String id = track.getIdentifier();
    if (id == null) {
      track.setIdentifier(createElementIdentifier());
    }
    tracks.add(track);
  }

  @Override
  public Attachment[] getAttachments() {
    return attachments.toArray(new Attachment[attachments.size()]);
  }

  @Override
  public void addAttachment(Attachment attachment) {
    // Check (uniqueness of) attachment identifier
    String id = attachment.getIdentifier();
    if (id == null) {
      attachment.setIdentifier(createElementIdentifier());
    }
    attachments.add(attachment);
  }

  @Override
  public void removeAttachmentById(String attachmentId) {
    attachments.removeIf(a -> a.getIdentifier().equals(attachmentId));
  }

  @Override
  public Catalog[] getCatalogs() {
    return catalogs.toArray(new Catalog[catalogs.size()]);
  }

  @Override
  public void addCatalog(Catalog catalog) {
    // Check (uniqueness of) catalog identifier
    String id = catalog.getIdentifier();
    if (id == null) {
      catalog.setIdentifier(createElementIdentifier());
    }
    catalogs.add(catalog);
  }

  @Override
  public void setFlavor(MediaPackageElementFlavor flavor) {
    throw new UnsupportedOperationException("Unable to set the flavor of publications.");
  }

  /**
   * Returns a media package element identifier. The identifier will be unique within the media package.
   *
   * @return the element identifier
   */
  private String createElementIdentifier() {
    return UUID.randomUUID().toString();
  }

  /**
   * Adds a {@link MediaPackageElement} to this publication by determining its type.
   *
   * @param publication
   *          The {@link Publication} to add the {@link MediaPackageElement} to.
   * @param element
   *          The {@link MediaPackageElement} to add. If it is not a {@link Attachment}, {@link Catalog} or
   *          {@link Track} it will not be added to the {@link Publication}.
   */
  public static void addElementToPublication(Publication publication, MediaPackageElement element) {
    if (MediaPackageElement.Type.Track.equals(element.getElementType())) {
      publication.addTrack((Track) element);
    } else if (MediaPackageElement.Type.Catalog.equals(element.getElementType())) {
      publication.addCatalog((Catalog) element);
    } else if (MediaPackageElement.Type.Attachment.equals(element.getElementType())) {
      publication.addAttachment((Attachment) element);
    }
  }

  /** JAXB adapter */
  public static class Adapter extends XmlAdapter<PublicationImpl, Publication> {
    @Override
    public PublicationImpl marshal(Publication e) throws Exception {
      return (PublicationImpl) e;
    }

    @Override
    public Publication unmarshal(PublicationImpl e) throws Exception {
      return e;
    }
  }
}
