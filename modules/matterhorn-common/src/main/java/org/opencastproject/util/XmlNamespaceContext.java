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

import static com.entwinemedia.fn.Stream.$;
import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.util.data.Function0;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Booleans;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

@Immutable
@ParametersAreNonnullByDefault
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
    return mk($(bindings));
  }

  public static XmlNamespaceContext mk(String prefix, String namespaceUri) {
    return new XmlNamespaceContext(Collections.singletonMap(prefix, namespaceUri));
  }

  public static XmlNamespaceContext mk(List<XmlNamespaceBinding> bindings) {
    return mk($(bindings));
  }

  public static XmlNamespaceContext mk(Stream<XmlNamespaceBinding> bindings) {
    return new XmlNamespaceContext(
            bindings.foldl(
                    new HashMap<String, String>(),
                    new Fn2<HashMap<String, String>, XmlNamespaceBinding, HashMap<String, String>>() {
                      @Override
                      public HashMap<String, String> ap(
                              HashMap<String, String> prefixToUri, XmlNamespaceBinding binding) {
                        prefixToUri.put(binding.getPrefix(), binding.getNamespaceURI());
                        return prefixToUri;
                      }
                    }));
  }

  @Override @Nonnull
  public String getNamespaceURI(String prefix) {
    return Opt.nul(prefixToUri.get(prefix)).or(XMLConstants.NULL_NS_URI);
  }

  @Override @Nullable
  public String getPrefix(String uri) {
    return $(prefixToUri.entrySet()).find(Booleans.eq(RequireUtil.notNull(uri, "uri")).o(value)).map(key).orNull();
  }

  @Override
  public Iterator getPrefixes(String uri) {
    return $(prefixToUri.entrySet()).filter(Booleans.eq(uri).o(value)).map(key).iterator();
  }

  public List<XmlNamespaceBinding> getBindings() {
    return $(prefixToUri.entrySet()).map(toBinding).toList();
  }

  /** Create a new context with the given bindings added. Existing bindings will not be overwritten. */
  public XmlNamespaceContext add(XmlNamespaceBinding... bindings) {
    return add($(bindings));
  }

  /** Create a new context with the given bindings added. Existing bindings will not be overwritten. */
  public XmlNamespaceContext add(XmlNamespaceContext bindings) {
    if (bindings.prefixToUri.size() == DEFAULT_BINDINGS) {
      // bindings contains only the default bindings
      return this;
    } else {
      return add($(bindings.getBindings()));
    }
  }

  private XmlNamespaceContext add(Stream<XmlNamespaceBinding> bindings) {
    return mk(bindings.append(getBindings()));
  }

  private static final Fn<Entry<String, String>, String> key = new Fn<Entry<String, String>, String>() {
    @Override public String ap(Entry<String, String> e) {
      return e.getKey();
    }
  };

  private static final Fn<Entry<String, String>, String> value = new Fn<Entry<String, String>, String>() {
    @Override public String ap(Entry<String, String> e) {
      return e.getValue();
    }
  };

  private static final Fn<Entry<String, String>, XmlNamespaceBinding> toBinding = new Fn<Entry<String, String>, XmlNamespaceBinding>() {
    @Override public XmlNamespaceBinding ap(Entry<String, String> e) {
      return new XmlNamespaceBinding(e.getKey(), e.getValue());
    }
  };

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
