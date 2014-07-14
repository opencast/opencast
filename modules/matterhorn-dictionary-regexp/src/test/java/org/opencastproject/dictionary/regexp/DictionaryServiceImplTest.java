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
package org.opencastproject.dictionary.regexp;

import org.junit.Assert;
import org.junit.Test;

public class DictionaryServiceImplTest {

  @Test
  public void testSetPattern() throws Exception {
    DictionaryServiceImpl service = new DictionaryServiceImpl();
    String pattern = "123";
    service.setPattern(pattern);
    Assert.assertEquals(pattern, service.getPattern());
  }

  @Test
  public void testSetInvalidPattern() throws Exception {
    DictionaryServiceImpl service = new DictionaryServiceImpl();
    String pattern = service.getPattern();
    /* The service should fail to compile this */
    service.setPattern("*[[[[");
    Assert.assertEquals(pattern, service.getPattern());
  }

  @Test
  public void testEmpty() throws Exception {
    DictionaryServiceImpl service = new DictionaryServiceImpl();
    Assert.assertEquals(null, service.cleanUpText(""));
  }

  @Test
  public void testCleanUp() throws Exception {
    DictionaryServiceImpl service = new DictionaryServiceImpl();
    String in       = "This is a test sentence.";
    String out      = "This is a test sentence";
    Assert.assertEquals(out, service.cleanUpText(in).getText());
  }

  @Test
  public void testSpecialCharacters() throws Exception {
    DictionaryServiceImpl service = new DictionaryServiceImpl();
    String in = "Zwar weiß ich viel, doch möcht ich alles wissen.";
    /* This will match German special characters and basic punctuation */
    service.setPattern("[\\wßäöüÄÖÜ,.!]+");
    Assert.assertEquals(in, service.cleanUpText(in).getText());
  }


}
