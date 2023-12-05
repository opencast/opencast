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

package org.opencastproject.mediapackage.attachment;

import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.UnknownFileTypeException;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Basic implementation of an attachment.
 */
@XmlType(name = "attachment", namespace = "http://mediapackage.opencastproject.org")
@XmlRootElement(name = "attachment", namespace = "http://mediapackage.opencastproject.org")
public class AttachmentImpl extends AbstractMediaPackageElement implements Attachment {

  /** Serial version UID */
  private static final long serialVersionUID = 6626531251856698138L;

  /** The object properties */
  @XmlElement(name = "additionalProperties")
  @XmlJavaTypeAdapter(PropertiesXmlAdapter.class)
  protected Map<String, String> properties = null;

  /**
   * Needed by JAXB
   */
  public AttachmentImpl() {
    super(Type.Attachment, null, null);
    properties = new HashMap<>();
  }

  /**
   * Creates an attachment.
   *
   * @param identifier
   *          the attachment identifier
   * @param flavor
   *          the attachment type
   * @param uri
   *          the attachments location
   * @param size
   *          the attachments size
   * @param checksum
   *          the attachments checksum
   * @param mimeType
   *          the attachments mime type
   */
  protected AttachmentImpl(String identifier, MediaPackageElementFlavor flavor, URI uri, Long size, Checksum checksum,
          MimeType mimeType) {
    super(identifier, Type.Attachment, flavor, uri, size, checksum, mimeType);
    if (uri != null)
      try {
        this.setMimeType(MimeTypes.fromURI(uri));
      } catch (UnknownFileTypeException e) { }
  }

  /**
   * Creates an attachment.
   *
   * @param uri
   *          the attachments location
   */
  protected AttachmentImpl(URI uri) {
    this(null, null, uri, null, null, null);
  }

  /**
   * Creates a new attachment from the url.
   *
   * @param uri
   *          the attachment location
   * @return the attachment
   */
  public static Attachment fromURI(URI uri) {
    return new AttachmentImpl(uri);
  }

  @Override
  public Map<String, String> getProperties() {
    if (properties == null)
      properties = new HashMap<String, String>();

    return properties;
  }

  /**
   * JAXB properties xml adapter class.
   */
  private static class PropertiesXmlAdapter extends XmlAdapter<PropertiesAdapter, Map<String, String>> {

    @Override
    public Map<String, String> unmarshal(PropertiesAdapter pa) throws Exception {
      Map<String, String> properties = new HashMap<>();
      if (pa != null) {
        for (Property p : pa.propertiesList) {
          properties.put(p.key, p.value);
        }
      }
      return properties;
    }

    @Override
    public PropertiesAdapter marshal(Map<String, String> p) throws Exception {
      if (p == null || p.size() == 0) return null;

      PropertiesAdapter pa = new PropertiesAdapter();
        for (String key : p.keySet()) {
          pa.propertiesList.add(new Property(key, p.get(key)));
        }
      return pa;
    }
  }

  /**
   * Properties map to list of entries adapter class.
   */
  private static class PropertiesAdapter {
    @XmlElement(name = "property")
    private List<Property> propertiesList;

    PropertiesAdapter() {
      this(new LinkedList<Property>());
    }

    PropertiesAdapter(List<Property> propertiesList) {
      this.propertiesList = propertiesList;
    }
  }

  /**
   * Properties entry adapter class.
   */
  private static class Property {
    @XmlAttribute(name = "key")
    private String key;
    @XmlValue
    private String value;

    Property() {
      // Default constructor
    }

    Property(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  public static class Adapter extends XmlAdapter<AttachmentImpl, Attachment> {
    public AttachmentImpl marshal(Attachment mp) throws Exception {
      return (AttachmentImpl) mp;
    }

    public Attachment unmarshal(AttachmentImpl mp) throws Exception {
      return mp;
    }
  }
}
