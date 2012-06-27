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

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;

import de.schlichtherle.io.FileWriter;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Contains operations concerning IO.
 */
public final class IoSupport {

  /** the logging facility provided by log4j */
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
   * Closes a <code>StreamHelper</code> quietly so that no exceptions are thrown.
   * 
   * @param s
   *          maybe null
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
   *          the process
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
   *          the input stream
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
   *          The {@code URL} of the local file you wish to write to.
   * @param contents
   *          The contents of the file you wish to create.
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
   * @param file
   *          The {@code File} of the local file you wish to write to.
   * @param contents
   *          The contents of the file you wish to create.
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
   *          The {@code URL} to read the source data from.
   * @return A String containing the source data or null in the case of an error.
   */
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
}
