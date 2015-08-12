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

import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.XProperties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Serves static content from a configured path on the filesystem. In production systems, this should be replaced with
 * apache httpd or another web server optimized for serving static content.
 */
public class ResourceServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(ResourceServlet.class);

  protected String root;
  protected String serverAlias;
  protected DocumentBuilder builder = null;
  private static final String dateFormat = "yyyy-MM-dd HH:mm:ss Z";
  private SecurityService securityService = null;

  public ResourceServlet() {
  }

  public ResourceServlet(String alias, String filesystemDir) {
    root = filesystemDir;
    serverAlias = alias;
  }

  public SecurityService getSecurityService() {
    return securityService;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void activate(ComponentContext cc) throws ParserConfigurationException {
    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    XProperties props = new XProperties();
    props.setBundleContext(cc.getBundleContext());

    String rootKey = (String) cc.getProperties().get("rootKey");
    if (rootKey != null) {
      if (root == null)
        root = (String) cc.getProperties().get(rootKey);
      if (root == null) {
        logger.warn("No value for key " + rootKey
                + " found for this service.  Defaulting to value of org.opencastproject.download.directory.");
      }
    }

    if (root == null) {
      root = (String) cc.getBundleContext().getProperty("org.opencastproject.download.directory");
    }

    if (root == null) {
      throw new IllegalStateException("Unable to find root for servlet, please check your config files.");
    }

    if (serverAlias == null)
      serverAlias = (String) cc.getProperties().get("alias");

    // Get the interpreted values of the keys.
    props.put("root", root);
    root = props.getProperty("root");
    props.put("serverAlias", serverAlias);
    serverAlias = props.getProperty("serverAlias");

    if (serverAlias == null || StringUtils.isBlank(serverAlias)) {
      throw new IllegalStateException("Unable to create servlet, alias property is null");
    } else if (root == null) {
      throw new IllegalStateException("Unable to create servlet, root property is null");
    }

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
    logger.debug("Activating servlet with alias " + serverAlias + " on directory " + rootDir.getAbsolutePath());
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
    if (path == null) {
      path = "/";
    } else {
      // Replace duplicate slashes with a single slash, and remove .. from the listing
      path = path.trim().replaceAll("/+", "/").replaceAll("\\.\\.", "");
    }
    if (normalized != null && normalized.startsWith("/") && normalized.length() > 1) {
      normalized = normalized.substring(1);
    }

    File f = new File(root, normalized);
    boolean allowed = true;
    if (f.isFile() && f.canRead()) {
      allowed = checkDirectory(f.getParentFile());
      if (!allowed) {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }

      logger.debug("Serving static resource '{}'", f.getAbsolutePath());
      FileInputStream in = new FileInputStream(f);
      try {
        IOUtils.copyLarge(in, resp.getOutputStream());
      } finally {
        IOUtils.closeQuietly(in);
      }
    } else if (f.isDirectory() && f.canRead()) {
      allowed = checkDirectory(f);
      if (!allowed) {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }

      logger.debug("Serving index page for '{}'", f.getAbsolutePath());
      PrintWriter out = resp.getWriter();
      resp.setContentType("text/html;charset=UTF-8");
      out.write("<html>");
      out.write("<head><title>File Index for " + normalized + "</title></head>");
      out.write("<body>");
      out.write("<table>");
      SimpleDateFormat sdf = new SimpleDateFormat();
      sdf.applyPattern(dateFormat);
      for (File child : f.listFiles()) {

        if (child.isDirectory() && !checkDirectory(child)) {
          continue;
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<tr><td>");
        sb.append("<a href=\"");
        if (req.getRequestURL().charAt(req.getRequestURL().length() - 1) != '/') {
          sb.append(req.getRequestURL().append("/").append(child.getName()));
        } else {
          sb.append(req.getRequestURL().append(child.getName()));
        }
        sb.append("\">");
        sb.append(child.getName());
        sb.append("</a>");
        sb.append("</td><td>");
        sb.append(formatLength(child.length()));
        sb.append("</td><td>");
        sb.append(sdf.format(child.lastModified()));
        sb.append("</td>");
        sb.append("</tr>");
        out.write(sb.toString());
      }
      out.write("</table>");
      out.write("</body>");
      out.write("</html>");
    } else {
      logger.debug("Error state for '{}', returning HTTP 404", f.getAbsolutePath());
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  protected boolean checkDirectory(File directory) {
    // If security is off then everyone has access!
    if (securityService == null) {
      return true;
    }

    boolean allowed = false;
    File aclFile = null;
    try {
      String[] pathBits = directory.getAbsolutePath().split("" + File.separatorChar);
      aclFile = new File(directory, pathBits[pathBits.length - 1] + ".acl");
      allowed = isUserAllowed(aclFile);
    } catch (IOException e) {
      logger.debug("Unable to read file " + aclFile.getAbsolutePath() + ", denying access by default");
    } catch (SAXException e) {
      if (aclFile.isFile()) {
        logger.warn("Invalid XML in file " + aclFile.getAbsolutePath() + ", denying access by default");
      }
    } catch (XPathExpressionException e) {
      logger.error("Wrong xPath expression: {}", e);
    }
    return allowed;
  }

  protected boolean isUserAllowed(File aclFile) throws SAXException, IOException, XPathExpressionException {
    Document aclDoc = builder.parse(aclFile);
    XPath xPath = XPathFactory.newInstance().newXPath();
    NodeList roles = (NodeList) xPath.evaluate("//*[local-name() = 'role']", aclDoc, XPathConstants.NODESET);
    for (int i = 0; i < roles.getLength(); i++) {
      Node role = roles.item(i);
      for (Role userRole : securityService.getUser().getRoles()) {
        if (userRole.getName().equals(role.getTextContent())) {
          return true;
        }
      }
    }
    return false;
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
