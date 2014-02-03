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
package org.opencastproject.manager.impl;

import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;
import org.opencastproject.manager.system.configeditor.Config;



public class ConfigTest {
  
  
  private Config config;
  
  @Test
  public void testConfigConstructor() {
    
    URL url = this.getClass().getResource("/configs");
    String absFile = url.getFile();  
    
    try {
      Config config = new Config(absFile, "/system.properties");
      
      Assert.assertEquals(config.getRootPath(), absFile);
      Assert.assertEquals(config.getRelPath(), "/system.properties");
      
      String json = "{\"path\":\"/system.properties\",\"lines\":[\"#\",\"# Uncommented property.\",\"#\",\"\",{\"enabled\":true,\"value\":\"8080\",\"key\":\"org.osgi.service.http.port\"},\"\",\"#\",\"# Commented property\",\"#\",\"\",{\"enabled\":false,\"value\":\"commented.property.value\",\"key\":\"commented.property.name\"}]}";

      Assert.assertEquals(config.toJSON(), json);
      
      this.config = config;
      
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
  
  @Test
  public void testUpdateProperty() {
    
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
      Assert.assertEquals(config.toJSON(), json);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    
    // change prop value
    this.config.updateProperty("commented.property.name", "commented.property.new_value", true);
    
    json = "{\"path\":\"/system.properties\",\"lines\":[\"#\",\"# Uncommented property.\",\"#\",\"\",{\"enabled\":true,\"value\":\"8080\",\"key\":\"org.osgi.service.http.port\"},\"\",\"#\",\"# Commented property\",\"#\",\"\",{\"enabled\":true,\"value\":\"commented.property.new_value\",\"key\":\"commented.property.name\"}]}";

    try {
      Assert.assertEquals(config.toJSON(), json);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    
  }
  
  @Test
  public void testAddProperty() {
    
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

      Assert.assertEquals(config.toJSON(), json);
    
    } catch (JSONException e) {
      e.printStackTrace();
    }    
  }
  
  @Test
  public void testSaveConfig() {
    
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

      Assert.assertEquals(config.toJSON(), json);
    
    } catch (JSONException e) {
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
  }
}
