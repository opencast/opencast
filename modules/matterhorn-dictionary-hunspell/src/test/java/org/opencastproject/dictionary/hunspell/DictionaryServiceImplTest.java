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

package org.opencastproject.dictionary.hunspell;

import org.junit.Assert;
import org.junit.Test;
import org.junit.BeforeClass;

public class DictionaryServiceImplTest {

  private static boolean hunspellInstalled = true;
  private static boolean hunspellEngDictAvailable = true;
  private static boolean hunspellDeuDictAvailable = true;

  @BeforeClass
  public static void testHunspell() {
    DictionaryServiceImpl service = new DictionaryServiceImpl();

    /* Check if hunspell is available */
    service.setCommand("-v");
    try {
      service.runHunspell("");
    } catch (Throwable t) {
      /* Seems like no hunspell is available */
      hunspellInstalled = false;
    }

    /* Check if the English dictionary is available */
    service.setCommand("-d en_US");
    try {
      service.runHunspell("");
    } catch (Throwable t) {
      /* Seems like no hunspell is available */
      hunspellEngDictAvailable = false;
    }

    /* Check if the German dictionary is available */
    service.setCommand("-d de_DE");
    try {
      service.runHunspell("");
    } catch (Throwable t) {
      /* Seems like no hunspell is available */
      hunspellDeuDictAvailable = false;
    }

  }

  @Test
  public void testSetBinary() throws Exception {
    DictionaryServiceImpl service = new DictionaryServiceImpl();
    String binary = "123";
    service.setBinary(binary);
    Assert.assertEquals(binary, service.getBinary());
  }

  @Test
  public void testSetCommand() throws Exception {
    DictionaryServiceImpl service = new DictionaryServiceImpl();
    String command = "123";
    service.setCommand(command);
    Assert.assertEquals(command, service.getCommand());
  }

  @Test
  public void testEmpty() throws Exception {
    if (hunspellEngDictAvailable) {
      DictionaryServiceImpl service = new DictionaryServiceImpl();
      service.setCommand("-d en_US -G");
      Assert.assertEquals(null, service.cleanUpText(""));
    }
  }

  @Test
  public void testCleanUp() throws Exception {
    if (hunspellEngDictAvailable) {
      DictionaryServiceImpl service = new DictionaryServiceImpl();
      service.setCommand("-d en_US -G");
      String in  = "This is a test sentence.";
      String out = "This is a test sentence";
      Assert.assertEquals(out, service.cleanUpText(in).getText());
    }
  }

  @Test
  public void testSpecialCharacters() throws Exception {
    if (hunspellDeuDictAvailable) {
      DictionaryServiceImpl service = new DictionaryServiceImpl();
      service.setCommand("-i utf-8 -d de_DE -G");
      String in  = "Ich hab' hier bloß ein Amt und keine Meinung.";
      String out = "Ich hab hier bloß ein Amt und keine Meinung.";
      Assert.assertEquals(out, service.cleanUpText(in).getText());
    }
  }


}
