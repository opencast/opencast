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

import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException;
import org.opencastproject.oaipmh.persistence.OaiPmhElementEntity;
import org.opencastproject.oaipmh.persistence.SearchResultElementItem;


public class SearchResultElementItemImpl implements SearchResultElementItem {

  private final String elementType;
  private final String flavor;
  private final String xml;

  public SearchResultElementItemImpl(final OaiPmhElementEntity entity) {
    this(entity.getElementType(), entity.getFlavor(), entity.getXml());
  }

  public SearchResultElementItemImpl(final String elementType, final String flavor, final String xml) {
    this.elementType = elementType;
    this.flavor = flavor;
    this.xml = xml;
  }

  @Override
  public String getType() {
    return elementType.toLowerCase();
  }

  @Override
  public String getFlavor() {
    return flavor;
  }

  @Override
  public String getXml() {
    return xml;
  }

  @Override
  public boolean isEpisodeDublinCore() {
    return MediaPackageElementFlavor.parseFlavor(getFlavor()).matches(MediaPackageElements.EPISODE);
  }

  @Override
  public boolean isSeriesDublinCore() {
    return MediaPackageElementFlavor.parseFlavor(getFlavor()).matches(MediaPackageElements.SERIES);
  }

  @Override
  public DublinCoreCatalog asDublinCore() throws OaiPmhDatabaseException {
    if (isEpisodeDublinCore() || isSeriesDublinCore())
      try {
        return DublinCoreXmlFormat.read(getXml());
    } catch (Exception ex) {
      throw new OaiPmhDatabaseException("Can not parse dublincore catalog", ex);
    }

    throw new OaiPmhDatabaseException("This element isn't a dublincore catalog");
  }
}
