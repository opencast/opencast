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
package org.opencastproject.caption.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Imports caption catalogs to a list of caption objects and exports these objects to catalog presentations.
 */
public interface CaptionConverter {

  /**
   * Imports captions to {@link List}. If caption format is capable of containing more than one language, language
   * parameter is used to define which captions are parsed.
   * 
   * @param inputStream
   *          stream from where captions are read
   * @param language
   *          (optional) captions' language
   * @return {@link List} List of captions
   * @throws IllegalCaptionFormatException
   *           if parser encounters an exception
   */
  List<Caption> importCaption(InputStream inputStream, String language) throws CaptionConverterException;

  /**
   * Exports caption collection. Language parameter is used to set language of the captions for those caption format
   * that are capable of storing information about language.
   * 
   * @param outputStream
   *          stream to which captions are written
   * @param captions
   *          collection to be exported
   * @param language
   *          (optional) captions' language
   * @throws IOException
   *           if exception occurs writing to output stream
   */
  void exportCaption(OutputStream outputStream, List<Caption> captions, String language) throws IOException;

  /**
   * Reads captions and return information about language if such information is available. Returns empty list
   * otherwise.
   * 
   * @param inputStream
   *          stream from where captions are read
   * @return Array containing languages in captions
   * @throws IllegalCaptionFormatException
   *           if parser encounters exception
   */
  String[] getLanguageList(InputStream inputStream) throws CaptionConverterException;

  /**
   * Get extension of specific caption format.
   * 
   * @return caption format extension
   */
  String getExtension();

}
