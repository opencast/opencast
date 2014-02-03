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

package org.opencastproject.manager.system.configeditor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class Config {

  public static final String PATH_TO_CONFIGS = "etc";
  private LinkedList<ConfigLine> lines;
  private String relPath;
  private String rootPath;

  public Config(String rootPath, String relPath) throws IOException {

    this.relPath = relPath.replace("../", "").replace("./", "");

    if (rootPath == null) {
      this.rootPath = Config.PATH_TO_CONFIGS;
    }
    else {
      this.rootPath = rootPath;
    }

    parseConfigLines();

  }

  public String getRelPath() {
    return this.relPath;
  }

  public void setRelPath(String path) {
    this.relPath = path;
  }

  public String getRootPath() {
    return this.rootPath;
  }


  private void parseConfigLines() throws IOException {

    LinkedList<ConfigLine> lines = new LinkedList<ConfigLine>();

    FileReader fr = new FileReader(this.rootPath + this.relPath);
    BufferedReader br = new BufferedReader(fr);

    String line;
    String multiline = null;

    while ((line = br.readLine()) != null) {

      multiline = line.trim();

      // multiline (ends with "\")
      while (line.endsWith("\\")) {

        line = br.readLine().trim();
        multiline = multiline.concat(line);

      }

      lines.add(new ConfigLine(multiline));

    }

    br.close();
    fr.close();

    this.lines = lines;

  }


  public void save() throws IOException {

    FileWriter fw = new FileWriter(this.rootPath + this.relPath);
    BufferedWriter bw = new BufferedWriter(fw);

    for (ConfigLine cl : this.lines) {
      bw.write(cl.toString());
      bw.newLine();
    }

    bw.close();
    fw.close();

  }


  public boolean updateProperty(String key, String value, boolean enabled) {

    boolean success = false;

    for (ConfigLine cl : this.lines) {

      if (cl.getKey().equals(key)) {

        cl.update(value);
        cl.setEnabled(enabled);
        success = true;

      }
    }

    return success;
  }


  public boolean addProperty(String key, String value, boolean enabled, String prevKey) {

    ConfigLine cl = new ConfigLine(key, value, enabled);

    int index = 0;

    Iterator<ConfigLine> iter = this.lines.iterator();

    while (iter.hasNext() && !iter.next().getKey().equals(prevKey)) {

      index++;

    }

    this.lines.add(index + 1, cl);

    return true;

  }

  public String toJSON() throws JSONException {

    JSONObject json = new JSONObject();

    json.put("path", this.relPath);

    JSONArray lines = new JSONArray();

    for (ConfigLine cl : this.lines) {

      lines.put(cl.toJSON());

    }

    json.put("lines", lines);

    return json.toString();

  }


  public static String getConfigPathsAsJsonTree(String rootDir, String parentDir) throws IOException {

    if (rootDir == null) {
      rootDir = Config.PATH_TO_CONFIGS;
    }

    String jsonTree = "[";
    String tree = getConfigPathsAsTree(rootDir, "");

    jsonTree += tree.substring(0, tree.length() - 1);
    jsonTree += "]";

    return jsonTree;
  }


  private static String getConfigPathsAsTree(String dir, String parentDir) throws IOException {

    String tree = "";

    File folder = new File(dir);

    if (!"".equals(parentDir)) {
      parentDir = "/"  + parentDir;
    }

    for (final File fileEntry : folder.listFiles()) {

      if (fileEntry.isDirectory()) {

        String dirName = fileEntry.getName();

        if (!"dictionaries".equals(dirName) && !"security".equals(dirName)
            && !"branding".equals(dirName) && !"workflows".equals(dirName)) {

          tree += "{\"id\":\"/" + dirName + "\","
          + "\"parent_id\":\"" + parentDir + "\","
              + "\"type\":\"dir\"},";

          tree += getConfigPathsAsTree(fileEntry.getAbsolutePath(), dirName);
        }
      } else if (fileEntry.isFile()) {

        String fileName = fileEntry.getName();

        if (fileName.endsWith(".properties") || fileName.endsWith(".cfg")) {

          tree += "{\"id\":\"" + fileName + "\","
                  + "\"parent_id\":\"" + parentDir + "\","
                  + "\"type\":\"file\"},";

        }
      }
    }

    return tree;

  }
}
