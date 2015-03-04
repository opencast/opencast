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

package org.opencastproject.index.service.impl.index.group;

import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchMetadata;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.impl.SearchMetadataCollection;
import org.opencastproject.security.api.User;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Utility implementation to deal with the conversion of groups and its corresponding index data structures.
 */
public final class GroupIndexUtils {

  private static final Logger logger = LoggerFactory.getLogger(GroupIndexUtils.class);

  /**
   * This is a utility class and should therefore not be instantiated.
   */
  private GroupIndexUtils() {
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
  public static Group toGroup(SearchMetadataCollection metadata) throws IOException {
    Map<String, SearchMetadata<?>> metadataMap = metadata.toMap();
    String groupXml = (String) metadataMap.get(GroupIndexSchema.OBJECT).getValue();
    return Group.valueOf(IOUtils.toInputStream(groupXml));
  }

  /**
   * Creates search metadata from a group such that the group can be stored in the search index.
   *
   * @param group
   *          the group
   * @return the set of metadata
   * @throws IOException
   *           if marshalling fails
   */
  public static SearchMetadataCollection toSearchMetadata(Group group) {
    SearchMetadataCollection metadata = new SearchMetadataCollection(group.getIdentifier().concat(
            group.getOrganization()), Group.DOCUMENT_TYPE);
    metadata.addField(GroupIndexSchema.UID, group.getIdentifier(), true);
    metadata.addField(GroupIndexSchema.ORGANIZATION, group.getOrganization(), false);
    metadata.addField(GroupIndexSchema.OBJECT, group.toXML(), false);
    if (StringUtils.trimToNull(group.getDescription()) != null) {
      metadata.addField(GroupIndexSchema.DESCRIPTION, group.getDescription(), true);
    }
    if (StringUtils.trimToNull(group.getName()) != null) {
      metadata.addField(GroupIndexSchema.NAME, group.getName(), true);
    }
    if (StringUtils.trimToNull(group.getOrganization()) != null) {
      metadata.addField(GroupIndexSchema.ORGANIZATION, group.getOrganization(), true);
    }
    if (group.getRoles() != null) {
      metadata.addField(GroupIndexSchema.ROLES, group.getRoles().toArray(), true);
    }
    if (group.getMembers() != null) {
      metadata.addField(GroupIndexSchema.MEMBERS, group.getMembers().toArray(), true);
    }
    return metadata;
  }

  /**
   * Loads the group from the search index or creates a new one that can then be persisted.
   *
   * @param groupId
   *          the group identifier
   * @param organization
   *          the organization
   * @param user
   *          the user
   * @param searchIndex
   *          the {@link ExternalIndex} to search in
   * @return the group
   * @throws SearchIndexException
   *           if querying the search index fails
   * @throws IllegalStateException
   *           if multiple groups with the same identifier are found
   */
  public static Group getOrCreate(String groupId, String organization, User user, AbstractSearchIndex searchIndex)
          throws SearchIndexException {
    GroupSearchQuery query = new GroupSearchQuery(organization, user).withoutActions().withIdentifier(groupId);
    SearchResult<Group> searchResult = searchIndex.getByQuery(query);
    if (searchResult.getDocumentCount() == 0) {
      return new Group(groupId, organization);
    } else if (searchResult.getDocumentCount() == 1) {
      return searchResult.getItems()[0].getSource();
    } else {
      throw new IllegalStateException("Multiple groups with identifier " + groupId + " found in search index");
    }
  }
}
