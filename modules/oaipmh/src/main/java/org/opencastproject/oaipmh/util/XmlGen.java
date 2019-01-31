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
package org.opencastproject.oaipmh.util;

import static com.entwinemedia.fn.Stream.$;
import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.Fn;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * DOM based XML generation environment. Implement {@link #create()} to create the XML. Serialize to an output stream
 * with {@link #generate(java.io.OutputStream)}.
 *
 * todo document the node creator functions
 */
public abstract class XmlGen {
  private final Document document;
  private final Option<String> defaultNamespace;

  /**
   * Create a new environment.
   */
  public XmlGen(Option<String> defaultNamespace) {
    document = createDocument();
    this.defaultNamespace = defaultNamespace;
  }

  private Document createDocument() {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.newDocument();
    } catch (ParserConfigurationException e) {
      return chuck(e);
    }
  }

  private void write(OutputStream out) {
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(out);
      transformer.transform(source, result);
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Generate the XML and write it to <code>out</code>.
   */
  public void generate(OutputStream out) {
    generate();
    write(out);
  }

  /**
   * Generate the document.
   */
  public Document generate() {
    final Node node = document.importNode(create(), true);
    final Element docElem = document.getDocumentElement();
    if (docElem != null) {
      document.removeChild(docElem);
    }
    document.appendChild(node);
    return document;
  }

  /** Generate the document as a string. */
  public String generateAsString() {
    return withResource(new ByteArrayOutputStream(), new Function<ByteArrayOutputStream, String>() {
      @Override public String apply(ByteArrayOutputStream out) {
        generate(out);
        return out.toString();
      }
    });
  }

  /**
   * Implement this method to create the DOM. Use the various node creation functions for this purpose.
   */
  public abstract Element create();

  // --

  protected Namespace ns(String prefix, String namespace) {
    return new Namespace(prefix, namespace);
  }

  protected Node schemaLocation(String location) {
    return $a("xsi:schemaLocation", location);
  }

  // CHECKSTYLE:OFF

  protected Node $langNode(String language) {
    if (StringUtils.isBlank(language) || DublinCore.LANGUAGE_UNDEFINED.equals(language)
            || DublinCore.LANGUAGE_ANY.equals(language))
      return nodeZero();

    Attr a = document.createAttributeNS(XMLConstants.XML_NS_URI, "xml:lang");
    a.setValue(language);
    return a;
  }

  protected Node $a(String name, String value) {
    Attr a = document.createAttribute(name);
    a.setValue(value);
    return a;
  }

  protected Node $aBlank(String name, String value) {
    if (StringUtils.isNotBlank(value)) {
      Attr a = document.createAttribute(name);
      a.setValue(value);
      return a;
    } else {
      return nodeZero();
    }
  }

  protected Node $aSome(final String name, final Option<String> value) {
    return value.fold(new Option.Match<String, Node>() {
      @Override
      public Node some(String value) {
        Attr a = document.createAttribute(name);
        a.setValue(value);
        return a;
      }

      @Override
      public Node none() {
        return nodeZero();
      }
    });
  }

  protected Element $e(String qname, Option<String> namespace, List<Node> nodes) {
    return appendTo(createElemNs(namespace, qname), nodes);
  }

  /**
   * Create an element with the qualified name <code>qname</code> -- i.e. <code>prefix:tagname</code> -- in the
   * namespace <code>namespace</code> with children <code>nodes</code>.
   */
  protected Element $e(String qname, Option<String> namespace, NodeList nodes) {
    return appendTo(createElemNs(namespace, qname), nodes);
  }

  protected Element $e(String qname, Option<String> namespace, Node... nodes) {
    return $e(qname, namespace, Arrays.asList(nodes));
  }

  protected Element $e(String name, Node... nodes) {
    return $e(name, defaultNamespace, Arrays.asList(nodes));
  }

  protected Element $e(String name, List<Node> nodes) {
    return $e(name, defaultNamespace, Collections.unmodifiableList(nodes));
  }

  /**
   * Create an element with the qualified name <code>qname</code> -- i.e. <code>prefix:tagname</code> -- in the
   * namespace <code>namespace</code> with children <code>nodes</code>.
   */
  protected Element $e(String qname, String namespace, Node... nodes) {
    return $e(qname, some(namespace), Arrays.asList(nodes));
  }

  protected Element $e(String qname, String namespace, List<Node> nodes) {
    return $e(qname, some(namespace), nodes);
  }

  protected Node $eTxtBlank(final String name, String text) {
    return $txtBlank(text).map(new Function<Node, Node>() {
      @Override
      public Node apply(Node text) {
        final Element e = createElemDefaultNs(name);
        e.appendChild(text);
        return e;
      }
    }).getOrElse(nodeZero);
  }

  protected Node $eTxt(final String name, String text) {
    final Element e = createElemDefaultNs(name);
    e.appendChild($txt(text));
    return e;
  }

  protected Node $eTxt(final String qname, final String namespace, String text) {
    final Element e = createElemNs(namespace, qname);
    e.appendChild($txt(text));
    return e;
  }

  protected Element $e(String name, List<Namespace> namespaces, Node... nodes) {
    return appendTo(appendNs(createElemDefaultNs(name), namespaces), Arrays.asList(nodes));
  }

  protected Element $e(String name, List<Namespace> namespaces, NodeList nodes) {
    return appendTo(appendNs(createElemDefaultNs(name), namespaces), nodes);
  }

  protected Element $e(String name, List<Namespace> namespaces, List<Node> nodes) {
    return appendTo(appendNs(createElemDefaultNs(name), namespaces), nodes);
  }

  protected Element $e(String qname, String namespace, List<Namespace> namespaces, Node... nodes) {
    return appendTo(appendNs(createElemNs(namespace, qname), namespaces), Arrays.asList(nodes));
  }

  private Element createElemDefaultNs(String name) {
    return createElemNs(defaultNamespace, name);
  }

  private Element createElemNs(Option<String> namespace, String qname) {
    return createElemNs(namespace.getOrElseNull(), qname);
  }

  /**
   * @param namespace
   *         may be null.
   */
  private Element createElemNs(String namespace, String qname) {
    return document.createElementNS(namespace, qname);
  }

  /**
   * Create a new DOM element.
   *
   * @param qname
   *         fully qualified tag name, e.g. "name" or "dc:title"
   * @param namespace
   *         namespace to which this tag belongs to
   * @param namespaces
   *         additional namespace declarations
   * @param nodes
   *         child nodes
   */
  protected Element $e(String qname, String namespace, List<Namespace> namespaces, List<Node> nodes) {
    return appendTo(appendNs(createElemNs(namespace, qname), namespaces), nodes);
  }

  /**
   * Conditional element. Only created if at least one subnode is present. Subnodes may be attributes, elements, text
   * nodes, etc.
   */
  protected Node $e(String name, Option<Node>... nodes) {
    final List<Node> existing = filter(Arrays.asList(nodes));
    if (!existing.isEmpty()) {
      return $e(name, existing);
    } else {
      return nodeZero();
    }
  }

  protected Node $txt(String text) {
    return document.createTextNode(text);
  }

  protected Node $cdata(String text) {
    return document.createCDATASection(text);
  }

  /**
   * Text blank.
   */
  protected Option<Node> $txtBlank(String text) {
    return StringUtils.isNotBlank(text) ? some($txt(text)) : Option.<Node>none();
  }

  // --

  // CHECKSTYLE:ON

  private List<Node> filter(List<Option<Node>> nodes) {
    return $(nodes).bind(new Fn<Option<Node>, Collection<Node>>() {
      @Override
      public Collection<Node> apply(Option<Node> nodeOption) {
        return nodeOption.fold(new Option.Match<Node, Collection<Node>>() {
          @Override
          public Collection<Node> some(Node node) {
            return Collections.singletonList(node);
          }

          @Override
          public Collection<Node> none() {
            return Collections.emptyList();
          }
        });
      }
    }).toList();
  }

  private Element appendNs(Element e, List<Namespace> namespaces) {
    for (Namespace n : namespaces) {
      e.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + n.getPrefix(),
                       n.getNamespace());
    }
    return e;
  }

  /**
   * Append <code>nodes</code> to element <code>e</code>. Respects different node types like attributes and elements.
   */
  private Element appendTo(Element e, List<Node> nodes) {
    for (Node node : nodes)
      appendTo(e, node);
    return e;
  }

  /**
   * Like {@link #appendTo(org.w3c.dom.Element, java.util.List)} but with a different signature.
   */
  private Element appendTo(Element e, NodeList nodes) {
    for (int i = 0; i < nodes.getLength(); i++)
      appendTo(e, nodes.item(i));
    return e;
  }

  /**
   * Append node <code>n</code> to element <code>e</code> respecting different node types like attributes and elements.
   */
  private void appendTo(Element e, Node n) {
    Node toAppend = ObjectUtils.equals(n.getOwnerDocument(), document) ? n : document.importNode(n, true);
    if (toAppend instanceof Attr) {
      e.setAttributeNode((Attr) toAppend);
    } else {
      e.appendChild(toAppend);
    }
  }

  /**
   * The neutral element.
   */
  protected Node nodeZero() {
    return document.createTextNode("");
  }

  /**
   * Lazy version of {@link #nodeZero()}.
   */
  protected Function0<Node> nodeZero = new Function0<Node>() {
    @Override
    public Node apply() {
      return nodeZero();
    }
  };

  /**
   * Create a text node from a string.
   */
  protected Function<String, Node> mkText = new Function<String, Node>() {
    @Override
    public Node apply(String token) {
      return $txt(token);
    }
  };

  protected class Namespace {
    private final String prefix;
    private final String namespace;

    Namespace(String prefix, String namespace) {
      this.prefix = prefix;
      this.namespace = namespace;
    }

    public String getPrefix() {
      return prefix;
    }

    public String getNamespace() {
      return namespace;
    }
  }
}
