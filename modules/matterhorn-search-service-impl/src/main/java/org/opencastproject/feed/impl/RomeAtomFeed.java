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
import org.opencastproject.feed.api.Link;
import org.opencastproject.feed.api.Person;

import com.sun.syndication.feed.atom.Entry;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper around the Rome Atom feed implementation
 */
public class RomeAtomFeed extends com.sun.syndication.feed.atom.Feed {

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
  public RomeAtomFeed(Feed feed, FeedInfo feedInfo) {
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
    setAuthors(toRomeAtomPersons(originalFeed.getAuthors()));
    setCategories(toRomeAtomCategories(originalFeed.getCategories()));
    setContributors(toRomeAtomPersons(originalFeed.getContributors()));
    setInfo(toRomeAtomContent(originalFeed.getDescription()));
    setLanguage(originalFeed.getLanguage());
    setAlternateLinks(toRomeAtomLinks(originalFeed.getLinks()));
    setUpdated(originalFeed.getUpdatedDate());
    setTitleEx(toRomeAtomContent(originalFeed.getTitle()));
    setId(originalFeed.getUri());
    List <Link> otherLinks = new ArrayList <Link>();
    otherLinks.add(new LinkImpl(originalFeed.getLink()));
    setOtherLinks(toRomeAtomLinks(otherLinks));

    // Add SyndFeedEntries
    if (originalFeed.getEntries() != null) {
      List<Entry> romeEntries = new ArrayList<Entry>();
      for (FeedEntry entry : originalFeed.getEntries()) {
        Entry e = new Entry();
        e.setModules(toRomeModules(entry.getModules()));
        e.setAuthors(toRomeAtomPersons(entry.getAuthors()));
        e.setCategories(toRomeAtomCategories(entry.getCategories()));
        e.setContents(toRomeAtomContents(entry.getContents()));
        e.setContributors(toRomeAtomPersons(entry.getContributors()));
        e.setSummary(toRomeAtomContent(entry.getDescription()));
        e.setPublished(entry.getPublishedDate());
        e.setTitleEx(toRomeAtomContent(entry.getTitle()));
        e.setUpdated(entry.getUpdatedDate());
        e.setId(entry.getUri());
        List<com.sun.syndication.feed.atom.Link> links = toRomeAtomLinks(entry.getLinks());
        links.addAll(toRomeAtomEnclosures(entry.getEnclosures()));
        e.setOtherLinks(links);
        // todo this strategy seems to work but is unproven
        //if (links.size() > 0)
        //  e.setLink(links.get(0).getHref());
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
  private List<com.sun.syndication.feed.atom.Category> toRomeAtomCategories(List<Category> categories) {
    if (categories == null)
      return Collections.emptyList();
    List<com.sun.syndication.feed.atom.Category> romeCategories = new ArrayList<com.sun.syndication.feed.atom.Category>(categories.size());
    for (Category category : categories) {
      com.sun.syndication.feed.atom.Category romeCategory = new com.sun.syndication.feed.atom.Category();
      romeCategory.setLabel(category.getName());
      romeCategory.setScheme(category.getTaxonomyUri());
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
  private com.sun.syndication.feed.atom.Content toRomeAtomContent(Content content) {
    if (content == null)
      return null;
    com.sun.syndication.feed.atom.Content romeContent = new com.sun.syndication.feed.atom.Content();
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
  private List<com.sun.syndication.feed.atom.Content> toRomeAtomContents(List<Content> contents) {
    if (contents == null)
      return Collections.emptyList();
    List<com.sun.syndication.feed.atom.Content> romeContents = new ArrayList<com.sun.syndication.feed.atom.Content>(contents.size());
    for (Content content : contents) {
      romeContents.add(toRomeAtomContent(content));
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
  private List<com.sun.syndication.feed.atom.Link> toRomeAtomEnclosures(List<Enclosure> enclosures) {
    if (enclosures == null)
      return Collections.emptyList();
    List<com.sun.syndication.feed.atom.Link> romeEnclosures = new ArrayList<com.sun.syndication.feed.atom.Link>(enclosures.size());
    for (Enclosure enclosure : enclosures) {
      com.sun.syndication.feed.atom.Link romeEnclosure = new com.sun.syndication.feed.atom.Link();
      romeEnclosure.setLength(enclosure.getLength());
      romeEnclosure.setType(enclosure.getType());
      romeEnclosure.setHref(enclosure.getUrl());
      romeEnclosure.setTitle(enclosure.getFlavor());
      romeEnclosure.setRel("enclosure");
      romeEnclosures.add(romeEnclosure);
    }
    return romeEnclosures;
  }

  /**
   * Converts a list of links to a <code>ROME</code> link list.
   *
   * @param links
   *          original links
   * @return <code>ROME</code> link list
   */
  private List<com.sun.syndication.feed.atom.Link> toRomeAtomLinks(List<Link> links) {
    if (links == null)
      return Collections.emptyList();
    List<com.sun.syndication.feed.atom.Link> romeLinks = new ArrayList<com.sun.syndication.feed.atom.Link>(links.size());
    for (Link link : links) {
      com.sun.syndication.feed.atom.Link romeLink = new com.sun.syndication.feed.atom.Link();
      romeLink.setHref(link.getHref());
      romeLink.setHreflang(link.getHreflang());
      romeLink.setLength(link.getLength());
      romeLink.setRel(link.getRel());
      romeLink.setTitle(link.getTitle());
      romeLink.setType(link.getType());
      romeLink.setTitle(link.getFlavour());
      romeLink.setLength(1);
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
  private List<com.sun.syndication.feed.atom.Person> toRomeAtomPersons(List<Person> persons) {
    if (persons == null)
      return Collections.emptyList();
    List<com.sun.syndication.feed.atom.Person> romePersons = new ArrayList<com.sun.syndication.feed.atom.Person>(persons.size());
    for (Person person : persons) {
      com.sun.syndication.feed.atom.Person romePerson = new com.sun.syndication.feed.atom.Person();
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
