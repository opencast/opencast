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

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class MetadataList implements Iterable<Entry<String, Tuple<String, MetadataCollection>>> {

  public enum Locked {
    NONE("NONE"), WORKFLOW_RUNNING("EVENTS.EVENTS.DETAILS.METADATA.LOCKED.RUNNING");

    private String languageConstant;

    Locked(String languageConstant) {
      this.languageConstant = languageConstant;
    }

    public String getValue() {
      return languageConstant;
    }

  }

  private Map<String, Tuple<String, MetadataCollection>> metadataList = new HashMap<>();

  private Locked locked = Locked.NONE;

  public MetadataList() {
  }

  public Locked getLocked() {
    return locked;
  }

  public void makeMetadataCollectionReadOnly(MetadataCollection metadataCollection) {
    for (MetadataField<?> field : metadataCollection.getFields())
      field.setReadOnly(true);
  }

  public Map<String, Tuple<String, MetadataCollection>> getMetadataList() {
    return metadataList;
  }

  public Opt<MetadataCollection> getMetadataByAdapter(SeriesCatalogUIAdapter catalogUIAdapter) {
    return Stream.$(metadataList.keySet()).filter(adapterFilter._2(catalogUIAdapter.getFlavor().toString()))
            .map(toMetadata).head();
  }

  public Opt<MetadataCollection> getMetadataByAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    return Stream.$(metadataList.keySet()).filter(adapterFilter._2(catalogUIAdapter.getFlavor().toString()))
            .map(toMetadata).head();
  }

  public Opt<MetadataCollection> getMetadataByFlavor(String flavor) {
    return Stream.$(metadataList.keySet()).filter(adapterFilter._2(flavor)).map(toMetadata).head();
  }

  private static final Fn2<String, String, Boolean> adapterFilter = new Fn2<String, String, Boolean>() {
    @Override
    public Boolean apply(String key, String flavor) {
      return key.equals(flavor);
    }
  };

  private final Fn<String, MetadataCollection> toMetadata = new Fn<String, MetadataCollection>() {
    @Override
    public MetadataCollection apply(String key) {
      return metadataList.get(key).getB();
    }
  };

  public void add(EventCatalogUIAdapter adapter, MetadataCollection metadata) {
    metadataList.put(adapter.getFlavor().toString(), Tuple.tuple(adapter.getUITitle(), metadata));
  }

  public void add(SeriesCatalogUIAdapter adapter, MetadataCollection metadata) {
    metadataList.put(adapter.getFlavor().toString(), Tuple.tuple(adapter.getUITitle(), metadata));
  }

  public void add(String flavor, String title, MetadataCollection metadata) {
    metadataList.put(flavor, Tuple.tuple(title, metadata));
  }

  @Override
  public Iterator<Entry<String, Tuple<String, MetadataCollection>>> iterator() {
    return metadataList.entrySet().iterator();
  }

  public void setLocked(Locked locked) {
    this.locked = locked;
  }

}
