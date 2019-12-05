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

import org.opencastproject.util.data.Tuple;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class MetadataList implements Iterable<Entry<String, Tuple<String, MetadataCollection>>> {

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

  private final Map<String, Tuple<String, MetadataCollection>> metadataList = new HashMap<>();

  private Locked locked = Locked.NONE;

  public MetadataList() {
  }

  public Locked getLocked() {
    return locked;
  }

  public void makeMetadataCollectionReadOnly(final MetadataCollection metadataCollection) {
    for (final MetadataField field : metadataCollection.getFields())
      field.setReadOnly(true);
  }

  public Map<String, Tuple<String, MetadataCollection>> getMetadataList() {
    return metadataList;
  }

  public MetadataCollection getMetadataByAdapter(final SeriesCatalogUIAdapter catalogUIAdapter) {
    return getMetadataByFlavor(catalogUIAdapter.getFlavor().toString());
  }

  public MetadataCollection getMetadataByAdapter(final EventCatalogUIAdapter catalogUIAdapter) {
    return getMetadataByFlavor(catalogUIAdapter.getFlavor().toString());
  }

  public MetadataCollection getMetadataByFlavor(final String flavor) {
    return metadataList.keySet().stream().filter(e -> e.equals(flavor)).map(metadataList::get).map(Tuple::getB)
            .findAny().orElse(null);
  }

  public void add(final EventCatalogUIAdapter adapter, final MetadataCollection metadata) {
    metadataList.put(adapter.getFlavor().toString(), Tuple.tuple(adapter.getUITitle(), metadata));
  }

  public void add(final SeriesCatalogUIAdapter adapter, final MetadataCollection metadata) {
    metadataList.put(adapter.getFlavor().toString(), Tuple.tuple(adapter.getUITitle(), metadata));
  }

  public void add(final String flavor, final String title, final MetadataCollection metadata) {
    metadataList.put(flavor, Tuple.tuple(title, metadata));
  }

  @Override
  public Iterator<Entry<String, Tuple<String, MetadataCollection>>> iterator() {
    return metadataList.entrySet().iterator();
  }

  public void setLocked(final Locked locked) {
    this.locked = locked;
  }

}
