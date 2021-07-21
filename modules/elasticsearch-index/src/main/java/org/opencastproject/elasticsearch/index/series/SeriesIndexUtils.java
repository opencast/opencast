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

package org.opencastproject.elasticsearch.index.series;

import org.opencastproject.elasticsearch.api.SearchMetadata;
import org.opencastproject.elasticsearch.impl.SearchMetadataCollection;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.Permissions.Action;
import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.Unmarshaller;

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
   * @param unmarshaller the unmarshaller to use
   * @return the search result item
   * @throws IOException
   *           if unmarshalling fails
   */
  public static Series toSeries(SearchMetadataCollection metadata, Unmarshaller unmarshaller) throws IOException {
    Map<String, SearchMetadata<?>> metadataMap = metadata.toMap();
    String seriesXml = (String) metadataMap.get(SeriesIndexSchema.OBJECT).getValue();
    return Series.valueOf(IOUtils.toInputStream(seriesXml, Charset.defaultCharset()), unmarshaller);
  }

  /**
   * Creates search metadata from a series such that the event can be stored in the search index.
   *
   * @param series
   *          the series
   * @return the set of metadata
   */
  public static SearchMetadataCollection toSearchMetadata(Series series) {
    SearchMetadataCollection metadata = new SearchMetadataCollection(
            series.getIdentifier().concat(series.getOrganization()), Series.DOCUMENT_TYPE);
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
      metadata.addField(SeriesIndexSchema.ACCESS_POLICY, series.getAccessPolicy(), false);
      addAuthorization(metadata, series.getAccessPolicy());
    }

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
}
