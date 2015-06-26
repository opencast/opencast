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

import java.io.Serializable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.xml.XMLConstants;

/**
 * An XML <dfn>Expanded Name</dfn>, cf. <a href="http://www.w3.org/TR/xml-names11/#dt-expname">W3C definition</a>.
 * <p/>
 * Expanded names in XML consists of a namespace name (URI) and a local part.
 * In opposite to <dfn>Qualified Names</dfn>,
 * cf. <a href="http://www.w3.org/TR/xml-names11/#dt-qualname">W3C definition</a> -
 * which are made from an optional prefix and the local part - expanded names are <em>not</em> subject to
 * namespace interpretation.
 * <p/>
 * Please see <a href="http://www.w3.org/TR/xml-names/">http://www.w3.org/TR/xml-names/</a> for a
 * complete definition and reference.
 */
@Immutable
@ParametersAreNonnullByDefault
public final class EName implements Serializable {
  private static final long serialVersionUID = -5494762745288614634L;

  private final String namespaceURI;
  private final String localName;

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
   * Check, if this name belongs to a namespace, i.e. its namespace URI
   * is not {@link javax.xml.XMLConstants#NULL_NS_URI}.
   */
  public boolean hasNamespace() {
    return !XMLConstants.NULL_NS_URI.equals(namespaceURI);
  }

  @Override public int hashCode() {
    return hash(namespaceURI, localName);
  }

  @Override public boolean equals(Object that) {
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
}
