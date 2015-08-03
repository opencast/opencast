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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * input stream to get only a part of a file
 *
 */
public class ChunkedFileInputStream extends FileInputStream {

  /**
   * starting offset
   */
  private long offset;
  /**
   * the current offset
   */
  private long currentOffset;
  /**
   * ending offset
   */
  private long endOffset;

  private static final Logger logger = LoggerFactory.getLogger(ChunkedFileInputStream.class);

  /**
   * constructor
   *
   * @param name the name of the file
   * @throws FileNotFoundException if the file was not found
   */
  public ChunkedFileInputStream(String name) throws FileNotFoundException {
    this(name != null ? new File(name) : null, 0, 0);
  }

  /**
   * constructor
   *
   * @param file the file to load
   * @param offset the starting offset
   * @param endOffset the ending offset
   * @throws FileNotFoundException if the requested file was not found
   */
  public ChunkedFileInputStream(File file, long offset, long endOffset)
      throws FileNotFoundException {
    super(file);
    this.offset = offset;
    this.currentOffset = offset;
    this.endOffset = endOffset == 0 ? file.length() : endOffset;
    if (offset != 0) {
      logger.debug("skipping first {} bytes", offset);
      try {
        this.skip(offset);
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }
    }
  }

  /**
   * read the next byte
   *
   * @return the next byte or -1 if the expected offset has been reached
   */
  public int read() throws IOException {
    this.currentOffset++;
    if (currentOffset > endOffset) {
      return -1;
    }
    return super.read();
  }

  /**
   * get the ending offset
   *
   * @return the ending offset
   */
  public long getEndOffset() {
    return endOffset;
  }

  /**
   * set the ending offset
   *
   * @param endOffset the ending offset
   */
  public void setEndOffset(long endOffset) {
    this.endOffset = endOffset;
  }

  /***
   * get the starting offset
   *
   * @return the starting offset
   */
  public long getOffset() {
    return offset;
  }

  /**
   * set the starting offset
   *
   * @param offset the starting offset
   */
  public void setOffset(long offset) {
    this.offset = offset;
  }

}
