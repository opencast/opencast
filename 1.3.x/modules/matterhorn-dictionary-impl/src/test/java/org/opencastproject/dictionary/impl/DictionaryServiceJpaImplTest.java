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
package org.opencastproject.dictionary.impl;

import org.opencastproject.dictionary.api.DictionaryService.DICT_TOKEN;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictionaryServiceJpaImplTest {
  private ComboPooledDataSource pooledDataSource = null;

  private DictionaryServiceJpaImpl service = null;

  @Before
  public void setUp() throws Exception {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    service = new DictionaryServiceJpaImpl();
    service.setPersistenceProperties(props);
    service.setPersistenceProvider(new PersistenceProvider());
    service.activate(null);
  }

  @After
  public void tearDown() throws Exception {
    service.deactivate();
    pooledDataSource.close();
  }
  
  @Test
  public void testAddAndRetrieveWords() throws Exception {
    // Add a word that exists in just one language, and a word that exists in multiple languages
    service.addWord("foo", "en");
    service.addWord("foo", "de");
    service.addWord("bar", "de");

    Assert.assertTrue(service.isWord("foo"));
    Assert.assertTrue(service.isWord("foo", "en"));
    Assert.assertTrue(service.isWord("foo", "de"));
    
    Assert.assertTrue(service.isWord("bar"));
    Assert.assertFalse(service.isWord("bar", "en"));
    Assert.assertTrue(service.isWord("bar", "de"));
    
    // Make sure we can count the number of languages we have
    List<String> allLanguages = Arrays.asList(service.getLanguages());
    Assert.assertEquals(2, allLanguages.size());

    // Make sure we can identify the languages in which a particular word appears
    List<String> fooLanguages = Arrays.asList(service.getLanguages("foo"));
    Assert.assertEquals(2, fooLanguages.size());
    Assert.assertTrue(fooLanguages.contains("en"));
    Assert.assertTrue(fooLanguages.contains("de"));

    List<String> barLanguages = Arrays.asList(service.getLanguages("bar"));
    Assert.assertEquals(1, barLanguages.size());
    Assert.assertFalse(barLanguages.contains("en"));
    Assert.assertTrue(barLanguages.contains("de"));

    // Make sure we can clear entire language sets
    service.clear("en");
    Assert.assertEquals(1, service.getLanguages().length);
    Assert.assertEquals("de", service.getLanguages()[0]);
    service.clear("de");
    Assert.assertEquals(0, service.getLanguages().length);
  }
  
  @Test
  public void testWordCapitalization() throws Exception {
    service.addWord("foo", "en");
    Assert.assertTrue(service.isWord("foo"));
    Assert.assertTrue(service.isWord("FOO"));
    Assert.assertTrue(service.isWord("FOO"));
    Assert.assertTrue(service.isWord("FoO"));
    Assert.assertTrue(service.isWord("foO"));
  }

  @Test
  public void testAddWordAgain() throws Exception {
    // we should be able to add a word more than once, and have its properties be updated
    service.addWord("foo", "en", 10, 0.1d);
    Assert.assertEquals(new Long(10), service.getWordCount("foo", "en"));
    service.addWord("foo", "en", 20, 0.1d);
    Assert.assertEquals(new Long(20), service.getWordCount("foo", "en"));

    // same for not specifying the weight
    service.addWord("foo", "en", 10);
    Assert.assertEquals(new Long(10), service.getWordCount("foo", "en"));
    service.addWord("foo", "en", 20);
    Assert.assertEquals(new Long(20), service.getWordCount("foo", "en"));
  }
  
  @Test
  public void testCleanText() throws Exception {
    service.addWord("foo", "en");
    service.addWord("bar", "en", 0);
    service.addWord("and", "en", 0, 0d);
    service.markStopWord("and", "en");
    
    String[] potentialWords = new String[] {"The", "foo", "and", "the", "bar"};
    
    DICT_TOKEN[] tokens = service.cleanText(potentialWords, "en");
    
    Assert.assertEquals(DICT_TOKEN.NONE, tokens[0]); // "The" is not a word
    Assert.assertEquals(DICT_TOKEN.WORD, tokens[1]); // "foo" is a word
    Assert.assertEquals(DICT_TOKEN.STOPWORD, tokens[2]); // "and" is a stop word
    Assert.assertEquals(DICT_TOKEN.NONE, tokens[3]); // "The" is not a word
    Assert.assertEquals(DICT_TOKEN.WORD, tokens[4]); // "bar" is a word
  }

  @Test
  public void testDetermineLanguages() throws Exception {
    service.addWord("foo", "en");
    service.addWord("bar", "en");
    service.addWord("bar", "de");
    
    String[] potentialWords = new String[] {"foo", "and", "bar"};
    
    // Two of the three words are English, only one is German.  So this should be an English phrase first, and
    // a German phrase second
    Assert.assertEquals("en", service.detectLanguage(potentialWords)[0]);
    Assert.assertEquals("de", service.detectLanguage(potentialWords)[1]);

    // If all three words are in German, while only two are in English, this should become a German phrase first.
    service.addWord("foo", "de");
    service.addWord("and", "de");
    Assert.assertEquals("de", service.detectLanguage(potentialWords)[0]);
    Assert.assertEquals("en", service.detectLanguage(potentialWords)[1]);
  }    

  @Test
  public void testWordProperties() throws Exception {
    service.addWord("foo", "en", 10, 0.5d);
    service.addWord("bar", "en", 2, 0.1d);

    Assert.assertEquals(new Long(10), service.getWordCount("foo", "en"));
    Assert.assertEquals(0.5d, service.getWordWeight("foo", "en"), 0.001d);

    // These are not stop words (yet)
    Assert.assertFalse(service.isStopWord("foo"));
    Assert.assertFalse(service.isStopWord("foo", "en"));
    Assert.assertFalse(service.isStopWord("bar"));
    Assert.assertFalse(service.isStopWord("bar", "en"));

    // Scan the English dictionary for words with a weight above 0.4, and set these to stop words
    service.parseStopWords(0.4d, "en");
    
    // 'foo' should now be a stop word
    Assert.assertTrue(service.isStopWord("foo"));
    Assert.assertTrue(service.isStopWord("foo", "en"));
  }

  
}
