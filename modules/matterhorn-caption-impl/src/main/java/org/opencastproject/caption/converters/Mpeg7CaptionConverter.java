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
package org.opencastproject.caption.converters;

import org.opencastproject.caption.api.Caption;
import org.opencastproject.caption.api.CaptionConverter;
import org.opencastproject.caption.api.CaptionConverterException;
import org.opencastproject.caption.api.IllegalTimeFormatException;
import org.opencastproject.caption.api.Time;
import org.opencastproject.caption.impl.CaptionImpl;
import org.opencastproject.caption.impl.TimeImpl;
import org.opencastproject.metadata.mpeg7.Audio;
import org.opencastproject.metadata.mpeg7.AudioSegment;
import org.opencastproject.metadata.mpeg7.FreeTextAnnotation;
import org.opencastproject.metadata.mpeg7.FreeTextAnnotationImpl;
import org.opencastproject.metadata.mpeg7.MediaDuration;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimeImpl;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.TextAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * This is converter for Mpeg7 caption format.
 */
public class Mpeg7CaptionConverter implements CaptionConverter {

  /** File extension for mpeg 7 catalogs */
  private static final String EXTENSION = "xml";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(Mpeg7CaptionConverter.class);

  /**
   * @see org.opencastproject.caption.api.CaptionConverter#importCaption(java.io.InputStream, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<Caption> importCaption(InputStream inputStream, String language) throws CaptionConverterException {
    List<Caption> captions = new ArrayList<Caption>();
    Mpeg7Catalog catalog = new Mpeg7CatalogImpl(inputStream);
    Iterator<Audio> audioContentIterator = catalog.audioContent();
    if (audioContentIterator == null)
      return captions;
    content: while (audioContentIterator.hasNext()) {
      Audio audioContent = audioContentIterator.next();
      TemporalDecomposition<AudioSegment> audioSegments = (TemporalDecomposition<AudioSegment>) audioContent
              .getTemporalDecomposition();
      Iterator<AudioSegment> audioSegmentIterator = audioSegments.segments();
      if (audioSegmentIterator == null)
        continue content;
      while (audioSegmentIterator.hasNext()) {
        AudioSegment segment = audioSegmentIterator.next();
        Iterator<TextAnnotation> annotationIterator = segment.textAnnotations();
        if (annotationIterator == null)
          continue content;
        while (annotationIterator.hasNext()) {
          TextAnnotation annotation = annotationIterator.next();
          if (!annotation.getLanguage().equals(language)) {
            logger.debug("Skipping audio content '{}' because of language mismatch", audioContent.getId());
            continue content;
          }

          List<String> captionLines = new ArrayList<String>();
          Iterator<FreeTextAnnotation> freeTextAnnotationIterator = annotation.freeTextAnnotations();
          if (freeTextAnnotationIterator == null)
            continue;

          while (freeTextAnnotationIterator.hasNext()) {
            FreeTextAnnotation freeTextAnnotation = freeTextAnnotationIterator.next();
            captionLines.add(freeTextAnnotation.getText());
          }

          MediaTime segmentTime = segment.getMediaTime();
          MediaTimePoint stp = segmentTime.getMediaTimePoint();
          MediaDuration d = segmentTime.getMediaDuration();

          Calendar startCalendar = Calendar.getInstance();
          int millisAtStart = (int) (stp.getTimeInMilliseconds() - (((stp.getHour() * 60 + stp.getMinutes()) * 60 + stp
                  .getSeconds()) * 1000));
          int millisAtEnd = (int) (d.getDurationInMilliseconds() - (((d.getHours() * 60 + d.getMinutes()) * 60 + d
                  .getSeconds()) * 1000));

          startCalendar.set(Calendar.HOUR, stp.getHour());
          startCalendar.set(Calendar.MINUTE, stp.getMinutes());
          startCalendar.set(Calendar.SECOND, stp.getSeconds());
          startCalendar.set(Calendar.MILLISECOND, millisAtStart);

          startCalendar.add(Calendar.HOUR, d.getHours());
          startCalendar.add(Calendar.MINUTE, d.getMinutes());
          startCalendar.add(Calendar.SECOND, d.getSeconds());
          startCalendar.set(Calendar.MILLISECOND, millisAtEnd);

          try {
            Time startTime = new TimeImpl(stp.getHour(), stp.getMinutes(), stp.getSeconds(), millisAtStart);
            Time endTime = new TimeImpl(startCalendar.get(Calendar.HOUR), startCalendar.get(Calendar.MINUTE),
                    startCalendar.get(Calendar.SECOND), startCalendar.get(Calendar.MILLISECOND));
            Caption caption = new CaptionImpl(startTime, endTime, captionLines.toArray(new String[captionLines.size()]));
            captions.add(caption);
          } catch (IllegalTimeFormatException e) {
            logger.warn("Error setting caption time: {}", e.getMessage());
          }
        }
      }
    }

    return captions;
  }

  /**
   * @see org.opencastproject.caption.api.CaptionConverter#exportCaption(java.io.OutputStream, java.lang.String)
   */
  @Override
  public void exportCaption(OutputStream outputStream, List<Caption> captions, String language) throws IOException {

    Mpeg7Catalog mpeg7 = Mpeg7CatalogImpl.newInstance();

    MediaTime mediaTime = new MediaTimeImpl(0, 0);
    Audio audioContent = mpeg7.addAudioContent("captions", mediaTime, null);
    @SuppressWarnings("unchecked")
    TemporalDecomposition<AudioSegment> captionDecomposition = (TemporalDecomposition<AudioSegment>) audioContent
            .getTemporalDecomposition();

    int segmentCount = 0;
    for (Caption caption : captions) {

      // Get all the words/parts for the transcript
      String[] words = caption.getCaption();
      if (words.length == 0)
        continue;

      // Create a new segment
      AudioSegment segment = captionDecomposition.createSegment("segment-" + segmentCount++);

      Time captionST = caption.getStartTime();
      Time captionET = caption.getStopTime();

      // Calculate start time
      Calendar startTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      startTime.setTimeInMillis(0);
      startTime.add(Calendar.HOUR_OF_DAY, captionST.getHours());
      startTime.add(Calendar.MINUTE, captionST.getMinutes());
      startTime.add(Calendar.SECOND, captionST.getSeconds());
      startTime.add(Calendar.MILLISECOND, captionST.getMilliseconds());

      // Calculate end time
      Calendar endTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      endTime.setTimeInMillis(0);
      endTime.add(Calendar.HOUR_OF_DAY, captionET.getHours());
      endTime.add(Calendar.MINUTE, captionET.getMinutes());
      endTime.add(Calendar.SECOND, captionET.getSeconds());
      endTime.add(Calendar.MILLISECOND, captionET.getMilliseconds());

      long startTimeInMillis = startTime.getTimeInMillis();
      long endTimeInMillis = endTime.getTimeInMillis();

      long duration = endTimeInMillis - startTimeInMillis;

      segment.setMediaTime(new MediaTimeImpl(startTimeInMillis, duration));
      TextAnnotation textAnnotation = segment.createTextAnnotation(0, 0, language);

      // Collect all the words in the segment
      StringBuffer captionLine = new StringBuffer();

      // Add each words/parts as segment to the catalog
      for (String word : words) {
        if (captionLine.length() > 0)
          captionLine.append(' ');
        captionLine.append(word);
      }

      // Append the text to the annotation
      textAnnotation.addFreeTextAnnotation(new FreeTextAnnotationImpl(captionLine.toString()));

    }

    Transformer tf = null;
    try {
      tf = TransformerFactory.newInstance().newTransformer();
      DOMSource xmlSource = new DOMSource(mpeg7.toXml());
      tf.transform(xmlSource, new StreamResult(outputStream));
    } catch (TransformerConfigurationException e) {
      logger.warn("Error serializing mpeg7 captions catalog: {}", e.getMessage());
      throw new IOException(e);
    } catch (TransformerFactoryConfigurationError e) {
      logger.warn("Error serializing mpeg7 captions catalog: {}", e.getMessage());
      throw new IOException(e);
    } catch (TransformerException e) {
      logger.warn("Error serializing mpeg7 captions catalog: {}", e.getMessage());
      throw new IOException(e);
    } catch (ParserConfigurationException e) {
      logger.warn("Error serializing mpeg7 captions catalog: {}", e.getMessage());
      throw new IOException(e);
    }
  }

  /**
   * @see org.opencastproject.caption.api.CaptionConverter#getLanguageList(java.io.InputStream)
   */
  @SuppressWarnings("unchecked")
  @Override
  public String[] getLanguageList(InputStream inputStream) throws CaptionConverterException {
    Set<String> languages = new HashSet<String>();

    Mpeg7Catalog catalog = new Mpeg7CatalogImpl(inputStream);
    Iterator<Audio> audioContentIterator = catalog.audioContent();
    if (audioContentIterator == null)
      return languages.toArray(new String[languages.size()]);
    content: while (audioContentIterator.hasNext()) {
      Audio audioContent = audioContentIterator.next();
      TemporalDecomposition<AudioSegment> audioSegments = (TemporalDecomposition<AudioSegment>) audioContent
              .getTemporalDecomposition();
      Iterator<AudioSegment> audioSegmentIterator = audioSegments.segments();
      if (audioSegmentIterator == null)
        continue content;
      while (audioSegmentIterator.hasNext()) {
        AudioSegment segment = audioSegmentIterator.next();
        Iterator<TextAnnotation> annotationIterator = segment.textAnnotations();
        if (annotationIterator == null)
          continue content;
        while (annotationIterator.hasNext()) {
          TextAnnotation annotation = annotationIterator.next();
          String language = annotation.getLanguage();
          if (language != null)
            languages.add(language);
        }
      }
    }

    return languages.toArray(new String[languages.size()]);
  }

  /**
   * @see org.opencastproject.caption.api.CaptionConverter#getExtension()
   */
  @Override
  public String getExtension() {
    return EXTENSION;
  }

}
