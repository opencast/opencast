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

import static org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_UNDEFINED;
import static org.opencastproject.util.EqualsUtil.ne;

import com.entwinemedia.fn.data.Opt;
import org.opencastproject.mediapackage.EName;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Parse a DublinCore catalog from JSON.
 * <p/>
 * <strong>Known limitations:</strong> Encoding schemas can currently only be from the
 * {@link DublinCore#TERMS_NS_URI} namespace using the {@link DublinCore#TERMS_NS_PREFIX}
 * since the JSON format does not serialize namespace bindings. Example: <code>dcterms:W3CDTF</code>
 */
@ParametersAreNonnullByDefault
public final class DublinCoreJsonFormat {
  private DublinCoreJsonFormat() {
  }

  /**
   * Read a JSON encoded catalog from a stream.
   */
  @Nonnull
  public static DublinCoreCatalog read(InputStream json) throws IOException, ParseException {
    return read((JSONObject) new JSONParser().parse(new InputStreamReader(json)));
  }

  /**
   * Read a JSON encoded catalog from a string.
   */
  @Nonnull
  public static DublinCoreCatalog read(String json) throws IOException, ParseException {
    return read((JSONObject) new JSONParser().parse(json));
  }

  /**
   * Reads values from a JSON object into a new DublinCore catalog.
   */
  @SuppressWarnings("unchecked")
  @Nonnull
  public static DublinCoreCatalog read(JSONObject json) {
    // Use a standard catalog to get the required namespace bindings in order to be able
    // to parse standard DublinCore encoding schemes.
    // See http://dublincore.org/documents/dc-xml-guidelines/, section 5.2, recommendation 7 for details.
    // TODO the JSON representation should serialize the contained bindings like XML to be able to
    //   reconstruct a catalog from the serialization alone without the need to rely on bindings, registered
    //   before.
    final DublinCoreCatalog dc = DublinCores.mkStandard();
    final Set<Entry<String, JSONObject>> namespaceEntrySet = json.entrySet();
    for (Entry<String, JSONObject> namespaceEntry : namespaceEntrySet) { // e.g. http://purl.org/dc/terms/
      final String namespace = namespaceEntry.getKey();
      final JSONObject namespaceObj = namespaceEntry.getValue();
      final Set<Entry<String, JSONArray>> entrySet = namespaceObj.entrySet();
      for (final Entry<String, JSONArray> entry : entrySet) { // e.g. title
        final String key = entry.getKey();
        final JSONArray values = entry.getValue();
        for (final Object valueObject : values) {
          final JSONObject value = (JSONObject) valueObject;
          // the value
          final String valueString = (String) value.get("value");
          // the language
          final String lang;
          {
            final String l = (String) value.get("lang");
            lang = l != null ? l : LANGUAGE_UNDEFINED;
          }
          // the encoding scheme
          final EName encodingScheme;
          {
            final String s = (String) value.get("type");
            encodingScheme = s != null ? dc.toEName(s) : null;
          }
          // add the new value to this DC document
          dc.add(new EName(namespace, key), valueString, lang, encodingScheme);
        }
      }
    }
    return dc;
  }

  /**
   * Converts the catalog to JSON object.
   *
   * @return JSON object
   */
  @SuppressWarnings("unchecked")
  @Nonnull
  public static JSONObject writeJsonObject(DublinCoreCatalog dc) {
    // The top-level json object
    final JSONObject json = new JSONObject();
    // First collect all namespaces
    final SortedSet<String> namespaces = new TreeSet<String>();
    final Set<Entry<EName, List<DublinCoreValue>>> values = dc.getValues().entrySet();
    for (final Entry<EName, List<DublinCoreValue>> entry : values) {
      namespaces.add(entry.getKey().getNamespaceURI());
    }
    // Add a json object for each namespace
    for (String namespace : namespaces) {
      json.put(namespace, new JSONObject());
    }
    // Add the data into the appropriate array
    for (final Entry<EName, List<DublinCoreValue>> entry : values) {
      final EName ename = entry.getKey();
      final String namespace = ename.getNamespaceURI();
      final String localName = ename.getLocalName();
      final JSONObject namespaceObject = (JSONObject) json.get(namespace);
      final JSONArray localNameArray;
      {
        final JSONArray ns = (JSONArray) namespaceObject.get(localName);
        if (ns != null) {
          localNameArray = ns;
        } else {
          localNameArray = new JSONArray();
          namespaceObject.put(localName, localNameArray);
        }
      }
      for (DublinCoreValue value : entry.getValue()) {
        final String lang = value.getLanguage();
        final Opt<EName> encScheme = value.getEncodingScheme();
        final JSONObject v = new JSONObject();
        v.put("value", value.getValue());
        if (ne(DublinCore.LANGUAGE_UNDEFINED, lang)) {
          v.put("lang", lang);
        }
        for (EName e : encScheme) {
          v.put("type", dc.toQName(e));
        }
        localNameArray.add(v);
      }
    }
    return json;
  }
}
