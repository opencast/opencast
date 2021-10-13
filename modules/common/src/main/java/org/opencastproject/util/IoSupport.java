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

import static org.opencastproject.util.PathSupport.path;
import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.FnX;
import com.entwinemedia.fn.Prelude;
import com.entwinemedia.fn.Unit;
import com.google.common.io.Resources;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.util.Properties;

import de.schlichtherle.io.FileWriter;

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
   *          maybe null
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
   * Closes the processes input, output and error streams.
   *
   * @param process
   *          the process
   * @return <code>true</code> if the streams were closed
   */
  public static boolean closeQuietly(final Process process) {
    if (process != null) {
      closeQuietly(process.getInputStream());
      closeQuietly(process.getErrorStream());
      closeQuietly(process.getOutputStream());
      return true;
    }
    return false;
  }

  /**
   * Writes the contents variable to the {@code URL}. Note that the URL must be a local {@code URL}.
   *
   * @param file
   *          The {@code URL} of the local file you wish to write to.
   * @param contents
   *          The contents of the file you wish to create.
   * @throws IOException
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
   *          The {@code File} of the local file you wish to write to.
   * @param contents
   *          The contents of the file you wish to create.
   */
  public static void writeUTF8File(File file, String contents) throws IOException {
    writeUTF8File(file.getAbsolutePath(), contents);
  }

  /**
   * Writes the contents variable to the {@code File} located at the filename.
   *
   * @param filename
   *          The {@code File} of the local file you wish to write to.
   * @param contents
   *          The contents of the file you wish to create.
   */
  public static void writeUTF8File(String filename, String contents) throws IOException {
    FileWriter out = new FileWriter(filename);
    try {
      out.write(contents);
    } finally {
      closeQuietly(out);
    }
  }

  /**
   * Convenience method to read in a file from a local source.
   *
   * @param url
   *          The {@code URL} to read the source data from.
   * @return A String containing the source data or null in the case of an error.
   * @deprecated this method doesn't support UTF8 or handle HTTP response codes
   */
  @Deprecated
  public static String readFileFromURL(URL url) {
    return readFileFromURL(url, null);
  }

  /**
   * Convenience method to read in a file from either a remote or local source.
   *
   * @param url
   *          The {@code URL} to read the source data from.
   * @param trustedClient
   *          The {@code TrustedHttpClient} which should be used to communicate with the remote server. This can be null
   *          for local file reads.
   * @return A String containing the source data or null in the case of an error.
   * @deprecated this method doesn't support UTF8 or handle HTTP response codes
   */
  @Deprecated
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

      if (response != null) {
        try {
          trustedClient.close(response);
        } catch (IOException e) {
        }
      }
    }

    return sb.toString();
  }

  public static Properties loadPropertiesFromUrl(final URL url) {
    try {
      return loadPropertiesFromStream(url.openStream());
    } catch (IOException e) {
      return chuck(e);
    }
  }

  /** Load properties from a stream. Close the stream after reading. */
  public static Properties loadPropertiesFromStream(final InputStream stream) {
    return withResource(stream, new Function.X<InputStream, Properties>() {
      @Override
      public Properties xapply(InputStream in) throws Exception {
        final Properties p = new Properties();
        p.load(in);
        return p;
      }
    });
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
   * Handle a closeable resource inside <code>f</code> and ensure it gets closed properly.
   */
  public static <A, B extends Closeable> A withResource(B b, Fn<? super B, ? extends A> f) {
    try {
      return f.apply(b);
    } finally {
      IoSupport.closeQuietly(b);
    }
  }

  /**
   * Open a classpath resource using the class loader of the given class.
   *
   * @return an input stream to the resource wrapped in a Some or none if the resource cannot be found
   */
  public static Option<InputStream> openClassPathResource(String resource, Class<?> clazz) {
    return option(clazz.getResourceAsStream(resource));
  }

  /**
   * Open a classpath resource using the class loader of {@link IoSupport}.
   *
   * @see #openClassPathResource(String, Class)
   */
  public static Option<InputStream> openClassPathResource(String resource) {
    return openClassPathResource(resource, IoSupport.class);
  }

  /** Get a classpath resource as a file using the class loader of {@link IoSupport}. */
  public static Option<File> classPathResourceAsFile(String resource) {
    try {
      final URL res = IoSupport.class.getResource(resource);
      if (res != null) {
        return Option.some(new File(res.toURI()));
      } else {
        return Option.none();
      }
    } catch (URISyntaxException e) {
      return Option.none();
    }
  }

  /**
   * Load a classpath resource into a string using UTF-8 encoding and the class loader of the given class.
   *
   * @return the content of the resource wrapped in a Some or none in case of any error
   */
  public static Option<String> loadFileFromClassPathAsString(String resource, Class<?> clazz) {
    try {
      final URL url = clazz.getResource(resource);
      return url != null ? some(Resources.toString(clazz.getResource(resource), Charset.forName("UTF-8")))
              : none(String.class);
    } catch (IOException e) {
      return none();
    }
  }

  /**
   * Load a classpath resource into a string using the class loader of {@link IoSupport}.
   *
   * @see #loadFileFromClassPathAsString(String, Class)
   */
  public static Option<String> loadFileFromClassPathAsString(String resource) {
    return loadFileFromClassPathAsString(resource, IoSupport.class);
  }

  /**
   * Handle a stream inside <code>f</code> and ensure that <code>s</code> gets closed properly.
   * <p>
   * <strong>Please note:</strong> The outcome of <code>f</code> is wrapped into a some. Therefore <code>f</code> is
   * <em>not</em> allowed to return <code>null</code>. Use an <code>Option</code> instead and flatten the overall
   * result.
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
   * @deprecated use
   *             {@link #withResource(org.opencastproject.util.data.Function0, org.opencastproject.util.data.Function, org.opencastproject.util.data.Function)}
   *             instead
   */
  @Deprecated
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
          Function<Exception, Err> toErr, Function<B, A> f) {
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
  @Deprecated
  public static <A> A withStream(OutputStream s, Function<OutputStream, A> f) {
    try {
      return f.apply(s);
    } finally {
      IoSupport.closeQuietly(s);
    }
  }

  /** Function that reads an input stream into a string using utf-8 encoding. Stream does not get closed. */
  public static final Function<InputStream, String> readToString = new Function.X<InputStream, String>() {
    @Override
    public String xapply(InputStream in) throws IOException {
      return IOUtils.toString(in, "utf-8");
    }
  };

  /** Create a function that creates a {@link java.io.FileInputStream}. */
  public static Function0<InputStream> fileInputStream(final File a) {
    return new Function0.X<InputStream>() {
      @Override
      public InputStream xapply() throws Exception {
        return new FileInputStream(a);
      }
    };
  }

  /** Create a file from the list of path elements. */
  public static File file(String... pathElems) {
    return new File(path(pathElems));
  }

  /**
   * Run function <code>f</code> having exclusive read/write access to the given file.
   * <p>
   * Please note that the implementation uses Java NIO {@link java.nio.channels.FileLock} which only guarantees that two
   * Java processes cannot interfere with each other.
   * <p>
   * The implementation blocks until a lock can be acquired.
   *
   * @throws NotFoundException
   *            if the path to the file, to create a lock for, does not exist
   * @throws IOException
   *            if the file lock can not be created due to access limitations
   */
  public static synchronized <A> A locked(File file, Function<File, A> f) throws NotFoundException, IOException {
    final Effect0 key = acquireLock(file);
    try {
      return f.apply(file);
    } finally {
      key.apply();
    }
  }

  /**
   * Acquire a lock on a file. Return a key to release the lock.
   *
   * @return a key to release the lock
   *
   * @throws NotFoundException
   *            if the path to the file, to create a lock for, does not exist
   * @throws IOException
   *            if the file lock can not be created due to access limitations
   */
  private static Effect0 acquireLock(File file) throws NotFoundException, IOException {
    final RandomAccessFile raf;
    try {
      raf = new RandomAccessFile(file, "rw");
    } catch (FileNotFoundException e) {
      // this exception is thrown only if the directory path to the file isn't exist
      // make sure to create all parent directories before locking the file
      throw new NotFoundException("Error acquiring lock for " + file.getAbsolutePath(), e);
    }
    final FileLock lock = raf.getChannel().lock();
    return new Effect0() {
      @Override
      protected void run() {
        try {
          lock.release();
        } catch (IOException ignore) {
        }
        IoSupport.closeQuietly(raf);
      }
    };
  }

  /**
   * Serialize and deserialize an object. To test serializability.
   */
  public static <A extends Serializable> A serializeDeserialize(final A a) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      withResource(
              new ObjectOutputStream(out),
              new FnX<ObjectOutputStream, Unit>() {
                @Override public Unit applyX(ObjectOutputStream out) throws Exception {
                  out.writeObject(a);
                  return Unit.unit;
                }
              });
      return withResource(
              new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())),
              new FnX<ObjectInputStream, A>() {
                @Override public A applyX(ObjectInputStream in) throws Exception {
                  return (A) in.readObject();
                }
              });
    } catch (IOException e) {
      return Prelude.chuck(e);
    }
  }
}
