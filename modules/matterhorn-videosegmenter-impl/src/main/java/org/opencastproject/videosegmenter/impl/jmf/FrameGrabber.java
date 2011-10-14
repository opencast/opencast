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
package org.opencastproject.videosegmenter.impl.jmf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.DataSink;
import javax.media.IncompatibleSourceException;
import javax.media.MediaLocator;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

/**
 * This DataSourceHandler implementation reads from a DataSource and display information of each frame of data received.
 */
public class FrameGrabber implements DataSink, BufferTransferHandler {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(FrameGrabber.class);

  /** The data source */
  private DataSource source = null;

  /** Data sink listeners */
  private Vector<DataSinkListener> listeners = new Vector<DataSinkListener>(1);

  /** The current buffer */
  private Buffer readBuffer = null;

  /** Flag to indicate whether there is more input to come */
  private boolean endOfStream = false;

  /**
   * {@inheritDoc}
   * 
   * @see javax.media.MediaHandler#setSource(javax.media.protocol.DataSource)
   */
  public void setSource(DataSource source) throws IncompatibleSourceException {
    if (source instanceof PushBufferDataSource) {
      PushBufferStream[] pushStrms = ((PushBufferDataSource) source).getStreams();
      if (pushStrms.length != 1)
        throw new IllegalStateException("Unable to handle more than one stream per data source");
      pushStrms[0].setTransferHandler(this);
    } else {
      throw new IncompatibleSourceException();
    }
    this.source = source;
  }

  /**
   * For completeness, DataSink's require this method. But we don't need it.
   */
  public void setOutputLocator(MediaLocator ml) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the output locator, which will always be <code>null</code>.
   * 
   * @see javax.media.DataSink#getOutputLocator()
   */
  public MediaLocator getOutputLocator() {
    return null;
  }

  /**
   * Returns the source's content type.
   */
  public String getContentType() {
    return source.getContentType();
  }

  /**
   * Our DataSink does not need to be opened.
   */
  public void open() {
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.media.DataSink#start()
   */
  public void start() {
    try {
      source.start();
    } catch (IOException e) {
      sendEvent(new DataSinkErrorEvent(this, e.getMessage()));
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.media.DataSink#stop()
   */
  public void stop() {
    try {
      source.stop();
    } catch (IOException e) {
      sendEvent(new DataSinkErrorEvent(this, e.getMessage()));
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.media.DataSink#close()
   */
  public void close() {
    stop();
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.media.DataSink#addDataSinkListener(javax.media.datasink.DataSinkListener)
   */
  public void addDataSinkListener(DataSinkListener dsl) {
    if (dsl != null)
      if (!listeners.contains(dsl))
        listeners.addElement(dsl);
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.media.DataSink#removeDataSinkListener(javax.media.datasink.DataSinkListener)
   */
  public void removeDataSinkListener(DataSinkListener dsl) {
    if (dsl != null)
      listeners.removeElement(dsl);
  }

  /**
   * 
   * @param event
   */
  protected void sendEvent(DataSinkEvent event) {
    if (!listeners.isEmpty()) {
      synchronized (listeners) {
        Enumeration<DataSinkListener> list = listeners.elements();
        while (list.hasMoreElements()) {
          DataSinkListener listener = list.nextElement();
          listener.dataSinkUpdate(event);
        }
      }
    }
  }

  /**
   * This will get called when there's data pushed from the PushBufferDataSource.
   */
  public void transferData(PushBufferStream stream) {
    try {
      synchronized (this) {
        while (!endOfStream && !stream.endOfStream()) {
          try {
            readBuffer = new Buffer();
            stream.read(readBuffer);
            if (stream.endOfStream() || readBuffer.isEOM()) {
              sendEvent(new EndOfStreamEvent(this));
              readBuffer = null;
              endOfStream = true;
            }
            this.notifyAll();
            if (!endOfStream) {
              logger.debug("Found buffer {} at {} s ({} bytes)", new Object[] { readBuffer.getSequenceNumber(),
                      readBuffer.getTimeStamp() / 100000000, readBuffer.getLength() });
              this.wait();
            }
          } catch (InterruptedException e) {
            logger.warn("Interrupted!");
            // Finally!
          }
        }
        this.notifyAll();
      }
    } catch (IOException e) {
      sendEvent(new DataSinkErrorEvent(this, e.getMessage()));
      return;
    } catch (IllegalMonitorStateException illegalMonitorStateException) {
      // Added to deal with MH-7924 even though it isn't explicitly defined in the API as a possible exception.
      sendEvent(new DataSinkErrorEvent(this, illegalMonitorStateException.getMessage()));
      return;
    }
  }

  /**
   * Returns the current buffer.
   * 
   * @return the buffer the buffer
   * @throws IOException
   */
  public Buffer getBuffer() throws IOException {
    Buffer currentBuffer = null;
    synchronized (this) {
      if (endOfStream) {
        this.notifyAll();
        return null;
      }
      while (readBuffer == null && !endOfStream) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          logger.warn("Interrupted!");
          // Finally!
        }
      }
      currentBuffer = readBuffer;
      readBuffer = null;
      this.notifyAll();
    }

    return currentBuffer;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.media.Controls#getControls()
   */
  public Object[] getControls() {
    return new Object[0];
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.media.Controls#getControl(java.lang.String)
   */
  public Object getControl(String name) {
    return null;
  }

}
