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


package org.opencastproject.util;

import org.apache.commons.io.IOUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class stores value and type of a generated checksum.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "checksum", namespace = "http://mediapackage.opencastproject.org")
public final class Checksum implements Serializable {

  /** Serial version uid */
  private static final long serialVersionUID = 1L;

  /** The checksum value */
  @XmlValue
  protected String value = null;

  /** The checksum type */
  @XmlAttribute(name = "type")
  protected ChecksumType type = null;

  /** Needed by JAXB */
  public Checksum() {
  }

  /**
   * Creates a new checksum object of the specified value and checksum type.
   *
   * @param value
   *          the value
   * @param type
   *          the type
   */
  private Checksum(String value, ChecksumType type) {
    if (value == null)
      throw new IllegalArgumentException("Checksum value is null");
    if (type == null)
      throw new IllegalArgumentException("Checksum type is null");
    this.value = value;
    this.type = type;
  }

  /**
   * Returns the checksum type.
   *
   * @return the type
   */
  public ChecksumType getType() {
    return type;
  }

  /**
   * Returns the checksum value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Converts the checksum to a hex string.
   *
   * @param data
   *          the digest
   * @return the digest hex representation
   */
  public static String convertToHex(byte[] data) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < data.length; i++) {
      int halfbyte = (data[i] >>> 4) & 0x0F;
      int twoHalfs = 0;
      do {
        if ((0 <= halfbyte) && (halfbyte <= 9))
          buf.append((char) ('0' + halfbyte));
        else
          buf.append((char) ('a' + (halfbyte - 10)));
        halfbyte = data[i] & 0x0F;
      } while (twoHalfs++ < 1);
    }
    return buf.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Checksum) {
      Checksum c = (Checksum) obj;
      return type.equals(c.type) && value.equals(c.value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value + " (" + type + ")";
  }

  /**
   * Creates a checksum of type <code>type</code> and value <code>value</code>.
   *
   * @param type
   *          the checksum type name
   * @param value
   *          the checksum value
   * @return the checksum
   * @throws NoSuchAlgorithmException
   *           if the checksum of the specified type cannot be created
   */
  public static Checksum create(String type, String value) throws NoSuchAlgorithmException {
    ChecksumType t = ChecksumType.fromString(type);
    return new Checksum(value, t);
  }

  /**
   * Creates a checksum of type <code>type</code> and value <code>value</code>.
   *
   * @param type
   *          the checksum type
   * @param value
   *          the checksum value
   * @return the checksum
   */
  public static Checksum create(ChecksumType type, String value) {
    return new Checksum(value, type);
  }

  /**
   * Creates a checksum of type <code>type</code> from the given file.
   *
   * @param type
   *          the checksum type
   * @param file
   *          the file
   * @return the checksum
   * @throws NoSuchAlgorithmException
   *           if the checksum of the specified type cannot be created
   * @throws IOException
   *           if the file cannot be accessed
   */
  public static Checksum create(ChecksumType type, File file) throws IOException {
    return create(type, new BufferedInputStream(new FileInputStream(file)));
  }

  /**
   * Creates a checksum of type <code>type</code> from the given input stream.
   * The stream gets closed afterwards.
   */
  public static Checksum create(ChecksumType type, InputStream is) throws IOException {
    MessageDigest checksum;
    try {
      checksum = MessageDigest.getInstance(type.getName());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("This system does not support checksums of type " + type.getName());
    }
    try {
      byte[] bytes = new byte[1024];
      int len = 0;
      while ((len = is.read(bytes)) >= 0) {
        checksum.update(bytes, 0, len);
      }
    } finally {
      IoSupport.closeQuietly(is);
    }
    return new Checksum(convertToHex(checksum.digest()), type);
  }

  /** Create a checksum of type <code>type</code> for the given <code>string</code>. */
  public static Checksum createFor(ChecksumType type, String string) throws IOException {
    return create(type, IOUtils.toInputStream(string, "UTF-8"));
  }
}
