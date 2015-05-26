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


package org.opencastproject.manager.system.configeditor;

import org.json.JSONException;
import org.json.JSONObject;



public class ConfigLine {

  private String line;

  private String key;
  private String value;

  private boolean isProperty;
  private boolean isCommentedProperty;

  public ConfigLine(String line) {

    this.line = line.trim();
    parseLine();

  }


  public ConfigLine(String key, String value, boolean enabled) {

    this.key = key;
    this.value = value;
    this.isProperty = true;
    this.isCommentedProperty = !enabled;
    this.line = key + "=" + value;

    if (!enabled) {
      this.line += "#";
    }

  }


  private void parseLine() {

    String uncommented = this.line.replaceFirst("#*", "");
    String[] lines = uncommented.split("=", 2);

    // property (key-value paar separated with "=")
    if (lines.length == 2) {

      this.key = lines[0].trim();
      this.value = lines[1].trim();

      this.isProperty = true;
      this.isCommentedProperty = this.line.startsWith("#") ? true : false;

    }
    // comment or uncommented text or empty line
    else {

      this.isCommentedProperty = false;
      this.isProperty = false;

      // comment uncommented text
      if (!this.line.equals("") && !this.line.startsWith("#")) {

        this.line = "#".concat(this.line);

      }
    }
  }


  public String toString() {

    if (!this.isProperty) {
      return this.line;
    }

    String retStr = this.key + "=" + this.value;

    if (this.isCommentedProperty) {
      retStr = "#".concat(retStr);
    }

    return retStr.replace("\\", "\\\n ");

  }

  public Object toJSON() throws JSONException {

    if (!this.isProperty) {
      return this.line;
    }


    JSONObject json = new JSONObject();
    json.put("key", this.key);
    json.put("value", this.value);
    json.put("enabled", !this.isCommentedProperty);

    return json;

  }


  public String getValue() {
    return this.value == null ? "" : this.value;
  }

  public String getKey() {
    return this.key == null ? "" : this.key;
  }

  public void setEnabled(boolean enabled) {
    if (this.isProperty) {
      this.isCommentedProperty = !enabled;
    }
  }

  public void update(String value) {
    if (this.isProperty) {
      this.value = value.trim();
    }
  }

}
