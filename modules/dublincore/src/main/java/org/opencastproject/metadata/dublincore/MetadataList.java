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

package org.opencastproject.metadata.dublincore;

import java.util.HashMap;
import java.util.Map;

public final class MetadataList {

  public enum Locked {
    NONE("NONE"), WORKFLOW_RUNNING("EVENTS.EVENTS.DETAILS.METADATA.LOCKED.RUNNING");

    private final String languageConstant;

    Locked(final String languageConstant) {
      this.languageConstant = languageConstant;
    }

    public String getValue() {
      return languageConstant;
    }

  }

  public static final class TitledMetadataCollection {
    private final String title;
    private final DublinCoreMetadataCollection collection;

    public TitledMetadataCollection(String title, DublinCoreMetadataCollection collection) {
      this.title = title;
      this.collection = collection;
    }

    public String getTitle() {
      return title;
    }

    public DublinCoreMetadataCollection getCollection() {
      return collection;
    }
  }

  private final Map<String, TitledMetadataCollection> metadataList = new HashMap<>();

  private Locked locked = Locked.NONE;

  public MetadataList() {
  }

  public Locked getLocked() {
    return locked;
  }

  public Map<String, TitledMetadataCollection> getMetadataList() {
    return metadataList;
  }

  public DublinCoreMetadataCollection getMetadataByAdapter(final SeriesCatalogUIAdapter catalogUIAdapter) {
    return getMetadataByFlavor(catalogUIAdapter.getFlavor().toString());
  }

  public DublinCoreMetadataCollection getMetadataByAdapter(final EventCatalogUIAdapter catalogUIAdapter) {
    return getMetadataByFlavor(catalogUIAdapter.getFlavor().toString());
  }

  public DublinCoreMetadataCollection getMetadataByFlavor(final String flavor) {
    return metadataList.keySet().stream().filter(e -> e.equals(flavor)).map(metadataList::get)
                       .map(TitledMetadataCollection::getCollection).findAny().orElse(null);
  }

  public void add(final EventCatalogUIAdapter adapter, final DublinCoreMetadataCollection metadata) {
    metadataList.put(adapter.getFlavor().toString(), new TitledMetadataCollection(adapter.getUITitle(), metadata));
  }

  public void add(final SeriesCatalogUIAdapter adapter, final DublinCoreMetadataCollection metadata) {
    metadataList.put(adapter.getFlavor().toString(), new TitledMetadataCollection(adapter.getUITitle(), metadata));
  }

  public void add(final String flavor, final String title, final DublinCoreMetadataCollection metadata) {
    metadataList.put(flavor, new TitledMetadataCollection(title, metadata));
  }

  public void setLocked(final Locked locked) {
    this.locked = locked;
  }

}
