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
package org.opencastproject.archive.api;

import org.opencastproject.util.MimeType;

import java.io.InputStream;

/** Archived media package element. */
public class ArchivedMediaPackageElement {
  private final InputStream inputStream;
  private final MimeType mimeType;
  private final long size;

  public ArchivedMediaPackageElement(InputStream inputStream, MimeType mimeType, long size) {
    this.inputStream = inputStream;
    this.mimeType = mimeType;
    this.size = size;
  }

  /**
   * Return an input stream to the element's content.
   * Client code is in charge of closing the stream.
   */
  public InputStream getInputStream() {
    return inputStream;
  }

  /**
   * Return the element's mimetype as found in the ISO Mime Type Registrations.
   * <p/>
   * For example, in case of motion jpeg slides, this method will return the mime type for <code>video/mj2</code>.
   */
  public MimeType getMimeType() {
    return mimeType;
  }

  /** Return the number of bytes that are occupied by this media package element. */
  public long getSize() {
    return size;
  }
}
