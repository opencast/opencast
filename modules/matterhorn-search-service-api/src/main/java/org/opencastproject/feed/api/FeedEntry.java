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

package org.opencastproject.feed.api;

import java.util.Date;
import java.util.List;

/**
 * Feed entries are the child elements of an rrs/atom feed.
 * <p>
 * Note that this interface is heavily inspired and backed by the excellent rss/atom feed library <tt>Rome</tt>
 * (http://rome.dev.java.net).
 */
public interface FeedEntry {

  /**
   * Returns the entry URI.
   *
   * @return the entry URI, <b>null</b> if none
   */
  String getUri();

  /**
   * Sets the entry URI.
   *
   * @param uri
   *          the entry URI to set, <b>null</b> if none
   */
  void setUri(String uri);

  /**
   * Sets the entry title.
   *
   * @param title
   *          the entry title to set, <b>null</b> if none
   */
  void setTitle(String title);

  /**
   * Returns the entry title as a text construct.
   *
   * @return the entry title, <b>null</b> if none
   */
  Content getTitle();

  /**
   * Sets the entry title as a text construct.
   *
   * @param title
   *          the entry title to set, <b>null</b> if none
   */
  void setTitle(Content title);

  /**
   * Returns the entry links.
   *
   * @return the entry links, <b>null</b> if none
   */
  List<Link> getLinks();

  /**
   * Sets the entry links.
   *
   * @param links
   *          the entry links to set, <b>null</b> if none
   */
  void setLinks(List<Link> links);

  /**
   * Adds a link.
   *
   * @param link
   *          the link to add
   */
  void addLink(Link link);

  /**
   * Returns the entry description.
   *
   * @return the entry description, <b>null</b> if none
   */
  Content getDescription();

  /**
   * Sets the entry description.
   *
   * @param description
   *          the entry description to set, <b>null</b> if none
   */
  void setDescription(Content description);

  /**
   * Returns the entry contents.
   *
   * @return a list of Content elements with the entry contents, an empty list if none
   */
  List<Content> getContents();

  /**
   * Sets the entry contents.
   *
   * @param contents
   *          the list of Content elements with the entry contents to set, an empty list or <b>null</b> if none
   */
  void setContents(List<Content> contents);

  /**
   * Adds the feed body as a content object. The entry can have multiple bodies, preferably satisfying different content
   * encodings.
   *
   * @param content
   *          the entry's body
   * @see Content#setType(String)
   */
  void addContent(Content content);

  /**
   * Returns the entry enclosures.
   *
   * @return a list of Enclosure elements with the entry enclosures, an empty list if none.
   */
  List<Enclosure> getEnclosures();

  /**
   * Sets the entry enclosures.
   * <p>
   *
   * @param enclosures
   *          the list of Enclosure elements with the entry enclosures to set, an empty list or <b>null</b> if none
   */
  void setEnclosures(List<Enclosure> enclosures);

  /**
   * Adds an entry enclosure.
   *
   * @param enclosure
   *          the enclosure element
   */
  void addEnclosure(Enclosure enclosure);

  /**
   * Returns the entry published date.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module date.
   *
   * @return the entry published date, <b>null</b> if none
   */
  Date getPublishedDate();

  /**
   * Sets the entry published date.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module date.
   *
   * @param publishedDate
   *          the entry published date to set, <b>null</b> if none
   */
  void setPublishedDate(Date publishedDate);

  /**
   * Returns the entry updated date.
   *
   * @return the entry updated date, <b>null</b> if none
   */
  Date getUpdatedDate();

  /**
   * Sets the entry updated date.
   *
   * @param updatedDate
   *          the entry updated date to set, <b>null</b> if none
   */
  void setUpdatedDate(Date updatedDate);

  /**
   * Returns the entry authors.
   * <p>
   * For Atom feeds, this returns the authors as a list of Person objects, for RSS feeds this method is a convenience
   * method, it maps to the Dublin Core module creator.
   *
   * @return the feed author, <b>null</b> if none
   */
  List<Person> getAuthors();

  /**
   * Sets the entry author.
   * <p>
   * For Atom feeds, this sets the authors as a list of Person objects, for RSS feeds this method is a convenience
   * method, it maps to the Dublin Core module creator.
   *
   * @param authors
   *          the feed author to set, <b>null</b> if none
   */
  void setAuthors(List<Person> authors);

  /**
   * Adds a feed author.
   * <p>
   * For Atom feeds, this adds the author to a list of Person objects, for RSS feeds this method is a convenience
   * method, it maps to the Dublin Core module creator.
   *
   * @param author
   *          the feed author to add
   */
  void addAuthor(Person author);

  /**
   * Returns the feed author.
   * <p>
   * For Atom feeds, this returns the contributors as a list of Person objects
   *
   * @return the feed author, <b>null</b> if none
   */
  List<Person> getContributors();

  /**
   * Sets the feed contributors.
   * <p>
   * Returns contributors as a list of Person objects.
   *
   * @param contributors
   *          the feed contributors to set, <b>null</b> if none
   */
  void setContributors(List<Person> contributors);

  /**
   * Adds a feed contributor.
   *
   * @param contributor
   *          the contributor to add
   */
  void addContributor(Person contributor);

  /**
   * Returns the entry categories.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core subjects.
   *
   * @return a list of Category elements with the entry categories, an empty list if none
   */
  List<Category> getCategories();

  /**
   * Sets the entry categories.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core subjects.
   *
   * @param categories
   *          the list of Category elements with the entry categories to set, an empty list or <b>null</b> if none
   */
  void setCategories(List<Category> categories);

  /**
   * The category to add.
   *
   * This method is a convenience method, it maps to the Dublin Core module subjects.
   *
   * @param category
   *          the category to add
   */
  void addCategory(Category category);

  /**
   * Returns the entry source.
   *
   * @return the Feed to which this entry is attributed
   */
  Feed getSource();

  /**
   * Returns the module identified by a given URI.
   *
   * @param uri
   *          the URI of the Module.
   * @return The module with the given URI, <b>null</b> if none
   */
  FeedExtension getModule(String uri);

  /**
   * Returns the entry modules.
   *
   * @return a list of Module elements with the entry modules, an empty list if none
   */
  List<FeedExtension> getModules();

  /**
   * Sets the entry extensions.
   *
   * @param extensions
   *          the list of feed extensions
   */
  void setExtensions(List<FeedExtension> extensions);

  /**
   * Adds the extension.
   *
   * @param extension
   *          the extension to add
   */
  void addExtension(FeedExtension extension);

}
