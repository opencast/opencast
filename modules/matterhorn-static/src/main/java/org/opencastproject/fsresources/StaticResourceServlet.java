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

package org.opencastproject.fsresources;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.zip.CRC32;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves static content from a configured path on the filesystem. In production systems, this should be replaced with
 * apache httpd or another web server optimized for serving static content.
 */
public class StaticResourceServlet extends HttpServlet {

  /** The serialization UID */
  private static final long serialVersionUID = 1L;
  /** Full range marker. */
  private static final ArrayList<Range> FULL_RANGE;
  /** The mime types map */
  private static final MimetypesFileTypeMap MIME_TYPES_MAP;
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(StaticResourceServlet.class);

  /** static initializer */
  static {
    FULL_RANGE = new ArrayList<Range>();
    MIME_TYPES_MAP = new MimetypesFileTypeMap();
  }
  /** The filesystem directory to serve files fro */
  protected String distributionDirectory;

  /**
   * No-arg constructor
   */
  public StaticResourceServlet() {
  }

  /**
   * OSGI Activation callback
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    if (cc != null) {
      String ccDistributionDirectory = cc.getBundleContext().getProperty("org.opencastproject.download.directory");
      logger.info("serving static files from '{}'", ccDistributionDirectory);
      if (ccDistributionDirectory != null) {
        this.distributionDirectory = ccDistributionDirectory;
      }
    }

    if (distributionDirectory == null) {
      distributionDirectory = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator
              + "static";
    }

    InputStream is = this.getClass().getResourceAsStream("/META-INF/mime.types");
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        MIME_TYPES_MAP.addMimeTypes(line);
      }
    } catch (IOException ex) {
      java.util.logging.Logger.getLogger(StaticResourceServlet.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * OSGI Deactivation callback
   */
  public void deactivate() {
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    logger.debug("Looking for static resource '{}'", req.getRequestURI());
    String path = req.getPathInfo();
    if (path == null) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    String normalized = path.trim().replaceAll("/+", "/").replaceAll("\\.\\.", "");
    if (normalized != null && normalized.startsWith("/") && normalized.length() > 1) {
      normalized = normalized.substring(1);
    }

    File f = new File(distributionDirectory, normalized);
    String eTag = null;
    if (f.isFile() && f.canRead()) {
      logger.debug("Serving static resource '{}'", f.getAbsolutePath());
      eTag = computeEtag(f);
      if (eTag.equals(req.getHeader("If-None-Match"))) {
        resp.setStatus(304);
        return;
      }
      resp.setHeader("ETag", eTag);
      String contentType = MIME_TYPES_MAP.getContentType(f);
      if (!"application/octet-stream".equals(contentType)) {
        resp.setContentType(contentType);
      }
      resp.setHeader("Content-Length", Long.toString(f.length()));
      resp.setDateHeader("Last-Modified", f.lastModified());

      resp.setHeader("Accept-Ranges", "bytes");
      ArrayList<Range> ranges = parseRange(req, resp, eTag, f.lastModified(), f.length());

      if ((((ranges == null) || (ranges.isEmpty())) && (req.getHeader("Range") == null)) || (ranges == FULL_RANGE)) {
        IOException e = copyRange(new FileInputStream(f), resp.getOutputStream(), 0, f.length());
        if (e != null) {
          try {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
          } catch (IOException e1) {
            logger.warn("unable to send http 500 error: {}", e1);
            return;
          }
        }
      } else {
        if ((ranges == null) || (ranges.isEmpty())) {
          return;
        }
        if (ranges.size() == 1) {
          Range range = ranges.get(0);
          resp.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length);
          long length = range.end - range.start + 1;
          if (length < Integer.MAX_VALUE) {
            resp.setContentLength((int) length);
          } else {
            // Set the content-length as String to be able to use a long
            resp.setHeader("content-length", "" + length);
          }
          try {
            resp.setBufferSize(2048);
          } catch (IllegalStateException e) {
            logger.debug(e.getMessage(), e);
          }
          resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
          IOException e = copyRange(new FileInputStream(f), resp.getOutputStream(), range.start, range.end);
          if (e != null) {
            try {
              resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
              return;
            } catch (IOException e1) {
              logger.warn("unable to send http 500 error: {}", e1);
              return;
            }
          }
        } else {
          resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
          resp.setContentType("multipart/byteranges; boundary=" + mimeSeparation);
          try {
            resp.setBufferSize(2048);
          } catch (IllegalStateException e) {
            logger.debug(e.getMessage(), e);
          }
          copy(f, resp.getOutputStream(), ranges.iterator(), contentType);
        }
      }
    } else {
      logger.debug("unable to find file '{}', returning HTTP 404");
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  /**
   * Computes an etag for a file using the filename, last modified, and length of the file.
   *
   * @param file
   *          the file
   * @return the etag
   */
  protected String computeEtag(File file) {
    CRC32 crc = new CRC32();
    crc.update(file.getName().getBytes());
    checksum(file.lastModified(), crc);
    checksum(file.length(), crc);
    return Long.toString(crc.getValue());
  }

  private static void checksum(long l, CRC32 crc) {
    for (int i = 0; i < 8; i++) {
      crc.update((int) (l & 0x000000ff));
      l >>= 8;
    }
  }

  protected void copy(File f, ServletOutputStream out, Iterator<Range> ranges, String contentType) throws IOException {
    IOException exception = null;
    while ((exception == null) && (ranges.hasNext())) {
      Range currentRange = ranges.next();
      // Writing MIME header.
      out.println();
      out.println("--" + mimeSeparation);
      if (contentType != null) {
        out.println("Content-Type: " + contentType);
      }
      out.println("Content-Range: bytes " + currentRange.start + "-" + currentRange.end + "/" + currentRange.length);
      out.println();

      // Printing content
      InputStream in = new FileInputStream(f);
      exception = copyRange(in, out, currentRange.start, currentRange.end);
      in.close();
    }
    out.println();
    out.print("--" + mimeSeparation + "--");
    // Rethrow any exception that has occurred
    if (exception != null) {
      throw exception;
    }
  }
  /**
   * MIME multipart separation string
   */
  protected static final String mimeSeparation = "MATTERHORN_MIME_BOUNDARY";

  /**
   * Parse the range header.
   *
   * @param req
   *          The servlet request we are processing
   * @param response
   *          The servlet response we are creating
   * @return Vector of ranges
   */
  protected ArrayList<Range> parseRange(HttpServletRequest req, HttpServletResponse response, String eTag,
          long lastModified, long fileLength) throws IOException {

    // Checking If-Range
    String headerValue = req.getHeader("If-Range");
    if (headerValue != null) {
      long headerValueTime = (-1L);
      try {
        headerValueTime = req.getDateHeader("If-Range");
      } catch (IllegalArgumentException e) {
        logger.debug(e.getMessage(), e);
      }

      if (headerValueTime == (-1L)) {
        // If the ETag the client gave does not match the entity
        // etag, then the entire entity is returned.
        if (!eTag.equals(headerValue.trim())) {
          return FULL_RANGE;
        }
      } else {
        // If the timestamp of the entity the client got is older than
        // the last modification date of the entity, the entire entity
        // is returned.
        if (lastModified > (headerValueTime + 1000)) {
          return FULL_RANGE;
        }
      }
    }

    if (fileLength == 0) {
      return null;
    }

    // Retrieving the range header (if any is specified
    String rangeHeader = req.getHeader("Range");

    if (rangeHeader == null) {
      return null;
    }
    // bytes is the only range unit supported (and I don't see the point
    // of adding new ones).
    if (!rangeHeader.startsWith("bytes")) {
      response.addHeader("Content-Range", "bytes */" + fileLength);
      response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
      return null;
    }

    rangeHeader = rangeHeader.substring(6);

    // Vector which will contain all the ranges which are successfully
    // parsed.
    ArrayList<Range> result = new ArrayList<Range>();
    StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");

    // Parsing the range list
    while (commaTokenizer.hasMoreTokens()) {
      String rangeDefinition = commaTokenizer.nextToken().trim();
      Range currentRange = new Range();
      currentRange.length = fileLength;
      int dashPos = rangeDefinition.indexOf('-');
      if (dashPos == -1) {
        response.addHeader("Content-Range", "bytes */" + fileLength);
        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        return null;
      }
      if (dashPos == 0) {
        try {
          long offset = Long.parseLong(rangeDefinition);
          currentRange.start = fileLength + offset;
          currentRange.end = fileLength - 1;
        } catch (NumberFormatException e) {
          response.addHeader("Content-Range", "bytes */" + fileLength);
          response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
          return null;
        }
      } else {
        try {
          currentRange.start = Long.parseLong(rangeDefinition.substring(0, dashPos));
          if (dashPos < rangeDefinition.length() - 1) {
            currentRange.end = Long.parseLong(rangeDefinition.substring(dashPos + 1, rangeDefinition.length()));
          } else {
            currentRange.end = fileLength - 1;
          }
        } catch (NumberFormatException e) {
          response.addHeader("Content-Range", "bytes */" + fileLength);
          response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
          return null;
        }
      }
      if (!currentRange.validate()) {
        response.addHeader("Content-Range", "bytes */" + fileLength);
        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        return null;
      }
      result.add(currentRange);
    }
    return result;
  }

  /**
   * Copy the contents of the specified input stream to the specified output stream, and ensure that both streams are
   * closed before returning (even in the face of an exception).
   *
   * @param istream
   *          The input stream to read from
   * @param ostream
   *          The output stream to write to
   * @param start
   *          Start of the range which will be copied
   * @param end
   *          End of the range which will be copied
   * @return Exception which occurred during processing
   */
  protected IOException copyRange(InputStream istream, ServletOutputStream ostream, long start, long end) {
    logger.debug("Serving bytes:{}-{}", start, end);
    try {
      istream.skip(start);
    } catch (IOException e) {
      return e;
    }
    // MH-10447, fix for files of size 2048*C bytes
    long bytesToRead = end - start + 1;
    byte[] buffer = new byte[2048];
    int len = buffer.length;
    try {
      len = (int) bytesToRead % buffer.length;
      if (len > 0) {
        len = istream.read(buffer, 0, len);
        if (len > 0) {
          // This test coud actually be "if (len != -1)"
          ostream.write(buffer, 0, len);
          bytesToRead -= len;
          if (bytesToRead == 0)
            return null;
        } else
          return null;
      }

      for (len = istream.read(buffer); len > 0; len = istream.read(buffer)) {
        ostream.write(buffer, 0, len);
        bytesToRead -= len;
        if (bytesToRead < 1)
          break;
      }
    } catch (IOException e) {
      return e;
    }
    return null;
  }

  protected class Range {

    protected long start;
    protected long end;
    protected long length;

    /**
     * Validate range.
     */
    public boolean validate() {
      if (end >= length) {
        end = length - 1;
      }
      return ((start >= 0) && (end >= 0) && (start <= end) && (length > 0));
    }

    public void recycle() {
      start = 0;
      end = 0;
      length = 0;
    }
  }
}
