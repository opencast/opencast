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

package org.opencastproject.mediapackage;

import static java.lang.String.format;
import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.EqualsUtil.hash;

import org.opencastproject.util.RequireUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;

/**
 * An XML <dfn>Expanded Name</dfn>, cf. <a href="http://www.w3.org/TR/xml-names11/#dt-expname">W3C definition</a>.
 * <p>
 * Expanded names in XML consists of a namespace name (URI) and a local part. In opposite to <dfn>Qualified Names</dfn>,
 * cf. <a href="http://www.w3.org/TR/xml-names11/#dt-qualname">W3C definition</a> - which are made from an optional
 * prefix and the local part - expanded names are <em>not</em> subject to namespace interpretation.
 * <p>
 * Please see <a href="http://www.w3.org/TR/xml-names/">http://www.w3.org/TR/xml-names/</a> for a complete definition
 * and reference.
 */
public final class EName implements Serializable, Comparable<EName> {
  private static final long serialVersionUID = -5494762745288614634L;

  private final String namespaceURI;
  private final String localName;

  /**
   * A pattern to parse strings as ENames. A String representing an EName may start with a Namespace among curly braces
   * ("{" and "}") and then it must contain a local name *without* any curly braces.
   */
  private static final Pattern pattern = Pattern.compile("^(?:\\{(?<namespace>[^{}\\s]*)\\})?(?<localname>[^{}\\s]+)$");

  /**
   * Create a new expanded name.
   *
   * @param namespaceURI
   *          the name of the namespace this EName belongs to. If set to {@link javax.xml.XMLConstants#NULL_NS_URI},
   *          this name does not belong to any namespace. Use this option with care.
   * @param localName
   *          the local part of the name. Must not be empty.
   */
  public EName(String namespaceURI, String localName) {
    RequireUtil.notNull(namespaceURI, "namespaceURI");
    RequireUtil.notEmpty(localName, "localName");

    this.namespaceURI = namespaceURI;
    this.localName = localName;
  }

  public static EName mk(String namespaceURI, String localName) {
    return new EName(namespaceURI, localName);
  }

  /**
   * Create a new expanded name which does not belong to a namespace. The namespace name is set to
   * {@link javax.xml.XMLConstants#NULL_NS_URI}.
   */
  public static EName mk(String localName) {
    return new EName(XMLConstants.NULL_NS_URI, localName);
  }

  /**
   * Return the namespace name. Usually the name will be a URI.
   *
   * @return the namespace name or {@link javax.xml.XMLConstants#NULL_NS_URI} if the name does not belong to a namespace
   */
  public String getNamespaceURI() {
    return namespaceURI;
  }

  /** Return the local part of the name. */
  public String getLocalName() {
    return localName;
  }

  /**
   * Check, if this name belongs to a namespace, i.e. its namespace URI is not
   * {@link javax.xml.XMLConstants#NULL_NS_URI}.
   */
  public boolean hasNamespace() {
    return !XMLConstants.NULL_NS_URI.equals(namespaceURI);
  }

  @Override
  public int hashCode() {
    return hash(namespaceURI, localName);
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof EName && eqFields((EName) that));
  }

  private boolean eqFields(EName that) {
    return eq(localName, that.localName) && eq(namespaceURI, that.namespaceURI);
  }

  /** Return a W3C compliant string representation <code>{namespaceURI}localname</code>. */
  @Override
  public String toString() {
    return format("{%s}%s", namespaceURI, localName);
  }

  @Override
  public int compareTo(EName o) {
    final int r = getNamespaceURI().compareTo(o.getNamespaceURI());
    if (r == 0) {
      return getLocalName().compareTo(o.getLocalName());
    } else {
      return r;
    }
  }

  /**
   * Parse a W3C compliant string representation <code>{namespaceURI}localname</code>.
   *
   * A String representing an EName may start with a namespace among curly braces ("{" and "}") and then it must contain
   * a local name *without* any blank characters or curly braces.
   *
   * This is a superset of the character restrictions defined by the XML standard, where neither namespaces nor local
   * names may contain curly braces or spaces.
   *
   * Examples:
   *
   * <ul>
   * <li>{http://my-namespace}mylocalname
   * <li>{}localname-with-explicit-empty-namespace
   * <li>localname-without-namespace
   * </ul>
   *
   * Incorrect examples:
   *
   * <ul>
   * <li>{namespace-only}
   * <li>contains{curly}braces
   * </ul>
   *
   * @param strEName
   *          A {@link java.lang.String} representing an {@code EName}
   * @param defaultNameSpace
   *          A NameSpace to apply if the provided {@code String} does not have any. Please note that a explicit empty
   *          NameSpace **is** a NameSpace. If this argument is blank or {@code null}, it has no effect.
   */
  public static EName fromString(String strEName, String defaultNameSpace) throws IllegalArgumentException {
    if (strEName == null) {
      throw new IllegalArgumentException("Cannot parse 'null' as EName");
    }
    Matcher m = pattern.matcher(strEName);

    if (m.matches()) {
      if (StringUtils.isNotBlank(defaultNameSpace) && m.group("namespace") == null)
        return new EName(defaultNameSpace, m.group("localname"));
      else
        return new EName(StringUtils.trimToEmpty(m.group("namespace")), m.group("localname"));
    }
    throw new IllegalArgumentException(format("Cannot parse '%s' as EName", strEName));
  }

  /**
   * Parse a W3C compliant string representation <code>{namespaceURI}localname</code>.
   *
   * A String representing an EName may start with a namespace among curly braces ("{" and "}") and then it must contain
   * a local name *without* any curly braces.
   *
   * This is a superset of the character restrictions defined by the XML standard, where neither namespaces nor local
   * names may contain curly braces.
   *
   * Examples:
   *
   * <ul>
   * <li>{http://my-namespace}mylocalname
   * <li>localname-without-namespace
   * </ul>
   *
   * Incorrect examples:
   *
   * <ul>
   * <li>{namespace-only}
   * <li>contains{curly}braces
   * </ul>
   *
   * @param strEName
   *          A {@link java.lang.String} representing an {@code EName}
   *
   */
  public static EName fromString(String strEName) throws IllegalArgumentException {
    return EName.fromString(strEName, null);
  }
}
