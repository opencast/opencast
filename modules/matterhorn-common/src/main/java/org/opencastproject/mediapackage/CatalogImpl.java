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
import org.opencastproject.util.MimeTypes;

import java.io.File;
import java.net.URI;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * This is a basic implementation for handling simple catalogs of metadata.
 */
@XmlRootElement(name = "catalog", namespace = "http://mediapackage.opencastproject.org")
@XmlType(name = "catalog", namespace = "http://mediapackage.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class CatalogImpl extends AbstractMediaPackageElement implements Catalog {

  /** Serial version UID */
  private static final long serialVersionUID = -908525367616L;

  /** Needed by JAXB */
  protected CatalogImpl() {
    // default to text/xml mimetype
    super(Type.Catalog, null, null, null, null, MimeTypes.parseMimeType("text/xml"));
  }

  /**
   * Creates an abstract metadata container.
   *
   * @param id
   *          the element identifier withing the package
   * @param flavor
   *          the catalog flavor
   * @param uri
   *          the document location
   * @param size
   *          the catalog size in bytes
   * @param checksum
   *          the catalog checksum
   * @param mimeType
   *          the catalog mime type
   */
  protected CatalogImpl(String id, MediaPackageElementFlavor flavor, URI uri, long size, Checksum checksum,
          MimeType mimeType) {
    super(Type.Catalog, flavor, uri, size, checksum, mimeType);
  }

  /**
   * Creates an abstract metadata container.
   *
   * @param flavor
   *          the catalog flavor
   * @param uri
   *          the document location
   * @param size
   *          the catalog size in bytes
   * @param checksum
   *          the catalog checksum
   * @param mimeType
   *          the catalog mime type
   */
  protected CatalogImpl(MediaPackageElementFlavor flavor, URI uri, long size, Checksum checksum, MimeType mimeType) {
    this(null, flavor, uri, size, checksum, mimeType);
  }

  /**
   * Reads the metadata from the specified file and returns it encapsulated in a {@link Catalog} object.
   *
   * @param catalog
   *          the dublin core metadata container file
   * @return the dublin core object
   */
  public static Catalog fromFile(File catalog) {
    return fromURI(catalog.toURI());
  }

  /**
   * Reads the metadata from the specified file and returns it encapsulated in a {@link Catalog} object.
   *
   * @param uri
   *          the dublin core metadata container file
   * @return the dublin core object
   */
  public static Catalog fromURI(URI uri) {
    CatalogImpl cat = new CatalogImpl();
    cat.setURI(uri);
    return cat;
  }

  public static class Adapter extends XmlAdapter<CatalogImpl, Catalog> {
    public CatalogImpl marshal(Catalog cat) throws Exception {
      return (CatalogImpl) cat;
    }

    public Catalog unmarshal(CatalogImpl cat) throws Exception {
      return cat;
    }
  }

  /**
   * @return a new catalog instance
   */
  public static Catalog newInstance() {
    return new CatalogImpl();
  }

}
