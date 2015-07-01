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

package org.opencastproject.index.service.impl.index.theme;

import org.opencastproject.index.service.impl.index.IndexObject;
import org.opencastproject.util.DateTimeSupport.UtcTimestampAdapter;
import org.opencastproject.util.IoSupport;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

/**
 * Object wrapper for a theme.
 */
@XmlType(name = "theme", namespace = IndexObject.INDEX_XML_NAMESPACE, propOrder = { "identifier", "creationDate",
        "isDefault", "description", "name", "creator", "organization", "bumperActive", "bumperFile", "trailerActive",
        "trailerFile", "titleSlideActive", "titleSlideMetadata", "titleSlideBackground", "licenseSlideActive",
        "licenseSlideDescription", "licenseSlideBackground", "watermarkActive", "watermarkFile", "watermarkPosition" })
@XmlRootElement(name = "theme", namespace = IndexObject.INDEX_XML_NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class Theme implements IndexObject {

  /** The document type */
  public static final String DOCUMENT_TYPE = "theme";

  /** The name of the surrounding XML tag to wrap a result of multiple theme */
  public static final String XML_SURROUNDING_TAG = "themes";

  /** The identifier */
  @XmlElement(name = "identifier")
  private Long identifier = null;

  /** The date and time the theme was created in UTC format e.g. 2011-07-16T20:39:05Z */
  @XmlElement(name = "creationDate")
  @XmlJavaTypeAdapter(UtcTimestampAdapter.class)
  private Date creationDate;

  /** Whether the theme is the default theme */
  @XmlElement(name = "default")
  private boolean isDefault = false;

  /** The description of the theme. */
  @XmlElement(name = "description")
  private String description = null;

  /** The name of this theme. */
  @XmlElement(name = "name")
  private String name = null;

  /** The creator of the theme */
  @XmlElement(name = "creator")
  private String creator = null;

  /** The organization of the theme */
  @XmlElement(name = "organization")
  private String organization = null;

  /** Whether the bumper set in this theme should be used */
  @XmlElement(name = "bumperActive")
  private boolean bumperActive = false;

  /** The id of the file to use as a bumper */
  @XmlElement(name = "bumperFile")
  private String bumperFile = null;

  /** Whether the trailer set in this theme should be used */
  @XmlElement(name = "trailerActive")
  private boolean trailerActive = false;

  /** The id of the file to use as a trailer for this theme. */
  @XmlElement(name = "trailerFile")
  private String trailerFile = null;

  /** Whether the title slide should be used in this theme */
  @XmlElement(name = "titleSlideActive")
  private boolean titleSlideActive = false;

  /** The definition about which metadata to use in the title slide */
  @XmlElement(name = "titleSlideMetadata")
  private String titleSlideMetadata = null;

  /** The id of the file to use as the background to the title slide */
  @XmlElement(name = "titleSlideBackground")
  private String titleSlideBackground = null;

  /** Whether the license slide in this theme should be used */
  @XmlElement(name = "licenseSlideActive")
  private boolean licenseSlideActive = false;

  /** The license description for the video */
  @XmlElement(name = "licenseSlideDescription")
  private String licenseSlideDescription = null;

  /** The id of the file to use as a background for the license */
  @XmlElement(name = "licenseSlideBackground")
  private String licenseSlideBackground = null;

  /** Whether the watermark from the theme should be applied to the video */
  @XmlElement(name = "watermarkActive")
  private boolean watermarkActive = false;

  /** The id of the file to use as watermark on the video */
  @XmlElement(name = "watermarkFile")
  private String watermarkFile = null;

  /** Dictates where the watermark should be placed on the video */
  @XmlElement(name = "watermarkPosition")
  private String watermarkPosition = null;

  /** Context for serializing and deserializing */
  private static JAXBContext context = null;

  /**
   * Required default no arg constructor for JAXB.
   */
  public Theme() {
  }

  /**
   * @param identifier
   *          the theme identifier
   * @param organization
   *          the organization
   */
  public Theme(long identifier, String organization) {
    this.identifier = identifier;
    this.organization = organization;
  }

  public long getIdentifier() {
    return identifier;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public String getOrganization() {
    return organization;
  }

  public boolean isBumperActive() {
    return bumperActive;
  }

  public void setBumperActive(boolean bumperActive) {
    this.bumperActive = bumperActive;
  }

  public String getBumperFile() {
    return bumperFile;
  }

  public void setBumperFile(String bumperFile) {
    this.bumperFile = bumperFile;
  }

  public boolean isTrailerActive() {
    return trailerActive;
  }

  public void setTrailerActive(boolean trailerActive) {
    this.trailerActive = trailerActive;
  }

  public String getTrailerFile() {
    return trailerFile;
  }

  public void setTrailerFile(String trailerFile) {
    this.trailerFile = trailerFile;
  }

  public boolean isTitleSlideActive() {
    return titleSlideActive;
  }

  public void setTitleSlideActive(boolean titleSlideActive) {
    this.titleSlideActive = titleSlideActive;
  }

  public String getTitleSlideMetadata() {
    return titleSlideMetadata;
  }

  public void setTitleSlideMetadata(String titleSlideMetadata) {
    this.titleSlideMetadata = titleSlideMetadata;
  }

  public String getTitleSlideBackground() {
    return titleSlideBackground;
  }

  public void setTitleSlideBackground(String titleSlideBackground) {
    this.titleSlideBackground = titleSlideBackground;
  }

  public boolean isLicenseSlideActive() {
    return licenseSlideActive;
  }

  public void setLicenseSlideActive(boolean licenseSlideActive) {
    this.licenseSlideActive = licenseSlideActive;
  }

  public String getLicenseSlideDescription() {
    return licenseSlideDescription;
  }

  public void setLicenseSlideDescription(String licenseSlideDescription) {
    this.licenseSlideDescription = licenseSlideDescription;
  }

  public String getLicenseSlideBackground() {
    return licenseSlideBackground;
  }

  public void setLicenseSlideBackground(String licenseSlideBackground) {
    this.licenseSlideBackground = licenseSlideBackground;
  }

  public boolean isWatermarkActive() {
    return watermarkActive;
  }

  public void setWatermarkActive(boolean watermarkActive) {
    this.watermarkActive = watermarkActive;
  }

  public String getWatermarkFile() {
    return watermarkFile;
  }

  public void setWatermarkFile(String watermarkFile) {
    this.watermarkFile = watermarkFile;
  }

  public String getWatermarkPosition() {
    return watermarkPosition;
  }

  public void setWatermarkPosition(String watermarkPosition) {
    this.watermarkPosition = watermarkPosition;
  }

  /**
   * Reads the theme from the input stream.
   *
   * @param xml
   *          the input stream
   * @return the deserialized theme
   * @throws JSONException
   * @throws XMLStreamException
   * @throws JAXBException
   */
  public static Theme valueOf(InputStream xml) throws IOException {
    try {
      if (context == null) {
        createJAXBContext();
      }
      Unmarshaller unmarshaller = context.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(xml), Theme.class).getValue();
    } catch (JAXBException e) {
      throw new IOException(e.getLinkedException() != null ? e.getLinkedException() : e);
    } finally {
      IoSupport.closeQuietly(xml);
    }
  }

  /**
   * Reads the theme from the input stream.
   *
   * @param json
   *          the input stream
   * @return the deserialized theme
   * @throws JSONException
   * @throws XMLStreamException
   * @throws JAXBException
   */
  public static Theme valueOfJson(InputStream json) throws IOException, JSONException, XMLStreamException,
          JAXBException {
    // TODO Get this to work, it is currently returning null properties for all properties.
    if (context == null) {
      createJAXBContext();
    }

    BufferedReader streamReader = new BufferedReader(new InputStreamReader(json, "UTF-8"));
    StringBuilder jsonStringBuilder = new StringBuilder();
    String inputStr;
    while ((inputStr = streamReader.readLine()) != null)
      jsonStringBuilder.append(inputStr);

    JSONObject obj = new JSONObject(jsonStringBuilder.toString());
    Configuration config = new Configuration();
    config.setSupressAtAttributes(true);
    Map<String, String> xmlToJsonNamespaces = new HashMap<String, String>(1);
    xmlToJsonNamespaces.put(IndexObject.INDEX_XML_NAMESPACE, "");
    config.setXmlToJsonNamespaces(xmlToJsonNamespaces);
    MappedNamespaceConvention con = new MappedNamespaceConvention(config);
    XMLStreamReader xmlStreamReader = new MappedXMLStreamReader(obj, con);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    Theme event = (Theme) unmarshaller.unmarshal(xmlStreamReader);
    return event;
  }

  /**
   * Initialize the JAXBContext.
   */
  private static void createJAXBContext() throws JAXBException {
    context = JAXBContext.newInstance(Theme.class);
  }

  /**
   * Serializes the theme.
   *
   * @return the serialized theme
   */
  @Override
  public String toJSON() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      Marshaller marshaller = Theme.context.createMarshaller();

      Configuration config = new Configuration();
      config.setSupressAtAttributes(true);
      MappedNamespaceConvention con = new MappedNamespaceConvention(config);
      StringWriter writer = new StringWriter();
      XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(con, writer) {
        @Override
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }

        @Override
        public void writeStartElement(String uri, String local) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }

        @Override
        public void setPrefix(String pfx, String uri) throws XMLStreamException {
        }

        @Override
        public void setDefaultNamespace(String uri) throws XMLStreamException {
        }
      };

      marshaller.marshal(this, xmlStreamWriter);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * Serializes the theme to an XML format.
   *
   * @return A String with this theme's content as XML.
   */
  public String toXML() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      StringWriter writer = new StringWriter();
      Marshaller marshaller = Theme.context.createMarshaller();
      marshaller.marshal(this, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

}
