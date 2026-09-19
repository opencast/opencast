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

package org.opencastproject.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public final class XmlNamespaceContext implements NamespaceContext {
  // the number of default bindings
  private static final int DEFAULT_BINDINGS = 2;

  // prefix -> namespace URI
  private final Map<String, String> prefixToUri = new HashMap<String, String>();

  /**
   * Create a new namespace context with bindings from prefix to URI and bind the
   * default namespaces as described in the documentation of {@link javax.xml.namespace.NamespaceContext}.
   */
  public XmlNamespaceContext(Map<String, String> prefixToUri) {
    this.prefixToUri.putAll(prefixToUri);
    this.prefixToUri.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
    this.prefixToUri.put(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
  }

  public static XmlNamespaceContext mk(Map<String, String> prefixToUri) {
    return new XmlNamespaceContext(prefixToUri);
  }

  public static XmlNamespaceContext mk(XmlNamespaceBinding... bindings) {
    return mk(Arrays.asList(bindings));
  }

  public static XmlNamespaceContext mk(String prefix, String namespaceUri) {
    return new XmlNamespaceContext(Collections.singletonMap(prefix, namespaceUri));
  }

  public static XmlNamespaceContext mk(List<XmlNamespaceBinding> bindings) {
    Map<String, String> prefixToUri = new HashMap<>();
    for (XmlNamespaceBinding binding : bindings) {
      prefixToUri.put(binding.getPrefix(), binding.getNamespaceURI());
    }
    return new XmlNamespaceContext(prefixToUri);
  }


  @Override
  public String getNamespaceURI(String prefix) {
    return Optional.ofNullable(prefixToUri.get(prefix)).orElse(XMLConstants.NULL_NS_URI);
  }

  @Override
  public String getPrefix(String uri) {
    RequireUtil.notNull(uri, "uri");
    for (Map.Entry<String, String> entry : prefixToUri.entrySet()) {
      if (uri.equals(entry.getValue())) {
        return entry.getKey();
      }
    }
    return null;
  }

  @Override
  public Iterator<String> getPrefixes(String uri) {
    RequireUtil.notNull(uri, "uri");
    List<String> matchingPrefixes = new ArrayList<>();
    for (Map.Entry<String, String> entry : prefixToUri.entrySet()) {
      if (uri.equals(entry.getValue())) {
        matchingPrefixes.add(entry.getKey());
      }
    }
    return matchingPrefixes.iterator();
  }


  public List<XmlNamespaceBinding> getBindings() {
    List<XmlNamespaceBinding> bindings = new ArrayList<>();
    for (Map.Entry<String, String> entry : prefixToUri.entrySet()) {
      bindings.add(new XmlNamespaceBinding(entry.getKey(), entry.getValue()));
    }
    return bindings;
  }

  /** Create a new context with the given bindings added. Existing bindings will not be overwritten. */
  public XmlNamespaceContext add(XmlNamespaceBinding... bindings) {
    return add(Arrays.asList(bindings));
  }

  /** Create a new context with the given bindings added. Existing bindings will not be overwritten. */
  public XmlNamespaceContext add(XmlNamespaceContext binding) {
    if (binding.prefixToUri.size() == DEFAULT_BINDINGS) {
      return this;
    } else {
      return add(binding.getBindings());
    }
  }

  private XmlNamespaceContext add(List<XmlNamespaceBinding> newBindings) {
    Map<String, String> existingMap = new HashMap<>();
    for (XmlNamespaceBinding b : getBindings()) {
      existingMap.put(b.getPrefix(), b.getNamespaceURI());
    }
    for (XmlNamespaceBinding b : newBindings) {
      if (!existingMap.containsKey(b.getPrefix())) {
        existingMap.put(b.getPrefix(), b.getNamespaceURI());
      }
    }
    return mk(existingMap);
  }

  public NamespaceContext merge(final NamespaceContext precedence) {
    return merge(this, precedence);
  }

  /** Merge <code>b</code> into <code>a</code> so that <code>b</code> takes precedence over <code>a</code>. */
  public static NamespaceContext merge(final NamespaceContext a, final NamespaceContext b) {
    return new NamespaceContext() {
      @Override public String getNamespaceURI(String prefix) {
        final String uri = b.getNamespaceURI(prefix);
        if (Objects.equals(XMLConstants.DEFAULT_NS_PREFIX, prefix) && Objects.equals(XMLConstants.NULL_NS_URI, uri)) {
          return a.getNamespaceURI(prefix);
        } else {
          return uri;
        }
      }

      @Override public String getPrefix(final String uri) {
        return Optional.ofNullable(b.getPrefix(uri))
            .orElseGet(() -> a.getPrefix(uri));
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
