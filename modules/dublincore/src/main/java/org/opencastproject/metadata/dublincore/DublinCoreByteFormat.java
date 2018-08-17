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
package org.opencastproject.metadata.dublincore;

import static com.entwinemedia.fn.Prelude.chuck;

import com.entwinemedia.fn.Fn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Byte serialization of Dublin Core catalogs.
 */
public final class DublinCoreByteFormat {
  private DublinCoreByteFormat() {
  }

  public static DublinCoreCatalog read(byte[] bytes) {
    return DublinCores.read(new ByteArrayInputStream(bytes));
  }

  /** Serialize a DublinCore catalog to a UTF-8 encoded byte array. */
  public static byte[] writeByteArray(DublinCoreCatalog dc) {
    try {
      return dc.toXmlString().getBytes(StandardCharsets.UTF_8);
    } catch (IOException e) {
      return chuck(e);
    }
  }

  /**
   * {@link #read(byte[])} as a function.
   */
  public static final Fn<byte[], DublinCoreCatalog> readFromArray = new Fn<byte[], DublinCoreCatalog>() {
    @Override public DublinCoreCatalog apply(byte[] bytes) {
      return DublinCoreByteFormat.read(bytes);
    }
  };
}
