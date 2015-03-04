/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.metadata.dublincore;

import static com.entwinemedia.fn.Stream.$;
import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.data.Monadics.mlist;

import com.entwinemedia.fn.Fns;
import com.entwinemedia.fn.data.ImmutableSetWrapper;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.XMLCatalogImpl;
import org.opencastproject.metadata.api.MetadataCatalog;
import org.opencastproject.util.RequireUtil;
import org.opencastproject.util.XmlNamespaceContext;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Catalog for DublinCore structured metadata to be serialized as XML.
 * <p/>
 * Attention: Encoding schemes are not preserved! See http://opencast.jira.com/browse/MH-8759
 */
@ParametersAreNonnullByDefault
public class DublinCoreCatalog extends XMLCatalogImpl implements DublinCore, MetadataCatalog, Cloneable {
  private static final long serialVersionUID = -4568663918115847488L;

  /** A flavor that matches any dublin core element */
  public static final MediaPackageElementFlavor ANY_DUBLINCORE = MediaPackageElementFlavor.parseFlavor("dublincore/*");

  private EName rootTag;

  /** Create a new catalog. */
  DublinCoreCatalog() {
  }

  public void setRootTag(EName rootTag) {
    this.rootTag = rootTag;
  }

  @Nullable
  public EName getRootTag() {
    return rootTag;
  }

  public void addBindings(XmlNamespaceContext ctx) {
    bindings = this.bindings.add(ctx);
  }

  @Override
  public String toString() {
    return "DublinCore" + (getIdentifier() != null ? "(" + getIdentifier() + ")" : "");
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> get(EName property, final String language) {
    RequireUtil.notNull(property, "property");
    RequireUtil.notNull(language, "language");
    if (LANGUAGE_ANY.equals(language)) {
      return (List<String>) CollectionUtils.collect(getValuesAsList(property), new Transformer() {
        @Override
        public Object transform(Object o) {
          return ((CatalogEntry) o).getValue();
        }
      });
    } else {
      final List<String> values = new ArrayList<String>();
      final boolean langUndef = LANGUAGE_UNDEFINED.equals(language);
      CollectionUtils.forAllDo(getValuesAsList(property), new Closure() {
        @Override
        public void execute(Object o) {
          CatalogEntry c = (CatalogEntry) o;
          String lang = c.getAttribute(XML_LANG_ATTR);
          if ((langUndef && lang == null) || (language.equals(lang)))
            values.add(c.getValue());
        }
      });
      return values;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DublinCoreValue> get(EName property) {
    RequireUtil.notNull(property, "property");
    return mlist(getValuesAsList(property)).map(toDublinCoreValue).value();
  }

  private DublinCoreValue toDublinCoreValue(CatalogEntry e) {
    final String langRaw = e.getAttribute(XML_LANG_ATTR);
    final String lang = langRaw != null ? langRaw : LANGUAGE_UNDEFINED;
    final String typeRaw = e.getAttribute(XSI_TYPE_ATTR);
    if (typeRaw != null) {
      return DublinCoreValue.mk(e.getValue(), lang, toEName(typeRaw));
    } else {
      return DublinCoreValue.mk(e.getValue(), lang);
    }
  }

  private final Function<CatalogEntry, DublinCoreValue> toDublinCoreValue = new Function<CatalogEntry, DublinCoreValue>() {
    @Override
    public DublinCoreValue apply(CatalogEntry e) {
      return toDublinCoreValue(e);
    }
  };

  @Override
  public Map<EName, List<DublinCoreValue>> getValues() {
    return mlist(data.values().iterator())
            .foldl(new HashMap<EName, List<DublinCoreValue>>(),
                    new Function2<HashMap<EName, List<DublinCoreValue>>, List<CatalogEntry>, HashMap<EName, List<DublinCoreValue>>>() {
                      @Override
                      public HashMap<EName, List<DublinCoreValue>> apply(HashMap<EName, List<DublinCoreValue>> map,
                              List<CatalogEntry> entries) {
                        if (entries.size() > 0) {
                          final EName property = entries.get(0).getEName();
                          map.put(property, mlist(entries).map(toDublinCoreValue).value());
                        }
                        return map;
                      }
                    });
  }

  @Override public List<DublinCoreValue> getValuesFlat() {
    return $(data.values()).bind(Fns.<List<CatalogEntry>>id()).map(toDublinCoreValue.toFn()).toList();
  }

  @Override
  @Nullable
  public String getFirst(EName property, String language) {
    RequireUtil.notNull(property, "property");
    RequireUtil.notNull(language, "language");

    final CatalogEntry f = getFirstCatalogEntry(property, language);
    return f != null ? f.getValue() : null;
  }

  @Override
  public String getFirst(EName property) {
    RequireUtil.notNull(property, "property");

    final CatalogEntry f = getFirstCatalogEntry(property, LANGUAGE_ANY);
    return f != null ? f.getValue() : null;
  }

  @Override
  public DublinCoreValue getFirstVal(EName property) {
    final CatalogEntry f = getFirstCatalogEntry(property, LANGUAGE_ANY);
    return f != null ? toDublinCoreValue(f) : null;
  }

  private CatalogEntry getFirstCatalogEntry(EName property, String language) {
    CatalogEntry entry = null;
    switch (language) {
      case LANGUAGE_UNDEFINED:
        entry = getFirstLocalizedValue(property, null);
        break;
      case LANGUAGE_ANY:
        for (CatalogEntry value : getValuesAsList(property)) {
          entry = value;
          // Prefer values without language information
          if (!value.hasAttribute(XML_LANG_ATTR))
            break;
        }
        break;
      default:
        entry = getFirstLocalizedValue(property, language);
        break;
    }
    return entry;
  }

  @Override
  public String getAsText(EName property, String language, String delimiter) {
    RequireUtil.notNull(property, "property");
    RequireUtil.notNull(language, "language");
    RequireUtil.notNull(delimiter, "delimiter");
    final List<CatalogEntry> values;
    switch (language) {
      case LANGUAGE_UNDEFINED:
        values = getLocalizedValuesAsList(property, null);
        break;
      case LANGUAGE_ANY:
        values = getValuesAsList(property);
        break;
      default:
        values = getLocalizedValuesAsList(property, language);
        break;
    }
    return values.size() > 0 ? $(values).mkString(delimiter) : null;
  }

  @Override
  public Set<String> getLanguages(EName property) {
    RequireUtil.notNull(property, "property");
    Set<String> languages = new HashSet<String>();
    for (CatalogEntry entry : getValuesAsList(property)) {
      String language = entry.getAttribute(XML_LANG_ATTR);
      if (language != null)
        languages.add(language);
      else
        languages.add(LANGUAGE_UNDEFINED);
    }
    return languages;
  }

  @Override
  public boolean hasMultipleValues(EName property, String language) {
    RequireUtil.notNull(property, "property");
    RequireUtil.notNull(language, "language");
    return hasMultiplePropertyValues(property, language);
  }

  @Override
  public boolean hasMultipleValues(EName property) {
    RequireUtil.notNull(property, "property");
    return hasMultiplePropertyValues(property, LANGUAGE_ANY);
  }

  private boolean hasMultiplePropertyValues(EName property, String language) {
    if (LANGUAGE_ANY.equals(language)) {
      return getValuesAsList(property).size() > 1;
    } else {
      int counter = 0;
      for (CatalogEntry entry : getValuesAsList(property)) {
        if (equalLanguage(language, entry.getAttribute(XML_LANG_ATTR)))
          counter++;
        if (counter > 1)
          return true;
      }
      return false;
    }
  }

  @Override
  public boolean hasValue(EName property, String language) {
    RequireUtil.notNull(property, "property");
    RequireUtil.notNull(language, "language");
    return hasPropertyValue(property, language);
  }

  @Override
  public boolean hasValue(EName property) {
    RequireUtil.notNull(property, "property");
    return hasPropertyValue(property, LANGUAGE_ANY);
  }

  private boolean hasPropertyValue(EName property, final String language) {
    if (LANGUAGE_ANY.equals(language)) {
      return getValuesAsList(property).size() > 0;
    } else {
      return CollectionUtils.find(getValuesAsList(property), new Predicate() {
        @Override
        public boolean evaluate(Object o) {
          return equalLanguage(((CatalogEntry) o).getAttribute(XML_LANG_ATTR), language);
        }
      }) != null;
    }
  }

  @Override
  public void set(EName property, @Nullable String value, String language) {
    RequireUtil.notNull(property, "property");
    if (language == null || LANGUAGE_ANY.equals(language))
      throw new IllegalArgumentException("Language code may not be null or LANGUAGE_ANY");
    setValue(property, value, language, null);
  }

  @Override
  public void set(EName property, String value) {
    RequireUtil.notNull(property, "property");
    setValue(property, value, LANGUAGE_UNDEFINED, null);
  }

  @Override
  public void set(EName property, @Nullable DublinCoreValue value) {
    RequireUtil.notNull(property, "property");
    if (value != null) {
      setValue(property, value.getValue(), value.getLanguage(), value.getEncodingScheme().orNull());
    } else {
      removeValue(property, LANGUAGE_ANY);
    }
  }

  @Override
  public void set(EName property, List<DublinCoreValue> values) {
    RequireUtil.notNull(property, "property");
    RequireUtil.notNull(values, "values");
    removeValue(property, LANGUAGE_ANY);
    for (DublinCoreValue v : values) {
      add(property, v);
    }
  }

  private void setValue(EName property, @Nullable String value, String language, @Nullable EName encodingScheme) {
    if (value == null) {
      // No value, remove the whole element
      removeValue(property, language);
    } else {
      String lang = !LANGUAGE_UNDEFINED.equals(language) ? language : null;
      removeLocalizedValues(property, lang);
      add(property, value, language, encodingScheme);
    }
  }

  @Override
  public void add(EName property, String value) {
    RequireUtil.notNull(property, "property");
    RequireUtil.notNull(value, "value");

    add(property, value, LANGUAGE_UNDEFINED, null);
  }

  @Override
  public void add(EName property, String value, String language) {
    RequireUtil.notNull(property, "property");
    RequireUtil.notNull(value, "value");
    if (language == null || LANGUAGE_ANY.equals(language))
      throw new IllegalArgumentException("Language code may not be null or LANGUAGE_ANY");

    add(property, value, language, null);
  }

  @Override
  public void add(EName property, DublinCoreValue value) {
    RequireUtil.notNull(property, "property");
    RequireUtil.notNull(value, "value");

    add(property, value.getValue(), value.getLanguage(), value.getEncodingScheme().orNull());
  }

  void add(EName property, String value, String language, @Nullable EName encodingScheme) {
    if (LANGUAGE_UNDEFINED.equals(language)) {
      if (encodingScheme == null) {
        addElement(property, value);
      } else {
        addTypedElement(property, value, encodingScheme);
      }
    } else {
      // Language defined
      if (encodingScheme == null) {
        addLocalizedElement(property, value, language);
      } else {
        addTypedLocalizedElement(property, value, language, encodingScheme);
      }
    }
  }

  @Override
  public void remove(EName property, String language) {
    RequireUtil.notNull(property, "property");
    RequireUtil.notNull(language, "language");
    removeValue(property, language);
  }

  @Override
  public void remove(EName property) {
    RequireUtil.notNull(property, "property");
    removeValue(property, LANGUAGE_ANY);
  }

  private void removeValue(EName property, String language) {
    switch (language) {
      case LANGUAGE_ANY:
        removeElement(property);
        break;
      case LANGUAGE_UNDEFINED:
        removeLocalizedValues(property, null);
        break;
      default:
        removeLocalizedValues(property, language);
        break;
    }
  }

  @Override
  public void clear() {
    super.clear();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object clone() {
    DublinCoreCatalog clone = new DublinCoreCatalog();
    clone.setIdentifier(getIdentifier());
    clone.setFlavor(getFlavor());
    clone.setSize(getSize());
    clone.setChecksum(getChecksum());
    clone.bindings = bindings; // safe, since XmlNamespaceContext is immutable
    clone.rootTag = rootTag;
    for (Map.Entry<EName, List<CatalogEntry>> entry : data.entrySet()) {
      EName elmName = entry.getKey();
      EName elmNameCopy = new EName(elmName.getNamespaceURI(), elmName.getLocalName());
      List<CatalogEntry> elmsCopy = new ArrayList<CatalogEntry>();
      for (CatalogEntry catalogEntry : entry.getValue()) {
        elmsCopy.add(new CatalogEntry(catalogEntry.getEName(), catalogEntry.getValue(), catalogEntry.getAttributes()));
      }
      clone.data.put(elmNameCopy, elmsCopy);
    }
    return clone;
  }

  @Override
  public Set<EName> getProperties() {
    return new ImmutableSetWrapper<>(data.keySet());
  }

  boolean equalLanguage(String a, String b) {
    return (a == null && eq(b, LANGUAGE_UNDEFINED)) || (b == null && eq(a, LANGUAGE_UNDEFINED)) || eq(a, LANGUAGE_ANY)
            || eq(b, LANGUAGE_ANY) || (a != null && eq(a, b));
  }

  // make public
  @Override public EName toEName(String qName) {
    return super.toEName(qName);
  }

  // make public
  @Nonnull @Override public String toQName(EName eName) {
    return super.toQName(eName);
  }

  // make public
  @Override public void addElement(EName element, String value, Attributes attributes) {
    super.addElement(element, value, attributes);
  }

  // make public
  @Override public CatalogEntry[] getValues(EName element) {
    return super.getValues(element);
  }

  /**
   * Saves the dublin core metadata container to a dom.
   *
   * @throws ParserConfigurationException
   *           if the xml parser environment is not correctly configured
   * @throws TransformerException
   *           if serialization of the metadata document fails
   * @throws IOException
   *           if an error with catalog serialization occurs
   */
  @Override
  public Document toXml() throws ParserConfigurationException, TransformerException, IOException {
    return DublinCoreXmlFormat.writeDocument(this);
  }

  @Override
  public String toJson() throws IOException {
    return DublinCoreJsonFormat.writeJsonObject(this).toJSONString();
  }
}
