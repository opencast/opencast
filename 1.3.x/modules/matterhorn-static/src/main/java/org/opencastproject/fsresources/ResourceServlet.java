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
package org.opencastproject.fsresources;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves static content from a configured path on the filesystem. In production systems, this should be replaced with
 * apache httpd or another web server optimized for serving static content.
 */
public class ResourceServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(ResourceServlet.class);

  protected String root;
  protected String serverAlias;
  private static final String dateFormat = "yyyy-MM-dd HH:mm:ss Z";
  
  public ResourceServlet() {
  }

  public ResourceServlet(String alias, String filesystemDir) {
    root = filesystemDir;
    serverAlias = alias;
  }

  public void activate(ComponentContext cc) {
    if (root == null)
      root = (String) cc.getProperties().get("filesystemDir");
    if (serverAlias == null)
      serverAlias = (String) cc.getProperties().get("alias");

    if (serverAlias.charAt(0) != '/') {
      serverAlias = '/' + serverAlias;
    }

    File rootDir = new File(root);
    if (!rootDir.exists()) {
      if (!rootDir.mkdirs()) {
        logger.error("Unable to create directories for {}!", rootDir.getAbsolutePath());
        return;
      }
    }

    if (!(rootDir.isDirectory() || rootDir.isFile())) {
      throw new IllegalStateException("Unable to create servlet for " + serverAlias + " because "
              + rootDir.getAbsolutePath() + " is not a file or directory!");
    }
  }

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
    String normalized = path == null ? "/" : path.trim().replaceAll("/+", "/").replaceAll("\\.\\.", "");
    if (normalized != null && normalized.startsWith("/") && normalized.length() > 1) {
      normalized = normalized.substring(1);
    }

    File f = new File(root, normalized);
    if (f.isFile() && f.canRead()) {
      logger.debug("Serving static resource '{}'", f.getAbsolutePath());
      FileInputStream in = new FileInputStream(f);
      try {
        IOUtils.copyLarge(in, resp.getOutputStream());
      } finally {
        IOUtils.closeQuietly(in);
      }
    } else if (f.isDirectory() && f.canRead()) {
      logger.debug("Serving index page for '{}'", f.getAbsolutePath());
      PrintWriter out = resp.getWriter();
      resp.setContentType("text/html;charset=UTF-8");
      out.write("<html>");
      out.write("<head><title>File Index for " + normalized + "</title></head>");
      out.write("<body>");
      out.write("<pre>");
      SimpleDateFormat sdf = new SimpleDateFormat();
      sdf.applyPattern(dateFormat);
      for (File child : f.listFiles()) {
        StringBuffer sb = new StringBuffer();
        sb.append("<a href=\"");
        if (req.getRequestURL().charAt(req.getRequestURL().length() - 1) != '/') {
          sb.append(req.getRequestURL().append("/").append(child.getName()));
        } else {
          sb.append(req.getRequestURL().append(child.getName()));
        }
        sb.append("\">");
        sb.append(child.getName());
        sb.append("</a>\t");
        sb.append(formatLength(child.length()));
        sb.append("\t");
        sb.append(sdf.format(child.lastModified()));
        sb.append("\n");
        out.write(sb.toString());
      }
      out.write("</pre>");
      out.write("</body>");
      out.write("</html>");
    } else {
      logger.debug("Error state for '{}', returning HTTP 404", f.getAbsolutePath());
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  protected String formatLength(long length) {
    // FIXME: Why isn't there a library function for this?!
    // TODO: Make this better
    if (length > 1073741824.0) {
      return length / 1073741824 + " GB";
    } else if (length > 1048576.0) {
      return length / 1048576 + " MB";
    } else if (length > 1024.0) {
      return length / 1024 + " KB";
    } else {
      return length + " B";
    }
  }

}
