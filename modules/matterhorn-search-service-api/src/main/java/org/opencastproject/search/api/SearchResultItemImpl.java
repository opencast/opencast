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

package org.opencastproject.search.api;

import org.opencastproject.mediapackage.MediaPackage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This class models an item in the search result. It represents a 'video' or 'series' object. It does not, however,
 * represent the complete solr document. Authorization information, for instance, is not serialized.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "result", namespace = "http://search.opencastproject.org")
@XmlRootElement(name = "result", namespace = "http://search.opencastproject.org")
public class SearchResultItemImpl implements SearchResultItem {

  /** Serial version id **/
  private static final long serialVersionUID = 1L;

  /** Media identificator. **/
  @XmlID
  @XmlAttribute(name = "id")
  private String id = "";

  @XmlAttribute(name = "org")
  private String organization;

  /** The media package */
  @XmlElement(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org")
  private MediaPackage mediaPackage = null;

  /** Dublin core field 'dc:extent' */
  @XmlElement
  private long dcExtent = -1;

  /** Dublin core field 'dc:title' */
  @XmlElement
  private String dcTitle = null;

  /** Dublin core field 'dc:subject' */
  @XmlElement
  private String dcSubject = null;

  /** Dublin core field 'dc:creator' */
  @XmlElement
  private String dcCreator = null;

  /** Dublin core field 'cd:publisher' */
  @XmlElement
  private String dcPublisher = null;

  /** Dublin core field 'dc:contributor' */
  @XmlElement
  private String dcContributor = null;

  /** Dublin core field 'dc:abstract' */
  @XmlElement
  private String dcAbstract = null;

  /** Dublin core field 'dc:description' */
  @XmlElement
  private String dcDescription = null;

  /** Dublin core field 'dc:created' */
  @XmlElement
  private Date dcCreated = null;

  /** Dublin core field 'dc:availablefrom' */
  @XmlElement
  private Date dcAvailableFrom = null;

  /** Dublin core field 'dc:availableto' */
  @XmlElement
  private Date dcAvailableTo = null;

  /** Dublin core field 'dc:language' */
  @XmlElement
  private String dcLanguage = null;

  /** Dublin core field 'dc:rightsholder' */
  @XmlElement
  private String dcRightsHolder = null;

  /** Dublin core field 'dc:spacial' */
  @XmlElement
  private String dcSpatial = null;

  /** Dublin core field 'dc:temporal' */
  @XmlElement
  private String dcTemporal = null;

  /** Dublin core field 'dc:isPartOf' */
  @XmlElement
  private String dcIsPartOf = null;

  /** Dublin core field 'dc:replaces' */
  @XmlElement
  private String dcReplaces = null;

  /** Dublin core field 'dc:type' */
  @XmlElement
  private String dcType = null;

  /** Dublin core field 'dc:accessrights' */
  @XmlElement
  private String dcAccessRights = null;

  /** Dublin core field 'dc:license' */
  @XmlElement
  private String dcLicense = null;

  /** Field oc_mediapackage */
  private String ocMediapackage;

  /** Search result item type */
  @XmlElement
  private SearchResultItemType mediaType = null;

  /** Media keyword list */
  @XmlElementWrapper(name = "keywords")
  private List<String> keywords = new ArrayList<String>();

  /** The cover url. **/
  @XmlElement
  private String cover = null;

  /** Media date of last modification in milliseconds **/
  @XmlElement
  private Date modified = null;

  /** Result ranking score **/
  @XmlElement
  private double score = -1;

  /** Media segment list **/
  @XmlElementWrapper(name = "segments")
  @XmlElement(name = "segment")
  private SortedSet<MediaSegmentImpl> mediaSegments = null;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getId()
   */
  public String getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getOrganization()
   */
  @Override
  public String getOrganization() {
    return organization;
  }

  /**
   * @param organization
   *          the organization to set
   */
  public void setOrganization(String organization) {
    this.organization = organization;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcExtent()
   */
  public long getDcExtent() {
    return dcExtent;
  }

  /**
   * @param dcExtent
   *          the dcExtent to set
   */
  public void setDcExtent(long dcExtent) {
    this.dcExtent = dcExtent;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcTitle()
   */
  public String getDcTitle() {
    return dcTitle;
  }

  /**
   * @param dcTitle
   *          the dcTitle to set
   */
  public void setDcTitle(String dcTitle) {
    this.dcTitle = dcTitle;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcSubject()
   */
  public String getDcSubject() {
    return dcSubject;
  }

  /**
   * @param dcSubject
   *          the dcSubject to set
   */
  public void setDcSubject(String dcSubject) {
    this.dcSubject = dcSubject;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcCreator()
   */
  public String getDcCreator() {
    return dcCreator;
  }

  /**
   * @param dcCreator
   *          the dcCreator to set
   */
  public void setDcCreator(String dcCreator) {
    this.dcCreator = dcCreator;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcPublisher()
   */
  public String getDcPublisher() {
    return dcPublisher;
  }

  /**
   * @param dcPublisher
   *          the dcPublisher to set
   */
  public void setDcPublisher(String dcPublisher) {
    this.dcPublisher = dcPublisher;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcContributor()
   */
  public String getDcContributor() {
    return dcContributor;
  }

  /**
   * @param dcContributor
   *          the dcContributor to set
   */
  public void setDcContributor(String dcContributor) {
    this.dcContributor = dcContributor;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcAbstract()
   */
  public String getDcAbstract() {
    return dcAbstract;
  }

  /**
   * @param dcAbstract
   *          the dcAbtract to set
   */
  public void setDcAbstract(String dcAbstract) {
    this.dcAbstract = dcAbstract;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcDescription()
   */
  public String getDcDescription() {
    return dcDescription;
  }

  /**
   * @param dcDescription
   *          the dcDescription to set
   */
  public void setDcDescription(String dcDescription) {
    this.dcDescription = dcDescription;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcCreated()
   */
  public Date getDcCreated() {
    return dcCreated;
  }

  /**
   * @param dcCreated
   *          the dcCreated to set
   */
  public void setDcCreated(Date dcCreated) {
    this.dcCreated = dcCreated;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcAvailableFrom()
   */
  public Date getDcAvailableFrom() {
    return dcAvailableFrom;
  }

  /**
   * @param dcAvailableFrom
   *          the dcAvailableFrom to set
   */
  public void setDcAvailableFrom(Date dcAvailableFrom) {
    this.dcAvailableFrom = dcAvailableFrom;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcAvailableTo()
   */
  public Date getDcAvailableTo() {
    return dcAvailableTo;
  }

  /**
   * @param dcAvailableTo
   *          the dcAvailableTo to set
   */
  public void setDcAvailableTo(Date dcAvailableTo) {
    this.dcAvailableTo = dcAvailableTo;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcLanguage()
   */
  public String getDcLanguage() {
    return dcLanguage;
  }

  /**
   * @param dcLanguage
   *          the dcLanguage to set
   */
  public void setDcLanguage(String dcLanguage) {
    this.dcLanguage = dcLanguage;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcRightsHolder()
   */
  public String getDcRightsHolder() {
    return dcRightsHolder;
  }

  /**
   * @param dcRightsHolder
   *          the dcRightsHolder to set
   */
  public void setDcRightsHolder(String dcRightsHolder) {
    this.dcRightsHolder = dcRightsHolder;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcSpatial()
   */
  public String getDcSpatial() {
    return dcSpatial;
  }

  /**
   * @param dcSpatial
   *          the dcSpatial to set
   */
  public void setDcSpatial(String dcSpatial) {
    this.dcSpatial = dcSpatial;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcTemporal()
   */
  public String getDcTemporal() {
    return dcTemporal;
  }

  /**
   * @param dcTemporal
   *          the dcTemporal to set
   */
  public void setDcTemporal(String dcTemporal) {
    this.dcTemporal = dcTemporal;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcIsPartOf()
   */
  public String getDcIsPartOf() {
    return dcIsPartOf;
  }

  /**
   * @param dcIsPartOf
   *          the dcIsPartOf to set
   */
  public void setDcIsPartOf(String dcIsPartOf) {
    this.dcIsPartOf = dcIsPartOf;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcReplaces()
   */
  public String getDcReplaces() {
    return dcReplaces;
  }

  /**
   * @param dcReplaces
   *          the dcReplaces to set
   */
  public void setDcReplaces(String dcReplaces) {
    this.dcReplaces = dcReplaces;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcType()
   */
  public String getDcType() {
    return dcType;
  }

  /**
   * @param dcType
   *          the dcType to set
   */
  public void setDcType(String dcType) {
    this.dcType = dcType;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcAccessRights()
   */
  public String getDcAccessRights() {
    return dcAccessRights;
  }

  /**
   * @param dcAccessRights
   *          the dcAccessRights to set
   */
  public void setDcAccessRights(String dcAccessRights) {
    this.dcAccessRights = dcAccessRights;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getDcLicense()
   */
  public String getDcLicense() {
    return dcLicense;
  }

  /**
   * @param dcLicense
   *          the dcLicense to set
   */
  public void setDcLicense(String dcLicense) {
    this.dcLicense = dcLicense;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getType()
   */
  public SearchResultItemType getType() {
    return mediaType;
  }

  @Override
  public String getOcMediapackage() {
    return ocMediapackage;
  }

  public void setOcMediapackage(String mediapackage) {
    this.ocMediapackage = mediapackage;
  }

  /**
   * @param mediaType
   *          the mediaType to set
   */
  public void setMediaType(SearchResultItemType mediaType) {
    this.mediaType = mediaType;
  }

  /**
   * Sets the media package that is associated with the search result item.
   *
   * @param mediaPackage
   *          the media package
   */
  public void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackage = mediaPackage;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getMediaPackage()
   */
  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getKeywords()
   */
  public String[] getKeywords() {
    return keywords.toArray(new String[keywords.size()]);
  }

  /**
   * Add a keyword to this search item.
   *
   * @param keyword
   *          the keyword
   */
  public void addKeyword(String keyword) {
    if (keywords == null)
      keywords = new ArrayList<String>();
    keywords.add(keyword);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getCover()
   */
  public String getCover() {
    return cover;
  }

  /**
   * @param cover
   *          the cover to set
   */
  public void setCover(String cover) {
    this.cover = cover;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getModified()
   */
  public Date getModified() {
    return modified;
  }

  /**
   * @param modified
   *          the modified to set
   */
  public void setModified(Date modified) {
    this.modified = modified;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getScore()
   */
  public double getScore() {
    return score;
  }

  /**
   * @param score
   *          the score to set
   */
  public void setScore(double score) {
    this.score = score;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchResultItem#getSegments()
   */
  public MediaSegment[] getSegments() {
    return mediaSegments != null ? mediaSegments.toArray(new MediaSegment[mediaSegments.size()])
            : new MediaSegmentImpl[0];
  }

  /**
   * Adds a segment to the list of media segments. The list is backed by a sorted set, so there is no need to add the
   * segments in order, although it is certainly more performant.
   *
   * @param segment
   *          the segment to add
   */
  public void addSegment(MediaSegment segment) {
    if (mediaSegments == null)
      mediaSegments = new TreeSet<MediaSegmentImpl>();
    mediaSegments.add((MediaSegmentImpl) segment); // TODO: assuming this
  }

  /**
   * Build a result item from an anonymously implemented interface to ensure you don't miss any fields.
   */
  public static SearchResultItemImpl fill(SearchResultItem from) {
    SearchResultItemImpl item = new SearchResultItemImpl();
    item.setId(from.getId());
    item.setOrganization(from.getOrganization());
    item.setMediaPackage(from.getMediaPackage());
    item.setDcExtent(from.getDcExtent());
    item.setDcTitle(from.getDcTitle());
    item.setDcSubject(from.getDcSubject());
    item.setDcDescription(from.getDcDescription());
    item.setDcCreator(from.getDcCreator());
    item.setDcPublisher(from.getDcPublisher());
    item.setDcContributor(from.getDcContributor());
    item.setDcAbstract(from.getDcAbstract());
    item.setDcCreated(from.getDcCreated());
    item.setDcAvailableFrom(from.getDcAvailableFrom());
    item.setDcAvailableTo(from.getDcAvailableTo());
    item.setDcLanguage(from.getDcLanguage());
    item.setDcRightsHolder(from.getDcRightsHolder());
    item.setDcSpatial(from.getDcSpatial());
    item.setDcTemporal(from.getDcTemporal());
    item.setDcIsPartOf(from.getDcIsPartOf());
    item.setDcReplaces(from.getDcReplaces());
    item.setDcType(from.getDcType());
    item.setDcAccessRights(from.getDcAccessRights());
    item.setDcLicense(from.getDcLicense());
    item.setOcMediapackage(from.getOcMediapackage());
    item.setMediaType(from.getType());
    for (String k : from.getKeywords())
      item.addKeyword(k);
    item.setCover(from.getCover());
    item.setModified(from.getModified());
    item.setScore(from.getScore());
    for (MediaSegment s : from.getSegments())
      item.addSegment(s);
    return item;
  }
}
