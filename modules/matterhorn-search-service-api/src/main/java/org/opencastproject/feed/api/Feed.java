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


package org.opencastproject.feed.api;

import java.util.Date;
import java.util.List;

/**
 * This interface defines the methods of a general feed.
 * <p>
 * Note that this interface is heavily inspired and backed by the excellent rss/atom feed library <tt>Rome</tt>
 * (http://https://rome.dev.java.net).
 */
public interface Feed {

  /** The type of feed to generate */
  public enum Type {
    RSS, Atom;
    public static Type parseString(String type) {
      if (RSS.toString().equalsIgnoreCase(type))
        return RSS;
      else
        return Atom;
    }
  };

  /**
   * Returns the feed type.
   *
   * @return the feed type
   */
  Type getType();

  /**
   * Returns the charset encoding of a the feed.
   *
   * @return the charset encoding of the feed
   */
  String getEncoding();

  /**
   * Sets the charset encoding of a the feed.
   *
   * @param encoding
   *          the charset encoding of the feed
   */
  void setEncoding(String encoding);

  /**
   * Returns the feed URI.
   * <p>
   * How the feed URI maps to a concrete feed type (RSS or Atom) depends on the concrete feed type.
   * <p>
   * The returned URI is a normalized URI as specified in RFC 2396bis.
   * <p>
   * Note: The URI is the unique identifier, in the RSS 2.0/atom case this is the GUID, for RSS 1.0 this is the URI
   * attribute of the item. The Link is the URL that the item is accessible under, the URI is the permanent identifier
   * which the aggregator should use to reference this item. Often the URI will use some standardized identifier scheme
   * such as DOI's so that items can be identified even if they appear in multiple feeds with different "links" (they
   * might be on different hosting platforms but be the same item). Also, though rare, there could be multiple items
   * with the same link but a different URI and associated metadata which need to be treated as distinct entities. In
   * the RSS 1.0 case the URI must be a valid RDF URI reference.
   *
   * @return the feed URI, <b>null</b> if none.
   */
  String getUri();

  /**
   * Sets the feed URI.
   * <p>
   * How the feed URI maps to a concrete feed type (RSS or Atom) depends on the concrete feed type.
   * <p>
   * Note: The URI is the unique identifier, in the RSS 2.0/atom case this is the GUID, for RSS 1.0 this is the URI
   * attribute of the item. The Link is the URL that the item is accessible under, the URI is the permanent identifier
   * which the aggregator should use to reference this item. Often the URI will use some standardized identifier scheme
   * such as DOI's so that items can be identified even if they appear in multiple feeds with different "links" (they
   * might be on different hosting platforms but be the same item). Also, though rare, there could be multiple items
   * with the same link but a different URI and associated metadata which need to be treated as distinct entities. In
   * the RSS 1.0 case the URI must be a valid RDF URI reference.
   *
   * @param uri
   *          the feed URI to set, <b>null</b> if none
   */
  void setUri(String uri);

  /**
   * Get the feed link.
   *
   * @return the feed link
   */
  String getLink();

  /**
   * Set the feed link.
   *
   * @param link the link to the feed
   */
  void setLink(String link);

  /**
   * Sets the feed title.
   *
   * @param title
   *          the feed title to set, <b>null</b> if none
   */
  void setTitle(String title);

  /**
   * Returns the feed title.
   *
   * @return the feed title, <b>null</b> if none
   */
  Content getTitle();

  /**
   * Sets the feed title.
   *
   * @param title
   *          the feed title to set, <b>null</b> if none
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
   * Sets the feed link.
   * <p>
   * Note: The URI is the unique identifier, in the RSS 2.0/atom case this is the GUID, for RSS 1.0 this is the URI
   * attribute of the item. The Link is the URL that the item is accessible under, the URI is the permanent identifier
   * which the aggregator should use to reference this item. Often the URI will use some standardized identifier scheme
   * such as DOI's so that items can be identified even if they appear in multiple feeds with different "links" (they
   * might be on different hosting platforms but be the same item). Also, though rare, there could be multiple items
   * with the same link but a different URI and associated metadata which need to be treated as distinct entities. In
   * the RSS 1.0 case the URI must be a valid RDF URI reference.
   *
   * @param link
   *          the link to add
   */
  void addLink(Link link);

  /**
   * Sets the feed description.
   *
   * @param description
   *          the feed description to set, <b>null</b> if none
   */
  void setDescription(String description);

  /**
   * Returns the feed description as a text construct.
   *
   * @return the feed description, <b>null</b> if none
   */
  Content getDescription();

  /**
   * Sets the feed description as a text construct.
   *
   * @param description
   *          the feed description to set, <b>null</b> if none
   */
  void setDescription(Content description);

  /**
   * Returns the feed published date.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module date.
   *
   * @return the feed published date, <b>null</b> if none
   */
  Date getPublishedDate();

  /**
   * Sets the feed published date.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module date.
   *
   * @param publishedDate
   *          the feed published date to set, <b>null</b> if none
   */
  void setPublishedDate(Date publishedDate);

  /**
   * Returns the feed authors.
   * <p>
   * For Atom feeds, this returns the authors as a list of SyndPerson objects, for RSS feeds this method is a
   * convenience method, it maps to the Dublin Core module creator.
   *
   * @return the feed authors, <b>null</b> if none
   */
  List<Person> getAuthors();

  /**
   * Sets the feed authors.
   * <p>
   * For Atom feeds, this sets the authors as a list of SyndPerson objects, for RSS feeds this method is a convenience
   * method, it maps to the Dublin Core module creator.
   *
   * @param authors
   *          the feed authors to set, <b>null</b> if none
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
   * Returns the feed contributors.
   * <p>
   * For Atom feeds, this returns the contributors as a list of Person objects.
   *
   * @return the feed contributors, <b>null</b> if none
   */
  List<Person> getContributors();

  /**
   * Sets the feed contributors.
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
   * Returns the feed copyright.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module rights.
   *
   * @return the feed copyright, <b>null</b> if none
   */
  String getCopyright();

  /**
   * Sets the feed copyright.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module rights.
   *
   * @param copyright
   *          the feed copyright to set, <b>null</b> if none
   */
  void setCopyright(String copyright);

  /**
   * Returns the feed image.
   *
   * @return the feed image, <b>null</b> if none
   */
  Image getImage();

  /**
   * Sets the feed image.
   *
   * @param image
   *          the feed image to set, <b>null</b> if none
   */
  void setImage(Image image);

  /**
   * Returns the feed categories.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module subjects.
   *
   * @return a list of SyndCategoryImpl elements with the feed categories, an empty list if none
   */
  List<Category> getCategories();

  /**
   * Sets the feed categories.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module subjects.
   *
   * @param categories
   *          the list of SyndCategoryImpl elements with the feed categories to set, an empty list or <b>null</b> if
   *          none
   */
  void setCategories(List<Category> categories);

  /**
   * Returns the feed entries.
   *
   * @return a list of FeedEntry elements with the feed entries, an empty list if none.
   */
  List<FeedEntry> getEntries();

  /**
   * Sets the feed entries.
   *
   * @param entries
   *          the list of FeedEntry elements with the feed entries to set, an empty list or <b>null</b> if none
   */
  void setEntries(List<FeedEntry> entries);

  /**
   * Adds a feed entry.
   *
   * @param entry
   *          the feed entry to add
   */
  void addEntry(FeedEntry entry);

  /**
   * Returns the feed language.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module language.
   *
   * @return the feed language, <b>null</b> if none
   */
  String getLanguage();

  /**
   * Sets the feed language.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module language.
   *
   * @param language
   *          the feed language to set, <b>null</b> if none
   */
  void setLanguage(String language);

  /**
   * Returns the module identified by a given URI.
   *
   * @param uri
   *          the URI of the module
   * @return The module with the given URI, <b>null</b> if none
   */
  FeedExtension getModule(String uri);

  /**
   * Returns the feed modules.
   *
   * @return a list of module elements with the feed modules, an empty list if none
   */
  List<FeedExtension> getModules();

  /**
   * Sets the feed modules.
   *
   * @param modules
   *          the list of module elements with the feed modules to set, an empty list or <b>null</b> if none
   */
  void setModules(List<FeedExtension> modules);

  /**
   * Adds the feed module.
   *
   * @param module
   *          the module to add
   */
  void addModule(FeedExtension module);

  /**
   * Returns the feed updated date.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module date.
   *
   * @return the feed updated date, <b>null</b> if none
   */
  Date getUpdatedDate();

  /**
   * Sets the feed updated date, which is needed for a valid Atom feed.
   * <p>
   * This method is a convenience method, it maps to the Dublin Core module date.
   *
   * @param updatedDate
   *          the feed updated date to set, <b>null</b> if none
   */
  void setUpdatedDate(Date updatedDate);

}
