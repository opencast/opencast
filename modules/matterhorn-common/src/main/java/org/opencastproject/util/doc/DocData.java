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

package org.opencastproject.util.doc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * This is the document model class which is the basis for all doc data models
 *
 * @see DocRestData if you want to create rest endpoint docs
 */
public class DocData {
  private static final Logger logger = LoggerFactory.getLogger(DocData.class);
  protected static final String TEMPLATE_DEFAULT = "/ui/restdocs/template.xhtml";

  /**
   * This is the document meta data
   */
  protected Map<String, String> meta;
  protected List<String> notes;

  /**
   * Create a new DocData object
   *
   * @param name
   *          the name of the document (must be alphanumeric (includes _) and no spaces or special chars)
   * @param title
   *          [OPTIONAL] the title of the document
   * @param notes
   *          [OPTIONAL] an array of notes to add into the document
   */
  public DocData(String name, String title, String[] notes) {
    if (!isValidName(name)) {
      throw new IllegalArgumentException("name must be set and only alphanumeric");
    }
    if (isBlank(title)) {
      title = name;
    }
    this.meta = new LinkedHashMap<String, String>();
    this.meta.put("name", name);
    this.meta.put("title", title);
    // notes
    this.notes = new Vector<String>(3);
    if (notes != null && notes.length > 0) {
      for (int i = 0; i < notes.length; i++) {
        this.notes.add(notes[i]);
      }
    }
    logger.debug("Created new Doc: {}", name);
  }

  /**
   * @return the map version of the data in this doc data holder
   * @throws IllegalArgumentException
   *           if the data cannot be turned into a valid map
   */
  public Map<String, Object> toMap() {
    LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
    m.put("meta", this.meta);
    m.put("notes", this.notes);
    return m;
  }

  /**
   * @return the default template path for this type of document data
   */
  public String getDefaultTemplatePath() {
    return TEMPLATE_DEFAULT;
  }

  /**
   * Add a note to the document
   *
   * @param note
   *          the text of the note
   */
  public void addNote(String note) {
    if (note != null && !"".equals(note)) {
      this.notes.add(note);
    }
  }

  public static boolean isBlank(String str) {
    boolean blank = false;
    if (str == null || "".equals(str)) {
      blank = true;
    }
    return blank;
  }

  public static boolean isValidName(String name) {
    boolean valid = true;
    if (isBlank(name)) {
      valid = false;
    } else {
      if (!name.matches("^\\w+$")) {
        valid = false;
      }
    }
    return valid;
  }

  public String getMetaData(String key) {
    return meta.get(key);
  }

  public Map<String, String> getMeta() {
    return meta; // never null
  }

  public List<String> getNotes() {
    return notes; // never null
  }

}
