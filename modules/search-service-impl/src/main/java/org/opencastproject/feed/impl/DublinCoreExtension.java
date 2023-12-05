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


package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.FeedExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Dublin core extension module.
 */
public class DublinCoreExtension implements FeedExtension {

  /** The dublin core module uri */
  public static final String URI = "http://purl.org/dc/elements/1.1/";

  private String title;
  private List<String> creators;
  private List<Subject> subjects;
  private String description;
  private List<String> publishers;
  private List<String> contributors;
  private Date date;
  private String type;
  private String format;
  private String identifier;
  private String source;
  private String language;
  private String relation;
  private String coverage;
  private String rights;

  /**
   * Default constructor. All properties are set to <b>null</b>.
   */
  public DublinCoreExtension() {
    this.subjects = new ArrayList<Subject>();
    this.creators = new ArrayList<String>();
    this.contributors = new ArrayList<String>();
    this.publishers = new ArrayList<String>();
  }

  /**
   * @see org.opencastproject.feed.api.FeedExtension#getUri()
   */
  public String getUri() {
    return URI;
  }

  /**
   * Returns the title.
   *
   * @return the dublin core title
   */
  public String geTitles() {
    return this.title;
  }

  /**
   * Returns the title.
   *
   * @return the title, <b>null</b> if none.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title.
   *
   * @param title
   *          the title to set, <b>null</b> if none.
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Returns the creator.
   *
   * @return the creator, <b>null</b> if none.
   */
  public String getCreator() {
    return (creators.size() > 0) ? creators.get(0) : null;
  }

  /**
   * Returns the list of creators.
   *
   * @return the creators
   */
  public List<String> getCreators() {
    return creators;
  }

  /**
   * Sets the creator.
   *
   * @param creator
   *          the creator to set
   */
  public void setCreator(String creator) {
    creators.clear();
    creators.add(creator);
  }

  /**
   * Adds a creator to the list of creators.
   *
   * @param creator
   *          the creator to add
   */
  public void addCreator(String creator) {
    creators.add(creator);
  }

  /**
   * Returns the subject.
   *
   * @return the subject, <b>null</b> if none.
   */
  public List<Subject> getSubjects() {
    return subjects;
  }

  /**
   * Adds a subject element.
   *
   * @param taxonomyUri
   *          the taxonomy uri
   * @param value
   *          the subject value
   */
  public void addSubject(String taxonomyUri, String value) {
    subjects.add(new Subject(taxonomyUri, value));
  }

  /**
   * Returns the description.
   *
   * @return the description, <b>null</b> if none.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description
   *          the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the title.
   *
   * @return the title, <b>null</b> if none.
   */
  public String getPublisher() {
    return (publishers.size() > 0) ? publishers.get(0) : null;
  }

  /**
   * Returns the list of publishers.
   *
   * @return the publishers
   */
  public List<String> getPublishers() {
    return publishers;
  }

  /**
   * Sets the publisher.
   *
   * @param publisher
   *          the publisher to set
   */
  public void setPublisher(String publisher) {
    publishers.clear();
    publishers.add(publisher);
  }

  /**
   * Adds a publisher to the list of publishers.
   *
   * @param publisher
   *          the publisher to add
   */
  public void addPublisher(String publisher) {
    publishers.add(publisher);
  }

  /**
   * Returns the contributor.
   *
   * @return the contributor, <b>null</b> if none.
   */
  public String getContributor() {
    return (contributors.size() > 0) ? contributors.get(0) : null;
  }

  /**
   * Returns the list of contributors.
   *
   * @return the contributors
   */
  public List<String> getContributors() {
    return contributors;
  }

  /**
   * Sets the contributor.
   *
   * @param contributor
   *          the contributor to set
   */
  public void setContributor(String contributor) {
    contributors.clear();
    contributors.add(contributor);
  }

  /**
   * Adds a contributor to the list of contributors.
   *
   * @param contributor
   *          the contributor to add
   */
  public void addContributor(String contributor) {
    contributors.add(contributor);
  }

  /**
   * Returns the date.
   *
   * @return the date, <b>null</b> if none.
   */
  public Date getDate() {
    return date;
  }

  /**
   * Sets the date.
   *
   * @param date
   *          the date to set, <b>null</b> if none.
   */
  public void setDate(Date date) {
    this.date = date;
  }

  /**
   * Returns the type
   *
   * @return the type, <b>null</b> if none.
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type
   *          the type to set, <b>null</b> if none.
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Returns the format
   *
   * @return the format, <b>null</b> if none.
   */
  public String getFormat() {
    return format;
  }

  /**
   * Sets the format.
   *
   * @param format
   *          the format to set, <b>null</b> if none.
   */
  public void setFormat(String format) {
    this.format = format;
  }

  /**
   * Returns the identifier.
   *
   * @return the identifier, <b>null</b> if none.
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Sets the identifier.
   *
   * @param identifier
   *          the identifier to set, <b>null</b> if none.
   */
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  /**
   * Returns the source.
   *
   * @return the source, <b>null</b> if none.
   */
  public String getSource() {
    return source;
  }

  /**
   * Sets the source.
   *
   * @param source
   *          the source to set, <b>null</b> if none.
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * Returns the language.
   *
   * @return the langauge, <b>null</b> if none.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Sets the language.
   *
   * @param language
   *          the language to set, <b>null</b> if none.
   */
  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * Returns the relation.
   *
   * @return the relation, <b>null</b> if none.
   */
  public String getRelation() {
    return relation;
  }

  /**
   * Sets the relation.
   *
   * @param relation
   *          the relation to set, <b>null</b> if none.
   */
  public void setRelation(String relation) {
    this.relation = relation;
  }

  /**
   * Returns the coverage.
   *
   * @return the coverage, <b>null</b> if none.
   */
  public String getCoverage() {
    return coverage;
  }

  /**
   * Sets the coverage.
   *
   * @param coverage
   *          the coverage to set, <b>null</b> if none.
   *
   */
  public void setCoverage(String coverage) {
    this.coverage = coverage;
  }

  /**
   * Returns the rights.
   *
   * @return the rights, <b>null</b> if none.
   */
  public String getRights() {
    return rights;
  }

  /**
   * Sets the rights.
   *
   * @param rights
   *          the rights to set, <b>null</b> if none.
   */
  public void setRights(String rights) {
    this.rights = rights;
  }

  /**
   * Class used to module dublin core subjects.
   */
  public class Subject {

    protected String taxonomyUri = null;

    protected String value = null;

    /**
     * Creates a new subject definition.
     *
     * @param taxonomyUri
     *          the taxonomy uri
     * @param value
     *          the value
     */
    Subject(String taxonomyUri, String value) {
      this.taxonomyUri = taxonomyUri;
      this.value = value;
    }

    /**
     * Reutrns the taxonomy uri.
     *
     * @return the uri
     */
    public String getTaxonomyUri() {
      return taxonomyUri;
    }

    /**
     * Returns the subject value.
     *
     * @return the value
     */
    public String getValue() {
      return value;
    }

  }

}
