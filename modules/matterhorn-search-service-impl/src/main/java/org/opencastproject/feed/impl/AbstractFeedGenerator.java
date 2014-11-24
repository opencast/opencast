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

import org.opencastproject.feed.api.Content;
import org.opencastproject.feed.api.Content.Mode;
import org.opencastproject.feed.api.Enclosure;
import org.opencastproject.feed.api.Feed;
import org.opencastproject.feed.api.FeedEntry;
import org.opencastproject.feed.api.FeedExtension;
import org.opencastproject.feed.api.FeedGenerator;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchResultItem.SearchResultItemType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class provides basic functionality for creating feeds and is used as the base implementation for the default
 * feed generators.
 */
public abstract class AbstractFeedGenerator implements FeedGenerator {

  /** A default value for limit */
  protected static final int DEFAULT_LIMIT = 100;

  /** Unlimited */
  protected static final int NO_LIMIT = Integer.MAX_VALUE;

  /** A default value for offset */
  protected static final int DEFAULT_OFFSET = 0;

  /** The date parser format **/
  protected static final String DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";

  /** The default feed encoding */
  public static final String ENCODING = "UTF-8";

  /** Default rss media type */
  public static final String PROP_RSS_MEDIA_TYPE_DEFAULT = "*";

  /** Link to the user interface */
  protected String linkTemplate = null;

  /** Link to the user alternative interface */
  protected String linkSelf = null;

  /** The feed homepage */
  protected String home = null;

  /** Default format for rss feeds */
  protected List<MediaPackageElementFlavor> rssTrackFlavors = null;

  /** The */
  protected List<String> rssMediaTypes = null;

  /** Formats for atom feeds */
  protected Set<MediaPackageElementFlavor> atomTrackFlavors = null;

  /** Tags used to mark rss tracks */
  protected Set<String> rssTags = null;

  /** Tags used to mark atom tracks */
  protected Set<String> atomTags = null;

  /** the feed uri */
  protected String uri = null;

  /** the feed size */
  protected int size = DEFAULT_LIMIT;

  /** The feed name */
  protected String name = null;

  /** Url to the cover image */
  protected String cover = null;

  /** Copyright notice */
  protected String copyright = null;

  /** The feed description */
  protected String description = null;

  /** The URL of the server for valid URIs in the feeds */
  protected String serverUrl = null;

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(AbstractFeedGenerator.class);

  /**
   * Creates a new abstract feed generator.
   * <p>
   * <b>Note:</b> Subclasses using this constructor need to set required member variables prior to calling
   * {@link #createFeed(org.opencastproject.feed.api.Feed.Type, String[], int)} for the first time.
   */
  protected AbstractFeedGenerator() {
    atomTrackFlavors = new HashSet<MediaPackageElementFlavor>();
    rssTrackFlavors = new ArrayList<MediaPackageElementFlavor>();
    rssMediaTypes = new ArrayList<String>();
    rssTags = new HashSet<String>();
    atomTags = new HashSet<String>();
  }

  /**
   * Creates a new abstract feed generator.
   *
   * @param uri
   *          the feed identifier
   * @param feedHome
   *          the feed's home url
   * @param rssFlavors
   *          the ordered list of flavors identifying the track to be included in rss feeds
   * @param rssMediaTypes
   *          the ordered list of media types to include in rss feeds
   * @param atomFlavors
   *          the flavors identifying tracks to be included in atom feeds
   * @param entryLinkTemplate
   *          the link template
   */
  public AbstractFeedGenerator(String uri, String feedHome, MediaPackageElementFlavor[] rssFlavors,
          String[] rssMediaTypes, MediaPackageElementFlavor[] atomFlavors, String entryLinkTemplate) {
    this();
    this.uri = uri;
    this.home = feedHome;
    if (rssFlavors != null)
      this.rssTrackFlavors.addAll(Arrays.asList(rssFlavors));
    if (rssMediaTypes != null)
      this.rssMediaTypes.addAll(Arrays.asList(rssMediaTypes));
    this.linkTemplate = entryLinkTemplate;
    if (atomFlavors != null)
      this.atomTrackFlavors.addAll(Arrays.asList(atomFlavors));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.FeedGenerator#getIdentifier()
   */
  public String getIdentifier() {
    return uri;
  }

  /**
   * Sets the feed name.
   *
   * @param name
   *          the feed name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.FeedGenerator#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the feed description.
   *
   * @param description
   *          the feed description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.FeedGenerator#getDescription()
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the copyright notice.
   *
   * @param copyright
   *          the copyright notice
   */
  public void setCopyright(String copyright) {
    this.copyright = copyright;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.FeedGenerator#getCopyright()
   */
  @Override
  public String getCopyright() {
    return copyright;
  }

  /**
   * Sets the url to the cover url.
   *
   * @param cover
   *          the cover url
   */
  public void setCover(String cover) {
    this.cover = cover;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.FeedGenerator#getCover()
   */
  @Override
  public String getCover() {
    return cover;
  }

  /**
   * Loads and returns the feed data.
   *
   * @param type
   *          the requested feed type
   * @param query
   *          the query parameter
   * @param limit
   *          the number of entries
   * @param offset
   *          the starting entry
   * @return the feed data
   */
  protected abstract SearchResult loadFeedData(Feed.Type type, String[] query, int limit, int offset);

  /**
   * Creates a search query that is taking into account the flavors and tags as configured in the feed definition as
   * well as limit and offset passed into this method.
   *
   * @param type
   *          the requested feed type
   * @param limit
   *          the number of entries
   * @param offset
   *          the starting entry
   * @return the base query
   */
  protected SearchQuery createBaseQuery(Feed.Type type, int limit, int offset) {
    SearchQuery searchQuery = new SearchQuery();
    searchQuery.withLimit(limit);
    searchQuery.withOffset(offset);
    searchQuery.withSort(SearchQuery.Sort.DATE_CREATED);
    switch (type) {
      case Atom:
        if (atomTags != null && atomTags.size() > 0)
          searchQuery.withElementTags(atomTags.toArray(new String[atomTags.size()]));
        break;
      case RSS:
        if (rssTags != null && rssTags.size() > 0)
          searchQuery.withElementTags(rssTags.toArray(new String[rssTags.size()]));
        break;
      default:
        throw new IllegalStateException("Unknown feed type '" + type + "' is not supported");
    }
    return searchQuery;
  }

  /**
   * Creates a new feed.
   *
   * @param type
   *          the feed type
   * @param uri
   *          the feed identifier
   * @param title
   *          the feed title
   * @param description
   *          the feed description
   * @param link
   *          the link to the feed homepage
   * @return the new feed
   */
  protected Feed createFeed(Feed.Type type, String uri, Content title, Content description, String link) {
    return new FeedImpl(type, uri, title, description, link);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.feed.api.FeedGenerator#createFeed(org.opencastproject.feed.api.Feed.Type,
   *      java.lang.String[], int)
   */
  public final Feed createFeed(Feed.Type type, String[] query, int size) {
    logger.debug("Started to create {} feed", type);
    SearchResult result = null;

    if (type == null)
      throw new IllegalArgumentException("Feed type must not be null");

    if (size <= 0) {
      logger.trace("Using the feed's configured size of {}", this.size);
      size = this.size;
    }

    // Check if the feed generator is correctly set up
    if (uri == null)
      throw new IllegalStateException("Feed uri (feed.uri) must be configured");
    if (name == null)
      throw new IllegalStateException("Feed name (feed.name) must be configured");
    if (home == null)
      throw new IllegalStateException("Feed url (feed.home) must be configured");
    if (linkTemplate == null)
      throw new IllegalStateException("Feed link template (feed.entry) must be configured");

    // Have the concrete implementation load the feed data
    result = loadFeedData(type, query, size, DEFAULT_OFFSET);
    if (result == null) {
      logger.debug("Cannot retrieve solr result for feed '" + type.toString() + "' with query '" + query + "'.");
      return null;
    }

    // Create the feed
    Feed f = createFeed(type, getIdentifier(), new ContentImpl(getName()), new ContentImpl(getDescription()),
            getFeedLink());

    // Set the copyright
    if (StringUtils.isNotBlank(getCopyright()))
      f.setCopyright(getCopyright());

    // Adjust the feed encoding
    f.setEncoding(ENCODING);

    // Set iTunes tags
    ITunesFeedExtension iTunesFeed = new ITunesFeedExtension();
    f.addModule(iTunesFeed);

    if (cover != null)
      f.setImage(new ImageImpl(cover, "Feed Image"));

    // Check if a default format has been specified
    // TODO: Parse flavor and set member variable rssTrackFlavor
    // String rssFlavor = query.length > 1 ? query[query.length - 1] : null;

    // Iterate over the feed data and create the entries
    int itemCount = 0;
    for (SearchResultItem resultItem : result.getItems()) {
      try {
        if (resultItem.getType().equals(SearchResultItemType.Series))
          addSeries(f, query, resultItem);
        else
          addEpisode(f, query, resultItem);
      } catch (Throwable t) {
        logger.error(
                "Error creating entry with id " + resultItem.getId() + " for feed " + this + ": " + t.getMessage(), t);
      }
      itemCount++;
      if (itemCount >= size)
        break;
    }
    return f;
  }

  /**
   * Adds series information to the feed.
   *
   * @param feed
   *          the feed
   * @param query
   *          the query that results in the feed
   * @param resultItem
   *          the series item
   * @return the feed
   */
  protected Feed addSeries(Feed feed, String[] query, SearchResultItem resultItem) {
    Date d = resultItem.getDcCreated();

    // find iTunes module
    ITunesFeedExtension iTunesFeed = null;

    for (FeedExtension extension : feed.getModules()) {
      if (extension instanceof ITunesFeedExtension) {
        iTunesFeed = (ITunesFeedExtension) extension;
        break;
      }
    }

    if (!StringUtils.isEmpty(resultItem.getDcTitle()))
      feed.setTitle(resultItem.getDcTitle());

    if (!StringUtils.isEmpty(resultItem.getDcDescription())) {
      feed.setDescription(resultItem.getDcDescription());
      if (iTunesFeed != null)
        iTunesFeed.setSummary(resultItem.getDcDescription());
    }

    if (!StringUtils.isEmpty(resultItem.getDcCreator())) {
      PersonImpl personImpl = new PersonImpl(resultItem.getDcCreator());
      feed.addAuthor(personImpl);
      if (iTunesFeed != null) {
        iTunesFeed.setAuthor(personImpl.getName());
        iTunesFeed.setOwnerName(personImpl.getName());
      }
    }

    if (!StringUtils.isEmpty(resultItem.getDcContributor()))
      feed.addContributor(new PersonImpl(resultItem.getDcContributor()));

    if (!StringUtils.isEmpty(resultItem.getDcAccessRights()))
      feed.setCopyright(resultItem.getDcAccessRights());

    if (!StringUtils.isEmpty(resultItem.getDcLanguage()))
      feed.setLanguage(resultItem.getDcLanguage());

    feed.setUri(resultItem.getId());
    feed.addLink(new LinkImpl(getLinkForEntry(feed, resultItem)));

    if (d != null)
      feed.setPublishedDate(d);

    // Set the cover image
    String coverUrl = null;
    if (!StringUtils.isEmpty(resultItem.getCover())) {
      coverUrl = resultItem.getCover();
      feed.setImage(new ImageImpl(coverUrl, resultItem.getDcTitle()));
      try {
        if (iTunesFeed != null)
          iTunesFeed.setImage(new URL(coverUrl));
      } catch (MalformedURLException e) {
        logger.error("Error creating cover URL from " + coverUrl);
      }
    }
    return feed;
  }

  /**
   * Adds episode information to the feed.
   *
   * @param feed
   *          the feed
   * @param query
   *          the query that results in the feed
   * @param resultItem
   *          the episodes item
   * @return the feed
   */
  protected Feed addEpisode(Feed feed, String[] query, SearchResultItem resultItem) {
    String link = getLinkForEntry(feed, resultItem);
    String title = resultItem.getDcTitle();

    // Get the media enclosures
    List<MediaPackageElement> enclosures = getEnclosures(feed, resultItem);
    if (enclosures.size() == 0) {
      logger.debug("No media formats found for feed entry {}", title);
      return feed;
    }

    String entryUri = null;

    // For RSS feeds, create multiple entries (one per enclosure). For Atom, add all enclosures to the same item
    switch (feed.getType()) {
      case RSS:
        entryUri = resultItem.getId();
        for (MediaPackageElement e : enclosures) {
          List<MediaPackageElement> enclosure = new ArrayList<MediaPackageElement>(1);
          enclosure.add(e);
          FeedEntry entry = createEntry(feed, title, link, entryUri);
          entry = populateFeedEntry(entry, resultItem, enclosure);
          entry.setUri(entry.getUri() + "/" + e.getIdentifier());
          feed.addEntry(entry);
        }
        break;
      case Atom:
        entryUri = generateEntryUri(resultItem.getId());
        FeedEntry entry = createEntry(feed, title, link, entryUri);
        entry = populateFeedEntry(entry, resultItem, enclosures);
        if (getLinkSelf() != null) {
          LinkImpl self = new LinkImpl(getSelfLinkForEntry(feed, resultItem));
          self.setRel("self");
          entry.addLink(self);
        }
        feed.addEntry(entry);
        if (feed.getUpdatedDate() == null)
          feed.setUpdatedDate(entry.getUpdatedDate());
        else if (entry.getUpdatedDate().before(feed.getUpdatedDate()))
          feed.setUpdatedDate(entry.getUpdatedDate());
        break;
      default:
        throw new IllegalStateException("Unsupported feed type " + feed.getType());
    }

    return feed;
  }

  protected String generateEntryUri(String id) {
    return serverUrl + "/entry-uri/" + id;
  }

  /**
   * Populates the feed entry with metadata and the enclosures.
   *
   * @param entry
   *          the entry to enrich
   * @param metadata
   *          the metadata
   * @param enclosures
   *          the media enclosures
   * @return the enriched item
   */
  private FeedEntry populateFeedEntry(FeedEntry entry, SearchResultItem metadata, List<MediaPackageElement> enclosures) {
    Date d = metadata.getDcCreated();
    Date updatedDate = metadata.getModified();
    String title = metadata.getDcTitle();

    // Configure the iTunes extension

    ITunesFeedEntryExtension iTunesEntry = new ITunesFeedEntryExtension();
    iTunesEntry.setDuration(metadata.getDcExtent());
    iTunesEntry.setBlocked(false);
    iTunesEntry.setExplicit(false);
    if (StringUtils.isNotBlank(metadata.getDcCreator()))
      iTunesEntry.setAuthor(metadata.getDcCreator());
    // TODO: Add iTunes keywords and subtitles
    // iTunesEntry.setKeywords(keywords);
    // iTunesEntry.setSubtitle(subtitle);

    // Configure the DC extension

    DublinCoreExtension dcExtension = new DublinCoreExtension();
    dcExtension.setTitle(title);
    dcExtension.setIdentifier(metadata.getId());

    // Set contributor
    if (!StringUtils.isEmpty(metadata.getDcContributor())) {
      for (String contributor : metadata.getDcContributor().split(";;")) {
        entry.addContributor(new PersonImpl(contributor));
        dcExtension.addContributor(contributor);
      }
    }

    // Set creator
    if (!StringUtils.isEmpty(metadata.getDcCreator())) {
      for (String creator : metadata.getDcCreator().split(";;")) {
        if (iTunesEntry.getAuthor() == null)
          iTunesEntry.setAuthor(creator);
        entry.addAuthor(new PersonImpl(creator));
        dcExtension.addCreator(creator);
      }
    }

    // Set publisher
    if (!StringUtils.isEmpty(metadata.getDcPublisher())) {
      dcExtension.addPublisher(metadata.getDcPublisher());
    }

    // Set rights
    if (!StringUtils.isEmpty(metadata.getDcAccessRights())) {
      dcExtension.setRights(metadata.getDcAccessRights());
    }

    // Set description
    if (!StringUtils.isEmpty(metadata.getDcDescription())) {
      String summary = metadata.getDcDescription();
      entry.setDescription(new ContentImpl(summary));
      iTunesEntry.setSummary(summary);
      dcExtension.setDescription(summary);
    }

    // Set the language
    if (!StringUtils.isEmpty(metadata.getDcLanguage())) {
      dcExtension.setLanguage(metadata.getDcLanguage());
    }

    // Set the publication date
    if (d != null) {
      entry.setPublishedDate(d);
      dcExtension.setDate(d);
    } else if (metadata.getModified() != null) {
      entry.setPublishedDate(metadata.getModified());
      dcExtension.setDate(metadata.getModified());
    }

    // Set the updated date
    if (updatedDate == null)
      updatedDate = d;
    entry.setUpdatedDate(updatedDate);

    // TODO: Finish dc support

    // Set format
    // if (!StringUtils.isEmpty(resultItem.getMediaType())) {
    // dcExtension.setFormat(resultItem.getMediaType());
    // }

    // dcEntry.setCoverage(arg0);
    // dcEntry.setRelation(arg0);
    // dcEntry.setSource(arg0);
    // dcEntry.setSubject(arg0);

    // Set the cover image
    String coverUrl = null;
    if (!StringUtils.isEmpty(metadata.getCover())) {
      coverUrl = metadata.getCover();
      setImage(entry, coverUrl);
    }

    entry.addExtension(iTunesEntry);
    entry.addExtension(dcExtension);

    // Add the enclosures
    for (MediaPackageElement element : enclosures) {

      String trackMimeType = element.getMimeType().toString();
      long trackLength = element.getSize();
      if (trackLength <= 0 && element instanceof Track) {
        // filesize unset so estimate from duration and bitrate
        trackLength = 0;
        if (((TrackImpl) element).hasVideo()) {
          List<VideoStream> video = ((TrackImpl) element).getVideo();
          trackLength += metadata.getDcExtent() / 1000 * video.get(0).getBitRate() / 8;
        }

        if (((TrackImpl) element).hasAudio()) {
          List<AudioStream> audio = ((TrackImpl) element).getAudio();
          trackLength += metadata.getDcExtent() / 1000 * audio.get(0).getBitRate() / 8;
        }
      }

      // if no bitrate data default to value of duration which is probably within an
      // order of magnitude correct
      if (trackLength <= 0) {
        trackLength = metadata.getDcExtent();
      }

      String trackFlavor = element.getFlavor().toString();

      String trackUrl = null;
      try {
        trackUrl = element.getURI().toURL().toExternalForm();
      } catch (MalformedURLException e) {
        // Can't happen
      }

      Enclosure enclosure = new EnclosureImpl(trackUrl, trackMimeType, trackFlavor, trackLength);
      entry.addEnclosure(enclosure);
    }

    return entry;
  }

  /**
   * Creates a new feed entry that can be added to the feed.
   *
   * @param feed
   *          the feed that is being created
   * @param title
   *          the entry title
   * @param link
   *          link to the orginal resource
   * @param uri
   *          the entry uri
   * @return the feed
   */
  protected FeedEntry createEntry(Feed feed, String title, String link, String uri) {
    return createEntry(feed, title, null, link, uri);
  }

  /**
   * Creates a new feed entry that can be added to the feed.
   *
   * @param feed
   *          the feed that is being created
   * @param title
   *          the entry title
   * @param description
   *          the entry description
   * @param link
   *          link to the orginal resource
   * @param uri
   *          the entry uri
   * @return the feed
   */
  protected FeedEntry createEntry(Feed feed, String title, String description, String link, String uri) {
    if (feed == null)
      throw new IllegalStateException("Feed must be created prior to creating feed entries");
    FeedEntryImpl entry = new FeedEntryImpl(feed, title, description, new LinkImpl(link), uri);
    return entry;
  }

  /**
   * Adds the image as a content element to the feed entry.
   *
   * @param entry
   *          the feed entry
   * @param imageUrl
   *          the image url
   * @return the image
   */
  protected Content setImage(FeedEntry entry, String imageUrl) {
    StringBuffer buf = new StringBuffer("<div xmlns=\"http://www.w3.org/1999/xhtml\">");
    buf.append("<img src=\"");
    buf.append(imageUrl);
    buf.append("\" />");
    buf.append("</div>");
    Content image = new ContentImpl(buf.toString(), "application/xhtml+xml", Mode.Xml);
    entry.addContent(image);
    return image;
  }

  /**
   * Sets the rss media types.
   *
   * @param mediaTypes
   *          The ordered array of media types to choose for RSS feeds.
   */
  protected void setRssMediatypes(String[] mediaTypes) {
    if (mediaTypes == null || mediaTypes.length == 0) {
      throw new IllegalArgumentException("mediaTypes are required for an rss feed");
    }
    this.rssMediaTypes.clear();
    this.rssMediaTypes.addAll(Arrays.asList(mediaTypes));
  }

  /**
   * Returns an ordered list of media types for the RSS feed. The feed will select the first media type where a track is
   * available.
   *
   * @return The rss media types
   */
  protected List<String> getRssMediaTypes() {
    return rssMediaTypes;
  }

  /**
   * Adds the flavor to the set of flavors of tracks that are to be included in rss feeds.
   *
   * @param flavor
   *          the flavor to add
   */
  protected void addRssTrackFlavor(MediaPackageElementFlavor flavor) {
    rssTrackFlavors.add(flavor);
  }

  /**
   * Removes the flavor from the set of flavors of tracks that are to be included in rss feeds.
   *
   * @param flavor
   *          the flavor to add
   */
  protected void removeRssTrackFlavor(MediaPackageElementFlavor flavor) {
    rssTrackFlavors.remove(flavor);
  }

  /**
   * Returns the ordered list of flavors of the tracks to be included in rss feeds. If the first flavor specified in
   * this list is not available, we try to use the next flavor, until a track is found or no more flavors are available.
   *
   * @return the flavors for rss feed tracks
   */
  protected List<MediaPackageElementFlavor> getRssTrackFlavors() {
    return rssTrackFlavors;
  }

  /**
   * Adds the flavor to the set of flavors of tracks that are to be included in atom feeds.
   *
   * @param flavor
   *          the flavor to add
   */
  protected void addAtomTrackFlavor(MediaPackageElementFlavor flavor) {
    atomTrackFlavors.add(flavor);
  }

  /**
   * Removes the flavor from the set of flavors of tracks that are to be included in atom feeds.
   *
   * @param flavor
   *          the flavor to add
   */
  protected void removeAtomTrackFlavor(MediaPackageElementFlavor flavor) {
    atomTrackFlavors.remove(flavor);
  }

  /**
   * Returns the flavors of the tracks to be included in atom feeds.
   *
   * @return the flavors for atom feed tracks
   */
  protected Set<MediaPackageElementFlavor> getAtomTrackFlavors() {
    return atomTrackFlavors;
  }

  /**
   * Adds the tag to the set of tags that identify the tracks that are to be included in atom feeds.
   *
   * @param tag
   *          the tag to add
   */
  protected void addAtomTag(String tag) {
    atomTags.add(tag);
  }

  /**
   * Removes the tag from the set of tags that identify the tracks that are to be included in atom feeds.
   *
   * @param tag
   *          the tag to add
   */
  protected void removeAtomTag(String tag) {
    atomTags.remove(tag);
  }

  /**
   * Returns the tags of the tracks to be included in atom feeds.
   *
   * @return the tags for atom feed tracks
   */
  protected Set<String> getAtomTags() {
    return atomTags;
  }

  /**
   * Adds the tag to the set of tags that identify the tracks that are to be included in rss feeds.
   *
   * @param tag
   *          the tag to add
   */
  protected void addRSSTag(String tag) {
    rssTags.add(tag);
  }

  /**
   * Removes the tag from the set of tags that identify the tracks that are to be included in rss feeds.
   *
   * @param tag
   *          the tag to add
   */
  protected void removeRSSTag(String tag) {
    rssTags.remove(tag);
  }

  /**
   * Returns the tags of the tracks to be included in rss feeds.
   *
   * @return the tags for rss feed tracks
   */
  protected Set<String> getRSSTags() {
    return rssTags;
  }

  /**
   * Returns the identifier of those tracks that are to be included as enclosures in the feed. Note that for a feed type
   * of {@link Feed.Type#RSS}, the list must exactly contain one single entry.
   * <p>
   * This default implementation will include the track identified by the flavor as specified in the constructor for rss
   * feeds and every distribution track for atom feeds.
   *
   * @param feed
   *          the feed
   * @param resultItem
   *          the result item
   * @return the set of identifier
   */
  protected List<MediaPackageElement> getEnclosures(Feed feed, SearchResultItem resultItem) {
    MediaPackage mediaPackage = resultItem.getMediaPackage();

    List<MediaPackageElement> candidateElements = new ArrayList<MediaPackageElement>();
    List<MediaPackageElementFlavor> flavors = new ArrayList<MediaPackageElementFlavor>();
    Set<String> tags = new HashSet<String>();

    switch (feed.getType()) {
      case Atom:
        flavors.addAll(atomTrackFlavors);
        tags.addAll(atomTags);
        break;
      default:
        flavors.addAll(rssTrackFlavors);
        tags.addAll(rssTags);
        break;
    }

    // Collect track id's by flavor
    if (flavors.size() > 0) {
      for (MediaPackageElementFlavor flavor : flavors) {
        MediaPackageElement[] elements = mediaPackage.getElementsByFlavor(flavor);
        for (MediaPackageElement element : elements) {
          if (element.containsTag(tags)) {
            candidateElements.add(element);
          }
        }
      }
    }

    if (candidateElements.size() == 0) {
      logger.debug("No distributed media found for feed entry");
    }

    return candidateElements;
  }

  /**
   * Sets the url to the feed's homepage.
   *
   * @param url
   *          the homepage
   */
  public void setFeedLink(String url) {
    this.home = url;
  }

  /**
   * Returns the url to the feed's homepage.
   *
   * @return the feed home
   */
  public String getFeedLink() {
    return home;
  }

  /**
   * Sets the entry's base url that will be used to form the episode link in the feeds. If the url contains a
   * placeholder in the form <code>{0}</code>, it will be replaced by the episode id.
   *
   * @param url
   *          the url
   */
  public void setLinkTemplate(String url) {
    linkTemplate = url;
  }

  /**
   * Returns the link template to the default user interface.
   *
   * @return the link to the ui
   */
  public String getLinkTemplate() {
    return linkTemplate;
  }

  /**
   * Sets the entry's base url that will be used to form the alternate episode link in the feeds. If the url contains a
   * placeholder in the form <code>{0}</code>, it will be replaced by the episode id.
   *
   * @param url
   *          the url
   */
  public void setLinkSelf(String url) {
    linkSelf = url;
  }

  /**
   * Returns the self link template to the default user interface.
   *
   * @return the link to the ui
   */
  public String getLinkSelf() {
    return linkSelf;
  }

  /**
   * Generates a link for the current feed entry by using the entry identifier and the result of
   * {@link #getLinkTemplate()} to create the url. Overwrite this method to provide your own way of generating links to
   * feed entries.
   *
   * @param feed
   *          the feed
   * @param solrResultItem
   *          solr search result for this feed entry
   * @return the link to the ui
   */
  protected String getLinkForEntry(Feed feed, SearchResultItem solrResultItem) {
    if (linkTemplate == null)
      throw new IllegalStateException("No template defined");
    return MessageFormat.format(linkTemplate, solrResultItem.getId());
  }

  /**
   * Generates a link for the current feed entry by using the entry identifier and the result of {@link #getLinkSelf()}
   * to create the url. Overwrite this method to provide your own way of generating links to feed entries.
   *
   * @param feed
   *          the feed
   * @param solrResultItem
   *          solr search result for this feed entry
   * @return the link to the ui
   */
  protected String getSelfLinkForEntry(Feed feed, SearchResultItem solrResultItem) {
    if (linkSelf == null)
      return linkSelf;
    return MessageFormat.format(linkSelf, solrResultItem.getId());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return uri.hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FeedGenerator))
      return false;
    FeedGenerator generator = (FeedGenerator) o;
    return getIdentifier().equals(generator.getIdentifier());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (this.getName() != null)
      return getName();
    return super.toString();
  }

}
