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

import org.opencastproject.dictionary.api.DictionaryService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.spi.PersistenceProvider;

/**
 * A JPA-based implementation of the DictionaryService. This implementation stores all words in a single table, and
 * keeps track of the available languages in a separate language table.
 */
public class DictionaryServiceJpaImpl implements DictionaryService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(DictionaryServiceJpaImpl.class);

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /** The properties used to generate an entity manager factory */
  protected Map<String, Object> persistenceProperties;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /**
   * Sets the JPA persistence provider
   * 
   * @param persistenceProvider
   *          the JPA PersistenceProvider
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * Sets the persistence properties used to customize the entity manager factory
   * 
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  public void activate(ComponentContext cc) {
    logger.debug("activate");
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.dictionary", persistenceProperties);
  }

  public void deactivate() {
    logger.debug("deactivate");
    emf.close();
  }

  /**
   * Gets a word in a particular language, or null if it doesn't exist in that language.
   * 
   * @param text
   *          The text of the word
   * @param language
   *          The language of the word
   * @return The word itself, or null if it doesn't exist in this language. Note that the word might contain text that
   *         is a transformation of the queried text, since Words store case-normalized text.
   */
  protected Word getWord(String text, String language) {
    text = Word.fixCase(text);
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Word.get");
      query.setParameter("text", text);
      query.setParameter("language", language);
      try {
        return (Word) query.getSingleResult();
      } catch (NoResultException e) {
        return null;
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#addWord(java.lang.String, java.lang.String)
   */
  @Override
  public void addWord(String text, String language) {
    text = Word.fixCase(text);
    Word word = getWord(text, language);
    if (word == null) {
      word = new Word(text, language);
      EntityManager em = null;
      EntityTransaction tx = null;
      try {
        em = emf.createEntityManager();
        tx = em.getTransaction();
        tx.begin();
        em.persist(word);
        tx.commit();
        logger.debug("Added '{}' to the {} dictionary", text, language);
      } catch (RollbackException e) {
        tx.rollback();
        throw e;
      } finally {
        if (em != null)
          em.close();
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#addWord(java.lang.String, java.lang.String,
   *      java.lang.Integer)
   */
  @Override
  public void addWord(String text, String language, Integer count) {
    text = Word.fixCase(text);
    Word word = getWord(text, language);
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      if (word == null) {
        word = new Word(text, language, count);
        em.persist(word);
        logger.debug("Added '{}' to the {} dictionary with count {}", new Object[] { text, language, count });
      } else {
        // update the word with the specified count
        word.count = count;
        em.merge(word);
      }
      tx.commit();
    } catch (RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#addWord(java.lang.String, java.lang.String,
   *      java.lang.Integer, java.lang.Double)
   */
  @Override
  public void addWord(String text, String language, Integer count, Double weight) {
    text = Word.fixCase(text);
    Word word = getWord(text, language);
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      if (word == null) {
        word = new Word(text, language, count, weight);
        em.persist(word);
        logger.debug("Added '{}' to the {} dictionary with count {} and weight {}", new Object[] { text, language,
                count, weight });
      } else {
        // update the word with the specified count and weight
        word.count = count;
        word.weight = weight;
        em.merge(word);
      }
      tx.commit();
    } catch (RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#cleanText(java.lang.String[], java.lang.String)
   */
  @Override
  public DICT_TOKEN[] cleanText(String[] text, String language) {
    DICT_TOKEN[] tokens = new DICT_TOKEN[text.length];
    for (int i = 0; i < text.length; i++) {
      Word word = getWord(text[i], language);
      if (word == null) {
        tokens[i] = DICT_TOKEN.NONE;
      } else if (word.stopWord) {
        tokens[i] = DICT_TOKEN.STOPWORD;
      } else {
        tokens[i] = DICT_TOKEN.WORD;
      }
    }
    return tokens;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#clear(java.lang.String)
   */
  @Override
  public void clear(String language) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Query q = em.createNamedQuery("Word.deleteLanguage");
      q.setParameter("language", language);
      int totalDeleted = q.executeUpdate();
      tx.commit();
      logger.info("Deleted {} words from the {} dictionary", totalDeleted, language);
    } catch (RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets words for a particular string.
   * 
   * @param text
   *          The text of the word
   * @return The words from all languages
   */
  @SuppressWarnings("unchecked")
  protected Word[] getWords(String text) {
    text = Word.fixCase(text);
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Word.wordsFromText");
      query.setParameter("text", text);
      return (Word[]) query.getResultList().toArray(new Word[0]);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#detectLanguage(java.lang.String[])
   */
  @Override
  public String[] detectLanguage(String[] text) {
    // FIXME This is not an efficient use of the database
    Map<String, Integer> languageScores = new HashMap<String, Integer>();
    for (String t : text) {
      for (Word word : getWords(t)) {
        Integer previousScore = languageScores.get(word.language);
        if (previousScore == null) {
          languageScores.put(word.language, 1);
        } else {
          languageScores.put(word.language, ++previousScore);
        }
      }
    }
    return sortByValue(languageScores).toArray(new String[0]);
  }

  /**
   * Sorts map keys by the entry values, high to low
   */
  protected static <K, V extends Comparable<? super V>> List<K> sortByValue(Map<K, V> map) {
    List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>(map.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
      public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
        return (o2.getValue()).compareTo(o1.getValue());
      }
    });
    List<K> result = new ArrayList<K>();
    for (Map.Entry<K, V> entry : list) {
      result.add(entry.getKey());
    }
    return result;
  }

  class ValueComparator implements Comparator<String> {
    private Map<String, Integer> base;

    public ValueComparator(Map<String, Integer> base) {
      this.base = base;
    }

    public int compare(String a, String b) {
      if (base.get(a) < base.get(b)) {
        return 1;
      } else if (base.get(a) == base.get(b)) {
        return 0;
      } else {
        return -1;
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#getLanguages()
   */
  @SuppressWarnings("unchecked")
  @Override
  public String[] getLanguages() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Word.languageCount");
      return (String[]) q.getResultList().toArray(new String[0]);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#getLanguages(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public String[] getLanguages(String text) {
    text = Word.fixCase(text);
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Word.wordByLanguage");
      q.setParameter("text", text);
      return (String[]) q.getResultList().toArray(new String[0]);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#getWordCount(java.lang.String, java.lang.String)
   */
  @Override
  public Long getWordCount(String text, String language) {
    Word word = getWord(text, language);
    if (word == null) {
      return 0L;
    } else {
      return word.count;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#getWordWeight(java.lang.String, java.lang.String)
   */
  @Override
  public double getWordWeight(String text, String language) {
    Word word = getWord(text, language);
    if (word == null) {
      return 0L;
    } else {
      return word.weight;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#isStopWord(java.lang.String)
   */
  @Override
  public Boolean isStopWord(String text) {
    Word[] words = getWords(text);
    for (Word w : words) {
      if (w.stopWord)
        return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#isStopWord(java.lang.String, java.lang.String)
   */
  @Override
  public Boolean isStopWord(String text, String language) {
    Word word = getWord(text, language);
    if (word == null) {
      return false;
    } else {
      return word.stopWord;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#isWord(java.lang.String)
   */
  @Override
  public Boolean isWord(String text) {
    return getWords(text).length > 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#isWord(java.lang.String, java.lang.String)
   */
  @Override
  public Boolean isWord(String text, String language) {
    return getWord(text, language) != null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#markStopWord(java.lang.String, java.lang.String)
   */
  @Override
  public void markStopWord(String text, String language) {
    Word word = getWord(text, language);
    text = Word.fixCase(text);
    if (word == null) {
      word = new Word(text, language);
    }
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      word.stopWord = true;
      em.merge(word);
      logger.debug("Marked '{}' in the {} dictionary as a stop word", text, language);
      tx.commit();
    } catch (RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.dictionary.api.DictionaryService#parseStopWords(java.lang.Double, java.lang.String)
   */
  @Override
  public void parseStopWords(Double threshold, String language) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Query q = em.createNamedQuery("Word.updateStopWords");
      q.setParameter("threshold", threshold);
      q.setParameter("language", language);
      int numUpdatedRows = q.executeUpdate();
      logger.info("Marked {} words with weights > {} as {} language stopwords", new Object[] { numUpdatedRows,
              threshold, language });
      tx.commit();
    } catch (RollbackException e) {
      tx.rollback();
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }
}
