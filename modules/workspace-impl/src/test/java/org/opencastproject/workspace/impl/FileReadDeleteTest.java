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
package org.opencastproject.workspace.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.util.FileSupport;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;

public class FileReadDeleteTest {
  private static final String FILE = "/opencast_header.gif";

  private static final Logger logger = LoggerFactory.getLogger(FileReadDeleteTest.class);

  private final Object start = new Object();
  private volatile long totalRead = 0;
  private long expectedSize = 0;

  @Before
  public void setUp() {
    totalRead = 0;
    expectedSize = resourceAsFile(FILE).length();
  }

  @Test
  public void testFileDeletionWhileReadingIo() throws Exception {
    testFileDeletionWhileReading(readIo);
  }

  @Test
  public void testFileDeletionWhileReadingNio() throws Exception {
    testFileDeletionWhileReading(readNio);
  }

  private void testFileDeletionWhileReading(final Function<FileInputStream, Function<Long, Long>> mkReader)
          throws Exception {
    final File source = resourceAsFile(FILE);
    final File work = FileSupport.copy(source, new File(source.getParentFile(), UUID.randomUUID().toString() + ".tmp"));
    assertTrue("Work file could not be created", work.exists());
    try {
      final Thread readerThread = new Thread(mkRunnable(work, mkReader));
      synchronized (start) {
        readerThread.start();
        // wait for reader
        start.wait();
      }
      assertEquals("Reader already finished", 0, totalRead);
      assertTrue("File could not be deleted", work.delete());
      assertFalse("File still exists", work.exists());
      logger.debug("Work file deleted");
      // wait for reader to complete
      readerThread.join();
      assertEquals("File not completely read", expectedSize, totalRead);
    } finally {
      // cleanup
      FileSupport.delete(work);
      assertFalse(work.exists());
    }
  }

  /** Create a runnable containing a reader created from a <code>readerMaker</code>. */
  private Runnable mkRunnable(final File file, final Function<FileInputStream, Function<Long, Long>> readerMaker) {
    return new Runnable() {
      @Override public void run() {
        try {
          withResource(new FileInputStream(file), mkReaderFrom(readerMaker));
        } catch (FileNotFoundException e) {
          chuck(e);
        }
      }
    };
  }

  /**
   * Create a read effect from a <code>readerMaker</code> function.
   * This read effect will be used to consume a file input stream.
   */
  private Effect<FileInputStream> mkReaderFrom(final Function<FileInputStream, Function<Long, Long>> readerMaker) {
    return new Effect<FileInputStream>() {
      @Override public void run(final FileInputStream in) {
        logger.debug("Start reading");
        long total = 0L;
        long read;
        final Function<Long, Long> readFile = readerMaker.apply(in);
        try {
          while ((read = readFile.apply(total)) > 0) {
            total = total + read;
            logger.debug("Read " + total);
            if (total > 3000) {
              synchronized (start) {
                start.notifyAll();
              }
            }
            Thread.sleep(100);
          }
          totalRead = total;
          logger.debug("File completely read " + total);
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
    };
  }

  /** NIO based file reader. */
  private Function<FileInputStream, Function<Long, Long>> readNio
      = new Function<FileInputStream, Function<Long, Long>>() {
        @Override public Function<Long, Long> apply(final FileInputStream in) {
          final FileChannel channel = in.getChannel();
          final Sink sink = new Sink();
          //
          return new Function.X<Long, Long>() {
            @Override public Long xapply(Long total) throws Exception {
              return channel.transferTo(total, 1024, sink);
            }
          };
        }
      };

  /** Normal IO based file reader. */
  private Function<FileInputStream, Function<Long, Long>> readIo
      = new Function<FileInputStream, Function<Long, Long>>() {
        @Override public Function<Long, Long> apply(final FileInputStream in) {
          final byte[] buffer = new byte[1024];
          //
          return new Function.X<Long, Long>() {
            @Override public Long xapply(Long total) throws Exception {
              return (long) in.read(buffer);
            }
          };
        }
      };

  private File resourceAsFile(String resource) {
    try {
      return new File(this.getClass().getResource(resource).toURI());
    } catch (URISyntaxException e) {
      return chuck(e);
    }
  }

  private static class Sink implements WritableByteChannel {
    private boolean closed = false;

    @Override public int write(ByteBuffer byteBuffer) throws IOException {
      return byteBuffer.limit();
    }

    @Override public boolean isOpen() {
      return !closed;
    }

    @Override public void close() throws IOException {
      closed = true;
    }
  }
}
