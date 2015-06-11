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

package org.opencastproject.manager.impl;

import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;
import org.opencastproject.manager.system.configeditor.Config;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;




public class ConfigTest {


  private Config config;

  @Test
  public void testConfigConstructor() throws ParseException {

    URL url = this.getClass().getResource("/configs");
    String absFile = url.getFile();

    try {
      Config config = new Config(absFile, "/system.properties");

      Assert.assertEquals(config.getRootPath(), absFile);
      Assert.assertEquals(config.getRelPath(), "/system.properties");

      String json = "{\"path\":\"/system.properties\",\"lines\":[\"#\",\"# Uncommented property.\",\"#\",\"\",{\"enabled\":true,\"value\":\"8080\",\"key\":\"org.osgi.service.http.port\"},\"\",\"#\",\"# Commented property\",\"#\",\"\",{\"enabled\":false,\"value\":\"commented.property.value\",\"key\":\"commented.property.name\"}]}";

      JSONParser parser = new JSONParser();
      JSONObject obj1 = (JSONObject) parser.parse(config.toJSON());
      JSONObject obj2 = (JSONObject) parser.parse(json);
      Assert.assertTrue(obj1.equals(obj2));

      this.config = config;

    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testUpdateProperty() throws ParseException {

    URL url = this.getClass().getResource("/configs");
    String absFile = url.getFile();

    try {
      this.config = new Config(absFile, "/system.properties");
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    // uncomment property
    this.config.updateProperty("commented.property.name", "commented.property.value", true);

    String json = "{\"path\":\"/system.properties\",\"lines\":[\"#\",\"# Uncommented property.\",\"#\",\"\",{\"enabled\":true,\"value\":\"8080\",\"key\":\"org.osgi.service.http.port\"},\"\",\"#\",\"# Commented property\",\"#\",\"\",{\"enabled\":true,\"value\":\"commented.property.value\",\"key\":\"commented.property.name\"}]}";

    try {
      JSONParser parser = new JSONParser();
      JSONObject obj1 = (JSONObject) parser.parse(config.toJSON());
      JSONObject obj2 = (JSONObject) parser.parse(json);
      Assert.assertTrue(obj1.equals(obj2));
    } catch (JSONException e) {
      e.printStackTrace();
    }

    // change prop value
    this.config.updateProperty("commented.property.name", "commented.property.new_value", true);

    json = "{\"path\":\"/system.properties\",\"lines\":[\"#\",\"# Uncommented property.\",\"#\",\"\",{\"enabled\":true,\"value\":\"8080\",\"key\":\"org.osgi.service.http.port\"},\"\",\"#\",\"# Commented property\",\"#\",\"\",{\"enabled\":true,\"value\":\"commented.property.new_value\",\"key\":\"commented.property.name\"}]}";

    try {
      JSONParser parser = new JSONParser();
      JSONObject obj1 = (JSONObject) parser.parse(config.toJSON());
      JSONObject obj2 = (JSONObject) parser.parse(json);
      Assert.assertTrue(obj1.equals(obj2));
    } catch (JSONException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testAddProperty() throws ParseException {

    URL url = this.getClass().getResource("/configs");
    String absFile = url.getFile();

    try {
      this.config = new Config(absFile, "/system.properties");
    } catch (IOException e1) {
      e1.printStackTrace();
    }

    // add property
    this.config.addProperty("new.property.name", "new.property.value", true, "org.osgi.service.http.port");

    String json = "{\"path\":\"/system.properties\",\"lines\":[\"#\",\"# Uncommented property.\",\"#\",\"\",{\"enabled\":true,\"value\":\"8080\",\"key\":\"org.osgi.service.http.port\"},{\"enabled\":true,\"value\":\"new.property.value\",\"key\":\"new.property.name\"},\"\",\"#\",\"# Commented property\",\"#\",\"\",{\"enabled\":false,\"value\":\"commented.property.value\",\"key\":\"commented.property.name\"}]}";

    try {
      JSONParser parser = new JSONParser();
      JSONObject obj1 = (JSONObject) parser.parse(config.toJSON());
      JSONObject obj2 = (JSONObject) parser.parse(json);
      Assert.assertTrue(obj1.equals(obj2));
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testSaveConfig() throws ParseException {

    String absFile = this.getClass().getResource("/temp").getFile();

    try {
      this.config = new Config(absFile, "/system.properties");
    } catch (IOException e1) {
      e1.printStackTrace();
    }

    // add property
    this.config.addProperty("new.property.name", "new.property.value", true, "org.osgi.service.http.port");


    try {

      // save config
      String newPath = "/system_new.properties";
      config.setRelPath(newPath);
      config.save();

      // read saved config
      config = new Config(absFile, newPath);

      // compare
      String json = "{\"path\":\"/system_new.properties\",\"lines\":[\"#\",\"# Uncommented property.\",\"#\",\"\",{\"enabled\":true,\"value\":\"8080\",\"key\":\"org.osgi.service.http.port\"},{\"enabled\":true,\"value\":\"new.property.value\",\"key\":\"new.property.name\"},\"\",\"#\",\"# Commented property\",\"#\",\"\",{\"enabled\":false,\"value\":\"commented.property.value\",\"key\":\"commented.property.name\"}]}";

      JSONParser parser = new JSONParser();
      JSONObject obj1 = (JSONObject) parser.parse(config.toJSON());
      JSONObject obj2 = (JSONObject) parser.parse(json);
      Assert.assertTrue(obj1.equals(obj2));

    } catch (JSONException e) {
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
