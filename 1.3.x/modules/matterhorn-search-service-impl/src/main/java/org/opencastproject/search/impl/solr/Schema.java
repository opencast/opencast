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

package org.opencastproject.search.impl.solr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.opencastproject.util.data.CollectionUtil;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

/**
 * This class reflects the solr schema.xml. Note that all getters returning simple values may always return null. Please
 * access the index _only_ by means of this class.
 */
public final class Schema {

  private Schema() {
  }

  public static final String LANGUAGE_UNDEFINED = "__";

  public static final String ID = "id";

  // /
  // Dublin core fields

  // Localization independent fields
  public static final String DC_EXTENT = "dc_extent";
  public static final String DC_TYPE = "dc_type";
  public static final String DC_CREATED = "dc_created";
  public static final String DC_LANGUAGE = "dc_language";
  public static final String DC_TEMPORAL = "dc_temporal";
  public static final String DC_IS_PART_OF = "dc_is_part_of";
  public static final String DC_REPLACES = "dc_replaces";
  // Expand with #SUFFIX_FROM and #SUFFIX_TO
  public static final String DC_AVAILABLE_PREFIX = "dc_available_";

  // Localized fields
  public static final String DC_TITLE_PREFIX = "dc_title_";
  public static final String DC_SUBJECT_PREFIX = "dc_subject_";
  public static final String DC_CREATOR_PREFIX = "dc_creator_";
  public static final String DC_PUBLISHER_PREFIX = "dc_publisher_";
  public static final String DC_CONTRIBUTOR_PREFIX = "dc_contributor_";
  public static final String DC_ABSTRACT_PREFIX = "dc_abstract_";
  public static final String DC_DESCRIPTION_PREFIX = "dc_description_";
  public static final String DC_RIGHTS_HOLDER_PREFIX = "dc_rights_holder_";
  public static final String DC_SPATIAL_PREFIX = "dc_spatial_";
  public static final String DC_ACCESS_RIGHTS_PREFIX = "dc_access_rights_";
  public static final String DC_LICENSE_PREFIX = "dc_license_";

  // Filters for audio and video files
  public static final String HAS_AUDIO = "has_audio_file";
  public static final String HAS_VIDEO = "has_video_file";

  // Suffixes
  public static final String SUFFIX_FROM = "from";
  public static final String SUFFIX_TO = "to";

  // Additional fields
  public static final String OC_ORGANIZATION = "oc_organization";
  public static final String OC_MEDIAPACKAGE = "oc_mediapackage";
  public static final String OC_KEYWORDS = "oc_keywords";
  public static final String OC_COVER = "oc_cover";
  public static final String OC_MODIFIED = "oc_modified";
  public static final String OC_DELETED = "oc_deleted";
  public static final String OC_MEDIATYPE = "oc_mediatype";
  public static final String OC_ELEMENTTAGS = "oc_elementtags";
  public static final String OC_ELEMENTFLAVORS = "oc_elementflavors";
  public static final String OC_TEXT_PREFIX = "oc_text_";
  public static final String OC_HINT_PREFIX = "oc_hint_";
  public static final String OC_ACL_PREFIX = "oc_acl_";

  /**
   * Solr ranking score
   */
  public static final String SCORE = "score";

  /**
   * Accumulative fulltext field
   */
  public static final String FULLTEXT = "fulltext";

  /**
   * Just a constant to set the solr dynamic field name for segment text.
   */
  public static final String SEGMENT_TEXT_PREFIX = "oc_text_";

  /**
   * Just a constant to set the solr dynamic field name for segment hints.
   */
  public static final String SEGMENT_HINT_PREFIX = "oc_hint_";

  /**
   * The solr highlighting tag to use.
   */
  public static final String HIGHLIGHT_MATCH = "b";

  /**
   * Boost values for ranking
   */
  // TODO: move this to configuration file
  public static final float DC_TITLE_BOOST = 6.0f;
  public static final float DC_ABSTRACT_BOOST = 4.0f;
  public static final float DC_DESCRIPTION_BOOST = 4.0f;
  public static final float DC_CONTRIBUTOR_BOOST = 2.0f;
  public static final float DC_PUBLISHER_BOOST = 2.0f;
  public static final float DC_CREATOR_BOOST = 4.0f;
  public static final float DC_SUBJECT_BOOST = 4.0f;

  /**
   * Implement this interface to ensure that you don't miss any fields.
   * <p/>
   * Return some(..) to set a field, return none to skip setting it.
   */
  interface FieldCollector {
    Option<String> getId();

    Option<String> getOrganization();

    Option<Date> getDcCreated();

    Option<Long> getDcExtent();

    Option<String> getDcLanguage();

    Option<String> getDcIsPartOf();

    Option<String> getDcReplaces();

    Option<String> getDcType();

    Option<Date> getDcAvailableFrom();

    Option<Date> getDcAvailableTo();

    List<DField<String>> getDcTitle();

    List<DField<String>> getDcSubject();

    List<DField<String>> getDcCreator();

    List<DField<String>> getDcPublisher();

    List<DField<String>> getDcContributor();

    List<DField<String>> getDcDescription();

    List<DField<String>> getDcRightsHolder();

    List<DField<String>> getDcSpatial();

    List<DField<String>> getDcAccessRights();

    List<DField<String>> getDcLicense();

    Option<String> getOcMediatype();

    Option<String> getOcMediapackage();

    Option<String> getOcKeywords();

    Option<String> getOcCover();

    Option<Date> getOcModified();

    Option<Date> getOcDeleted();

    Option<String> getOcElementtags();

    Option<String> getOcElementflavors();

    List<DField<String>> getOcAcl();

    List<DField<String>> getSegmentText();

    List<DField<String>> getSegmentHint();
  }

  /**
   * Fill a solr document. Return a "some" if you want a field to get copied to the solr document.
   */
  public static void fill(SolrInputDocument doc, FieldCollector fields) {
    if (fields.getId().isSome())
      setId(doc, fields.getId().get());
    if (fields.getOrganization().isSome())
      setOrganization(doc, fields.getOrganization().get());
    if (fields.getDcCreated().isSome())
      setDcCreated(doc, fields.getDcCreated().get());
    if (fields.getDcExtent().isSome())
      setDcExtent(doc, fields.getDcExtent().get());
    if (fields.getDcLanguage().isSome())
      setDcLanguage(doc, fields.getDcLanguage().get());
    if (fields.getDcIsPartOf().isSome())
      setDcIsPartOf(doc, fields.getDcIsPartOf().get());
    if (fields.getDcReplaces().isSome())
      setDcReplaces(doc, fields.getDcReplaces().get());
    if (fields.getDcType().isSome())
      setDcType(doc, fields.getDcType().get());
    if (fields.getDcAvailableFrom().isSome())
      setDcAvailableFrom(doc, fields.getDcAvailableFrom().get());
    if (fields.getDcAvailableTo().isSome())
      setDcAvailableTo(doc, fields.getDcAvailableTo().get());
    for (DField<String> v : fields.getDcTitle())
      setDcTitle(doc, v);
    for (DField<String> v : fields.getDcSubject())
      setDcSubject(doc, v);
    for (DField<String> v : fields.getDcCreator())
      setDcCreator(doc, v);
    for (DField<String> v : fields.getDcPublisher())
      setDcPublisher(doc, v);
    for (DField<String> v : fields.getDcContributor())
      setDcContributor(doc, v);
    for (DField<String> v : fields.getDcDescription())
      setDcDescription(doc, v);
    for (DField<String> v : fields.getDcRightsHolder())
      setDcRightsHolder(doc, v);
    for (DField<String> v : fields.getDcSpatial())
      setDcSpatial(doc, v);
    for (DField<String> v : fields.getDcAccessRights())
      setDcAccessRights(doc, v);
    for (DField<String> v : fields.getDcLicense())
      setDcLicense(doc, v);
    if (fields.getOcMediatype().isSome())
      setOcMediatype(doc, fields.getOcMediatype().get());
    if (fields.getOcMediapackage().isSome())
      setOcMediapackage(doc, fields.getOcMediapackage().get());
    if (fields.getOcKeywords().isSome())
      setOcKeywords(doc, fields.getOcKeywords().get());
    if (fields.getOcCover().isSome())
      setOcCover(doc, fields.getOcCover().get());
    if (fields.getOcModified().isSome())
      setOcModified(doc, fields.getOcModified().get());
    if (fields.getOcDeleted().isSome())
      setOcDeleted(doc, fields.getOcDeleted().get());
    if (fields.getOcElementtags().isSome())
      setOcElementtags(doc, fields.getOcElementtags().get());
    if (fields.getOcElementflavors().isSome())
      setOcElementflavors(doc, fields.getOcElementflavors().get());
    for (DField<String> v : fields.getOcAcl())
      setOcAcl(doc, v);
    for (DField<String> v : fields.getSegmentText())
      setSegmentText(doc, v);
    for (DField<String> v : fields.getSegmentHint())
      setSegmentHint(doc, v);
  }

  /**
   * Adds one solr document's data as unstructured, full-text searchable data to another document.
   * 
   * @param docToEnrich
   *          the solr document to enrich with the other additional metadata
   * 
   * @param additionalMetadata
   *          the solr document containing the additional metadata
   * @throws IllegalArgumentException
   *           if either of the documents are null
   */
  public static void enrich(SolrInputDocument docToEnrich, SolrInputDocument additionalMetadata)
          throws IllegalArgumentException {
    if (docToEnrich == null || additionalMetadata == null) {
      throw new IllegalArgumentException("Documents must not be null");
    }
    for (String fieldName : additionalMetadata.getFieldNames()) {
      for (Object value : additionalMetadata.getFieldValues(fieldName)) {
        docToEnrich.addField(FULLTEXT, value);
      }
    }
  }

  public static String getId(SolrDocument doc) {
    return mkString(doc.get(ID));
  }

  public static String getOrganization(SolrDocument doc) {
    return mkString(doc.get(OC_ORGANIZATION));
  }

  public static String getId(SolrInputDocument doc) {
    SolrInputField f = doc.get(ID);
    return f != null ? mkString(f.getFirstValue()) : null;
  }

  public static void setId(SolrInputDocument doc, String id) {
    doc.setField(ID, id);
  }

  public static void setOrganization(SolrInputDocument doc, String organization) {
    doc.setField(OC_ORGANIZATION, organization);
  }

  public static Date getDcCreated(SolrDocument doc) {
    return (Date) doc.get(DC_CREATED);
  }

  public static void setDcCreated(SolrInputDocument doc, Date date) {
    doc.setField(DC_CREATED, date);
  }

  public static Long getDcExtent(SolrDocument doc) {
    Integer extent = (Integer) doc.get(DC_EXTENT);
    return extent != null ? extent.longValue() : null;
  }

  public static void setDcExtent(SolrInputDocument doc, Long extent) {
    doc.setField(DC_EXTENT, extent);
  }

  public static String getDcLanguage(SolrDocument doc) {
    return mkString(doc.get(DC_LANGUAGE));
  }

  public static void setDcLanguage(SolrInputDocument doc, String language) {
    doc.setField(DC_LANGUAGE, language);
  }

  public static String getDcIsPartOf(SolrDocument doc) {
    return mkString(doc.get(DC_IS_PART_OF));
  }

  public static void setDcIsPartOf(SolrInputDocument doc, String isPartOf) {
    doc.setField(DC_IS_PART_OF, isPartOf);
  }

  public static String getDcReplaces(SolrDocument doc) {
    return mkString(doc.get(DC_REPLACES));
  }

  public static void setDcReplaces(SolrInputDocument doc, String replaces) {
    doc.setField(DC_REPLACES, replaces);
  }

  public static String getDcType(SolrDocument doc) {
    return mkString(doc.get(DC_TYPE));
  }

  public static void setDcType(SolrInputDocument doc, String type) {
    doc.setField(DC_TYPE, type);
  }

  public static Date getDcAvailableFrom(SolrDocument doc) {
    return (Date) doc.get(DC_AVAILABLE_PREFIX + SUFFIX_FROM);
  }

  public static void setDcAvailableFrom(SolrInputDocument doc, Date from) {
    doc.setField(DC_AVAILABLE_PREFIX + SUFFIX_FROM, from);
  }

  public static Date getDcAvailableTo(SolrDocument doc) {
    return (Date) doc.get(DC_AVAILABLE_PREFIX + SUFFIX_TO);
  }

  public static void setDcAvailableTo(SolrInputDocument doc, Date to) {
    doc.setField(DC_AVAILABLE_PREFIX + SUFFIX_TO, to);
  }

  public static List<DField<String>> getDcTitle(SolrDocument doc) {
    return getDynamicStringValues(doc, DC_TITLE_PREFIX);
  }

  public static void setDcTitle(SolrInputDocument doc, DField<String> title) {
    doc.setField(DC_TITLE_PREFIX + title.getSuffix(), title.getValue(), DC_TITLE_BOOST);
  }

  public static List<DField<String>> getDcSubject(SolrDocument doc) {
    return getDynamicStringValues(doc, DC_SUBJECT_PREFIX);
  }

  public static void setDcSubject(SolrInputDocument doc, DField<String> subject) {
    doc.setField(DC_SUBJECT_PREFIX + subject.getSuffix(), subject.getValue(), DC_SUBJECT_BOOST);
  }

  public static List<DField<String>> getDcCreator(SolrDocument doc) {
    return getDynamicStringValues(doc, DC_CREATOR_PREFIX);
  }

  public static void setDcCreator(SolrInputDocument doc, DField<String> creator) {
    doc.setField(DC_CREATOR_PREFIX + creator.getSuffix(), creator.getValue(), DC_CREATOR_BOOST);
  }

  public static List<DField<String>> getDcPublisher(SolrDocument doc) {
    return getDynamicStringValues(doc, DC_PUBLISHER_PREFIX);
  }

  public static void setDcPublisher(SolrInputDocument doc, DField<String> publisher) {
    doc.setField(DC_PUBLISHER_PREFIX + publisher.getSuffix(), publisher.getValue(), DC_PUBLISHER_BOOST);
  }

  public static List<DField<String>> getDcContributor(SolrDocument doc) {
    return getDynamicStringValues(doc, DC_CONTRIBUTOR_PREFIX);
  }

  public static void setDcContributor(SolrInputDocument doc, DField<String> contributor) {
    doc.setField(DC_CONTRIBUTOR_PREFIX + contributor.getSuffix(), contributor.getValue(), DC_CONTRIBUTOR_BOOST);
  }

  public static List<DField<String>> getDcDescription(SolrDocument doc) {
    return getDynamicStringValues(doc, DC_DESCRIPTION_PREFIX);
  }

  public static void setDcDescription(SolrInputDocument doc, DField<String> description) {
    doc.setField(DC_DESCRIPTION_PREFIX + description.getSuffix(), description.getValue(), DC_DESCRIPTION_BOOST);
  }

  public static List<DField<String>> getDcRightsHolder(SolrDocument doc) {
    return getDynamicStringValues(doc, DC_RIGHTS_HOLDER_PREFIX);
  }

  public static void setDcRightsHolder(SolrInputDocument doc, DField<String> rightsHolder) {
    doc.setField(DC_RIGHTS_HOLDER_PREFIX + rightsHolder.getSuffix(), rightsHolder.getValue());
  }

  public static List<DField<String>> getDcSpatial(SolrDocument doc) {
    return getDynamicStringValues(doc, DC_SPATIAL_PREFIX);
  }

  public static void setDcSpatial(SolrInputDocument doc, DField<String> spatial) {
    doc.setField(DC_SPATIAL_PREFIX + spatial.getSuffix(), spatial.getValue());
  }

  public static List<DField<String>> getDcAccessRights(SolrDocument doc) {
    return getDynamicStringValues(doc, DC_ACCESS_RIGHTS_PREFIX);
  }

  public static void setDcAccessRights(SolrInputDocument doc, DField<String> accessRights) {
    doc.setField(DC_ACCESS_RIGHTS_PREFIX + accessRights.getSuffix(), accessRights.getValue());
  }

  public static List<DField<String>> getDcLicense(SolrDocument doc) {
    return getDynamicStringValues(doc, DC_LICENSE_PREFIX);
  }

  public static void setDcLicense(SolrInputDocument doc, DField<String> license) {
    doc.setField(DC_LICENSE_PREFIX + license.getSuffix(), license.getValue());
  }

  public static String getOcMediatype(SolrDocument doc) {
    return mkString(doc.get(OC_MEDIATYPE));
  }

  public static void setOcMediatype(SolrInputDocument doc, String mediatype) {
    doc.setField(OC_MEDIATYPE, mediatype);
  }

  public static String getOcMediapackage(SolrDocument doc) {
    return mkString(doc.get(OC_MEDIAPACKAGE));
  }

  public static void setOcMediapackage(SolrInputDocument doc, String mediapackage) {
    doc.setField(OC_MEDIAPACKAGE, mediapackage);
  }

  public static String getOcKeywords(SolrDocument doc) {
    return mkString(doc.get(OC_KEYWORDS));
  }

  public static String getOcKeywords(SolrInputDocument doc) {
    SolrInputField f = doc.get(OC_KEYWORDS);
    return f != null ? mkString(f.getFirstValue()) : null;
  }

  public static void setOcKeywords(SolrInputDocument doc, String keywords) {
    doc.setField(OC_KEYWORDS, keywords);
  }

  public static String getOcCover(SolrDocument doc) {
    return mkString(doc.get(OC_COVER));
  }

  public static void setOcCover(SolrInputDocument doc, String cover) {
    doc.setField(OC_COVER, cover);
  }

  public static Date getOcModified(SolrDocument doc) {
    return (Date) doc.get(OC_MODIFIED);
  }

  public static void setOcModified(SolrInputDocument doc, Date modified) {
    doc.setField(OC_MODIFIED, modified);
  }

  public static Date getOcDeleted(SolrDocument doc) {
    return mkDate(doc.get(OC_DELETED));
  }

  public static void setOcDeleted(SolrInputDocument doc, Date deleted) {
    doc.setField(OC_DELETED, deleted);
  }

  public static String getOcElementtags(SolrDocument doc) {
    return mkString(doc.get(OC_ELEMENTTAGS));
  }

  public static void setOcElementtags(SolrInputDocument doc, String elementtags) {
    doc.setField(OC_ELEMENTTAGS, elementtags);
  }

  public static String getOcElementflavors(SolrDocument doc) {
    return mkString(doc.get(OC_ELEMENTFLAVORS));
  }

  public static void setOcElementflavors(SolrInputDocument doc, String elementflavors) {
    doc.setField(OC_ELEMENTFLAVORS, elementflavors);
  }

  public static List<DField<String>> getOcText(SolrDocument doc) {
    return getDynamicStringValues(doc, OC_TEXT_PREFIX);
  }

  public static List<DField<String>> getOcHint(SolrDocument doc) {
    return getDynamicStringValues(doc, OC_HINT_PREFIX);
  }

  public static List<DField<String>> getOcAcl(SolrDocument doc) {
    return getDynamicStringValues(doc, OC_ACL_PREFIX);
  }

  public static void setOcAcl(SolrInputDocument doc, DField<String> acl) {
    doc.setField(OC_ACL_PREFIX + acl.getSuffix(), acl.getValue());
  }

  public static double getScore(SolrDocument doc) {
    return Double.parseDouble(mkString(doc.getFieldValue(SCORE)));
  }

  public static void setSegmentText(SolrInputDocument doc, DField<String> segmentText) {
    doc.setField(SEGMENT_TEXT_PREFIX + segmentText.getSuffix(), segmentText.getValue());
  }

  public static void setSegmentHint(SolrInputDocument doc, DField<String> segmentHint) {
    doc.setField(SEGMENT_HINT_PREFIX + segmentHint.getSuffix(), segmentHint.getValue());
  }

  //

  /**
   * Helper to get the first element of the given list or a default value <code>dflt</code> if the list is empty.
   */
  public static <A> A getFirst(List<DField<A>> fs, A dflt) {
    return CollectionUtil.head(fs).map(new Function<DField<A>, A>() {
      @Override
      public A apply(DField<A> f) {
        return f.getValue();
      }
    }).getOrElse(dflt);
  }

  private static String mkString(Object v) {
    return v != null ? v.toString() : null;
  }

  private static Date mkDate(Object v) {
    return v != null ? (Date) v : null;
  }

  @SuppressWarnings("unchecked")
  private static List<DField<String>> getDynamicStringValues(SolrDocument doc, String fieldPrefix) {
    List<DField<String>> r = new ArrayList<DField<String>>();
    for (String f : doc.getFieldNames()) {
      if (f.startsWith(fieldPrefix)) {
        String lang = f.substring(fieldPrefix.length());
        for (Object v : doc.getFieldValues(f)) {
          r.add(new DField(mkString(v), lang));
        }
      }
    }
    return r;
  }

}
