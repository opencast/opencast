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

import java.io.Serializable;

import javax.xml.XMLConstants;

/**
 * An XML <dfn>Expanded Name.</dfn>
 * <p>
 * Expanded names in XML consists of a namespace name and a local part. In opposite to <dfn>Qualified Names</dfn> -
 * which are made from an optional prefix and the local part - expanded names are <em>not</em> subject to
 * interpretation. Please see <a href="http://www.w3.org/TR/xml-names/">http://www.w3.org/TR/xml-names/</a> for a
 * complete definition and reference.
 */
public class EName implements Serializable {

  /** Serial version uid */
  private static final long serialVersionUID = 1L;

  private String namespaceName;
  private String localName;

  /**
   * Create a new expanded name.
   * 
   * @param namespaceName
   *          the name of the namespace this EName belongs to. If set to {@link javax.xml.XMLConstants#NULL_NS_URI},
   *          this name does not belong to any namespace. Use this option with care.
   * @param localName
   *          the local part of the name. Must not be empty.
   */
  public EName(String namespaceName, String localName) {
    if (namespaceName == null)
      throw new IllegalArgumentException("Namespace name must not be null");
    if (localName == null || localName.length() == 0)
      throw new IllegalArgumentException("Local name must not be empty");

    this.namespaceName = namespaceName;
    this.localName = localName;
  }

  /**
   * Creates a new expanded name which does not belong to a namespace. The namespace name is set to
   * {@link javax.xml.XMLConstants#NULL_NS_URI}.
   */
  public EName(String localName) {
    this(XMLConstants.NULL_NS_URI, localName);
  }

  /**
   * Returns the namespace name. Usually the name will be a URI.
   * 
   * @return the namespace name or {@link javax.xml.XMLConstants#NULL_NS_URI} if the name does not belong to a namespace
   */
  public String getNamespaceName() {
    return namespaceName;
  }

  /**
   * Returns the local part of the name.
   */
  public String getLocalName() {
    return localName;
  }

  /**
   * Checks, if this name belongs to a namespace.
   */
  public boolean hasNamespace() {
    return !XMLConstants.NULL_NS_URI.equals(namespaceName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    EName eName = (EName) o;

    if (!localName.equals(eName.localName))
      return false;
    if (!namespaceName.equals(eName.namespaceName))
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = namespaceName.hashCode();
    result = 31 * result + localName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "EName{" + "namespaceName='" + namespaceName + '\'' + ", localName='" + localName + '\'' + '}';
  }
}
