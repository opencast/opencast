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
package org.opencastproject.oaipmh.persistence.impl;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException;
import org.opencastproject.oaipmh.persistence.OaiPmhElementEntity;
import org.opencastproject.oaipmh.persistence.OaiPmhEntity;
import org.opencastproject.oaipmh.persistence.SearchResultElementItem;
import org.opencastproject.oaipmh.persistence.SearchResultItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SearchResultItemImpl implements SearchResultItem {

  private final String mediaPackageId;
  private final MediaPackage mediaPackage;
  private final String mediaPackageXml;
  private final String organization;
  private final String repoId;
  private final Date modificationDate;
  private final boolean isDeleted;
  private final List<SearchResultElementItem> mediaPackageElements;

  public SearchResultItemImpl(final OaiPmhEntity entity) throws MediaPackageException {
    this.mediaPackageId = entity.getMediaPackageId();
    this.mediaPackageXml = entity.getMediaPackageXML();
    this.organization = entity.getOrganization();
    this.repoId = entity.getRepositoryId();
    this.modificationDate = entity.getModificationDate();
    this.isDeleted = entity.isDeleted();
    this.mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
    this.mediaPackageElements = new ArrayList<>();
    for (OaiPmhElementEntity elementEntity : entity.getMediaPackageElements()) {
      SearchResultElementItemImpl searchResultElementItem = new SearchResultElementItemImpl(elementEntity);
      mediaPackageElements.add(searchResultElementItem);
    }
  }

  @Override
  public String getId() {
    return mediaPackageId;
  }

  @Override
  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  @Override
  public String getMediaPackageXml() {
    return mediaPackageXml;
  }

  @Override
  public String getOrganization() {
    return organization;
  }

  @Override
  public String getRepository() {
    return repoId;
  }

  @Override
  public Date getModificationDate() {
    return modificationDate;
  }

  @Override
  public boolean isDeleted() {
    return isDeleted;
  }

  @Override
  public List<SearchResultElementItem> getElements() {
    return mediaPackageElements;
  }

  @Override
  public DublinCoreCatalog getEpisodeDublinCore() throws OaiPmhDatabaseException {
    for (SearchResultElementItem element : getElements()) {
      if (element.isEpisodeDublinCore()) {
        return element.asDublinCore();
      }
    }
    throw new OaiPmhDatabaseException("Episode dublincore not found");
  }

  @Override
  public DublinCoreCatalog getSeriesDublinCore() throws OaiPmhDatabaseException {
    for (SearchResultElementItem element : getElements()) {
      if (element.isSeriesDublinCore()) {
        return element.asDublinCore();
      }
    }
    throw new OaiPmhDatabaseException("Series dublincore catalog not found");
  }
}
