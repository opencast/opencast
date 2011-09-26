/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.mediapackage;

import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;

/**
 * This class provides base functionality for media package elements.
 */
@XmlTransient
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractMediaPackageElement implements MediaPackageElement, Serializable {

  /** Serial version uid */
  private static final long serialVersionUID = 1L;

  /** The element identifier */
  @XmlID
  @XmlAttribute(name = "id")
  protected String id = null;

  /** The element's type whithin the manifest: Track, Catalog etc. */
  protected Type elementType = null;

  /** The element's description */
  protected String description = null;

  /** The element's mime type, e. g. 'audio/mp3' */
  @XmlElement(name = "mimetype")
  protected MimeType mimeType = null;

  /** The element's type, e. g. 'track/slide' */
  @XmlAttribute(name = "type")
  protected MediaPackageElementFlavor flavor = null;

  /** The tags */
  @XmlElementWrapper(name = "tags")
  @XmlElement(name = "tag")
  protected SortedSet<String> tags = new TreeSet<String>();

  /** The element's location */
  @XmlElement(name = "url")
  protected URI uri = null;

  /** Size in bytes */
  protected long size = -1L;

  /** The element's checksum */
  @XmlElement(name = "checksum")
  protected Checksum checksum = null;

  /** The parent media package */
  protected MediaPackage mediaPackage = null;

  /** The optional reference to other elements or series */
  @XmlAttribute(name = "ref")
  protected MediaPackageReference reference = null;

  /** Needed by JAXB */
  protected AbstractMediaPackageElement() {
  }

  /**
   * Creates a new media package element.
   * 
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param uri
   *          the elements location
   */
  protected AbstractMediaPackageElement(Type elementType, MediaPackageElementFlavor flavor, URI uri) {
    this(null, elementType, flavor, uri, -1, null, null);
  }

  /**
   * Creates a new media package element.
   * 
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param uri
   *          the elements location
   * @param size
   *          the element size in bytes
   * @param checksum
   *          the element checksum
   * @param mimeType
   *          the element mime type
   */
  protected AbstractMediaPackageElement(Type elementType, MediaPackageElementFlavor flavor, URI uri, long size,
          Checksum checksum, MimeType mimeType) {
    this(null, elementType, flavor, uri, size, checksum, mimeType);
  }

  /**
   * Creates a new media package element.
   * 
   * @param id
   *          the element identifier withing the package
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param uri
   *          the elements location
   * @param size
   *          the element size in bytes
   * @param checksum
   *          the element checksum
   * @param mimeType
   *          the element mime type
   */
  protected AbstractMediaPackageElement(String id, Type elementType, MediaPackageElementFlavor flavor, URI uri,
          long size, Checksum checksum, MimeType mimeType) {
    if (elementType == null)
      throw new IllegalArgumentException("Argument 'elementType' is null");
    this.id = id;
    this.elementType = elementType;
    this.flavor = flavor;
    this.mimeType = mimeType;
    this.uri = uri;
    this.checksum = checksum;
    this.tags = new TreeSet<String>();
  }

  /**
   * Sets the element id.
   * 
   * @param id
   *          the new id
   */
  public void setIdentifier(String id) {
    this.id = id;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#getIdentifier()
   */
  public String getIdentifier() {
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageElement#addTag(java.lang.String)
   */
  public void addTag(String tag) {
    if (tag == null)
      throw new IllegalArgumentException("Tag must not be null");
    tags.add(tag);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageElement#removeTag(java.lang.String)
   */
  public void removeTag(String tag) {
    if (tag == null)
      return;
    tags.remove(tag);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageElement#containsTag(java.lang.String)
   */
  @Override
  public boolean containsTag(String tag) {
    if (tag == null || tags == null)
      return false;
    return tags.contains(tag);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageElement#containsTag(java.util.Collection)
   */
  @Override
  public boolean containsTag(Collection<String> tags) {
    if (tags == null || tags.size() == 0)
      return true;
    for (String tag : tags) {
      if (containsTag(tag))
        return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageElement#getTags()
   */
  public String[] getTags() {
    return tags.toArray(new String[tags.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.MediaPackageElement#clearTags()
   */
  @Override
  public void clearTags() {
    if (tags != null)
      tags.clear();
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#getMediaPackage()
   */
  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#getElementType()
   */
  public Type getElementType() {
    return elementType;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#getElementDescription()
   */
  public String getElementDescription() {
    return (description != null) ? description : uri.toString();
  }

  /**
   * Sets the element name.
   * 
   * @param name
   *          the name
   */
  public void setElementDescription(String name) {
    this.description = name;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#getReference()
   */
  public MediaPackageReference getReference() {
    return reference;
  }

  /**
   * Sets the media package element's reference.
   * 
   * @param reference
   *          the reference
   */
  public void setReference(MediaPackageReference reference) {
    this.reference = reference;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#getURI()
   */
  public URI getURI() {
    return uri;
  }

  /**
   * Sets the url that is used to store the media package element.
   * <p>
   * Make sure you know what you are doing, since usually, the media package will take care of the elements locations.
   * 
   * @param uri
   *          the elements url
   */
  public void setURI(URI uri) {
    this.uri = uri;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#getChecksum()
   */
  public Checksum getChecksum() {
    return checksum;
  }

  /**
   * Sets the element checksum.
   * 
   * @param checksum
   *          the checksum
   */
  public void setChecksum(Checksum checksum) {
    this.checksum = checksum;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#getMimeType()
   */
  public MimeType getMimeType() {
    return mimeType;
  }

  /**
   * Sets the element mimetype.
   * 
   * @param mimeType
   *          the element mimetype
   */
  public void setMimeType(MimeType mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * Sets the element's flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public void setFlavor(MediaPackageElementFlavor flavor) {
    this.flavor = flavor;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#getFlavor()
   */
  public MediaPackageElementFlavor getFlavor() {
    return flavor;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#getSize()
   */
  public long getSize() {
    return size;
  }

  /**
   * Sets the element size in bytes.
   * 
   * @param size
   *          size in bytes
   */
  public void setSize(long size) {
    this.size = size;
  }

  /**
   * Sets the parent media package.
   * <p>
   * <b>Note</b> This method is only used by the media package and should not be called from elsewhere.
   * 
   * @param mediaPackage
   *          the parent media package
   */
  void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackage = mediaPackage;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#referTo(org.opencastproject.mediapackage.MediaPackage)
   */
  public void referTo(MediaPackage mediaPackage) {
    referTo(new MediaPackageReferenceImpl(mediaPackage));
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#referTo(org.opencastproject.mediapackage.MediaPackageElement)
   */
  public void referTo(MediaPackageElement element) {
    referTo(new MediaPackageReferenceImpl(element));
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#referTo(org.opencastproject.mediapackage.MediaPackageReference)
   */
  public void referTo(MediaPackageReference reference) {
    // TODO: Check reference consistency
    this.reference = reference;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#clearReference()
   */
  public void clearReference() {
    this.reference = null;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElement#verify()
   */
  public void verify() throws MediaPackageException {
    // TODO: Check availability at url
    // TODO: Download (?) and check checksum
    // Checksum c = calculateChecksum();
    // if (checksum != null && !checksum.equals(c)) {
    // throw new MediaPackageException("Checksum mismatch for " + this);
    // }
    // checksum = c;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(MediaPackageElement o) {
    return uri.toString().compareTo(o.getURI().toString());
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MediaPackageElement))
      return false;
    MediaPackageElement e = (MediaPackageElement) obj;
    if (mediaPackage != null && e.getMediaPackage() != null && !mediaPackage.equals(e.getMediaPackage()))
      return false;
    if (id != null && !id.equals(e.getIdentifier()))
      return false;
    if (uri != null && !uri.equals(e.getURI()))
      return false;
    return true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((mediaPackage == null) ? 0 : mediaPackage.hashCode());
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    return result;
  }

  /**
   * @see org.opencastproject.mediapackage.ManifestContributor#toManifest(org.w3c.dom.Document,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  public Node toManifest(Document document, MediaPackageSerializer serializer) {
    Element node = document.createElement(elementType.toString().toLowerCase());
    if (id != null)
      node.setAttribute("id", id);

    // Flavor
    if (flavor != null)
      node.setAttribute("type", flavor.toString());

    // Reference
    if (reference != null)
      if (mediaPackage == null || !reference.matches(new MediaPackageReferenceImpl(mediaPackage)))
        node.setAttribute("ref", reference.toString());

    // Description
    if (description != null) {
      Element descriptionNode = document.createElement("description");
      descriptionNode.appendChild(document.createTextNode(description));
      node.appendChild(descriptionNode);
    }

    // Tags
    if (tags.size() > 0) {
      Element tagsNode = document.createElement("tags");
      node.appendChild(tagsNode);
      for (String tag : tags) {
        Element tagNode = document.createElement("tag");
        tagsNode.appendChild(tagNode);
        tagNode.appendChild(document.createTextNode(tag));
      }
    }

    // Url
    Element urlNode = document.createElement("url");
    String urlValue = (serializer != null) ? serializer.encodeURI(uri) : uri.toString();
    urlNode.appendChild(document.createTextNode(urlValue));
    node.appendChild(urlNode);

    // MimeType
    if (mimeType != null) {
      Element mimeNode = document.createElement("mimetype");
      mimeNode.appendChild(document.createTextNode(mimeType.toString()));
      node.appendChild(mimeNode);
    }

    // Size
    if (size != -1) {
      Element sizeNode = document.createElement("size");
      sizeNode.appendChild(document.createTextNode(Long.toString(size)));
      node.appendChild(sizeNode);
    }

    // Checksum
    if (checksum != null) {
      Element checksumNode = document.createElement("checksum");
      checksumNode.setAttribute("type", checksum.getType().getName());
      checksumNode.appendChild(document.createTextNode(checksum.getValue()));
      node.appendChild(checksumNode);
    }

    return node;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String s = (description != null) ? description : uri.toString();
    return s.toLowerCase();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#clone()
   */
  public Object clone() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      Marshaller marshaller = MediaPackageImpl.context.createMarshaller();
      marshaller.marshal(this, out);
      Unmarshaller unmarshaller = MediaPackageImpl.context.createUnmarshaller();
      ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
      return unmarshaller.unmarshal(in);
    } catch (JAXBException e) {
      throw new RuntimeException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

}
