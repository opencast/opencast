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
package org.opencastproject.episode.impl.solr;

import static org.opencastproject.util.data.Collections.flatMap;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.episode.api.SearchResultItem.SearchResultItemType;
import org.opencastproject.episode.api.Version;
import org.opencastproject.episode.impl.persistence.EpisodeServiceDatabaseException;
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
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Cell;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

/** Utility class used to manage the search index. */
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
  private Cell<List<StaticMetadataService>> metadataSvcs;

  private SeriesService seriesSvc;

  private Mpeg7CatalogService mpeg7CatalogSvc;

  private Workspace workspace;

  private SecurityService securitySvc;

  /**
   * Creates a new management instance for the search index.
   * 
   * @param solrServer
   *          connection to the database
   */
  public SolrIndexManager(SolrServer solrServer, Workspace workspace, Cell<List<StaticMetadataService>> metadataSvcs,
          SeriesService seriesSvc, Mpeg7CatalogService mpeg7CatalogSvc, SecurityService securitySvc) {
    this.solrServer = solrServer;
    this.workspace = workspace;
    this.seriesSvc = seriesSvc;
    this.mpeg7CatalogSvc = mpeg7CatalogSvc;
    this.securitySvc = securitySvc;
    this.metadataSvcs = metadataSvcs.lift(new Function<List<StaticMetadataService>, List<StaticMetadataService>>() {
      @Override
      public List<StaticMetadataService> apply(List<StaticMetadataService> metadataSvcs) {
        return mlist(metadataSvcs).sort(priorityComparator).value();
      }
    });
  }

  public static final Comparator<StaticMetadataService> priorityComparator = new Comparator<StaticMetadataService>() {
    @Override
    public int compare(StaticMetadataService a, StaticMetadataService b) {
      return b.getPriority() - a.getPriority();
    }
  };

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
   * Returns number of episodes in search index, across all organizations.
   * 
   * @return number of episodes in search index
   * @throws EpisodeServiceDatabaseException
   *           if count cannot be retrieved
   */
  public long count() throws EpisodeServiceDatabaseException {
    try {
      QueryResponse response = solrServer.query(new SolrQuery("*:*"));
      return response.getResults().getNumFound();
    } catch (SolrServerException e) {
      throw new EpisodeServiceDatabaseException(e);
    }
  }

  /**
   * Set the deleted flag of all versions of the media package with the given id.
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
        SolrQuery query = new SolrQuery(Schema.DC_ID + ":" + ClientUtils.escapeQueryChars(id) + " AND -"
                + Schema.OC_DELETED + ":[* TO *]");
        solrResponse = solrServer.query(query);
      } catch (Exception e1) {
        throw new SolrServerException(e1);
      }

      // Did we find the episode?
      if (solrResponse.getResults().size() == 0) {
        return false;
      }

      for (SolrDocument doc : solrResponse.getResults()) {
        // Use all existing fields
        SolrInputDocument inputDocument = new SolrInputDocument();
        for (String field : doc.getFieldNames()) {
          inputDocument.setField(field, doc.get(field));
        }

        // Set the oc_deleted field to the current date, then update
        Schema.setOcDeleted(inputDocument, deletionDate);
        solrServer.add(inputDocument);
        solrServer.commit();
      }
      return true;
    } catch (IOException e) {
      throw new SolrServerException(e);
    }
  }

  /** Set the "locked" flag of an index entry. */
  public boolean setLocked(String id, boolean locked) throws SolrServerException {
    try {
      // Load the existing episode
      QueryResponse solrResponse = null;
      try {
        SolrQuery query = new SolrQuery(Schema.DC_ID + ":" + ClientUtils.escapeQueryChars(id));
        // + " AND -" + Schema.OC_DELETED + ":[* TO *]"
        // + " AND " + Schema.OC_LOCKED + ":" + (!locked));
        solrResponse = solrServer.query(query);
      } catch (Exception e) {
        throw new SolrServerException(e);
      }

      // Did we find the episode?
      if (solrResponse.getResults().size() == 0) {
        return false;
      }

      // Use all existing fields
      for (SolrDocument doc : solrResponse.getResults()) {
        SolrInputDocument inputDocument = new SolrInputDocument();
        for (String field : doc.getFieldNames()) {
          inputDocument.setField(field, doc.get(field));
        }

        solrServer.add(inputDocument);
        solrServer.commit();
      }
      return true;
    } catch (IOException e) {
      throw new SolrServerException(e);
    }
  }

  /** Set the "latestVersion" flag of an index entry. */
  private void resetFormerLatestVersion(MediaPackage sourceMediaPackage, Version version) throws SolrServerException,
          IOException {
    final SolrQuery query = new SolrQuery(Schema.ID + ":"
            + ClientUtils.escapeQueryChars(sourceMediaPackage.getIdentifier() + version.toString()));
    QueryResponse response = solrServer.query(query);

    // Did we find the episode?
    if (response.getResults().size() == 0)
      return;

    if (response.getResults().size() > 1)
      throw new SolrServerException("Multiple values with the same unique identifier found!");

    SolrDocument doc = response.getResults().get(0);
    SolrInputDocument inputDoc = new SolrInputDocument();
    for (String field : doc.getFieldNames()) {
      inputDoc.setField(field, doc.get(field));
    }
    Schema.setOcLatestVersion(inputDoc, false);
    solrServer.add(inputDoc);
  }

  /**
   * Posts the media package to solr. Depending on what is referenced in the media package, the method might create one
   * or two entries: one for the episode and one for the series that the episode belongs to.
   * 
   * Note: Media package element URIs need to be URLs pointing to existing locations.
   * 
   * @param sourceMediaPackage
   *          the media package to post
   * @param acl
   *          the access control list for this mediapackage
   * @param now
   *          current date
   * @param version
   *          the archive version
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public void add(MediaPackage sourceMediaPackage, AccessControlList acl, Date now, Version version)
          throws SolrServerException {
    try {
      final SolrInputDocument episodeDocument = createEpisodeInputDocument(sourceMediaPackage, acl, version);
      Schema.setOcTimestamp(episodeDocument, now);
      Schema.setOcLatestVersion(episodeDocument, true);
      resetFormerLatestVersion(sourceMediaPackage, new Version(version.value() - 1L));
      // Post everything to the search index
      solrServer.add(episodeDocument);
      solrServer.commit();
    } catch (Exception e) {
      try {
        solrServer.rollback();
      } catch (IOException e1) {
        throw new SolrServerException(e1);
      }
      throw new SolrServerException(e);
    }
  }

  /**
   * Posts the media package to solr. Depending on what is referenced in the media package, the method might create one
   * or two entries: one for the episode and one for the series that the episode belongs to.
   * <p/>
   * Note: Media package element URIs need to be URLs pointing to existing locations.
   * 
   * 
   * @param sourceMediaPackage
   *          the media package to post
   * @param acl
   *          the access control list for this mediapackage
   * @param version
   *          the archive version
   * @param modificationDate
   *          the modification date
   * @param deletionDate
   *          the deletion date
   * @param isLatestVersion
   *          the latest version flag
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public void add(MediaPackage sourceMediaPackage, AccessControlList acl, Version version, Option<Date> deletionDate,
          Date modificationDate, boolean isLatestVersion) throws SolrServerException {
    try {
      final SolrInputDocument episodeDocument = createEpisodeInputDocument(sourceMediaPackage, acl, version);
      Schema.setOcTimestamp(episodeDocument, modificationDate);
      Schema.setOcLatestVersion(episodeDocument, isLatestVersion);
      for (Date a : deletionDate) {
        Schema.setOcDeleted(episodeDocument, a);
      }
      solrServer.add(episodeDocument);
      solrServer.commit();
    } catch (Exception e) {
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
   * @param version
   *          the archive version
   * @return an input document ready to be posted to solr
   * @throws MediaPackageException
   *           if serialization of the media package fails
   */
  private SolrInputDocument createEpisodeInputDocument(final MediaPackage mediaPackage, AccessControlList acl,
          final Version version) throws MediaPackageException, IOException {
    final SolrInputDocument doc = new SolrInputDocument();
    final String mediaPackageId = mediaPackage.getIdentifier().toString();
    // todo fix id generation ambiguity. currently tests are broken
    // if (mediaPackageId.contains("#"))
    // throw new Error("Media package id must not contain '#' characters: " + mediaPackageId);
    // Schema.setId(doc, mediaPackageId + "#" + version);
    Schema.setId(doc, mediaPackageId + version);
    Schema.setDcId(doc, mediaPackageId);
    // /
    // OC specific fields
    Schema.setOcMediatype(doc, SearchResultItemType.AudioVisual.toString());
    Schema.setOrganization(doc, securitySvc.getOrganization().getId());
    Schema.setOcMediapackage(doc, MediaPackageParser.getAsXml(mediaPackage));
    Schema.setOcAcl(doc, AccessControlParser.toXml(acl));
    Schema.setOcElementtags(doc, tags(mediaPackage));
    Schema.setOcElementflavors(doc, flavors(mediaPackage));
    Schema.setOcVersion(doc, version);
    // Add cover
    Attachment[] cover = mediaPackage.getAttachments(MediaPackageElements.MEDIAPACKAGE_COVER_FLAVOR);
    if (cover != null && cover.length > 0) {
      Schema.setOcCover(doc, cover[0].getURI().toString());
    }

    // episode fields
    for (StaticMetadata md : getMetadata(metadataSvcs.get(), mediaPackage)) {
      addEpisodeMetadata(doc, md);
    }
    // series fields
    for (DublinCoreCatalog dc : getSeriesDc(mediaPackage)) {
      for (DField<String> a : fromDCValue(dc.get(DublinCore.PROPERTY_TITLE))) {
        Schema.setSeriesDcTitle(doc, a);
      }
    }
    // mpeg7 fields
    logger.debug("Looking for mpeg-7 catalogs containing segment texts");
    // TODO: merge the segments from each mpeg7 if there is more than one mpeg7 catalog
    mlist(mediaPackage.getCatalogs(MediaPackageElements.TEXTS)).headOpt().orElse(new Function0<Option<Catalog>>() {
      @Override
      public Option<Catalog> apply() {
        logger.debug("No text catalogs found, trying segments only");
        return mlist(mediaPackage.getCatalogs(MediaPackageElements.SEGMENTS)).headOpt();
      }
    }).fold(new Option.EMatch<Catalog>() {
      @Override
      public void esome(final Catalog mpeg7) {
        // load catalog and add it to the solr input document
        addMpeg7Metadata(doc, mediaPackage, loadMpeg7Catalog(mpeg7));
      }

      @Override
      public void enone() {
        logger.debug("No segmentation catalog found");
      }
    });

    return doc;
  }

  private Option<DublinCoreCatalog> getSeriesDc(MediaPackage mp) {
    for (String id : option(mp.getSeries())) {
      try {
        return some(seriesSvc.getSeries(id));
      } catch (Exception e) {
        logger.debug("No series dublincore found for series id " + id, e);
      }
    }
    return none();
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
      public Option<String> getDcId() {
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
      public List<DField<String>> getSeriesDcTitle() {
        return nil(); // set elsewhere
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
      public Option<String> getOcAcl() {
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
      public List<DField<String>> getSegmentText() {
        return nil(); // set elsewhere
      }

      @Override
      public List<DField<String>> getSegmentHint() {
        return nil(); // set elsewhere
      }

      @Override
      public Option<Version> getOcVersion() {
        return Option.none(); // set elsewhere
      }

      @Override
      public Option<Boolean> getOcLatestVersion() {
        return Option.none(); // set elsewhere
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

  static String mkString(Collection<?> as, String sep) {
    StringBuffer b = new StringBuffer();
    for (Object a : as) {
      b.append(a).append(sep);
    }
    return b.substring(0, b.length() - sep.length());
  }

  private Mpeg7Catalog loadMpeg7Catalog(Catalog cat) {
    InputStream in = null;
    try {
      File f = workspace.get(cat.getURI());
      in = new FileInputStream(f);
      return mpeg7CatalogSvc.load(in);
    } catch (NotFoundException e) {
      return chuck(new IOException("Unable to load metadata from mpeg7 catalog " + cat));
    } catch (IOException e) {
      return chuck(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
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

  /** Get metadata from all registered metadata services. */
  static List<StaticMetadata> getMetadata(final List<StaticMetadataService> mdServices, final MediaPackage mp) {
    return flatMap(mdServices, new ArrayList<StaticMetadata>(),
            new Function<StaticMetadataService, Collection<StaticMetadata>>() {
              @Override
              public Collection<StaticMetadata> apply(StaticMetadataService s) {
                StaticMetadata md = s.getMetadata(mp);
                return md != null ? list(md) : Collections.EMPTY_LIST;
              }
            });
  }

  /** Return all media package tags as a space separated string. */
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

  /** Return all media package flavors as a space separated string. */
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
}
