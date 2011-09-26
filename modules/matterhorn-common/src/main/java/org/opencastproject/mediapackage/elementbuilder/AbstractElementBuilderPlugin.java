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

package org.opencastproject.mediapackage.elementbuilder;

import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.UnknownFileTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

/**
 * This general inplementation of a media package element builder supports specialized implementations by providing
 * tests on the filename an mime type of the file in question.
 */
public abstract class AbstractElementBuilderPlugin implements MediaPackageElementBuilderPlugin {

  /** The registered mime types */
  protected List<MimeType> mimeTypes = null;

  /** The xpath facility */
  protected XPath xpath = XPathFactory.newInstance().newXPath();

  /** The builder's priority */
  protected int priority = -1;

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(AbstractElementBuilderPlugin.class);

  /**
   * Creates a new abstract element builder plugin.
   */
  public AbstractElementBuilderPlugin() {
  }

  /**
   * This is a convenience implementation for subclasses doing nothing.
   * 
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#init()
   */
  public void init() throws Exception {
  }

  /**
   * This is a convenience implementation for subclasses doing nothing.
   * 
   * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin#destroy()
   */
  public void destroy() {
  }

  /**
   * Returns -1 by default.
   */
  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  /**
   * Registers the given mime types with the media package element builder.
   * 
   * @param mimeType
   *          the type to register (e. g. text/xml)
   * @param description
   *          a description for the mime type
   * @throws UnknownFileTypeException
   *           if the mime type is not known
   */
  protected void registerMimeType(String mimeType, String description) throws UnknownFileTypeException {
    if (this.mimeTypes == null)
      this.mimeTypes = new ArrayList<MimeType>();
    MimeType m = MimeTypes.parseMimeType(mimeType);
    m.setDescription(description);
    this.mimeTypes.add(m);
    logger.debug("Processing of documents of type " + description + "(" + m + ") enabled");
  }

  /**
   * Returns <code>true</code> if the file's mime type could be detected, is known and supported.
   * 
   * @param file
   *          the file
   * @param mimeTypes
   *          the supported mime types
   * @return <code>true</code> if the file has a supported mime type
   * @throws IOException
   *           if the file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the file's mime type cannot be gathered or is unknown
   */
  protected boolean checkMimeType(File file, MimeType[] mimeTypes) throws IOException, UnknownFileTypeException {
    if (file == null)
      throw new IllegalArgumentException("File is null");
    else if (!file.isFile())
      return false;

    // Check mimetype
    if (mimeTypes != null) {
      if (mimeTypes.length > 0) {
        for (MimeType m : mimeTypes) {
          if (m.equals(MimeTypes.fromURL(file.toURI().toURL())))
            return true;
        }
      } else {
        return false;
      }
    }

    return false;
  }

  /**
   * Returns <code>true</code> if the file's mime type could be detected, is known and supported.
   * 
   * @param file
   *          the file
   * @return <code>true</code> if the file has a supported mime type
   * @throws IOException
   *           if the file cannot be accessed
   * @throws UnknownFileTypeException
   *           if the file's mime type cannot be gathered or is unknown
   */
  protected boolean checkMimeType(File file) throws IOException, UnknownFileTypeException {
    if (file == null)
      throw new IllegalArgumentException("File is null");
    else if (!file.isFile())
      return false;
    if (mimeTypes == null)
      throw new IllegalStateException("Mime types have not been initialized");

    // Check mimetype
    if (mimeTypes != null) {
      if (mimeTypes.size() > 0) {
        for (MimeType m : mimeTypes) {
          if (m.equals(MimeTypes.fromURL(file.toURI().toURL())))
            return true;
        }
      } else {
        return false;
      }
    }

    return false;
  }

  /**
   * Returns <code>true</code> if the file's name matches <code>filename</code>.
   * 
   * @param file
   *          the file
   * @param filename
   *          the filename to check
   * @return <code>true</code> if the filename matches
   * @throws IOException
   *           if the file cannot be accessed
   */
  protected boolean checkFilename(File file, String filename) throws IOException {
    if (file == null)
      throw new IllegalArgumentException("File is null");
    else if (!file.isFile())
      return false;

    // Check filename
    return (file.getName().equals(filename));
  }

  /**
   * Checks the file for its existence and validates it's checksum with the proviced values.
   * 
   * @param file
   *          the file to check
   * @param checksum
   *          the expected checksum
   * @return <code>true</code> if the file passed verification
   * @throws IOException
   *           if the file cannot be accessed
   * @throws NoSuchAlgorithmException
   *           if the checksum cannot be created
   */
  protected boolean verifyFileIntegrity(File file, Checksum checksum) throws IOException, NoSuchAlgorithmException {
    if (!file.exists())
      throw new IOException(file + " cannot be found");
    if (!file.isFile())
      throw new IOException(file + " is not a regular file");
    if (!file.canRead())
      throw new IOException(file + " cannot be accessed");

    Checksum c = Checksum.create(checksum.getType(), file);
    return c.equals(checksum);
  }

}
