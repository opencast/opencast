/*
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

package org.opencastproject.search.api;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.OpencastDctermsDublinCore;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;

import com.google.gson.Gson;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchResult {

  public static final String TYPE = "type";
  public static final String MEDIAPACKAGE = "mediapackage";
  public static final String MEDIAPACKAGE_XML = "mediapackage_xml";
  public static final String DUBLINCORE = "dc";
  public static final String ORG = "org";
  public static final String MODIFIED_DATE = "modified";
  public static final String DELETED_DATE = "deleted";
  public static final String INDEX_ACL = "searchable_acl";
  public static final String REST_ACL = "acl";

  private static final Gson gson = new Gson();

  private SearchService.IndexEntryType type;

  private MediaPackage mp;

  private DublinCoreCatalog dublinCore;

  private AccessControlList acl;

  private String orgId;

  private String id = null;

  private Instant modified = null;

  private Instant deleted = null;

  public SearchResult(SearchService.IndexEntryType type, DublinCoreCatalog dc, AccessControlList acl,
          String orgId, MediaPackage mp, Instant modified, Instant deleted) {
    this.type = type;
    this.dublinCore = dc;
    this.acl = acl;
    this.orgId = orgId;
    this.mp = mp;
    this.deleted = deleted;
    this.modified = modified;

    if (SearchService.IndexEntryType.Episode.equals(type)) {
      this.id = this.getMediaPackage().getIdentifier().toString();
    } else if (SearchService.IndexEntryType.Series.equals(type)) {
      this.id = this.dublinCore.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    }
  }

  public Date getModifiedDate() {
    return new Date(this.modified.toEpochMilli());
  }

  public String getId() {
    return this.id;
  }

  public Date getDeletionDate() {
    return null == this.deleted ? null : new Date(this.deleted.toEpochMilli());
  }

  @SuppressWarnings("unchecked")
  public static SearchResult rehydrate(Map<String, Object> data) throws SearchException {
    //Our sole parameter here is a map containing a mix of string:string pairs, and String:Map<String, Object> pairs

    try {
      //We're *really* hoping that no one feeds us things that aren't what we expect
      // but ES results come back in json, and get turned into multi-layered Maps
      SearchService.IndexEntryType type = SearchService.IndexEntryType.valueOf((String) data.get(TYPE));
      DublinCoreCatalog dc = rehydrateDC(type, (Map<String, Object>) data.get(DUBLINCORE));
      AccessControlList acl = rehydrateACL((Map<String, Object>) data.get(INDEX_ACL));
      String org = (String) data.get(ORG);

      Instant deleted = null;
      if (data.containsKey(DELETED_DATE) && !data.get(DELETED_DATE).equals("null")) {
        deleted = Instant.parse((String) data.get(DELETED_DATE));
      }

      Instant modified = null;
      if (data.containsKey(MODIFIED_DATE) && !data.get(MODIFIED_DATE).equals("null")) {
        modified = Instant.parse((String) data.get(MODIFIED_DATE));
      }


      MediaPackage mp = null;
      //There had better be a mediapackage with an episode...
      if (SearchService.IndexEntryType.Episode.equals(type)) {
        mp = MediaPackageParser.getFromXml((String) data.get(MEDIAPACKAGE_XML));
      }
      return new SearchResult(type, dc, acl, org, mp, modified, deleted);
    } catch (MediaPackageException e) {
      throw new SearchException(e);
    }
  }

  public static Map<String, List<String>> dehydrateDC(DublinCoreCatalog dublinCoreCatalog, Instant now) {
    var metadata = new HashMap<String, List<String>>();
    for (var entry : dublinCoreCatalog.getValues().entrySet()) {
      var key = entry.getKey().getLocalName();
      var values = entry.getValue().stream().map(DublinCoreValue::getValue).collect(Collectors.toList());
      metadata.put(key, values);
    }

    return metadata;
  }

  /**
   * Simplify ACL structure, so we can easily search by action.
   * @param acl The access control List to restructure
   * @return Restructured ACL
   */
  public static Map<String, Set<String>> dehydrateAclForIndex(AccessControlList acl) {
    var result = new HashMap<String, Set<String>>();
    for (var entry : acl.getEntries()) {
      var action = entry.getAction();
      if (!result.containsKey(action)) {
        result.put(action, new HashSet<>());
      }
      result.get(action).add(entry.getRole());
    }
    return result;
  }

  public static List<Map<String, ?>> dehydrateAclForREST(AccessControlList acl) {
    return acl.getEntries().stream()
        .map(ace -> Map.of("action", ace.getAction(), "role", ace.getRole(), "allow", Boolean.TRUE))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  public static AccessControlList rehydrateACL(Map<String, Object> map) {
    List<AccessControlEntry> aces = new LinkedList<>();
    for (var entry : map.entrySet()) {
      String action = entry.getKey();
      for (String rolename : (List<String>)  entry.getValue()) {
        AccessControlEntry ace = new AccessControlEntry(rolename, action, true);
        aces.add(ace);
      }
    }
    return new AccessControlList(aces);
  }

  @SuppressWarnings("unchecked")
  public static DublinCoreCatalog rehydrateDC(SearchService.IndexEntryType type, Map<String, Object> map)
          throws SearchException {
    OpencastDctermsDublinCore dc;
    if (SearchService.IndexEntryType.Episode.equals(type)) {
      dc = DublinCores.mkOpencastEpisode();
    } else if (SearchService.IndexEntryType.Series.equals(type)) {
      dc = DublinCores.mkOpencastSeries();
    } else {
      throw new SearchException("Unknown DC type!");
    }
    for (var entry: map.entrySet()) {
      String key = entry.getKey();
      //This is *always* a list, per dehydrateACL
      List<String> value = (List<String>) entry.getValue();
      dc.set(EName.mk(DublinCore.TERMS_NS_URI, key), value);
    }
    return dc.getCatalog();
  }

  public Map<String, Object> dehydrateForIndex() {
    return dehydrate().entrySet().stream()
        .filter(entry -> !entry.getKey().equals(REST_ACL))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<String, Object> dehydrateForREST() {
    return dehydrate().entrySet().stream()
        .filter(entry -> !entry.getKey().equals(INDEX_ACL))
        .filter(entry -> !entry.getKey().equals(MEDIAPACKAGE_XML))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<String, Object> dehydrate() {
    if (SearchService.IndexEntryType.Episode.equals(getType())) {
      return dehydrateEpisode();
    } else if (SearchService.IndexEntryType.Series.equals(getType())) {
      return dehydrateSeries();
    }
    return null;
  }

  public Map<String, Object> dehydrateEpisode() {
    Instant now = Instant.now();
    Map<String, List<String>> metadata = SearchResult.dehydrateDC(this.dublinCore, now);

    var mediaPackageJson = gson.fromJson(MediaPackageParser.getAsJSON(this.mp), Map.class).get(MEDIAPACKAGE);
    var jsonDel = null == this.deleted ? "null" : DateTimeFormatter.ISO_INSTANT.format(this.deleted);
    var jsonMod = null == this.modified ? "null" : DateTimeFormatter.ISO_INSTANT.format(this.modified);

    return Map.of(MEDIAPACKAGE, mediaPackageJson, MEDIAPACKAGE_XML, MediaPackageParser.getAsXml(this.mp),
        INDEX_ACL, SearchResult.dehydrateAclForIndex(acl), REST_ACL, SearchResult.dehydrateAclForREST(acl),
        DUBLINCORE, metadata,
        ORG, this.orgId, TYPE, this.type.name(),
        DELETED_DATE, jsonDel, MODIFIED_DATE, jsonMod);

  }

  public Map<String, Object> dehydrateSeries() {
    Instant now = Instant.now();
    Map<String, List<String>> metadata = SearchResult.dehydrateDC(this.dublinCore, now);
    var jsonDel = null == this.deleted ? "null" : DateTimeFormatter.ISO_INSTANT.format(this.deleted);
    var jsonMod = null == this.modified ? "null" : DateTimeFormatter.ISO_INSTANT.format(this.modified);

    return Map.of(
        INDEX_ACL, SearchResult.dehydrateAclForIndex(acl), REST_ACL, SearchResult.dehydrateAclForREST(acl),
        DUBLINCORE, metadata,
        ORG, this.orgId, TYPE, this.type.name(),
        MODIFIED_DATE, jsonMod, DELETED_DATE, jsonDel);
  }

  public DublinCoreCatalog getDublinCore() {
    return this.dublinCore;
  }

  public AccessControlList getAcl() {
    return acl;
  }

  public MediaPackage getMediaPackage() {
    return mp;
  }

  public SearchService.IndexEntryType getType() {
    return type;
  }

  public String getOrgId() {
    return orgId;
  }
}
