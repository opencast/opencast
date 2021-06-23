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

package org.opencastproject.elasticsearch.index.theme;

import org.opencastproject.elasticsearch.api.SearchMetadata;
import org.opencastproject.elasticsearch.impl.SearchMetadataCollection;
import org.opencastproject.elasticsearch.index.IndexObject;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.DateTimeSupport.UtcTimestampAdapter;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.XmlSafeParser;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
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

/**
 * Object wrapper for a theme.
 */
@XmlType(name = "theme", namespace = IndexObject.INDEX_XML_NAMESPACE, propOrder = { "identifier", "creationDate",
        "isDefault", "description", "name", "creator", "organization", "bumperActive", "bumperFile", "trailerActive",
        "trailerFile", "titleSlideActive", "titleSlideMetadata", "titleSlideBackground", "licenseSlideActive",
        "licenseSlideDescription", "licenseSlideBackground", "watermarkActive", "watermarkFile", "watermarkPosition" })
@XmlRootElement(name = "theme", namespace = IndexObject.INDEX_XML_NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class IndexTheme implements IndexObject {

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
  public IndexTheme() {
  }

  /**
   * @param identifier
   *          the theme identifier
   * @param organization
   *          the organization
   */
  public IndexTheme(long identifier, String organization) {
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
   * @throws IOException
   */
  public static IndexTheme valueOf(InputStream xml) throws IOException {
    try {
      if (context == null) {
        createJAXBContext();
      }
      Unmarshaller unmarshaller = context.createUnmarshaller();
      return unmarshaller.unmarshal(XmlSafeParser.parse(xml), IndexTheme.class).getValue();
    } catch (JAXBException e) {
      throw new IOException(e.getLinkedException() != null ? e.getLinkedException() : e);
    } catch (SAXException e) {
      throw new IOException(e);
    } finally {
      IoSupport.closeQuietly(xml);
    }
  }

  /**
   * Initialize the JAXBContext.
   */
  private static void createJAXBContext() throws JAXBException {
    context = JAXBContext.newInstance(IndexTheme.class);
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
      Marshaller marshaller = IndexTheme.context.createMarshaller();
      marshaller.marshal(this, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * Creates a search result item based on the data returned from the search index.
   *
   * @param metadata
   *          the search metadata
   * @return the search result item
   * @throws IOException
   *           if unmarshalling fails
   */
  public static IndexTheme fromSearchMetadata(SearchMetadataCollection metadata) throws IOException {
    Map<String, SearchMetadata<?>> metadataMap = metadata.toMap();
    String themeXml = (String) metadataMap.get(ThemeIndexSchema.OBJECT).getValue();
    return IndexTheme.valueOf(IOUtils.toInputStream(themeXml));
  }

  /**
   * Creates search metadata from a theme such that the theme can be stored in the search index.
   *
   * @return the set of metadata
   */
  public SearchMetadataCollection toSearchMetadata() {
    SearchMetadataCollection metadata = new SearchMetadataCollection(Long.toString(getIdentifier()).concat(
            getOrganization()), IndexTheme.DOCUMENT_TYPE);
    // Mandatory fields
    metadata.addField(ThemeIndexSchema.ID, getIdentifier(), true);
    metadata.addField(ThemeIndexSchema.ORGANIZATION, getOrganization(), false);
    metadata.addField(ThemeIndexSchema.OBJECT, toXML(), false);

    // Optional fields
    if (StringUtils.isNotBlank(getName())) {
      metadata.addField(ThemeIndexSchema.NAME, getName(), true);
    }

    if (StringUtils.isNotBlank(getDescription())) {
      metadata.addField(ThemeIndexSchema.DESCRIPTION, getDescription(), true);
    }

    metadata.addField(ThemeIndexSchema.DEFAULT, isDefault(), false);

    if (getCreationDate() != null) {
      metadata.addField(ThemeIndexSchema.CREATION_DATE,
              DateTimeSupport.toUTC(getCreationDate().getTime()), true);
    }

    if (StringUtils.isNotBlank(getCreator())) {
      metadata.addField(ThemeIndexSchema.CREATOR, getCreator(), true);
    }

    metadata.addField(ThemeIndexSchema.BUMPER_ACTIVE, isBumperActive(), false);

    if (StringUtils.isNotBlank(getBumperFile())) {
      metadata.addField(ThemeIndexSchema.BUMPER_FILE, getBumperFile(), false);
    }

    metadata.addField(ThemeIndexSchema.TRAILER_ACTIVE, isTrailerActive(), false);

    if (StringUtils.isNotBlank(getTrailerFile())) {
      metadata.addField(ThemeIndexSchema.TRAILER_FILE, getTrailerFile(), false);
    }

    metadata.addField(ThemeIndexSchema.TITLE_SLIDE_ACTIVE, isTrailerActive(), false);

    if (StringUtils.isNotBlank(getTitleSlideMetadata())) {
      metadata.addField(ThemeIndexSchema.TITLE_SLIDE_METADATA, getTitleSlideMetadata(), false);
    }

    if (StringUtils.isNotBlank(getTitleSlideBackground())) {
      metadata.addField(ThemeIndexSchema.TITLE_SLIDE_BACKGROUND, getTitleSlideBackground(), false);
    }

    metadata.addField(ThemeIndexSchema.LICENSE_SLIDE_ACTIVE, isLicenseSlideActive(), false);

    if (StringUtils.isNotBlank(getLicenseSlideDescription())) {
      metadata.addField(ThemeIndexSchema.LICENSE_SLIDE_DESCRIPTION, getLicenseSlideDescription(), false);
    }

    if (StringUtils.isNotBlank(getLicenseSlideBackground())) {
      metadata.addField(ThemeIndexSchema.LICENSE_SLIDE_BACKGROUND, getLicenseSlideBackground(), false);
    }

    metadata.addField(ThemeIndexSchema.WATERMARK_ACTIVE, isWatermarkActive(), false);

    if (StringUtils.isNotBlank(getWatermarkFile())) {
      metadata.addField(ThemeIndexSchema.WATERMARK_FILE, getWatermarkFile(), false);
    }

    if (StringUtils.isNotBlank(getWatermarkPosition())) {
      metadata.addField(ThemeIndexSchema.WATERMARK_POSITION, getWatermarkPosition(), false);
    }
    return metadata;
  }
}
