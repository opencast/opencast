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


package org.opencastproject.metadata.mpeg7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Parser implementation for mpeg-7 files. Note that this implementation does by far not cover the full mpeg-7 standard
 * but only deals with those parts relevant to matterhorn, mainly temporal decompositions.
 */
public class Mpeg7Parser extends DefaultHandler {

  /** the logging facility */
  private static final Logger logger = LoggerFactory.getLogger(Mpeg7Parser.class.getName());

  /** The current parser state */
  enum ParserState {
    Document, MultimediaContent, Segment, VideoText
  };

  /**
   * Number formatter, used to deal with relevance values in a locale
   * independent way
   */
  private static DecimalFormatSymbols standardSymbols = new DecimalFormatSymbols(Locale.US);

  /** The manifest */
  private Mpeg7CatalogImpl mpeg7Doc = null;

  /** The element content */
  private StringBuffer tagContent = new StringBuffer();

  /** The multimedia content */
  private MultimediaContentType multimediaContent = null;

  /** Current multimedia content type (audio, video, audiovisual) */
  private MultimediaContentType.Type contentType = null;

  /** The multimedia content identifier */
  private String contentId = null;

  /** The media locator */
  private MediaLocator mediaLocator = null;

  /** The content media time point (will usually refer to 0:00:00) */
  private MediaTimePoint contentTimePoint = null;

  /** The time point (relative to the content time point) */
  private MediaTimePoint mediaTimePoint = null;

  /** The duration */
  private MediaDuration mediaDuration = null;

  /** The media time and duration */
  private MediaTime mediaTime = null;

  /** The temporal decomposition container */
  private TemporalDecomposition<?> temporalDecomposition = null;

  /** The temporal segment */
  private Segment segment = null;

  /** The spatio temporal decomposition container */
  private SpatioTemporalDecomposition spatioTemporalDecomposition = null;

  /** The text annoation */
  private TextAnnotation textAnnotation = null;

  /** The videotext element */
  private VideoText videoText = null;

  /** The videotext text */
  private Textual textual = null;

  /** The current parser state */
  private Mpeg7Parser.ParserState state = ParserState.Document;

  /** Flag to check if this is not just an arbitrary xml document */
  private boolean isMpeg7 = false;

  private DecimalFormat floatFormat = new DecimalFormat();

  /**
   * Creates a new parser for mpeg-7 files.
   */
  public Mpeg7Parser() {
    floatFormat.setDecimalFormatSymbols(standardSymbols);
  }

  public Mpeg7Parser(Mpeg7CatalogImpl catalog) {
    this.mpeg7Doc = catalog;
    floatFormat.setDecimalFormatSymbols(standardSymbols);
  }

  /**
   * Parses the mpeg-7 catalog file and returns its object representation.
   *
   * @param is
   *          the input stream containing the catalog
   * @return the catalog representation
   * @throws ParserConfigurationException
   *           if setting up the parser failed
   * @throws SAXException
   *           if an error occured while parsing the document
   * @throws IOException
   *           if the file cannot be accessed in a proper way
   * @throws IllegalArgumentException
   *           if the provided file does not contain mpeg-7 data
   */
  public Mpeg7CatalogImpl parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
    if (mpeg7Doc == null)
      mpeg7Doc = new Mpeg7CatalogImpl();
    SAXParserFactory factory = SAXParserFactory.newInstance();
    // REPLAY does not use a DTD here
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    SAXParser parser = factory.newSAXParser();
    parser.parse(is, this);

    // Did we parse an mpeg-7 document?
    if (!isMpeg7)
      throw new IllegalArgumentException("Content of input stream is not mpeg-7");
    return mpeg7Doc;
  }

  /**
   * Read <code>type</code> attribute from track or catalog element.
   *
   * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
   *      org.xml.sax.Attributes)
   */
  @Override
  public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
    super.startElement(uri, localName, name, attributes);
    tagContent = new StringBuffer();

    // Make sure this is an mpeg-7 catalog
    // TODO: Improve this test, add namespace awareness
    if (!isMpeg7 && "Mpeg7".equals(name))
      isMpeg7 = true;

    // Handle parser state
    if ("MultimediaContent".equals(localName)) {
      state = ParserState.MultimediaContent;
    }

    // Content type
    if ("Audio".equals(localName) || "Video".equals(localName) || "AudioVisual".equals(localName)) {
      contentType = MultimediaContentType.Type.valueOf(localName);
      contentId = attributes.getValue("id");
      if (MultimediaContentType.Type.Audio.equals(contentType))
        multimediaContent = mpeg7Doc.addAudioContent(contentId, mediaTime, mediaLocator);
      else if (MultimediaContentType.Type.Video.equals(contentType))
        multimediaContent = mpeg7Doc.addVideoContent(contentId, mediaTime, mediaLocator);
      else if (MultimediaContentType.Type.AudioVisual.equals(contentType))
        multimediaContent = mpeg7Doc.addAudioVisualContent(contentId, mediaTime, mediaLocator);
    }

    // Temporal decomposition
    if ("TemporalDecomposition".equals(localName)) {
      String hasGap = attributes.getValue("gap");
      String isOverlapping = attributes.getValue("overlap");
      String criteria = attributes.getValue("criteria");
      if (!"temporal".equals(criteria))
        throw new IllegalStateException("Decompositions other than temporal are not supported");
      temporalDecomposition = multimediaContent.getTemporalDecomposition();
      temporalDecomposition.setGap("true".equals(hasGap));
      temporalDecomposition.setOverlapping("overlap".equals(isOverlapping));
    }

    // Segment
    if ("AudioSegment".equals(localName) || "VideoSegment".equals(localName) || "AudioVisualSegment".equals(localName)) {
      String segmentId = attributes.getValue("id");
      segment = temporalDecomposition.createSegment(segmentId);
      state = ParserState.Segment;
    }

    // TextAnnotation
    if ("TextAnnotation".equals(localName)) {
      String language = attributes.getValue("xml:lang");
      float confidence = -1.0f;
      float relevance = -1.0f;
      try {
        confidence = floatFormat.parse(attributes.getValue("confidence")).floatValue();
      } catch (Exception e) {
        confidence = -1.0f;
      }
      try {
        relevance = floatFormat.parse(attributes.getValue("relevance")).floatValue();
      } catch (Exception e) {
        relevance = -1.0f;
      }
      textAnnotation = segment.createTextAnnotation(confidence, relevance, language);
    }

    // Spatiotemporal decomposition
    if ("SpatioTemporalDecomposition".equals(localName)) {
      String hasGap = attributes.getValue("gap");
      String isOverlapping = attributes.getValue("overlap");
      if (!(segment instanceof VideoSegment))
        throw new IllegalStateException("Can't have a spatio temporal decomposition outside of a video segment");
      boolean gap = "true".equalsIgnoreCase(attributes.getValue("gap"));
      boolean overlap = "true".equalsIgnoreCase(attributes.getValue("overlap"));
      spatioTemporalDecomposition = ((VideoSegment) segment).createSpatioTemporalDecomposition(gap, overlap);
      spatioTemporalDecomposition.setGap("true".equals(hasGap));
      spatioTemporalDecomposition.setOverlapping("overlap".equals(isOverlapping));
    }

    // Video Text
    if ("VideoText".equals(localName)) {
      String id = attributes.getValue("id");
      videoText = new VideoTextImpl(id);
      state = ParserState.VideoText;
    }

    // Textual
    if ("Text".equals(localName)) {
      String language = attributes.getValue("xml:lang");
      textual = new TextualImpl();
      textual.setLanguage(language);
    }

  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    super.endElement(uri, localName, name);

    // Handle parser state
    if ("MultimediaContent".equals(localName))
      state = ParserState.Document;
    else if ("AudioSegment".equals(localName) || "VideoSegment".equals(localName)
            || "AudioVisualSegment".equals(localName))
      state = ParserState.MultimediaContent;

    // Media locator uri
    if ("MediaUri".equals(localName)) {
      MediaLocatorImpl locator = new MediaLocatorImpl();
      URI mediaUri = URI.create(getTagContent());
      locator.setMediaURI(mediaUri);
      if (ParserState.MultimediaContent.equals(state)) {
        multimediaContent.setMediaLocator(locator);
      }
    }

    // Media/Segment time
    if ("MediaTime".equals(localName)) {
      if (ParserState.MultimediaContent.equals(state)) {
        mediaTime = new MediaTimeImpl(mediaTimePoint, mediaDuration);
        multimediaContent.setMediaTime(mediaTime);
      } else if (ParserState.Segment.equals(state)) {
        mediaTime = new MediaTimeImpl(mediaTimePoint, mediaDuration);
        segment.setMediaTime(mediaTime);
      } else if (ParserState.VideoText.equals(state)) {
        SpatioTemporalLocator spatioTemporalLocator = new SpatioTemporalLocatorImpl(mediaTime);
        videoText.setSpatioTemporalLocator(spatioTemporalLocator);
      }
    }

    // Media/Segment time point
    if ("MediaTimePoint".equals(localName)) {
      mediaTimePoint = MediaTimePointImpl.parseTimePoint(getTagContent());
      if (ParserState.MultimediaContent.equals(state)) {
        contentTimePoint = mediaTimePoint;
      }
    }

    // Media/Segment time point
    if ("MediaRelTimePoint".equals(localName)) {
      MediaRelTimePointImpl tp = MediaRelTimePointImpl.parseTimePoint(getTagContent());
      mediaTimePoint = tp;
      if (ParserState.MultimediaContent.equals(state))
        contentTimePoint = tp;
      else if (ParserState.Segment.equals(state)) {
        tp.setReferenceTimePoint(contentTimePoint);
      }
    }

    // Media/Segment duration
    if ("MediaDuration".equals(localName)) {
      mediaDuration = MediaDurationImpl.parseDuration(getTagContent());
    }

    // Keyword
    if ("Keyword".equals(localName)) {
      KeywordAnnotation keyword = new KeywordAnnotationImpl(tagContent.toString());
      textAnnotation.addKeywordAnnotation(keyword);
    }

    // Free text
    if ("FreeTextAnnotation".equals(localName)) {
      FreeTextAnnotation freeText = new FreeTextAnnotationImpl(tagContent.toString());
      textAnnotation.addFreeTextAnnotation(freeText);
    }

    // Video Text
    if ("VideoText".equals(localName)) {
      spatioTemporalDecomposition.addVideoText(videoText);
    }

    // SpatioTemporalLocator
    if ("SpatioTemporalLocator".equals(localName)) {
      videoText.setSpatioTemporalLocator(new SpatioTemporalLocatorImpl(mediaTime));
    }

    // Videotext text
    if ("Text".equals(localName)) {
      textual.setText(tagContent.toString());
      videoText.setText(textual);
    }

    // Videotext bouding box
    if ("Box".equals(localName)) {
      String[] coords = tagContent.toString().trim().split(" ");
      if (coords.length != 4)
        throw new IllegalStateException("Box coordinates '" + tagContent + "' is malformatted");
      int[] coordsL = new int[4];
      for (int i = 0; i < 4; i++)
        try {
          coordsL[i] = (int) floatFormat.parse(coords[i]).floatValue();
        } catch (ParseException e) {
          throw new SAXException(e);
        }
      videoText.setBoundary(new Rectangle(coordsL[0], coordsL[1], (coordsL[2] - coordsL[0]), coordsL[3] - coordsL[1]));
    }

  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
   */
  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    super.characters(ch, start, length);
    tagContent.append(ch, start, length);
  }

  /**
   * Returns the element content.
   *
   * @return the element content
   */
  private String getTagContent() {
    String str = tagContent.toString().trim();
    return str;
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
   */
  @Override
  public void error(SAXParseException e) throws SAXException {
    logger.warn("Error while parsing mpeg-7 catalog: " + e.getMessage());
    super.error(e);
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
   */
  @Override
  public void fatalError(SAXParseException e) throws SAXException {
    logger.warn("Fatal error while parsing mpeg-7 catalog: " + e.getMessage());
    super.fatalError(e);
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
   */
  @Override
  public void warning(SAXParseException e) throws SAXException {
    logger.warn("Warning while parsing mpeg-7 catalog: " + e.getMessage());
    super.warning(e);
  }

}
