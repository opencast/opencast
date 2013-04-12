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
package org.opencastproject.ingest.impl;

import com.google.common.base.Preconditions;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ZipEntryInputStream extends FilterInputStream {

  /** Length of the zip entry */
  private long bytesToRead = 0;

  /**
   * Creates a wrapper around the input stream <code>in</code> and reads the given number of bytes from it.
   * 
   * @param in
   *          the input stream
   * @param length
   *          the number of bytes to read
   */
  public ZipEntryInputStream(InputStream in, long length) {
    super(in);
    bytesToRead = length;
    Preconditions.checkNotNull(in);
  }

  @Override
  public int read() throws IOException {
    if (bytesToRead == 0)
      return -1;
    bytesToRead--;
    int byteContent = in.read();
    if (byteContent == -1)
      throw new IOException("Zip entry is shorter than anticipated");
    return byteContent;
  }

  @Override
  public void close() throws IOException {
    // This stream mustn't be closed because it handles the Zip entry parts of the given Zip input stream
    // make sure the given zip input stream is closed after reading
  }

}