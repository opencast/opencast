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


package org.opencastproject.mediapackage.identifier;

import java.net.URL;

/**
 * Implementation of a CNRI handle.
 */
public final class HandleImpl extends IdImpl implements Handle {

  /** Serial version UID */
  private static final long serialVersionUID = -6345773795959884639L;

  /** The naming authority */
  private String namingAuthority = null;

  /** The local name */
  private String localName = null;

  /** The url where this identifier is pointing to */
  private URL target = null;

  /** The handle builder that created the handle instance */
  private transient HandleBuilder builder = null;

  /**
   * Creates a new handle from the given naming authority and local name. For example, if a handle were
   * <code>10.254/test</code>, then the naming authority would be <code>10.254</code> while the local name would be
   * <code>test</code>.
   *
   * @param namingAuthority
   *          the naming authority
   * @param localName
   *          the handle local name
   * @param builder
   *          the handle builder that created this handle
   */
  HandleImpl(String namingAuthority, String localName, HandleBuilder builder) {
    if (namingAuthority == null)
      throw new IllegalArgumentException("Naming authority is null");
    if (localName == null)
      throw new IllegalArgumentException("Local name is null");
    this.namingAuthority = namingAuthority;
    this.localName = localName;
    this.builder = builder;
    id = formatHandle(namingAuthority, localName);
  }

  /**
   * Creates a new handle from the given naming authority and local name, pointing to the given url as the handle's
   * target.
   *
   * @param namingAuthority
   *          the naming authority
   * @param localName
   *          the handle local name
   * @param url
   *          the handle's target url
   * @param builder
   *          the handle builder that created this handle
   */
  HandleImpl(String namingAuthority, String localName, URL url, HandleBuilder builder) {
    this(namingAuthority, localName, builder);
    if (url == null)
      throw new IllegalArgumentException("Url is null");
    target = url;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.identifier.Handle#getLocalName()
   */
  public String getLocalName() {
    return localName;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.identifier.Handle#getNamingAuthority()
   */
  public String getNamingAuthority() {
    return namingAuthority;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.identifier.Handle#getFullName()
   */
  public String getFullName() {
    StringBuffer buf = new StringBuffer();
    buf.append(namingAuthority);
    buf.append("/");
    buf.append(localName);
    return buf.toString();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.identifier.Handle#resolve()
   */
  public URL resolve() throws HandleException {
    if (target == null) {
      if (builder == null)
        builder = HandleBuilderFactory.newInstance().newHandleBuilder();
      target = builder.resolve(this);
    }
    return target;
  }

  /**
   * @see org.opencastproject.mediapackage.identifier.Handle#update(java.net.URL)
   */
  public void update(URL target) throws HandleException {
    builder.update(this, target);
    this.target = target;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Handle) {
      Handle h = (Handle) obj;
      return namingAuthority.equals(h.getNamingAuthority()) && localName.equals(h.getLocalName());
    }
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return id;
  }

  /**
   * Returns the formatted version of a handle consisting of the specified <code>namingAuthority</code> and
   * <code>localName</code>.
   *
   * @param namingAuthority
   *          the naming authority
   * @param localName
   *          the local name
   * @return the formatted handle
   */
  static String formatHandle(String namingAuthority, String localName) {
    StringBuffer buf = new StringBuffer(namingAuthority);
    buf.append("/");
    buf.append(localName);
    return buf.toString();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.identifier.Id#compact()
   */
  public String compact() {
    StringBuffer buf = new StringBuffer();
    buf.append(namingAuthority);
    buf.append("-");
    buf.append(localName.replaceAll("/", "-").replaceAll("\\\\", "-"));
    return buf.toString();
  }

}
