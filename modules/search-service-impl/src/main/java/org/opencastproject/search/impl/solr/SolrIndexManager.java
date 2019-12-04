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


package org.opencastproject.search.impl.solr;

import static org.opencastproject.security.api.Permissions.Action.READ;
import static org.opencastproject.security.api.Permissions.Action.WRITE;
import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.data.Collections.flatMap;
import static org.opencastproject.util.data.Collections.head;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.metadata.api.MetadataValue;
import org.opencastproject.metadata.api.StaticMetadata;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.api.util.Interval;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Temporal;
import org.opencastproject.metadata.mpeg7.AudioVisual;
import org.opencastproject.metadata.mpeg7.FreeTextAnnotation;
import org.opencastproject.metadata.mpeg7.KeywordAnnotation;
import org.opencastproject.metadata.mpeg7.MediaDuration;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.MultimediaContent;
import org.opencastproject.metadata.mpeg7.MultimediaContentType;
import org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition;
import org.opencastproject.metadata.mpeg7.TextAnnotation;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.metadata.mpeg7.VideoSegment;
import org.opencastproject.metadata.mpeg7.VideoText;
import org.opencastproject.search.api.SearchResultItem.SearchResultItemType;
import org.opencastproject.search.impl.persistence.SearchServiceDatabaseException;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.servlet.SolrRequestParsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Utility class used to manage the search index.
 */
public class SolrIndexManager {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SolrIndexManager.class);

  /** Connection to the database */
  private SolrServer solrServer = null;

  /**
   * Factor multiplied to fine tune relevance and confidence impact on important keyword decision. importance =
   * RELEVANCE_BOOST * relevance + confidence
   */
  private static final double RELEVANCE_BOOST = 2.0;

  /** Number of characters an important should have at least. */
  private static final int MAX_CHAR = 3;

  /** Maximum number of important keywords to detect. */
  private static final int MAX_IMPORTANT_COUNT = 10;

  /** List of metadata services sorted by priority in reverse order. */
  private List<StaticMetadataService> mdServices;

  private SeriesService seriesService;

  private Mpeg7CatalogService mpeg7CatalogService;

  private Workspace workspace;

  private SecurityService securityService;

  /** Convert a DublinCoreValue into a date. */
  private static Function<DublinCoreValue, Option<Date>> toDateF = new Function<DublinCoreValue, Option<Date>>() {
    @Override
    public Option<Date> apply(DublinCoreValue v) {
      return EncodingSchemeUtils.decodeTemporal(v).fold(new Temporal.Match<Option<Date>>() {
        @Override
        public Option<Date> period(DCMIPeriod period) {
          return option(period.getStart());
        }

        @Override
        public Option<Date> instant(Date instant) {
          return Option.some(instant);
        }

        @Override
        public Option<Date> duration(long duration) {
          return Option.none();
        }
      });
    }
  };

  /** Convert a DublinCoreValue into a duration (long). */
  private static Function<DublinCoreValue, Option<Long>> toDurationF = new Function<DublinCoreValue, Option<Long>>() {
    @Override
    public Option<Long> apply(DublinCoreValue dublinCoreValue) {
      return option(EncodingSchemeUtils.decodeDuration(dublinCoreValue));
    }
  };

  /** Dynamic reference. */
  public void setStaticMetadataServices(List<StaticMetadataService> mdServices) {
    this.mdServices = new ArrayList<StaticMetadataService>(mdServices);
    Collections.sort(this.mdServices, new Comparator<StaticMetadataService>() {
      @Override
      public int compare(StaticMetadataService a, StaticMetadataService b) {
        return b.getPriority() - a.getPriority();
      }
    });
  }

  /**
   * Creates a new management instance for the search index.
   *
   * @param connection
   *          connection to the database
   */
  public SolrIndexManager(SolrServer connection, Workspace workspace, List<StaticMetadataService> mdServices,
          SeriesService seriesService, Mpeg7CatalogService mpeg7CatalogService, SecurityService securityService) {

    this.solrServer = notNull(connection, "solr connection");
    this.workspace = notNull(workspace, "workspace");
    this.seriesService = notNull(seriesService, "series service");
    this.mpeg7CatalogService = notNull(mpeg7CatalogService, "mpeg7 service");
    this.securityService = notNull(securityService, "security service");
    setStaticMetadataServices(notNull(mdServices, "metadata service"));
  }

  /**
   * Clears the search index. Make sure you know what you are doing.
   *
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public void clear() throws SolrServerException {
    try {
      solrServer.deleteByQuery("*:*");
      solrServer.commit();
    } catch (IOException e) {
      throw new SolrServerException(e);
    }
  }

  /**
   * Removes the entry with the given <code>id</code> from the database. The entry can either be a series or an episode.
   *
   * @param id
   *          identifier of the series or episode to delete
   * @param deletionDate
   *          the deletion date
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public boolean delete(String id, Date deletionDate) throws SolrServerException {
    try {
      // Load the existing episode
      QueryResponse solrResponse = null;
      try {
        SolrQuery query = new SolrQuery(Schema.ID + ":" + ClientUtils.escapeQueryChars(id) + " AND -"
                + Schema.OC_DELETED + ":[* TO *]");
        solrResponse = solrServer.query(query);
      } catch (Exception e1) {
        throw new SolrServerException(e1);
      }

      // Did we find the episode?
      if (solrResponse.getResults().size() == 0) {
        logger.warn("Trying to delete non-existing media package {} from the search index", id);
        return false;
      }

      // Use all existing fields
      SolrDocument doc = solrResponse.getResults().get(0);
      SolrInputDocument inputDocument = new SolrInputDocument();
      for (String field : doc.getFieldNames()) {
        inputDocument.setField(field, doc.get(field));
      }

      // Set the oc_deleted field to the current date, then update
      Schema.setOcDeleted(inputDocument, deletionDate);
      solrServer.add(inputDocument);
      solrServer.commit();
      return true;
    } catch (IOException e) {
      throw new SolrServerException(e);
    }
  }

  /**
   * Posts the media package to solr. Depending on what is referenced in the media package, the method might create one
   * or two entries: one for the episode and one for the series that the episode belongs to.
   *
   * This implementation of the search service removes all references to non "engage/download" media tracks
   *
   * @param sourceMediaPackage
   *          the media package to post
   * @param acl
   *          the access control list for this mediapackage
   * @param now
   *          current date
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public boolean add(MediaPackage sourceMediaPackage, AccessControlList acl, Date now) throws SolrServerException,
          UnauthorizedException {
    try {
      SolrInputDocument episodeDocument = createEpisodeInputDocument(sourceMediaPackage, acl);
      Schema.setOcModified(episodeDocument, now);

      SolrInputDocument seriesDocument = createSeriesInputDocument(sourceMediaPackage.getSeries(), acl);
      if (seriesDocument != null)
        Schema.enrich(episodeDocument, seriesDocument);

      // If neither an episode nor a series was contained, there is no point in trying to update
      if (episodeDocument == null && seriesDocument == null) {
        logger.warn("Neither episode nor series metadata found");
        return false;
      }

      // Post everything to the search index
      if (episodeDocument != null)
        solrServer.add(episodeDocument);
      if (seriesDocument != null)
        solrServer.add(seriesDocument);
      solrServer.commit();
      return true;
    } catch (Exception e) {
      logger.error("Unable to add mediapackage {} to index", sourceMediaPackage.getIdentifier());
      throw new SolrServerException(e);
    }
  }

  /**
   * Posts the media package to solr. Depending on what is referenced in the media package, the method might create one
   * or two entries: one for the episode and one for the series that the episode belongs to.
   *
   * This implementation of the search service removes all references to non "engage/download" media tracks
   *
   * @param sourceMediaPackage
   *          the media package to post
   * @param acl
   *          the access control list for this mediapackage
   * @param deletionDate
   *          the deletion date
   * @param modificationDate
   *          the modification date
   * @return <code>true</code> if successfully added
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public boolean add(MediaPackage sourceMediaPackage, AccessControlList acl, Date deletionDate, Date modificationDate)
          throws SolrServerException {
    try {
      SolrInputDocument episodeDocument = createEpisodeInputDocument(sourceMediaPackage, acl);

      SolrInputDocument seriesDocument = createSeriesInputDocument(sourceMediaPackage.getSeries(), acl);
      if (seriesDocument != null)
        Schema.enrich(episodeDocument, seriesDocument);

      Schema.setOcModified(episodeDocument, modificationDate);
      if (deletionDate != null)
        Schema.setOcDeleted(episodeDocument, deletionDate);

      solrServer.add(episodeDocument);
      solrServer.add(seriesDocument);
      solrServer.commit();
      return true;
    } catch (Exception e) {
      logger.error("Unable to add mediapackage {} to index", sourceMediaPackage.getIdentifier());
      try {
        solrServer.rollback();
      } catch (IOException e1) {
        throw new SolrServerException(e1);
      }
      throw new SolrServerException(e);
    }
  }

  /**
   * Creates a solr input document for the episode metadata of the media package.
   *
   * @param mediaPackage
   *          the media package
   * @param acl
   *          the access control list for this mediapackage
   * @return an input document ready to be posted to solr
   * @throws MediaPackageException
   *           if serialization of the media package fails
   */
  private SolrInputDocument createEpisodeInputDocument(MediaPackage mediaPackage, AccessControlList acl)
          throws MediaPackageException, IOException {

    SolrInputDocument doc = new SolrInputDocument();
    String mediaPackageId = mediaPackage.getIdentifier().toString();

    // Fill the input document
    Schema.setId(doc, mediaPackageId);
    // /
    // OC specific fields
    Schema.setOcMediatype(doc, SearchResultItemType.AudioVisual.toString());
    Schema.setOrganization(doc, securityService.getOrganization().getId());
    Schema.setOcMediapackage(doc, MediaPackageParser.getAsXml(mediaPackage));
    Schema.setOcElementtags(doc, tags(mediaPackage));
    Schema.setOcElementflavors(doc, flavors(mediaPackage));
    // Add cover
    Attachment[] cover = mediaPackage.getAttachments(MediaPackageElements.MEDIAPACKAGE_COVER_FLAVOR);
    if (cover != null && cover.length > 0) {
      Schema.setOcCover(doc, cover[0].getURI().toString());
    }

    // /
    // Add standard dublin core fields
    // naive approach. works as long as only setters, not adders are available in the schema
    for (StaticMetadata md : getMetadata(mdServices, mediaPackage))
      addEpisodeMetadata(doc, md);

    // /
    // Add mpeg7
    logger.debug("Looking for mpeg-7 catalogs containing segment texts");
    Catalog[] mpeg7Catalogs = mediaPackage.getCatalogs(MediaPackageElements.TEXTS);
    if (mpeg7Catalogs.length == 0) {
      logger.debug("No text catalogs found, trying segments only");
      mpeg7Catalogs = mediaPackage.getCatalogs(MediaPackageElements.SEGMENTS);
    }
    // TODO: merge the segments from each mpeg7 if there is more than one mpeg7 catalog
    if (mpeg7Catalogs.length > 0) {
      try {
        Mpeg7Catalog mpeg7Catalog = loadMpeg7Catalog(mpeg7Catalogs[0]);
        addMpeg7Metadata(doc, mediaPackage, mpeg7Catalog);
      } catch (IOException e) {
        logger.error("Error loading mpeg7 catalog. Skipping catalog: {}", e.getMessage());
      }
    } else {
      logger.debug("No segmentation catalog found");
    }

    // /
    // Add authorization
    setAuthorization(doc, securityService, acl);

    return doc;
  }

  static void addEpisodeMetadata(final SolrInputDocument doc, final StaticMetadata md) {
    Schema.fill(doc, new Schema.FieldCollector() {
      @Override
      public Option<String> getId() {
        return Option.none();
      }

      @Override
      public Option<String> getOrganization() {
        return Option.none();
      }

      @Override
      public Option<Date> getDcCreated() {
        return md.getCreated();
      }

      @Override
      public Option<Long> getDcExtent() {
        return md.getExtent();
      }

      @Override
      public Option<String> getDcLanguage() {
        return md.getLanguage();
      }

      @Override
      public Option<String> getDcIsPartOf() {
        return md.getIsPartOf();
      }

      @Override
      public Option<String> getDcReplaces() {
        return md.getReplaces();
      }

      @Override
      public Option<String> getDcType() {
        return md.getType();
      }

      @Override
      public Option<Date> getDcAvailableFrom() {
        return md.getAvailable().flatMap(new Function<Interval, Option<Date>>() {
          @Override
          public Option<Date> apply(Interval interval) {
            return interval.fold(new Interval.Match<Option<Date>>() {
              @Override
              public Option<Date> bounded(Date leftBound, Date rightBound) {
                return Option.some(leftBound);
              }

              @Override
              public Option<Date> leftInfinite(Date rightBound) {
                return Option.none();
              }

              @Override
              public Option<Date> rightInfinite(Date leftBound) {
                return Option.some(leftBound);
              }
            });
          }
        });
      }

      @Override
      public Option<Date> getDcAvailableTo() {
        return md.getAvailable().flatMap(new Function<Interval, Option<Date>>() {
          @Override
          public Option<Date> apply(Interval interval) {
            return interval.fold(new Interval.Match<Option<Date>>() {
              @Override
              public Option<Date> bounded(Date leftBound, Date rightBound) {
                return Option.some(rightBound);
              }

              @Override
              public Option<Date> leftInfinite(Date rightBound) {
                return Option.some(rightBound);
              }

              @Override
              public Option<Date> rightInfinite(Date leftBound) {
                return Option.none();
              }
            });
          }
        });
      }

      @Override
      public List<DField<String>> getDcTitle() {
        return fromMValue(md.getTitles());
      }

      @Override
      public List<DField<String>> getDcSubject() {
        return fromMValue(md.getSubjects());
      }

      @Override
      public List<DField<String>> getDcCreator() {
        return fromMValue(md.getCreators());
      }

      @Override
      public List<DField<String>> getDcPublisher() {
        return fromMValue(md.getPublishers());
      }

      @Override
      public List<DField<String>> getDcContributor() {
        return fromMValue(md.getContributors());
      }

      @Override
      public List<DField<String>> getDcDescription() {
        return fromMValue(md.getDescription());
      }

      @Override
      public List<DField<String>> getDcRightsHolder() {
        return fromMValue(md.getRightsHolders());
      }

      @Override
      public List<DField<String>> getDcSpatial() {
        return fromMValue(md.getSpatials());
      }

      @Override
      public List<DField<String>> getDcAccessRights() {
        return fromMValue(md.getAccessRights());
      }

      @Override
      public List<DField<String>> getDcLicense() {
        return fromMValue(md.getLicenses());
      }

      @Override
      public Option<String> getOcMediatype() {
        return Option.none(); // set elsewhere
      }

      @Override
      public Option<String> getOcMediapackage() {
        return Option.none(); // set elsewhere
      }

      @Override
      public Option<String> getOcKeywords() {
        return Option.none(); // set elsewhere
      }

      @Override
      public Option<String> getOcCover() {
        return Option.none(); // set elsewhere
      }

      @Override
      public Option<Date> getOcModified() {
        return Option.none(); // set elsewhere
      }

      @Override
      public Option<Date> getOcDeleted() {
        return Option.none(); // set elsewhere
      }

      @Override
      public Option<String> getOcElementtags() {
        return Option.none(); // set elsewhere
      }

      @Override
      public Option<String> getOcElementflavors() {
        return Option.none(); // set elsewhere
      }

      @Override
      public List<DField<String>> getOcAcl() {
        return Collections.EMPTY_LIST; // set elsewhere
      }

      @Override
      public List<DField<String>> getSegmentText() {
        return Collections.EMPTY_LIST; // set elsewhere
      }

      @Override
      public List<DField<String>> getSegmentHint() {
        return Collections.EMPTY_LIST; // set elsewhere
      }
    });
  }

  static List<DField<String>> fromMValue(List<MetadataValue<String>> as) {
    return map(as, new ArrayList<DField<String>>(), new Function<MetadataValue<String>, DField<String>>() {
      @Override
      public DField<String> apply(MetadataValue<String> v) {
        return new DField<String>(v.getValue(), v.getLanguage());
      }
    });
  }

  static List<DField<String>> fromDCValue(List<DublinCoreValue> as) {
    return map(as, new ArrayList<DField<String>>(), new Function<DublinCoreValue, DField<String>>() {
      @Override
      public DField<String> apply(DublinCoreValue v) {
        return new DField<String>(v.getValue(), v.getLanguage());
      }
    });
  }

  /**
   * Adds authorization fields to the solr document.
   *
   * @param doc
   *          the solr document
   * @param acl
   *          the access control list
   */
  static void setAuthorization(SolrInputDocument doc, SecurityService securityService, AccessControlList acl) {
    Map<String, List<String>> permissions = new HashMap<String, List<String>>();

    // Define containers for common permissions
    List<String> reads = new ArrayList<String>();
    permissions.put(READ.toString(), reads);
    List<String> writes = new ArrayList<String>();
    permissions.put(WRITE.toString(), writes);

    String adminRole = securityService.getOrganization().getAdminRole();

    // The admin user can read and write
    if (adminRole != null) {
      reads.add(adminRole);
      writes.add(adminRole);
    }

    for (AccessControlEntry entry : acl.getEntries()) {
      if (!entry.isAllow()) {
        logger.warn("Search service does not support denial via ACL, ignoring {}", entry);
        continue;
      }
      List<String> actionPermissions = permissions.get(entry.getAction());
      /*
       * MH-8353 a series could have a permission defined we don't know how to handle -DH
       */
      if (actionPermissions == null) {
        logger.warn("Search service doesn't know how to handle action: " + entry.getAction());
        continue;
      }
      if (acl == null) {
        actionPermissions = new ArrayList<String>();
        permissions.put(entry.getAction(), actionPermissions);
      }
      actionPermissions.add(entry.getRole());

    }

    // Write the permissions to the solr document
    for (Map.Entry<String, List<String>> entry : permissions.entrySet()) {
      Schema.setOcAcl(doc, new DField<String>(mkString(entry.getValue(), " "), entry.getKey()));
    }
  }

  static String mkString(Collection<?> as, String sep) {
    StringBuffer b = new StringBuffer();
    for (Object a : as) {
      b.append(a).append(sep);
    }
    return b.substring(0, b.length() - sep.length());
  }

  private Mpeg7Catalog loadMpeg7Catalog(Catalog catalog) throws IOException {
    try (InputStream in = workspace.read(catalog.getURI())) {
      return mpeg7CatalogService.load(in);
    } catch (NotFoundException e) {
      throw new IOException("Unable to load metadata from mpeg7 catalog " + catalog);
    }
  }

  /**
   * Creates a solr input document for the series metadata of the media package.
   *
   * @param seriesId
   *          the id of the series
   * @param acl
   *          the access control list for this mediapackage
   * @return an input document ready to be posted to solr or null
   */
  private SolrInputDocument createSeriesInputDocument(String seriesId, AccessControlList acl) throws IOException,
          UnauthorizedException {

    if (seriesId == null)
      return null;
    DublinCoreCatalog dc = null;
    try {
      dc = seriesService.getSeries(seriesId);
    } catch (SeriesException e) {
      logger.debug("No series dublincore found for series id " + seriesId);
      return null;
    } catch (NotFoundException e) {
      logger.debug("No series dublincore found for series id " + seriesId);
      return null;
    }

    SolrInputDocument doc = new SolrInputDocument();

    // Populate document with existing data
    try {
      StringBuffer query = new StringBuffer("q=");
      query = query.append(Schema.ID).append(":").append(SolrUtils.clean(seriesId));
      SolrParams params = SolrRequestParsers.parseQueryString(query.toString());
      QueryResponse solrResponse = solrServer.query(params);
      if (solrResponse.getResults().size() > 0) {
        SolrDocument existingSolrDocument = solrResponse.getResults().get(0);
        for (String fieldName : existingSolrDocument.getFieldNames()) {
          doc.addField(fieldName, existingSolrDocument.getFieldValue(fieldName));
        }
      }
    } catch (Exception e) {
      logger.error("Error trying to load series " + seriesId, e);
    }

    // Fill document
    Schema.setId(doc, seriesId);

    // OC specific fields
    Schema.setOrganization(doc, securityService.getOrganization().getId());
    Schema.setOcMediatype(doc, SearchResultItemType.Series.toString());
    Schema.setOcModified(doc, new Date());

    // DC fields
    addSeriesMetadata(doc, dc);

    // Authorization
    setAuthorization(doc, securityService, acl);

    return doc;
  }

  /**
   * Add the standard dublin core fields to a series document.
   *
   * @param doc
   *          the solr document to fill
   * @param dc
   *          the dublin core catalog to get the data from
   */
  static void addSeriesMetadata(final SolrInputDocument doc, final DublinCoreCatalog dc) throws IOException {
    Schema.fill(doc, new Schema.FieldCollector() {
      @Override
      public Option<String> getId() {
        return Option.some(dc.getFirst(DublinCore.PROPERTY_IDENTIFIER));
      }

      @Override
      public Option<String> getOrganization() {
        return Option.none();
      }

      @Override
      public Option<Date> getDcCreated() {
        return head(dc.get(DublinCore.PROPERTY_CREATED)).flatMap(toDateF);
      }

      @Override
      public Option<Long> getDcExtent() {
        return head(dc.get(DublinCore.PROPERTY_EXTENT)).flatMap(toDurationF);
      }

      @Override
      public Option<String> getDcLanguage() {
        return option(dc.getFirst(DublinCore.PROPERTY_LANGUAGE));
      }

      @Override
      public Option<String> getDcIsPartOf() {
        return option(dc.getFirst(DublinCore.PROPERTY_IS_PART_OF));
      }

      @Override
      public Option<String> getDcReplaces() {
        return option(dc.getFirst(DublinCore.PROPERTY_REPLACES));
      }

      @Override
      public Option<String> getDcType() {
        return option(dc.getFirst(DublinCore.PROPERTY_TYPE));
      }

      @Override
      public Option<Date> getDcAvailableFrom() {
        return option(dc.getFirst(DublinCore.PROPERTY_AVAILABLE)).flatMap(new Function<String, Option<Date>>() {
          @Override
          public Option<Date> apply(String s) {
            return option(EncodingSchemeUtils.decodePeriod(s).getStart());
          }
        });
      }

      @Override
      public Option<Date> getDcAvailableTo() {
        return option(dc.getFirst(DublinCore.PROPERTY_AVAILABLE)).flatMap(new Function<String, Option<Date>>() {
          @Override
          public Option<Date> apply(String s) {
            return option(EncodingSchemeUtils.decodePeriod(s).getEnd());
          }
        });
      }

      @Override
      public List<DField<String>> getDcTitle() {
        return fromDCValue(dc.get(DublinCore.PROPERTY_TITLE));
      }

      @Override
      public List<DField<String>> getDcSubject() {
        return fromDCValue(dc.get(DublinCore.PROPERTY_SUBJECT));
      }

      @Override
      public List<DField<String>> getDcCreator() {
        return fromDCValue(dc.get(DublinCore.PROPERTY_CREATOR));
      }

      @Override
      public List<DField<String>> getDcPublisher() {
        return fromDCValue(dc.get(DublinCore.PROPERTY_PUBLISHER));
      }

      @Override
      public List<DField<String>> getDcContributor() {
        return fromDCValue(dc.get(DublinCore.PROPERTY_CONTRIBUTOR));

      }

      @Override
      public List<DField<String>> getDcDescription() {
        return fromDCValue(dc.get(DublinCore.PROPERTY_DESCRIPTION));
      }

      @Override
      public List<DField<String>> getDcRightsHolder() {
        return fromDCValue(dc.get(DublinCore.PROPERTY_RIGHTS_HOLDER));
      }

      @Override
      public List<DField<String>> getDcSpatial() {
        return fromDCValue(dc.get(DublinCore.PROPERTY_SPATIAL));
      }

      @Override
      public List<DField<String>> getDcAccessRights() {
        return fromDCValue(dc.get(DublinCore.PROPERTY_ACCESS_RIGHTS));
      }

      @Override
      public List<DField<String>> getDcLicense() {
        return fromDCValue(dc.get(DublinCore.PROPERTY_LICENSE));
      }

      @Override
      public Option<String> getOcMediatype() {
        return Option.none();
      }

      @Override
      public Option<String> getOcMediapackage() {
        return Option.none();
      }

      @Override
      public Option<String> getOcKeywords() {
        return Option.none();
      }

      @Override
      public Option<String> getOcCover() {
        return Option.none();
      }

      @Override
      public Option<Date> getOcModified() {
        return Option.none();
      }

      @Override
      public Option<Date> getOcDeleted() {
        return Option.none();
      }

      @Override
      public Option<String> getOcElementtags() {
        return Option.none();
      }

      @Override
      public Option<String> getOcElementflavors() {
        return Option.none();
      }

      @Override
      public List<DField<String>> getOcAcl() {
        return Collections.EMPTY_LIST;
      }

      @Override
      public List<DField<String>> getSegmentText() {
        return Collections.EMPTY_LIST;
      }

      @Override
      public List<DField<String>> getSegmentHint() {
        return Collections.EMPTY_LIST;
      }
    });
  }

  /**
   * Add the mpeg 7 catalog data to the solr document.
   *
   * @param doc
   *          the input document to the solr index
   * @param mpeg7
   *          the mpeg7 catalog
   */
  @SuppressWarnings("unchecked")
  static void addMpeg7Metadata(SolrInputDocument doc, MediaPackage mediaPackage, Mpeg7Catalog mpeg7) {

    // Check for multimedia content
    if (!mpeg7.multimediaContent().hasNext()) {
      logger.warn("Mpeg-7 doesn't contain  multimedia content");
      return;
    }

    // Get the content duration by looking at the first content track. This
    // of course assumes that all tracks are equally long.
    MultimediaContent<? extends MultimediaContentType> mc = mpeg7.multimediaContent().next();
    MultimediaContentType mct = mc.elements().next();
    MediaTime mediaTime = mct.getMediaTime();
    Schema.setDcExtent(doc, mediaTime.getMediaDuration().getDurationInMilliseconds());

    // Check if the keywords have been filled by (manually) added dublin
    // core data. If not, look for the most relevant fields in mpeg-7.
    SortedSet<TextAnnotation> sortedAnnotations = null;
    if (!"".equals(Schema.getOcKeywords(doc))) {
      sortedAnnotations = new TreeSet<TextAnnotation>(new Comparator<TextAnnotation>() {
        @Override
        public int compare(TextAnnotation a1, TextAnnotation a2) {
          if ((RELEVANCE_BOOST * a1.getRelevance() + a1.getConfidence()) > (RELEVANCE_BOOST * a2.getRelevance() + a2
                  .getConfidence()))
            return -1;
          else if ((RELEVANCE_BOOST * a1.getRelevance() + a1.getConfidence()) < (RELEVANCE_BOOST * a2.getRelevance() + a2
                  .getConfidence()))
            return 1;
          return 0;
        }
      });
    }

    // Iterate over the tracks and extract keywords and hints
    Iterator<MultimediaContent<? extends MultimediaContentType>> mmIter = mpeg7.multimediaContent();
    int segmentCount = 0;

    while (mmIter.hasNext()) {
      MultimediaContent<?> multimediaContent = mmIter.next();

      // We need to process visual segments first, due to the way they are handled in the ui.
      for (Iterator<?> iterator = multimediaContent.elements(); iterator.hasNext();) {

        MultimediaContentType type = (MultimediaContentType) iterator.next();
        if (!(type instanceof Video) && !(type instanceof AudioVisual))
          continue;

        // for every segment in the current multimedia content track

        Video video = (Video) type;
        Iterator<VideoSegment> vsegments = (Iterator<VideoSegment>) video.getTemporalDecomposition().segments();
        while (vsegments.hasNext()) {
          VideoSegment segment = vsegments.next();

          StringBuffer segmentText = new StringBuffer();
          StringBuffer hintField = new StringBuffer();

          // Collect the video text elements to a segment text
          SpatioTemporalDecomposition spt = segment.getSpatioTemporalDecomposition();
          if (spt != null) {
            for (VideoText videoText : spt.getVideoText()) {
              if (segmentText.length() > 0)
                segmentText.append(" ");
              segmentText.append(videoText.getText().getText());
              // TODO: Add hint on bounding box
            }
          }

          // Add keyword annotations
          Iterator<TextAnnotation> textAnnotations = segment.textAnnotations();
          while (textAnnotations.hasNext()) {
            TextAnnotation textAnnotation = textAnnotations.next();
            Iterator<?> kwIter = textAnnotation.keywordAnnotations();
            while (kwIter.hasNext()) {
              KeywordAnnotation keywordAnnotation = (KeywordAnnotation) kwIter.next();
              if (segmentText.length() > 0)
                segmentText.append(" ");
              segmentText.append(keywordAnnotation.getKeyword());
            }
          }

          // Add free text annotations
          Iterator<TextAnnotation> freeIter = segment.textAnnotations();
          if (freeIter.hasNext()) {
            Iterator<FreeTextAnnotation> freeTextIter = freeIter.next().freeTextAnnotations();
            while (freeTextIter.hasNext()) {
              FreeTextAnnotation freeTextAnnotation = freeTextIter.next();
              if (segmentText.length() > 0)
                segmentText.append(" ");
              segmentText.append(freeTextAnnotation.getText());
            }
          }

          // add segment text to solr document
          Schema.setSegmentText(doc, new DField<String>(segmentText.toString(), Integer.toString(segmentCount)));

          // get the segments time properties
          MediaTimePoint timepoint = segment.getMediaTime().getMediaTimePoint();
          MediaDuration duration = segment.getMediaTime().getMediaDuration();

          // TODO: define a class with hint field constants
          hintField.append("time=" + timepoint.getTimeInMilliseconds() + "\n");
          hintField.append("duration=" + duration.getDurationInMilliseconds() + "\n");

          // Look for preview images. Their characteristics are that they are
          // attached as attachments with a flavor of preview/<something>.
          String time = timepoint.toString();
          for (Attachment slide : mediaPackage.getAttachments(MediaPackageElements.PRESENTATION_SEGMENT_PREVIEW)) {
            MediaPackageReference ref = slide.getReference();
            if (ref != null && time.equals(ref.getProperty("time"))) {
              hintField.append("preview");
              hintField.append(".");
              hintField.append(ref.getIdentifier());
              hintField.append("=");
              hintField.append(slide.getURI().toString());
              hintField.append("\n");
            }
          }

          logger.trace("Adding segment: " + timepoint.toString());
          Schema.setSegmentHint(doc, new DField<String>(hintField.toString(), Integer.toString(segmentCount)));

          // increase segment counter
          segmentCount++;
        }
      }
    }

    // Put the most important keywords into a special solr field
    if (sortedAnnotations != null) {
      Schema.setOcKeywords(doc, importantKeywordsString(sortedAnnotations).toString());
    }
  }

  /**
   * Generates a string with the most important kewords from the text annotation.
   *
   * @param sortedAnnotations
   * @return The keyword string.
   */
  static StringBuffer importantKeywordsString(SortedSet<TextAnnotation> sortedAnnotations) {

    // important keyword:
    // - high relevance
    // - high confidence
    // - occur often
    // - more than MAX_CHAR chars

    // calculate keyword occurences (histogram) and importance
    ArrayList<String> list = new ArrayList<String>();
    Iterator<TextAnnotation> textAnnotations = sortedAnnotations.iterator();
    TextAnnotation textAnnotation = null;
    String keyword = null;

    HashMap<String, Integer> histogram = new HashMap<String, Integer>();
    HashMap<String, Double> importance = new HashMap<String, Double>();
    int occ = 0;
    double imp;
    while (textAnnotations.hasNext()) {
      textAnnotation = textAnnotations.next();
      Iterator<KeywordAnnotation> keywordAnnotations = textAnnotation.keywordAnnotations();
      while (keywordAnnotations.hasNext()) {
        KeywordAnnotation annotation = keywordAnnotations.next();
        keyword = annotation.getKeyword().toLowerCase();
        if (keyword.length() > MAX_CHAR) {
          occ = 0;
          if (histogram.keySet().contains(keyword)) {
            occ = histogram.get(keyword);
          }
          histogram.put(keyword, occ + 1);

          // here the importance value is calculated
          // from relevance, confidence and frequency of occurence.
          imp = (RELEVANCE_BOOST * getMaxRelevance(keyword, sortedAnnotations) + getMaxConfidence(keyword,
                  sortedAnnotations)) * (occ + 1);
          importance.put(keyword, imp);
        }
      }
    }

    // get the MAX_IMPORTANT_COUNT most important keywords
    StringBuffer buf = new StringBuffer();

    while (list.size() < MAX_IMPORTANT_COUNT && importance.size() > 0) {
      double max = 0.0;
      String maxKeyword = null;

      // get maximum from importance list
      for (Entry<String, Double> entry : importance.entrySet()) {
        keyword = entry.getKey();
        if (max < entry.getValue()) {
          max = entry.getValue();
          maxKeyword = keyword;
        }
      }

      // pop maximum
      importance.remove(maxKeyword);

      // append keyword to string
      if (buf.length() > 0)
        buf.append(" ");
      buf.append(maxKeyword);
    }

    return buf;
  }

  /**
   * Gets the maximum confidence for a given keyword in the text annotation.
   *
   * @param keyword
   * @param sortedAnnotations
   * @return The maximum confidence value.
   */
  static double getMaxConfidence(String keyword, SortedSet<TextAnnotation> sortedAnnotations) {
    double max = 0.0;
    String needle = null;
    TextAnnotation textAnnotation = null;
    Iterator<TextAnnotation> textAnnotations = sortedAnnotations.iterator();
    while (textAnnotations.hasNext()) {
      textAnnotation = textAnnotations.next();
      Iterator<KeywordAnnotation> keywordAnnotations = textAnnotation.keywordAnnotations();
      while (keywordAnnotations.hasNext()) {
        KeywordAnnotation ann = keywordAnnotations.next();
        needle = ann.getKeyword().toLowerCase();
        if (keyword.equals(needle)) {
          if (max < textAnnotation.getConfidence()) {
            max = textAnnotation.getConfidence();
          }
        }
      }
    }
    return max;
  }

  /**
   * Gets the maximum relevance for a given keyword in the text annotation.
   *
   * @param keyword
   * @param sortedAnnotations
   * @return The maximum relevance value.
   */
  static double getMaxRelevance(String keyword, SortedSet<TextAnnotation> sortedAnnotations) {
    double max = 0.0;
    String needle = null;
    TextAnnotation textAnnotation = null;
    Iterator<TextAnnotation> textAnnotations = sortedAnnotations.iterator();
    while (textAnnotations.hasNext()) {
      textAnnotation = textAnnotations.next();
      Iterator<KeywordAnnotation> keywordAnnotations = textAnnotation.keywordAnnotations();
      while (keywordAnnotations.hasNext()) {
        KeywordAnnotation ann = keywordAnnotations.next();
        needle = ann.getKeyword().toLowerCase();
        if (keyword.equals(needle)) {
          if (max < textAnnotation.getRelevance()) {
            max = textAnnotation.getRelevance();
          }
        }
      }
    }
    return max;
  }

  /**
   * Get metadata from all registered metadata services.
   */
  static List<StaticMetadata> getMetadata(final List<StaticMetadataService> mdServices, final MediaPackage mp) {
    return flatMap(mdServices, new ArrayList<StaticMetadata>(),
            new Function<StaticMetadataService, Collection<StaticMetadata>>() {
              @Override
              public Collection<StaticMetadata> apply(StaticMetadataService s) {
                StaticMetadata md = s.getMetadata(mp);
                return md != null ? Arrays.asList(md) : Collections.<StaticMetadata> emptyList();
              }
            });
  }

  /**
   * Return all media package tags as a space separated string.
   */
  static String tags(MediaPackage mp) {
    StringBuilder sb = new StringBuilder();
    for (MediaPackageElement element : mp.getElements()) {
      for (String tag : element.getTags()) {
        sb.append(tag);
        sb.append(" ");
      }
    }
    return sb.toString();
  }

  /**
   * Return all media package flavors as a space separated string.
   */
  static String flavors(MediaPackage mp) {
    StringBuilder sb = new StringBuilder();
    for (MediaPackageElement element : mp.getElements()) {
      if (element.getFlavor() != null) {
        sb.append(element.getFlavor().toString());
        sb.append(" ");
      }
    }
    return sb.toString();
  }

  /**
   * Returns number of entries in search index, across all organizations.
   *
   * @return number of entries in search index
   * @throws SearchServiceDatabaseException
   *           if count cannot be retrieved
   */
  public long count() throws SearchServiceDatabaseException {
    try {
      QueryResponse response = solrServer.query(new SolrQuery("*:*"));
      return response.getResults().getNumFound();
    } catch (SolrServerException e) {
      throw new SearchServiceDatabaseException(e);
    }
  }
}
