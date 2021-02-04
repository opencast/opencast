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


package org.opencastproject.elasticsearch.index.group;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchMetadata;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.impl.SearchMetadataCollection;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.security.api.User;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;

import javax.xml.bind.Unmarshaller;

/**
 * Utility implementation to deal with the conversion of groups and its corresponding index data structures.
 */
public final class GroupIndexUtils {

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
  public static Group toGroup(SearchMetadataCollection metadata, Unmarshaller unmarshaller) throws IOException {
    Map<String, SearchMetadata<?>> metadataMap = metadata.toMap();
    String groupXml = (String) metadataMap.get(GroupIndexSchema.OBJECT).getValue();
    return Group.valueOf(IOUtils.toInputStream(groupXml), unmarshaller);
  }

  /**
   * Creates search metadata from a group such that the group can be stored in the search index.
   *
   * @param group
   *          the group
   * @return the set of metadata
   */
  public static SearchMetadataCollection toSearchMetadata(Group group) {
    SearchMetadataCollection metadata = new SearchMetadataCollection(group.getIdentifier().concat(
            group.getOrganization()), Group.DOCUMENT_TYPE);
    metadata.addField(GroupIndexSchema.UID, group.getIdentifier(), true);
    metadata.addField(GroupIndexSchema.ORGANIZATION, group.getOrganization(), false);
    metadata.addField(GroupIndexSchema.OBJECT, group.toXML(), false);
    metadata.addField(GroupIndexSchema.ROLE, group.getRole(), true);
    if (StringUtils.isNotBlank(group.getDescription())) {
      metadata.addField(GroupIndexSchema.DESCRIPTION, group.getDescription(), true);
    }
    if (StringUtils.isNotBlank(group.getName())) {
      metadata.addField(GroupIndexSchema.NAME, group.getName(), true);
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
   *          the Index to search in
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
