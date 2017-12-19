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

import org.opencastproject.feed.api.Category;
import org.opencastproject.feed.api.Content;
import org.opencastproject.feed.api.Feed;
import org.opencastproject.feed.api.FeedEntry;
import org.opencastproject.feed.api.FeedExtension;
import org.opencastproject.feed.api.Image;
import org.opencastproject.feed.api.Link;
import org.opencastproject.feed.api.Person;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Default feed implementation.
 */
public class FeedImpl implements Feed {

  /** Feed enconding, defaults to utf-8 */
  private String encoding = "utf-8";

  /** Unique uri */
  private String uri = null;

  /** The feed title */
  private Content title = null;

  /** The feed description */
  private Content description = null;

  /** Copyright disclaimer */
  private String copyright = null;

  /** Dublin Core Language */
  private String language = null;

  /** Dublin Core Publication date */
  private Date publishedDate = null;

  /** Date when the feed has bee updated */
  private Date updatedDate = null;

  /** Dublin core categories */
  private List<Category> categories = null;

  /** Additional links */
  private List<Link> links = null;

  /** Feed image */
  private Image image = null;

  /** The feed entries */
  private List<FeedEntry> entries = null;

  /** Modules that are used in this feed */
  private List<FeedExtension> modules = null;

  /** The list of authors */
  private List<Person> authors = null;

  /** The list of contributors */
  private List<Person> contributors = null;

  /** Link to the feed homepage */
  private String link = null;

  /** The feed type */
  private Type type = null;

  /**
   * Constructor used to create a new feed with the given uri and title.
   *
   * @param type
   *          feed type
   * @param uri
   *          the feed uri
   * @param title
   *          the feed title
   * @param description
   *          the feed description
   * @param link
   *          the link to the feed homepage
   */
  FeedImpl(Type type, String uri, Content title, Content description, String link) {
    this.type = type;
    this.uri = uri;
    this.title = title;
    this.description = description;
    this.link = link;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getType()
   */
  public Type getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getLink()
   */
  public String getLink() {
    return link;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setLink(java.lang.String)
   */
  public void setLink(String link) {
    this.link = link;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#addAuthor(org.opencastproject.feed.api.Person)
   */
  public void addAuthor(Person author) {
    if (authors == null)
      authors = new ArrayList<Person>();
    authors.add(author);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#addContributor(org.opencastproject.feed.api.Person)
   */
  public void addContributor(Person contributor) {
    if (contributors == null)
      contributors = new ArrayList<Person>();
    contributors.add(contributor);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#addEntry(org.opencastproject.feed.api.FeedEntry)
   */
  public void addEntry(FeedEntry entry) {
    if (entries == null)
      entries = new ArrayList<FeedEntry>();
    entries.add(entry);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#addLink(org.opencastproject.feed.api.Link)
   */
  public void addLink(Link link) {
    if (links == null)
      links = new ArrayList<Link>();
    links.add(link);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#addModule(org.opencastproject.feed.api.FeedExtension)
   */
  public void addModule(FeedExtension module) {
    if (modules == null)
      modules = new ArrayList<FeedExtension>();
    modules.add(module);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getAuthors()
   */
  public List<Person> getAuthors() {
    return authors;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getCategories()
   */
  public List<Category> getCategories() {
    return categories;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getContributors()
   */
  public List<Person> getContributors() {
    return contributors;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getCopyright()
   */
  public String getCopyright() {
    return copyright;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getDescription()
   */
  public Content getDescription() {
    return description;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getEncoding()
   */
  public String getEncoding() {
    return encoding;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getEntries()
   */
  public List<FeedEntry> getEntries() {
    return entries;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getImage()
   */
  public Image getImage() {
    return image;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getLanguage()
   */
  public String getLanguage() {
    return language;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getLinks()
   */
  public List<Link> getLinks() {
    return links;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getModule(java.lang.String)
   */
  public FeedExtension getModule(String uri) {
    if (modules == null)
      return null;
    for (FeedExtension m : modules)
      if (uri.equals(m.getUri()))
        return m;
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getModules()
   */
  public List<FeedExtension> getModules() {
    return modules;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getPublishedDate()
   */
  public Date getPublishedDate() {
    return publishedDate;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getUpdatedDate()
   */
  public Date getUpdatedDate() {
    return updatedDate;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getTitle()
   */
  public Content getTitle() {
    return title;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#getUri()
   */
  public String getUri() {
    return uri;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setAuthors(java.util.List)
   */
  public void setAuthors(List<Person> authors) {
    this.authors = authors;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setCategories(java.util.List)
   */
  public void setCategories(List<Category> categories) {
    this.categories = categories;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setContributors(java.util.List)
   */
  public void setContributors(List<Person> contributors) {
    this.contributors = contributors;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setCopyright(java.lang.String)
   */
  public void setCopyright(String copyright) {
    this.copyright = copyright;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setDescription(java.lang.String)
   */
  public void setDescription(String description) {
    this.description = new ContentImpl(description);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setDescription(org.opencastproject.feed.api.Content)
   */
  public void setDescription(Content description) {
    this.description = description;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setEncoding(java.lang.String)
   */
  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setEntries(java.util.List)
   */
  public void setEntries(List<FeedEntry> entries) {
    this.entries = entries;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setImage(org.opencastproject.feed.api.Image)
   */
  public void setImage(Image image) {
    this.image = image;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setLanguage(java.lang.String)
   */
  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setLinks(java.util.List)
   */
  public void setLinks(List<Link> links) {
    this.links = links;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setModules(java.util.List)
   */
  public void setModules(List<FeedExtension> modules) {
    this.modules = modules;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setPublishedDate(java.util.Date)
   */
  public void setPublishedDate(Date publishedDate) {
    this.publishedDate = publishedDate;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setPublishedDate(java.util.Date)
   */
  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setTitle(java.lang.String)
   */
  public void setTitle(String title) {
    this.title = new ContentImpl(title);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setTitle(org.opencastproject.feed.api.Content)
   */
  public void setTitle(Content title) {
    this.title = title;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.Feed#setUri(java.lang.String)
   */
  public void setUri(String uri) {
    this.uri = uri;
  }

}
