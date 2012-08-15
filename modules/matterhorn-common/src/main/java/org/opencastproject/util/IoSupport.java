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

import de.schlichtherle.io.FileWriter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;

import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/**
 * Contains operations concerning IO.
 */
public final class IoSupport {

  /**
   * the logging facility provided by log4j
   */
  private static Logger logger = LoggerFactory.getLogger(IoSupport.class.getName());

  public static String getSystemTmpDir() {
    String tmpdir = System.getProperty("java.io.tmpdir");
    if (tmpdir == null) {
      tmpdir = File.separator + "tmp" + File.separator;
    } else {
      if (!tmpdir.endsWith(File.separator)) {
        tmpdir += File.separator;
      }
    }
    return tmpdir;
  }

  private IoSupport() {
  }

  /**
   * Closes a <code>Closable</code> quietly so that no exceptions are thrown.
   *
   * @param s
   *         maybe null
   */
  public static boolean closeQuietly(final Closeable s) {
    if (s == null) {
      return false;
    }
    try {
      s.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Closes a <code>StreamHelper</code> quietly so that no exceptions are thrown.
   *
   * @param s
   *         maybe null
   */
  public static boolean closeQuietly(final StreamHelper s) {
    if (s == null) {
      return false;
    }
    s.stopReading();
    return true;
  }

  /**
   * Closes the processes input, output and error streams.
   *
   * @param process
   *         the process
   * @return <code>true</code> if the streams were closed
   */
  public static boolean closeQuietly(final Process process) {
    if (process != null) {
      try {
        if (process.getErrorStream() != null)
          process.getErrorStream().close();
        if (process.getInputStream() != null)
          process.getInputStream().close();
        if (process.getOutputStream() != null)
          process.getOutputStream().close();
        return true;
      } catch (Throwable t) {
        logger.trace("Error closing process streams: " + t.getMessage());
      }
    }
    return false;
  }

  /**
   * Extracts the content from the given input stream. This method is intended to faciliate handling of processes that
   * have error, input and output streams.
   *
   * @param is
   *         the input stream
   * @return the stream content
   */
  public static String getOutput(InputStream is) {
    InputStreamReader bis = new InputStreamReader(is);
    StringBuffer outputMsg = new StringBuffer();
    char[] chars = new char[1024];
    try {
      int len = 0;
      try {
        while ((len = bis.read(chars)) > 0) {
          outputMsg.append(chars, 0, len);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    } finally {
      if (bis != null)
        try {
          bis.close();
        } catch (IOException e) {
        }
    }
    return outputMsg.toString();
  }

  /**
   * Writes the contents variable to the {@code URL}. Note that the URL must be a local {@code URL}.
   *
   * @param file
   *         The {@code URL} of the local file you wish to write to.
   * @param contents
   *         The contents of the file you wish to create.
   * @throws URISyntaxException
   */
  public static void writeUTF8File(URL file, String contents) throws IOException {
    try {
      writeUTF8File(new File(file.toURI()), contents);
    } catch (URISyntaxException e) {
      throw new IOException("Couldn't parse the URL", e);
    }
  }

  /**
   * Writes the contents variable to the {@code File}.
   *
   * @param file
   *         The {@code File} of the local file you wish to write to.
   * @param contents
   *         The contents of the file you wish to create.
   */
  public static void writeUTF8File(File file, String contents) throws IOException {
    writeUTF8File(file.getAbsolutePath(), contents);
  }

  /**
   * Writes the contents variable to the {@code File} located at the filename.
   *
   * @param filename
   *         The {@code File} of the local file you wish to write to.
   * @param contents
   *         The contents of the file you wish to create.
   */
  public static void writeUTF8File(String filename, String contents) throws IOException {
    FileWriter out = new FileWriter(filename);
    out.write(contents);
    closeQuietly(out);
  }

  /**
   * Convenience method to read in a file from a local source.
   *
   * @param url
   *         The {@code URL} to read the source data from.
   * @return A String containing the source data or null in the case of an error.
   */
  public static String readFileFromURL(URL url) {
    return readFileFromURL(url, null);
  }

  /**
   * Convenience method to read in a file from either a remote or local source.
   *
   * @param url
   *         The {@code URL} to read the source data from.
   * @param trustedClient
   *         The {@code TrustedHttpClient} which should be used to communicate with the remote server. This can be null
   *         for local file reads.
   * @return A String containing the source data or null in the case of an error.
   */
  public static String readFileFromURL(URL url, TrustedHttpClient trustedClient) {
    StringBuilder sb = new StringBuilder();
    DataInputStream in = null;
    HttpResponse response = null;
    try {
      // Do different things depending on what we're reading...
      if ("file".equals(url.getProtocol())) {
        in = new DataInputStream(url.openStream());
      } else {
        if (trustedClient == null) {
          logger.error("Unable to read from remote source {} because trusted client is null!", url.getFile());
          return null;
        }
        HttpGet get = new HttpGet(url.toURI());
        try {
          response = trustedClient.execute(get);
        } catch (TrustedHttpClientException e) {
          logger.warn("Unable to fetch file from {}.", url, e);
          trustedClient.close(response);
          return null;
        }
        in = new DataInputStream(response.getEntity().getContent());
      }
      int c = 0;
      while ((c = in.read()) != -1) {
        sb.append((char) c);
      }
    } catch (IOException e) {
      logger.warn("IOException attempting to get file from {}.", url);
      return null;
    } catch (URISyntaxException e) {
      logger.warn("URI error attempting to get file from {}.", url);
      return null;
    } catch (NullPointerException e) {
      logger.warn("Nullpointer attempting to get file from {}.", url);
      return null;
    } finally {
      IOUtils.closeQuietly(in);

      if (response != null && trustedClient != null) {
        trustedClient.close(response);
        response = null;
      }
    }

    return sb.toString();
  }

  /**
   * Handle a stream inside <code>f</code> and ensure that <code>s</code> gets closed properly.
   *
   * @deprecated use {@link #withResource(java.io.Closeable, org.opencastproject.util.data.Function)} instead
   */
  public static <A> A withStream(InputStream s, Function<InputStream, A> f) {
    try {
      return f.apply(s);
    } finally {
      IoSupport.closeQuietly(s);
    }
  }

  /**
   * Handle a closeable resource inside <code>f</code> and ensure it gets closed properly.
   */
  public static <A, B extends Closeable> A withResource(B b, Function<B, A> f) {
    try {
      return f.apply(b);
    } finally {
      IoSupport.closeQuietly(b);
    }
  }

  /**
   * Handle a stream inside <code>f</code> and ensure that <code>s</code> gets closed properly.
   * <p/>
   * <strong>Please note:</strong> The outcome of <code>f</code> is wrapped into a some. Therefore <code>f</code> is
   * <em>not</em> allowed to return <code>null</code>. Use an <code>Option</code> instead and {@link org.opencastproject.util.data.Option#flatten() flatten}
   * the overall result.
   *
   * @return none, if the file does not exist
   */
  public static <A> Option<A> withFile(File file, Function2<InputStream, File, A> f) {
    InputStream s = null;
    try {
      s = new FileInputStream(file);
      return some(f.apply(s, file));
    } catch (FileNotFoundException ignore) {
      return none();
    } finally {
      IoSupport.closeQuietly(s);
    }
  }

  /**
   * Handle a stream inside <code>f</code> and ensure that <code>s</code> gets closed properly.
   *
   * @param s
   *          the stream creation function
   * @param toErr
   *          error handler transforming an exception into something else
   * @param f
   *          stream handler
   * @deprecated use {@link #withResource(org.opencastproject.util.data.Function0, org.opencastproject.util.data.Function, org.opencastproject.util.data.Function)} instead
   */
  public static <A, Err> Either<Err, A> withStream(Function0<InputStream> s, Function<Exception, Err> toErr,
                                                   Function<InputStream, A> f) {
    InputStream in = null;
    try {
      in = s.apply();
      return right(f.apply(in));
    } catch (Exception e) {
      return left(toErr.apply(e));
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  /**
   * Handle a closeable resource inside <code>f</code> and ensure that <code>r</code> gets closed properly.
   *
   * @param r
   *          resource creation function
   * @param toErr
   *          error handler transforming an exception into something else
   * @param f
   *          resource handler
   */
  public static <A, Err, B extends Closeable> Either<Err, A> withResource(Function0<B> r,
                                                                          Function<Exception, Err> toErr,
                                                                          Function<B, A> f) {
    B b = null;
    try {
      b = r.apply();
      return right(f.apply(b));
    } catch (Exception e) {
      return left(toErr.apply(e));
    } finally {
      IoSupport.closeQuietly(b);
    }
  }

  /**
   * Handle a stream inside <code>f</code> and ensure that <code>s</code> gets closed properly.
   *
   * @deprecated use {@link #withResource(java.io.Closeable, org.opencastproject.util.data.Function)} instead
   */
  public static <A> A withStream(OutputStream s, Function<OutputStream, A> f) {
    try {
      return f.apply(s);
    } finally {
      IoSupport.closeQuietly(s);
    }
  }

  /**
   * Handle multiple streams inside <code>f</code> and ensure that they get closed properly.
   */
  public static <A> A withStreams(InputStream[] in, OutputStream[] out, Function2<InputStream[], OutputStream[], A> f) {
    try {
      return f.apply(in, out);
    } finally {
      for (Closeable a : in) {
        IoSupport.closeQuietly(a);
      }
      for (Closeable a : out) {
        IoSupport.closeQuietly(a);
      }
    }
  }

  /**
   * Create a function that creates a {@link java.io.FileInputStream}.
   */
  public static Function0<InputStream> fileInputStream(final File a) {
    return new Function0.X<InputStream>() {
      @Override
      public InputStream xapply() throws Exception {
        return new FileInputStream(a);
      }
    };
  }
}
