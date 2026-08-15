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

package org.opencastproject.metadata.dublincore;

import static org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_UNDEFINED;

import org.opencastproject.mediapackage.EName;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Parse a DublinCore catalog from JSON.
 * <p>
 * <strong>Known limitations:</strong> Encoding schemas can currently only be from the
 * {@link DublinCore#TERMS_NS_URI} namespace using the {@link DublinCore#TERMS_NS_PREFIX}
 * since the JSON format does not serialize namespace bindings. Example: <code>dcterms:W3CDTF</code>
 */
@ParametersAreNonnullByDefault
public final class DublinCoreJsonFormat {
  private DublinCoreJsonFormat() {
  }

  /**
   * Read a JSON encoded catalog from a string.
   */
  @Nonnull
  public static DublinCoreCatalog read(String json) {
    return read(JsonParser.parseString(json).getAsJsonObject());
  }

  /**
   * Reads values from a JSON object into a new DublinCore catalog.
   */
  @Nonnull
  public static DublinCoreCatalog read(JsonObject json) {
    // Use a standard catalog to get the required namespace bindings in order to be able
    // to parse standard DublinCore encoding schemes.
    // See http://dublincore.org/documents/dc-xml-guidelines/, section 5.2, recommendation 7 for details.
    // TODO the JSON representation should serialize the contained bindings like XML to be able to
    //   reconstruct a catalog from the serialization alone without the need to rely on bindings, registered
    //   before.
    final DublinCoreCatalog dc = DublinCores.mkStandard();
    for (Entry<String, JsonElement> namespaceEntry : json.entrySet()) { // e.g. http://purl.org/dc/terms/
      final String namespace = namespaceEntry.getKey();
      final JsonObject namespaceObj = namespaceEntry.getValue().getAsJsonObject();
      for (final Entry<String, JsonElement> entry : namespaceObj.entrySet()) { // e.g. title
        final String key = entry.getKey();
        for (final JsonElement valueObject : entry.getValue().getAsJsonArray()) {
          final JsonObject value = valueObject.getAsJsonObject();
          // the value
          final String valueString = getStringOrNull(value, "value");
          // the language
          final String lang;
          {
            final String l = getStringOrNull(value, "lang");
            lang = l != null ? l : LANGUAGE_UNDEFINED;
          }
          // the encoding scheme
          final EName encodingScheme;
          {
            final String s = getStringOrNull(value, "type");
            encodingScheme = s != null ? dc.toEName(s) : null;
          }
          // add the new value to this DC document
          dc.add(new EName(namespace, key), valueString, lang, encodingScheme);
        }
      }
    }
    return dc;
  }

  /** Read a string member, returning null when it is absent or JSON null. */
  private static String getStringOrNull(JsonObject json, String key) {
    final JsonElement value = json.get(key);
    return value == null || value.isJsonNull() ? null : value.getAsString();
  }

  /**
   * Converts the catalog to JSON object.
   *
   * @return JSON object
   */
  @Nonnull
  public static JsonObject writeJsonObject(DublinCoreCatalog dc) {
    // The top-level json object
    final JsonObject json = new JsonObject();
    // First collect all namespaces
    final SortedSet<String> namespaces = new TreeSet<String>();
    final Set<Entry<EName, List<DublinCoreValue>>> values = dc.getValues().entrySet();
    for (final Entry<EName, List<DublinCoreValue>> entry : values) {
      namespaces.add(entry.getKey().getNamespaceURI());
    }
    // Add a json object for each namespace
    for (String namespace : namespaces) {
      json.add(namespace, new JsonObject());
    }
    // Add the data into the appropriate array
    for (final Entry<EName, List<DublinCoreValue>> entry : values) {
      final EName ename = entry.getKey();
      final String namespace = ename.getNamespaceURI();
      final String localName = ename.getLocalName();
      final JsonObject namespaceObject = json.getAsJsonObject(namespace);
      final JsonArray localNameArray;
      {
        final JsonArray ns = namespaceObject.getAsJsonArray(localName);
        if (ns != null) {
          localNameArray = ns;
        } else {
          localNameArray = new JsonArray();
          namespaceObject.add(localName, localNameArray);
        }
      }
      for (DublinCoreValue value : entry.getValue()) {
        final String lang = value.getLanguage();
        final Optional<EName> encScheme = value.getEncodingScheme();
        final JsonObject v = new JsonObject();
        v.addProperty("value", value.getValue());
        if (!DublinCore.LANGUAGE_UNDEFINED.equals(lang)) {
          v.addProperty("lang", lang);
        }
        if (encScheme.isPresent()) {
          v.addProperty("type", dc.toQName(encScheme.get()));
        }
        localNameArray.add(v);
      }
    }
    return json;
  }

  /**
   * Checks if the catalog string is in JSON format.
   *
   * @param catalogString
   *         Dublin Core catalog as a string
   * @return
   *         true if it's in JSON format
   */
  public static boolean isJson(String catalogString) {
    return catalogString.startsWith("{");
  }
}
