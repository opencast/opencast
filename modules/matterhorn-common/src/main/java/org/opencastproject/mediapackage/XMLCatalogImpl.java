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

package org.opencastproject.mediapackage;

import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
 * This is a basic implementation for handling simple catalogs of metadata. It provides utility methods to store
 * key-value data.
 * <p/>
 * For a definition of the terms <def>expanded name</def>, <def>qualified name</def> or <def>QName</def>, <def>namespace
 * prefix</def>, <def>local part</def> and <def>local name</def>, please see <a
 * href="http://www.w3.org/TR/REC-xml-names">http://www.w3.org/TR/REC-xml-names</a>
 * <p/>
 * By default the following namespace prefixes are bound:
 * <ul>
 * <li>xml - http://www.w3.org/XML/1998/namespace
 * <li>xmlns - http://www.w3.org/2000/xmlns/
 * <li>xsi - http://www.w3.org/2001/XMLSchema-instance
 * </ul>
 * <p/>
 * <h3>Limitations</h3>
 * XMLCatalog supports only <em>one</em> prefix binding per namespace name, so you cannot create documents like the
 * following using XMLCatalog:
 *
 * <pre>
 * &lt;root xmlns:x=&quot;http://x.demo.org&quot; xmlns:y=&quot;http://x.demo.org&quot;&gt;
 *   &lt;x:elem&gt;value&lt;/x:elem&gt;
 *   &lt;y:elem&gt;value&lt;/y:elem&gt;
 * &lt;/root&gt;
 * </pre>
 *
 * However, reading of those documents is supported.
 */
public abstract class XMLCatalogImpl extends CatalogImpl implements XMLCatalog {

  /** Serial version UID */
  private static final long serialVersionUID = -908525367616L;

  protected static final int PREFIX = 0;
  protected static final int LOCAL_NAME = 1;

  /** Expanded name of the XML language attribute <code>xml:lang</code>. */
  protected static final EName XML_LANG_ATTR = new EName(XMLConstants.XML_NS_URI, "lang");

  /** Namespace prefix for XML schema instance. */
  protected static final String XSI_NS_PREFIX = "xsi";

  /** Namespace name for XML schema instance. */
  protected static final String XSI_NS_URI = "http://www.w3.org/2001/XMLSchema-instance";

  /**
   * Expanded name of the XSI type attribute.
   * <p/>
   * See <a href="http://www.w3.org/TR/xmlschema-1/#xsi_type">http://www.w3.org/TR/xmlschema-1/#xsi_type</a> for the
   * definition.
   */
  protected static final EName XSI_TYPE_ATTR = new EName(XSI_NS_URI, "type");

  /** Key (QName) value meta data */
  protected Map<EName, List<CatalogEntry>> data = new HashMap<EName, List<CatalogEntry>>();

  /** Namespace - prefix bindings */
  protected Bindings bindings = new Bindings(false);

  /** Needed by JAXB */
  protected XMLCatalogImpl() {
    super();
  }

  /**
   * Creates an abstract metadata container.
   *
   * @param id
   *          the element identifier withing the package
   * @param flavor
   *          the catalog flavor
   * @param uri
   *          the document location
   * @param size
   *          the catalog size in bytes
   * @param checksum
   *          the catalog checksum
   * @param mimeType
   *          the catalog mime type
   */
  protected XMLCatalogImpl(String id, MediaPackageElementFlavor flavor, URI uri, long size, Checksum checksum,
          MimeType mimeType) {
    super(id, flavor, uri, size, checksum, mimeType);
    bindPrefix(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
    bindPrefix(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
    bindPrefix(XSI_NS_PREFIX, XSI_NS_URI);
  }

  /**
   * Creates an abstract metadata container.
   *
   * @param flavor
   *          the catalog flavor
   * @param uri
   *          the document location
   * @param size
   *          the catalog size in bytes
   * @param checksum
   *          the catalog checksum
   * @param mimeType
   *          the catalog mime type
   */
  protected XMLCatalogImpl(MediaPackageElementFlavor flavor, URI uri, long size, Checksum checksum, MimeType mimeType) {
    this(null, flavor, uri, size, checksum, mimeType);
  }

  /**
   * Clears the catalog.
   */
  protected void clear() {
    data.clear();
  }

  /**
   * Bind a prefix to a namespace.
   *
   * @param prefix
   *          the prefix
   * @param namespaceName
   *          the namespace
   */
  protected void bindPrefix(String prefix, String namespaceName) {
    bindings.bindPrefix(prefix, namespaceName);
  }

  /**
   * Adds the element to the metadata collection.
   *
   * @param element
   *          the expanded name of the element
   * @param value
   *          the value
   */
  protected void addElement(EName element, String value) {
    if (element == null)
      throw new IllegalArgumentException("Expanded name must not be null");

    addElement(new CatalogEntry(element, value));
  }

  /**
   * Adds the element with the <code>xml:lang</code> attribute to the metadata collection.
   *
   * @param element
   *          the expanded name of the element
   * @param value
   *          the value
   * @param language
   *          the language identifier (two letter ISO 639)
   */
  protected void addLocalizedElement(EName element, String value, String language) {
    if (element == null)
      throw new IllegalArgumentException("Expanded name must not be null");
    if (language == null)
      throw new IllegalArgumentException("Language must not be null");

    Map<EName, String> attributes = new HashMap<EName, String>(1);
    attributes.put(XML_LANG_ATTR, language);
    addElement(new CatalogEntry(element, value, attributes));
  }

  /**
   * Adds the element with the <code>xsi:type</code> attribute to the metadata collection.
   *
   * @param value
   *          the value
   * @param type
   *          the element type
   */
  protected void addTypedElement(EName element, String value, EName type) {
    if (element == null)
      throw new IllegalArgumentException("EName name must not be null");
    if (type == null)
      throw new IllegalArgumentException("Type must not be null");

    Map<EName, String> attributes = new HashMap<EName, String>(1);
    attributes.put(XSI_TYPE_ATTR, toQName(type));
    addElement(new CatalogEntry(element, value, attributes));
  }

  /**
   * Adds an element with the <code>xml:lang</code> and <code>xsi:type</code> attributes to the metadata collection.
   *
   * @param element
   *          the expanded name of the element
   * @param value
   *          the value
   * @param language
   *          the language identifier (two letter ISO 639)
   * @param type
   *          the element type
   */
  protected void addTypedLocalizedElement(EName element, String value, String language, EName type) {
    if (element == null)
      throw new IllegalArgumentException("EName name must not be null");
    if (type == null)
      throw new IllegalArgumentException("Type must not be null");
    if (language == null)
      throw new IllegalArgumentException("Language must not be null");

    Map<EName, String> attributes = new HashMap<EName, String>(2);
    attributes.put(XML_LANG_ATTR, language);
    attributes.put(XSI_TYPE_ATTR, toQName(type));
    addElement(new CatalogEntry(element, value, attributes));
  }

  /**
   * Adds an element with attributes to the catalog.
   *
   * @param element
   *          the expanded name of the element
   * @param value
   *          the element's value
   * @param attributes
   *          the attributes. May be null
   */
  protected void addElement(EName element, String value, Attributes attributes) {
    if (element == null)
      throw new IllegalArgumentException("Expanded name must not be null");

    Map<EName, String> attributeMap = new HashMap<EName, String>();
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        attributeMap.put(new EName(attributes.getURI(i), attributes.getLocalName(i)), attributes.getValue(i));
      }
    }
    addElement(new CatalogEntry(element, value, attributeMap));
  }

  /**
   * Adds the catalog element to the list of elements.
   *
   * @param element
   *          the element
   */
  private void addElement(CatalogEntry element) {
    if (element == null || StringUtils.trimToNull(element.getValue()) == null)
      return;
    List<CatalogEntry> values = data.get(element.getEName());
    if (values == null) {
      values = new ArrayList<CatalogEntry>();
      data.put(element.getEName(), values);
    }
    values.add(element);
  }

  /**
   * Completely removes an element.
   *
   * @param element
   *          the expanded name of the element
   */
  protected void removeElement(EName element) {
    removeValues(element, null, true);
  }

  /**
   * Removes all entries in a certain language from an element.
   *
   * @param element
   *          the expanded name of the element
   * @param language
   *          the language code (two letter ISO 639) or null to <em>only</em> remove entries without an
   *          <code>xml:lang</code> attribute
   */
  protected void removeLocalizedValues(EName element, String language) {
    removeValues(element, language, false);
  }

  /**
   * Removes values from an element or the complete element from the catalog.
   *
   * @param element
   *          the expanded name of the element
   * @param language
   *          the language code (two letter ISO 639) to remove or null to remove entries without language code
   * @param all
   *          true - remove all entries for that element. This parameter overrides the language parameter.
   */
  private void removeValues(EName element, String language, boolean all) {
    if (all) {
      data.remove(element);
    } else {
      List<CatalogEntry> entries = data.get(element);
      if (entries != null) {
        for (Iterator<CatalogEntry> i = entries.iterator(); i.hasNext();) {
          CatalogEntry entry = i.next();
          if (equal(language, entry.getAttribute(XML_LANG_ATTR))) {
            i.remove();
          }
        }
      }
    }
  }

  /**
   * Returns the values that are associated with the specified key.
   *
   * @param element
   *          the expanded name of the element
   * @return the elements
   */
  protected CatalogEntry[] getValues(EName element) {
    List<CatalogEntry> values = data.get(element);
    if (values != null && values.size() > 0) {
      return values.toArray(new CatalogEntry[values.size()]);
    }
    return new CatalogEntry[] {};
  }

  /**
   * Returns the values that are associated with the specified key.
   *
   * @param element
   *          the expanded name of the element
   * @return all values of the element or an empty list if this element does not exist or does not have any values
   */
  @SuppressWarnings("unchecked")
  protected List<CatalogEntry> getValuesAsList(EName element) {
    List<CatalogEntry> values = data.get(element);
    return values != null ? values : Collections.EMPTY_LIST;
  }

  /**
   * Returns the values that are associated with the specified key.
   *
   * @param element
   *          the expandend name of the element
   * @param language
   *          a language code or null to get values without <code>xml:lang</code> attribute
   * @return all values of the element
   */
  @SuppressWarnings("unchecked")
  protected List<CatalogEntry> getLocalizedValuesAsList(EName element, String language) {
    List<CatalogEntry> values = data.get(element);

    if (values != null) {
      List<CatalogEntry> filtered = new ArrayList<CatalogEntry>();
      for (CatalogEntry value : values) {
        if (equal(language, value.getAttribute(XML_LANG_ATTR))) {
          filtered.add(value);
        }
      }
      return filtered;
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  /**
   * Returns the first value that is associated with the specified name.
   *
   * @param element
   *          the expanded name of the element
   * @return the first value
   */
  protected CatalogEntry getFirstValue(EName element) {
    List<CatalogEntry> elements = data.get(element);
    if (elements != null && elements.size() > 0) {
      return elements.get(0);
    }
    return null;
  }

  /**
   * Returns the first element that is associated with the specified name and attribute.
   *
   * @param element
   *          the expanded name of the element
   * @param attributeEName
   *          the expanded attribute name
   * @param attributeValue
   *          the attribute value
   * @return the first value
   */
  protected CatalogEntry getFirstValue(EName element, EName attributeEName, String attributeValue) {
    List<CatalogEntry> elements = data.get(element);
    if (elements != null) {
      for (CatalogEntry entry : elements) {
        String v = entry.getAttribute(attributeEName);
        if (equal(attributeValue, v))
          return entry;
      }
    }
    return null;
  }

  /**
   * Returns the first value that is associated with the specified name and language.
   *
   * @param element
   *          the expanded name of the element
   * @param language
   *          the language identifier or null to get only elements without <code>xml:lang</code> attribute
   * @return the first value
   */
  protected CatalogEntry getFirstLocalizedValue(EName element, String language) {
    return getFirstValue(element, XML_LANG_ATTR, language);
  }

  /**
   * Returns the first value that is associated with the specified name and language.
   *
   * @param element
   *          the expanded name of the element
   * @param type
   *          the <code>xsi:type</code> value
   * @return the element
   */
  protected CatalogEntry getFirstTypedValue(EName element, String type) {
    return getFirstValue(element, XSI_TYPE_ATTR, type);
  }

  /**
   * Tests two objects for equality.
   */
  protected boolean equal(Object a, Object b) {
    return (a == null && b == null) || (a != null && a.equals(b));
  }

  /**
   * Creates an xml document root and returns it.
   *
   * @return the document
   * @throws ParserConfigurationException
   *           If the xml parser environment is not correctly configured
   */
  protected Document newDocument() throws ParserConfigurationException {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setNamespaceAware(true);
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.newDocument();
    return doc;
  }

  /**
   * Serializes the given xml document to the associated file. Please note that this method does <em>not</em> close the
   * output stream. Anyone using this method is responsible for doing it by itself.
   *
   * @param document
   *          the document
   * @param docType
   *          the document type definition (dtd)
   * @throws TransformerException
   *           if serialization fails
   */
  protected void saveToXml(Node document, String docType, OutputStream out) throws TransformerException, IOException {
    StreamResult streamResult = new StreamResult(out);
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer serializer = tf.newTransformer();
    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    if (docType != null)
      serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType);
    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
    serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    serializer.transform(new DOMSource(document), streamResult);
    out.flush();
  }

  /**
   * @see org.opencastproject.mediapackage.AbstractMediaPackageElement#toManifest(org.w3c.dom.Document,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  @Override
  public Node toManifest(Document document, MediaPackageSerializer serializer) {
    Node node = super.toManifest(document, serializer);
    return node;
  }

  /**
   * Transform an expanded name to a qualified name based on the registered binding.
   *
   * @param eName
   *          the expanded name to transform
   * @return the qualified name, e.g. <code>dcterms:title</code>
   * @throws NamespaceBindingException
   *           if the namespace name is not bound to a prefix
   */
  protected String toQName(EName eName) {
    if (!eName.hasNamespace()) {
      return eName.getLocalName();
    }
    String prefix = bindings.lookupPrefix(eName.getNamespaceName());
    return toQName(prefix, eName.getLocalName());
  }

  /**
   * Transform an qualified name consisting of prefix and local part to an expanded name, based on the registered
   * binding.
   *
   * @param prefix
   *          the prefix
   * @param localName
   *          the local part
   * @return the expanded name
   * @throws NamespaceBindingException
   *           if the namespace name is not bound to a prefix
   */
  protected EName toEName(String prefix, String localName) {
    String namespaceName = bindings.lookupNamespace(prefix);
    return new EName(namespaceName, localName);
  }

  /**
   * Transform an qualified name to an expanded name, based on the registered binding.
   *
   * @param qName
   *          the qualified name, e.g. <code>dcterms:title</code> or <code>title</code>
   * @return the expanded name
   * @throws NamespaceBindingException
   *           if the namespace name is not bound to a prefix
   */
  protected EName toEName(String qName) {
    String[] parts = splitQName(qName);
    return new EName(bindings.lookupNamespace(parts[0]), parts[1]);
  }

  /**
   * Splits a QName into its parts.
   *
   * @param qName
   *          the qname to split
   * @return an array of prefix (0) and local part (1). The prefix is "" if the qname belongs to the default namespace.
   */
  private String[] splitQName(String qName) {
    String[] parts = qName.split(":", 3);
    if (parts.length > 2)
      throw new IllegalArgumentException("Local name must not contain ':'");
    if (parts.length == 2)
      return parts;
    return new String[] { XMLConstants.DEFAULT_NS_PREFIX, parts[0] };
  }

  /**
   * Returns a "prefixed name" consisting of namespace prefix and local name.
   *
   * @param prefix
   *          the namespace prefix, may be <code>null</code>
   * @param localName
   *          the local name
   * @return the "prefixed name" <code>prefix:localName</code>
   */
  private String toQName(String prefix, String localName) {
    StringBuilder b = new StringBuilder();
    if (prefix != null && !XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
      b.append(prefix);
      b.append(":");
    }
    b.append(localName);
    return b.toString();
  }

  // --------------------------------------------------------------------------------------------

  /**
   * Element representation.
   */
  protected class CatalogEntry implements XmlElement, Comparable<CatalogEntry>, Serializable {

    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 793064320233482150L;

    private EName name;

    private String value = null;

    /**
     * The attributes of this element
     */
    private Map<EName, String> attributes = null;

    /**
     * Creates a new catalog element representation.
     *
     * @param value
     *          the element value
     */
    public CatalogEntry(EName name, String value) {
      this(name, value, null);
    }

    /**
     * Creates a new catalog element representation with name, value and attributes.
     *
     * @param value
     *          the element value
     * @param attributes
     *          the element attributes
     */
    public CatalogEntry(EName name, String value, Map<EName, String> attributes) {
      this.name = name;
      this.value = value;
      this.attributes = attributes;
    }

    /**
     * Returns the element namespace.
     *
     * @return the namespace
     */
    public String lookupPrefix() {
      return bindings.lookupPrefix(name.getNamespaceName());
    }

    /**
     * Returns the qualified name of the entry as a string. The namespace of the entry has to be bound to a prefix for
     * this method to succeed.
     */
    public String getQName() {
      return toQName(name);
    }

    /**
     * Returns the expanded name of the entry.
     */
    public EName getEName() {
      return name;
    }

    /**
     * Returns the element value.
     *
     * @return the value
     */
    public String getValue() {
      return value;
    }

    /**
     * Returns <code>true</code> if the element contains attributes.
     *
     * @return <code>true</code> if the element contains attributes
     */
    public boolean hasAttributes() {
      return attributes != null && attributes.size() > 0;
    }

    /**
     * Returns the element's attributes.
     *
     * @return the attributes
     */
    public Map<EName, String> getAttributes() {
      return attributes;
    }

    /**
     * Returns <code>true</code> if the element contains an attribute with the given name.
     *
     * @return <code>true</code> if the element contains the attribute
     */
    public boolean hasAttribute(EName name) {
      return attributes != null && attributes.containsKey(name);
    }

    /**
     * Returns the attribute value for the given attribute.
     *
     * @return the attribute or null
     */
    public String getAttribute(EName name) {
      if (attributes == null)
        return null;
      return attributes.get(name);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return name.hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CatalogEntry) {
        CatalogEntry entry = (CatalogEntry) obj;
        boolean equal = name.equals(entry.name);
        equal &= value.equals(entry.value);
        equal &= (attributes == null && entry.attributes == null) || attributes.equals(entry.attributes);
        return equal;
      }
      return super.equals(obj);
    }

    /**
     * Returns the XML representation of this entry.
     *
     * @param document
     *          the document
     * @return the xml node
     */
    public Node toXml(Document document) {
      Element node = document.createElement(toQName(name));
      // Write prefix binding to document root element
      bindNamespaceFor(document, name);
      if (attributes != null) {
        for (Map.Entry<EName, String> entry : attributes.entrySet()) {
          EName attrEName = entry.getKey();
          if (attrEName.hasNamespace()) {
            // Write prefix binding to document root element
            bindNamespaceFor(document, attrEName);
            if (XSI_TYPE_ATTR.equals(attrEName)) {
              // Special treatment for xsi:type attributes
              try {
                EName typeName = toEName(entry.getValue());
                bindNamespaceFor(document, typeName);
              } catch (NamespaceBindingException ignore) {
                // Type is either not a QName or its namespace is not bound.
                // We decide to gently ignore those cases.
              }
            }
          }
          node.setAttribute(toQName(entry.getKey()), entry.getValue());
        }
      }
      if (value != null) {
        node.appendChild(document.createTextNode(value));
      }
      return node;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(CatalogEntry o) {
      return name.getLocalName().compareTo(name.getLocalName());
    }

    /**
     * Writes a namespace binding for catalog entry <code>name</code> to the documents root element.
     * <code>xmlns:prefix="namespace"</code>
     */
    private void bindNamespaceFor(Document document, EName name) {
      Element root = (Element) document.getFirstChild();
      String namespace = name.getNamespaceName();
      // Do not bind the "xml" namespace. It is bound by default
      if (!XMLConstants.XML_NS_URI.equals(namespace)) {
        root.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + bindings.lookupPrefix(name.getNamespaceName()),
                name.getNamespaceName());
      }
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return value;
    }
  }

  // --------------------------------------------------------------------------------------------

  /**
   * Manages the prefix - namespace bindings.
   */
  protected static class Bindings implements Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = 45L;

    private Map<String, String> prefix2Namespace = new HashMap<String, String>();
    private Map<String, String> namespace2prefix = new HashMap<String, String>();
    private boolean allowRebind;

    /**
     * @param allowRebind
     *          true - prefixes may be rebound, false - an exception will be thrown
     */
    public Bindings(boolean allowRebind) {
      this.allowRebind = allowRebind;
    }

    /**
     * Bind a prefix to a namespace.
     *
     * @param prefix
     *          the prefix
     * @param namespace
     *          the namespace
     */
    public void bindPrefix(String prefix, String namespace) {
      if (prefix == null)
        throw new IllegalArgumentException("Prefix must not be null");
      if (namespace == null)
        throw new IllegalArgumentException("Namespace must not be empty");

      if (!allowRebind) {
        String namespaceCurrent = prefix2Namespace.get(prefix);
        if (namespaceCurrent != null && !namespaceCurrent.equals(namespace)) {
          throw new NamespaceBindingException("Prefix '" + prefix + "' is already bound to namespace " + "'"
                  + namespaceCurrent + "'");
        }
        String prefixCurrent = namespace2prefix.get(namespace);
        if (prefixCurrent != null && !prefixCurrent.equals(prefix)) {
          throw new NamespaceBindingException("Prefix '" + prefixCurrent + "' " + "is already bound to namespace '"
                  + namespace + "'");
        }
      }
      prefix2Namespace.put(prefix, namespace);
      namespace2prefix.put(namespace, prefix);
    }

    /**
     * Returns the bound namespace.
     *
     * @throws NamespaceBindingException
     *           if the prefix is not bound
     */
    public String lookupNamespace(String prefix) {
      String namespace = prefix2Namespace.get(prefix);
      if (namespace == null) {
        throw new NamespaceBindingException("Prefix '" + prefix + "' is not bound");
      }
      return namespace;
    }

    /**
     * Returns the prefix bound to this namespace
     *
     * @throws NamespaceBindingException
     *           if the namespace is not bound
     */
    public String lookupPrefix(String namespace) {
      String prefix = namespace2prefix.get(namespace);
      if (prefix == null) {
        throw new NamespaceBindingException("Namespace '" + namespace + "' is not bound");
      }
      return prefix;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.XMLCatalog#toXml()
   */
  @Override
  public abstract Document toXml() throws ParserConfigurationException, TransformerException, IOException;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.XMLCatalog#toJson()
   */
  @Override
  public abstract String toJson() throws IOException;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.XMLCatalog#toXml(java.io.OutputStream, boolean)
   */
  @Override
  public void toXml(OutputStream out, boolean format) throws IOException {
    try {
      Document doc = this.toXml();
      DOMSource domSource = new DOMSource(doc);
      StreamResult result = new StreamResult(out);
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(domSource, result);
    } catch (ParserConfigurationException e) {
      throw new IOException("unable to parse document");
    } catch (TransformerException e) {
      throw new IOException("unable to transform dom to a stream");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.XMLCatalog#toXmlString()
   */
  @Override
  public String toXmlString() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    toXml(out, true);
    return new String(out.toByteArray(), "UTF-8");
  }

}
