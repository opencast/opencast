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

package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.Category;
import org.opencastproject.feed.api.Content;
import org.opencastproject.feed.api.Enclosure;
import org.opencastproject.feed.api.Feed;
import org.opencastproject.feed.api.FeedEntry;
import org.opencastproject.feed.api.FeedExtension;
import org.opencastproject.feed.api.Image;
import org.opencastproject.feed.api.Link;
import org.opencastproject.feed.api.Person;

import com.sun.syndication.feed.module.DCModule;
import com.sun.syndication.feed.module.DCModuleImpl;
import com.sun.syndication.feed.module.DCSubject;
import com.sun.syndication.feed.module.DCSubjectImpl;
import com.sun.syndication.feed.module.Module;
import com.sun.syndication.feed.module.itunes.EntryInformation;
import com.sun.syndication.feed.module.itunes.EntryInformationImpl;
import com.sun.syndication.feed.module.itunes.FeedInformation;
import com.sun.syndication.feed.module.itunes.FeedInformationImpl;
import com.sun.syndication.feed.module.itunes.types.Duration;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.feed.synd.SyndImageImpl;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.feed.synd.SyndLinkImpl;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.feed.synd.SyndPersonImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper around the Rome RSS feed implementation
 */
public class RomeRssFeed extends SyndFeedImpl {

  /** Serial version UID */
  private static final long serialVersionUID = -2449605551424421096L;

  /**
   * Creates a new feed of type <code>SyndFeed</code>.
   *
   * @param feed
   *          the original feed
   * @param feedInfo
   *          the target feed information
   */
  public RomeRssFeed(Feed feed, FeedInfo feedInfo) {
    init(feed, feedInfo);
  }

  /**
   * Converts the replay feed to a rome feed, that can then be written to the reponse.
   *
   * @param originalFeed
   *          the original feed
   * @param feedInfo
   *          the feed info
   */
  private void init(Feed originalFeed, FeedInfo feedInfo) {
    if (originalFeed == null)
      throw new IllegalArgumentException("Feed is null");

    // Create SyndFeed
    setEncoding(originalFeed.getEncoding());
    setFeedType(feedInfo.toROMEVersion());

    // Convert fields
    setModules(toRomeModules(originalFeed.getModules()));
    setAuthors(toRomePersons(originalFeed.getAuthors()));
    setCategories(toRomeCategories(originalFeed.getCategories()));
    setContributors(toRomePersons(originalFeed.getContributors()));
    setDescriptionEx(toRomeContent(originalFeed.getDescription()));
    setImage(toRomeImage(originalFeed.getImage()));
    setLanguage(originalFeed.getLanguage());
    setLinks(toRomeLinks(originalFeed.getLinks()));
    setPublishedDate(originalFeed.getPublishedDate());
    setTitleEx(toRomeContent(originalFeed.getTitle()));
    setCopyright(originalFeed.getCopyright());
    setUri(originalFeed.getUri());
    setLink(originalFeed.getLink());

    // Add SyndFeedEntries
    if (originalFeed.getEntries() != null) {
      List<SyndEntry> romeEntries = new ArrayList<SyndEntry>();
      for (FeedEntry entry : originalFeed.getEntries()) {
        SyndEntryImpl e = new SyndEntryImpl();
        e.setModules(toRomeModules(entry.getModules()));
        e.setAuthors(toRomePersons(entry.getAuthors()));
        e.setCategories(toRomeCategories(entry.getCategories()));
        e.setContents(toRomeContents(entry.getContents()));
        e.setContributors(toRomePersons(entry.getContributors()));
        e.setDescription(toRomeContent(entry.getDescription()));
        e.setEnclosures(toRomeEnclosures(entry.getEnclosures()));
        e.setPublishedDate(entry.getPublishedDate());
        e.setTitleEx(toRomeContent(entry.getTitle()));
        e.setUpdatedDate(entry.getUpdatedDate());
        e.setUri(entry.getUri());
        List<SyndLink> links = toRomeLinks(entry.getLinks());
        e.setLinks(links);
        // todo this strategy seems to work but is unproven
        if (links.size() > 0)
          e.setLink(links.get(0).getHref());
        romeEntries.add(e);
      }
      setEntries(romeEntries);
    }
  }

  /**
   * Converts a list of categories to a <code>ROME</code> category list.
   *
   * @param categories
   *          original categories
   * @return <code>ROME</code> category list
   */
  private List<SyndCategory> toRomeCategories(List<Category> categories) {
    if (categories == null)
      return Collections.emptyList();
    List<SyndCategory> romeCategories = new ArrayList<SyndCategory>(categories.size());
    for (Category category : categories) {
      SyndCategoryImpl romeCategory = new SyndCategoryImpl();
      romeCategory.setName(category.getName());
      romeCategory.setTaxonomyUri(category.getTaxonomyUri());
      romeCategories.add(romeCategory);
    }
    return romeCategories;
  }

  /**
   * Converts the content to a <code>ROME</code> object.
   *
   * @param content
   *          original content
   * @return <code>ROME</code> content object
   */
  private SyndContent toRomeContent(Content content) {
    if (content == null)
      return null;
    SyndContentImpl romeContent = new SyndContentImpl();
    romeContent.setMode(content.getMode().toString().toLowerCase());
    romeContent.setType(content.getType());
    romeContent.setValue(content.getValue());
    return romeContent;
  }

  /**
   * Converts a list of content elements to a <code>ROME</code> content list.
   *
   * @param contents
   *          original contents
   * @return <code>ROME</code> content list
   */
  private List<SyndContent> toRomeContents(List<Content> contents) {
    if (contents == null)
      return Collections.emptyList();
    List<SyndContent> romeContents = new ArrayList<SyndContent>(contents.size());
    for (Content content : contents) {
      romeContents.add(toRomeContent(content));
    }
    return romeContents;
  }

  /**
   * Converts a list of enclosures to a <code>ROME</code> enclosure list.
   *
   * @param enclosures
   *          original enclosures
   * @return <code>ROME</code> enclosure list
   */
  private List<SyndEnclosure> toRomeEnclosures(List<Enclosure> enclosures) {
    if (enclosures == null)
      return Collections.emptyList();
    List<SyndEnclosure> romeEnclosures = new ArrayList<SyndEnclosure>(enclosures.size());
    for (Enclosure enclosure : enclosures) {
      SyndEnclosureImpl romeEnclosure = new SyndEnclosureImpl();
      romeEnclosure.setLength(enclosure.getLength());
      romeEnclosure.setType(enclosure.getType());
      romeEnclosure.setUrl(enclosure.getUrl());
      romeEnclosures.add(romeEnclosure);
    }
    return romeEnclosures;
  }

  /**
   * Converts the image to a <code>ROME</code> object.
   *
   * @param image
   *          original image
   * @return <code>ROME</code> image object
   */
  private SyndImage toRomeImage(Image image) {
    if (image == null)
      return null;
    SyndImageImpl romeImage = new SyndImageImpl();
    romeImage.setDescription(image.getDescription());
    romeImage.setLink(image.getLink());
    romeImage.setTitle(image.getTitle());
    romeImage.setUrl(image.getUrl());
    return romeImage;
  }

  /**
   * Converts a list of links to a <code>ROME</code> link list.
   *
   * @param links
   *          original links
   * @return <code>ROME</code> link list
   */
  private List<SyndLink> toRomeLinks(List<Link> links) {
    if (links == null)
      return Collections.emptyList();
    List<SyndLink> romeLinks = new ArrayList<SyndLink>(links.size());
    for (Link link : links) {
      SyndLinkImpl romeLink = new SyndLinkImpl();
      romeLink.setHref(link.getHref());
      romeLink.setHreflang(link.getHreflang());
      romeLink.setLength(link.getLength());
      romeLink.setRel(link.getRel());
      romeLink.setTitle(link.getTitle());
      romeLink.setType(link.getType());
      romeLinks.add(romeLink);
    }
    return romeLinks;
  }

  /**
   * Converts a list of persons to a <code>ROME</code> person list.
   *
   * @param persons
   *          original persons
   * @return <code>ROME</code> person list
   */
  private List<SyndPerson> toRomePersons(List<Person> persons) {
    if (persons == null)
      return Collections.emptyList();
    List<SyndPerson> romePersons = new ArrayList<SyndPerson>(persons.size());
    for (Person person : persons) {
      SyndPersonImpl romePerson = new SyndPersonImpl();
      romePerson.setEmail(person.getEmail());
      romePerson.setName(person.getName());
      romePerson.setUri(person.getUri());
      romePersons.add(romePerson);
    }
    return romePersons;
  }

  /**
   * Returns the rome version of the feed extensions.
   *
   * @param modules
   *          the feed extensions
   * @return the rome feed extensions
   */
  private List<Module> toRomeModules(List<FeedExtension> modules) {
    if (modules == null)
      return null;
    List<Module> romeModules = new ArrayList<Module>();
    for (FeedExtension extension : modules) {
      if (DublinCoreExtension.URI.equals(extension.getUri()))
        romeModules.add(toRomeModule((DublinCoreExtension) extension));
      if (extension instanceof ITunesFeedExtension)
        romeModules.add(toRomeModule((ITunesFeedExtension) extension));
      if (extension instanceof ITunesFeedEntryExtension)
        romeModules.add(toRomeModule((ITunesFeedEntryExtension) extension));
    }
    return romeModules;
  }

  /**
   * Creates a rome compatible dublin core module.
   *
   * @param dcExtension
   *          the dublin core feed extension
   * @return the rome module
   */
  private Module toRomeModule(DublinCoreExtension dcExtension) {
    DCModule m = new DCModuleImpl();
    m.setContributors(dcExtension.getContributors());
    m.setCoverage(dcExtension.getCoverage());
    m.setCreators(dcExtension.getCreators());
    m.setDate(dcExtension.getDate());
    m.setDescription(dcExtension.getDescription());
    m.setFormat(dcExtension.getFormat());
    m.setIdentifier(dcExtension.getIdentifier());
    m.setLanguage(dcExtension.getLanguage());
    m.setPublishers(dcExtension.getPublishers());
    m.setRelation(dcExtension.getRelation());
    m.setRights(dcExtension.getRights());
    m.setSource(dcExtension.getSource());
    List<DCSubject> subjects = new ArrayList<DCSubject>();
    for (DublinCoreExtension.Subject subject : dcExtension.getSubjects()) {
      DCSubject s = new DCSubjectImpl();
      s.setTaxonomyUri(subject.getTaxonomyUri());
      s.setValue(subject.getValue());
      subjects.add(s);
    }
    m.setSubjects(subjects);
    m.setTitle(dcExtension.getTitle());
    m.setType(dcExtension.getType());
    return m;
  }

  /**
   * Creates a rome compatible itunes feed module.
   *
   * @param ext
   *          the itunes feed extension
   * @return the rome module
   */
  private Module toRomeModule(ITunesFeedExtension ext) {
    FeedInformation m = new FeedInformationImpl();
    m.setAuthor(ext.getAuthor());
    m.setBlock(ext.isBlocked());
    m.setCategories(ext.getCategories());
    m.setExplicit(ext.isExplicit());
    if (ext.getKeywords() != null)
      m.setKeywords(ext.getKeywords().toArray(new String[ext.getKeywords().size()]));
    m.setOwnerEmailAddress(ext.getOwnerEmail());
    m.setOwnerName(ext.getOwnerName());
    m.setSubtitle(ext.getSubtitle());
    m.setSummary(ext.getSummary());
    return m;
  }

  /**
   * Creates a rome compatible itunes feed entry extension.
   *
   * @param ext
   *          the itunes entry extension
   * @return the rome module
   */
  private Module toRomeModule(ITunesFeedEntryExtension ext) {
    EntryInformation m = new EntryInformationImpl();
    m.setAuthor(ext.getAuthor());
    m.setBlock(ext.isBlocked());
    m.setDuration(new Duration(ext.getDuration()));
    m.setExplicit(ext.isExplicit());
    m.setKeywords(ext.getKeywords().toArray(new String[ext.getKeywords().size()]));
    m.setSubtitle(ext.getSubtitle());
    m.setSummary(ext.getSummary());
    return m;
  }
}
