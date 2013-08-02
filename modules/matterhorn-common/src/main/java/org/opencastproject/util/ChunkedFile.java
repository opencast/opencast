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

import java.io.File;
import java.io.IOException;

public class ChunkedFile {

  private File file;
  private long contentLength;
  private long offset;

  /**
   * 
   * @param file the file containing the data
   * @throws IOException if an error occured
   */
  public ChunkedFile(File file) throws IOException {
    this(file, 0, file.length());
  }

  /**
   * Creates a new instance that fetches data from the specified file.
   * 
   * @param offset the offset of the file where the transfer begins
   * @param contentLength the number of bytes to transfer
   */
  public ChunkedFile(File file, long offset, long contentLength)
      throws IOException {
    if (file == null) {
      throw new NullPointerException("file");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset: " + offset + " (expected: 0 or greater)");
    }
    if (contentLength < 0) {
      throw new IllegalArgumentException("length: " + contentLength + " (expected: 0 or greater)");
    }

    this.file = file;
    this.offset = offset;
    this.contentLength = contentLength;

  }

  /**
   * @return the offset in the file where the transfer began.
   */
  public long getOffset() {
    return offset;
  }
  
  /**
   * @return the file
   */
  public File getFile() {
    return file;
  }
  
  /**
   * @return the size of the content
   */
  public long getContentLength() {
    return contentLength;
  }
}
