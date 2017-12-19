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

import org.apache.commons.io.input.ProxyInputStream;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link InputStream} that counts the number of bytes read.
 */
public class ProgressInputStream extends ProxyInputStream {

  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  private volatile long totalNumBytesRead;

  public ProgressInputStream(InputStream in) {
    super(in);
  }

  public long getTotalNumBytesRead() {
    return totalNumBytesRead;
  }

  /**
   * Adds a {@link PropertyChangeListener}
   *
   * The listener gets notified as soon as the input stream is read.
   *
   * @param l
   *          the {@link PropertyChangeListener}
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    propertyChangeSupport.addPropertyChangeListener(l);
  }

  /**
   * Removes a {@link PropertyChangeListener}
   *
   * The listener gets notified as soon as the input stream is read.
   *
   * @param l
   *          the {@link PropertyChangeListener}
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    propertyChangeSupport.removePropertyChangeListener(l);
  }

  @Override
  public int read() throws IOException {
    return (int) updateProgress(super.read());
  }

  @Override
  public int read(byte[] b) throws IOException {
    return (int) updateProgress(super.read(b));
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return (int) updateProgress(super.read(b, off, len));
  }

  @Override
  public long skip(long n) throws IOException {
    return updateProgress(super.skip(n));
  }

  @Override
  public void mark(int readlimit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void reset() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  private long updateProgress(long numBytesRead) {
    if (numBytesRead > 0) {
      long oldTotalNumBytesRead = totalNumBytesRead;
      totalNumBytesRead += numBytesRead;
      propertyChangeSupport.firePropertyChange("totalNumBytesRead", oldTotalNumBytesRead, totalNumBytesRead);
    }
    return numBytesRead;
  }
}
