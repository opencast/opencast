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

import org.apache.commons.io.IOUtils;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Options;
import org.opencastproject.util.data.functions.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.opencastproject.util.MimeType.mimeType;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

/**
 * This class represents the mime type registry that is responsible for providing resolving mime types through all
 * system components.
 * <p>
 * The registry is initialized from the file <code>org.opencastproject.util.MimeTypes.xml</code>.
 */
public final class MimeTypes {
  /** Disallow construction of this utility class */
  private MimeTypes() {
  }

  public static final Pattern MIME_TYPE_PATTERN = Pattern.compile("([a-zA-Z0-9-]+)/([a-zA-Z0-9-+.]+)");

  /** Name of the mime type files */
  public static final String DEFINITION_FILE = "/org/opencastproject/util/MimeTypes.xml";

  /** The mime types */
  private static final List<MimeType> mimeTypes = new ArrayList<MimeType>();

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(MimeType.class);

  /** Common mime types */
  public static final MimeType XML;
  public static final MimeType TEXT;
  public static final MimeType JSON;
  public static final MimeType JPG;
  public static final MimeType MJPEG;
  public static final MimeType MPEG4;
  public static final MimeType MPEG4_AAC;
  public static final MimeType DV;
  public static final MimeType MJPEG2000;
  public static final MimeType MP3;
  public static final MimeType AAC;
  public static final MimeType CALENDAR;
  public static final MimeType ZIP;
  public static final MimeType JAR;

  // Initialize common mime types
  static {
    XML = MimeTypes.parseMimeType("text/xml");
    TEXT = MimeTypes.parseMimeType("text/plain");
    JSON = MimeTypes.parseMimeType("application/json");
    JPG = MimeTypes.parseMimeType("image/jpg");
    MJPEG = MimeTypes.parseMimeType("video/x-motion-jpeg");
    MPEG4 = MimeTypes.parseMimeType("video/mp4");
    MPEG4_AAC = MimeTypes.parseMimeType("video/x-m4v");
    DV = MimeTypes.parseMimeType("video/x-dv");
    MJPEG2000 = MimeTypes.parseMimeType("video/mj2");
    MP3 = MimeTypes.parseMimeType("audio/mpeg");
    AAC = MimeTypes.parseMimeType("audio/x-m4a");
    CALENDAR = MimeTypes.parseMimeType("text/calendar");
    ZIP = MimeTypes.parseMimeType("application/zip");
    JAR = MimeTypes.parseMimeType("application/java-archive");
    // initialize from file
    InputStream is = null;
    InputStreamReader isr = null;
    try {
      String definitions = null;
      is = MimeTypes.class.getResourceAsStream(DEFINITION_FILE);
      StringBuilder buf = new StringBuilder();
      if (is == null)
        throw new FileNotFoundException(DEFINITION_FILE);

      isr = new InputStreamReader(is);
      char[] chars = new char[1024];
      int count = 0;
      while ((count = isr.read(chars)) > 0) {
        for (int i = 0; i < count; i++) {
          buf.append(chars[i]);
        }
      }

      definitions = buf.toString();
      SAXParserFactory parserFactory = SAXParserFactory.newInstance();
      SAXParser parser = parserFactory.newSAXParser();
      DefaultHandler handler = new MimeTypeParser(mimeTypes);
      parser.parse(new InputSource(new StringReader(definitions)), handler);
    } catch (FileNotFoundException e) {
      logger.error("Error initializing mime type registry: definition file not found!");
    } catch (IOException e) {
      logger.error("Error initializing mime type registry: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      logger.error("Configuration error while parsing mime type registry: " + e.getMessage());
    } catch (SAXException e) {
      logger.error("Error parsing mime type registry: " + e.getMessage());
    } finally {
      IOUtils.closeQuietly(isr);
      IOUtils.closeQuietly(is);
    }
  }

  /** Get a mime type from the registry. */
  public static Option<MimeType> get(String type, String subtype) {
    for (MimeType t : mimeTypes) {
      if (t.getType().equals(type) && t.getSubtype().equals(subtype))
        return some(t);
    }
    return none();
  }

  /** Get a mime type from the registry or create a new one if not available. */
  public static MimeType getOrCreate(String type, String subtype) {
    for (MimeType t : get(type, subtype)) return t;
    return mimeType(type, subtype);
  }

  /**
   * Returns a mime type for the given type and subtype, e. g. <code>video/mj2</code>.
   *
   * @param mimeType
   *          the mime type
   * @return the corresponding mime type
   */
  public static MimeType parseMimeType(String mimeType) {
    final Matcher m = MIME_TYPE_PATTERN.matcher(mimeType);
    if (!m.matches())
      throw new IllegalArgumentException("Malformed mime type '" + mimeType + "'");
    final String type = m.group(1);
    final String subtype = m.group(2);
    for (MimeType t : mimeTypes) {
      if (t.getType().equals(type) && t.getSubtype().equals(subtype))
        return t;
    }
    return mimeType(type, subtype);
  }

  /**
   * Returns a mime type for the provided file suffix.
   * <p>
   * For example, if the suffix is <code>mj2</code>, the mime type will be that of a ISO Motion JPEG 2000 document.
   * <p>
   * If no mime type is found for the suffix, a <code>UnknownFileTypeException</code> is thrown.
   *
   * @param suffix
   *          the file suffix
   * @return the corresponding mime type
   * @throws UnknownFileTypeException
   *           if the suffix does not map to a mime type
   */
  public static MimeType fromSuffix(String suffix) throws UnknownFileTypeException {
    if (suffix == null)
      throw new IllegalArgumentException("Argument 'suffix' was null!");

    for (MimeType m : mimeTypes) {
      if (m.supportsSuffix(suffix)) return m;
    }
    throw new UnknownFileTypeException("File suffix '" + suffix + "' cannot be matched to any mime type");
  }

  /**
   * Returns a mime type for the provided file.
   * <p>
   * This method tries various ways to extract mime type information from the files name or its contents.
   * <p>
   * If no mime type can be derived from either the file name or its contents, a <code>UnknownFileTypeException</code>
   * is thrown.
   *
   * @param url
   *          the file
   * @return the corresponding mime type
   * @throws UnknownFileTypeException
   *           if the mime type cannot be derived from the file
   */
  public static MimeType fromURL(URL url) throws UnknownFileTypeException {
    if (url == null)
      throw new IllegalArgumentException("Argument 'url' is null");
    return fromString(url.getFile());
  }

  /**
   * Returns a mime type for the provided file.
   * <p>
   * This method tries various ways to extract mime type information from the files name or its contents.
   * <p>
   * If no mime type can be derived from either the file name or its contents, a <code>UnknownFileTypeException</code>
   * is thrown.
   *
   * @param uri
   *          the file
   * @return the corresponding mime type
   * @throws UnknownFileTypeException
   *           if the mime type cannot be derived from the file
   */
  public static MimeType fromURI(URI uri) throws UnknownFileTypeException {
    if (uri == null)
      throw new IllegalArgumentException("Argument 'uri' is null");
    return fromString(uri.getPath());
  }

  /**
   * Returns a mime type for the provided file name.
   * <p>
   * This method tries various ways to extract mime type information from the files name or its contents.
   * <p>
   * If no mime type can be derived from either the file name or its contents, a <code>UnknownFileTypeException</code>
   * is thrown.
   *
   * @param name
   *          the file
   * @return the corresponding mime type
   * @throws UnknownFileTypeException
   *           if the mime type cannot be derived from the file
   */
  public static MimeType fromString(String name) throws UnknownFileTypeException {
    if (name == null)
      throw new IllegalArgumentException("Argument 'name' is null");

    MimeType mimeType = null;

    // Extract suffix
    String filename = name;
    String suffix = null;
    int separatorPos = filename.lastIndexOf('.');
    if (separatorPos > 0 && separatorPos < filename.length() - 1) {
      suffix = filename.substring(separatorPos + 1);
    } else {
      throw new UnknownFileTypeException("Unable to get mime type without suffix");
    }

    // Try to get mime type for file suffix
    try {
      mimeType = fromSuffix(suffix);
      if (mimeType != null)
        return mimeType;
    } catch (UnknownFileTypeException e) {
      throw e;
    }

    // TODO
    // Try to match according to file contents
    // if (mimeType == null) {
    // for (MimeType m : mimeTypes_.values()) {
    // // TODO: Search file contents for mime type using magic bits
    // }
    // }

    throw new UnknownFileTypeException("File '" + name + "' cannot be matched to any mime type");
  }

  /**
   * Reads the mime type definitions from the xml file comming with this distribution.
   */
  private static class MimeTypeParser extends DefaultHandler {

    /** The mime types */
    private List<MimeType> registry = null;

    /** Element content */
    private StringBuffer content = new StringBuffer();

    /** Type */
    private String type = null;

    /** Description */
    private String description = null;

    /** Extensions, comma separated */
    private String extensions = null;

    /**
     * Creates a new mime type reader.
     *
     * @param registry
     *          the registry
     */
    MimeTypeParser(List<MimeType> registry) {
      this.registry = registry;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      super.characters(ch, start, length);
      content.append(ch, start, length);
    }

    /**
     * Returns the element content.
     *
     * @return the element content
     */
    private String getContent() {
      String str = content.toString();
      content = new StringBuffer();
      return str;
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
      super.endElement(uri, localName, name);

      if ("Type".equals(name)) {
        this.type = getContent();
        return;
      } else if ("Description".equals(name)) {
        this.description = getContent();
        return;
      }
      if ("Extensions".equals(name)) {
        this.extensions = getContent();
        return;
      } else if ("MimeType".equals(name)) {
        String[] t = type.split("/");
        MimeType mimeType = mimeType(t[0].trim(), t[1].trim(),
                                     mlist(extensions.split(",")).bind(Options.<String>asList().o(Strings.trimToNone)).value(),
                                     Collections.<MimeType>nil(),
                                     option(description), none(""), none(""));
        registry.add(mimeType);
      }
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
      super.warning(e);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      super.error(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      super.fatalError(e);
    }

  }

}
