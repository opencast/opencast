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
package org.opencastproject.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Tuple;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Tuple.tuple;

public class XmlNamespaceContext implements NamespaceContext {
  // prefix -> namespace URI
  private final Map<String, String> prefixToUri;
  private final Multimap<String, String> uriToPrefix;

  public XmlNamespaceContext(Map<String, String> prefixToUri) {
    this.prefixToUri = new HashMap<String, String>();
    this.prefixToUri.put(XMLConstants.DEFAULT_NS_PREFIX, XMLConstants.NULL_NS_URI);
    this.prefixToUri.putAll(prefixToUri);
    this.prefixToUri.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
    this.prefixToUri.put(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);

    this.uriToPrefix = Collections.makeMap(
            ArrayListMultimap.<String, String>create(),
            this.prefixToUri.entrySet(),
            new Function<Map.Entry<String, String>, Tuple<String, String>>() {
              @Override public Tuple<String, String> apply(Map.Entry<String, String> entry) {
                return tuple(entry.getValue(), entry.getKey());
              }
            });
  }

  @Override public String getNamespaceURI(String prefix) {
    return option(prefixToUri.get(prefix)).getOrElse(XMLConstants.NULL_NS_URI);
  }

  @Override public String getPrefix(String uri) {
    return mlist(uriToPrefix.get(uri)).headOpt().getOrElseNull();
  }

  @Override public Iterator getPrefixes(String uri) {
    return java.util.Collections.unmodifiableCollection(uriToPrefix.get(uri)).iterator();
  }

  public NamespaceContext merge(final NamespaceContext precedence) {
    return merge(this, precedence);
  }

  /** Merge <code>b</code> into <code>a</code> so that <code>b</code> takes precedence over <code>a</code>. */
  public static NamespaceContext merge(final NamespaceContext a, final NamespaceContext b) {
    return new NamespaceContext() {
      @Override public String getNamespaceURI(String prefix) {
        final String uri = b.getNamespaceURI(prefix);
        if (eq(XMLConstants.DEFAULT_NS_PREFIX, prefix) && eq(XMLConstants.NULL_NS_URI, uri)) {
          return a.getNamespaceURI(prefix);
        } else {
          return uri;
        }
      }

      @Override public String getPrefix(final String uri) {
        return option(b.getPrefix(uri)).getOrElse(new Function0<String>() {
          @Override public String apply() {
            return a.getPrefix(uri);
          }
        });
      }

      @Override public Iterator getPrefixes(String uri) {
        final Iterator prefixes = b.getPrefixes(uri);
        if (prefixes.hasNext()) {
          return prefixes;
        } else {
          return a.getPrefixes(uri);
        }
      }
    };
  }
}
