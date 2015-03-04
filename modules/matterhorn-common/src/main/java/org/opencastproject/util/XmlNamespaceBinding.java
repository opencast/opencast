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

import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.EqualsUtil.hash;

import java.io.Serializable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * Declaration of an XML namespace binding which is the association of a prefix to a namespace URI (namespace name).
 * <p/>
 * See <a href="http://www.w3.org/TR/xml-names11/#sec-namespaces">W3C specification</a> for details.
 */
@Immutable
@ParametersAreNonnullByDefault
public final class XmlNamespaceBinding implements Serializable {
  private static final long serialVersionUID = -3189348197739705012L;

  private final String prefix;
  private final String namespaceURI;

  /**
   * Bind a prefix to a namespace URI (namespace name).
   *
   * @param prefix a prefix or the empty string ({@link javax.xml.XMLConstants#DEFAULT_NS_PREFIX}) to bind
   *          the default namespace
   * @param namespaceURI Either a URI or the empty string ({@link javax.xml.XMLConstants#NULL_NS_URI}).
   *          See <a href="http://www.w3.org/TR/REC-xml-names/#ns-decl">Declaring Namespaces</a>
   *          for details about namespace declarations.
   */
  public XmlNamespaceBinding(String prefix, String namespaceURI) {
    this.prefix = RequireUtil.notNull(prefix, "prefix");
    this.namespaceURI = RequireUtil.notNull(namespaceURI, "namespaceURI");
  }

  /**
   * Constructor method.
   *
   * @see org.opencastproject.util.XmlNamespaceBinding#XmlNamespaceBinding(String, String)
   */
  public static XmlNamespaceBinding mk(String prefix, String namespaceURI) {
    return new XmlNamespaceBinding(prefix, namespaceURI);
  }

  public String getPrefix() {
    return prefix;
  }

  public String getNamespaceURI() {
    return namespaceURI;
  }

  @Override public int hashCode() {
    return hash(prefix, namespaceURI);
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof XmlNamespaceBinding && eqFields((XmlNamespaceBinding) that));
  }

  private boolean eqFields(XmlNamespaceBinding that) {
    return eq(prefix, that.prefix) && eq(namespaceURI, that.namespaceURI);
  }
}
