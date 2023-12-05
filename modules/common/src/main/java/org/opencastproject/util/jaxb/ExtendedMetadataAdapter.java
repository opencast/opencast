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

package org.opencastproject.util.jaxb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * An adapter to transform a map with extended metadata into an object that can be represented in XMl, and back.
 */
public class ExtendedMetadataAdapter extends XmlAdapter<ExtendedMetadataAdapter.ExtendedMetadata,
        Map<String, Map<String, List<String>>>> {

  @Override
  public ExtendedMetadata marshal(Map<String, Map<String, List<String>>> map) {
    List<ExtendedMetadataEntry> entries = new ArrayList<>();
    for (String catalogType: map.keySet()) {
      Map<String, List<String>> catalog = map.get(catalogType);

      for (String fieldName: catalog.keySet()) {
        List<String> values = catalog.get(fieldName);
        entries.add(new ExtendedMetadataEntry(catalogType, fieldName, values));
      }
    }
    return new ExtendedMetadata(entries);
  }

  @Override
  public Map<String, Map<String, List<String>>> unmarshal(ExtendedMetadata extendedMetadata) {
    Map<String, Map<String, List<String>>> map = new HashMap<>();
    for (ExtendedMetadataEntry entry: extendedMetadata.getEntries()) {
      String catalogType = entry.getCatalogType();
      String fieldName = entry.getFieldName();
      List<String> values = entry.getValues();

      Map<String, List<String>> catalog = map.computeIfAbsent(catalogType, c -> new HashMap<>());
      catalog.put(fieldName, values);
    }
    return map;
  }

  /**
   * These classes are used to represent the extended metadata map in XML.
   */

  protected static class ExtendedMetadata {
    @XmlElement(name = "entry")
    private List<ExtendedMetadataEntry> entries;

    public ExtendedMetadata() {
      // needed by JAXB
    }

    public ExtendedMetadata(List<ExtendedMetadataEntry> entries) {
      this.entries = entries;
    }

    public List<ExtendedMetadataEntry> getEntries() {
      return entries;
    }
  }

  protected static class ExtendedMetadataEntry {
    @XmlAttribute
    private String catalogType;
    @XmlAttribute
    private String fieldName;
    @XmlValue
    private List<String> values;

    public ExtendedMetadataEntry() {
      // needed by JAXB
    }

    public ExtendedMetadataEntry(String catalogType, String fieldName, List<String> values) {
      this.catalogType = catalogType;
      this.fieldName = fieldName;
      this.values = values;
    }

    public String getCatalogType() {
      return catalogType;
    }

    public String getFieldName() {
      return fieldName;
    }

    public List<String> getValues() {
      return values;
    }
  }
}
