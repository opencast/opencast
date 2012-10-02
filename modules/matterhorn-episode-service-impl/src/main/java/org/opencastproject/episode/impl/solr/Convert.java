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

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.opencastproject.episode.api.JaxbMediaSegment;
import org.opencastproject.episode.api.JaxbSearchResult;
import org.opencastproject.episode.api.JaxbSearchResultItem;
import org.opencastproject.episode.api.MediaSegment;
import org.opencastproject.episode.api.SearchResult;
import org.opencastproject.episode.api.SearchResultItem;
import org.opencastproject.episode.api.Version;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Predicate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.opencastproject.util.data.Collections.filter;
import static org.opencastproject.util.data.Collections.head;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Misc.chuck;

/** Functions to convert between data types. */
public final class Convert {
  private Convert() {
  }

  public static JaxbSearchResultItem convertToJaxbSearchResultItem(Function<JaxbSearchResultItem, SearchResultItem> f) {
    final JaxbSearchResultItem conv = new JaxbSearchResultItem();
    // copy to conv
    callAllMethods(SearchResultItem.class, f.apply(conv));
    return conv;
  }

  /** Convert a {@link SearchResultItem} into its JAXB representation. */
  public static JaxbSearchResultItem convert(final SearchResultItem item) {
    return convertToJaxbSearchResultItem(new Function<JaxbSearchResultItem, SearchResultItem>() {
      @Override public SearchResultItem apply(final JaxbSearchResultItem conv) {
        return new SearchResultItem() {
          @Override public String getId() {
            conv.setId(item.getId());
            return null;
          }

          @Override public MediaPackage getMediaPackage() {
            conv.setMediaPackage(item.getMediaPackage());
            return null;
          }

          @Override public String getOrganization() {
            conv.setOrganization(item.getOrganization());
            return null;
          }

          @Override public long getDcExtent() {
            conv.setDcExtent(item.getDcExtent());
            return 0;
          }

          @Override public String getDcTitle() {
            conv.setDcTitle(item.getDcTitle());
            return null;
          }

          @Override public String getDcSubject() {
            conv.setDcSubject(item.getDcSubject());
            return null;
          }

          @Override public String getDcDescription() {
            conv.setDcDescription(item.getDcDescription());
            return null;
          }

          @Override public String getDcCreator() {
            conv.setDcCreator(item.getDcCreator());
            return null;
          }

          @Override public String getDcPublisher() {
            conv.setDcPublisher(item.getDcPublisher());
            return null;
          }

          @Override public String getDcContributor() {
            conv.setDcContributor(item.getDcContributor());
            return null;
          }

          @Override public String getDcAbstract() {
            conv.setDcAbstract(item.getDcAbstract());
            return null;
          }

          @Override public Date getDcCreated() {
            conv.setDcCreated(item.getDcCreated());
            return null;
          }

          @Override public Date getDcAvailableFrom() {
            conv.setDcAvailableFrom(item.getDcAvailableFrom());
            return null;
          }

          @Override public Date getDcAvailableTo() {
            conv.setDcAvailableTo(item.getDcAvailableTo());
            return null;
          }

          @Override public String getDcLanguage() {
            conv.setDcLanguage(item.getDcLanguage());
            return null;
          }

          @Override public String getDcRightsHolder() {
            conv.setDcRightsHolder(item.getDcRightsHolder());
            return null;
          }

          @Override public String getDcSpatial() {
            conv.setDcSpatial(item.getDcSpatial());
            return null;
          }

          @Override public String getDcIsPartOf() {
            conv.setDcIsPartOf(item.getDcIsPartOf());
            return null;
          }

          @Override public String getDcReplaces() {
            conv.setDcReplaces(item.getDcReplaces());
            return null;
          }

          @Override public String getDcType() {
            conv.setDcType(item.getDcType());
            return null;
          }

          @Override public String getDcAccessRights() {
            conv.setDcAccessRights(item.getDcAccessRights());
            return null;
          }

          @Override public String getDcLicense() {
            conv.setDcLicense(item.getDcLicense());
            return null;
          }

          @Override public String getOcMediapackage() {
            conv.setOcMediapackage(item.getOcMediapackage());
            return null;
          }

          @Override public String getOcAcl() {
            conv.setOcAcl(item.getOcAcl());
            return null;
          }

          @Override public SearchResultItemType getType() {
            conv.setMediaType(item.getType());
            return null;
          }

          @Override public String[] getKeywords() {
            for (String a : item.getKeywords())
              conv.addKeyword(a);
            return null;
          }

          @Override public String getCover() {
            conv.setCover(item.getCover());
            return null;
          }

          @Override public Date getTimestamp() {
            conv.setTimestamp(item.getTimestamp());
            return null;
          }

          @Override public double getScore() {
            conv.setScore(item.getScore());
            return 0;
          }

          @Override public MediaSegment[] getSegments() {
            for (MediaSegment a : item.getSegments())
              conv.addSegment(a);
            return null;
          }

          @Override public Option<Date> getOcDeleted() {
            conv.setOcDeleted(item.getOcDeleted());
            return null;
          }

          @Override public Version getOcVersion() {
            conv.setOcVersion(item.getOcVersion());
            return null;
          }
        };
      }
    });
  }

  /** Convert a {@link SolrDocument} into a {@link org.opencastproject.episode.api.JaxbSearchResultItem}. */
  public static JaxbSearchResultItem convert(final SolrDocument doc, final SolrQuery query) {
    return convertToJaxbSearchResultItem(new Function<JaxbSearchResultItem, SearchResultItem>() {
      @Override public SearchResultItem apply(final JaxbSearchResultItem conv) {
        return new SearchResultItem() {
          private final String dfltString = null;

          @Override public String getId() {
            conv.setId(Schema.getDcId(doc));
            return null;
          }

          @Override public String getOrganization() {
            conv.setOrganization(Schema.getOrganization(doc));
            return null;
          }

          @Override public MediaPackage getMediaPackage() {
            try {
              conv.setMediaPackage(MediaPackageParser.getFromXml(Schema.getOcMediapackage(doc)));
            } catch (MediaPackageException e) {
              return chuck(e);
            }
            return null;
          }

          @Override public long getDcExtent() {
            final long extent = new Function0<Long>() {
              @Override public Long apply() {
                if (SearchResultItemType.AudioVisual.equals(getTypeInternal())) {
                  Long extent = Schema.getDcExtent(doc);
                  if (extent != null)
                    return extent;
                }
                return -1L;
              }
            } .apply();
            conv.setDcExtent(extent);
            return -1;
          }

          @Override public String getDcTitle() {
            final List<DField<String>> titles = Schema.getDcTitle(doc);
            // try to return the first title without any language information first...
            final String title = head(filter(titles, new Predicate<DField<String>>() {
              @Override public Boolean apply(DField<String> f) {
                return f.getSuffix().equals(Schema.LANGUAGE_UNDEFINED);
              }
            })).map(new Function<DField<String>, String>() {
              @Override public String apply(DField<String> f) {
                return f.getValue();
              }
            }).getOrElse(new Function0<String>() {
              @Override public String apply() {
                // ... since none is present return the first arbitrary title
                return Schema.getFirst(titles, dfltString);
              }
            });
            conv.setDcTitle(title);
            return null;
          }

          @Override public String getDcSubject() {
            conv.setDcSubject(Schema.getFirst(Schema.getDcSubject(doc), dfltString));
            return null;
          }

          @Override public String getDcDescription() {
            conv.setDcDescription(Schema.getFirst(Schema.getDcDescription(doc), dfltString));
            return null;
          }

          @Override public String getDcCreator() {
            conv.setDcCreator(Schema.getFirst(Schema.getDcCreator(doc), dfltString));
            return null;
          }

          @Override public String getDcPublisher() {
            conv.setDcPublisher(Schema.getFirst(Schema.getDcPublisher(doc), dfltString));
            return null;
          }

          @Override public String getDcContributor() {
            conv.setDcContributor(Schema.getFirst(Schema.getDcContributor(doc), dfltString));
            return null;
          }

          @Override public String getDcAbstract() {
            return null;
          }

          @Override public Date getDcCreated() {
            conv.setDcCreated(Schema.getDcCreated(doc));
            return null;
          }

          @Override public Date getDcAvailableFrom() {
            conv.setDcAvailableFrom(Schema.getDcAvailableFrom(doc));
            return null;
          }

          @Override public Date getDcAvailableTo() {
            conv.setDcAvailableTo(Schema.getDcAvailableTo(doc));
            return null;
          }

          @Override public String getDcLanguage() {
            conv.setDcLanguage(Schema.getDcLanguage(doc));
            return null;
          }

          @Override public String getDcRightsHolder() {
            conv.setDcRightsHolder(Schema.getFirst(Schema.getDcRightsHolder(doc), dfltString));
            return null;
          }

          @Override public String getDcSpatial() {
            conv.setDcSpatial(Schema.getFirst(Schema.getDcSpatial(doc), dfltString));
            return null;
          }

          @Override public String getDcIsPartOf() {
            conv.setDcIsPartOf(Schema.getDcIsPartOf(doc));
            return null;
          }

          @Override public String getDcReplaces() {
            conv.setDcReplaces(Schema.getDcReplaces(doc));
            return null;
          }

          @Override public String getDcType() {
            conv.setDcType(Schema.getDcType(doc));
            return null;
          }

          @Override public String getDcAccessRights() {
            conv.setDcAccessRights(Schema.getFirst(Schema.getDcAccessRights(doc), dfltString));
            return null;
          }

          @Override public String getDcLicense() {
            conv.setDcLicense(Schema.getFirst(Schema.getDcLicense(doc), dfltString));
            return null;
          }

          @Override public String getOcMediapackage() {
            conv.setOcMediapackage(Schema.getOcMediapackage(doc));
            return null;
          }

          @Override public String getOcAcl() {
            conv.setOcAcl(Schema.getOcAcl(doc));
            return null;
          }

          @Override public SearchResultItemType getType() {
            conv.setMediaType(getTypeInternal());
            return null;
          }

          private SearchResultItemType getTypeInternal() {
            String t = Schema.getOcMediatype(doc);
            return t != null ? SearchResultItemType.valueOf(t) : null;
          }

          @Override public String[] getKeywords() {
            final String[] keywords = new Function0<String[]>() {
              @Override public String[] apply() {
                if (SearchResultItemType.AudioVisual.equals(getTypeInternal())) {
                  String k = Schema.getOcKeywords(doc);
                  return k != null ? k.split(" ") : new String[0];
                } else
                  return new String[0];
              }
            } .apply();
            for (String a : keywords)
              conv.addKeyword(a);
            return null;
          }

          @Override public String getCover() {
            conv.setCover(Schema.getOcCover(doc));
            return null;
          }

          @Override public Date getTimestamp() {
            conv.setTimestamp(Schema.getOcTimestamp(doc));
            return null;
          }

          @Override public double getScore() {
            conv.setScore(Schema.getScore(doc));
            return -1;
          }

          @Override public MediaSegment[] getSegments() {
            final MediaSegment[] segments = new Function0<MediaSegment[]>() {
              @Override public MediaSegment[] apply() {
                if (SearchResultItemType.AudioVisual.equals(getType()))
                  return createSearchResultSegments(doc, query).toArray(new JaxbMediaSegment[0]);
                else
                  return new JaxbMediaSegment[0];
              }
            } .apply();
            for (MediaSegment a : segments)
              conv.addSegment(a);
            return null;
          }

          @Override public Option<Date> getOcDeleted() {
            conv.setOcDeleted(option(Schema.getOcDeleted(doc)));
            return null;
          }

          @Override public Version getOcVersion() {
            conv.setOcVersion(Schema.getOcVersion(doc));
            return null;
          }
        };
      }
    });
  }

  /** Convert a {@link SearchResult} into its JAXB representation. */
  public static JaxbSearchResult convert(SearchResult a) {
    final JaxbSearchResult r = new JaxbSearchResult();
    r.setLimit(a.getLimit());
    r.setOffset(a.getOffset());
    r.setItems(mlist(a.getItems()).map(new Function<SearchResultItem, JaxbSearchResultItem>() {
      @Override public JaxbSearchResultItem apply(SearchResultItem item) {
        return convert(item);
      }
    }).value());
    r.setSearchTime(a.getSearchTime());
    r.setTotalSize(a.getTotalSize());
    return r;
  }

  //

  /**
   * Creates a list of <code>MediaSegment</code>s from the given result document.
   *
   * @param doc
   *         the result document
   * @param query
   *         the original query
   */
  private static List<JaxbMediaSegment> createSearchResultSegments(SolrDocument doc, SolrQuery query) {
    List<JaxbMediaSegment> segments = new ArrayList<JaxbMediaSegment>();

    // The maximum number of hits in a segment
    int maxHits = 0;

    // Loop over every segment
    for (String fieldName : doc.getFieldNames()) {
      if (!fieldName.startsWith(Schema.SEGMENT_TEXT_PREFIX))
        continue;

      // Ceate a new segment
      int segmentId = Integer.parseInt(fieldName.substring(Schema.SEGMENT_TEXT_PREFIX.length()));
      JaxbMediaSegment segment = new JaxbMediaSegment(segmentId);
      segment.setText(mkString(doc.getFieldValue(fieldName)));

      // Read the hints for this segment
      Properties segmentHints = new Properties();
      try {
        String hintFieldName = Schema.SEGMENT_HINT_PREFIX + segment.getIndex();
        Object hintFieldValue = doc.getFieldValue(hintFieldName);
        segmentHints.load(new ByteArrayInputStream(hintFieldValue.toString().getBytes()));
      } catch (IOException e) {
      }

      // get segment time
      String segmentTime = segmentHints.getProperty("time");
      if (segmentTime == null)
        throw new IllegalStateException("Found segment without time hint");
      segment.setTime(Long.parseLong(segmentTime));

      // get segment duration
      String segmentDuration = segmentHints.getProperty("duration");
      if (segmentDuration == null)
        throw new IllegalStateException("Found segment without duration hint");
      segment.setDuration(Long.parseLong(segmentDuration));

      // get preview urls
      for (Map.Entry<Object, Object> entry : segmentHints.entrySet()) {
        if (entry.getKey().toString().startsWith("preview.")) {
          String[] parts = entry.getKey().toString().split("\\.");
          segment.addPreview(entry.getValue().toString(), parts[1]);
        }
      }

      // calculate the segment's relevance with respect to the query
      String queryText = query.getQuery();
      String segmentText = segment.getText();
      if (!StringUtils.isBlank(queryText) && !StringUtils.isBlank(segmentText)) {
        segmentText = segmentText.toLowerCase();
        Pattern p = Pattern.compile(".*fulltext:\\(([^)]*)\\).*");
        Matcher m = p.matcher(queryText);
        if (m.matches()) {
          String[] queryTerms = StringUtils.split(m.group(1).toLowerCase());
          int segmentHits = 0;
          int textLength = segmentText.length();
          for (String t : queryTerms) {
            int startIndex = 0;
            while (startIndex < textLength - 1) {
              int foundAt = segmentText.indexOf(t, startIndex);
              if (foundAt < 0)
                break;
              segmentHits++;
              startIndex = foundAt + t.length();
            }
          }

          // for now, just store the number of hits, but keep track of the maximum hit count
          segment.setRelevance(segmentHits);
          if (segmentHits > maxHits)
            maxHits = segmentHits;
        }
      }

      segments.add(segment);
    }

    for (JaxbMediaSegment segment : segments) {
      int hitsInSegment = segment.getRelevance();
      if (hitsInSegment > 0)
        segment.setRelevance((int) ((100 * hitsInSegment) / maxHits));
    }

    return segments;
  }

  /**
   * Simple helper method to avoid null strings.
   *
   * @param f
   *         object which implements <code>toString()</code> method.
   * @return The input object or empty string.
   */
  private static String mkString(Object f) {
    if (f != null)
      return f.toString();
    else
      return "";
  }

  /** Call all methods of <code>c</code> on object <code>o</code>. */
  private static void callAllMethods(Class<?> c, Object o) {
    try {
      for (Method m : c.getDeclaredMethods()) {
        m.invoke(o);
      }
    } catch (Exception e) {
      chuck(e);
    }
  }
}
