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

package org.opencastproject.mediapackage.identifier;

import org.opencastproject.mediapackage.MediaPackageSupport;

import java.util.UUID;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Simple and straightforward implementation of the {@link Id} interface.
 */
@XmlType
@XmlAccessorType(XmlAccessType.NONE)
public class IdImpl implements Id {

  /**
   * Identifiers created programmatically may only contain the characters of this class
   * (which in particular rules out path separators and the empty identifier). In addition,
   * the shared traversal rule (see {@link MediaPackageSupport#containsPathTraversal(String)})
   * rejects the current and parent directory references {@code .} and {@code ..} as complete
   * identifiers: identifiers are joined into file system paths by several components, and such
   * identifiers would allow path traversal. A {@code ..} sequence inside the identifier
   * (e.g. {@code track..1}) is a harmless literal file system name and stays legal.
   */
  private static final Pattern pattern = Pattern.compile("[\\w-_.:;()]+");

  /** The identifier */
  @XmlValue
  protected String id = null;

  /**
   * Needed for JAXB serialization
   */
  public IdImpl() {
  }

  /**
   * Creates a new identifier.
   *
   * @param id
   *          the identifier
   */
  public IdImpl(final String id) {
    validate(id);
    this.id = id;
  }

  /**
   * Note: this class deliberately has no JAXB lifecycle callback. The identifier is bound as
   * an XML attribute, and the JAXB runtime does not invoke {@code afterUnmarshal} callbacks
   * for attribute values (nor for the one-argument or no-argument signatures at all, in the
   * GlassFish JAXB 2.3.x runtime). Unmarshalling is covered by
   * {@link MediaPackageImpl#afterUnmarshal(Unmarshaller, Object)} and by the parse choke
   * points ({@code MediaPackageImpl.valueOf}, {@code MediaPackageSupport.checkPathSafety}).
   * This class enforces the rule on the programmatic creation path (the constructors), which
   * is stricter than the unmarshal path on purpose: identifiers created programmatically must
   * match {@link #pattern}, while existing (unmarshalled) identifiers only have to be free of
   * path traversal sequences, since they may already contain other characters (e.g. a space
   * or {@code @}) that are not unsafe.
   */
  private static void validate(String id) {
    if (!pattern.matcher(id).matches()) {
      throw new IllegalArgumentException("Id must match " + pattern);
    }
    if (MediaPackageSupport.containsPathTraversal(id)) {
      throw new IllegalArgumentException("Id must not be empty, must not be '.' or '..' "
              + "and must not contain path separators (path traversal identifiers are not allowed): "
              + id);
    }
  }

  @Override
  public String toString() {
    return id;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof IdImpl) {
      IdImpl other = (IdImpl) o;
      return id != null && other.id != null && id.equals(other.id);
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
   * Generate a new UUID-based Id.
   * @return New Id
   */
  public static Id fromUUID() {
    return new IdImpl(UUID.randomUUID().toString());
  }
}
