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

package org.opencastproject.index.service.impl.index.series;

import static org.opencastproject.index.service.util.ListProviderUtil.splitStringList;

import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchMetadata;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.matterhorn.search.impl.SearchMetadataCollection;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.Permissions.Action;
import org.opencastproject.security.api.User;
import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility implementation to deal with the conversion of series and its corresponding index data structures.
 */
public final class SeriesIndexUtils {

  private static final Logger logger = LoggerFactory.getLogger(SeriesIndexUtils.class);

  /**
   * This is a utility class and should therefore not be instantiated.
   */
  private SeriesIndexUtils() {
  }

  /**
   * Creates a search result item based on the data returned from the search index.
   *
   * @param metadata
   *          the search metadata
   * @return the search result item
   * @throws IOException
   *           if unmarshalling fails
   */
  public static Series toSeries(SearchMetadataCollection metadata) throws IOException {
    Map<String, SearchMetadata<?>> metadataMap = metadata.toMap();
    String seriesXml = (String) metadataMap.get(SeriesIndexSchema.OBJECT).getValue();
    return Series.valueOf(IOUtils.toInputStream(seriesXml));
  }

  /**
   * Creates search metadata from a series such that the event can be stored in the search index.
   *
   * @param series
   *          the series
   * @return the set of metadata
   * @throws IOException
   *           if marshalling fails
   */
  public static SearchMetadataCollection toSearchMetadata(Series series) {
    SearchMetadataCollection metadata = new SearchMetadataCollection(series.getIdentifier().concat(
            series.getOrganization()), Series.DOCUMENT_TYPE);
    metadata.addField(SeriesIndexSchema.UID, series.getIdentifier(), true);
    metadata.addField(SeriesIndexSchema.ORGANIZATION, series.getOrganization(), false);
    metadata.addField(SeriesIndexSchema.OBJECT, series.toXML(), false);
    metadata.addField(SeriesIndexSchema.TITLE, series.getTitle(), true);
    if (StringUtils.trimToNull(series.getDescription()) != null) {
      metadata.addField(SeriesIndexSchema.DESCRIPTION, series.getDescription(), true);
    }
    if (StringUtils.trimToNull(series.getSubject()) != null) {
      metadata.addField(SeriesIndexSchema.SUBJECT, series.getSubject(), true);
    }
    if (StringUtils.trimToNull(series.getLanguage()) != null) {
      metadata.addField(SeriesIndexSchema.LANGUAGE, series.getLanguage(), true);
    }
    if (StringUtils.trimToNull(series.getCreator()) != null) {
      metadata.addField(SeriesIndexSchema.CREATOR, series.getCreator(), true);
    }
    if (StringUtils.trimToNull(series.getLicense()) != null) {
      metadata.addField(SeriesIndexSchema.LICENSE, series.getLicense(), true);
    }
    if (StringUtils.trimToNull(series.getManagedAcl()) != null) {
      metadata.addField(SeriesIndexSchema.MANAGED_ACL, series.getManagedAcl(), true);
    }
    if (series.getCreatedDateTime() != null) {
      metadata.addField(SeriesIndexSchema.CREATED_DATE_TIME,
              DateTimeSupport.toUTC(series.getCreatedDateTime().getTime()), true);
    }
    if (series.getOrganizers() != null) {
      metadata.addField(SeriesIndexSchema.ORGANIZERS, series.getOrganizers().toArray(), true);
    }
    if (series.getContributors() != null) {
      metadata.addField(SeriesIndexSchema.CONTRIBUTORS, series.getContributors().toArray(), true);
    }
    if (series.getPublishers() != null) {
      metadata.addField(SeriesIndexSchema.PUBLISHERS, series.getPublishers().toArray(), true);
    }
    if (series.getRightsHolder() != null) {
      metadata.addField(SeriesIndexSchema.RIGHTS_HOLDER, series.getRightsHolder(), true);
    }

    if (StringUtils.trimToNull(series.getAccessPolicy()) != null) {
      metadata.addField(SeriesIndexSchema.ACCESS_POLICY, series.getAccessPolicy(), true);
      addAuthorization(metadata, series.getAccessPolicy());
    }

    metadata.addField(SeriesIndexSchema.OPT_OUT, series.isOptedOut(), false);

    if (series.getTheme() != null) {
      metadata.addField(SeriesIndexSchema.THEME, series.getTheme(), false);
    }
    return metadata;
  }

  /**
   * Adds authorization fields to the input document.
   *
   * @param doc
   *          the input document
   * @param aclString
   *          the access control list string
   */
  private static void addAuthorization(SearchMetadataCollection doc, String aclString) {
    Map<String, List<String>> permissions = new HashMap<String, List<String>>();

    // Define containers for common permissions
    for (Action action : Permissions.Action.values()) {
      permissions.put(action.toString(), new ArrayList<String>());
    }

    AccessControlList acl = AccessControlParser.parseAclSilent(aclString);
    for (AccessControlEntry entry : acl.getEntries()) {
      if (!entry.isAllow()) {
        logger.info("Series index does not support denial via ACL, ignoring {}", entry);
        continue;
      }
      List<String> actionPermissions = permissions.get(entry.getAction());
      if (actionPermissions == null) {
        actionPermissions = new ArrayList<String>();
        permissions.put(entry.getAction(), actionPermissions);
      }
      actionPermissions.add(entry.getRole());
    }

    // Write the permissions to the input document
    for (Map.Entry<String, List<String>> entry : permissions.entrySet()) {
      String fieldName = SeriesIndexSchema.ACL_PERMISSION_PREFIX.concat(entry.getKey());
      doc.addField(fieldName, entry.getValue(), false);
    }
  }

  /**
   * Loads the series from the search index or creates a new one that can then be persisted.
   *
   * @param seriesId
   *          the series identifier
   * @param organization
   *          the organization
   * @param user
   *          the user
   * @param searchIndex
   *          the {@link AdminUISearchIndex} to search in
   * @return the series
   * @throws SearchIndexException
   *           if querying the search index fails
   * @throws IllegalStateException
   *           if multiple series with the same identifier are found
   */
  public static Series getOrCreate(String seriesId, String organization, User user, AbstractSearchIndex searchIndex)
          throws SearchIndexException {
    SeriesSearchQuery query = new SeriesSearchQuery(organization, user).withoutActions().withIdentifier(seriesId);
    SearchResult<Series> searchResult = searchIndex.getByQuery(query);
    if (searchResult.getDocumentCount() == 0) {
      return new Series(seriesId, organization);
    } else if (searchResult.getDocumentCount() == 1) {
      return searchResult.getItems()[0].getSource();
    } else {
      throw new IllegalStateException("Multiple series with identifier " + seriesId + " found in search index");
    }
  }

  /**
   * Update the given {@link Series} with the given {@link DublinCore}.
   *
   * @param series
   *          the series to update
   * @param dc
   *          the catalog with the metadata for the update
   * @return the updated series
   */
  public static Series updateSeries(Series series, DublinCore dc) {
    series.setTitle(dc.getFirst(DublinCoreCatalog.PROPERTY_TITLE));
    series.setDescription(dc.getFirst(DublinCore.PROPERTY_DESCRIPTION));
    series.setSubject(dc.getFirst(DublinCore.PROPERTY_SUBJECT));
    series.setLanguage(dc.getFirst(DublinCoreCatalog.PROPERTY_LANGUAGE));
    series.setLicense(dc.getFirst(DublinCoreCatalog.PROPERTY_LICENSE));
    series.setRightsHolder(dc.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER));
    Date createdDate = EncodingSchemeUtils.decodeDate(dc.getFirst(DublinCoreCatalog.PROPERTY_CREATED));
    series.setCreatedDateTime(createdDate);

    series.setPublishers(splitStringList(dc.get(DublinCore.PROPERTY_PUBLISHER, DublinCore.LANGUAGE_ANY)));
    series.setContributors(splitStringList(dc.get(DublinCore.PROPERTY_CONTRIBUTOR, DublinCore.LANGUAGE_ANY)));
    series.setOrganizers(splitStringList(dc.get(DublinCoreCatalog.PROPERTY_CREATOR, DublinCore.LANGUAGE_ANY)));
    return series;
  }

  public static void updateEventSeriesTitles(Series series, String organization, User user,
          AbstractSearchIndex searchIndex) throws SearchIndexException {
    if (!series.isSeriesTitleUpdated())
      return;

    SearchResult<Event> events = searchIndex.getByQuery(new EventSearchQuery(organization, user).withoutActions()
            .withSeriesId(series.getIdentifier()));
    for (SearchResultItem<Event> searchResultItem : events.getItems()) {
      Event event = searchResultItem.getSource();
      event.setSeriesName(series.getTitle());
      searchIndex.addOrUpdate(event);
    }
  }

  /**
   * Update a unique managed acl name to a new one for all of the series.
   *
   * @param currentManagedAcl
   *          The current name for the managed acl.
   * @param newManagedAcl
   *          The new name for the managed acl.
   * @param organization
   *          The organization for the managed acl.
   * @param user
   *          The user.
   * @param searchIndex
   *          The search index to update the managed acl name.
   */
  public static void updateManagedAclName(String currentManagedAcl, String newManagedAcl, String organization,
          User user, AbstractSearchIndex searchIndex) {
    SearchResult<Series> result = null;
    try {
      result = searchIndex.getByQuery(new SeriesSearchQuery(organization, user).withoutActions().withManagedAcl(
              currentManagedAcl));
    } catch (SearchIndexException e) {
      logger.error("Unable to find the series in org '{}' with current managed acl name '{}' because {}", new Object[] {
              organization, currentManagedAcl, ExceptionUtils.getStackTrace(e) });
    }
    if (result != null && result.getHitCount() > 0) {
      for (SearchResultItem<Series> seriesItem : result.getItems()) {
        Series series = seriesItem.getSource();
        series.setManagedAcl(newManagedAcl);
        try {
          searchIndex.addOrUpdate(series);
        } catch (SearchIndexException e) {
          logger.warn(
                  "Unable to update event '{}' from current managed acl '{}' to new managed acl name '{}' because {}",
                  new Object[] { series, currentManagedAcl, newManagedAcl, ExceptionUtils.getStackTrace(e) });
        }
      }
    }
  }

  /**
   * Delete a managed acl from all of the series that reference it.
   *
   * @param managedAcl
   *          The managed acl's unique name that will be removed.
   * @param organization
   *          The organization for the managed acl
   * @param user
   *          The user
   * @param searchIndex
   *          The search index to remove the managed acl from.
   */
  public static void deleteManagedAcl(String managedAcl, String organization, User user, AbstractSearchIndex searchIndex) {
    SearchResult<Series> result = null;
    try {
      result = searchIndex.getByQuery(new SeriesSearchQuery(organization, user).withoutActions().withManagedAcl(
              managedAcl));
    } catch (SearchIndexException e) {
      logger.error("Unable to find the series in org '{}' with current managed acl name '{}' because {}", new Object[] {
              organization, managedAcl, ExceptionUtils.getStackTrace(e) });
    }
    if (result != null && result.getHitCount() > 0) {
      for (SearchResultItem<Series> seriesItem : result.getItems()) {
        Series series = seriesItem.getSource();
        series.setManagedAcl(null);
        try {
          searchIndex.addOrUpdate(series);
        } catch (SearchIndexException e) {
          logger.warn("Unable to update series '{}' to remove managed acl '{}' because {}", new Object[] { series,
                  managedAcl, ExceptionUtils.getStackTrace(e) });
        }
      }
    }
  }
}
